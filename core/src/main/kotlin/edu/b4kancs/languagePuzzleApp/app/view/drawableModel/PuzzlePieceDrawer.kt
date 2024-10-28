package edu.b4kancs.languagePuzzleApp.app.view.drawableModel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole.UNDEFINED
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole.VERB
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleBlank
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.model.Side
import ktx.graphics.use
import ktx.inject.Context
import ktx.log.logger

class PuzzlePieceDrawer(
    context: Context
) : Disposable {
    private val batch = context.inject<Batch>()
    private val font: BitmapFont = context.inject()
    private val base9Patch: NinePatch
    private val blankTexture: Texture
    private val tabTexture: Texture
    private val shapeRenderer = ShapeRenderer()

    private val glowBlankShader: ShaderProgram
    private val glowTabShader: ShaderProgram

    companion object {
        private val logger = logger<PuzzlePieceDrawer>()
        private const val BASE_OFFSET = PuzzleTab.HEIGHT
    }

    init {
        logger.debug { "init" }
        val baseTexture = Texture(Gdx.files.internal("puzzle_base_02.png"))
        base9Patch = NinePatch(baseTexture, 20, 20, 20, 20)

        blankTexture = Texture(Gdx.files.internal("puzzle_blank_06.png"), Pixmap.Format.RGBA8888, true)
        blankTexture.bind()
        blankTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)

        tabTexture = Texture(Gdx.files.internal("puzzle_tab_08.png"), Pixmap.Format.RGBA8888, true)
        tabTexture.bind()
        tabTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)

        // Load shaders
        glowTabShader = ShaderProgram(
            Gdx.files.internal("shaders/glow.vert"),
            Gdx.files.internal("shaders/glow_tab.frag")
        )
        if (!glowTabShader.isCompiled) {
            logger.error { "glowTabShader compilation failed: ${glowTabShader.log}" }
            throw RuntimeException("Shader compilation failed: ${glowTabShader.log}")
        }

        glowBlankShader = ShaderProgram(
            Gdx.files.internal("shaders/glow.vert"),
            Gdx.files.internal("shaders/glow_blank.frag")
        )
        if (!glowBlankShader.isCompiled) {
            logger.error { "glowBlankShader compilation failed: ${glowBlankShader.log}" }
            throw RuntimeException("Shader compilation failed: ${glowBlankShader.log}")
        }
    }

    fun render(puzzlePiece: PuzzlePiece) {
        logger.misc { "render" }

        shapeRenderer.flush()

        batch.use {
            batch.color = calculatePuzzleColor(puzzlePiece)

//            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            base9Patch.draw(batch, BASE_OFFSET, BASE_OFFSET, puzzlePiece.width, puzzlePiece.height)

            drawTextOnPuzzle(puzzlePiece)

            drawBlanks(puzzlePiece)

            drawTabs(puzzlePiece)
        }
    }

    private fun drawTextOnPuzzle(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawText" }

        // Draw the text centered on the puzzle piece
        font.color = Color.BLACK
        val text = puzzlePiece.text + " (${puzzlePiece.connections.size})"
        if (text.isNotEmpty()) {
//            val maxTextWidth = puzzlePiece.boundingBoxSize.first - 20f // 10f padding on each side

            val leftBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.LEFT }) PuzzleBlank.WIDTH * 0.75f else 0f
            val rightBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.RIGHT }) PuzzleBlank.WIDTH * 0.75f else 0f
            val topBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.TOP }) PuzzleBlank.HEIGHT * 1.2f else 0f
            val bottomBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.BOTTOM }) PuzzleBlank.HEIGHT * 1.2f else 0f

            val maxLayoutWidth = puzzlePiece.width - leftBlankOffset - rightBlankOffset - 20f
            val maxLayoutHeight = puzzlePiece.height - topBlankOffset - bottomBlankOffset - 20f

            val layout = GlyphLayout().apply {
                setText(font, text, Color.BLACK, maxLayoutWidth, Align.center, true)
            }

            if (layout.height > maxLayoutHeight) {
                puzzlePiece.height += 4f
                puzzlePiece.pos.x -= 2f
                puzzlePiece.width += 4f
                puzzlePiece.pos.y -= 2f
            }

            // Calculate positions to center the text
            val textX = BASE_OFFSET + 10f + leftBlankOffset
            val textY = BASE_OFFSET - 5f + (puzzlePiece.height - layout.height) / 2 - bottomBlankOffset / 4

            font.draw(batch, layout, textX, textY)

//            batch.end()
//
//            shapeRenderer.projectionMatrix = batch.projectionMatrix  // Match the projection matrix
//            shapeRenderer.transformMatrix = batch.transformMatrix    // Match the transform matrix
//
//            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
//            shapeRenderer.color = Color.RED  // Choose a color for the border
//
//            // Calculate the rectangle around the text
//            // Note: In LibGDX, y increases upwards, and BitmapFont.draw uses the baseline for y
//            shapeRenderer.rect(textX, textY - maxLayoutHeight / 2, maxLayoutWidth, maxLayoutHeight)
//            shapeRenderer.end()
//
//            batch.begin()  // Restart the SpriteBatch for further rendering
        }
    }

    private fun drawBlanks(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawBlanks" }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ZERO)

        for (blank in puzzlePiece.blanks) {
            batch.color = calculatePuzzleColor(puzzlePiece)

//            if (blank.isGlowing) {
//                batch.shader = glowBlankShader
//                glowTabShader.bind()
//                glowTabShader.setUniformf("u_glowColor", Color.FIREBRICK)
//                glowTabShader.setUniformf("u_glowIntensity", 0.3f)
//                glowTabShader.setUniformf("u_resolution", PuzzleBlank.WIDTH)
//                batch.color = Color.WHITE
//            }

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

            @Suppress("InconsistentCommentForJavaParameter")
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

            batch.shader = null
            batch.color = Color.WHITE
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawTabs(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawBlanks" }

        batch.setBlendFunctionSeparate(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR)

        for (tab in puzzlePiece.tabs) {
            batch.color = tab.grammaticalRole.color

            if (tab.isGlowing) {
                batch.shader = glowTabShader
                glowTabShader.bind()
                glowTabShader.setUniformf("u_glowColor", Color.TEAL)
                glowTabShader.setUniformf("u_glowIntensity", 0.05f) // Adjustable
                glowTabShader.setUniformf("u_resolution", PuzzleTab.WIDTH)
            }

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

            @Suppress("InconsistentCommentForJavaParameter")
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

            if (tab.text.isNotEmpty()) {
                drawTextOnTab(tab.text, Vector2(tabX, tabY))
            }

            batch.color = Color.WHITE

            batch.shader = null
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawTextOnTab(text: String, pos: Vector2) {
        logger.misc { "drawText" }

        font.color = Color.BLACK

        val layout = GlyphLayout(font, text)
        val textX = pos.x + (BASE_OFFSET - layout.width) / 2
        val textY = pos.y + (BASE_OFFSET - layout.height) / 2
        font.draw(batch, text, textX, textY)
    }

    private fun calculatePuzzleColor(puzzlePiece: PuzzlePiece): Color {
        return puzzlePiece.let {
            if (it.grammaticalRole != UNDEFINED) {
                it.grammaticalRole.color
            }
            else {
                it.connections.firstOrNull { c -> c.puzzlesConnected.any { p -> p.grammaticalRole == VERB } }?.via?.grammaticalRole?.color
                    ?: UNDEFINED.color
            }
        }

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
