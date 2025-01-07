package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.view.ui.TextEditorPopup
import edu.b4kancs.languagePuzzleApp.app.view.utils.TaskFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadTaskFont
import ktx.inject.Context

class UIManager(
    private val context: Context,
    private val uiStage: Stage,
    private val uiSkin: Skin,
    private val onBack: () -> Unit
) {

    companion object {
        val logger = ktx.log.logger<UIManager>()
    }

    var currentPopupWindow: Window? = null

    private val hudViewport: HudViewport = context.inject()
    private val gameViewport: GameViewport = context.inject()
    private val gameModel: GameModel = context.inject()
    private val taskFont = context.inject<TaskFontHolder>().font

    lateinit var exerciseDescriptionLabel: Label
        private set
    lateinit var returnButton: ImageButton
        private set
    lateinit var checkMarkImage: Image
        private set
    private lateinit var topBarTable: Table


    private var tabIcon = Texture(Gdx.files.internal("puzzle_tab_general_1.png"), Pixmap.Format.RGBA8888, true)
    private var backIcon = Texture(Gdx.files.internal("back_button_1.png"), Pixmap.Format.RGBA8888, true)

    fun initializeUI() {
        logger.debug { "initializeUI" }
        initializeTopBar()
        initializeCheckMarkUI()
    }

    private fun initializeTopBar() {
        logger.debug { "Initializing Top Bar UI." }

        topBarTable = Table().apply {
            setFillParent(true) // Make the table occupy the entire stage
            padTop(4f)
            padLeft(4f)
            padRight(24f)
            top().left() // Align content to the top-left

            //debug = true // Uncomment for debugging table layout
        }

        // Create the label for the description text
        exerciseDescriptionLabel = Label("", uiSkin).apply {
            setWrap(true) // Enable text wrapping
            setAlignment(Align.center) // Center-align the text
        }
        exerciseDescriptionLabel.style.background = uiSkin.getDrawable("white")
        exerciseDescriptionLabel.color.a = 0.75f
        exerciseDescriptionLabel.style.font = taskFont
//        exerciseDescriptionLabel.fontScaleX = 0.2f
//        exerciseDescriptionLabel.fontScaleY = 0.2f
        // ↓ This needs to be done or it wont work ↓
        exerciseDescriptionLabel.style = exerciseDescriptionLabel.style

        val buttonStyle = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(backIcon)).tint(Color(120f, 120f, 255f, 0.6f))
            this.down = TextureRegionDrawable(TextureRegion(backIcon)).tint(Color(120f, 120f, 255f, 1f))
        }
        returnButton = ImageButton(buttonStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "Back button clicked" }
                    onBack()
                }
            })
        }

        // Add label and button to the table
        topBarTable.add(exerciseDescriptionLabel).expandX().fillX().padRight(10f)
        topBarTable.add(returnButton).width(72f).height(72f).pad(12f)

        // Add the table to the UI stage
        uiStage.addActor(topBarTable)

        // Update the description based on the current exercise
        updateExerciseDescription()
    }

    fun updateExerciseDescription() {
        val currentExercise = gameModel.currentExercise
        if (currentExercise!!.taskDescription.isNotBlank()) {
            // Set the description text
            exerciseDescriptionLabel.setText(currentExercise.taskDescription)

            // Make sure the table is visible
            topBarTable.isVisible = true
        }
        else {
            // Hide the table if there's no description
            topBarTable.isVisible = false
        }
    }

    private fun initializeCheckMarkUI() {
        logger.debug { "initializeCheckMarkUI" }

        val texture = Texture(Gdx.files.internal("graphics/checkmark.png"), Pixmap.Format.RGBA8888, true)
        // Create an Image actor with the checkmark texture
        checkMarkImage = Image(texture).apply {
            setSize(80f, 80f) // Example size; adjust based on your design
            // Position it at the lower right corner with 20px padding from the edges
            setPosition(
                Gdx.graphics.width - 100f, // Adjusted to dynamically use screen width
                20f
            )
            isVisible = false
        }
        // Add the checkmark image to the UI stage
        uiStage.addActor(checkMarkImage)
    }

    fun displayCheckMark() {
        logger.debug { "displayCheckMark" }

        if (::checkMarkImage.isInitialized) {
            checkMarkImage.isVisible = true
            checkMarkImage.color.a = 0.5f
            checkMarkImage.addAction(Actions.fadeIn(1f))
        }
    }

    fun displayTextEditorPopup(text: String, pos: Vector2, onSave: (String) -> Unit, onCancel: () -> Unit) {
        logger.debug { "displayTextEditorPopup puzzlePiece=$text" }

        TextEditorPopup(
            stage = uiStage,
            skin = uiSkin,
            text = text,
            pos = pos,
            onSave = onSave,
            onCancel = onCancel,
            hudViewport = hudViewport,
            gameViewport = gameViewport
        )
    }

    fun displayGrammaticalRolePopup(puzzlePiece: PuzzlePiece, side: Side, onRoleSelected: (GrammaticalRole) -> Unit) {
        logger.debug { "displayGrammaticalRolePopup for puzzlePiece=${puzzlePiece.text}" }

        val popupWindow = Window("", uiSkin).also { popup ->
            popup.isMovable = true
            popup.isResizable = false

            // Layout group for the buttons
            val buttonTable = Table()

            // Define the grammatical roles you want to offer
            val roles = listOf(GrammaticalRole.SUBJECT, GrammaticalRole.OBJECT, GrammaticalRole.ADVERBIAL)

            roles.forEach { role ->
                val buttonStyle = ImageButton.ImageButtonStyle().apply {
                    this.up = TextureRegionDrawable(TextureRegion(tabIcon)).tint(role.color)
                }
                val roleButton = ImageButton(buttonStyle).apply {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            logger.info { "ImageButton grammaticalRole = $role clicked" }
                            onRoleSelected(role)
                            currentPopupWindow = null
                            popup.remove()
                            true
                        }
                    })
                }
                buttonTable.add(roleButton).size(40f).pad(5f)
                if (side == Side.LEFT || side == Side.RIGHT) {
                    buttonTable.row()
                }
            }
            popup.add(buttonTable).row()

            popup.pack() // Adjust size to fit content

            // Position the popup near the puzzle piece
            val popupX: Float
            val popupY: Float
            with(puzzlePiece) {
                when (side) {
                    Side.TOP -> {
                        popupX = pos.x + size / 2f
                        popupY = pos.y + size + 50f
                    }

                    Side.BOTTOM -> {
                        popupX = pos.x + size / 2f
                        popupY = pos.y - 150f
                    }

                    Side.LEFT -> {
                        popupX = pos.x - popup.width - 50f
                        popupY = pos.y + size / 4f
                    }

                    Side.RIGHT -> {
                        popupX = pos.x + size + popup.width + 50f
                        popupY = pos.y + size / 4f
                    }
                }
            }

            val projectedCoords = gameViewport.project(Vector2(popupX, popupY))
            popup.setPosition(projectedCoords.x, projectedCoords.y)
        }

        uiStage.addActor(popupWindow)
        currentPopupWindow = popupWindow
    }

    fun updateFonts() {
        val font = loadTaskFont()
        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        exerciseDescriptionLabel.style.font = font
        exerciseDescriptionLabel.style = exerciseDescriptionLabel.style
    }

    fun dispose() {

    }
}
