package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import edu.b4kancs.languagePuzzleApp.app.model.Connection
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleBlank
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePieceFeature
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import ktx.collections.GdxMap
import ktx.log.logger

class PuzzleSnapHelper(private val gameModel: GameModel) {

    companion object {
        val logger = logger<PuzzleSnapHelper>()
        const val SNAPPING_THRESHOLD = 75f
    }

    private val puzzleFeatureCompatibilityMap = GdxMap<PuzzlePieceFeature, List<PuzzlePieceFeature>>()
    private var innerSnapFeature: PuzzlePieceFeature? = null
    private var innerTargetFeature: PuzzlePieceFeature? = null

    // Uses the innerSnapFeature and innerTargetFeature to snap the pieces together
    fun performSnapIfAny() {
        logger.info { "performSnap snapFeature=$innerSnapFeature snapTarget=$innerTargetFeature" }
        if (innerSnapFeature == null || innerTargetFeature == null) return
        performSnapLogic(innerSnapFeature!!, innerTargetFeature!!)
    }

    // Can be called in the initial puzzle placing phase to force a snap between two features
    // The target feature is the one who's puzzle remains fixed!
    fun performForcedSnap(snapFeature: PuzzlePieceFeature, targetFeature: PuzzlePieceFeature) {
        logger.debug { "performForcedSnap snapFeature=$snapFeature targetFeature=$targetFeature" }
        performSnapLogic(snapFeature, targetFeature)
    }

    private fun performSnapLogic(snapFeature: PuzzlePieceFeature, targetFeature: PuzzlePieceFeature) {
        val snapPiece = snapFeature.owner!!
        val targetPiece = targetFeature.owner!!

        adjustSizeIfNecessary(snapPiece, targetPiece, snapFeature, targetFeature)

        val delta = snapFeature.getFeatureMidpoint().sub(targetFeature.getFeatureMidpoint())
        val puzzleToSnap = snapFeature.owner!!
        puzzleToSnap.pos = puzzleToSnap.pos.sub(delta)

        val puzzleTab = (if (snapFeature is PuzzleTab) snapFeature else targetFeature) as PuzzleTab
        val newConnection =
            Connection(
                setOf(snapPiece, targetPiece),
                puzzleTab,
//                GrammaticalRole.ADVERBIAL
                puzzleTab.grammaticalRole
            )

        snapPiece.addConnection(newConnection)
        targetPiece.addConnection(newConnection)
    }

    fun updatePuzzleFeatureCompatibilityMap(feature: PuzzlePieceFeature) {
        logger.debug { "updatePuzzleFeatureCompatibilityMap feature=$feature" }

        if (!puzzleFeatureCompatibilityMap.containsKey(feature)) {
            val compatibles =
                gameModel.puzzlePieces
                    .flatMap { it.getAllFeatures().minus(feature) }
                    .filter { feature.side.opposite() == it.side }
                    .filter { other -> (feature is PuzzleTab && other is PuzzleBlank) || (feature is PuzzleBlank && other is PuzzleTab) }

            puzzleFeatureCompatibilityMap.put(feature, compatibles)
        }
    }

    fun clearPuzzleFeatureCompatibilityMap(feature: PuzzlePieceFeature) {
        logger.debug { "clearPuzzleFeatureCompatibilityMap feature=$feature" }
        puzzleFeatureCompatibilityMap.clear()
//        puzzleFeatureCompatibilityMap.put(feature, emptyList())
    }

    // Make the smaller puzzle grow to the matching size
    private fun adjustSizeIfNecessary(
        puzzle1: PuzzlePiece,
        puzzle2: PuzzlePiece,
        feature1: PuzzlePieceFeature,
        feature2: PuzzlePieceFeature
    ) {
        logger.debug { "adjustSizeIfNecessary snapPiece=$puzzle1 targetPiece=$puzzle2" }

        if (puzzle2.size == puzzle1.size) return

        val smallerPuzzle: PuzzlePiece
        val largerPuzzle: PuzzlePiece
        val smallerFeature: PuzzlePieceFeature

        if (puzzle1.size < puzzle2.size) {
            smallerPuzzle = puzzle1
            largerPuzzle = puzzle2
            smallerFeature = feature1
        }
        else {
            smallerPuzzle = puzzle2
            largerPuzzle = puzzle1
            smallerFeature = feature2
        }

        if (smallerPuzzle.isConnected) return

        val preMidpoint = smallerFeature.getFeatureMidpoint()
        smallerPuzzle.changeSize(largerPuzzle.size, false)
        val postGrowthDelta = smallerFeature.getFeatureMidpoint().sub(preMidpoint)
        smallerPuzzle.pos = smallerPuzzle.pos.sub(postGrowthDelta)
        logger.debug { "smallerPuzzle = \"${smallerPuzzle.text}\" largerPuzzle = \"${largerPuzzle.text}\" \n\t\tpostGrowthDelta = $postGrowthDelta" }
    }

    fun updatePuzzleFeaturesByProximity() {
        logger.debug { "updatePuzzleFeaturesByProximity" }

        var minDistance = Float.MAX_VALUE
        var closestPair: Pair<PuzzlePieceFeature, PuzzlePieceFeature>? = null

        // Iterate through all entries in the compatibility map
        for (item in puzzleFeatureCompatibilityMap) {
            val feature = item.key
            val compatibles = item.value

            for (target in compatibles) {
                val distance = feature.getFeatureMidpoint().dst(target.getFeatureMidpoint())
                val isFeatureConnected = feature in target.owner!!.copyOfConnections.map { it.via }

                if (distance < minDistance && !isFeatureConnected) {
                    minDistance = distance
                    closestPair = Pair(feature, target)
                }
            }
        }

        // Reset all glowColors
        gameModel.puzzlePieces.forEach { puzzlePiece ->
            puzzlePiece.getAllFeatures().forEach { feature ->
                feature.isGlowing = false
            }
        }

        // Apply glow colors if the closest pair is within the threshold
        if (closestPair != null && minDistance <= SNAPPING_THRESHOLD) {
            innerSnapFeature = closestPair.first
            innerTargetFeature = closestPair.second
            logger.info { "snapFeature=(${innerSnapFeature!!.getType()}, ${innerSnapFeature!!.side}, ${innerSnapFeature!!.getFeatureMidpoint()}), targetFeature=(${innerTargetFeature!!.getType()}, ${innerTargetFeature!!.side}, ${innerTargetFeature!!.getFeatureMidpoint()}), distance=$minDistance" }
            innerTargetFeature!!.isGlowing = true
            innerSnapFeature!!.isGlowing = true
            logger.debug { "Snapping pair found with distance=$minDistance: $innerSnapFeature and $innerTargetFeature" }
        }
        else {
            logger.debug { "No snapping pair within threshold found." }
            innerSnapFeature = null
            innerTargetFeature = null
        }
    }

    fun clearPuzzleFeaturesByProximity() {
        logger.debug { "clearPuzzleFeaturesByProximity" }
        gameModel.puzzlePieces.flatMap { it.getAllFeatures() }.forEach { it.isGlowing = false }
    }
}
