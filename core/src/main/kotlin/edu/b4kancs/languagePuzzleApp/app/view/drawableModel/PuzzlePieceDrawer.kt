package edu.b4kancs.languagePuzzleApp.app.view.drawableModel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.utils.Disposable
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece.Companion.BLANK_HEIGHT
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece.Companion.BLANK_WIDTH
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece.Companion.TAB_HEIGHT
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece.Companion.TAB_WIDTH
import edu.b4kancs.languagePuzzleApp.app.model.Side
import ktx.graphics.use
import ktx.inject.Context
import ktx.log.logger

class PuzzlePieceDrawer(
    private val context: Context
) : Disposable {
    private val batch = context.inject<Batch>()
    private val base9Patch: NinePatch
    private val blankTexture: Texture
    private val tabTexture: Texture

    companion object {
        private val logger = logger<PuzzlePieceDrawer>()
        private const val BASE_OFFSET = TAB_HEIGHT
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

            base9Patch.draw(batch, BASE_OFFSET, BASE_OFFSET, puzzlePiece.width, puzzlePiece.height)

            drawBlanks(puzzlePiece)

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            drawTabs(puzzlePiece)
        }
    }

    private fun drawBlanks(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawBlanks" }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ZERO)

        for (blank in puzzlePiece.blanks) {
            val blankX: Float
            val blankY: Float
            val rotation: Float
            val width: Float
            val height: Float
            when (blank.side) {
                Side.TOP -> {
                    width = BLANK_WIDTH
                    height = BLANK_HEIGHT
                    blankX = BASE_OFFSET + puzzlePiece.width / 2 - BLANK_WIDTH / 2
                    blankY = BASE_OFFSET + puzzlePiece.height - BLANK_HEIGHT
                    rotation = 180f
                }

                Side.BOTTOM -> {
                    width = BLANK_WIDTH
                    height = BLANK_HEIGHT
                    blankX = BASE_OFFSET + puzzlePiece.width / 2 - BLANK_WIDTH / 2
                    blankY = BASE_OFFSET
                    rotation = 0f
                }

                Side.LEFT -> {
                    width = BLANK_HEIGHT
                    height = BLANK_WIDTH
                    blankX = BASE_OFFSET + (height - width) / 2
                    blankY = BASE_OFFSET + puzzlePiece.height / 2 - BLANK_HEIGHT / 2
                    rotation = 270f
                }

                Side.RIGHT -> {
                    width = BLANK_HEIGHT
                    height = BLANK_WIDTH
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
                /* width = */ BLANK_WIDTH,
                /* height = */ BLANK_HEIGHT,
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
            batch.color = tab.color ?: puzzlePiece.color
            val tabX: Float
            val tabY: Float
            val width: Float
            val height: Float
            val rotation: Float
            val tabOffset = 10f
            when (tab.side) {
                Side.TOP -> {
                    width = TAB_WIDTH
                    height = TAB_HEIGHT
                    tabX = BASE_OFFSET + puzzlePiece.width / 2 - TAB_WIDTH / 2
                    tabY = BASE_OFFSET + puzzlePiece.height - tabOffset
                    rotation = 0f
                }

                Side.BOTTOM -> {
                    width = TAB_WIDTH
                    height = TAB_HEIGHT
                    tabX = BASE_OFFSET + puzzlePiece.width / 2 - TAB_WIDTH / 2
                    tabY = BASE_OFFSET - TAB_HEIGHT + tabOffset
                    rotation = 180f
                }

                Side.LEFT -> {
                    width = TAB_HEIGHT
                    height = TAB_WIDTH
                    tabX = BASE_OFFSET - width + tabOffset - 6.5f
                    tabY = BASE_OFFSET + puzzlePiece.height / 2 - TAB_HEIGHT / 2 - 13f
                    rotation = 90f
                }

                Side.RIGHT -> {
                    width = TAB_HEIGHT
                    height = TAB_WIDTH
                    tabX = BASE_OFFSET + puzzlePiece.width - tabOffset + 6.5f
                    tabY = BASE_OFFSET + puzzlePiece.height / 2 - TAB_HEIGHT / 2
                    rotation = 270f
                }
            }

            batch.draw(
                /* texture = */ tabTexture,
                /* x = */ tabX,
                /* y = */ tabY,
                /* originX = */ width / 2,
                /* originY = */ height / 2,
                /* width = */ TAB_WIDTH,
                /* height = */ TAB_HEIGHT,
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
