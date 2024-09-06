package edu.b4kancs.languagePuzzleApp.app

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.view.screen.GameScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.inject.Context
import ktx.inject.register
import ktx.log.logger


/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms. */
class Game(val environment: Environment) : KtxGame<KtxScreen>() {
    private val context = Context()
    private val hudCamera = HudCamera()
    private val gameCamera = GameCamera()
    private val disposables = ArrayList<Disposable>()
    private val gameModel = GameModel()

    companion object {
        const val WORLD_WIDTH = 2000
        const val WORLD_HEIGHT = 2000
        const val LOG_LEVEL = com.badlogic.gdx.utils.Logger.DEBUG
        const val IS_DEBUG_MODE_ON = true
        val logger = logger<Game>()
    }

    override fun create() {
        Gdx.app.logLevel = LOG_LEVEL
        logger.debug { "create" }

        disposables.add(context)
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        val aspectRatio = screenHeight / screenWidth

        val gameViewportWidth = WORLD_WIDTH / 2f
        val gameViewportHeight = gameViewportWidth * aspectRatio

        context.register {
            bindSingleton(gameModel)
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(AssetManager())

            bindSingleton(gameCamera.apply {
            })

            bindSingleton(hudCamera.apply {
                setToOrtho(false, screenWidth, screenHeight)
                position.set(gameViewportWidth / 2, gameViewportHeight / 2, 0f)
            })

            bindSingleton<GameViewport>(
                GameViewport(WORLD_WIDTH.toFloat(), WORLD_HEIGHT.toFloat(), gameCamera).apply {
                    worldWidth = 1200f//WORLD_WIDTH.toFloat()
                    worldHeight = 800f//WORLD_HEIGHT.toFloat()
                }
            )
            bindSingleton<HudViewport>(HudViewport(screenWidth, screenHeight, hudCamera))
        }

        with(context) {
            addScreen(
                GameScreen(
                    context = inject(),
                    batch = inject(),
                    assetManager = inject(),
                    gameViewport = inject(),
                    hudViewport = inject(),
                    gameCamera = inject(),
                    hudCamera = inject(),
                    gameModel = inject()
                )
            )
        }
        setScreen<GameScreen>()
        super.create()
    }

    override fun dispose() {
        logger.debug { "dispose" }
        disposables.disposeSafely()
        super.dispose()
    }
}

// Due to the simplistic DI system's constraints, named classes are necessary where multiple singletons of the same type have to be used.
class GameCamera : OrthographicCamera()

class HudCamera : OrthographicCamera()

class GameViewport(width: Float, height: Float, camera: Camera) : FitViewport(width, height, camera)

class HudViewport(width: Float, height: Float, camera: Camera) : FitViewport(width, height, camera)
