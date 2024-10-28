package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece

enum class TaskType {
    PUZZLE_ORDER,
    CREATE_PUZZLE,
    MATCH_PUZZLE,
    COMPLETE_PUZZLE
}

data class ExerciseModel(
    val type: TaskType,
    val sentence: String,
    val predefinedPieces: List<PuzzlePiece>,
)
