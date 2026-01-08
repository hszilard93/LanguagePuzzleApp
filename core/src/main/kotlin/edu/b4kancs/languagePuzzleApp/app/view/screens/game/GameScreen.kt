package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.viewport.ExtendViewport
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.Platform
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawer
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.input.GameGestureDetector
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.input.GameInputManager
import edu.b4kancs.languagePuzzleApp.app.view.screens.setBackgroundColor
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
import edu.b4kancs.languagePuzzleApp.app.view.utils.HudFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.toVector2
import ktx.app.KtxScreen
import ktx.graphics.moveTo
import ktx.inject.Context
import ktx.log.logger

class GameScreen(
    context: Context,
    private val game: Game,
    private val onBackButtonClicked: (KtxScreen) -> Unit
) : KtxScreen {

    companion object {
        val logger = logger<GameScreen>()
    }

    private val batch: Batch = context.inject()
    private val gameViewport: GameViewport = context.inject()
    private val hudViewport: HudViewport = context.inject()
    private val gameCamera: GameCamera = context.inject()
    private val hudCamera: HudCamera = context.inject()
    private val gameModel: GameModel = context.inject()
    private val environment: Environment = context.inject()
    private val hudFont = context.inject<HudFontHolder>().font
    private val filePicker: FilePickerInterface = context.inject()
    private val cursorManager: CursorManager = context.inject()

    private val puzzlePieceDrawer: PuzzlePieceDrawer = PuzzlePieceDrawer(context)

    private val uiViewPortDimensions = Vector2(1200f, 800f)
    private val uiStage = Stage(ExtendViewport(uiViewPortDimensions.x, uiViewPortDimensions.y))
    private val uiSkin = Skin(Gdx.files.internal("skin/holo/uiskin.json"))

    private val inputMultiplexer = InputMultiplexer()

    // Managers
    private var cameraController: CameraController
    private var uiManager: UIManager
    private var puzzleManager: PuzzleManager
    private var puzzleRenderer: PuzzleRenderer
    private var hudRenderer: HudRenderer
    private var regularInputManager: GameInputManager
    private var mobileGestureDetector: GameGestureDetector

    private var shouldDisplayDebugInfo = false
        private set(value) {
            field = value
            logger.info { "shouldDisplayDebugInfo = $shouldDisplayDebugInfo" }
            hudRenderer.displayDebugInfo(field)
        }


    private val puzzleSnapHelper = gameModel.puzzleSnapHelper

    init {
        logger.debug { "init" }

        cameraController = CameraController(gameCamera, hudCamera, gameViewport, hudViewport, gameModel)
        cameraController.setupCameras()

        uiManager = UIManager(context, uiStage, uiSkin) {
            onBackButtonClicked(this@GameScreen)
        }

        puzzleManager = PuzzleManager(gameModel, puzzleSnapHelper, uiManager)

        uiManager.registerPuzzleManager(puzzleManager)
        uiManager.initializeUI()

        puzzleRenderer = PuzzleRenderer(batch, gameCamera, gameViewport, gameModel, puzzlePieceDrawer)

        hudRenderer = HudRenderer(
            batch,
            hudCamera,
            hudViewport,
            hudFont,
            gameCamera,
            realToVirtualResolutionRatio = calculateResolutionRatio()
        )

        // Initialize Input Manager
        regularInputManager = GameInputManager(
            cameraController,
            puzzleManager,
            cursorManager,
            uiManager,
            environment,
            gameModel,
            realToVirtualResolutionRatio = calculateResolutionRatio(),
            setBackgroundColor = ::setBackgroundColor,
            toggleDebugInfo = { shouldDisplayDebugInfo = !shouldDisplayDebugInfo }
        )

        // Initialize Mobile Gesture Detector with all required parameters
        mobileGestureDetector = GameGestureDetector(
            cameraController,
            puzzleManager,
            cursorManager,
            uiManager,
            environment,
            gameModel,
            gameViewport
        )

        // Register the input manager with puzzle manager before setting up input processors
        puzzleManager.registerGameInputManager(regularInputManager)

        // Set up input processors
        inputMultiplexer.addProcessor(uiStage)
        if (environment.platform in setOf(Platform.WEB_ANDROID, Platform.WEB_IOS, Platform.WEB_IPAD)) {
            logger.info { "inputMultiplexer = mobileGestureDetector" }
            inputMultiplexer.addProcessor(mobileGestureDetector)
        }
        else {
            logger.info { "inputMultiplexer = mobileGestureDetector" }
            inputMultiplexer.addProcessor(regularInputManager)
        }

        Gdx.input.inputProcessor = inputMultiplexer
    }

    override fun show() {
        logger.debug { "show" }
        centerCameraByPuzzleGrid()

        // Temporary
        puzzleManager.checkSolution()

        super.show()
    }

    private fun centerCameraByPuzzleGrid() {
        if (gameModel.puzzlePieces.isNotEmpty()) {
            var maxX = Float.MIN_VALUE
            var minX = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE
            var minY = Float.MAX_VALUE

            for (piece in gameModel.puzzlePieces) {
                maxX = maxOf(maxX, piece.pos.x)
                minX = minOf(minX, piece.pos.x)
                maxY = maxOf(maxY, piece.pos.y)
                minY = minOf(minY, piece.pos.y)
            }

            val middlePos = Vector2((maxX + minX) / 2, (maxY + minY) / 2)
            val middlePosScreen = gameViewport.project(middlePos.cpy())//.add(-350f, -100f)
            logger.info { "middlePos = $middlePos \tmiddlePosScreen = $middlePosScreen" }
            gameCamera.moveTo(middlePosScreen, Gdx.graphics.width / 2f * -1, Gdx.graphics.height / 5f * -1)
//            gameCamera.moveTo(middlePosScreen)
            gameCamera.update()
        }
        else {
            gameCamera.moveTo(Vector2(0f, 0f))
            gameCamera.update()
        }
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }
        cameraController.resize(newWidth, newHeight)
//        (uiStage.viewport as ExtendViewport).
        uiStage.viewport.update(newWidth, newHeight, true)

        val multiplier = maxOf(1200f / newWidth, 800f / newHeight)
        try {
            uiManager.updateFonts(multiplier)
        } catch (e: GdxRuntimeException) {
            logger.error { e.message ?: "GdxRuntimeException occurred with no message." }
        }
        uiManager.updateTaskInfo()
    }

    override fun render(delta: Float) {
        // Update cameras
        cameraController.updateCameras()

        // Clear the screen with a background color
        setBackgroundColor(180, 255, 180, 1f)

        // Render the game world
        gameViewport.apply()
        puzzleRenderer.render(delta)

        // Render the HUD
        hudViewport.apply()
        hudRenderer.render()

        // Update and draw the UI Stage
        uiStage.act(delta)
        uiStage.draw()
    }

    override fun dispose() {
        logger.debug { "dispose" }
        uiStage.dispose()
        uiSkin.dispose()
        puzzlePieceDrawer.dispose()
        puzzleRenderer.dispose()
        hudRenderer.dispose()
        cursorManager.dispose()
        super.dispose()
    }

    private fun calculateResolutionRatio(): Float {
        val screenSize = Gdx.graphics.width.toFloat() to Gdx.graphics.height.toFloat()
        val viewPortSize = gameCamera.viewportWidth to gameCamera.viewportHeight
        return maxOf(screenSize.first / viewPortSize.first, screenSize.second / viewPortSize.second)
    }

    private fun getMousePositions(): Pair<Vector2, Vector2> {
        val screenX = Gdx.input.x.toFloat()
        val screenY = Gdx.input.y.toFloat()
        val renderVector = Vector2(screenX, Gdx.graphics.height - screenY)
        val worldVector = gameCamera.unproject(Vector3(screenX, screenY, 0f)).toVector2()
        return Pair(renderVector, worldVector)
    }
}
