package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.serialization.RulesetSerializer
import kotlinx.serialization.Serializable

@Serializable
enum class TaskType(val defaultRuleset: Ruleset) {
    PLACE_PUZZLES_IN_ORDER(Ruleset()),     // Készítsd el az előre megadott mondat puzzle-szerkezetét.
    EDIT_PUZZLES(
        Ruleset(                  // Szerkeszd a meglévő puzzle darabokat
            canAddMainPieces = false,
            canAddBlankPieces = false,
            canAddRemoveTabs = true,
            canColorTabs = true,
            canEditBaseText = true,
            canEditTabText = true,
            doesAllowTabText = true,
            doesBaseTextCount = true,
            doesBlankTextCount = true
        )
    ),
    FREE_EDIT_PUZZLE(
        Ruleset(
            canAddMainPieces = true,
            canAddBlankPieces = true,
            canAddRemoveTabs = true,
            canColorTabs = true,
            canEditBaseText = true,
            canEditTabText = true,
            doesAllowTabText = true,
            doesBaseTextCount = true,
            doesBlankTextCount = true
        )
    ),
    MATCH_PUZZLE(Ruleset()),                // Mondat társítása a puzzle-szerkezethez.
    COMPLETE_PUZZLE(Ruleset()),             //
    COMPLETE_ARGUMENTS(
        Ruleset(                            // Készítsd el helyesen a megadott ige puzzlejét.
            canAddMainPieces = false,
            canAddBlankPieces = false,
            canAddRemoveTabs = true,
            canColorTabs = true,
            canEditBaseText = false,
            canEditTabText = true,
            doesAllowTabText = true,
            doesBaseTextCount = true,
            doesBlankTextCount = false
        )
    )
}

@Serializable(with = RulesetSerializer::class)
data class Ruleset(
    val canAddMainPieces: Boolean? = null,
    val canAddBlankPieces: Boolean? = null,
    val canAddRemoveTabs: Boolean? = null,
    val canColorTabs: Boolean? = null,
    val canEditBaseText: Boolean? = null,
    val canEditBlankText: Boolean? = null,
    val canEditTabText: Boolean? = null,
    val doesAllowTabText: Boolean? = null,
    val doesBaseTextCount: Boolean? = null,
    val doesBlankTextCount: Boolean? = null,
    val shouldOfferPostpositions: Boolean? = null,      // Névutók
    val shouldOfferIndPronouns: Boolean? = null,        // Jelentéscímkék
    val doNotOfferSuffixes: Boolean? = null,            // NE ajánljon toldalékokat
)

@Serializable
data class Exercise(
    val type: TaskType,
    val buttonDescription: String = "",
    private val customRuleset: Ruleset? = null,
    val ruleset: Ruleset = initRuleset(customRuleset, type),
    val tasks: List<Task>
) {
    companion object {
        private fun initRuleset(customRuleset: Ruleset?, type: TaskType): Ruleset {
            return if (customRuleset != null) {
                val defaultRuleset = type.defaultRuleset   // Load the default ruleset
                Ruleset(
                    canAddMainPieces = customRuleset.canAddMainPieces ?: defaultRuleset.canAddMainPieces,
                    canAddBlankPieces = customRuleset.canAddBlankPieces ?: defaultRuleset.canAddBlankPieces,
                    canAddRemoveTabs = customRuleset.canAddRemoveTabs ?: defaultRuleset.canAddRemoveTabs,
                    canColorTabs = customRuleset.canColorTabs ?: defaultRuleset.canColorTabs,
                    canEditBaseText = customRuleset.canEditBaseText ?: defaultRuleset.canEditBaseText,
                    canEditTabText = customRuleset.canEditTabText ?: defaultRuleset.canEditTabText,
                    doesAllowTabText = customRuleset.doesAllowTabText ?: defaultRuleset.doesAllowTabText,
                    doesBaseTextCount = customRuleset.doesBaseTextCount ?: defaultRuleset.doesBaseTextCount,
                    doesBlankTextCount = customRuleset.doesBlankTextCount ?: defaultRuleset.doesBlankTextCount,
                    shouldOfferPostpositions = customRuleset.shouldOfferPostpositions ?: defaultRuleset.shouldOfferPostpositions,
                    shouldOfferIndPronouns = customRuleset.shouldOfferIndPronouns ?: defaultRuleset.shouldOfferIndPronouns
                )
            }
            else {
                type.defaultRuleset
            }
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
