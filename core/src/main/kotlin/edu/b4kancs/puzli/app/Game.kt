package edu.b4kancs.puzli.app

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import edu.b4kancs.puzli.app.model.Environment
import edu.b4kancs.puzli.app.model.GameModel
import edu.b4kancs.puzli.app.view.screen.GameScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.inject.Context
import ktx.inject.register


/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms. */
class Game(environment: Environment) : KtxGame<KtxScreen>() {
    private val context = Context()
    private val hudCamera = HudCamera()
    private val gameCamera = GameCamera()
    private val disposables = ArrayList<Disposable>()
    private val gameModel = GameModel(environment)

    override fun create() {
        disposables.add(context)
        Gdx.app.logLevel = GameModel.LOG_LEVEL

        val screenHeight = Gdx.graphics.height.toFloat()
        val screenWidth = Gdx.graphics.width.toFloat()
        val viewportWidth = 1200f
        val viewportHeight = 800f

        context.register {
            bindSingleton(gameModel)
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(AssetManager())

            bindSingleton(gameCamera.apply {
                setToOrtho(false, viewportWidth, viewportHeight)
                position.set(viewportWidth / 2, viewportHeight / 2, 0f)
            } )

            bindSingleton(hudCamera.apply {
                setToOrtho(false, viewportWidth, viewportHeight)
                position.set(viewportWidth / 2, viewportHeight / 2, 0f)
            } )
            bindSingleton<Viewport>(FitViewport(viewportWidth, viewportHeight, gameCamera))
        }
        with(context) {
            addScreen(GameScreen(inject(), inject(), inject(), inject(), inject(), inject()))
        }
        setScreen<GameScreen>()
        super.create()
    }

    override fun dispose() {
        disposables.disposeSafely()
        super.dispose()
    }
}

class GameCamera : OrthographicCamera()

class HudCamera : OrthographicCamera()
