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

data class BaseTextLayoutKey(
    val text: String,
    val blankPositions: Set<Side>
)

data class TabTextLayoutKey(
    val text: String,
    val tabPosX: Float,
    val tabPosY: Float
)

data class TextLayoutData(
    val layout: GlyphLayout,
    val layoutX: Float,
    val layoutY: Float
)

class PuzzlePieceDrawer(
    context: Context
) : Disposable {
    private val batch = context.inject<Batch>()
    private val font: BitmapFont = context.inject()
    private val base9Patch: NinePatch
    private val blankTexture: Texture
    private val tabTexture: Texture
//    private val shapeRenderer = ShapeRenderer()

//    private val glowBlankShader: ShaderProgram
    private val glowTabShader: ShaderProgram

    // Caches for GlyphLayouts and their positions
    private val textLayoutCache = mutableMapOf<BaseTextLayoutKey, TextLayoutData>()
    private val tabTextLayoutCache = mutableMapOf<TabTextLayoutKey, TextLayoutData>()


    companion object {
        private val logger = logger<PuzzlePieceDrawer>()

        /* The PuzzlePiece being drawn includes not just the base of the puzzle but also the tabs.
        The BASE_OFFSET is the amount by which the puzzle base is offset from the edges of the texture being drawn,
        i.e. the space the tabs can take up. */
        private const val BASE_OFFSET = PuzzleTab.HEIGHT
    }

    init {
        logger.debug { "init" }
        val baseTexture = Texture(Gdx.files.internal("puzzle_base_02.png"))
        base9Patch = NinePatch(baseTexture, 20, 20, 20, 20)

        blankTexture = Texture(Gdx.files.internal("puzzle_blank_alt_03.png"), Pixmap.Format.RGBA8888, true)
        blankTexture.bind()
        blankTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)

        tabTexture = Texture(Gdx.files.internal("puzzle_tab_alt_06.png"), Pixmap.Format.RGBA8888, true)
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

//        glowBlankShader = ShaderProgram(
//            Gdx.files.internal("shaders/glow.vert"),
//            Gdx.files.internal("shaders/glow_blank.frag")
//        )
//        if (!glowBlankShader.isCompiled) {
//            logger.error { "glowBlankShader compilation failed: ${glowBlankShader.log}" }
//            throw RuntimeException("Shader compilation failed: ${glowBlankShader.log}")
//        }
    }

    fun render(puzzlePiece: PuzzlePiece) {
        logger.misc { "render puzzlePiece.text=\"${puzzlePiece.text}\"" }

//        shapeRenderer.flush()

        batch.use {
            batch.color = calculatePuzzleColor(puzzlePiece)

            base9Patch.draw(batch, BASE_OFFSET, BASE_OFFSET, puzzlePiece.size, puzzlePiece.size)

            drawBlanks(puzzlePiece)

            drawTabs(puzzlePiece)

            drawTextOnPuzzle(puzzlePiece)
        }
    }

    private fun drawTextOnPuzzle(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawText" }

        val text = puzzlePiece.text + " (${puzzlePiece.connections.size})"
        if (text.isEmpty()) return

        val blanks = puzzlePiece.blanks.map { it.side }.toSet()
        val key = BaseTextLayoutKey(text, blanks)
        // Attempt to retrieve cached layout data
        val cachedData = textLayoutCache[key]

        if (cachedData != null) {
            logger.misc { "Reusing layout data for key: $key" }
            // Use cached layout and positions
            font.draw(batch, cachedData.layout, cachedData.layoutX, cachedData.layoutY)
        } else {
            logger.debug { "Creating new layout data for key: $key" }
            // Calculate offsets based on blanks
            val leftBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.LEFT }) PuzzleBlank.WIDTH * 0.5f else 0f
            val rightBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.RIGHT }) PuzzleBlank.WIDTH * 0.5f else 0f
            val topBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.TOP }) PuzzleBlank.HEIGHT * 0.5f else 0f
            val bottomBlankOffset = if (puzzlePiece.blanks.any { it.side == Side.BOTTOM }) PuzzleBlank.HEIGHT * 0.5f else 0f

            val maxLayoutWidth = puzzlePiece.size - leftBlankOffset - rightBlankOffset - 20f
            val maxLayoutHeight = puzzlePiece.size - topBlankOffset - bottomBlankOffset - 20f

            // Create a new GlyphLayout
            val layout = GlyphLayout().apply {
                setText(font, text, Color.BLACK, maxLayoutWidth, Align.left, true)
            }

            // Adjust puzzle piece size if necessary (Consider moving this logic outside the render loop)
            if (layout.height > maxLayoutHeight) {
                puzzlePiece.size += 4f
                puzzlePiece.pos.x -= 2f
                puzzlePiece.pos.y -= 2f
            } else if (
                puzzlePiece.size > PuzzlePiece.MIN_SIZE
                && maxLayoutHeight - layout.height > 40f
                && !puzzlePiece.isConnected()
            ) {
                puzzlePiece.size -= 4f
                puzzlePiece.pos.x += 2f
                puzzlePiece.pos.y += 2f
            }

            val verticalPaddingForSmallText =
                if (maxLayoutHeight - layout.height > 80f) {
                    if (topBlankOffset != 0f && bottomBlankOffset == 0f) -20f
                    else if (topBlankOffset == 0f && bottomBlankOffset != 0f) 20f
                    else 0f
                } else 0f

            // Calculate positions to center the text
            val layoutX = BASE_OFFSET + (puzzlePiece.size - leftBlankOffset - rightBlankOffset) / 2 -
                layout.width / 2 + leftBlankOffset
            val layoutY = BASE_OFFSET + (puzzlePiece.size - topBlankOffset - bottomBlankOffset) / 2 -
                layout.height / 2 + topBlankOffset + verticalPaddingForSmallText

            // Cache the layout and positions
            val layoutData = TextLayoutData(layout, layoutX, layoutY)
            textLayoutCache[key] = layoutData

            // Draw the text
            font.draw(batch, layout, layoutX, layoutY)

            // Optional: Log caching action
            logger.debug { "Cached layout for key: $key" }
        }
    }

    private fun drawBlanks(puzzlePiece: PuzzlePiece) {
        logger.misc { "drawBlanks" }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ZERO)

        for (blank in puzzlePiece.blanks) {
            batch.color = calculatePuzzleColor(puzzlePiece)

            val blankX: Float
            val blankY: Float
            val rotation: Float
            val width: Float
            val height: Float
            when (blank.side) {
                Side.TOP -> {
                    width = PuzzleBlank.WIDTH
                    height = PuzzleBlank.HEIGHT
                    blankX = BASE_OFFSET + puzzlePiece.size / 2 - PuzzleBlank.WIDTH / 2
                    blankY = BASE_OFFSET
                    rotation = 0f
                }

                Side.BOTTOM -> {
                    width = PuzzleBlank.WIDTH
                    height = PuzzleBlank.HEIGHT
                    blankX = BASE_OFFSET + puzzlePiece.size / 2 - PuzzleBlank.WIDTH / 2
                    blankY = BASE_OFFSET + puzzlePiece.size - PuzzleBlank.HEIGHT
                    rotation = 180f
                }

                Side.LEFT -> {
                    width = PuzzleBlank.HEIGHT
                    height = PuzzleBlank.WIDTH
                    blankX = BASE_OFFSET + (height - width) / 2
                    blankY = BASE_OFFSET + puzzlePiece.size / 2 - PuzzleBlank.HEIGHT / 2
                    rotation = 270f
                }

                Side.RIGHT -> {
                    width = PuzzleBlank.HEIGHT
                    height = PuzzleBlank.WIDTH
                    blankX = BASE_OFFSET + puzzlePiece.size - (width + height) / 2
                    blankY = BASE_OFFSET + (puzzlePiece.size / 2) - (height / 2) - (height - width) / 2
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

            batch.color = Color.WHITE
        }

//        Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD)
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
                    tabX = BASE_OFFSET + puzzlePiece.size / 2 - PuzzleTab.WIDTH / 2
                    tabY = BASE_OFFSET - PuzzleTab.HEIGHT + tabOffset
                    rotation = 180f
                }

                Side.BOTTOM -> {
                    width = PuzzleTab.WIDTH
                    height = PuzzleTab.HEIGHT
                    tabX = BASE_OFFSET + puzzlePiece.size / 2 - PuzzleTab.WIDTH / 2
                    tabY = BASE_OFFSET + puzzlePiece.size - tabOffset
                    rotation = 0f
                }

                Side.LEFT -> {
                    width = PuzzleTab.HEIGHT
                    height = PuzzleTab.WIDTH
                    tabX = BASE_OFFSET - width + tabOffset
                    tabY = BASE_OFFSET + puzzlePiece.size / 2 - PuzzleTab.HEIGHT / 2
                    rotation = 90f
                }

                Side.RIGHT -> {
                    width = PuzzleTab.HEIGHT
                    height = PuzzleTab.WIDTH
                    tabX = BASE_OFFSET + puzzlePiece.size - tabOffset
                    tabY = BASE_OFFSET + puzzlePiece.size / 2 - PuzzleTab.HEIGHT / 2
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
                drawTextOnTab(tab.text, Vector2(tabX, tabY), tab.side)
            }

            batch.color = Color.WHITE
            batch.shader = null
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawTextOnTab(text: String, tabPos: Vector2, side: Side) {
        logger.misc { "drawTextOnTab" }

        font.color = Color.BLACK

        val roundedX = tabPos.x
        val roundedY = tabPos.y

        // Create cache key
        val key = TabTextLayoutKey(text, roundedX, roundedY)

        // Check if the layout data is already cached
        val cachedData = tabTextLayoutCache[key]

        if (cachedData != null) {
            // Use cached layout and positions
            logger.misc { "Using cached layout data for tab $key" }
            font.draw(batch, cachedData.layout, cachedData.layoutX, cachedData.layoutY)
        } else {
            logger.debug { "Creating new layout data for tab $key" }

            // Create a new GlyphLayout
            val layout = GlyphLayout().apply {
                setText(font, text, Color.BLACK, PuzzleTab.WIDTH, Align.left, false)
            }

            // Calculate offsets based on the tab's side
            val xOffset: Float
            val yOffset: Float

            when (side) {
                Side.TOP -> {
                    xOffset = PuzzleTab.WIDTH / 2 - layout.width / 2
                    yOffset = (PuzzleTab.HEIGHT * 0.75f) - layout.height / 2 - 16f
                }

                Side.BOTTOM -> {
                    xOffset = PuzzleTab.WIDTH / 2 - layout.width / 2
                    yOffset = PuzzleTab.HEIGHT * 0.25f - layout.height / 2 + 8f
                }

                Side.LEFT -> {
                    xOffset = (PuzzleTab.HEIGHT * 0.75f) - layout.width / 2 - 8f
                    yOffset = PuzzleTab.WIDTH / 2 - layout.height / 2
                }

                Side.RIGHT -> {
                    xOffset = PuzzleTab.HEIGHT * 0.25f - layout.width / 2 + 8f
                    yOffset = PuzzleTab.WIDTH / 2 - layout.height / 2
                }
            }

            // Calculate final positions
            val layoutX = tabPos.x + xOffset
            val layoutY = tabPos.y + yOffset

            // Cache the layout data
            val layoutData = TextLayoutData(layout, layoutX, layoutY)
            tabTextLayoutCache[key] = layoutData

            // Draw the text
            font.draw(batch, layout, layoutX, layoutY)

            // Optional: Log caching action
            logger.debug { "Cached layout for key: $key" }
        }
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
//    private fun drawGlyphLayoutDebugBounds(layout: GlyphLayout, pos: Vector2) {
//        batch.end()
//
//        shapeRenderer.projectionMatrix = batch.projectionMatrix  // Match the projection matrix
//        shapeRenderer.transformMatrix = batch.transformMatrix    // Match the transform matrix
//
//        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
//        shapeRenderer.color = Color.RED  // Choose a color for the border
//
//        // Calculate the rectangle around the text
//        // Note: In LibGDX, y increases upwards, and BitmapFont.draw uses the baseline for y
//        shapeRenderer.rect(pos.x, pos.y, layout.width, layout.height)
//        shapeRenderer.end()
//
//        batch.begin()
//    }

    // For debugging only
    private fun drawCrosshair(batch: Batch, center: Vector2, color: Color = Color.WHITE) {
        // Just for debugging. Creating a ShapeRenderer here is inefficient!
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
        shapeRenderer.dispose()
    }

    override fun dispose() {
        logger.debug { "dispose" }

        base9Patch.texture.dispose()
        blankTexture.dispose()
        tabTexture.dispose()

        glowTabShader.dispose()
//        glowBlankShader.dispose()
    }
}
