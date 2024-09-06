package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.Game.Companion.WORLD_HEIGHT
import edu.b4kancs.languagePuzzleApp.app.Game.Companion.WORLD_WIDTH

const val LOG_LEVEL_MISC = 4

class GameModel {

    val puzzlePieces: MutableList<PuzzlePiece> = ArrayList()
//    val basePosition = Vector2(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f)
    val basePosition = Vector2(0f, 0f)

    init {
        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(basePosition.x + 300f, basePosition.y + 401f),
                tabs = listOf(PuzzleTab(Side.BOTTOM), PuzzleTab(Side.RIGHT), PuzzleTab(Side.TOP)),
                blanks = listOf(PuzzleBlank(Side.LEFT)),
                color = Color.ROYAL,
                depth = 1
            )
        )

        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(basePosition.x + 300f, basePosition.y + 100f),
                tabs = listOf(PuzzleTab(Side.LEFT)),
                blanks = listOf(PuzzleBlank(Side.TOP), PuzzleBlank(Side.RIGHT), PuzzleBlank(Side.BOTTOM)),
                color = Color.YELLOW
            )
        )

        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(basePosition.x + 700f, basePosition.y + 350f),
                tabs = listOf(PuzzleTab(Side.LEFT, Color.CHARTREUSE), PuzzleTab(Side.TOP, Color.SKY)),
                blanks = listOf(PuzzleBlank(Side.BOTTOM)),
                color = Color.YELLOW,
                depth = 2
            )
        )
    }
}
