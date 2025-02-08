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
import ktx.log.Logger
import ktx.log.logger
import java.io.BufferedReader
import java.io.IOException

abstract class AbstractExerciseMenuScreen(
    context: Context,
    protected val game: Game,
    protected val fileListPath: String // Path to the file list, now protected
) : KtxScreen {

    companion object {
        val logger = logger<AbstractExerciseMenuScreen>()
    }

    protected val environment = context.inject<Environment>()

    private val viewPortDimensions = Vector2(1200f, 800f)
    protected val viewport = ExtendViewport(viewPortDimensions.x, viewPortDimensions.y)

    protected val filePicker: FilePickerInterface = context.inject()
    protected val uiSkin = Skin(Gdx.files.internal("skin/holo/uiskin.json"))
    protected val hudFont = context.inject<HudFontHolder>().font
    protected val cursorManager: CursorManager = context.inject()

    protected val stage = Stage(viewport)
    protected val buttonTable = Table()
    protected val menuButtons = mutableListOf<TextButton>()
    protected val jsonReader = JsonReader()


    abstract val tasksWebPath: String // Abstract web path
    abstract val tasksDesktopPath: String // Abstract desktop path
    abstract val screenLogger: Logger

    abstract val menuButtonWidth: Float

    override fun show() {
        screenLogger.debug { "${this::class.simpleName}: show" }

        Gdx.input.inputProcessor = stage

        val fontMultiplier = maxOf(1200f / Gdx.graphics.width, 800f / Gdx.graphics.height)

        buttonTable.clear()
        buttonTable.center()

        // Add Back Button - Common to all menu screens
        val backButton = TextButton("<<- Vissza a főmenübe", uiSkin).apply {
            style = createButtonStyle(fontMultiplier)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    game.loadMainMenuScreen()   // It's the common path for all sub-menu screens
                }
            })
        }
        buttonTable.add(backButton).width(300f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(80f).padTop(20f).row()


        if (menuButtons.isEmpty()) {
            loadExercisesAndCreateButtons(fontMultiplier)
            menuButtons.sortWith(
                compareBy(
                    { extractPageNumber(it.text.toString()) ?: Int.MAX_VALUE },
                    { it.text.toString() }
                )
            )
            menuButtons.forEachIndexed { i, button ->
                val newText = button.text.toString().replace("FB2/", "").replace("VI_", "")
                button.setText("${i + 1}. feladat: $newText")
            }
        }

        menuButtons.forEach { button ->
            buttonTable.add(button).width(menuButtonWidth * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(80f).pad(0f).row()
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

        stage.addActor(outerTable)
        stage.setScrollFocus(scrollPane)
        cursorManager.setCursor(null)
    }


    private fun loadExercisesAndCreateButtons(fontMultiplier: Float) {
        screenLogger.info { "Loading exercises from file list: $fileListPath" }
        val fileListHandle = Gdx.files.internal(fileListPath)

        if (!fileListHandle.exists()) {
            screenLogger.error { "File list not found: $fileListPath" }
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
            screenLogger.error(e) { "Error reading file list: $fileListPath" }
            return
        }

        if (fileNames.isEmpty()) {
            screenLogger.info { "No filenames found in $fileListPath." }
            return
        }

        val tasksBasePath = if (environment.platform == Platform.WEB) tasksWebPath else tasksDesktopPath

        fileNames.forEach { fileName ->
            val exerciseFilePath = "$tasksBasePath/$fileName"
            val fileHandle = Gdx.files.internal(exerciseFilePath)

            try {
                screenLogger.debug { "Trying to load exercise from: ${fileHandle.path()}" }
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
                            screenLogger.info { "Button '${buttonDescription}' clicked, loading exercise from: ${fileHandle.path()}" }
                            Gdx.app.postRunnable {
                                game.loadExerciseFromDisk(fileHandle)
                                game.loadGameScreen(this@AbstractExerciseMenuScreen)
                            }
                        }
                    })
                } else {
                    screenLogger.error { "Exercise file '${fileHandle.path()}' is missing 'buttonDescription', skipping." }
                }
            } catch (e: Exception) {
                screenLogger.error(e) { "Error loading exercise file: ${fileHandle.path()}. Skipping file." }
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

    private fun extractPageNumber(buttonText: String): Int? { // Made protected
        val pageNumberRegex = Regex(".+[/_](\\d+)(?:[-–]\\d+)?[/]") // Example regex, adjust as needed
        val matchResult = pageNumberRegex.find(buttonText)
        return matchResult?.groups?.get(1)?.value?.toIntOrNull()
    }

    override fun render(delta: Float) {
        setBackgroundColor(180, 255, 180, 1f)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        screenLogger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }
        val multiplier = maxOf(newWidth / 1200f, newHeight / 800f)
        updateFonts(multiplier)
        stage.viewport.update(newWidth, newHeight, true)
    }

    protected fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Float) { // Made protected
        GameScreen.logger.misc { "setBackgroundColor red=$red, green=$green, blue=$blue, alpha=$alpha" }
        Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    protected fun updateFonts(multiplier: Float) { // Made protected
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
