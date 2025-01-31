package edu.b4kancs.languagePuzzleApp.app.view.screens.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.JsonReader
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
        private val logger = logger<MainMenuScreen>()
        private val PAGE_NUMBER_REGEX = Regex("FB2/(\\d+)(?:-\\d+)?[/]")
    }

    private val stage = Stage(viewport)
    private val buttonTable = Table() // Table for buttons (renamed for clarity)
    private val menuButtons = mutableListOf<TextButton>()
    private val jsonReader = JsonReader()

    override fun show() {
        logger.debug { "MainMenuScreen: show" }

        buttonTable.clear() // Clear the button table, not the outer table

        Gdx.input.inputProcessor = stage

        val fontMultiplier = maxOf(Gdx.graphics.width / 1200f, Gdx.graphics.height / 800f)

        buttonTable.center() // Center the button table content
        table.center()

        if (menuButtons.isEmpty()) {    // Load the exercises only once per instance.
            loadExercisesAndCreateButtons("assets/tasks/fb", fontMultiplier)

            menuButtons.sortBy { button ->
                extractPageNumber(button.text.toString()) ?: Int.MAX_VALUE
            }
            menuButtons.forEachIndexed { i, button ->
                button.setText("${i + 1}. feladat: ${button.text}")
            }
        }

        menuButtons.forEach { button ->
            buttonTable.add(button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(80f).pad(0f).row()
//            addManualButtons(fontMultiplier)
        }

        val scrollPane = ScrollPane(buttonTable, uiSkin).apply {
            fadeScrollBars = false
            setScrollbarsVisible(true)
            setScrollingDisabled(true, false)
            // Removed setFillParent(true) from ScrollPane
            width = buttonTable.width + 100f // Keep width setting if needed
            debug = true // Keep debug if needed
        }

        val outerTable = Table().apply {
            setFillParent(true) // Outer table fills the stage
            add().height(50f).row() // Top margin row
            add().width(Gdx.graphics.width / 5f)
            add(scrollPane).grow()     // ScrollPane in the middle row, grows to fill space
            add().width(Gdx.graphics.width / 5f).row()
            add().height(50f).row() // Bottom margin row
            debug = false // Set debug for outer table if needed
        }

        stage.addActor(outerTable) // Add the outer table to the stage

        cursorManager.setCursor(null)
    }

    private fun loadExercisesAndCreateButtons(path: String, fontMultiplier: Float) {
        logger.info { "Loading exercises from disk..." }
        val tasksDir = Gdx.files.internal(path) // Assuming exercises are in "tasks/" directory

        if (!tasksDir.exists() || !tasksDir.isDirectory) {
            logger.error { "Tasks directory '$path' not found or is not a directory." }
            return
        }

        val exerciseFiles = tasksDir.list(".json") // List only .json files

        if (exerciseFiles.isEmpty()) {
            logger.info { "No exercise files found in 'tasks/' directory." }
            // You might want to display a message to the user in the UI
            return
        }

        val buttonStyle = createButtonStyle(fontMultiplier) // Create button style once

        exerciseFiles.forEach { fileHandle ->
            try {
                logger.debug { "Trying to load exercise from: ${fileHandle.path()}" }
                val json = jsonReader.parse(fileHandle)
                val buttonDescription = json?.getString("buttonDescription")

                if (buttonDescription != null) {
                    val exerciseButton = TextButton(buttonDescription, uiSkin).apply {
                        style = buttonStyle
                        label.setAlignment(Align.left)
                        padLeft(120f)
                        menuButtons.add(this)
                    }

                    exerciseButton.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            logger.info { "Button '${buttonDescription}' clicked, loading exercise from: ${fileHandle.path()}" }
                            // Ensure that loadExerciseFromDisk runs on the LibGDX rendering thread
                            Gdx.app.postRunnable {
                                game.loadExerciseFromDisk(fileHandle)
                            }
                        }
                    })
                }
                else {
                    logger.error { "Exercise file '${fileHandle.path()}' is missing 'buttonDescription', skipping." }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error loading exercise file: ${fileHandle.path()}. Skipping file." }
            }
        }
    }

    private fun createButtonStyle(fontMultiplier: Float): TextButton.TextButtonStyle {
        val font = loadMenuFont(fontMultiplier)
        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        return TextButton.TextButtonStyle(uiSkin.get(TextButton.TextButtonStyle::class.java)).apply {
            this.font = font
        }
    }

    private fun addManualButtons(fontMultiplier: Float) {
        val buttonStyle = createButtonStyle(fontMultiplier)

        val loadExerciseButton = TextButton("Feladat betöltése fájlból", uiSkin).apply {
            style = buttonStyle
//            menuButtons.add(this)
        }
        val settingsButton = TextButton("Beallítások", uiSkin).apply {
            style = buttonStyle
//            menuButtons.add(this)
        }
        val exitButton = TextButton("Kilepés", uiSkin).apply {
            style = buttonStyle
//            menuButtons.add(this)
        }

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
                // game.setScreen<SettingsScreen>() // Implement SettingsScreen as needed
            }
        })

        exitButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                logger.info { "Exit button clicked" }
                Gdx.app.exit()
            }
        })
    }

    private fun extractPageNumber(buttonText: String): Int? {
        val matchResult = PAGE_NUMBER_REGEX.find(buttonText)
        return matchResult?.groups?.get(1)?.value?.toIntOrNull()
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
