package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.Connection
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import kotlinx.serialization.Serializable

@Serializable
enum class TaskType {
    PLACE_PUZZLES_IN_ORDER,     // Készítsd el az előre megadott mondat puzzle-szerkezetét.
    CREATE_PUZZLE,              // Add hozzá a puzzle darabokat, majd rakd őket sorrendbe.
    MATCH_PUZZLE,               // Mondat társítása a puzzle-szerkezethez.
    COMPLETE_PUZZLE             //
}

@Serializable
data class Exercise(
    val type: TaskType,
    val taskDescription: String,
    val predefinedPieces: Set<PuzzlePiece>,
    val solutionConfiguration: Set<Connection>
)
