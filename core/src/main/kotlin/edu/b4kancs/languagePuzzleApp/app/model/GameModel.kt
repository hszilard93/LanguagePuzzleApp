package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import ktx.log.Logger
import ktx.log.logger

const val LOG_LEVEL_MISC = 4

class GameModel {

    val puzzlePieces: MutableList<PuzzlePiece> = ArrayList()

    //    val basePosition = Vector2(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f)
    val basePosition = Vector2(0f, 0f)

    companion object {
        val logger = logger<GameModel>()
    }

    init {
        logger.debug { "init" }

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
                pos = Vector2(basePosition.x + 601f, basePosition.y + 100f),
                tabs = listOf(PuzzleTab(Side.LEFT, Color.CHARTREUSE), PuzzleTab(Side.TOP, Color.SKY)),
                blanks = listOf(PuzzleBlank(Side.BOTTOM)),
                color = Color.YELLOW,
                depth = 0
            )
        )
    }

    fun rebasePuzzleDepths() {
        logger.debug { "rebasePuzzleDepths" }
        if (puzzlePieces.isNotEmpty()) {
            val minDepth = puzzlePieces.minOf { it.depth }
            puzzlePieces.forEach { it.depth -= minDepth }
        }
    }
}
