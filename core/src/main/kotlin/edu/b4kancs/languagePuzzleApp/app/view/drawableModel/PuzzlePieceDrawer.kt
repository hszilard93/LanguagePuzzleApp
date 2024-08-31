package edu.b4kancs.languagePuzzleApp.app.view.drawableModel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.Side
import ktx.graphics.use
import ktx.inject.Context
import ktx.log.logger

class PuzzlePieceDrawer(
    private val context: Context
) {
    private val batch = context.inject<Batch>()
    private val base9Patch: NinePatch
    private val blankTexture: Texture
    private val tabTexture: Texture

    companion object {
        private val logger = logger<PuzzlePieceDrawer>()
        private const val BLANK_WIDTH = 120f
        private const val BLANK_HEIGHT = BLANK_WIDTH * 0.75f
        private const val TAB_WIDTH = 110f
        private const val TAB_HEIGHT = TAB_WIDTH * 0.88f
    }

    init {
        logger.debug { "init" }
        val baseTexture = Texture(Gdx.files.internal("puzzle_base_02.png"))
        base9Patch = NinePatch(baseTexture, 20, 20, 20, 20)

        blankTexture = Texture(Gdx.files.internal("puzzle_blank_03.png"), Pixmap.Format.RGBA8888, true)
        tabTexture = Texture(Gdx.files.internal("puzzle_tab_03_noalpha.png"), Pixmap.Format.RGBA8888, true)
//        blankTexture.bind()
//        tabTexture.bind()
        blankTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        tabTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }

    fun render(puzzlePiece: PuzzlePiece) {
        logger.misc { "render" }

        val defaultShader = batch.shader

        batch.use {

            batch.color = puzzlePiece.color

            base9Patch.draw(batch, puzzlePiece.pos.x, puzzlePiece.pos.y, puzzlePiece.width, puzzlePiece.height)

            drawBlanks(puzzlePiece)

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            drawTabs(puzzlePiece)
        }
        batch.shader = defaultShader
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
                    blankX = puzzlePiece.pos.x + puzzlePiece.width / 2 - BLANK_WIDTH / 2
                    blankY = puzzlePiece.pos.y + puzzlePiece.height - BLANK_HEIGHT
                    rotation = 180f
                }
                Side.BOTTOM -> {
                    width = BLANK_WIDTH
                    height = BLANK_HEIGHT
                    blankX = puzzlePiece.pos.x + puzzlePiece.width / 2 - BLANK_WIDTH / 2
                    blankY = puzzlePiece.pos.y
                    rotation = 0f
                }
                Side.LEFT -> {
                    width = BLANK_HEIGHT
                    height = BLANK_WIDTH
                    blankX = puzzlePiece.pos.x + (height - width) / 2
                    blankY = puzzlePiece.pos.y + puzzlePiece.height / 2 - BLANK_HEIGHT / 2
                    rotation = 270f
                }
                Side.RIGHT -> {
                    width = BLANK_HEIGHT
                    height = BLANK_WIDTH
                    blankX = puzzlePiece.pos.x + puzzlePiece.width - (width + height) / 2
                    blankY = puzzlePiece.pos.y + (puzzlePiece.height / 2) - (height / 2) - (height - width) / 2
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
                    tabX = puzzlePiece.pos.x + puzzlePiece.width / 2 - TAB_WIDTH / 2
                    tabY = puzzlePiece.pos.y + puzzlePiece.height - tabOffset
                    rotation = 0f
                }
                Side.BOTTOM -> {
                    width = TAB_WIDTH
                    height = TAB_HEIGHT
                    tabX = puzzlePiece.pos.x + puzzlePiece.width / 2 - TAB_WIDTH / 2
                    tabY = puzzlePiece.pos.y - TAB_HEIGHT + tabOffset
                    rotation = 180f
                }
                Side.LEFT -> {
                    width = TAB_HEIGHT
                    height = TAB_WIDTH
                    tabX = puzzlePiece.pos.x - width + tabOffset - 7
                    tabY = puzzlePiece.pos.y + puzzlePiece.height / 2 - TAB_HEIGHT / 2
                    rotation = 90f
                }
                Side.RIGHT -> {
                    width = TAB_HEIGHT
                    height = TAB_WIDTH
                    tabX = puzzlePiece.pos.x + puzzlePiece.width - tabOffset + 7
                    tabY = puzzlePiece.pos.y + puzzlePiece.height / 2 - TAB_HEIGHT / 2
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
    }
}
