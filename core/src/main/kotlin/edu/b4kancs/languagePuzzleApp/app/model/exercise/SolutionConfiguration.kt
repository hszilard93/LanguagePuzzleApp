package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.Connection
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.Suffix
import edu.b4kancs.languagePuzzleApp.app.serialization.SolutionConfigurationSerializer
import kotlinx.serialization.Serializable

enum class SolutionResult {
    CORRECT, INCORRECT, INELIGIBLE
}

@Serializable(with = SolutionConfigurationSerializer::class)
class SolutionConfiguration(
    private val solutionSet: Set<Connection>,
    private val checkTabText: Boolean = false
) {

    /*
        To check the validity of the solution, only the solutions of the central puzzle piece (the verb) are taken into account.
        The text of the tabs is optionally taken into account.
     */

    fun doesGameStateMatchSolution(puzzles: List<PuzzlePiece>): SolutionResult {
        if (solutionSet.isEmpty()) return SolutionResult.INELIGIBLE

        val verbConnections = puzzles.find { it.grammaticalRole == GrammaticalRole.VERB }?.copyOfConnections

        if (verbConnections == null) return SolutionResult.INELIGIBLE
        if (verbConnections.size != solutionSet.size) return SolutionResult.INELIGIBLE

        verbConnections.forEach { c1 ->
            // If the connection is not matched by any connection solution, return false
            if (!solutionSet.any { c2 -> c2.matches(c1, checkTabText) }) return SolutionResult.INCORRECT
        }

        return SolutionResult.CORRECT
    }

    private fun Connection.matches(other: Connection, checkTabText: Boolean): Boolean {
        val theseTexts = this.puzzlesConnected.map { it.text.lowercase() }.toSet()
        val thoseTexts = other.puzzlesConnected.map { it.text.lowercase() }.toSet()

        if (this.via.grammaticalRole != other.via.grammaticalRole) return false
        if (checkTabText) {
            val thisSuffix = Suffix.identifySuffixFromTabText(this.via.text.lowercase())
            val thatSuffix = Suffix.identifySuffixFromTabText(other.via.text.lowercase())
            if (thisSuffix != thatSuffix) {
                return false
            }
        }

        return theseTexts == thoseTexts

        // TODO: Fix incorrect role of connection
//        if (this.roleOfConnection != other.roleOfConnection) return false
    }
}
