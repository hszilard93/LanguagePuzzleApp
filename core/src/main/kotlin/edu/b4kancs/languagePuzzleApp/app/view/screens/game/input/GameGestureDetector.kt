package edu.b4kancs.languagePuzzleApp.app.view.screens.game.input

import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CameraController
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.Constants
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CursorManager
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.PuzzleManager
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.UIManager
import ktx.log.logger

/**
 * Handles touch gestures for mobile platforms (pan, zoom, pinch).
 * This detector is used instead of GameInputManager on mobile devices.
 */
class GameGestureDetector(
    private val cameraController: CameraController,
    private val puzzleManager: PuzzleManager,
    private val cursorManager: CursorManager,
    private val uiManager: UIManager,
    private val environment: Environment,
    private val gameModel: GameModel,
    private val gameViewport: GameViewport
) : GestureDetector(MobileGestureListener(cameraController)), GameInputHandler {

    companion object {
        val logger = logger<GameGestureDetector>()
    }

    private var lastTouchPos = Vector2()

    override fun emulatePuzzleDragging() {
        logger.debug { "emulatePuzzleDragging (mobile - no-op)" }
        // Not applicable for mobile gesture detection
    }

    override fun getLastTouch(): Vector2 {
        return lastTouchPos
    }
}

/**
 * Internal gesture listener implementation for mobile touch handling.
 */
private class MobileGestureListener(
    private val cameraController: CameraController
) : GestureDetector.GestureListener {

    private var lastZoomDistance = 0f
    private var lastZoomTime = System.currentTimeMillis()

    private val logger = logger<GameGestureDetector>()

    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        logger.debug { "touchDown x=$x y=$y pointer=$pointer button=$button" }
        return false
    }

    override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
        logger.debug { "tap x=$x y=$y count=$count button=$button" }
        return false
    }

    override fun longPress(x: Float, y: Float): Boolean {
        logger.debug { "longPress x=$x y=$y" }
        return true
    }

    override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
        logger.debug { "fling velocityX=$velocityX velocityY=$velocityY button=$button" }
        return false
    }

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        logger.debug { "pan x=$x y=$y deltaX=$deltaX deltaY=$deltaY" }

        // Move the camera in the opposite direction of the drag
        // Note: on mobile we invert deltaY as in the desktop version
        cameraController.gameCamera.translate(-deltaX / cameraController.gameCamera.zoom, deltaY / cameraController.gameCamera.zoom, 0f)
        cameraController.gameCamera.update()

        return true
    }

    override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        logger.debug { "panStop x=$x y=$y pointer=$pointer button=$button" }
        return true
    }

    override fun zoom(originalDistance: Float, currentDistance: Float): Boolean {
        logger.debug { "zoom originalDistance=$originalDistance currentDistance=$currentDistance" }

        // Reset zoom after 100ms of no zoom activity
        if (System.currentTimeMillis() - lastZoomTime > 100) {
            lastZoomDistance = 0f
        }

        if (originalDistance > 0f) {
            if (lastZoomDistance == 0f) lastZoomDistance = originalDistance

            val scaleDelta = (1f - currentDistance / lastZoomDistance).coerceIn(-0.05f, 0.05f)
            logger.debug { "scaleDelta=$scaleDelta" }
            val newZoom = (cameraController.gameCamera.zoom + scaleDelta).coerceIn(Constants.MIN_ZOOM, Constants.MAX_ZOOM)
            logger.debug { "newZoom=$newZoom" }
            cameraController.gameCamera.zoom = newZoom
            cameraController.gameCamera.update()

            lastZoomDistance = currentDistance
            lastZoomTime = System.currentTimeMillis()
        }
        return true
    }

    override fun pinch(initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?): Boolean {
        logger.debug { "pinch" }
        return false
    }

    override fun pinchStop() {
        logger.debug { "pinchStop" }
    }
}
