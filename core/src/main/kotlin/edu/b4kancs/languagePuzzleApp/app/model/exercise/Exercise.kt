package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece

enum class TaskType {
    PUZZLE_ORDER,
    CREATE_PUZZLE,
    MATCH_PUZZLE,
    COMPLETE_PUZZLE
}

data class Exercise(
    val type: TaskType,
    val task: String,
    val predefinedPieces: Set<PuzzlePiece>,
    val solutionConfiguration: List<PuzzlePiece>
)
