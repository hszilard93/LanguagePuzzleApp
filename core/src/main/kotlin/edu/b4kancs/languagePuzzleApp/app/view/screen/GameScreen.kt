package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
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

    private val hudFont: BitmapFont
    private val frameBufferPool: GdxArray<FrameBuffer>
    private val frameRegionPool: GdxArray<TextureRegion>
    private val shaderProgram: ShaderProgram

    private val puzzlePieceDrawer = PuzzlePieceDrawer(context)
    private val minZoom = 0.3f
    private val maxZoom = 1.4f
    private var shouldDisplayDebugInfo = false
    private var isDragging = false
    private val lastTouch = Vector3()

    private var hasSavedScreenshot = false

    init {
        logger.debug { "init" }

        shaderProgram = loadShader()

        hudFont = BitmapFont(Gdx.files.internal("hud_font.fnt"))

        frameBufferPool = gdxArrayOf<FrameBuffer>(true)
        frameRegionPool = gdxArrayOf<TextureRegion>(true)

        for (i in 0 until 3) {  // Start with 3 default layers. Will be enough in most cases.
            val frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, gameViewport.worldWidth.toInt(), gameViewport.worldHeight.toInt(), false)
            frameBufferPool.add(frameBuffer)
            val frameRegion = TextureRegion(frameBuffer.colorBufferTexture, gameViewport.screenWidth, gameViewport.screenHeight)
            frameRegion.flip(false, true)
            frameRegionPool.add(frameRegion)
        }
    }

    override fun show() {
        logger.debug { "show" }

        Gdx.input.inputProcessor = this

//        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, gameViewport.worldWidth.toInt(), gameViewport.worldHeight.toInt(), false)

        gameCamera.apply {
            setToOrtho(false)
            zoom = 0.5f
        }

        super.show()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }

        gameViewport.update(newWidth, newHeight, true)

        hudViewport.update(newWidth, newHeight, true)

        for (frameBuffer in frameBufferPool) {
//                updateFrameBuffer(frameBuffer)
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
                        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
                        puzzlePieceDrawer.render(puzzle)
                    }
                }

                if (!hasSavedScreenshot) {
                    hasSavedScreenshot = true
                    saveFrameBufferToPNG(frameBufferPool[layerI])
                }

                val resultTexture = frameBufferPool[layerI].colorBufferTexture

                shaderProgram.use { shader ->
                    shader.setUniform2fv(
                        "u_textureSize",
                        floatArrayOf(resultTexture.width.toFloat(), resultTexture.height.toFloat()),
                        0,
                        2
                    )
                    batch.use { batch ->
//                    val frameRegion = frameRegionPool[layerI]
//                        batch.shader = shader
                        batch.draw(resultTexture, 0f, 0f, resultTexture.width.toFloat(), resultTexture.height.toFloat())
                        batch.shader = null
                    }
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

        hudFont.draw(batch, "FPS=" + Gdx.graphics.framesPerSecond, 0f, hudCamera.viewportHeight)
        hudFont.draw(batch, "Lower left", 0f, hudFont.lineHeight)
        hudFont.draw(batch, "Zoom: %.1f".format(gameCamera.zoom), hudCamera.viewportWidth - 75f, hudCamera.viewportHeight)

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
                hudFont.draw(batch, "$mouseViewportX, $mouseViewportY", renderX, renderY + 10f)
            }
        }
    }

    private fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Float) {
        logger.misc { "setBackgroundColor red=$red green=$green blue=$blue alpha=$alpha" }
        Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        logger.debug { "touchDown button=$button" }

        return when (button) {
            Input.Buttons.RIGHT -> {
                shouldDisplayDebugInfo = !shouldDisplayDebugInfo
                logger.info { "displayDebugInfo = $shouldDisplayDebugInfo" }
                true
            }

            Input.Buttons.LEFT -> {
                isDragging = true
                lastTouch.set(gameCamera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f)))
                true
            }

            else -> false
        }
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (isDragging) {
            val currentTouch = gameCamera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
            val delta = currentTouch.sub(lastTouch)
            gameCamera.translate(-delta.x, -delta.y, 0f)
            lastTouch.set(screenX.toFloat(), screenY.toFloat(), 0f)
            return true
        }
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.LEFT) {
            isDragging = false
            return true
        }
        return false
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

    private fun loadShader(): ShaderProgram {
        val vertexShader = Gdx.files.internal("shaders/seamless.vert").readString()
        val fragmentShader = Gdx.files.internal("shaders/seamless.frag").readString()
        val shaderProgram = ShaderProgram(vertexShader, fragmentShader)

        if (!shaderProgram.isCompiled) {
            logger.error { "Shader compilation failed:\n" + shaderProgram.log }
            throw IllegalStateException()
        }
        return shaderProgram
    }

    private fun saveFrameBufferToPNG(frameBuffer: FrameBuffer) {
        val pixmap: Pixmap
        val fbWidth = frameBuffer.width
        val fbHeight = frameBuffer.height
        frameBuffer.use {
            pixmap = Pixmap.createFromFrameBuffer(0, 0, fbWidth, fbHeight)
        }
        val flippedPixmap: Pixmap = Pixmap(fbWidth, fbHeight, pixmap.format)
        for (y in 0 until fbHeight) {
            flippedPixmap.drawPixmap(pixmap, 0, y, 0, fbHeight - y - 1, fbWidth, 1)
        }
        pixmap.dispose() // Dispose the original pixmap
        PixmapIO.writePNG(Gdx.files.local("frameBufferScreenshot.png"), flippedPixmap);
        flippedPixmap.dispose();
    }

    override fun dispose() {
        logger.debug { "dispose" }

        hudFont.dispose()
        shaderProgram.dispose()
        puzzlePieceDrawer.dispose()

        frameBufferPool.forEach { it.dispose() }
        frameBufferPool.clear()
        frameRegionPool.clear()

        super.dispose()
    }
}
