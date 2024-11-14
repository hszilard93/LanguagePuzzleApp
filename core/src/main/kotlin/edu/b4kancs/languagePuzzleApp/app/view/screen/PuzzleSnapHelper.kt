package edu.b4kancs.languagePuzzleApp.app.view.screen

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
    private var snapFeature: PuzzlePieceFeature? = null
    private var targetFeature: PuzzlePieceFeature? = null

    fun performSnapIfAny() {
        logger.info { "performSnap snapFeature=$snapFeature snapTarget=$targetFeature" }

        if (snapFeature == null || targetFeature == null) return

//            val (maleFeature: PuzzleTab, femaleFeature: PuzzleBlank) =
//                if (snapFeature is PuzzleTab) (snapFeature as PuzzleTab) to (targetFeature as PuzzleBlank)
//                else (targetFeature as PuzzleTab) to (snapFeature as PuzzleBlank)
//
        val snapPiece = snapFeature!!.owner!!
        val targetPiece = targetFeature!!.owner!!

        adjustSizeIfNecessary(snapPiece, targetPiece, snapFeature!!, targetFeature!!)

        val delta = snapFeature!!.getFeatureMidpoint().sub(targetFeature!!.getFeatureMidpoint())
        val puzzleToSnap = snapFeature!!.owner!!
        puzzleToSnap.pos = puzzleToSnap.pos.sub(delta)

        val newConnection =
            Connection(
                setOf(snapPiece, targetPiece),
                (if (snapFeature is PuzzleTab) snapFeature else targetFeature) as PuzzleTab,
                GrammaticalRole.ADVERBIAL
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
                val isTargetConnected = target in target.owner!!.copyOfConnections.map { it.via }

                if (distance < minDistance && !isTargetConnected) {
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
            snapFeature = closestPair.first
            targetFeature = closestPair.second
            logger.info { "snapFeature=(${snapFeature!!.getType()}, ${snapFeature!!.side}, ${snapFeature!!.getFeatureMidpoint()}), targetFeature=(${targetFeature!!.getType()}, ${targetFeature!!.side}, ${targetFeature!!.getFeatureMidpoint()}), distance=$minDistance" }
            targetFeature!!.isGlowing = true
            snapFeature!!.isGlowing = true
            logger.debug { "Snapping pair found with distance=$minDistance: $snapFeature and $targetFeature" }
        }
        else {
            logger.debug { "No snapping pair within threshold found." }
            snapFeature = null
            targetFeature = null
        }
    }

    fun clearPuzzleFeaturesByProximity() {
        logger.debug { "clearPuzzleFeaturesByProximity" }
        gameModel.puzzlePieces.flatMap { it.getAllFeatures() }.forEach { it.isGlowing = false }
    }
}
