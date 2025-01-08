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
    private val toggleDebugInfo: () -> Unit
) : InputAdapter() {

    companion object {
        val logger = ktx.log.logger<GameInputManager>()
    }

    private var isEmulatedDragOn: Boolean = false
    private var isDraggingGame = false
    private var lastTouch = Vector2()
    private var lastMouseWorldPos = Vector2()
    private var lastClickTime: Long = 0
    private val doubleClickThreshold = 300
    private val longPressDuration = 500
    private var isPotentialClick = false

    var lastPublicMouseWorldPos = Vector2()
        private set

    private var initialTouchPos = Vector2()
    private val dragThreshold = 5f

    init {
        logger.debug { "GameInputManager.init" }
    }

    // Inner InputProcessor class logic moved to here
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        logger.misc { "mouseMoved screenX=$screenX screenY=$screenY" }

        val worldCoordinates = cameraController.gameCamera.unprojectScreenCoords(screenX, screenY)
        val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)
        lastPublicMouseWorldPos.set(mousePos)

        if (!environment.isMobile) {

            if (isEmulatedDragOn) {
                if (cursorM.currentCursor == null) {
                    cursorM.setCursor(cursorM.handClosedCursor)
                    lastMouseWorldPos.set(mousePos)
                }

                touchDragged(screenX, screenY, 0)
                return true
            }

            // First, if the mouse is above a button, change the cursor
            if (uiManager.isPointerOverButton(mousePos)) {
                cursorM.setCursor(cursorM.handPointingCursor)
                return false
            }

            // Second, if there is a popup active, we don't change cursors
            if (uiManager.currentPopupWindow != null || puzzleManager.editingPuzzlePiece != null || puzzleManager.editingPuzzleFeature != null) {
                cursorM.setCursor(null)
                return false
            }

            // Then we check if the pointer is over a puzzle piece's approximate area
            gameModel.puzzlePieces.sortByDescending { it.depth }
            val puzzleUnderPointer = gameModel.puzzlePieces.find { isPointerOverPuzzlePiece(mousePos, it, true) }

            if (puzzleUnderPointer != null && puzzleManager.draggedPuzzlePiece == null) {

                val rules = gameModel.currentExercise?.type?.ruleset

                // 1. Feature addition/removal

                // We check if the pointer is over a missing feature (TAB or BLANK)
                // But only if we are allowed to add/remove features
                if (rules?.canAddRemoveTabs == true) {
                    var potentialFeatureType: Pair<PuzzlePieceFeature.Type, Side>? = null

                    val typeAndSideOrNull = puzzleUnderPointer.findPotentialFeatureUnderPointer(mousePos)
                    if (!typeAndSideOrNull.isEmpty) {
                        logger.debug { "mouseMoved potentialFeatureType=${typeAndSideOrNull.get()}" }
                        potentialFeatureType = typeAndSideOrNull.get()
                    }

                    if (potentialFeatureType != null) {
                        cursorM.setCursor(cursorM.addFeatureCursor)
                        puzzleManager.featureTripleToAdd = Triple(
                            puzzleUnderPointer,
                            potentialFeatureType.second,
                            potentialFeatureType.first
                        )
                        return true
                    }
                    else {
                        puzzleManager.featureTripleToAdd = null
                    }
                }

                // We check if it's over an existing feature
                // But only if we are allowed to add/remove features
                if (rules?.canAddRemoveTabs == true || rules?.canEditTabText == true) {
                    val featureUnderPointer = isPointOverEditablePuzzleFeature(mousePos, puzzleUnderPointer)
                    if (!featureUnderPointer.isEmpty) {
                        val feature = featureUnderPointer.get()
                        if (feature is PuzzleTab) {
                            val isPointerOverText = feature.isPointerOverTextLayout(mousePos)
                            if (isPointerOverText && rules.canEditTabText) {
                                cursorM.setCursor(cursorM.editTextCursor)
                                puzzleManager.puzzleFeatureToEdit = feature
                                return true
                            }
                        }
                        puzzleManager.puzzleFeatureToEdit = null

                        if (rules.canAddRemoveTabs) {
                            cursorM.setCursor(cursorM.removeFeatureCursor)
                            puzzleManager.featureToRemove = Pair(puzzleUnderPointer, feature)
                        }
                        return true
                    }
                    else {
                        puzzleManager.featureToRemove = null
                    }
                }

                // 2. Puzzle text editing and detection of draggable piece

                // Check if we are allowed to edit puzzle text

                // We check if it's over a puzzle piece's text
                val isTextUnderPointer = puzzleUnderPointer.isPointerOverTextLayout(mousePos)
                if (isTextUnderPointer && rules?.canEditBaseText == true) {
                    cursorM.setCursor(cursorM.editTextCursor)
                    puzzleManager.puzzlePieceToEdit = puzzleUnderPointer
                    return true
                }
                puzzleManager.puzzlePieceToEdit = null

                // We recheck if it's over the puzzle piece's exact area
                if (isPointerOverPuzzlePiece(mousePos, puzzleUnderPointer, false)) {
                    cursorM.setCursor(cursorM.handOpenCursor)
                    puzzleManager.puzzlePieceToDragOrRotate = puzzleUnderPointer
                    return true
                }
                puzzleManager.puzzlePieceToDragOrRotate = null
            }

//            gameModel.puzzlePieces.filter { !it.isConnected }.forEach { puzzlePiece ->
//                val corner = isPointerNearCorner(mousePos, puzzlePiece)
//                if (corner != null) {
//                    puzzleManager.puzzlePieceToRotate = puzzlePiece
//                    cursorM.setCursor(
//                        when (corner) {
//                            Corner.TOP_LEFT -> cursorM.rotateLeftCursor
//                            Corner.TOP_RIGHT -> cursorM.rotateRightCursor
//                        }
//                    )
//                    return true
//                }
//            }

            if (cursorM.currentCursor != null) {
                cursorM.setCursor(null)
            }
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        logger.debug { "keyDown keycode=$keycode" }
        when (keycode) {
//            Input.Keys.W -> {
//                handleWPressed(Gdx.input.x, Gdx.input.y)
//                return true
//            }
            else -> return false
        }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            Input.Buttons.LEFT -> {
                handleLeftClick(screenX, screenY)
                return true
            }

            Input.Buttons.RIGHT -> {
                handleRightClick()
                return true
            }

            else -> return false
        }
    }

    private fun handleLeftClick(screenX: Int, screenY: Int) {
        logger.debug { "handleLeftClick" }

        val worldCoordinates = cameraController.gameCamera.unprojectScreenCoords(screenX, screenY)
        val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

        if (isEmulatedDragOn) {
            isEmulatedDragOn = false
            cursorM.setCursor(null)
            puzzleManager.stopDragging()
            isDraggingGame = false
            return
        }

        if (uiManager.currentPopupWindow != null) {
            logger.info { "Click besides popup, removing window." }
            uiManager.currentPopupWindow!!.remove()
            uiManager.currentPopupWindow = null
            return
        }

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

        if (puzzleManager.puzzleFeatureToEdit != null || puzzleManager.puzzlePieceToEdit != null) {
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

        if (puzzleManager.puzzlePieceToDragOrRotate != null) {
            isPotentialClick = true
            initialTouchPos.set(mousePos)
            lastMouseWorldPos.set(mousePos)
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

    private fun handleWPressed(screenX: Int, screenY: Int) {
        logger.debug { "handleWClick" }

        val worldCoordinates = cameraController.gameCamera.unprojectScreenCoords(screenX, screenY)
        val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)
        puzzleManager.addNewPuzzlePiece(mousePos.cpy().sub(Vector2(PuzzlePiece.MIN_SIZE / 2, PuzzlePiece.MIN_SIZE / 2)))
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        logger.misc { "touchDragged screenX=$screenX screenY=$screenY" }

        val worldCoordinates = cameraController.gameCamera.unprojectScreenCoords(screenX, screenY)
        val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

        if (isPotentialClick && puzzleManager.puzzlePieceToDragOrRotate != null) {
            val distanceMoved = mousePos.dst(initialTouchPos)
            if (distanceMoved > dragThreshold) {
                // Initiate drag
                isPotentialClick = false
                puzzleManager.startDragging(puzzleManager.puzzlePieceToDragOrRotate!!) // Use initialTouchPos for start
                cursorM.setCursor(cursorM.handClosedCursor)
                lastMouseWorldPos.set(mousePos)
                return true
            }
        }
        else if (puzzleManager.draggedPuzzlePiece != null) {
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
            if (isPotentialClick && puzzleManager.puzzlePieceToDragOrRotate != null) {
                logger.debug { "Single click: Rotating puzzle piece" }
                if (!puzzleManager.puzzlePieceToDragOrRotate!!.isConnected) {
                    puzzleManager.puzzlePieceToDragOrRotate?.rotateRight()
                }
            }

            puzzleManager.stopDragging()
            isDraggingGame = false
            isPotentialClick = false // Reset the flag

            if (!environment.isMobile) {
                if (cursorM.currentCursor == cursorM.handClosedCursor) {
                    cursorM.setCursor(cursorM.handOpenCursor)
                    gameModel.rebasePuzzleDepths()
                    mouseMoved(screenX, screenY)
                }
            }
            gameModel.isSolved()
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

    fun emulateDragging() {
        logger.debug { "emulateDragging" }
        isEmulatedDragOn = true
    }

    private fun isPointerOverPuzzlePiece(mousePos: Vector2, puzzlePiece: PuzzlePiece, approximate: Boolean = false): Boolean {
        val offset = if (approximate) 50f else 0f

        return mousePos.x in (puzzlePiece.pos.x - offset)..(puzzlePiece.pos.x + puzzlePiece.size + offset) &&
            mousePos.y in (puzzlePiece.pos.y - offset)..(puzzlePiece.pos.y + puzzlePiece.size + offset)
    }

    private fun isPointOverEditablePuzzleFeature(mousePos: Vector2, puzzlePiece: PuzzlePiece): Optional<PuzzlePieceFeature> {

        puzzlePiece.tabs.forEach { tab ->
            if (tab.isPointOverFeature(mousePos)) {
                logger.info { "Pointer is over tab." }
                if (!tab.owner!!.copyOfConnections.map { it.via }.contains(tab)) {
                    return Optional.of(tab)
                }
            }
        }

        // We don't need the blanks after all
//        puzzlePiece.blanks.forEach { blank ->
//            if (blank.isPointOverFeature(mousePos)) {
//                logger.info { "Pointer is over blank." }
//                if (blank.owner!!.copyOfConnections.map { it.via.side }.none { side -> side.opposite() == blank.side }) {
//                    return Optional.of(blank)
//                }
//            }
//        }

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
