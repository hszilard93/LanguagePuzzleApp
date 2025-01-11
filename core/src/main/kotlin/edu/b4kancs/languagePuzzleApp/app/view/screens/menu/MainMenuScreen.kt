package edu.b4kancs.languagePuzzleApp.app.view.screens.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CursorManager
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.GameScreen
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
import edu.b4kancs.languagePuzzleApp.app.view.utils.HudFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadMenuFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import ktx.app.KtxScreen
import ktx.inject.Context
import ktx.log.logger

class MainMenuScreen(
    context: Context,
    private val game: Game
) : KtxScreen {

    private val viewPortDimensions = Vector2(1200f, 800f)
    private val viewport = ExtendViewport(viewPortDimensions.x, viewPortDimensions.y)

    private val filePicker: FilePickerInterface = context.inject()
    private val uiSkin = Skin(Gdx.files.internal("skin/holo/uiskin.json"))
    private val hudFont = context.inject<HudFontHolder>().font
    private val cursorManager: CursorManager = context.inject()

    companion object {
        val logger = logger<MainMenuScreen>()
    }

    private val stage = Stage(viewport)

    private val menuButtons = mutableSetOf<TextButton>()

    override fun show() {
        logger.debug { "MainMenuScreen: show" }
        Gdx.input.inputProcessor = stage

        val fontMultiplier = maxOf(Gdx.graphics.width / 1200f, Gdx.graphics.height / 800f)

        // Create a table to organize buttons
        val table = Table().apply {
            setFillParent(true)
            center()
        }

        // Create buttons
        val startExercise1Button = TextButton("Indítás az 1. példafeladattal", uiSkin).apply { menuButtons.add(this) }
        val startExercise2Button = TextButton("Indítás a 2. példafeladattal", uiSkin).apply { menuButtons.add(this) }
        val startExercise3Button = TextButton("Indítás a 3. példafeladattal", uiSkin).apply { menuButtons.add(this) }
        val startExercise4Button = TextButton("1. feladat. Töltsd ki! (FB2 38–41 3.III.2.A)", uiSkin).apply { menuButtons.add(this) }
        val startExercise5Button = TextButton("2. feladat. Rakd ki! (FB2 38–41 3.III.2.B)", uiSkin).apply { menuButtons.add(this) }
        val startExercise6Button = TextButton("3. feladat. Rakd ki! (FB2 38–41 3.III.2.B #3)", uiSkin).apply { menuButtons.add(this) }
        val startExercise7Button = TextButton("4. feladat. Töltsd ki! (FB2 41-42 3.IV.1.b)", uiSkin).apply { menuButtons.add(this) }
        val startExercise8Button = TextButton("5. feladat. Rakd ki! (FB2 44 3.V.1)", uiSkin).apply { menuButtons.add(this) }
        val startExercise9Button = TextButton("6. feladat. Rakd ki! (FB2 145 28a #1)", uiSkin).apply { menuButtons.add(this) }

        val loadExerciseButton = TextButton("Feladat betöltése fájlból", uiSkin).apply { menuButtons.add(this) }
        val settingsButton = TextButton("Beallítások", uiSkin).apply { menuButtons.add(this) }
        val exitButton = TextButton("Kilepés", uiSkin).apply { menuButtons.add(this) }

        val buttonStyle = startExercise1Button.style.apply {
            font = loadMenuFont(fontMultiplier)
            font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        menuButtons.forEach { it.style = buttonStyle }

        startExercise1Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 1 button clicked" }
                game.startDemo1()
            }
        })

        startExercise2Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 2 button clicked" }
                game.startDemo2()
            }
        })

        startExercise3Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 3 button clicked" }
                game.startDemo3()
            }
        })

        startExercise4Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 4 button clicked" }
                game.startDemo4()
            }
        })

        startExercise5Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 5 button clicked" }
                game.startDemo5()
            }
        })

        startExercise6Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 6 button clicked" }
                game.startDemo6()
            }
        })

        startExercise7Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 7 button clicked" }
                game.startDemo7()
            }
        })

        startExercise8Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 8 button clicked" }
                game.startDemo8()
            }
        })

        startExercise9Button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with exercise 9 button clicked" }
                game.startDemo9()
            }
        })

        loadExerciseButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Load Exercise from Disk button clicked" }
                filePicker.openFileChooser { fileHandle ->
                    logger.info { "Selected file: ${fileHandle.path()}" }
                    // Ensure that loadExerciseFromDisk runs on the LibGDX rendering thread
                    Gdx.app.postRunnable {
                        game.loadExerciseFromDisk(fileHandle)
                    }
                }
            }
        })

        settingsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Settings button clicked" }
//                game.setScreen<SettingsScreen>() // Implement SettingsScreen as needed
            }
        })

        exitButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Exit button clicked" }
                Gdx.app.exit()
            }
        })

        // Add buttons to the table with spacing
//        table.add(startExercise1Button).width(600f).height(100f).pad(10f).row()
//        table.add(startExercise2Button).width(600f).height(100f).pad(10f).row()
//        table.add(startExercise3Button).width(600f).height(100f).pad(10f).row()
        table.add(startExercise4Button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(100f).pad(10f).row()
        table.add(startExercise5Button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(100f).pad(10f).row()
        table.add(startExercise6Button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(100f).pad(10f).row()
        table.add(startExercise7Button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(100f).pad(10f).row()
        table.add(startExercise8Button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(100f).pad(10f).row()
        table.add(startExercise9Button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(100f).pad(10f).row()
//        table.add(settingsButton).width(400f).height(100f).pad(10f).row()
//        table.add(exitButton).width(400f).height(100f).pad(10f).row()

        // Add the table to the stage
        stage.addActor(table)

        cursorManager.setCursor(null)
    }

    override fun render(delta: Float) {
        // Clear the screen with a background color
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

        menuButtons.forEach { button ->
            button.style.font
            button.style = button.style
        }
    }

    override fun hide() {
        stage.clear()
    }

    override fun dispose() {
        uiSkin.dispose()
        stage.dispose()
    }
}
