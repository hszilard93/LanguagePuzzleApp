package edu.b4kancs.languagePuzzleApp.app

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ExtendViewport
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

        context.register {
            bindSingleton(gameModel)
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(AssetManager())

            bindSingleton(gameCamera.apply {
                setToOrtho(false, screenWidth, screenHeight)
            })

            bindSingleton(hudCamera.apply {
                setToOrtho(false, screenWidth, screenHeight)
//                position.set(gameViewportWidth / 2, gameViewportHeight / 2, 0f)
            })

            bindSingleton<GameViewport>(GameViewport(screenWidth, screenHeight, gameCamera).apply {
                setScaling(Scaling.contain)
            })
            bindSingleton<HudViewport>(HudViewport(screenWidth, screenHeight, hudCamera).apply {
                setScaling(Scaling.contain)
            })
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

class GameViewport(width: Float, height: Float, camera: Camera) : ExtendViewport(width, height, camera)

class HudViewport(width: Float, height: Float, camera: Camera) : ExtendViewport(width, height, camera)
