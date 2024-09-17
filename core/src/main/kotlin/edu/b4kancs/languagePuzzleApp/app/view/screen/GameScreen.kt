package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawer
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import ktx.app.KtxScreen
import ktx.collections.GdxMap
import ktx.collections.gdxMapOf
import ktx.graphics.use
import ktx.inject.Context
import ktx.log.logger

class GameScreen(
    private val context: Context,
    private val batch: Batch,
    private val assetManager: AssetManager,
    private val gameViewport: GameViewport,
    private val hudViewport: HudViewport,
    private val gameCamera: GameCamera,
    private val hudCamera: HudCamera,
    private val gameModel: GameModel
) : KtxScreen, BaseInputProcessor() {

    companion object {
        val logger = logger<GameScreen>()
    }

    private val hudFont: BitmapFont
    private val frameBufferMap: GdxMap<PuzzlePiece, FrameBuffer>
    private val frameBufferCamera = OrthographicCamera()
    private val shaderProgram: ShaderProgram

//    private val displayDensity = Gdx.graphics.density
    private val puzzlePieceDrawer = PuzzlePieceDrawer(context)
    private val minZoom = 1f
    private val maxZoom = 3f
    private val startZoom = 2f //* displayDensity
    private var shouldDisplayDebugInfo = false
    private var isDragging = false
    private val lastTouch = Vector3()

    private var hasSavedScreenshot = false

    init {
        logger.debug { "init" }

        shaderProgram = loadShader()

        hudFont = BitmapFont(Gdx.files.internal("hud_font.fnt"))

        frameBufferMap = gdxMapOf()
    }

    override fun show() {
        logger.debug { "show" }

        Gdx.input.inputProcessor = this

//        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, gameViewport.worldWidth.toInt(), gameViewport.worldHeight.toInt(), false)

        gameCamera.apply {
            setToOrtho(false)
            zoom = startZoom
        }

        super.show()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }

        val aspectRatio = newHeight / newWidth

//        gameViewport.update(WORLD_WIDTH / 10, WORLD_WIDTH / 10 * aspectRatio, false)
        gameViewport.update(newWidth, newHeight, false)

        hudViewport.update(newWidth, newHeight, true)
    }

    override fun render(delta: Float) {
        logger.misc { "render delta=$delta" }

        gameCamera.update()
        setBackgroundColor(180, 255, 180, 1f)

        val puzzles = gameModel.puzzlePieces
        if (puzzles.isNotEmpty()) {

            val puzzlesByLayers = gameModel.puzzlePieces.groupBy { it.depth }

            for (layerI in puzzlesByLayers.keys) {
                puzzlesByLayers[layerI]?.forEach { puzzle ->
                    val frameBuffer = getFrameBufferByPuzzlePiece(puzzle)

                    frameBuffer.use {
                        Gdx.gl.glClearColor(0f, 0f, 0f, 0.05f)
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                        // Set up the camera for this FrameBuffer
                        frameBufferCamera.setToOrtho(false, puzzle.renderSize.first, puzzle.renderSize.second)
                        frameBufferCamera.update()
                        batch.projectionMatrix = frameBufferCamera.combined

                        puzzlePieceDrawer.render(puzzle)
                    }

                    // Reset the projection matrix for rendering to the screen
                    batch.projectionMatrix = gameCamera.combined

                    val textureRegion = frameBuffer.colorBufferTexture
                    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
                    textureRegion.bind()
                    batch.use {
                        gameViewport.apply()
                        it.draw(
                            textureRegion,
                            puzzle.renderPos.x,
                            puzzle.renderPos.y,
                            textureRegion.width.toFloat(),
                            textureRegion.height.toFloat()
                        )
                    }

                    if (!hasSavedScreenshot && layerI == 1) {
                        hasSavedScreenshot = true
                        saveFrameBufferToPNG(frameBuffer)
                    }
                }
            }
        }

        renderHud()
    }

    private fun renderHud() {
        logger.misc { "renderHud" }

        batch.use {
            // Use the HUD camera to render the text
            hudViewport.apply()
            batch.projectionMatrix = hudCamera.combined

//            hudFont.data.setScale(1f/displayDensity)
            hudFont.draw(batch, "FPS=" + Gdx.graphics.framesPerSecond, 0f, hudCamera.viewportHeight)
            hudFont.draw(batch, "Lower left", 0f, hudFont.lineHeight)
            hudFont.draw(batch, "Zoom: %.1f".format(gameCamera.zoom), hudCamera.viewportWidth - 75f, hudCamera.viewportHeight)

            if (Game.IS_DEBUG_MODE_ON && shouldDisplayDebugInfo) {
                // Render the mouse position interpreted as real pixel coordinates within the viewport ("0, 0" is the viewports bottom left corner)
                val screenHeight = Gdx.graphics.height.toFloat()
                val mouseX = Gdx.input.x.toFloat()
                val mouseY = Gdx.input.y.toFloat()
                val mouseViewportX = mouseX - hudViewport.leftGutterWidth
                val mouseViewportY = screenHeight - mouseY - hudViewport.bottomGutterHeight
                val renderVector = hudViewport.unproject(Vector2(mouseX, mouseY))
                val worldVector = gameCamera.unproject(Vector3(mouseX, mouseY, 0f))
                if (hudViewport.screenWidth >= 200) {  // Catches errors when the viewport is too small or not visible (e.g. window is minimized)
                    val renderX = (renderVector.x + 10f).coerceIn(10f, hudViewport.worldWidth - 100f)
                    val renderY = (renderVector.y + 10f).coerceIn(10f, hudViewport.worldHeight - 10f)
                    hudFont.draw(batch, "$mouseViewportX, $mouseViewportY\n${worldVector.x}, ${worldVector.y}", renderX, renderY + 10f)
                }
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
                lastTouch.set(gameCamera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f)))
                isDragging = true
                true
            }

            else -> false
        }
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (isDragging) {
//            val currentTouch = gameCamera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
//            val delta = currentTouch.sub(lastTouch)
//            gameCamera.translate(-delta.x, -delta.y, 0f)
//            lastTouch.set(gameCamera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f)))
            val x = Gdx.input.deltaX.toFloat()
            val y = Gdx.input.deltaY.toFloat()

            gameCamera.translate(-x, -y)
            gameViewport.apply()
            gameCamera.update()

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

        return true
    }

    private fun getFrameBufferByPuzzlePiece(puzzle: PuzzlePiece): FrameBuffer {
        if (!frameBufferMap.containsKey(puzzle) || puzzle.hasChangedSizeSinceLastRender) {
            val newFrameBuffer = FrameBuffer(
                Pixmap.Format.RGBA8888,
                puzzle.renderSize.first.toInt(),
                puzzle.renderSize.second.toInt(),
                false
            )
            frameBufferMap.put(puzzle, newFrameBuffer)
            puzzle.hasChangedSizeSinceLastRender = false
        }
        return frameBufferMap[puzzle]
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

        frameBufferMap.forEach { it.value.dispose() }
        frameBufferMap.clear()

        super.dispose()
    }
}
