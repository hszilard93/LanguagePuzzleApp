package edu.b4kancs.languagePuzzleApp.app.view.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import edu.b4kancs.languagePuzzleApp.app.view.util.font.loadManualFont
import edu.b4kancs.languagePuzzleApp.app.view.util.font.loadUIFont
import ktx.log.logger

class UserManualDialog(
    title: String,
    skin: Skin,
    style: WindowStyle,
    val setScrollFocus: (ScrollPane) -> Unit
) : Dialog(title, style) {

    companion object {
        private val logger = logger<UserManualDialog>()
    }

    private val scrollPane: ScrollPane

    private var fontMultiplier: Float
    private var textFont: BitmapFont
    private var buttonFont: BitmapFont

    init {
        logger.debug { "init" }

        isMovable = false
        isResizable = false

        fontMultiplier = maxOf(1200f / Gdx.graphics.width, 800f / Gdx.graphics.height)
        textFont = loadManualFont(fontMultiplier)

        val contentTable = Table()

        val manualText = appInstructionsDescription

        val labelStyle = Label.LabelStyle(textFont, skin.get(Label.LabelStyle::class.java).fontColor)
        val manualLabel = Label(manualText, labelStyle).apply {
            setAlignment(Align.topLeft)
            wrap = true
        }

        scrollPane = ScrollPane(manualLabel, skin).apply {
            fadeScrollBars = false
            setScrollingDisabled(true, false) // Allow vertical scrolling only
        }

        contentTable.add(scrollPane).grow().pad(10f).row() // Grow in both directions within ScrollPane

        buttonFont = loadUIFont(fontMultiplier)
        val buttonStyle = TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle::class.java)).apply {
            this.font = buttonFont
        }

        val closeButton = TextButton("Bezárás", buttonStyle).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    this@UserManualDialog.hide()
                }
            })
        }

        contentTable.add(closeButton).width(300f * (fontMultiplier - ((fontMultiplier - 1) / 2))).height(60f).pad(10f).row()

        this.contentTable.add(contentTable).grow()
    }

    override fun show(stage: Stage?): Dialog {
        super.show(stage)

        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        this.setSize(stage?.width ?: (screenWidth * 0.8f), stage?.height ?: (screenHeight * 0.8f))
        this.setPosition((screenWidth - width) / 2f, (screenHeight - height) / 2f)
        this.setScrollFocus(scrollPane)

        return this
    }

    fun resize(newWidth: Int, newHeight: Int) {
        fontMultiplier = maxOf(newWidth / 1200f, newHeight / 800f)
        textFont = loadManualFont(fontMultiplier)
        buttonFont = loadUIFont(fontMultiplier)
        if (isVisible) {
            val screenWidth = Gdx.graphics.width.toFloat()
            val screenHeight = Gdx.graphics.height.toFloat()
            this.setSize(stage?.width ?: screenWidth * 0.8f, stage?.height ?: screenHeight * 0.8f)
            this.setPosition((screenWidth - width) / 2f, (screenHeight - height) / 2f)
        }
    }
}
