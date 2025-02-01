package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import edu.b4kancs.languagePuzzleApp.app.Game.Companion.DEFAULT_ZOOM
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePieceFeature
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.view.utils.unprojectScreenCoords
import java.util.Optional


enum class Corner {
    TOP_LEFT, TOP_RIGHT
}

class GameInputManager(
    private val cameraController: CameraController,
    private val puzzleManager: PuzzleManager,
    private val cursorManager: CursorManager,
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
    private var lastZoomTime: Long = 0
    private val zoomTimeTreshold = 40
    private var isPotentialClick = false

    private var isCtrlPressed = false
    private var isAltPressed = false

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
                if (cursorManager.currentCursor == null) {
                    cursorManager.setCursor(cursorManager.handClosedCursor)
                    lastMouseWorldPos.set(mousePos)
                }

                touchDragged(screenX, screenY, 0)
                return true
            }

            // First, if the mouse is above a button, change the cursor
            if (uiManager.isPointerOverButton()) {
                cursorManager.setCursor(cursorManager.handPointingCursor)
                return false
            }

            // Second, if there is a popup active, we don't change cursors
            if (uiManager.currentPopupWindow != null || puzzleManager.editingPuzzlePiece != null || puzzleManager.editingPuzzleFeature != null) {
                cursorManager.setCursor(null)
                return false
            }

            // Make sure the scroll focus is not captured unnecessarily
            uiManager.setFocusToTaskDescription(false)

            // Then we check if the pointer is over a puzzle piece's approximate area
            gameModel.puzzlePieces.toMutableList().sortByDescending { it.depth }
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
                        cursorManager.setCursor(cursorManager.addFeatureCursor)
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

                // We check if the pointer is over an existing feature
                // But only if we are allowed to add/remove features
                val featureUnderPointer = isPointOverEditablePuzzleFeature(mousePos, puzzleUnderPointer)
                if (!featureUnderPointer.isEmpty) {
                    val feature = featureUnderPointer.get()
                    if (feature is PuzzleTab) {
                        if (feature.grammaticalRole == GrammaticalRole.UNDEFINED && rules?.canColorTabs == true) {
                            cursorManager.setCursor(cursorManager.gearCursor)
                            puzzleManager.featureTripleToAdd = Triple(puzzleUnderPointer, feature.side, PuzzlePieceFeature.Type.TAB)
                            return true
                        }

                        if (rules?.canEditTabText == true) {
                            val isPointerOverText = feature.isPointerOverTextLayout(mousePos)
                            if (isPointerOverText) {
                                cursorManager.setCursor(cursorManager.editTextCursor)
                                puzzleManager.featureToTextEdit = feature
                                return true
                            }
                        }
                        puzzleManager.featureToTextEdit = null

                        if (rules?.canAddRemoveTabs == true) {
                            cursorManager.setCursor(cursorManager.removeFeatureCursor)
                            puzzleManager.featureToRemove = Pair(puzzleUnderPointer, feature)
                            return true
                        }
                    }
                    puzzleManager.featureToRemove = null
                    return true
                }
                else {
                    puzzleManager.featureToRemove = null
                    puzzleManager.featureToTextEdit = null
                }

                // 2. Puzzle text editing and detection of draggable piece

                // Check if we are allowed to edit puzzle text

                // We check if it's over a puzzle piece's text
                val isTextUnderPointer = puzzleUnderPointer.isPointerOverTextLayout(mousePos)
                if (isTextUnderPointer && rules?.canEditBaseText == true) {
                    cursorManager.setCursor(cursorManager.editTextCursor)
                    puzzleManager.puzzlePieceToEdit = puzzleUnderPointer
                    return true
                }
                puzzleManager.puzzlePieceToEdit = null

                // We recheck if it's over the puzzle piece's exact area
                if (isPointerOverPuzzlePiece(mousePos, puzzleUnderPointer, false)) {
                    cursorManager.setCursor(cursorManager.handOpenCursor)
                    puzzleManager.potentialDragOrRotatePiece = puzzleUnderPointer
                    return true
                }
                puzzleManager.potentialDragOrRotatePiece = null
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

            if (cursorManager.currentCursor != null) {
                cursorManager.setCursor(null)
            }
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        logger.debug { "keyDown keycode=$keycode" }

        when (keycode) {
            Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> isCtrlPressed = true
            Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> isAltPressed = true
        }

        when (keycode) {
            Input.Keys.LEFT -> if (isCtrlPressed) {
                logger.debug { "Ctrl + Left Arrow pressed" }
                gameModel.setUpPreviousTask {
                    uiManager.updateTaskInfo()
                }
                return true
            }
            Input.Keys.RIGHT -> if (isCtrlPressed) {
                logger.debug { "Ctrl + Right Arrow pressed" }
                gameModel.setUpNextTask {
                    uiManager.updateTaskInfo()
                }
                return true
            }

            Input.Keys.I -> if (isAltPressed) {
                logger.debug { "Alt + I pressed" }
                resetZoom()
                return true
            }

            Input.Keys.U -> if (isAltPressed) {
                logger.debug { "Alt + U pressed" }
                zoomIn()
                return true
            }

            Input.Keys.O -> if (isAltPressed) {
                logger.debug { "Alt + O pressed" }
                zoomOut()
                return true
            }

//            Input.Keys.W -> {
//                handleWPressed(Gdx.input.x, Gdx.input.y)
//                return true
//            }
            else -> return false
        }

        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        logger.debug { "keyUp keycode=$keycode" }

        when (keycode) {
            Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> isCtrlPressed = false
            Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> isAltPressed = false
            else -> return false
        }

        return false
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
            cursorManager.setCursor(null)
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
                        cursorManager.setCursor(null)
                    }
                    return
                }
            }

            puzzleManager.featureToTextEdit?.let { feature ->
                feature as PuzzleTab
                if (feature.isPointerOverTextLayout(mousePos)) {
                    logger.debug { "doubleClick puzzleFeature=$feature" }
                    puzzleManager.openTextEditor(feature)
                    cursorManager.setCursor(null)
                }
                return
            }
        }

        lastClickTime = currentTime

        if (puzzleManager.featureToTextEdit != null || puzzleManager.puzzlePieceToEdit != null) {
            return
        }

        if (puzzleManager.featureTripleToAdd != null) {
            puzzleManager.addFeature()
            cursorManager.setCursor(null)
            return
        }

        if (puzzleManager.featureToRemove != null) {
            puzzleManager.removeFeature()
            cursorManager.setCursor(cursorManager.addFeatureCursor)
            return
        }

        if (puzzleManager.potentialDragOrRotatePiece != null) {
            isPotentialClick = true
            initialTouchPos.set(mousePos)
            lastMouseWorldPos.set(mousePos)
            return
        }

        if (cursorManager.currentCursor == cursorManager.rotateLeftCursor) {
            logger.debug { "rotateLeft" }
            puzzleManager.puzzlePieceToRotate!!.rotateLeft()
            return
        }
        else if (cursorManager.currentCursor == cursorManager.rotateRightCursor) {
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

        if (isPotentialClick && puzzleManager.potentialDragOrRotatePiece != null) {
            val distanceMoved = mousePos.dst(initialTouchPos)
            if (distanceMoved > dragThreshold) {
                // Initiate drag
                isPotentialClick = false
                puzzleManager.startDragging(puzzleManager.potentialDragOrRotatePiece!!) // Use initialTouchPos for start
                cursorManager.setCursor(cursorManager.handClosedCursor)
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
            if (isPotentialClick && puzzleManager.potentialDragOrRotatePiece != null) {
                if (!puzzleManager.potentialDragOrRotatePiece!!.isConnected &&
                    puzzleManager.potentialDragOrRotatePiece!!.grammaticalRole != GrammaticalRole.VERB) {
                    logger.debug { "Single click: Rotating puzzle piece" }
                    puzzleManager.potentialDragOrRotatePiece?.rotateRight()
                }
            }

            puzzleManager.stopDragging()
            isDraggingGame = false
            isPotentialClick = false // Reset the flag

            if (!environment.isMobile) {
                if (cursorManager.currentCursor == cursorManager.handClosedCursor) {
                    cursorManager.setCursor(cursorManager.handOpenCursor)
                    gameModel.rebasePuzzleDepths()
                    mouseMoved(screenX, screenY)
                }
            }
            return true
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        logger.debug { "scrolled amountX=$amountX amountY=$amountY zoom=${cameraController.gameCamera.zoom}" }

        if (uiManager.isPointerOverTaskDescription()) {
            uiManager.setFocusToTaskDescription(true)
            return false
        }
        else {
            scrollWorld(amountY)
            return true
        }
    }

    private fun scrollWorld(amountY: Float) {
        logger.misc { "scrollWorld amountY=$amountY" }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastZoomTime < zoomTimeTreshold) {
            return
        }
        lastZoomTime = currentTime

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
    }

    private fun resetZoom() {
        cameraController.gameCamera.zoom = DEFAULT_ZOOM
        cameraController.gameCamera.update()
    }

    private fun zoomIn() {
        val newZoom = (cameraController.gameCamera.zoom + 0.1f).coerceIn(1f, 3.5f)
        cameraController.gameCamera.zoom = newZoom
        cameraController.gameCamera.update()
    }

    private fun zoomOut() {
        val newZoom = (cameraController.gameCamera.zoom - 0.1f).coerceIn(1f, 3.5f)
        cameraController.gameCamera.zoom = newZoom
        cameraController.gameCamera.update()
    }

    fun emulateDragging() {
        logger.debug { "emulateDragging" }
        isEmulatedDragOn = true
    }

    private fun isPointerOverPuzzlePiece(mousePos: Vector2, puzzlePiece: PuzzlePiece, approximate: Boolean = false): Boolean {
        val offset = if (approximate) 90f else 0f

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
