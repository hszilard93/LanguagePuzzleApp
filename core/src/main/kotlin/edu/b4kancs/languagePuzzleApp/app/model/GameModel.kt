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
                text = "ad",
                grammaticalRole = GrammaticalRole.VERB,
                pos = Vector2(basePosition.x + 400f, basePosition.y + 300f),
                depth = 1
            ).apply {
                tabs.addAll(listOf(
                    PuzzleTab(this, Side.LEFT, GrammaticalRole.SUBJECT),
                    PuzzleTab(this, Side.TOP, GrammaticalRole.OBJECT, text = "-t"),
                    PuzzleTab(this, Side.RIGHT, GrammaticalRole.ADVERBIAL, text = "-nak\n/nek")
                ))
            }
        )

        puzzlePieces.add(
            PuzzlePiece(
                text = "Peti",
                grammaticalRole = GrammaticalRole.SUBJECT,
                pos = Vector2(basePosition.x + 0f, basePosition.y + 300f)
            ).apply {
                blanks.addAll(listOf(PuzzleBlank(this, Side.RIGHT)))
            }
        )

        puzzlePieces.add(
            PuzzlePiece(
                text = "virág",
                grammaticalRole = GrammaticalRole.OBJECT,
                pos = Vector2(basePosition.x + 400f, basePosition.y + 700),
                depth = 0
            ).apply {
                blanks.addAll(listOf(PuzzleBlank(this, Side.BOTTOM)))
            }
        )

        puzzlePieces.add(
            PuzzlePiece(
                text = "Eszti",
                grammaticalRole = GrammaticalRole.ADVERBIAL,
                pos = Vector2(basePosition.x + 800f, basePosition.y + 300f),
                depth = 0
            ).apply {
                blanks.addAll(listOf(PuzzleBlank(this, Side.LEFT)))
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
