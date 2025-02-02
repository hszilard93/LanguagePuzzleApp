package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.serialization.RulesetSerializer
import kotlinx.serialization.Serializable

@Serializable
enum class TaskType(var ruleset: Ruleset) {
    PLACE_PUZZLES_IN_ORDER(Ruleset()),     // Készítsd el az előre megadott mondat puzzle-szerkezetét.
    EDIT_PUZZLES(Ruleset(                  // Szerkeszd a meglévő puzzle darabokat
        canAddMainPieces = false,
        canAddBlankPieces = false,
        canAddRemoveTabs = true,
        canColorTabs = true,
        canEditBaseText = true,
        canEditTabText = true,
        doesAllowTabText = true,
        doesBaseTextCount = true,
        doesBlankTextCount = true
    )),
    FREE_EDIT_PUZZLE(Ruleset(
        canAddMainPieces = true,
        canAddBlankPieces = true,
        canAddRemoveTabs = true,
        canColorTabs = true,
        canEditBaseText = true,
        canEditTabText = true,
        doesAllowTabText = true,
        doesBaseTextCount = true,
        doesBlankTextCount = true
    )),
    MATCH_PUZZLE(Ruleset()),               // Mondat társítása a puzzle-szerkezethez.
    COMPLETE_PUZZLE(Ruleset()),            //
    COMPLETE_ARGUMENTS(Ruleset(            // Készítsd el helyesen a megadott ige vonzatait.
        canAddMainPieces = false,
        canAddBlankPieces = false,
        canAddRemoveTabs = true,
        canColorTabs = true,
        canEditBaseText = false,
        canEditTabText = true,
        doesAllowTabText = true,
        doesBaseTextCount = false,
        doesBlankTextCount = false
    ))
}

@Serializable(with = RulesetSerializer::class)
data class Ruleset(
    val canAddMainPieces: Boolean? = null,
    val canAddBlankPieces: Boolean? = null,
    val canAddRemoveTabs: Boolean? = null,
    val canColorTabs: Boolean? = null,
    val canEditBaseText: Boolean? = null,
    val canEditTabText: Boolean? = null,
    val doesAllowTabText: Boolean? = null,
    val doesBaseTextCount: Boolean? = null,
    val doesBlankTextCount: Boolean? = null,
    val shouldOfferPostpositions: Boolean? = null,      // Névutók
    val shouldOfferIndPronouns: Boolean? = null,        // Jelentéscímkék
)

@Serializable
data class Exercise(
    val type: TaskType,
    val buttonDescription: String = "",
    val customRuleset: Ruleset? = null,
    val tasks: List<Task>
) {
    init {
        if (customRuleset != null) {
            val defaultRuleset = type.ruleset   // Load the default ruleset
            type.ruleset = Ruleset(
                canAddMainPieces = customRuleset.canAddMainPieces ?: defaultRuleset.canAddMainPieces,
                canAddBlankPieces = customRuleset.canAddBlankPieces ?: defaultRuleset.canAddBlankPieces,
                canAddRemoveTabs = customRuleset.canAddRemoveTabs ?: defaultRuleset.canAddRemoveTabs,
                canColorTabs = customRuleset.canColorTabs ?: defaultRuleset.canColorTabs,
                canEditBaseText = customRuleset.canEditBaseText ?: defaultRuleset.canEditBaseText,
                canEditTabText = customRuleset.canEditTabText ?: defaultRuleset.canEditTabText,
                doesAllowTabText = customRuleset.doesAllowTabText ?: defaultRuleset.doesAllowTabText,
                doesBaseTextCount = customRuleset.doesBaseTextCount ?: defaultRuleset.doesBaseTextCount,
                doesBlankTextCount = customRuleset.doesBlankTextCount ?: defaultRuleset.doesBlankTextCount
            )
        }
    }
}

@Serializable
data class Task(
    val taskDescription: String,
    val predefinedPieces: Set<PuzzlePiece>,
    val requiredSolutions: Int = 1,
    val solutionConfigurations: List<SolutionConfiguration>
)
