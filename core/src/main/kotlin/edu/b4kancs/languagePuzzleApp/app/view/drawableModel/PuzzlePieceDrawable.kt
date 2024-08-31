package edu.b4kancs.languagePuzzleApp.app.view.drawableModel

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.view.screen.GameScreen.Companion.logger
import edu.b4kancs.languagePuzzleApp.app.view.utils.plus
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRadians
import ktx.inject.Context
import space.earlygrey.shapedrawer.ShapeDrawer


class PuzzlePieceDrawable(
    private val context: Context,
    val puzzlePiece: PuzzlePiece
) : DrawablePaddingImpl() {

    val parts = ArrayList<Feature>()
    val drawablePath = ArrayList<Vector2>()

    private val minWidth: Float
    private val minHeight: Float

    companion object {
        const val CORNER_RADIUS = 40f
    }

    init {
        minWidth = if (puzzlePiece.tabs.any { it.side == Side.TOP || it.side == Side.BOTTOM }) 400f else 200f
        minHeight = if ((puzzlePiece.tabs + puzzlePiece.blanks).any { it.side in setOf(Side.LEFT, Side.RIGHT) }) 300f else 150f

        generateParts()
    }

    private fun generateParts() {
        val startX = CORNER_RADIUS
        val startY = 0f

        // Creates a puzzle base shape without tabs or blanks with rounded corners for testing
        parts.add(Line(Vector2(startX, startY), minWidth, RIGHT_DIR, Side.TOP))
        parts.add(Corner(parts.last().endPosition, CORNER_RADIUS, RIGHT_DIR, Side.TOP))
        parts.add(Line(parts.last().endPosition, minHeight, DOWN_DIR, Side.LEFT))
        parts.add(Corner(parts.last().endPosition, CORNER_RADIUS, DOWN_DIR, Side.LEFT))
        parts.add(Line(parts.last().endPosition, minWidth, LEFT_DIR, Side.BOTTOM))
        parts.add(Corner(parts.last().endPosition, CORNER_RADIUS, LEFT_DIR, Side.BOTTOM))
        parts.add(Line(parts.last().endPosition, minHeight, UP_DIR, Side.RIGHT))
        parts.add(Corner(parts.last().endPosition, CORNER_RADIUS, UP_DIR, Side.RIGHT))
    }

    override fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float) {
        logger.misc { "Drawing puzzle piece: $puzzlePiece" }
        val drawer = context.inject<ShapeDrawer>()
        for (part in parts) {
            when (part) {
                is Line -> {
                    drawer.setColor(Color.GOLD)
                    val startPosition = part.startPosition + Vector2(x, y)
                    val endPosition = part.endPosition + Vector2(x, y)
                    drawer.line(startPosition, endPosition, 5f)
                }
                is Corner -> {
                    drawer.setColor(Color.BLUE)
                    val centerPosition = part.centerPosition + Vector2(x, y)
                    drawer.arc(centerPosition.x, centerPosition.y, part.length, part.startAngle, 90f.toRadians(), 5f, true, 50)
                }
            }
        }
//        drawer.filledPolygon(floatArrayOf(500f, 500f, 500f, 800f, 800f, 500f))
    }

    override fun getMinWidth(): Float {
        TODO("Not yet implemented")
    }

    override fun setMinWidth(minWidth: Float) {
        TODO("Not yet implemented")
    }

    override fun getMinHeight(): Float {
        TODO("Not yet implemented")
    }

    override fun setMinHeight(minHeight: Float) {
        TODO("Not yet implemented")
    }
}

