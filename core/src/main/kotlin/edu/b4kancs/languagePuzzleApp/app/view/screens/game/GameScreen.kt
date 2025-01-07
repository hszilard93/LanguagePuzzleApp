package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ExtendViewport
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawer
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
    private val game: Game
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

    private var shouldDisplayDebugInfo = false
        private set(value) {
            field = value
            logger.info { "shouldDisplayDebugInfo = $shouldDisplayDebugInfo" }
            hudRenderer.displayDebugInfo(field)
        }


    private val puzzleSnapHelper = PuzzleSnapHelper(gameModel)

    init {
        logger.debug { "init" }

        cameraController = CameraController(gameCamera, hudCamera, gameViewport, hudViewport, gameModel)
        cameraController.setupCameras()

        uiManager = UIManager(context, uiStage, uiSkin) {
            game.backToMenuScreen()
        }
        uiManager.initializeUI()

        puzzleManager = PuzzleManager(gameModel, puzzleSnapHelper, uiManager)

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
        val gameInputManager = GameInputManager(
            cameraController,
            puzzleManager,
            cursorManager,
            uiManager,
            environment,
            gameModel,
            realToVirtualResolutionRatio = calculateResolutionRatio(),
            setBackgroundColor = ::setBackgroundColor,
            toggleDebugInfo = { shouldDisplayDebugInfo = !shouldDisplayDebugInfo },
            displayCheckMark = { uiManager.displayCheckMark() }
        )
        inputMultiplexer.addProcessor(uiStage)
        inputMultiplexer.addProcessor(gameInputManager)
        Gdx.input.inputProcessor = inputMultiplexer
    }

    override fun show() {
        logger.debug { "show" }
        gameCamera.moveTo(gameCamera.position.toVector2().add(-100f, 100f))
        gameCamera.update()
        super.show()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }
        cameraController.resize(newWidth, newHeight)
//        (uiStage.viewport as ExtendViewport).
        uiStage.viewport.update(newWidth, newHeight, true)

        uiManager.updateFonts()
        uiManager.updateExerciseDescription()
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
