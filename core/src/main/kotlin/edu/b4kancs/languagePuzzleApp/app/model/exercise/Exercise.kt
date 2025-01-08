package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import kotlinx.serialization.Serializable

@Serializable
enum class TaskType(var ruleset: Ruleset) {
    PLACE_PUZZLES_IN_ORDER(Ruleset()),     // Készítsd el az előre megadott mondat puzzle-szerkezetét.
    CREATE_PUZZLE(Ruleset(                 // Add hozzá a puzzle darabokat, majd rakd őket sorrendbe.
        canAddMainPieces = false,
        canAddBlankPieces = false,
        canAddRemoveTabs = true,
        canEditBaseText = true,
        canEditTabText = true
    )),
    MATCH_PUZZLE(Ruleset()),               // Mondat társítása a puzzle-szerkezethez.
    COMPLETE_PUZZLE(Ruleset()),            //
    COMPLETE_ARGUMENTS(Ruleset()),         // Készítsd el helyesen a megadott ige vonzatait.
}

@Serializable
data class Ruleset(
    val canAddMainPieces: Boolean = false,
    val canAddBlankPieces: Boolean = false,
    val canAddRemoveTabs: Boolean = false,
    val canEditBaseText: Boolean = false,
    val canEditTabText: Boolean = false,
)

@Serializable
data class Exercise(
    val type: TaskType,
    val buttonDescription: String = "",
    val taskDescription: String,
    val predefinedPieces: Set<PuzzlePiece>,
    val solutionConfiguration: SolutionConfiguration,
    val customRuleset: Ruleset? = null
) {
    init {
        if (customRuleset != null) {
            type.ruleset = customRuleset
        }
    }
}
