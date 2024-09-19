package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.Environment
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
    private val gameModel: GameModel,
    private val environment: Environment
) : KtxScreen {

    companion object {
        val logger = logger<GameScreen>()
    }

    private val hudFont: BitmapFont
    private val frameBufferMap: GdxMap<PuzzlePiece, FrameBuffer>
    private val frameBufferCamera = OrthographicCamera()

    private val puzzlePieceDrawer = PuzzlePieceDrawer(context)
    private val inputMultiplexer = InputMultiplexer()
    private val displayDensity = Gdx.graphics.density
    private val minZoom = 0.1f
    private val maxZoom = 5f
    private val startZoom = 2f
    private var shouldDisplayDebugInfo = false
    private var isDraggingGame = false
    private val lastTouch = Vector3()

    private var realToVirtualResolutionRatio: Float = 1f

    private var hasSavedScreenshot = false

    init {
        logger.debug { "init" }

        // Adjust font scaling based on display density
        hudFont = BitmapFont(Gdx.files.internal("hud_font.fnt")).apply {
            data.setScale(1f * displayDensity)
        }

        frameBufferMap = gdxMapOf()
    }

    override fun show() {
        logger.debug { "show" }

        inputMultiplexer.addProcessor(createGameGestureDetector())
        inputMultiplexer.addProcessor(createBaseInputProcessor())
        Gdx.input.inputProcessor = inputMultiplexer

        gameCamera.apply {
            setToOrtho(false, Constants.GAME_VIRTUAL_WIDTH, Constants.GAME_VIRTUAL_HEIGHT)
            zoom = startZoom
        }

        super.show()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }

        // Update both viewports with min and max constraints
        gameViewport.update(newWidth, newHeight, false)
        hudViewport.update(newWidth, newHeight, true)

        // Update realToVirtualResolutionRatio
        val screenSize = Gdx.graphics.width to Gdx.graphics.height
        val viewPortSize = gameCamera.viewportWidth to gameCamera.viewportHeight
        realToVirtualResolutionRatio = maxOf(screenSize.first / viewPortSize.first, screenSize.second / viewPortSize.second)
        logger.debug { "screenSize=$screenSize; viewPortSize=$viewPortSize" }
        logger.debug { "realToVirtualResolutionRatio=$realToVirtualResolutionRatio" }
    }

    override fun render(delta: Float) {
        logger.misc { "render delta=$delta" }

        // Update cameras
        gameCamera.update()
        hudCamera.update()

        // Clear the screen with a background color
        setBackgroundColor(180, 255, 180, 1f)

        // Render the game world
        gameViewport.apply()
        batch.projectionMatrix = gameCamera.combined
        renderGameWorld(delta)

        // Render the HUD
        hudViewport.apply()
        batch.projectionMatrix = hudCamera.combined
        renderHud()
    }

    private fun renderGameWorld(delta: Float) {
        val puzzles = gameModel.puzzlePieces
        if (puzzles.isNotEmpty()) {

            val puzzlesByLayers = gameModel.puzzlePieces.groupBy { it.depth }

            for (layerI in puzzlesByLayers.keys.sorted()) {
                puzzlesByLayers[layerI]?.forEach { puzzle ->
                    if (!isPuzzleVisible(puzzle)) return@forEach

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
                            puzzle.renderSize.first,
                            puzzle.renderSize.second
                        )
                    }

                    if (!hasSavedScreenshot && layerI == 1) {
                        hasSavedScreenshot = true
                        saveFrameBufferToPNG(frameBuffer)
                    }
                }
            }
        }
    }

    private fun renderHud() {
        logger.misc { "renderHud" }

        batch.use {
            var message = "FPS=${Gdx.graphics.framesPerSecond}"
            hudFont.draw(batch, message, 10f, hudCamera.viewportHeight - 10f)
            message = "Lower left"
            hudFont.draw(batch, message, 10f, hudFont.lineHeight + 10f)
            message = "Zoom: %.1f".format(gameCamera.zoom)
            hudFont.draw(
                batch,
                message,
                hudCamera.viewportWidth - hudFont.lineHeight * (message.length / 2),
                hudCamera.viewportHeight - 10f
            )

            if (Game.IS_DEBUG_MODE_ON && shouldDisplayDebugInfo) {
                // Render the mouse position interpreted as real pixel coordinates within the viewport
                val screenHeight = Gdx.graphics.height.toFloat()
                val mouseX = Gdx.input.x.toFloat()
                val mouseY = Gdx.input.y.toFloat()
                val renderVector = hudViewport.unproject(Vector2(mouseX, mouseY))
                val worldVector = gameCamera.unproject(Vector3(mouseX, mouseY, 0f))
                if (hudViewport.screenWidth >= 200) {  // Ensure viewport is large enough
                    val renderX = (renderVector.x + 10f).coerceIn(10f, hudViewport.worldWidth - 100f)
                    val renderY = (renderVector.y + 10f).coerceIn(10f, hudViewport.worldHeight - 10f)
                    val message =
                        """$mouseX, $mouseY
                           |${worldVector.x}, ${worldVector.y}
                           |$realToVirtualResolutionRatio"""
                            .trimMargin()
                    hudFont.draw(batch, message, renderX, renderY + 10f)
                }
            }
        }
    }

    private fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Float) {
        logger.misc { "setBackgroundColor red=$red green=$green blue=$blue alpha=$alpha" }
        Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
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

    private fun isPuzzleVisible(puzzle: PuzzlePiece): Boolean {
        return gameCamera.frustum.boundsInFrustum(
            puzzle.renderPos.x,
            puzzle.renderPos.y,
            0f, // z-coordinate
            puzzle.renderSize.first,
            puzzle.renderSize.second,
            1f // depth (assuming 2D)
        )
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
        PixmapIO.writePNG(Gdx.files.local("frameBufferScreenshot.png"), flippedPixmap)
        flippedPixmap.dispose()
    }

    private fun createBaseInputProcessor(): BaseInputProcessor {
        logger.debug { "createBaseInputProcessor" }

        return object : BaseInputProcessor() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                logger.debug { "touchDown button=$button\n density = $displayDensity" }

                return when (button) {
                    Input.Buttons.RIGHT -> {
                        shouldDisplayDebugInfo = !shouldDisplayDebugInfo
                        logger.info { "displayDebugInfo = $shouldDisplayDebugInfo" }
                        true
                    }

                    Input.Buttons.LEFT -> {
                        lastTouch.set(gameCamera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f)))
                        isDraggingGame = true
                        true
                    }

                    else -> false
                }
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                logger.debug { "touchDragged screenX=$screenX screenY=$screenY" }

                if (isDraggingGame) {
                    val deltaX = Gdx.input.deltaX.toFloat() * (1 / realToVirtualResolutionRatio * gameCamera.zoom)
                    val deltaY = Gdx.input.deltaY.toFloat() * (1 / realToVirtualResolutionRatio * gameCamera.zoom)

                    gameCamera.translate(-deltaX, deltaY, 0f)
                    gameCamera.update()

                    return true
                }
                return false
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                logger.debug { "touchUp button=$button" }

                if (button == Input.Buttons.LEFT) {
                    isDraggingGame = false
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
                val newZoom: Float = (gameCamera.zoom + amountY * 0.1f).coerceIn(minZoom, maxZoom)
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
        }
    }

    // Touch specific gesture handling
    private fun createGameGestureDetector(): GestureDetector {
        logger.debug { "createGameGestureDetector" }

        return GestureDetector(object : AbstractGestureListener() {

            private var lastZoomDistance = 0f
            private var lastZoomTime = System.currentTimeMillis()

            override fun longPress(x: Float, y: Float): Boolean {
                logger.debug { "longPress x=$x y=$y" }
                // Toggle debug info on long press
                shouldDisplayDebugInfo = !shouldDisplayDebugInfo
                logger.info { "displayDebugInfo = $shouldDisplayDebugInfo" }
                return true
            }

            override fun zoom(originalDistance: Float, currentDistance: Float): Boolean {
                logger.debug { "zoom originalDistance=$originalDistance currentDistance=$currentDistance" }

                // Reset zoom after 0.2 seconds of no zoom
                if (System.currentTimeMillis() - lastZoomTime > 100) {
                    lastZoomDistance = 0f
                }

                if (originalDistance > 0f) {
                    if (lastZoomDistance == 0f) lastZoomDistance = originalDistance

                    val scaleDelta = (1f - currentDistance / lastZoomDistance).coerceIn(-0.05f, 0.05f)
                    logger.debug { "scaleDelta=$scaleDelta" }
                    val newZoom = (gameCamera.zoom + scaleDelta).coerceIn(minZoom, maxZoom)
                    logger.debug { "newZoom=$newZoom" }
                    gameCamera.zoom = newZoom
                    gameCamera.update()

                    lastZoomDistance = currentDistance
                    lastZoomTime = System.currentTimeMillis()
                }
                return true
            }
        })
    }

    override fun dispose() {
        logger.debug { "dispose" }

        hudFont.dispose()
        puzzlePieceDrawer.dispose()

        frameBufferMap.forEach { it.value.dispose() }
        frameBufferMap.clear()

        inputMultiplexer.clear()
        if (Gdx.input.inputProcessor == inputMultiplexer) {
            Gdx.input.inputProcessor = null
        }

        super.dispose()
    }
}
