package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.graphics.Color
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleBlank
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
    var snapFeature: PuzzlePieceFeature? = null
    var snapTarget: PuzzlePieceFeature? = null

    fun performSnap() {
        logger.info { "performSnap snapFeature=$snapFeature snapTarget=$snapTarget" }

        if (snapFeature != null && snapTarget != null) {
            val delta = snapFeature!!.getFeatureMidpoint().sub(snapTarget!!.getFeatureMidpoint())
            val puzzleToSnap = snapFeature!!.owner
            puzzleToSnap.pos = puzzleToSnap.pos.sub(delta)
        }
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
                if (distance < minDistance) {
                    minDistance = distance
                    closestPair = Pair(feature, target)
                }
            }
        }

        // Reset all glowColors
        gameModel.puzzlePieces.forEach { puzzlePiece ->
            puzzlePiece.getAllFeatures().forEach { feature ->
                feature.glowColor = null
            }
        }

        // Apply glow colors if the closest pair is within the threshold
        if (closestPair != null && minDistance <= SNAPPING_THRESHOLD) {
            snapFeature = closestPair.first
            snapTarget = closestPair.second
            logger.info { "snapFeature=(${snapFeature!!.getType()}, ${snapFeature!!.side}, ${snapFeature!!.getFeatureMidpoint()}), targetFeature=(${snapTarget!!.getType()}, ${snapTarget!!.side}, ${snapTarget!!.getFeatureMidpoint()}), distance=$minDistance" }
            snapTarget!!.glowColor = Color.CYAN
            snapFeature!!.glowColor = Color.VIOLET
            logger.debug { "Snapping pair found with distance=$minDistance: $snapFeature and $snapTarget" }
        }
        else {
            logger.debug { "No snapping pair within threshold found." }
            snapFeature = null
            snapTarget = null
        }
    }

    fun clearPuzzleFeaturesByProximity() {
        logger.debug { "clearPuzzleFeaturesByProximity" }
        gameModel.puzzlePieces.flatMap { it.getAllFeatures() }.forEach { it.glowColor = null }
    }
}
