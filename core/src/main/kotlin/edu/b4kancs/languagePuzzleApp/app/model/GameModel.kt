package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

const val LOG_LEVEL_MISC = 4

class GameModel {

    val puzzlePieces: MutableList<PuzzlePiece> = ArrayList()

    init {
        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(300f, 401f),
                tabs = listOf(PuzzleTab(Side.BOTTOM), PuzzleTab(Side.RIGHT), PuzzleTab(Side.TOP)),
                blanks = listOf(PuzzleBlank(Side.LEFT)),
                color = Color.ROYAL,
                depth = 1
            )
        )

        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(300f, 100f),
                tabs = listOf(PuzzleTab(Side.LEFT)),
                blanks = listOf(PuzzleBlank(Side.TOP), PuzzleBlank(Side.RIGHT), PuzzleBlank(Side.BOTTOM)),
                color = Color.YELLOW
            )
        )
    }
}
