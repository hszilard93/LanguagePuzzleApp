package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Align
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.view.ui.TextEditorPopup

class UIManager(
    private val uiStage: Stage,
    private val uiSkin: Skin,
    private val hudViewport: HudViewport,
    private val gameViewport: GameViewport,
    private val gameModel: GameModel
) {

    companion object {
        val logger = ktx.log.logger<UIManager>()
    }

    lateinit var exerciseDescriptionFrame: Window
        private set
    lateinit var exerciseDescriptionLabel: Label
        private set
    lateinit var checkMarkImage: Image
        private set

    private var additionalActors = ArrayList<Actor>()

    fun initializeUI() {
        logger.debug { "initializeUI" }
        initializeExerciseDescriptionUI()
        initializeCheckMarkUI()
    }

    private fun initializeExerciseDescriptionUI() {
        logger.debug { "Initializing Exercise Description UI." }

        // Create the window with no title
        exerciseDescriptionFrame = Window("", uiSkin).apply {
            background = skin.getDrawable("white")

            // Set semi-transparent background color (e.g., black with 50% opacity)
            background.minWidth = 300f
            background.minHeight = 100f
            color.a = 0.75f // Semi-transparent
            isMovable = false
            isResizable = false

            // Set specific padding: 20px top, 40px left and right, and 10px bottom
            padTop(20f)
            padLeft(40f)
            padRight(40f)
            padBottom(10f)

            // Initially invisible; visibility will be handled in updateExerciseDescription()
            isVisible = false
        }

        // Create the label for the description text
        exerciseDescriptionLabel = Label("", uiSkin).apply {
            setWrap(true) // Enable text wrapping
            setAlignment(Align.center) // Center-align the text
        }

        // Add the label to the window
        exerciseDescriptionFrame.add(exerciseDescriptionLabel).expand().fill().row()

        // Position the window at the top center of the screen with a small margin
        exerciseDescriptionFrame.setPosition(
            (hudViewport.worldWidth - exerciseDescriptionFrame.width) / 2,
            hudViewport.worldHeight - exerciseDescriptionFrame.height - 10f
        )

        // Add the window to the UI stage
        uiStage.addActor(exerciseDescriptionFrame)

        // Update the description based on the current exercise
        updateExerciseDescription()
    }

    fun updateExerciseDescription() {
        val currentExercise = gameModel.currentExercise
        if (currentExercise!!.taskDescription.isNotBlank()) {
            // Set the description text
            exerciseDescriptionLabel.setText(currentExercise.taskDescription)

            // Adjust the window size based on the content
            exerciseDescriptionFrame.pack()

            // Reposition the window to stay at the top center after packing
            exerciseDescriptionFrame.setPosition(
                (hudViewport.worldWidth - exerciseDescriptionFrame.width) / 2,
                hudViewport.worldHeight - exerciseDescriptionFrame.height - 10f
            )

            exerciseDescriptionFrame.setSize(hudViewport.screenWidth.toFloat(), 130f)
            exerciseDescriptionFrame.setPosition(0f, hudViewport.screenHeight.toFloat() - exerciseDescriptionFrame.height)

            // Make the window visible
            exerciseDescriptionFrame.isVisible = true
        }
        else {
            // Hide the window if there's no description
            exerciseDescriptionFrame.isVisible = false
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

    fun displayTextEditorPopup(puzzlePiece: PuzzlePiece, onSave: (String) -> Unit, onCancel: () -> Unit) {
        logger.debug { "displayTextEditorPopup puzzlePiece=${puzzlePiece.text}" }

        TextEditorPopup(
            stage = uiStage,
            skin = uiSkin,
            puzzlePiece = puzzlePiece,
            onSave = onSave,
            onCancel = onCancel,
            hudViewport = hudViewport,
            gameViewport = gameViewport
        )
    }

    fun dispose() {

    }
}
