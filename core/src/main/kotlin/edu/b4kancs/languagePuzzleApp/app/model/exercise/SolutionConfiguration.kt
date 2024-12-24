package edu.b4kancs.languagePuzzleApp.app.model.exercise

import edu.b4kancs.languagePuzzleApp.app.model.Connection
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import kotlinx.serialization.Serializable

@Serializable
class SolutionConfiguration(private val solutions: Set<Connection>) {

    fun doesGameStateMatchSolution(puzzles: List<PuzzlePiece>): Boolean {
        if (solutions.isEmpty()) return false

        val verbConnections = puzzles.find { it.grammaticalRole == GrammaticalRole.VERB }?.copyOfConnections

        if (verbConnections == null) return false
        if (verbConnections.size != solutions.size) return false

        verbConnections.forEach { c1 ->
            // If the connection is not matched by any connection solution, return false
            if (!solutions.any { c2 -> c2.matches(c1) }) return false
        }

        return true
    }

    private fun Connection.matches(other: Connection): Boolean {
        val theseTexts = this.puzzlesConnected.map { it.text }.toSet()
        val thoseTexts = other.puzzlesConnected.map { it.text}.toSet()

        if (this.via.grammaticalRole != other.via.grammaticalRole) return false

        return theseTexts == thoseTexts

        // TODO: Fix incorrect role of connection
//        if (this.roleOfConnection != other.roleOfConnection) return false
    }
}
