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
    var pos: Vector2,
    width: Float = MIN_WIDTH,
    height: Float = MIN_HEIGHT,
    var depth: Int = 0,
    val tabs: List<PuzzleTab> = emptyList(),
    val blanks: List<PuzzleBlank> = emptyList(),
    val color: Color = Color.YELLOW
) {
    var width: Float = MIN_WIDTH
        set(value) { field = value.coerceAtLeast(MIN_WIDTH) }
    var height: Float = MIN_HEIGHT
        set(value) { field = value.coerceAtLeast(MIN_HEIGHT) }

    // Validate the PuzzlePiece on initialization
    init {
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
    companion object {
        const val MIN_WIDTH = 300f
        const val MIN_HEIGHT = 300f
    }
}

class InvalidPuzzlePieceException(message: String) : IllegalArgumentException(message)
