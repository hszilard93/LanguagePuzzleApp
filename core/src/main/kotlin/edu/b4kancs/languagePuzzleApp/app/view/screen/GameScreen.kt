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
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Align
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
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.serialization.GdxSetSerializer
import edu.b4kancs.languagePuzzleApp.app.serialization.TestSerializableClass
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawer
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.CustomCursor.CLOSED_HAND_CURSOR
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.CustomCursor.OPEN_HAND_CURSOR
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.CustomCursor.ROTATE_LEFT_CURSOR
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.CustomCursor.ROTATE_RIGHT_CURSOR
import edu.b4kancs.languagePuzzleApp.app.view.screen.CustomCursorLoader.loadCustomCursor
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
import edu.b4kancs.languagePuzzleApp.app.view.ui.TextEditorPopup
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import edu.b4kancs.languagePuzzleApp.app.view.utils.toVector2
import edu.b4kancs.languagePuzzleApp.app.view.utils.toVector3
import edu.b4kancs.languagePuzzleApp.app.view.utils.unprojectScreenCoords
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.app.KtxScreen
import ktx.collections.GdxMap
import ktx.collections.GdxSet
import ktx.collections.gdxMapOf
import ktx.collections.set
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

    private val filePicker: FilePickerInterface = context.inject()

    // private val puzzleFont: BitmapFont = context.inject()
    private val puzzlePieceFrameBufferMap: GdxMap<PuzzlePiece, FrameBuffer>
    private val frameBufferCamera = OrthographicCamera()

    private val puzzlePieceTextureMap = GdxMap<PuzzlePiece, Texture>()

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
    private var puzzlePieceToRotate: PuzzlePiece? = null

    private val startZoom = 2f
    private var shouldDisplayDebugInfo = false
    private var isDraggingGame = false
    private val lastTouch = Vector3()
    private val handOpenCursor = if (!environment.isMobile) loadCustomCursor(OPEN_HAND_CURSOR) else null
    private val handClosedCursor = if (!environment.isMobile) loadCustomCursor(CLOSED_HAND_CURSOR) else null
    private val rotateLeftCursor = if (!environment.isMobile) loadCustomCursor(ROTATE_LEFT_CURSOR) else null
    private val rotateRightCursor = if (!environment.isMobile) loadCustomCursor(ROTATE_RIGHT_CURSOR) else null

    private val puzzleSnapHelper = PuzzleSnapHelper(gameModel)

    // UI Elements
    private lateinit var exerciseDescriptionFrame: Window
    private lateinit var exerciseDescriptionLabel: Label
    private lateinit var checkMarkImage: Image

    private var hasSavedScreenshot = false

    init {
        logger.debug { "init" }

        puzzlePieceFrameBufferMap = gdxMapOf()

        initializeExerciseDescriptionUI()
        initializeCheckMarkUI()
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

//        filePicker.openFileChooser { fh -> logger.info { fh.path() + fh.name() } }

//        val testSerializableGdxSet = GdxSet<TestSerializableClass>().apply {
//            addAll(
//                TestSerializableClass("test1", 1),
//                TestSerializableClass("test2", 2),
//                TestSerializableClass("test3", 3)
//            )
//        }
//        val jsonSerializer = Json {
//            prettyPrint = true
//        }
//        val serializedGdxSet = jsonSerializer.encodeToString(GdxSetSerializer.serializer(), testSerializableGdxSet)
//        logger.info { "serializedGdxSet = $serializedGdxSet" }
//
//        val deserializedGdxSet = Json.decodeFromString<GdxSet<TestSerializableClass>>(GdxSetSerializer.serializer(), serializedGdxSet)
//        logger.info { "deserializedGdxSet = ${deserializedGdxSet.toSet()}" }

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

        // Update the position and size of the exercise description frame if visible
        if (::exerciseDescriptionFrame.isInitialized && exerciseDescriptionFrame.isVisible) {
            exerciseDescriptionFrame.setSize(newWidth.toFloat(), 100f) // Height can be adjusted
            exerciseDescriptionFrame.setPosition(0f, newHeight - exerciseDescriptionFrame.height)
        }
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

        updatePuzzlePieceAnimations(delta)

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
        logger.misc { "renderGameWorld delta=$delta" }

        val puzzlesByLayers = gameModel.puzzlePieces.groupBy { it.depth }
        puzzlesByLayers.keys.sorted().forEach { layerI ->
            puzzlesByLayers[layerI]?.forEach inner@{ puzzlePiece ->
                if (!isPuzzleVisible(puzzlePiece)) return@inner

                val texture = getTextureByPuzzlePiece(puzzlePiece)

                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
                texture.bind()
                batch.use {
                    gameViewport.apply()
                    it.draw(
                        texture,
                        puzzlePiece.boundingBoxPos.x,
                        puzzlePiece.boundingBoxPos.y,
                        puzzlePiece.boundingBoxSize,
                        puzzlePiece.boundingBoxSize
                    )
                }

                if (!hasSavedScreenshot && layerI == 1) {
                    hasSavedScreenshot = true
//                    saveTextureToPNG(texture, layerI.toString())
                }
            }
        }
    }

    private fun renderPuzzle(puzzlePiece: PuzzlePiece): Texture {
        logger.debug { "renderPuzzle puzzlePiece = ${puzzlePiece.text}" }
        val frameBuffer = getFrameBufferByPuzzlePiece(puzzlePiece)

        frameBuffer.use {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            // Set up the camera for this FrameBuffer
            frameBufferCamera.setToOrtho(false, puzzlePiece.boundingBoxSize, puzzlePiece.boundingBoxSize)
            frameBufferCamera.update()
            batch.projectionMatrix = frameBufferCamera.combined

            puzzlePieceDrawer.render(puzzlePiece)
        }

        batch.projectionMatrix = gameCamera.combined

        return frameBuffer.colorBufferTexture
    }

    private fun updatePuzzlePieceAnimations(delta: Float) {
        for (puzzlePiece in gameModel.puzzlePieces) {
            if (puzzlePiece.size != puzzlePiece.targetSize) {
                puzzlePiece.animateSize(delta)
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
                    message =
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

    private fun getTextureByPuzzlePiece(puzzlePiece: PuzzlePiece): Texture {
        logger.misc { "getFrameBufferByPuzzlePiece puzzle=$puzzlePiece" }

        return if (!puzzlePiece.hasChangedAppearance) {
            val texture: Texture? = puzzlePieceTextureMap[puzzlePiece, null]
            if (texture != null) {
                texture
            }
            else {
                logger.debug { "getTextureByPuzzlePiece texture cache miss. puzzlePiece = ${puzzlePiece.text}" }
                puzzlePiece.hasChangedAppearance = true
                val newTexture = renderPuzzle(puzzlePiece)
                puzzlePieceTextureMap[puzzlePiece] = newTexture
                newTexture
            }
        }
        else {
            logger.debug { "getTextureByPuzzlePiece puzzle appearance changed, rerendering. puzzlePiece = ${puzzlePiece.text}" }
            puzzlePiece.hasChangedAppearance = true
            val newTexture = renderPuzzle(puzzlePiece)
            puzzlePieceTextureMap[puzzlePiece] = newTexture
            newTexture
        }
    }

    private fun getFrameBufferByPuzzlePiece(puzzle: PuzzlePiece): FrameBuffer {
        logger.misc { "getFrameBufferByPuzzlePiece puzzle=$puzzle" }

        return if (puzzlePieceFrameBufferMap.containsKey(puzzle) && !puzzle.hasChangedAppearance) {
            puzzlePieceFrameBufferMap[puzzle]
        }
        else {
            puzzlePieceFrameBufferMap[puzzle]?.dispose()   // Dispose of the old FrameBuffer
            val newFrameBuffer = FrameBuffer(
                Pixmap.Format.RGBA8888,
                puzzle.boundingBoxSize.toInt(),
                puzzle.boundingBoxSize.toInt(),
                false
            )
            puzzlePieceFrameBufferMap.put(puzzle, newFrameBuffer)
            puzzle.hasChangedAppearance = false
            puzzlePieceFrameBufferMap[puzzle]
        }
    }

    private fun isPuzzleVisible(puzzle: PuzzlePiece): Boolean {
        return gameCamera.frustum.boundsInFrustum(
            puzzle.boundingBoxPos.x,
            puzzle.boundingBoxPos.y,
            0f, // z-coordinate
            puzzle.boundingBoxSize,
            puzzle.boundingBoxSize,
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

    private fun saveTextureToPNG(texture: Texture, suffix: String = "0") {
        logger.misc { "saveTextureToPNG suffix=$suffix" }
        val dest = Gdx.files.local("screenshots/frameBufferScreenshot_$suffix.png")
        val textureData = texture.textureData

        if (!textureData.isPrepared) {
            textureData.prepare()
        }
        val pixmap: Pixmap? = textureData.consumePixmap()

        if (pixmap != null) {
            try {
                // Write the pixmap to the destination file as PNG
                val width = texture.width
                val height = texture.height
                val flippedPixmap = Pixmap(width, height, pixmap.format)
                for (y in 0 until height) {
                    flippedPixmap.drawPixmap(pixmap, 0, y, 0, height - y - 1, width, 1)
                }
                PixmapIO.writePNG(dest, flippedPixmap)
                flippedPixmap.dispose()
                logger.misc { "Screenshot saved to ${dest.path()}" }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.info { "Failed to save screenshot: ${e.message}" }
            } finally {
                pixmap.dispose()
            }
        }
        else {
            logger.info { "Failed to obtain Pixmap from TextureData." }
        }
    }

    private fun setCursor(cursor: Cursor?) {
        logger.misc { "setCursor cursor=$cursor" }
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
            gameCamera.position.set(it.pos.x + it.size / 2, it.pos.y + it.size / 2 + 200f, 0f)
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
                if (!puzzlePiece.isConnected) puzzlePiece.text = newText
                editingPuzzlePiece = null
            },
            onCancel = {
                editingPuzzlePiece = null
            }
        )
    }

    private fun initializeExerciseDescriptionUI() {
        logger.debug { "Initializing Exercise Description UI." }

        // Create the window with no title
        exerciseDescriptionFrame = Window("", uiSkin).apply {
            background = skin.getDrawable("white")

            // Set semi-transparent background color (e.g., black with 50% opacity)
            background.minWidth = 300f
            background.minHeight = 50f
            color.a = 0.75f // Semi-transparent
            isMovable = false
            isResizable = false

            // Set specific padding: 20px top, 40px left and right, and 10px bottom
            padTop(20f)
            padLeft(40f)
            padRight(40f)
            padBottom(10f)

            // Initially invisible; visibility will be handled in updateExerciseDescription()
            isVisible = false
        }

        // Create the label for the description text
        exerciseDescriptionLabel = Label("", uiSkin).apply {
            setWrap(true) // Enable text wrapping
            setAlignment(Align.center) // Center-align the text
        }

        // Add the label to the window
        exerciseDescriptionFrame.add(exerciseDescriptionLabel).expand().fill().row()

        // Position the window at the top center of the screen with a small margin
        exerciseDescriptionFrame.setPosition(
            (hudViewport.worldWidth - exerciseDescriptionFrame.width) / 2,
            hudViewport.worldHeight - exerciseDescriptionFrame.height - 10f
        )

        // Add the window to the UI stage
        uiStage.addActor(exerciseDescriptionFrame)

        // Update the description based on the current exercise
        updateExerciseDescription()
    }

    private fun updateExerciseDescription() {
        val currentExercise = gameModel.currentExercise
        if (currentExercise.taskDescription.isNotBlank()) {
            // Set the description text
            exerciseDescriptionLabel.setText(currentExercise.taskDescription)

            // Adjust the window size based on the content
            exerciseDescriptionFrame.pack()

            // Reposition the window to stay at the top center after packing
            exerciseDescriptionFrame.setPosition(
                (hudViewport.worldWidth - exerciseDescriptionFrame.width) / 2,
                hudViewport.worldHeight - exerciseDescriptionFrame.height - 10f
            )

            // Make the window visible
            exerciseDescriptionFrame.isVisible = true
        }
        else {
            // Hide the window if there's no description
            exerciseDescriptionFrame.isVisible = false
        }
    }

    private fun initializeCheckMarkUI() {
        logger.debug { "Initializing CheckMark UI." }

        val texture = Texture(Gdx.files.internal("graphics/checkmark.png"), Pixmap.Format.RGBA8888, true)

        // Create an Image actor with the checkmark texture
        checkMarkImage = Image(texture).apply {
            // Set the size of the checkmark (adjust as needed)
            setSize(80f, 80f) // Example size; adjust based on your design

            // Position it at the lower right corner with 20px padding from the edges
            setPosition(
                1080f, // 20px padding from the right
                20f // 20px padding from the bottom
            )

            // Initially invisible
            isVisible = false
        }

        // Add the checkmark image to the UI stage
        uiStage.addActor(checkMarkImage)
    }

    private fun displayCheckMark() {
        logger.debug { "displayCheckMark" }

        if (::checkMarkImage.isInitialized) {
            // Make the checkmark visible
            checkMarkImage.isVisible = true

            // Optionally, reset the alpha to 0 before fading in
            checkMarkImage.color.a = 0.5f

            // Add a fade-in action (e.g., over 1 second)
            checkMarkImage.addAction(Actions.fadeIn(1f))
        }
        else {
            logger.error { "CheckMarkImage has not been initialized!" }
        }
    }

    override fun dispose() {
        logger.debug { "dispose" }

        uiStage.dispose()
        uiSkin.dispose()
        puzzlePieceDrawer.dispose()

        puzzlePieceFrameBufferMap.forEach { it.value.dispose() }
        puzzlePieceFrameBufferMap.clear()
        handOpenCursor?.dispose()
        handClosedCursor?.dispose()

        inputMultiplexer.clear()
        if (Gdx.input.inputProcessor == inputMultiplexer) {
            Gdx.input.inputProcessor = null
        }

        super.dispose()
    }

    /* ###########################
    Input processing inner classes
    ############################ */
    private enum class Corner {
        TOP_LEFT, TOP_RIGHT
    }

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

            val overPuzzlePiece = gameModel.puzzlePieces.any { isPointOverPuzzlePiece(mousePos, it) }

            if (!environment.isMobile) {
                if (overPuzzlePiece && draggedPuzzlePiece == null) {
                    setCursor(handOpenCursor)
                    return true
                }
                else {
                    for (puzzlePiece in gameModel.puzzlePieces.filter { !it.isConnected }) {
                        val (ptr, corner) = isPointerNearCorner(mousePos, puzzlePiece) ?: (null to null)
                        puzzlePieceToRotate = ptr
                        if (puzzlePieceToRotate != null) {
                            if (corner == Corner.TOP_LEFT) {
                                setCursor(rotateLeftCursor)
                            }
                            else {
                                setCursor(rotateRightCursor)
                            }
                            return true
                        }
                    }
                    // If the cursor is not over a puzzle piece, or near a corner reset the cursor
                    if (currentCursor != null) {
                        setCursor(null)
                    }
                }
            }

            return false
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            logger.debug { "touchDown button=$button\t density = $displayDensity" }

            when (button) {
                Input.Buttons.LEFT -> {

                    if (currentCursor == rotateLeftCursor) {
                        logger.debug { "rotateLeft" }
                        puzzlePieceToRotate!!.rotateLeft()
                        return true
                    }
                    else if (currentCursor == rotateRightCursor) {
                        logger.debug { "rotateRight" }
                        puzzlePieceToRotate!!.rotateRight()
                        return true
                    }

                    val worldCoordinates = gameCamera.unprojectScreenCoords(screenX, screenY)
                    val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

                    val currentTime = System.currentTimeMillis()

                    for (puzzlePiece in gameModel.puzzlePieces) {
                        if (isPointOverPuzzlePiece(mousePos, puzzlePiece)) {
                            // Detect double click
                            if (currentTime - lastClickTime < doubleClickThreshold) {
                                logger.debug { "doubleClick puzzlePiece=$puzzlePiece" }
                                if (!puzzlePiece.isConnected) openTextEditor(puzzlePiece)

                                lastClickTime = 0
                                longPressTimer?.cancel()
                            }
                            else {
                                logger.debug { "touchDown puzzlePiece=$puzzlePiece" }
                                lastClickTime = currentTime

                                val isPuzzleConnectedToTwoOrMore = puzzlePiece.connectionSize >= 2
                                if (!isPuzzleConnectedToTwoOrMore) {
                                    draggedPuzzlePiece = puzzlePiece
                                    draggedPuzzlePiece!!.getAllFeatures()
                                        .forEach { puzzleSnapHelper.updatePuzzleFeatureCompatibilityMap(it) }

                                    lastMouseWorldPos.set(mousePos)
                                    if (!environment.isMobile) {
                                        setCursor(handClosedCursor)
                                    }
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
            logger.misc { "touchDragged screenX=$screenX screenY=$screenY" }

            val worldCoordinates = gameCamera.unprojectScreenCoords(screenX, screenY)
            val mousePos = Vector2(worldCoordinates.x, worldCoordinates.y)

            if (draggedPuzzlePiece != null) {
                try {
                    val delta = mousePos.cpy().sub(lastMouseWorldPos)
                    draggedPuzzlePiece!!.apply {
                        pos = pos.add(delta)
                    }
                    puzzleSnapHelper.updatePuzzleFeaturesByProximity()

                    lastMouseWorldPos.set(mousePos)
                    return true
                } catch (e: ConcurrentModificationException) {
                    logger.error { "touchDragged ConcurrentModificationException message=\n${e.message}" }
                }
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

                puzzleSnapHelper.performSnapIfAny()

                puzzleSnapHelper.clearPuzzleFeaturesByProximity()
                if (gameModel.isSolved()) {
                    displayCheckMark()
                }

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

        private fun isPointOverPuzzlePiece(mousePos: Vector2, puzzlePiece: PuzzlePiece): Boolean {
            val x = puzzlePiece.pos.x
            val y = puzzlePiece.pos.y
            val width = puzzlePiece.size
            val height = puzzlePiece.size

            return mousePos.x >= x && mousePos.x <= x + width && mousePos.y >= y && mousePos.y <= y + height
        }

        // TODO
        private fun isPointOverPuzzleTab(mousePos: Vector2, tab: PuzzleTab): Boolean {
//            val x = tab.
//            val y = puzzlePiece.pos.y
//            val width = puzzlePiece.size
//            val height = puzzlePiece.size

            return false
        }

        private fun isPointerNearCorner(mousePos: Vector2, puzzlePiece: PuzzlePiece): Pair<PuzzlePiece, Corner>? {
            logger.misc { "isPointerNearCorner mousePos=$mousePos" }

            val maxDistanceFromCorner = 25f
            // Define the top-left and top-right corners
            val topLeft = Vector2(
                puzzlePiece.pos.x,
                puzzlePiece.pos.y + puzzlePiece.size
            )
            val topRight = Vector2(
                puzzlePiece.pos.x + puzzlePiece.size,
                puzzlePiece.pos.y + puzzlePiece.size
            )

            // Calculate distances to corners
            val distanceToTopLeft = mousePos.dst(topLeft)
            val distanceToTopRight = mousePos.dst(topRight)

            // Check if within CORNER_RADIUS
            if (distanceToTopRight < maxDistanceFromCorner) {
                logger.misc { "isPointerNearCorner pointer is near ${puzzlePiece.text} distanceToTopRight=$distanceToTopRight" }
                return Pair(puzzlePiece, Corner.TOP_RIGHT)
            }
            else if (distanceToTopLeft < maxDistanceFromCorner) {
                logger.misc { "isPointerNearCorner pointer is near ${puzzlePiece.text} distanceToTopLeft=$distanceToTopLeft" }
                return Pair(puzzlePiece, Corner.TOP_LEFT)
            }

            return null
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
