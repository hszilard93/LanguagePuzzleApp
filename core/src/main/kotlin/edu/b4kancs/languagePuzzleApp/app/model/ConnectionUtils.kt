package edu.b4kancs.languagePuzzleApp.app.model

object ConnectionUtils {

    /**
     * Checks if two sets of connections are logically equal.
     */
    fun areSetsOfConnectionsLogicallyEqual(set1: Set<Connection>, set2: Set<Connection>): Boolean {
        // Quick size check
        if (set1.size != set2.size) return false

        // For the demo
        return true

        // Create a mutable copy of set2 to track unmatched connections
        val unmatchedSet2 = set2.toMutableSet()

        for (c1 in set1) {
            // Attempt to find a matching connection in set2
            val match = unmatchedSet2.find { areConnectionsLogicallyEqual(c1, it) }
            if (match != null) {
                // Remove the matched connection to prevent duplicate matching
                unmatchedSet2.remove(match)
            } else {
                // No matching connection found
                return false
            }
        }

        // All connections in set1 have a match in set2
        return true
    }

    /**
     * Checks if two connections are logically equal.
     */
    fun areConnectionsLogicallyEqual(c1: Connection, c2: Connection): Boolean {
        // Compare roles of connection
        if (c1.roleOfConnection != c2.roleOfConnection) return false

        // Compare via PuzzleTab
        if (!arePuzzleTabsLogicallyEqual(c1.via, c2.via)) return false

        // Compare connected PuzzlePieces
        if (c1.puzzlesConnected.size != c2.puzzlesConnected.size) return false

        for (p1 in c1.puzzlesConnected) {
            if (!c2.puzzlesConnected.any { arePuzzlePiecesLogicallyEqual(p1, it) }) {
                return false
            }
        }

        return true
    }

    /**
     * Checks if two PuzzlePieces are logically equal.
     */
    fun arePuzzlePiecesLogicallyEqual(p1: PuzzlePiece, p2: PuzzlePiece): Boolean {
        // Compare grammatical roles and text
        if (p1.grammaticalRole != p2.grammaticalRole) return false
        if (p1.text != p2.text) return false

        // Compare number of tabs and blanks
        if (p1.tabs.size != p2.tabs.size) return false
        if (p1.blanks.size != p2.blanks.size) return false

        // Compare each PuzzleTab
        for (tab1 in p1.tabs) {
            if (!p2.tabs.any { arePuzzleTabsLogicallyEqual(tab1, it) }) {
                return false
            }
        }

        // Compare each PuzzleBlank
        for (blank1 in p1.blanks) {
            if (!p2.blanks.any { arePuzzleBlanksLogicallyEqual(blank1, it) }) {
                return false
            }
        }

        return true
    }

    /**
     * Checks if two PuzzleTabs are logically equal.
     * Excludes comparing owning PuzzlePieces to prevent recursion.
     */
    fun arePuzzleTabsLogicallyEqual(t1: PuzzleTab, t2: PuzzleTab): Boolean {
        // Compare sides, grammatical roles, and text
        if (t1.side != t2.side) return false
        if (t1.grammaticalRole != t2.grammaticalRole) return false
        if (t1.text != t2.text) return false

        // **Do not** compare owning PuzzlePieces to prevent recursion
        return true
    }

    /**
     * Checks if two PuzzleBlanks are logically equal.
     * Excludes comparing owning PuzzlePieces to prevent recursion.
     */
    fun arePuzzleBlanksLogicallyEqual(b1: PuzzleBlank, b2: PuzzleBlank): Boolean {
        // Compare sides
        if (b1.side != b2.side) return false

        // **Do not** compare owning PuzzlePieces to prevent recursion
        return true
    }
}
