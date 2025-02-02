package edu.b4kancs.languagePuzzleApp.app.view.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Ruleset

class SelectEndingTypePopup(
    title: String = "",
    skin: Skin,
    font: BitmapFont,
    rules: Ruleset,
    side: Side,
    gameViewport: Viewport,
    puzzlePiece: PuzzlePiece,
    onSuffixesSelected: () -> Unit,
    onPostpSelected: () -> Unit,
    onIndPronounsSelected: () -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit
) : Window(
    title,
    skin
) {
    companion object {
        val logger = ktx.log.logger<SelectEndingTypePopup>()
    }

    init {
        isMovable = true
        isResizable = false

        // Outer table to manage title and content
        val contentTable = Table()
        contentTable.background = skin.getDrawable("white")

        // Layout group for the buttons
        val textTable = Table()

        titleLabel.style.font = font
        titleLabel.style = titleLabel.style

        val suffixButtonText = "Toldalék"
        val postpButtonText = "Névutó"
        val indPronounsButtonText = "Jelentéscímke"

        val items = arrayListOf(suffixButtonText)
        if (rules.shouldOfferPostpositions == true) items.add(postpButtonText)
        if (rules.shouldOfferIndPronouns == true) items.add(indPronounsButtonText)

        items.forEach { item ->
            val buttonStyle = TextButton.TextButtonStyle().apply {
                this.font = font
                this.fontColor = Color.BLACK
            }
            val button = TextButton(item, buttonStyle).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        logger.info { "TextButton $item clicked" }
                        onClose()
                        when (item) {
                            suffixButtonText -> onSuffixesSelected()
                            postpButtonText -> onPostpSelected()
                            indPronounsButtonText -> onIndPronounsSelected()
                        }
                    }
                })
            }

            textTable.add(button).pad(0f).space(10f).align(Align.left)
            textTable.row()
        }

        textTable.pack()

        val scrollPane = ScrollPane(textTable, skin).apply {
            pad(-5f)
            setFadeScrollBars(false)
            setScrollbarsVisible(false)
            setScrollingDisabled(true, false)
        }

        contentTable.add(titleLabel).growX().row()
        contentTable.add(scrollPane)

        // Clear the default window content and add our outer table
        this.clear()
        this.add(contentTable).grow()

        pack() // Adjust size to fit content

        // Position the popup near the puzzle piece
        val popupX: Float
        val popupY: Float
        puzzlePiece.let { pp ->
            when (side) {
                Side.TOP -> {
                    popupX = pp.pos.x + pp.size / 2f
                    popupY = pp.pos.y + pp.size + 50f
                }

                Side.BOTTOM -> {
                    popupX = pp.pos.x + pp.size / 2f
                    popupY = pp.pos.y - 150f
                }

                Side.LEFT -> {
                    popupX = pp.pos.x - this.width - 50f
                    popupY = pp.pos.y + pp.size / 4f
                }

                Side.RIGHT -> {
                    popupX = pp.pos.x + pp.size
                    popupY = pp.pos.y + pp.size / 4f
                }
            }
        }

        val projectedCoords = gameViewport.project(Vector2(popupX, popupY))
        setPosition(projectedCoords.x, projectedCoords.y)
    }
}
