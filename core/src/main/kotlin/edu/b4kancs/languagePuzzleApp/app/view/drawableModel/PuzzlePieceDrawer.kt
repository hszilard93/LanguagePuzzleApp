package edu.b4kancs.languagePuzzleApp.app.view.drawableModel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleBlank
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.model.Side
import ktx.graphics.use
import ktx.inject.Context
import ktx.log.logger
import space.earlygrey.shapedrawer.ShapeDrawer

class PuzzlePieceDrawer(
    private val context: Context
) : Disposable {
    private val batch = context.inject<Batch>()
    private val base9Patch: NinePatch
    private val blankTexture: Texture
    private val tabTexture: Texture

    companion object {
        private val logger = logger<PuzzlePieceDrawer>()
        private const val BASE_OFFSET = PuzzleTab.HEIGHT
    }

    init {
        logger.debug { "init" }
        val baseTexture = Texture(Gdx.files.internal("puzzle_base_02.png"))
        base9Patch = NinePatch(baseTexture, 20, 20, 20, 20)

        blankTexture = Texture(Gdx.files.internal("puzzle_blank_03.png"), Pixmap.Format.RGBA8888, true)
        blankTexture.bind()
        blankTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)

        tabTexture = Texture(Gdx.files.internal("puzzle_tab_07.png"), Pixmap.Format.RGBA8888, true)
        tabTexture.bind()
        tabTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }

    fun render(puzzlePiece: PuzzlePiece) {
        logger.misc { "render" }

        batch.use {
            batch.color = puzzlePiece.color

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            base9Patch.draw(batch, BASE_OFFSET, BASE_OFFSET, puzzlePiece.width, puzzlePiece.height)

            drawBlanks(puzzlePiece)

            drawTabs(puzzlePiece)
        }
    }

    private fun drawBlanks(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawBlanks" }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ZERO)

        for (blank in puzzlePiece.blanks) {
            batch.color = blank.glowColor ?: puzzlePiece.color

            val blankX: Float
            val blankY: Float
            val rotation: Float
            val width: Float
            val height: Float
            when (blank.side) {
                Side.TOP -> {
                    width = PuzzleBlank.WIDTH
                    height = PuzzleBlank.HEIGHT
                    blankX = BASE_OFFSET + puzzlePiece.width / 2 - PuzzleBlank.WIDTH / 2
                    blankY = BASE_OFFSET
                    rotation = 0f
                }

                Side.BOTTOM -> {
                    width = PuzzleBlank.WIDTH
                    height = PuzzleBlank.HEIGHT
                    blankX = BASE_OFFSET + puzzlePiece.width / 2 - PuzzleBlank.WIDTH / 2
                    blankY = BASE_OFFSET + puzzlePiece.height - PuzzleBlank.HEIGHT
                    rotation = 180f
                }

                Side.LEFT -> {
                    width = PuzzleBlank.HEIGHT
                    height = PuzzleBlank.WIDTH
                    blankX = BASE_OFFSET + (height - width) / 2
                    blankY = BASE_OFFSET + puzzlePiece.height / 2 - PuzzleBlank.HEIGHT / 2
                    rotation = 270f
                }

                Side.RIGHT -> {
                    width = PuzzleBlank.HEIGHT
                    height = PuzzleBlank.WIDTH
                    blankX = BASE_OFFSET + puzzlePiece.width - (width + height) / 2
                    blankY = BASE_OFFSET + (puzzlePiece.height / 2) - (height / 2) - (height - width) / 2
                    rotation = 90f
                }
            }

            batch.draw(
                /* texture = */ blankTexture,
                /* x = */ blankX,
                /* y = */ blankY,
                /* originX = */ width / 2,
                /* originY = */ height / 2,
                /* width = */ PuzzleBlank.WIDTH,
                /* height = */ PuzzleBlank.HEIGHT,
                /* scaleX = */ 1f,
                /* scaleY = */ 1f,
                /* rotation = */ rotation,
                /* srcX = */ 0,
                /* srcY = */ 0,
                /* srcWidth = */ blankTexture.width,
                /* srcHeight = */ blankTexture.height,
                /* flipX = */ false,
                /* flipY = */ false
            )
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawTabs(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawBlanks" }

        batch.setBlendFunctionSeparate(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR)

        for (tab in puzzlePiece.tabs) {
//            batch.color = tab.glowColor ?: (tab.color ?: puzzlePiece.color)
            batch.color = tab.color ?: puzzlePiece.color

            val tabX: Float
            val tabY: Float
            val width: Float
            val height: Float
            val rotation: Float
            val tabOffset = 10f
            when (tab.side) {
                Side.TOP -> {
                    width = PuzzleTab.WIDTH
                    height = PuzzleTab.HEIGHT
                    tabX = BASE_OFFSET + puzzlePiece.width / 2 - PuzzleTab.WIDTH / 2
                    tabY = BASE_OFFSET - PuzzleTab.HEIGHT + tabOffset
                    rotation = 180f
                }

                Side.BOTTOM -> {
                    width = PuzzleTab.WIDTH
                    height = PuzzleTab.HEIGHT
                    tabX = BASE_OFFSET + puzzlePiece.width / 2 - PuzzleTab.WIDTH / 2
                    tabY = BASE_OFFSET + puzzlePiece.height - tabOffset
                    rotation = 0f
                }

                Side.LEFT -> {
                    width = PuzzleTab.HEIGHT
                    height = PuzzleTab.WIDTH
                    tabX = BASE_OFFSET - width + tabOffset - 6.5f
                    tabY = BASE_OFFSET + puzzlePiece.height / 2 - PuzzleTab.HEIGHT / 2 - 13f
                    rotation = 90f
                }

                Side.RIGHT -> {
                    width = PuzzleTab.HEIGHT
                    height = PuzzleTab.WIDTH
                    tabX = BASE_OFFSET + puzzlePiece.width - tabOffset + 6.5f
                    tabY = BASE_OFFSET + puzzlePiece.height / 2 - PuzzleTab.HEIGHT / 2
                    rotation = 270f
                }
            }

            batch.draw(
                /* texture = */ tabTexture,
                /* x = */ tabX,
                /* y = */ tabY,
                /* originX = */ width / 2,
                /* originY = */ height / 2,
                /* width = */ PuzzleTab.WIDTH,
                /* height = */ PuzzleTab.HEIGHT,
                /* scaleX = */ 1f,
                /* scaleY = */ 1f,
                /* rotation = */ rotation,
                /* srcX = */ 0,
                /* srcY = */ 0,
                /* srcWidth = */ tabTexture.width,
                /* srcHeight = */ tabTexture.height,
                /* flipX = */ false,
                /* flipY = */ false
            )
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    // For debugging only
    private fun drawCrosshair(batch: Batch, center: Vector2, color: Color = Color.WHITE) {
        // Strongly discouraged: Creating a ShapeRenderer here is inefficient!
        val shapeRenderer = ShapeRenderer()

        // Set the projection matrix based on the batch's transform matrix. Important!
//        shapeRenderer.projectionMatrix = batch.projectionMatrix
//        shapeRenderer.transformMatrix = batch.transformMatrix
//        shapeRenderer.updateMatrices()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = color

        val x = center.x
        val y = center.y
        val halfSize = 10f

        shapeRenderer.line(x - halfSize, y, x + halfSize, y)
        shapeRenderer.line(x, y - halfSize, x, y + halfSize)

        shapeRenderer.end()

        // Dispose immediately – essential to prevent resource leaks.
        shapeRenderer.dispose()
    }

    override fun dispose() {
        logger.debug { "dispose" }

        base9Patch.texture.dispose()
        blankTexture.dispose()
        tabTexture.dispose()
    }

//    private fun loadAndPremultiplyTexture(path: String): Texture {
//        val pixmap = Pixmap(Gdx.files.internal(path))
//        val texture = Texture(pixmap, true) // true enables mipmaps
//        pixmap.dispose()
//        return texture
//    }
//
//    private fun premultiplyPixmap(pixmap: Pixmap) {
//        val pixels = pixmap.pixels
//        val numPixels = pixmap.width * pixmap.height
//        for (i in 0 until numPixels) {
//            val color = Color(pixels.getInt(i * 4))
//            val alpha = color.a
//            val newColor = Color.rgba8888(color.r * alpha, color.g * alpha, color.b * alpha, color.a)
//            pixels.putInt(i * 4, newColor)
//        }
//    }
//
//    private fun loadAndProcessTexture(path: String): Texture {
//        val unprocessedTexture = Texture(Gdx.files.internal(path))
//        unprocessedTexture.textureData.prepare()
//        val pixmap = unprocessedTexture.textureData.consumePixmap()
////        processPixmap(pixmap)
//        val processedTexture = Texture(pixmap, true)
//        processedTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
//        pixmap.dispose()
//        return processedTexture
//    }
//
//    private fun processPixmap(pixmap: Pixmap) {
//        val pixels = pixmap.pixels
//        for (i in 0 until pixmap.width * pixmap.height) {
//            val color = Color(pixels.getInt(i * 4))
//            if (color.a < 1.0 && color.a > 0.0) {
//                val newColor = Color.rgba8888(255f, 255f, 255f, 1f)
//                pixels.putInt(i * 4, newColor)
//            }
//        }
}
