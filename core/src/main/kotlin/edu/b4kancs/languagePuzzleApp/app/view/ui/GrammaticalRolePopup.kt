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
import com.badlogic.gdx.utils.viewport.Viewport
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.Side
import ktx.log.logger

class GrammaticalRolePopup(
    title: String,
    skin: Skin,
    gameViewport: Viewport,
    puzzlePiece: PuzzlePiece,
    side: Side,
    onRoleSelected: (GrammaticalRole) -> Unit,
    onClose: () -> Unit
) : Window(title, skin) {

    private var tabIcon = Texture(Gdx.files.internal("puzzle_tab_general_1.png"), Pixmap.Format.RGBA8888, true)
        .apply { setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) }

    companion object {
        val logger = logger<GrammaticalRolePopup>()
    }

    init {
        isMovable = true
        isResizable = false

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
                        onClose()
                    }
                })
            }
            buttonTable.add(roleButton).size(40f).pad(5f)
            if (side == Side.LEFT || side == Side.RIGHT) {
                buttonTable.row()
            }
        }
        add(buttonTable).row()

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
                    popupX = pp.pos.x + pp.size + this.width + 50f
                    popupY = pp.pos.y + pp.size / 4f
                }
            }
        }

        val projectedCoords = gameViewport.project(Vector2(popupX, popupY))
        setPosition(projectedCoords.x, projectedCoords.y)
    }
}
