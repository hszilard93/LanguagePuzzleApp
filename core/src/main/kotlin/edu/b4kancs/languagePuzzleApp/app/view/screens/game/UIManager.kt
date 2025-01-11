package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
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
import edu.b4kancs.languagePuzzleApp.app.model.Suffix
import edu.b4kancs.languagePuzzleApp.app.view.ui.AddPuzzlePopup
import edu.b4kancs.languagePuzzleApp.app.view.ui.GrammaticalRolePopup
import edu.b4kancs.languagePuzzleApp.app.view.ui.SelectSuffixPopup
import edu.b4kancs.languagePuzzleApp.app.view.ui.TextEditorPopup
import edu.b4kancs.languagePuzzleApp.app.view.utils.TaskFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.UIFontHolder
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadTaskDescriptionFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.loadUIFont
import edu.b4kancs.languagePuzzleApp.app.view.utils.partialFadeIn
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

    private lateinit var puzzleManager: PuzzleManager
    private val hudViewport: HudViewport = context.inject()
    private val gameViewport: GameViewport = context.inject()
    private val gameModel: GameModel = context.inject()
    private val taskFont = context.inject<TaskFontHolder>().descriptionFont
    private val counterFont = context.inject<TaskFontHolder>().counterFont
    private val uiFont = context.inject<UIFontHolder>()

    private lateinit var exerciseDescriptionLabel: Label
    private lateinit var taskCounterLabel: Label
    private lateinit var exerciseDescriptionScrollPane: ScrollPane
    private lateinit var returnButton: ImageButton
    private lateinit var checkMarkImage: Image
    private lateinit var backPageImageButton: ImageButton
    private lateinit var forwardPageImageButton: ImageButton
    private var addPuzzleButton: ImageButton? = null
    private var garbageBinImage: Image? = null

    private lateinit var topBarTable: Table
    private lateinit var bottomBarTable: Table

    private var backButtonTexture = Texture(Gdx.files.internal("back_button_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }
    private var addPuzzleTexture = Texture(Gdx.files.internal("add_puzzle_button_7.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }
    private var garbageBinClosedTexture = Texture(Gdx.files.internal("garbage_bin_closed_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }
    private var garbageBinOpenTexture = Texture(Gdx.files.internal("garbage_bin_open_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }

    private var correctCheckMarkTexture = Texture(Gdx.files.internal("graphics/checkmark.png"), Pixmap.Format.RGBA8888, true)
    private var incorrectCheckMarkTexture = Texture(Gdx.files.internal("graphics/incorrect_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }

    var isGarbageBinLifted = false
        private set

    fun registerPuzzleManager(puzzleManager: PuzzleManager) {
        this.puzzleManager = puzzleManager
    }

    fun initializeUI() {
        logger.debug { "initializeUI" }
        initializeTopBar()
        initializeBottomBar()

        val rules = gameModel.currentExercise?.type?.ruleset
        if (rules?.canAddMainPieces == true || rules?.canAddBlankPieces == true) {
            initializeAddPuzzleButton()
            initializeGarbageBin()
        }
    }

    private fun initializeTopBar() {
        logger.debug { "Initializing Top Bar UI." }

        topBarTable = Table().apply {
            setFillParent(true)
            padTop(4f)
            padLeft(4f)
            padRight(24f)
            top().left()
//            debug = true
        }

        exerciseDescriptionLabel = Label("", uiSkin).apply {
            setWrap(true)
            setAlignment(Align.center)
            color.a = 0.75f
        }

        // ↓ This needs to be done or the style wont update ↓
        exerciseDescriptionLabel.style = exerciseDescriptionLabel.style

        // ScrollPane makes the label scrollable
        exerciseDescriptionScrollPane = ScrollPane(exerciseDescriptionLabel, uiSkin).apply {
            setFadeScrollBars(true)
            setScrollbarsVisible(true)
            setScrollingDisabled(true, false)
            style.background = uiSkin.getDrawable("white").apply {
                topHeight = 10f
                bottomHeight = 10f
                leftWidth = 10f
                rightWidth = 10f
            }
        }

        val taskCounterLabelStyle = LabelStyle(counterFont, Color.WHITE)
        taskCounterLabel = Label("", taskCounterLabelStyle).apply {
            style.font.data.markupEnabled = true
//            style.fontColor.a = 0.75f     // Enabling this leads to some kind of bug with the blending
        }

        val buttonStyle = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(backButtonTexture)).tint(Color(120f, 120f, 255f, 0.6f))
            this.down = TextureRegionDrawable(TextureRegion(backButtonTexture)).tint(Color(120f, 120f, 255f, 1f))
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
        topBarTable.add(exerciseDescriptionScrollPane).expandX().fillX().maxHeight(200f).padRight(10f)
        topBarTable.add(returnButton).width(72f).height(72f).pad(12f).row()
        topBarTable.add(taskCounterLabel).align(Align.right).padTop(-30f).padRight(20f)

        // Add the table to the UI stage
        uiStage.addActor(topBarTable)

        // Update the description based on the current exercise
        updateTaskInfo()
    }

    fun updateTaskInfo() {
        val task = gameModel.currentTask!!
        if (task.taskDescription.isNotBlank()) {
            exerciseDescriptionLabel.setText(task.taskDescription)
            // Make sure the table is visible
            topBarTable.isVisible = true

            taskCounterLabel.apply {
                setText("[#22CC22]${gameModel.currentTaskNumber}[BLACK]/[#005500]${gameModel.totalTaskCount}[]")
                isVisible = gameModel.totalTaskCount > 1
            }

            if (this::forwardPageImageButton.isInitialized) {
                forwardPageImageButton.isVisible = false
            }

            if (this::checkMarkImage.isInitialized) {
                hideCheckMark()
            }
        }
        else {
            // Hide the table if there's no description
            topBarTable.isVisible = false
        }
    }

    private fun initializeBottomBar() {
        logger.debug { "initializeBottomBar" }

        initializePageButtons()
        initializeCheckMarkUI()

        bottomBarTable = Table().apply {
            setFillParent(true)
            padBottom(4f)
            padLeft(4f)
            padRight(24f)
            bottom().left()
        }

        if (garbageBinImage != null) {
            bottomBarTable.add(garbageBinImage).width(125f).height(125f).pad(12f)
        }
        bottomBarTable.add(backPageImageButton).width(72f).height(72f).pad(12f)
        bottomBarTable.add().expandX()
        bottomBarTable.add(forwardPageImageButton).width(72f).height(72f).pad(12f)
        bottomBarTable.add(checkMarkImage).width(80f).height(80f).padLeft(12f).padBottom(-25f)
        bottomBarTable.pack()

        uiStage.addActor(bottomBarTable)
    }

    private fun initializePageButtons() {
        logger.debug { "initializePageButtons" }

        val backPageImage = Texture(Gdx.files.internal("backpage_icon_1.png"), Pixmap.Format.RGBA8888, true)
        val backStyle = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(backPageImage)).tint(Color(120f, 120f, 120f, 0.8f))
            this.down = TextureRegionDrawable(TextureRegion(backPageImage)).tint(Color(120f, 255f, 255f, 1f))
        }

        backPageImageButton = ImageButton(backStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "Back page button clicked" }
                    gameModel.setUpPreviousTask() {
                        updateTaskInfo()
                    }
                }
            })
            isVisible = false
        }

        val forwardPageImage = Texture(Gdx.files.internal("forwardpage_icon_1.png"), Pixmap.Format.RGBA8888, true)
        val forwardStyle = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(forwardPageImage)).tint(Color(120f, 120f, 120f, 0.8f))
            this.down = TextureRegionDrawable(TextureRegion(forwardPageImage)).tint(Color(120f, 255f, 255f, 1f))
        }
        forwardPageImageButton = ImageButton(forwardStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "Forward page button clicked" }
                    gameModel.setUpNextTask() {
                        updateTaskInfo()
                    }
                }
            })
            isVisible = false
        }
    }

    private fun showForwardPageButton() {
        logger.debug { "showNextPageButton" }

        if (::forwardPageImageButton.isInitialized) {
            if (gameModel.currentTaskNumber < gameModel.totalTaskCount) {
                forwardPageImageButton.color.a = 0.2f
                forwardPageImageButton.isVisible = true
                forwardPageImageButton.addAction(Actions.fadeIn(0.5f))
            }
        }
    }

    private fun initializeCheckMarkUI() {
        logger.debug { "initializeCheckMarkUI" }

        // Create an Image actor with the checkmark texture
        checkMarkImage = Image(correctCheckMarkTexture).apply {
//            setSize(80f, 80f)
//            setPosition(
//                topBarTable.width - 20f,
//                20f
//            )
//            isVisible = false
            touchable = Touchable.disabled
            isVisible = false
        }
        // Add the checkmark image to the UI stage
//        uiStage.addActor(checkMarkImage)
    }

    fun showCheckMark(isCorrect: Boolean = true) {
        logger.debug { "showCheckMark isCorrect = $isCorrect" }

        if (::checkMarkImage.isInitialized) {
            if (isCorrect) {
                checkMarkImage.drawable = TextureRegionDrawable(correctCheckMarkTexture)
                showForwardPageButton()
            }
            else {
                checkMarkImage.drawable = TextureRegionDrawable(incorrectCheckMarkTexture)
            }

            checkMarkImage.setPosition(
                topBarTable.width - 100f,
                20f
            )
            checkMarkImage.color.a = 0.2f
            checkMarkImage.isVisible = true
            checkMarkImage.addAction(Actions.fadeIn(0.5f))
        }
    }

    fun hideCheckMark() {
        logger.debug { "hideCheckMark" }

        if (::checkMarkImage.isInitialized) {
            checkMarkImage.addAction(Actions.fadeOut(0.1f))
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
            title = "Válassz szerepet!",
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

    fun displaySelectSuffixPopupForResult(
        role: GrammaticalRole,
        puzzlePiece: PuzzlePiece,
        side: Side,
        onSuffixSelected: (Suffix) -> Unit,
        onCancel: () -> Unit
    ) {
        logger.debug { "displaySelectSuffixPopup role = $role" }

        val popupWindow = SelectSuffixPopup(
            title = "Válassz\n toldalékot!",
            skin = uiSkin,
            font = uiFont.font,
            role = role,
            puzzlePiece = puzzlePiece,
            side = side,
            gameViewport = gameViewport,
            onSuffixSelected = onSuffixSelected,
            onClose = {
                currentPopupWindow?.remove()
                currentPopupWindow = null
            },
            onCancel = onCancel
        )

        uiStage.addActor(popupWindow)
        currentPopupWindow = popupWindow
    }

    private fun initializeAddPuzzleButton() {
        logger.debug { "initializeAddPuzzleButton" }

        val style = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(addPuzzleTexture)).tint(Color(120f, 120f, 255f, 0.8f))
            this.down = TextureRegionDrawable(TextureRegion(addPuzzleTexture)).tint(Color(120f, 120f, 255f, 1f))
        }

        addPuzzleButton = ImageButton(style).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    logger.info { "Add puzzle button clicked" }
                    displayAddPuzzlePopup(
                        onAddBasePuzzle = {
                            puzzleManager.addNewPuzzlePieceViaDrag(isBlank = false)
                        },
                        onAddBlankPuzzle = {
                            puzzleManager.addNewPuzzlePieceViaDrag(isBlank = true)
                        }
                    )
                }
            })
        }

        topBarTable.row()
        topBarTable.add(addPuzzleButton).width(100f).height(100f).padLeft(10f).align(Align.topLeft)
    }

    fun displayAddPuzzlePopup(onAddBasePuzzle: () -> Unit, onAddBlankPuzzle: () -> Unit) {
        logger.debug { "displayAddPuzzlePopup" }

        if (addPuzzleButton == null) return

        val popupWindow = AddPuzzlePopup(
            title = "",
            skin = uiSkin,
            position = Vector2(addPuzzleButton!!.x + 50, addPuzzleButton!!.y - 250),
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

    private fun initializeGarbageBin() {
        logger.debug { "initializeGarbageBin" }

        garbageBinImage = Image(garbageBinClosedTexture).apply {
//            setSize(125f, 125f)
//            setPosition(10f, 20f)
//            isVisible = false
            isVisible = true
            setColor(200f, 50f, 50f, 0.1f)
        }
//        uiStage.addActor(garbageBinImage)
        isGarbageBinLifted = false
    }

    fun showClosedGarbageBin() {
        logger.debug { "showClosedGarbageBin" }

        garbageBinImage?.let { bin ->
            bin.drawable = TextureRegionDrawable(garbageBinClosedTexture)
            bin.isVisible = true
            bin.addAction(partialFadeIn(0.8f, 0.2f))
            isGarbageBinLifted = false
        }
    }

    fun showLiftedGarbageBin() {
        logger.debug { "showOpenGarbageBin" }
        garbageBinImage?.let { bin ->
            bin.drawable = TextureRegionDrawable(garbageBinOpenTexture)
            bin.isVisible = true
            bin.addAction(partialFadeIn(0.8f, 0.2f))
            isGarbageBinLifted = true
        }
    }

    fun hideGarbageBin() {
        logger.debug { "hideGarbageBin" }
        garbageBinImage?.addAction(Actions.fadeOut(0.1f))
//        garbageBinImage.isVisible = false
    }

    fun isPuzzlePieceOverGarbageBin(puzzlePiece: PuzzlePiece): Boolean {
        logger.debug { "isPuzzlePieceOverGarbageBin" }

        if (garbageBinImage == null) return false

        val puzzleBoundingBoxPos = gameViewport.project(puzzlePiece.boundingBoxPos.cpy())

        val puzzleStartX = puzzleBoundingBoxPos.x + 50f
        val puzzleEndX = puzzleStartX + puzzlePiece.boundingBoxSize - 50f
        val puzzleStartY = puzzleBoundingBoxPos.y + 50f
        val puzzleEndY = puzzleStartY + puzzlePiece.boundingBoxSize + 50f

        val binMiddleX = garbageBinImage!!.x + garbageBinImage!!.width / 2f
        val binMiddleY = garbageBinImage!!.y + garbageBinImage!!.height / 2f
        val binMiddleProjected = hudViewport.unproject(Vector2(binMiddleX, binMiddleY))

        logger.info { "puzzleStartX = $puzzleStartX puzzleEndX = $puzzleEndX puzzleStartY = $puzzleStartY puzzleEndY = $puzzleEndY binMiddleX = $binMiddleX binMiddleY = $binMiddleY projectedX = ${binMiddleProjected.x} projectedY = ${binMiddleProjected.y}" }

        return ((binMiddleX in puzzleStartX..puzzleEndX) && (binMiddleY in puzzleStartY..puzzleEndY))
    }

    fun updateFonts() {
        val taskFont = loadTaskDescriptionFont()
        taskFont.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        exerciseDescriptionLabel.style.font = taskFont
        exerciseDescriptionLabel.style = exerciseDescriptionLabel.style

        val uiFont = loadUIFont()
        uiFont.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

    }

    fun isPointerOverButton(): Boolean {
        logger.misc { "isPointerOverButton" }

        if (this::returnButton.isInitialized) {
            returnButton.let {
                if (it.isOver) {
                    return true
                }
            }
        }
        addPuzzleButton?.let {
            if (it.isOver) {
                return true
            }
        }
        return false
    }

    fun isPointerOverTaskDescription(): Boolean {
        logger.misc { "isPointerOverTaskDescription" }

        if (this::exerciseDescriptionLabel.isInitialized) {
            exerciseDescriptionScrollPane.let {
                val mouseCoords = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                val correctedCoords = mouseCoords// uiStage.screenToStageCoordinates(mouseCoords)
                if (it.hit(correctedCoords.x, correctedCoords.y, false) != null) {
                    return true
                }
            }
        }
        return false
    }

    fun setFocusToTaskDescription(shouldFocus: Boolean) {
        logger.info { "setFocusToTaskDescription toFocus = $shouldFocus" }

        if (shouldFocus && this::exerciseDescriptionScrollPane.isInitialized) {
            uiStage.scrollFocus = exerciseDescriptionScrollPane
        }
        else {
            uiStage.setScrollFocus(null)
        }
    }

    fun dispose() {

    }
}
