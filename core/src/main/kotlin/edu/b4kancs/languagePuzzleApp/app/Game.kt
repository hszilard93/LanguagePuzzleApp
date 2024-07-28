package edu.b4kancs.languagePuzzleApp.app

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FillViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
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
class Game(environment: Environment) : KtxGame<KtxScreen>() {
    private val context = Context()
    private val hudCamera = HudCamera()
    private val gameCamera = GameCamera()
    private val disposables = ArrayList<Disposable>()
    private val gameModel = GameModel(environment)

    companion object {
        val logger = logger<Game>()
    }

    override fun create() {
        Gdx.app.logLevel = GameModel.LOG_LEVEL
        logger.debug { "create" }

        disposables.add(context)
        val viewportWidth = 1200f
        val viewportHeight = 800f

        context.register {
            bindSingleton(gameModel)
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(AssetManager())

            bindSingleton(gameCamera.apply {
                setToOrtho(false, viewportWidth, viewportHeight)
                position.set(viewportWidth / 2, viewportHeight / 2, 0f)
            })

            bindSingleton(hudCamera.apply {
                setToOrtho(false, viewportWidth, viewportHeight)
                position.set(viewportWidth / 2, viewportHeight / 2, 0f)
            })

            bindSingleton<FillViewport>(FillViewport(viewportWidth, viewportHeight, gameCamera))
            bindSingleton<FitViewport>(FitViewport(viewportWidth, viewportHeight, hudCamera))
        }
        with(context) {
            addScreen(GameScreen(inject(), inject(), inject(), inject(), inject(), inject(), inject(), inject()))
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

class GameCamera : OrthographicCamera()

class HudCamera : OrthographicCamera()
