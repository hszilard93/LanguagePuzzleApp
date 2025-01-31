package edu.b4kancs.languagePuzzleApp.app

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.utils.viewport.ExtendViewport
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.EnvironmentalImplementations
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.other.gdxSmartFontMaster.SmartFontGenerator
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.Constants
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CursorManager
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.GameScreen
import edu.b4kancs.languagePuzzleApp.app.view.screens.menu.MainMenuScreen
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
import edu.b4kancs.languagePuzzleApp.app.view.utils.HudFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.PuzzleFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.TaskFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.UIFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadFreeTypeFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadPuzzleBaseFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadPuzzleTabFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadTaskCounterFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadTaskDescriptionFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadUIFont
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.DisposableContainer
import ktx.assets.disposeSafely
import ktx.inject.Context
import ktx.inject.register
import ktx.log.logger


class Game(
    private val environment: Environment,
    private val environmentalImplementations: EnvironmentalImplementations
) : KtxGame<KtxScreen>() {

    private val context = Context()
    private val hudCamera = HudCamera()
    private val gameCamera = GameCamera()
    private val disposables = DisposableContainer()
    private lateinit var gameModel: GameModel

    companion object {
        const val LOG_LEVEL = com.badlogic.gdx.utils.Logger.DEBUG
        const val IS_DEBUG_MODE_ON = true
        val logger = logger<Game>()
    }

    override fun create() {
        Gdx.app.logLevel = LOG_LEVEL
        logger.debug { "create" }

        /* Used to be needed to prerender font .ttf font files. */
//        generateAndExportBitmapFont("fonts/Roboto-Regular.ttf", 24)
        /* */

        disposables.register(context)
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val aspectRatio = screenHeight / screenWidth

        val gameVirtualWidth: Float
        val gameVirtualHeight: Float
        val gameMinWorldWidth: Float
        val gameMinWorldHeight: Float
        val gameMaxWorldWidth: Float
        val gameMaxWorldHeight: Float
        if (environment.isMobile) {
            gameVirtualWidth = Constants.GAME_MOBILE_VIRTUAL_WIDTH
            gameVirtualHeight = Constants.GAME_MOBILE_VIRTUAL_HEIGHT
            gameMinWorldWidth = Constants.GAME_MOBILE_MIN_WORLD_WIDTH
            gameMinWorldHeight = Constants.GAME_MOBILE_MIN_WORLD_HEIGHT
            gameMaxWorldWidth = Constants.GAME_MOBILE_MAX_WORLD_WIDTH
            gameMaxWorldHeight = Constants.GAME_MOBILE_MAX_WORLD_HEIGHT
        }
        else {
            gameVirtualWidth = Constants.GAME_VIRTUAL_WIDTH
            gameVirtualHeight = Constants.GAME_VIRTUAL_HEIGHT
            gameMinWorldWidth = Constants.GAME_MIN_WORLD_WIDTH
            gameMinWorldHeight = Constants.GAME_MIN_WORLD_HEIGHT
            gameMaxWorldWidth = Constants.GAME_MAX_WORLD_WIDTH
            gameMaxWorldHeight = Constants.GAME_MAX_WORLD_HEIGHT
        }

        context.register {
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(AssetManager())
            bindSingleton(environment)
            bindSingleton<FilePickerInterface>(environmentalImplementations.filePickerImpl)

            bindSingleton(gameCamera.apply {
                setToOrtho(false, gameVirtualWidth, gameVirtualHeight)
            })

            bindSingleton(hudCamera.apply {
                setToOrtho(false, gameVirtualWidth, gameVirtualHeight)
            })

            bindSingleton<HudFontHolder> {
                val font = loadFreeTypeFont("Roboto-Regular.ttf", 12)
                disposables.register(font)
                HudFontHolder(font)
            }

            bindSingleton<UIFontHolder> {
                val font = loadUIFont()
                disposables.register(font)
                UIFontHolder(font)
            }

            bindSingleton<TaskFontHolder> {
                val descFont = loadTaskDescriptionFont()
                val countFont = loadTaskCounterFont()
                disposables.register(descFont)
                disposables.register(countFont)
                TaskFontHolder(descFont, countFont)
            }

            bindSingleton<PuzzleFontHolder> {

                val baseFont = loadPuzzleBaseFont()
                disposables.register(baseFont)

                val tabFont = loadPuzzleTabFont()
                disposables.register(tabFont)

                PuzzleFontHolder(baseFont, tabFont)
            }

            bindSingleton<GameViewport>(
                GameViewport(
                    gameMinWorldWidth,
                    gameMinWorldHeight,
                    gameMaxWorldWidth,
                    gameMaxWorldHeight,
                    gameCamera
                )
            )

            bindSingleton<HudViewport>(
                HudViewport(
                    gameMinWorldWidth,
                    gameMinWorldHeight,
                    gameMaxWorldWidth,
                    gameMaxWorldHeight,
                    hudCamera
                )
            )
            bindSingleton<CursorManager>(CursorManager(environment))

            // It is important to initialize the gameModel late!
            gameModel = GameModel()
            bindSingleton(gameModel)
        }

        loadMainMenuScreen()
        super.create()
    }

    private fun loadMainMenuScreen() {
        logger.info { "loadMainMenuScreen" }

        this.removeScreen<GameScreen>()
        addScreen(
            MainMenuScreen(
                context,
                this@Game
            )
        )

        setScreen<MainMenuScreen>() // Set MainMenuScreen as the initial screen
    }

    private fun loadGameScreen() {
        logger.info { "loadGameScreen" }

        this.removeScreen<MainMenuScreen>()
        addScreen(
//                OldGameScreen(
            GameScreen(
                context,
                this@Game
            )
        )

//        setScreen<OldGameScreen>()
        setScreen<GameScreen>()
    }

    fun backToMenuScreen() {
        logger.info { "backToMenu" }
        this.removeScreen<GameScreen>()
        loadMainMenuScreen()
    }

    fun loadExerciseFromDisk(fileHandle: FileHandle) {
        logger.info { "Loading exercise from file: ${fileHandle.path()}" }

        gameModel.loadExerciseFromDisk(fileHandle)

        // Switch to GameScreen
        loadGameScreen()
    }

    override fun dispose() {
        logger.debug { "dispose" }
        disposables.disposeSafely()
        super.dispose()
    }

    private fun generateBitmapFont(): BitmapFont {
        val fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/libre-baskerville.regular.ttf"))
        val parameter = FreeTypeFontParameter().apply {
            size = 34
            flip = true
            // Add other parameters as needed
        }
        val bitmapFont = fontGenerator.generateFont(parameter)
            .apply { disposables.register(this) }

        fontGenerator.dispose()

        return bitmapFont
    }

    private fun generateAndExportBitmapFont(fileName: String, fontSize: Int): BitmapFont {
        logger.info { "generateAndExportBitmapFont fileName=$fileName" }
        val fontGenerator = SmartFontGenerator()
        val fileHandle = Gdx.files.internal(fileName)
        val font = fontGenerator.createFont(
            fileHandle,
            fileName,
            fontSize,
            flip = true,
            forceGenerate = true
        )
        return font
    }
}

// Due to the simplistic DI system's constraints, named classes are necessary where multiple singletons of the same type have to be used.
class GameCamera : OrthographicCamera()

class HudCamera : OrthographicCamera()

class GameViewport(minWorldWidth: Float, minWorldHeight: Float, maxWorldWidth: Float, maxWorldHeight: Float, camera: Camera) :
    ExtendViewport(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, camera)

class HudViewport(minWorldWidth: Float, minWorldHeight: Float, maxWorldWidth: Float, maxWorldHeight: Float, camera: Camera) :
    ExtendViewport(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, camera)

