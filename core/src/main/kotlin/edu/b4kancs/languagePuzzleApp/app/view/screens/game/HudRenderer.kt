package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.HudFont
import ktx.graphics.use

class HudRenderer(
    private val batch: Batch,
    private val hudCamera: HudCamera,
    private val hudFont: HudFont,
    private val gameCamera: GameCamera,
    private val realToVirtualResolutionRatio: Float,
    private val getMousePositions: () -> Pair<Vector2, Vector2>
) {

    companion object {
        val logger = ktx.log.logger<HudRenderer>()
    }

    private var shouldDisplayDebugInfo = false

    fun render() {
        batch.projectionMatrix = hudCamera.combined
        batch.use {
            var message = "FPS=${Gdx.graphics.framesPerSecond}"
            hudFont.draw(batch, message, 10f, hudCamera.viewportHeight - 10f)
            message = "Zoom: %.1f".format(gameCamera.zoom)
            hudFont.draw(
                batch,
                message,
                hudCamera.viewportWidth - hudFont.lineHeight * (message.length / 2),
                hudCamera.viewportHeight - 10f
            )

            if (Game.IS_DEBUG_MODE_ON && shouldDisplayDebugInfo) {
                val (renderVector, worldVector) = getMousePositions()
                message = """$renderVector
                             |$worldVector
                             |$realToVirtualResolutionRatio""".trimMargin()
                hudFont.draw(batch, message, renderVector.x + 10f, renderVector.y + 10f)
            }
        }
    }

    fun displayDebugInfo(should: Boolean) {
        logger.info { "shouldDisplayDebugInfo = $should" }
        shouldDisplayDebugInfo = should
    }

    fun dispose() {

    }
}
