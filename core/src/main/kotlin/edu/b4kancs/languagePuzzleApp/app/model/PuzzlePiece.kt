package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

interface PuzzlePieceFeature { val side: Side }

data class PuzzleTab(
    override val side: Side,
    val color: Color? = null
) : PuzzlePieceFeature

data class PuzzleBlank( // An indentation on a puzzle piece is called a 'blank'
    override val side: Side
) : PuzzlePieceFeature

class PuzzlePiece(
    pos: Vector2,
    width: Float = MIN_WIDTH,
    height: Float = MIN_HEIGHT,
    var depth: Int = 0,
    val tabs: List<PuzzleTab> = emptyList(),
    val blanks: List<PuzzleBlank> = emptyList(),
    val color: Color = Color.YELLOW
) {
    // Automatically update the renderPos every time the position of the puzzle changes
    var pos: Vector2 = Vector2(0f, 0f)
        set(value) {
            field = value
            renderPos = calculateRenderPosition(field)
        }
    var width: Float = MIN_WIDTH
        set(value) {
            field = value.coerceAtLeast(MIN_WIDTH)
            renderSize = calculateRenderSize(field, height)
        }
    var height: Float = MIN_HEIGHT
        set(value) {
            field = value.coerceAtLeast(MIN_HEIGHT)
            renderSize = calculateRenderSize(width, field)
        }

    lateinit var renderPos: Vector2
        private set
    lateinit var renderSize: Pair<Float, Float>
        private set

    companion object {
        const val MIN_WIDTH = 300f
        const val MIN_HEIGHT = 300f
    }

    // Validate the PuzzlePiece on initialization
    init {
        this.pos = pos
        this.width = width
        this.height = height
        if (tabs.map { it.side }.intersect(blanks.map { it.side }.toSet()).isNotEmpty()) {
            throw InvalidPuzzlePieceException(
                "PuzzlePiece has overlapping tabs and blanks!" +
                    "\ntabs = $tabs" +
                    "\nblanks = $blanks"
            )
        }
    }

    private fun calculateRenderPosition(pos: Vector2): Vector2 = Vector2(pos.x - 50, pos.y - 50)

    private fun calculateRenderSize(width: Float, height: Float): Pair<Float, Float> = width + 100 to height + 100
}

class InvalidPuzzlePieceException(message: String) : IllegalArgumentException(message)
