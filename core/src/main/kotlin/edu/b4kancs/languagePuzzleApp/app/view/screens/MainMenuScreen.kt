package edu.b4kancs.languagePuzzleApp.app.view.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.view.screens.GameScreen.Companion
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import ktx.app.KtxScreen
import ktx.log.logger

class MainMenuScreen(
    private val game: Game, // Reference to the main Game class to switch screens
    private val filePicker: FilePickerInterface // For loading exercises
) : KtxScreen {

    private val uiSkin = Skin(Gdx.files.internal("skin/holo/uiskin.json"))

    companion object {
        val logger = logger<MainMenuScreen>()
    }

    private val stage = Stage()

    override fun show() {
        logger.debug { "MainMenuScreen: show" }
        Gdx.input.inputProcessor = stage

        // Create a table to organize buttons
        val table = Table().apply {
            setFillParent(true)
            center()
        }

        // Create buttons
        val startDemoButton = TextButton("Indítás a kezdőfeladattal", uiSkin) // Use appropriate style
        val loadExerciseButton = TextButton("Feladat betöltése fájlból", uiSkin)
        val settingsButton = TextButton("Beallítások", uiSkin)
        val exitButton = TextButton("Kilepés", uiSkin)

        startDemoButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Start with the Demo button clicked" }
                game.startDemo()
            }
        })

        loadExerciseButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Load Exercise from Disk button clicked" }
                filePicker.openFileChooser { fileHandle ->
                    logger.info { "Selected file: ${fileHandle.path()}" }
                    game.loadExerciseFromDisk(fileHandle)
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
        table.add(startDemoButton).width(400f).height(100f).pad(10f).row()
        table.add(loadExerciseButton).width(400f).height(100f).pad(10f).row()
        table.add(settingsButton).width(400f).height(100f).pad(10f).row()
        table.add(exitButton).width(400f).height(100f).pad(10f).row()

        // Add the table to the stage
        stage.addActor(table)
    }

    override fun render(delta: Float) {
        // Clear the screen with a background color
        setBackgroundColor(180, 255, 180, 1f)
        stage.act(delta)
        stage.draw()
    }

    private fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Float) {
        GameScreen.logger.misc { "setBackgroundColor red=$red, green=$green, blue=$blue, alpha=$alpha" }
        Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    override fun hide() {
        stage.clear()
    }

    override fun dispose() {
        stage.dispose()
    }
}
