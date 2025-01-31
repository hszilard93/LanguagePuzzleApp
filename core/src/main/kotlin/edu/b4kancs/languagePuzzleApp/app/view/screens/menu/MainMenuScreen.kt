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
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.Platform
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CursorManager
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.GameScreen
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
import edu.b4kancs.languagePuzzleApp.app.view.utils.HudFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadMenuFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import ktx.app.KtxScreen
import ktx.inject.Context
import ktx.log.logger
import java.io.BufferedReader
import java.io.IOException

class MainMenuScreen(
    context: Context,
    private val game: Game
) : KtxScreen {

    companion object {
        private val logger = logger<MainMenuScreen>()
    }

    private val environment = context.inject<Environment>()

    private val viewPortDimensions = Vector2(1200f, 800f)
    private val viewport = ExtendViewport(viewPortDimensions.x, viewPortDimensions.y)

    private val filePicker: FilePickerInterface = context.inject()
    private val uiSkin = Skin(Gdx.files.internal("skin/holo/uiskin.json"))
    private val hudFont = context.inject<HudFontHolder>().font
    private val cursorManager: CursorManager = context.inject()

    private val stage = Stage(viewport)
    private val buttonTable = Table() // Table for buttons (renamed for clarity)
    private val menuButtons = mutableListOf<TextButton>()
    private val jsonReader = JsonReader()

    private val fbTasksWebPath = "tasks/fb" // Web path prefix. Does not work.
    private val fbTasksDesktopPath = "assets/tasks/fb" // Desktop path prefix
    private val fileListPath = "tasks/fb_task_list.txt" // Path to the file list

    override fun show() {
        logger.debug { "MainMenuScreen: show" }

        buttonTable.clear() // Clear the button table, not the outer table

        Gdx.input.inputProcessor = stage

        val fontMultiplier = maxOf(1200f / Gdx.graphics.width, 800f / Gdx.graphics.height) //* 1.5f

        buttonTable.center() // Center the button table content

        if (menuButtons.isEmpty()) {    // Load the exercises only once per instance.
            loadExercisesAndCreateButtons(fontMultiplier) // Modified to not take path as argument

            menuButtons.sortBy { button ->
                extractPageNumber(button.text.toString()) ?: Int.MAX_VALUE
            }
            menuButtons.forEachIndexed { i, button ->
                button.setText("${i + 1}. feladat: ${button.text}")
            }
        }

        menuButtons.forEach { button ->
            buttonTable.add(button).width(600f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(80f).pad(0f).row()
        }

        val scrollPane = ScrollPane(buttonTable, uiSkin).apply {
            fadeScrollBars = false
            setScrollbarsVisible(true)
            setScrollingDisabled(true, false)
            width = buttonTable.width + 100f
            debug = false
        }

        val outerTable = Table().apply {
            setFillParent(true)
            add().height(50f).row()
            add().width(Gdx.graphics.width / 5f)
            add(scrollPane).grow()
            add().width(Gdx.graphics.width / 5f).row()
            add().height(50f).row()
            debug = false
        }

        stage.addActor(outerTable) // Add the outer table to the stage
        stage.setScrollFocus(scrollPane)

        cursorManager.setCursor(null)
    }

    // I am using a pregenerated file list because the Gdx.files.internal works incorrectly in TeaVM in the case of directories.
    private fun loadExercisesAndCreateButtons(fontMultiplier: Float) {
        logger.info { "Loading exercises from file list: $fileListPath" }
        val fileListHandle = Gdx.files.internal(fileListPath)

        if (!fileListHandle.exists()) {
            logger.error { "File list not found: $fileListPath" }
            return
        }

        val buttonStyle = createButtonStyle(fontMultiplier)
        val fileNames = mutableListOf<String>()

        try {
            BufferedReader(fileListHandle.reader()).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val fileName = line.trim()
                    if (fileName.isNotBlank()) {
                        fileNames.add(fileName)
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Error reading file list: $fileListPath" }
            return
        }

        if (fileNames.isEmpty()) {
            logger.info { "No filenames found in $fileListPath." }
            return
        }

        val tasksBasePath = if (environment.platform == Platform.WEB) fbTasksWebPath else fbTasksDesktopPath

        fileNames.forEach { fileName ->
            val exerciseFilePath = "$tasksBasePath/$fileName" // Construct full path
            val fileHandle = Gdx.files.internal(exerciseFilePath)

            try {
                logger.debug { "Trying to load exercise from: ${fileHandle.path()}" }
                val json = jsonReader.parse(fileHandle)
                val buttonDescription = json?.getString("buttonDescription")

                if (buttonDescription != null) {
                    val exerciseButton = TextButton(buttonDescription, uiSkin).apply {
                        style = buttonStyle
                        label.setAlignment(Align.left)
                        padLeft(150f)
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
                } else {
                    logger.error { "Exercise file '${fileHandle.path()}' is missing 'buttonDescription', skipping." }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error loading exercise file: ${fileHandle.path()}. Skipping file." }
            }
        }
    }

    // Helps debug path errors
//    private fun logFolderStructure(fileHandle: FileHandle, indent: String = "") {
//        if (fileHandle.isDirectory) {
//            logger.error { "$indent[D] ${fileHandle.path()}\n" }
//            fileHandle.list().forEach { child ->
//                logFolderStructure(child, "$indent  ")
//            }
//        } else {
//            logger.error { "$indent[F] ${fileHandle.path()}\n" }
//        }
//    }

    private fun createButtonStyle(fontMultiplier: Float): TextButton.TextButtonStyle {
        val font = loadMenuFont(fontMultiplier)
        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        return TextButton.TextButtonStyle(uiSkin.get(TextButton.TextButtonStyle::class.java)).apply {
            this.font = font
        }
    }

    private fun extractPageNumber(buttonText: String): Int? {
        val pageNumberRegex = Regex("FB2/(\\d+)(?:[-–]\\d+)?[/]")
        val matchResult = pageNumberRegex.find(buttonText)
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
