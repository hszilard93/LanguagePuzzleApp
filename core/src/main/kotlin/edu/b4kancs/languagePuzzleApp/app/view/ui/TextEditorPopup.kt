package edu.b4kancs.languagePuzzleApp.app.view.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece

class TextEditorPopup(
    private val stage: Stage,
    private val skin: Skin,
    private val puzzlePiece: PuzzlePiece,
    private val onSave: (String) -> Unit,
    private val onCancel: () -> Unit
) {
    private val window: Window
    private val textField: TextField
    private val saveButton: TextButton
    private val cancelButton: TextButton

    init {
        // Initialize Window without a title
        window = Window("", skin).apply {
            setSize(250f, 100f)
            setPosition(
                puzzlePiece.pos.x + puzzlePiece.size / 2 - width / 2,
                puzzlePiece.pos.y + puzzlePiece.size / 2 - height / 2,
                Align.center
            )
            isMovable = true
            isResizable = true

            titleLabel.isVisible = false
            titleTable.isVisible = false
            background = skin.getDrawable("white")

            textField = TextField(puzzlePiece.text, skin)
            saveButton = TextButton("Mentés", skin)
            cancelButton = TextButton("Mégse", skin)

            // Make window draggable by adding an InputListener to the background area
            addListener(object : InputListener() {
                private var startX = 0f
                private var startY = 0f

                override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                    if (keycode == Input.Keys.ENTER) {
                        onSave(textField.text)
                        close()
                        return true
                    }
                    return super.keyDown(event, keycode)
                }

                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    // Only start dragging if clicking on the background (not text field or buttons)
                    val actor = hit(x, y, true)
                    if (actor == this@apply && button == 0) {
                        startX = x
                        startY = y
                        return true
                    }
                    return false
                }

                override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                    // Move the window based on drag delta
                    val deltaX = x - startX
                    val deltaY = y - startY
                    moveBy(deltaX, deltaY)
                }
            })
        }

        saveButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onSave(textField.text)
                close()
            }
        })

        cancelButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onCancel()
                close()
            }
        })

        // Table for layout management
        val table = Table(skin).apply {
            pad(2.5f)
            defaults().pad(2.5f).expandX().fillX()
            add(textField).height(40f).row()

            val buttonTable = Table(skin).apply {
//                defaults().pad(2f)
                add(saveButton).width(100f)
                add(cancelButton).width(100f)
            }
            add(buttonTable).height(25f)
        }

        window.add(table).expand().fill()
        stage.addActor(window)
    }

    private fun close() {
        window.remove()
    }
}
