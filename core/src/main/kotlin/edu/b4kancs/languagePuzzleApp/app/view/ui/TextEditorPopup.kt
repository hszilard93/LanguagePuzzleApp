package edu.b4kancs.languagePuzzleApp.app.view.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Vector2
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
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.HudViewport
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece

class TextEditorPopup(
    private val stage: Stage,
    private val skin: Skin,
    private val font: BitmapFont,
    private val hudViewport: HudViewport,
    private val gameViewport: GameViewport,
    private val text: String,
    private val pos: Vector2,
    private val onSave: (String) -> Unit,
    private val onCancel: () -> Unit
) {
    val window: Window
    val textField: TextField
    private val saveButton: TextButton
    private val cancelButton: TextButton

    init {
        window = Window("", skin).apply {
            isMovable = true
            isResizable = true

            titleLabel.isVisible = false
            titleTable.isVisible = false
            background = skin.getDrawable("white")

            textField = TextField(text, skin).apply {
                style.font = font
            }

            saveButton = TextButton("Mentés", skin).apply {
                style.font = font
            }
            saveButton.style = saveButton.style
            cancelButton = TextButton("Mégse", skin).apply {
                style.font = font
            }
            cancelButton.style = cancelButton.style

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

            val stagePosition = gameViewport.project(pos.cpy())
            setPosition(
                stagePosition.x + 100f,
                stagePosition.y + 20f,
                Align.center
            )
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
            add(textField).height(45f).pad(5f).row()

            val buttonTable = Table(skin).apply {
//                defaults().pad(2f)
                add(saveButton).width(100f).pad(5f)
                add(cancelButton).width(100f).pad(5f)
            }
            buttonTable.pack()
            add(buttonTable)
            pack()
        }

        // TODO refactor this eventually
        window.add(table).expand().fill()
        window.pack()
        stage.addActor(window)
        stage.setKeyboardFocus(textField)
    }

    private fun close() {
        window.remove()
    }
}
