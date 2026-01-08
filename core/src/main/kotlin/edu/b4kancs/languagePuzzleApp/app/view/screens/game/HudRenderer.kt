package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.view.util.toVector2
import ktx.graphics.use

class HudRenderer(
    private val batch: Batch,
    private val hudCamera: HudCamera,
    private val hudViewport: HudViewport,
    private val hudFont: BitmapFont,
    private val gameCamera: GameCamera,
    private val realToVirtualResolutionRatio: Float
) {

    companion object {
        val logger = ktx.log.logger<HudRenderer>()
    }

    private var shouldDisplayMouseInfo = false

    fun render() {

        if (Game.IS_DEBUG_MODE_ON) {

            hudFont.setColor(0f, 0f, 0f, 1f)

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

                if (shouldDisplayMouseInfo) {
                    // Render the mouse position interpreted as real pixel coordinates within the viewport
                    val mouseX = Gdx.input.x.toFloat()
                    val mouseY = Gdx.input.y.toFloat()
                    val renderVector = hudViewport.unproject(Vector2(mouseX, mouseY))
                    val worldVector = gameCamera.unproject(Vector3(mouseX, mouseY, 0f)).toVector2()
                    if (hudViewport.screenWidth >= 200) {  // Ensure viewport is large enough
                        val renderX = (renderVector.x + 10f).coerceIn(10f, hudViewport.worldWidth - 100f)
                        val renderY = (renderVector.y + 10f).coerceIn(10f, hudViewport.worldHeight - 10f)
                        message =
                            """SP: $mouseX, $mouseY
                           |WP: ${worldVector.x}, ${worldVector.y}
                           |RTVRR: $realToVirtualResolutionRatio"""
                                .trimMargin()
                        hudFont.draw(batch, message, renderX, renderY + 10f)
                    }
                }
            }
        }
    }

    fun displayDebugInfo(should: Boolean) {
        logger.info { "shouldDisplayDebugInfo = $should" }
        shouldDisplayMouseInfo = should
    }

    fun dispose() { }
}
