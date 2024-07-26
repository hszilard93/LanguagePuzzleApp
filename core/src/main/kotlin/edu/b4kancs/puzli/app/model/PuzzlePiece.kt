package edu.b4kancs.puzli.app.model

interface PuzzlePieceFeature { val onSide: Side }

data class PuzzleTab(
    override val onSide: Side
) : PuzzlePieceFeature

data class PuzzleBlank( // An indentation on a puzzle piece is called a 'blank'
    override val onSide: Side
) : PuzzlePieceFeature

data class PuzzlePiece(
//    var height: Float = 10f,
//    var width: Float = 10f,
    val tabs: List<PuzzleTab> = emptyList(),
    val blanks: List<PuzzleBlank> = emptyList()
) {
    // Validate the PuzzlePiece on initialization
    init {
        if (tabs.intersect(blanks.toSet()).isNotEmpty()) {
            throw InvalidPuzzlePieceException(
                "PuzzlePiece has overlapping tabs and blanks!" +
                    "\ntabs = $tabs" +
                    "\nblanks = $blanks"
            )
        }
    }
}

class InvalidPuzzlePieceException(message: String) : IllegalArgumentException(message)
