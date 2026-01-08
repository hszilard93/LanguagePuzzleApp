package edu.b4kancs.languagePuzzleApp.app.view.screens.game.input

import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.Constants

class GameGestureDetector(private val gameCamera: GameCamera) : GestureDetector(object : GestureListener {

    private var lastZoomDistance = 0f
    private var lastZoomTime = System.currentTimeMillis()

    private val logger = ktx.log.logger<GameGestureDetector>()

    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun longPress(x: Float, y: Float): Boolean {
        logger.debug { "longPress x=$x y=$y" }

        return true
    }

    override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        logger.debug { "pan x=$x y=$y deltaX=$deltaX deltaY=$deltaY" }

        // Move the camera in the opposite direction of the drag
        // Note: on mobile we invert deltaY as in the desktop version
        gameCamera.translate(-deltaX / gameCamera.zoom, deltaY / gameCamera.zoom, 0f)
        gameCamera.update()

        return true
    }

    override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        logger.debug { "panStop x=$x y=$y pointer=$pointer button=$button" }

        // Nothing special needed here for basic camera movement
        // This would be where you'd handle momentum if desired

        return true
    }

    override fun zoom(originalDistance: Float, currentDistance: Float): Boolean {
        logger.debug { "zoom originalDistance=$originalDistance currentDistance=$currentDistance" }

        // Reset zoom after 0.2 seconds of no zoom
        if (System.currentTimeMillis() - lastZoomTime > 100) {
            lastZoomDistance = 0f
        }

        if (originalDistance > 0f) {
            if (lastZoomDistance == 0f) lastZoomDistance = originalDistance

            val scaleDelta = (1f - currentDistance / lastZoomDistance).coerceIn(-0.05f, 0.05f)
            logger.debug { "scaleDelta=$scaleDelta" }
            val newZoom = (gameCamera.zoom + scaleDelta).coerceIn(Constants.MIN_ZOOM, Constants.MAX_ZOOM)
            logger.debug { "newZoom=$newZoom" }
            gameCamera.zoom = newZoom
            gameCamera.update()

            lastZoomDistance = currentDistance
            lastZoomTime = System.currentTimeMillis()
        }
        return true
    }

    override fun pinch(initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?): Boolean {
        TODO("Not yet implemented")
    }

    override fun pinchStop() {
        TODO("Not yet implemented")
    }
})
