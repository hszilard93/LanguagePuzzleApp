package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FillViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawer
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import ktx.app.KtxScreen
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf
import ktx.graphics.use
import ktx.inject.Context
import ktx.log.logger
import space.earlygrey.shapedrawer.ShapeDrawer


class GameScreen(
    private val context: Context,
    private val batch: Batch,
    private val assetManager: AssetManager,
    private val gameViewport: FillViewport,
    private val hudViewport: FitViewport,
    private val gameCamera: GameCamera,
    private val hudCamera: HudCamera,
    private val gameModel: GameModel
) : KtxScreen, BaseInputProcessor() {

    companion object {
        val logger = logger<GameScreen>()
    }

    private lateinit var localDrawer: ShapeDrawer
    private lateinit var font: BitmapFont
    //    private val puzzleDrawable = PuzzlePieceDrawable(context, PuzzlePiece())
    private lateinit var frameBufferPool: GdxArray<FrameBuffer>
    private lateinit var frameRegionPool: GdxArray<TextureRegion>

    private val puzzlePieceDrawer = PuzzlePieceDrawer(context)
    private val minZoom = 0.9f
    private val maxZoom = 2f
    private var shouldDisplayDebugInfo = false


    override fun show() {
        logger.debug { "show" }

        Gdx.input.inputProcessor = this

        frameBufferPool = gdxArrayOf<FrameBuffer>(true)
        frameRegionPool = gdxArrayOf<TextureRegion>(true)

        for (i in 0 until 3) {  // Start with 3 default layers. Will be enough in most cases.
            val frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, gameViewport.worldWidth.toInt(), gameViewport.worldHeight.toInt(), false)
            frameBufferPool.add(frameBuffer)
            val frameRegion = TextureRegion(frameBuffer.colorBufferTexture, gameViewport.screenWidth, gameViewport.screenHeight)
            frameRegion.flip(false, true)
            frameRegionPool.add(frameRegion)
        }

//        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, gameViewport.worldWidth.toInt(), gameViewport.worldHeight.toInt(), false)

        gameCamera.apply {
            setToOrtho(false)
            zoom = 1f
        }

        font = BitmapFont(Gdx.files.internal("hud_font.fnt"))
//        assetManager.finishLoading()

        super.show()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }

        gameViewport.update(newWidth, newHeight, true)

        hudViewport.update(newWidth, newHeight, true)

        if (::frameBufferPool.isInitialized) {
            for (frameBuffer in frameBufferPool) {
//                updateFrameBuffer(frameBuffer)
            }
        }
    }

    override fun render(delta: Float) {
        logger.misc { "render delta=$delta" }

        gameCamera.update()
        setBackgroundColor(180, 255, 180, 1f)

        batch.use {
            gameViewport.apply()
            it.projectionMatrix = gameCamera.combined
        }

        val puzzles = gameModel.puzzlePieces
        if (puzzles.isNotEmpty()) {

            val puzzlesByLayers = gameModel.puzzlePieces.groupBy { it.depth }

            if (puzzlesByLayers.keys.max() > frameBufferPool.size) {
                logger.error { "NEED TO CREATE DYNAMIC FRAMEBUFFER POOL!" }
            }

            for (layerI in 0 until frameBufferPool.size) {
                puzzlesByLayers[layerI]?.forEach { puzzle ->
                    frameBufferPool[layerI].use {
                        puzzlePieceDrawer.render(puzzle)
                    }
                }

                batch.use {
                    val frameRegion = frameRegionPool[layerI]
                    it.draw(frameRegion, 0f, 0f, frameRegion.regionWidth.toFloat(), frameRegion.regionHeight.toFloat())
                }
            }
        }

        batch.use {
            renderHud()
        }
    }

    private fun renderHud() {
        logger.misc { "renderHud" }

        // Use the HUD camera to render the text
        hudViewport.apply()
        batch.projectionMatrix = hudCamera.combined

        font.draw(batch, "FPS=" + Gdx.graphics.framesPerSecond, 0f, hudCamera.viewportHeight)
        font.draw(batch, "Lower left", 0f, font.lineHeight)
        font.draw(batch, "Zoom: %.1f".format(gameCamera.zoom), hudCamera.viewportWidth - 75f, hudCamera.viewportHeight)

        if (Game.isDebugModeOn && shouldDisplayDebugInfo) {
            // Render the mouse position interpreted as real pixel coordinates within the viewport ("0, 0" is the viewports bottom left corner)
            val screenHeight = Gdx.graphics.height.toFloat()
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = Gdx.input.y.toFloat()
            val mouseViewportX = mouseX - hudViewport.leftGutterWidth
            val mouseViewportY = screenHeight - mouseY - hudViewport.bottomGutterHeight
            val renderVector = hudViewport.unproject(Vector2(mouseX, mouseY))
            if (hudViewport.screenWidth >= 200) {  // Catches errors when the viewport is too small or not visible (e.g. window is minimized)
                val renderX = (renderVector.x + 10f).coerceIn(10f, hudViewport.worldWidth - 100f)
                val renderY = (renderVector.y + 10f).coerceIn(10f, hudViewport.worldHeight - 10f)
                font.draw(batch, "$mouseViewportX, $mouseViewportY", renderX, renderY + 10f)
            }
        }
    }

    private fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Float) {
        logger.misc { "setBackgroundColor red=$red green=$green blue=$blue alpha=$alpha" }
        Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    private fun createDrawer(): ShapeDrawer {
        logger.debug { "createDrawer" }

        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.drawPixel(0, 0)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val textureRegion = TextureRegion(texture, 0, 0, 1, 1)
        return ShapeDrawer(batch, textureRegion)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        logger.debug { "touchDown button=$button" }

        if (button == Input.Buttons.RIGHT) {
            shouldDisplayDebugInfo = !shouldDisplayDebugInfo
            logger.info { "displayDebugInfo = $shouldDisplayDebugInfo" }
        }
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        logger.debug { "keyDown keycode=$keycode" }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        // Get Mouse Position in World Coordinates BEFORE Zoom
        val mouseWorldPosBefore = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        gameCamera.unproject(mouseWorldPosBefore)

        // Apply Zoom
        val newZoom: Float = (gameCamera.zoom + amountY * 0.1f).coerceIn(minZoom, maxZoom) // Use clamp for cleaner limits
        gameCamera.zoom = newZoom
        gameCamera.update()

        // Get Mouse Position in World Coordinates AFTER Zoom
        val mouseWorldPosAfter = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        gameCamera.unproject(mouseWorldPosAfter)

        // Calculate Camera Offset
        val offsetX = mouseWorldPosAfter.x - mouseWorldPosBefore.x
        val offsetY = mouseWorldPosAfter.y - mouseWorldPosBefore.y

        // Move the Camera
        gameCamera.translate(-offsetX, -offsetY, 0f)
        gameCamera.update()

        logger.debug { "gameCamera.viewportWidth ${gameCamera.viewportWidth}; gameCamera.viewportHeight ${gameCamera.viewportHeight}" }

//        updateFrameBuffer()

        return true
    }

//    private fun updateFrameBuffer(frameBuffer: FrameBuffer) {
//        logger.debug { "updateFrameBuffer" }
//
//        // Calculate Visible World Width & Height
//        val visibleWorldWidth = (gameCamera.viewportWidth * gameCamera.zoom).toInt()
//        val visibleWorldHeight = (gameCamera.viewportHeight * gameCamera.zoom).toInt()
//        logger.debug { "visibleWorldWidth=$visibleWorldWidth visibleWorldHeight=$visibleWorldHeight" }
//
//        // Dispose of the old Framebuffer (if there is one)
//        if (::frameBuffer.isInitialized) frameBuffer.dispose()
//
//        // Create a new Framebuffer with the calculated size
//        logger.debug { "visibleWorldWidth=$visibleWorldWidth visibleWorldHeight=$visibleWorldHeight" }
//        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, visibleWorldWidth, visibleWorldHeight, false)
//
//        // Update the TextureRegion
//        frameRegion = TextureRegion(frameBuffer.colorBufferTexture, 0, 0, visibleWorldWidth, visibleWorldHeight)
//
//        logger.debug { "frameRegion.regionWidth=${frameRegion.regionWidth} frameRegion.regionHeight=${frameRegion.regionHeight}" }
//        frameRegion.flip(false, true)
//    }
}
