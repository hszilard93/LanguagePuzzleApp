package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.HudFont
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawer
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.CustomCursor.CLOSED_HAND_CURSOR
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.CustomCursor.OPEN_HAND_CURSOR
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.loadCustomCursor
import edu.b4kancs.languagePuzzleApp.app.view.ui.TextEditorPopup
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import edu.b4kancs.languagePuzzleApp.app.view.utils.toVector2
import edu.b4kancs.languagePuzzleApp.app.view.utils.toVector3
import edu.b4kancs.languagePuzzleApp.app.view.utils.unprojectScreenCoords
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

    private val uiStage = Stage(ScreenViewport())
    private val uiSkin = Skin(Gdx.files.internal("skin/holo/uiskin.json"))

    private val hudFont: HudFont = context.inject()

    // private val puzzleFont: BitmapFont = context.inject()
    private val frameBufferMap: GdxMap<PuzzlePiece, FrameBuffer>
    private val frameBufferCamera = OrthographicCamera()

    private var draggedPuzzlePiece: PuzzlePiece? = null
        set(puzzlePiece) {
            field = puzzlePiece
            puzzlePiece?.depth = gameModel.puzzlePieces.maxOfOrNull { it.depth }?.plus(1) ?: 0
        }
    private var lastMouseScreenPos: Vector2 = Vector2()
    private var lastMouseWorldPos: Vector2 = Vector2()
    private var currentCursor: Cursor? = null

    private val puzzlePieceDrawer = PuzzlePieceDrawer(context)
    private val inputMultiplexer = InputMultiplexer()
    private val displayDensity = Gdx.graphics.density
    private var realToVirtualResolutionRatio: Float = 1f
    private val minZoom = 0.1f
    private val maxZoom = 5f

    private var editingPuzzlePiece: PuzzlePiece? = null
    private var textEditorPopup: TextEditorPopup? = null

    private val startZoom = 2f
    private var shouldDisplayDebugInfo = false
    private var isDraggingGame = false
    private val lastTouch = Vector3()
    private val handOpenCursor = if (!environment.isMobile) loadCustomCursor(OPEN_HAND_CURSOR) else null
    private val handClosedCursor = if (!environment.isMobile) loadCustomCursor(CLOSED_HAND_CURSOR) else null

    private val puzzleSnapHelper = PuzzleSnapHelper(gameModel)

    private var hasSavedScreenshot = false

    init {
        logger.debug { "init" }

        frameBufferMap = gdxMapOf()
    }

    override fun show() {
        logger.debug { "show" }

        inputMultiplexer.addProcessor(uiStage)
        inputMultiplexer.addProcessor(GameGestureDetector())
        inputMultiplexer.addProcessor(GameInputProcessor())

        Gdx.input.inputProcessor = inputMultiplexer

        gameCamera.apply {
            setToOrtho(false, Constants.GAME_VIRTUAL_WIDTH, Constants.GAME_VIRTUAL_HEIGHT)
            zoom = startZoom
        }
        recenterCamera()

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

        // Update and draw the UI Stage
        uiStage.act(delta)
        uiStage.draw()
    }

    private fun renderGameWorld(delta: Float) {
        val puzzles = gameModel.puzzlePieces

        val puzzlesByLayers = gameModel.puzzlePieces.groupBy { it.depth }
        puzzlesByLayers.keys.sorted().forEach { layerI ->
            puzzlesByLayers[layerI]?.forEach { puzzle ->
                if (!isPuzzleVisible(puzzle)) return@forEach

                val frameBuffer = getFrameBufferByPuzzlePiece(puzzle)

                frameBuffer.use {
                    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                    // Set up the camera for this FrameBuffer
                    frameBufferCamera.setToOrtho(false, puzzle.boundingBoxSize.first, puzzle.boundingBoxSize.second)
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
                        puzzle.boundingBoxPos.x,
                        puzzle.boundingBoxPos.y,
                        puzzle.boundingBoxSize.first,
                        puzzle.boundingBoxSize.second
                    )
                }

                if (!hasSavedScreenshot && layerI == 1) {
                    hasSavedScreenshot = true
                    saveFrameBufferToPNG(frameBuffer)
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
                val worldVector = gameCamera.unproject(Vector3(mouseX, mouseY, 0f)).toVector2()
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
        logger.misc { "setBackgroundColor red=$red, green=$green, blue=$blue, alpha=$alpha" }
        Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    private fun getFrameBufferByPuzzlePiece(puzzle: PuzzlePiece): FrameBuffer {
        logger.misc { "getFrameBufferByPuzzlePiece puzzle=$puzzle" }

        return if (frameBufferMap.containsKey(puzzle) && !puzzle.hasChangedSizeSinceLastRender) {
            frameBufferMap[puzzle]
        }
        else {
            frameBufferMap[puzzle]?.dispose()   // Dispose of the old FrameBuffer
            val newFrameBuffer = FrameBuffer(
                Pixmap.Format.RGBA8888,
                puzzle.boundingBoxSize.first.toInt(),
                puzzle.boundingBoxSize.second.toInt(),
                false
            )
            frameBufferMap.put(puzzle, newFrameBuffer)
            puzzle.hasChangedSizeSinceLastRender = false
            frameBufferMap[puzzle]
        }
    }

    private fun isPuzzleVisible(puzzle: PuzzlePiece): Boolean {
        return gameCamera.frustum.boundsInFrustum(
            puzzle.boundingBoxPos.x,
            puzzle.boundingBoxPos.y,
            0f, // z-coordinate
            puzzle.boundingBoxSize.first,
            puzzle.boundingBoxSize.second,
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

    private fun setCursor(cursor: Cursor?) {
        logger.debug { "setCursor cursor=$cursor" }
        if (cursor != null) {
            Gdx.graphics.setCursor(cursor)
            currentCursor = cursor
        }
        else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
            currentCursor = null
        }
    }

    private fun recenterCamera() {
        logger.debug { "recenterCamera" }
        gameModel.puzzlePieces.find { it.grammaticalRole == GrammaticalRole.VERB }?.let {
            gameCamera.position.set(it.pos.x + it.width / 2, it.pos.y + it.height / 2 + 200f, 0f)
        }
    }

    private fun openTextEditor(puzzlePiece: PuzzlePiece) {
        // Prevent multiple popups
        if (editingPuzzlePiece != null) {
            return
        }
        editingPuzzlePiece = puzzlePiece

        TextEditorPopup(
            stage = uiStage,
            skin = uiSkin,
            puzzlePiece = puzzlePiece,
            onSave = { newText ->
                puzzlePiece.text = newText
                editingPuzzlePiece = null
            },
            onCancel = {
                editingPuzzlePiece = null
            }
        )
    }

    override fun dispose() {
        logger.debug { "dispose" }

        uiStage.dispose()
        uiSkin.dispose()
        hudFont.dispose()
        //puzzleFont.dispose()
        puzzlePieceDrawer.dispose()

        frameBufferMap.forEach { it.value.dispose() }
        frameBufferMap.clear()
        handOpenCursor?.dispose()
        handClosedCursor?.dispose()

        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)

        inputMultiplexer.clear()
        if (Gdx.input.inputProcessor == inputMultiplexer) {
            Gdx.input.inputProcessor = null
        }

        super.dispose()
    }

    /* ###########################
    Input processing inner classes
    ############################ */

    inner class GameInputProcessor : InputAdapter() {

        private var lastClickTime: Long = 0
        private val doubleClickThreshold = 300
        private var longPressTimer: Timer.Task? = null
        private val longPressDuration = 500

        init {
            logger.debug { "GameInputProcessor.init" }
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            logger.misc { "mouseMoved screenX=$screenX screenY=$screenY" }

            val worldCoordinates = gameCamera.unprojectScreenCoords(screenX, screenY)
            val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

            var overPuzzlePiece = gameModel.puzzlePieces.any { isPointOverPuzzlePiece(mousePos, it) }

            if (!environment.isMobile) {
                if (overPuzzlePiece && draggedPuzzlePiece == null) {
                    setCursor(handOpenCursor)
                }
                else if (currentCursor != null) {
                    setCursor(null)
                }
            }

            return false
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            logger.debug { "touchDown button=$button\t density = $displayDensity" }

            when (button) {
                Input.Buttons.LEFT -> {
                    val worldCoordinates = gameCamera.unprojectScreenCoords(screenX, screenY)
                    val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

                    val currentTime = System.currentTimeMillis()

                    for (puzzlePiece in gameModel.puzzlePieces) {
                        if (isPointOverPuzzlePiece(mousePos, puzzlePiece)) {
                            // Detect double click
                            if (currentTime - lastClickTime < doubleClickThreshold) {
                                logger.debug { "doubleClick puzzlePiece=$puzzlePiece" }
                                openTextEditor(puzzlePiece)
                                lastClickTime = 0
                                longPressTimer?.cancel()
                            }
                            else {
                                logger.debug { "touchDown puzzlePiece=$puzzlePiece" }
                                lastClickTime = currentTime

                                draggedPuzzlePiece = puzzlePiece
                                draggedPuzzlePiece!!.getAllFeatures().forEach { puzzleSnapHelper.updatePuzzleFeatureCompatibilityMap(it) }

                                lastMouseWorldPos.set(mousePos)
                                if (!environment.isMobile) {
                                    setCursor(handClosedCursor)
                                }
                            }
                            return true
                        }
                    }

                    // If not touching a puzzle piece, start dragging the game
                    isDraggingGame = true
                    lastTouch.set(worldCoordinates.toVector3())
                    return true
                }

                Input.Buttons.RIGHT -> {
                    shouldDisplayDebugInfo = !shouldDisplayDebugInfo
                    logger.info { "displayDebugInfo = $shouldDisplayDebugInfo" }
                    return true
                }

                else -> return false
            }
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            logger.debug { "touchDragged screenX=$screenX screenY=$screenY" }

            val worldCoordinates = gameCamera.unprojectScreenCoords(screenX, screenY)
            val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

            if (draggedPuzzlePiece != null) {
                val delta = mousePos.cpy().sub(lastMouseWorldPos)
                draggedPuzzlePiece!!.apply {
                    pos = pos.add(delta)
                }
                puzzleSnapHelper.updatePuzzleFeaturesByProximity()

                lastMouseWorldPos.set(mousePos)
                return true
            }
            else if (isDraggingGame) {
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
                draggedPuzzlePiece?.getAllFeatures()?.forEach { puzzleSnapHelper.clearPuzzleFeatureCompatibilityMap(it) }
                draggedPuzzlePiece = null

                puzzleSnapHelper.performSnap()
                puzzleSnapHelper.clearPuzzleFeaturesByProximity()

                isDraggingGame = false
                if (!environment.isMobile) {
                    if (currentCursor == handClosedCursor) {
                        setCursor(handOpenCursor)
                        gameModel.rebasePuzzleDepths()
                    }
                }
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

        private fun isPointOverPuzzlePiece(point: Vector2, puzzlePiece: PuzzlePiece): Boolean {
            val x = puzzlePiece.pos.x
            val y = puzzlePiece.pos.y
            val width = puzzlePiece.width
            val height = puzzlePiece.height

            return point.x >= x && point.x <= x + width && point.y >= y && point.y <= y + height
        }
    }

    // Touch specific gesture handling
    inner class GameGestureDetector : GestureDetector(object : AbstractGestureListener() {

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
