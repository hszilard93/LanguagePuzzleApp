package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePieceFeature
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.view.screens.OldGameScreen
import edu.b4kancs.languagePuzzleApp.app.view.utils.unprojectScreenCoords
import java.util.Optional


enum class Corner {
    TOP_LEFT, TOP_RIGHT
}

class GameInputManager(
    private val cameraController: CameraController,
    private val puzzleManager: PuzzleManager,
    private val cursorM: CursorManager,
    private val uiManager: UIManager,
    private val environment: Environment,
    private val gameModel: GameModel,
    private val realToVirtualResolutionRatio: Float,
    private val setBackgroundColor: (Int, Int, Int, Float) -> Unit,
    private val toggleDebugInfo: () -> Unit,
    private val displayCheckMark: () -> Unit
) : InputAdapter() {

    companion object {
        val logger = ktx.log.logger<GameInputManager>()
    }

    private var isDraggingGame = false
    private var lastTouch = Vector2()
    private var lastMouseWorldPos = Vector2()
    private var lastClickTime: Long = 0
    private val doubleClickThreshold = 300
    private val longPressDuration = 500

    init {
        logger.debug { "GameInputManager.init" }
    }

    // Inner InputProcessor class logic moved to here
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        OldGameScreen.logger.misc { "mouseMoved screenX=$screenX screenY=$screenY" }

        val worldCoordinates = cameraController.gameCamera.unprojectScreenCoords(screenX, screenY)
        val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

        if (!environment.isMobile) {
            // *First* first, If there is a popup active, we don't change cursors
            if (uiManager.currentPopupWindow != null || puzzleManager.editingPuzzlePiece != null || puzzleManager.editingPuzzleFeature != null) {
                cursorM.setCursor(null)
                return false
            }

            // First, we check if the pointer is over a puzzle piece's approximate area
            gameModel.puzzlePieces.sortByDescending { it.depth }
            val puzzleUnderPointer = gameModel.puzzlePieces.find { isPointerOverPuzzlePiece(mousePos, it, true) }

            if (puzzleUnderPointer != null && puzzleManager.draggedPuzzlePiece == null) {
                // We check if it's over a missing feature (TAB or BLANK)
                var potentialFeatureType: Pair<PuzzlePieceFeature.Type, Side>? = null

                val typeAndSideOrNull = puzzleUnderPointer.findPotentialFeatureUnderPointer(mousePos)
                if (!typeAndSideOrNull.isEmpty) {
                    logger.debug { "mouseMoved potentialFeatureType=${typeAndSideOrNull.get()}" }
                    potentialFeatureType = typeAndSideOrNull.get()
                }

                if (potentialFeatureType != null) {
                    cursorM.setCursor(cursorM.addFeatureCursor)
                    puzzleManager.featureTripleToAdd = Triple(puzzleUnderPointer, potentialFeatureType.second, potentialFeatureType.first)
                    return true
                }
                else {
                    puzzleManager.featureTripleToAdd = null
                }

                // We check if it's over an existing feature
                val featureUnderPointer = isPointOverPuzzleFeature(mousePos, puzzleUnderPointer)
                if (!featureUnderPointer.isEmpty) {
                    val feature = featureUnderPointer.get()
                    if (feature is PuzzleTab) {
                        val isPointerOverText = feature.isPointerOverTextLayout(mousePos)
                        if (isPointerOverText) {
                            cursorM.setCursor(cursorM.editTextCursor)
                            puzzleManager.puzzleFeatureToEdit = feature
                            return true
                        }
                    }
                    puzzleManager.puzzleFeatureToEdit = null

                    cursorM.setCursor(cursorM.removeFeatureCursor)
                    puzzleManager.featureToRemove = Pair(puzzleUnderPointer, feature)
                    return true
                }
                else {
                    puzzleManager.featureToRemove = null
                }

                // We check if it's over a puzzle piece's text
                val isTextUnderPointer = puzzleUnderPointer.isPointerOverTextLayout(mousePos)
                if (isTextUnderPointer) {
                    cursorM.setCursor(cursorM.editTextCursor)
                    puzzleManager.puzzlePieceToEdit = puzzleUnderPointer
                    return true
                }
                puzzleManager.puzzlePieceToEdit = null

                // We recheck if it's over the puzzle piece's exact area
                if (isPointerOverPuzzlePiece(mousePos, puzzleUnderPointer, false)) {
                    cursorM.setCursor(cursorM.handOpenCursor)
                    puzzleManager.puzzlePieceToDrag = puzzleUnderPointer
                    return true
                }
                puzzleManager.puzzlePieceToDrag = null
            }

            gameModel.puzzlePieces.filter { !it.isConnected }.forEach { puzzlePiece ->
                val corner = isPointerNearCorner(mousePos, puzzlePiece)
                if (corner != null) {
                    puzzleManager.puzzlePieceToRotate = puzzlePiece
                    cursorM.setCursor(
                        when (corner) {
                            Corner.TOP_LEFT -> cursorM.rotateLeftCursor
                            Corner.TOP_RIGHT -> cursorM.rotateRightCursor
                        }
                    )
                    return true
                }
            }

            if (cursorM.currentCursor != null) {
                cursorM.setCursor(null)
            }
        }
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            Input.Buttons.LEFT -> handleLeftClick(screenX, screenY)
            Input.Buttons.RIGHT -> handleRightClick()
            else -> return false
        }
        return true
    }

    private fun handleLeftClick(screenX: Int, screenY: Int) {
        logger.debug { "handleLeftClick" }

        if (uiManager.currentPopupWindow != null) {
            logger.info { "Click besides popup, removing window." }
            uiManager.currentPopupWindow!!.remove()
            uiManager.currentPopupWindow = null
            return
        }

        val worldCoordinates = cameraController.gameCamera.unprojectScreenCoords(screenX, screenY)
        val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

        val currentTime = System.currentTimeMillis()
        val isDoubleClick = currentTime - lastClickTime < doubleClickThreshold

        if (isDoubleClick) {
            puzzleManager.puzzlePieceToEdit?.let { puzzlePiece ->
                if (puzzleManager.puzzlePieceToEdit!!.isPointerOverTextLayout(mousePos)) {
                    logger.debug { "doubleClick puzzlePiece=$puzzlePiece" }
                    if (!puzzlePiece.isConnected) {
                        puzzleManager.openTextEditor(puzzlePiece)
                        cursorM.setCursor(null)
                    }
                    return
                }
            }

            puzzleManager.puzzleFeatureToEdit?.let { feature ->
                feature as PuzzleTab
                if (feature.isPointerOverTextLayout(mousePos)) {
                    logger.debug { "doubleClick puzzleFeature=$feature" }
                    puzzleManager.openTextEditor(feature)
                    cursorM.setCursor(null)
                }
                return
            }
        }

        lastClickTime = currentTime

        if (uiManager.currentPopupWindow != null || puzzleManager.puzzleFeatureToEdit != null || puzzleManager.puzzlePieceToEdit != null) {
            return
        }

        if (puzzleManager.puzzlePieceToDrag != null) {
            puzzleManager.startDragging(puzzleManager.puzzlePieceToDrag!!, mousePos)
            cursorM.setCursor(cursorM.handClosedCursor)
            lastMouseWorldPos.set(mousePos)
            return
        }

        if (puzzleManager.featureTripleToAdd != null) {
            puzzleManager.addFeature()
            cursorM.setCursor(null)
            return
        }

        if (puzzleManager.featureToRemove != null) {
            puzzleManager.removeFeature()
            cursorM.setCursor(cursorM.addFeatureCursor)
            return
        }

        if (cursorM.currentCursor == cursorM.rotateLeftCursor) {
            logger.debug { "rotateLeft" }
            puzzleManager.puzzlePieceToRotate!!.rotateLeft()
            return
        }
        else if (cursorM.currentCursor == cursorM.rotateRightCursor) {
            logger.debug { "rotateRight" }
            puzzleManager.puzzlePieceToRotate!!.rotateRight()
            return
        }

        // Start dragging the game
        isDraggingGame = true
        lastTouch.set(worldCoordinates)
    }

    private fun handleRightClick() {
        logger.debug { "handleRightClick" }
        // Toggle logic should be handled externally or via a callback
        toggleDebugInfo()
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        logger.misc { "touchDragged screenX=$screenX screenY=$screenY" }

        val worldCoordinates = cameraController.gameCamera.unprojectScreenCoords(screenX, screenY)
        val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

        if (puzzleManager.draggedPuzzlePiece != null) {
            puzzleManager.dragPuzzle(mousePos, lastMouseWorldPos)
            lastMouseWorldPos.set(mousePos)
            return true
        }
        else if (isDraggingGame) {
            val deltaX = Gdx.input.deltaX.toFloat() * (1 / realToVirtualResolutionRatio) * cameraController.gameCamera.zoom
            val deltaY = Gdx.input.deltaY.toFloat() * (1 / realToVirtualResolutionRatio) * cameraController.gameCamera.zoom
            cameraController.gameCamera.translate(-deltaX, deltaY, 0f)
            cameraController.gameCamera.update()
            return true
        }
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        logger.debug { "touchUp button = $button" }

        if (button == Input.Buttons.LEFT) {
//            if (isDraggingGame) {
            puzzleManager.stopDragging()
            isDraggingGame = false
//            }

            if (!environment.isMobile) {
                if (cursorM.currentCursor == cursorM.handClosedCursor) {
                    cursorM.setCursor(cursorM.handOpenCursor)
                    gameModel.rebasePuzzleDepths()
                }
            }
            if (gameModel.isSolved()) {
                displayCheckMark()
            }
            return true
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        logger.debug { "scrolled amountX=$amountX amountY=$amountY zoom=${cameraController.gameCamera.zoom}" }

        val mouseWorldPosBefore = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        cameraController.gameCamera.unproject(mouseWorldPosBefore)
        val newZoom = (cameraController.gameCamera.zoom + amountY * 0.05f).coerceIn(1f, 3.5f)
        cameraController.gameCamera.zoom = newZoom
        cameraController.gameCamera.update()

        val mouseWorldPosAfter = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        cameraController.gameCamera.unproject(mouseWorldPosAfter)
        val offsetX = mouseWorldPosAfter.x - mouseWorldPosBefore.x
        val offsetY = mouseWorldPosAfter.y - mouseWorldPosBefore.y
        cameraController.gameCamera.translate(-offsetX, -offsetY, 0f)
        cameraController.gameCamera.update()
        return true
    }

    private fun isPointerOverPuzzlePiece(mousePos: Vector2, puzzlePiece: PuzzlePiece, approximate: Boolean = false): Boolean {
        val offset = if (approximate) 50f else 0f

        return mousePos.x in (puzzlePiece.pos.x - offset)..(puzzlePiece.pos.x + puzzlePiece.size + offset) &&
            mousePos.y in (puzzlePiece.pos.y - offset)..(puzzlePiece.pos.y + puzzlePiece.size + offset)
    }

    private fun isPointOverPuzzleFeature(mousePos: Vector2, puzzlePiece: PuzzlePiece): Optional<PuzzlePieceFeature> {

        puzzlePiece.tabs.forEach { tab ->
            if (tab.isPointOverFeature(mousePos)) {
                logger.info { "Pointer is over tab." }
                return Optional.of(tab)
            }
        }

        puzzlePiece.blanks.forEach { blank ->
            if (blank.isPointOverFeature(mousePos)) {
                logger.info { "Pointer is over blank." }
                return Optional.of(blank)
            }
        }

        return Optional.empty()
    }

    private fun isPointerNearCorner(mousePos: Vector2, puzzlePiece: PuzzlePiece): Corner? {
        val maxDistance = 25f
        val topLeft = Vector2(puzzlePiece.pos.x, puzzlePiece.pos.y + puzzlePiece.size)
        val topRight = Vector2(puzzlePiece.pos.x + puzzlePiece.size, puzzlePiece.pos.y + puzzlePiece.size)
        return when {
            mousePos.dst(topRight) < maxDistance -> Corner.TOP_RIGHT
            mousePos.dst(topLeft) < maxDistance -> Corner.TOP_LEFT
            else -> null
        }
    }
}
