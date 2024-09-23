package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import ktx.log.Logger
import ktx.log.logger

const val LOG_LEVEL_MISC = 4

class GameModel {

    val puzzlePieces: MutableList<PuzzlePiece> = ArrayList()

    //    val basePosition = Vector2(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f)
    private val basePosition = Vector2(0f, 0f)

    companion object {
        val logger = logger<GameModel>()
    }

    init {
        logger.debug { "init" }

        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(basePosition.x + 300f, basePosition.y + 401f),
                color = Color.ROYAL,
                depth = 1
            ).apply {
                tabs.addAll(listOf(PuzzleTab(this, Side.BOTTOM), PuzzleTab(this, Side.TOP)))
                blanks.addAll(listOf(PuzzleBlank(this, Side.LEFT), PuzzleBlank(this, Side.RIGHT)))
            }
        )

        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(basePosition.x + 300f, basePosition.y + 100f),
                color = Color.YELLOW
            ).apply {
                tabs.addAll(listOf(PuzzleTab(this, Side.LEFT)))
                blanks.addAll(listOf(PuzzleBlank(this, Side.TOP), PuzzleBlank(this, Side.RIGHT), PuzzleBlank(this, Side.BOTTOM)))
            }
        )

        puzzlePieces.add(
            PuzzlePiece(
                pos = Vector2(basePosition.x + 601f, basePosition.y + 100f),
                color = Color.OLIVE,
                depth = 0
            ).apply {
                tabs.addAll(listOf(PuzzleTab(this, Side.LEFT, Color.CHARTREUSE), PuzzleTab(this, Side.TOP, Color.SKY)))
                blanks.addAll(listOf(PuzzleBlank(this, Side.BOTTOM), PuzzleBlank(this, Side.RIGHT)))
            }
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
