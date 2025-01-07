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
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.view.ui.AddPuzzlePopup
import edu.b4kancs.languagePuzzleApp.app.view.ui.GrammaticalRolePopup
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
    lateinit var addPuzzleButton: ImageButton
        private set

    private lateinit var topBarTable: Table


    private var backIcon = Texture(Gdx.files.internal("back_button_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }
    private var addPuzzleIcon = Texture(Gdx.files.internal("add_puzzle_button_7.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }

    fun initializeUI() {
        logger.debug { "initializeUI" }
        initializeTopBar()
        initializeCheckMarkUI()
        initializeAddPuzzleButton()
    }

    private fun initializeTopBar() {
        logger.debug { "Initializing Top Bar UI." }

        topBarTable = Table().apply {
            setFillParent(true)
            padTop(4f)
            padLeft(4f)
            padRight(24f)
            top().left()
            //debug = true // Uncomment for debugging table layout
        }

        exerciseDescriptionLabel = Label("", uiSkin).apply {
            setWrap(true)
            setAlignment(Align.center)
        }
        exerciseDescriptionLabel.style.background = uiSkin.getDrawable("white")
        exerciseDescriptionLabel.color.a = 0.75f
        exerciseDescriptionLabel.style.font = taskFont

        // ↓ This needs to be done or the style wont update ↓
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

        val popupWindow = GrammaticalRolePopup(
            title = "",
            skin = uiSkin,
            gameViewport = gameViewport,
            puzzlePiece = puzzlePiece,
            side = side,
            onRoleSelected = onRoleSelected,
            onClose = {
                currentPopupWindow?.remove()
                currentPopupWindow = null
            }
        )

        uiStage.addActor(popupWindow)
        currentPopupWindow = popupWindow
    }

    private fun initializeAddPuzzleButton() {
        logger.debug { "initializeAddPuzzleButton" }

        val style = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(addPuzzleIcon)).tint(Color(120f, 120f, 255f, 0.8f))
            this.down = TextureRegionDrawable(TextureRegion(addPuzzleIcon)).tint(Color(120f, 120f, 255f, 1f))
        }

        addPuzzleButton = ImageButton(style).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "Add puzzle button clicked" }
                    displayAddPuzzlePopup(
                        onAddBlankPuzzle = {},
                        onAddBasePuzzle = {}
                    )
                }
            })
        }

        topBarTable.row()
        topBarTable.add(addPuzzleButton).width(100f).height(100f).padLeft(10f).align(Align.topLeft)
    }

    fun displayAddPuzzlePopup(onAddBasePuzzle: () -> Unit, onAddBlankPuzzle: () -> Unit) {
        logger.debug { "displayAddPuzzlePopup" }

        val popupWindow = AddPuzzlePopup(
            title = "",
            skin = uiSkin,
            position = Vector2(addPuzzleButton.x + 50, addPuzzleButton.y - 250),
            onAddBasePuzzle = onAddBasePuzzle,
            onAddBlankPuzzle = onAddBlankPuzzle,
            onClose = {
                currentPopupWindow?.remove()
                currentPopupWindow = null
            }
        )

        uiStage.addActor(popupWindow)
        currentPopupWindow = popupWindow
    }

    fun updateFonts() {
        val font = loadTaskFont()
        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        exerciseDescriptionLabel.style.font = font
        exerciseDescriptionLabel.style = exerciseDescriptionLabel.style
    }

    fun isPointerOverButton(mousePos: Vector2): Boolean {
        logger.misc { "isPointerOverButton mousePos = $mousePos" }

        val realMousePos = gameViewport.project(mousePos.cpy())

        if (this::returnButton.isInitialized) {
            returnButton.let {
                if (it.isOver) {
                    return true
                }
            }
        }
        if (this::addPuzzleButton.isInitialized) {
            addPuzzleButton.let {
                if (addPuzzleButton.isOver) {
                    return true
                }
            }
        }
        return false
    }

    fun dispose() {

    }
}
