package edu.b4kancs.languagePuzzleApp.app.view.screens.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CursorManager
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.GameScreen
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadMenuFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadPuzzleBaseFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import ktx.app.KtxScreen
import ktx.inject.Context
import ktx.log.logger

class MainMenuScreen(
    context: Context,
    private val game: Game
) : KtxScreen {

    companion object {
        private val logger = logger<MainMenuScreen>()
    }

    private val viewPortDimensions = Vector2(1200f, 800f)
    private val viewport = ExtendViewport(viewPortDimensions.x, viewPortDimensions.y)

    private val uiSkin = Skin(Gdx.files.internal("skin/holo/uiskin.json"))
    private val cursorManager: CursorManager = context.inject()

    private val stage = Stage(viewport)
    private val buttonTable = Table()
    private val outerTable = Table()

    override fun show() {
        logger.debug { "MainMenuScreen: show" }

        Gdx.input.inputProcessor = stage

        outerTable.clear()

        buttonTable.clear()
        buttonTable.center()

        val fontMultiplier = maxOf(1200f / Gdx.graphics.width, 800f / Gdx.graphics.height)

        val menuButtons = mutableListOf<TextButton>()
        val fbMenuButton = TextButton("A Feladatbank feladatai", uiSkin).apply {
            style = createButtonStyle(fontMultiplier)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "'A Feladatbank feladatai' button clicked" }
                    game.loadFBMenuScreen(this@MainMenuScreen)
                }
            })
        }
        menuButtons.add(fbMenuButton)

        val tkMenuButton = TextButton("A tankönyv feladatai", uiSkin).apply {
            style = createButtonStyle(fontMultiplier)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "'A tankönyv feladatai' button clicked" }
                    game.loadTKMenuScreen(this@MainMenuScreen)
                }
            })
        }
        menuButtons.add(tkMenuButton)

        val userManualButton = TextButton("Használati útmutató (Olvass el!)", uiSkin).apply {
            style = createButtonStyle(fontMultiplier)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "'Használati útmutató' button clicked" }
//                    game.loadManualGuideScreen(this@MainMenuScreen)
                }
            })
        }
        menuButtons.add(userManualButton)

        menuButtons.forEach { textButton ->
            buttonTable.add(textButton).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(80f).pad(10f).row()
        }

        val footerLabelStyle = Label.LabelStyle(loadMenuFont(fontMultiplier), Color(0.1f, 0f, 0f, 0.9f))
        val footerLabel = Label("Jelen oktatási segédlet az MTA Domus Programja által támogatott tevékenység keretében jött létre.", footerLabelStyle).apply {
            setAlignment(Align.center)
            wrap = true
        }

        outerTable.apply {
            setFillParent(true)
            add().height(Gdx.graphics.height / 5f).row() // Spacing at the top
            add(buttonTable).growX().row()
            add().height(Gdx.graphics.height / 5f).row() // Spacing at the bottom

            add(footerLabel).growX().height(50f).padLeft(20f).padRight(20f).align(Align.bottom).row

            center()
            debug = false
        }

        stage.addActor(outerTable)
        cursorManager.setCursor(null)
    }


    private fun createButtonStyle(fontMultiplier: Float): TextButton.TextButtonStyle {
        val font = loadMenuFont(fontMultiplier)
        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        return TextButton.TextButtonStyle(uiSkin.get(TextButton.TextButtonStyle::class.java)).apply {
            this.font = font
        }
    }


    override fun render(delta: Float) {
        setBackgroundColor(180, 255, 180, 1f)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }
        val multiplier = maxOf(newWidth / 1200f, newHeight / 800f)
        updateFonts(multiplier)
        stage.viewport.update(newWidth, newHeight, true)
    }

    private fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Float) {
        GameScreen.logger.misc { "setBackgroundColor red=$red, green=$green, blue=$blue, alpha=$alpha" }
        Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    private fun updateFonts(multiplier: Float) {
        val font = loadMenuFont(multiplier)
        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        // No need to update menuButtons here, as this screen only has two buttons with fixed text.
    }

    override fun hide() {
        stage.clear()
    }

    override fun dispose() {
        uiSkin.dispose()
        stage.dispose()
    }
}
