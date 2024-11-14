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
import edu.b4kancs.languagePuzzleApp.app.view.screens.Constants
import edu.b4kancs.languagePuzzleApp.app.view.screens.GameScreen
import edu.b4kancs.languagePuzzleApp.app.view.screens.MainMenuScreen
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
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

        /* Run this if you need to prerender font .ttf font files. */
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

            bindSingleton<HudFont>(
                HudFont(Gdx.files.internal("fonts/hud_font.fnt"), false).apply {
                    data.setScale(1f * Gdx.graphics.density)
                    disposables.register(this)
                }
//                HudFont(Gdx.files.internal("fonts/pregen/32_liberation-mono.regular.ttf.fnt"), false)
            )

            bindSingleton<BitmapFont> {
//                val typeFontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/libre-baskerville.regular.ttf"))
//                val typeFontParameter = FreeTypeFontParameter().apply {
//                    size = 34
//                    flip = true
//                }
//                 typeFontGenerator.generateFont(typeFontParameter)
//                    .apply { disposables.register(this) }


//                BitmapFont(Gdx.files.internal("fonts/exp/libre-baskerville.fnt"), true).apply {
//                    data.setScale(0.11f * Gdx.graphics.density)
//                    disposables.register(this)
//                }

                BitmapFont(Gdx.files.internal("fonts/pregen/35_libre-baskerville.regular.ttf.fnt"), true)
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
            // It is important to initialize the gameModel late!
            gameModel = GameModel()
            bindSingleton(gameModel)
        }

        with(context) {
            // Register Screens
            addScreen(
                MainMenuScreen(
                    game = this@Game,
                    filePicker = inject()
                )
            )

            addScreen(
                GameScreen(
                    context = inject(),
                    batch = inject(),
                    assetManager = inject(),
                    gameViewport = inject(),
                    hudViewport = inject(),
                    gameCamera = inject(),
                    hudCamera = inject(),
                    gameModel = inject(),
                    environment = inject()
                )
            )
        }

        setScreen<MainMenuScreen>() // Set MainMenuScreen as the initial screen
        super.create()
    }

    fun startDemo() {
        logger.info { "Starting demo exercise" }
        // Initialize a demo exercise in gameModel
        // gameModel.loadDemoExercise()

        // Switch to GameScreen
        setScreen<GameScreen>()
    }

    fun loadExerciseFromDisk(fileHandle: FileHandle) {
        logger.info { "Loading exercise from file: ${fileHandle.path()}" }
        // Deserialize the exercise from the file and load it into gameModel
        // Example:
        // val json = Json { serializersModule = ... }
        // val exercise = json.decodeFromString<Exercise>(fileHandle.readString())
        // gameModel.loadExercise(exercise)

        // Switch to GameScreen
        setScreen<GameScreen>()
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
        disposables.register(bitmapFont)
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

class HudFont(fileHandle: FileHandle, flip: Boolean) : BitmapFont(fileHandle, flip)
