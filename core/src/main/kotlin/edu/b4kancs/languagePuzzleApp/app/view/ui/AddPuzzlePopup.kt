package edu.b4kancs.languagePuzzleApp.app.view.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.log.logger

class AddPuzzlePopup(
    title: String,
    skin: Skin,
    position: Vector2,
    onAddBasePuzzle: () -> Unit,
    onAddBlankPuzzle: () -> Unit,
    onClose: () -> Unit
) : Window(title, skin) {

    companion object {
        val logger = logger<AddPuzzlePopup>()
    }

    private val addBasePuzzleImage = Texture(Gdx.files.internal("add_puzzle_base_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }
    private val andBlankPuzzleImage = Texture(Gdx.files.internal("add_puzzle_blank_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }

    init {
        isMovable = false
        isResizable = false

        val buttonTable = Table()//.apply { debug = true }

        val basePuzzleButtonStyle = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(addBasePuzzleImage))
        }
        val addBasePuzzleButton = ImageButton(basePuzzleButtonStyle).also { button ->
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {

                    val relativeY = y - button.y

                    val graphicHeight = button.height * 0.5f
                    val offset = 15f
                    val hitBottomY = (button.height - graphicHeight) / 2
                    val hitTopY = button.height - hitBottomY
                    if (relativeY in hitBottomY + offset..hitTopY + offset) {
                        logger.info { "addBasePuzzleButton clicked x = $x\ty = $y" }
                        onAddBasePuzzle()
                        onClose()
                    }
                    else {
                        logger.info { "addBasePuzzleButton click missed x = $x\ty = $y" }
                    }
                }
            })
        }

        val blankPuzzleButtonStyle = ImageButton.ImageButtonStyle().apply {
            this.up = TextureRegionDrawable(TextureRegion(andBlankPuzzleImage))
        }
        val addBlankPuzzleButton = ImageButton(blankPuzzleButtonStyle).also { button ->
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val relativeY = y - button.y

                    val graphicHeight = button.height * 0.5f
                    val offset = 15f
                    val hitBottomY = (button.height - graphicHeight) / 2
                    val hitTopY = button.height - hitBottomY
                    if (relativeY in hitBottomY + offset..hitTopY + offset) {
                        logger.info { "addBlankPuzzleButton clicked x = $x\ty = $y" }
                        onAddBlankPuzzle()
                        onClose()
                    }
                    else {
                        logger.info { "addBlankPuzzleButton click missed x = $x\ty = $y" }
                    }
                }
            })
        }

        buttonTable.add(addBasePuzzleButton).size(150f).pad(5f).padTop(-20f).row()
        buttonTable.add(addBlankPuzzleButton).size(150f).pad(5f).padTop(-20f).row()

        add(buttonTable)

        setPosition(position.x, position.y)
        pack()
    }
}
