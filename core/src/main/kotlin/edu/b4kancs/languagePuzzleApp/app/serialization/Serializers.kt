package edu.b4kancs.languagePuzzleApp.app.serialization

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import edu.b4kancs.languagePuzzleApp.app.model.Connection
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleBlank
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.model.exercise.SolutionConfiguration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import ktx.collections.GdxSet
import ktx.collections.toGdxSet


@Serializer(forClass = ObjectSet::class)
class GdxSetSerializer<T>(
    private val elementSerializer: KSerializer<T>
) : KSerializer<ObjectSet<T>> {

    override val descriptor: SerialDescriptor =
        SetSerializer(elementSerializer).descriptor

    override fun serialize(encoder: Encoder, value: ObjectSet<T>) {
        // Convert ObjectSet to Kotlin Set
        val standardSet: Set<T> = value.toSet()
        // Delegate serialization to the standard Set serializer
        SetSerializer(elementSerializer).serialize(encoder, standardSet)
    }

    override fun deserialize(decoder: Decoder): ObjectSet<T> {
        // Delegate deserialization to the standard Set serializer
        val standardSet: Set<T> = SetSerializer(elementSerializer).deserialize(decoder)
        // Convert Kotlin Set back to ObjectSet
        return standardSet.toGdxSet()
    }

    companion object {
        // Helper function to create a serializer with reified type
        inline fun <reified T> serializer(): GdxSetSerializer<T> =
            GdxSetSerializer(elementSerializer = kotlinx.serialization.serializer())
    }
}

@Serializer(forClass = PuzzlePiece::class)
object PuzzlePieceSerializer : KSerializer<PuzzlePiece> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PuzzlePiece") {
        element<String>("text")
        element<GrammaticalRole>("grammaticalRole")
        element<List<PuzzleTab>>("tabs")
        element<List<PuzzleBlank>>("blanks")
    }

    override fun serialize(encoder: Encoder, value: PuzzlePiece) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, value.text)
        composite.encodeSerializableElement(descriptor, 1, GrammaticalRole.serializer(), value.grammaticalRole)
        composite.encodeSerializableElement(descriptor, 2, ListSerializer(PuzzleTab.serializer()), value.tabs)
        composite.encodeSerializableElement(descriptor, 3, ListSerializer(PuzzleBlank.serializer()), value.blanks)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): PuzzlePiece {
        val dec = decoder.beginStructure(descriptor)
        var text: String = ""
        var grammaticalRole: GrammaticalRole = GrammaticalRole.SUBJECT // default value
        var tabs: List<PuzzleTab> = emptyList()
        var blanks: List<PuzzleBlank> = emptyList()

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> text = dec.decodeStringElement(descriptor, 0)
                1 -> grammaticalRole = dec.decodeSerializableElement(descriptor, 1, GrammaticalRole.serializer())
                2 -> tabs = dec.decodeSerializableElement(descriptor, 2, ListSerializer(PuzzleTab.serializer()))
                3 -> blanks = dec.decodeSerializableElement(descriptor, 3, ListSerializer(PuzzleBlank.serializer()))
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unknown index $index")
            }
        }

        dec.endStructure(descriptor)

        // Initialize PuzzlePiece with default or desired values for non-serialized properties
        return PuzzlePiece(
            text = text,
            grammaticalRole = grammaticalRole,
            pos = Vector2(0f, 0f),
            depth = 0
        ).apply {
            this.tabs.addAll(tabs)
            this.blanks.addAll(blanks)
            this.tabs.forEach { it.owner = this }
            this.blanks.forEach { it.owner = this }
        }
    }
}

@Serializer(forClass = PuzzleTab::class)
object PuzzleTabSerializer : KSerializer<PuzzleTab> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PuzzleTab") {
        element<Side>("side")
        element<GrammaticalRole>("grammaticalRole")
        element<String>("text")
    }

    override fun serialize(encoder: Encoder, value: PuzzleTab) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, Side.serializer(), value.side)
        composite.encodeSerializableElement(descriptor, 1, GrammaticalRole.serializer(), value.grammaticalRole)
        composite.encodeStringElement(descriptor, 2, value.text)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): PuzzleTab {
        val decStructure = decoder.beginStructure(descriptor)
        var side: Side = Side.BOTTOM // Replace with an appropriate default
        var grammaticalRole: GrammaticalRole = GrammaticalRole.SUBJECT // Replace with an appropriate default
        var text: String = ""

        loop@ while (true) {
            when (val index = decStructure.decodeElementIndex(descriptor)) {
                0 -> side = decStructure.decodeSerializableElement(descriptor, 0, Side.serializer())
                1 -> grammaticalRole = decStructure.decodeSerializableElement(descriptor, 1, GrammaticalRole.serializer())
                2 -> text = decStructure.decodeStringElement(descriptor, 2)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unknown index $index in PuzzleTab deserialization")
            }
        }

        decStructure.endStructure(descriptor)

        // Create PuzzleTab without owner; it will be set later
        return PuzzleTab(
            owner = null, // Will be assigned by PuzzlePieceSerializer
            side = side,
            grammaticalRole = grammaticalRole,
            text = text
        )
    }
}

@Serializer(forClass = PuzzleBlank::class)
object PuzzleBlankSerializer : KSerializer<PuzzleBlank> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PuzzleBlank") {
        element<Side>("side")
    }

    override fun serialize(encoder: Encoder, value: PuzzleBlank) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, Side.serializer(), value.side)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): PuzzleBlank {
        val decStructure = decoder.beginStructure(descriptor)
        var side: Side = Side.BOTTOM // Replace with an appropriate default

        loop@ while (true) {
            when (val index = decStructure.decodeElementIndex(descriptor)) {
                0 -> side = decStructure.decodeSerializableElement(descriptor, 0, Side.serializer())
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unknown index $index in PuzzleBlank deserialization")
            }
        }

        decStructure.endStructure(descriptor)

        // Create PuzzleBlank without owner; it will be set later
        return PuzzleBlank(
            owner = null, // Will be assigned by PuzzlePieceSerializer
            side = side
        )
    }
}


@Serializer(forClass = SolutionConfiguration::class)
object SolutionConfigurationSerializer : KSerializer<SolutionConfiguration> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("solutionConfiguration") {
        element<Boolean>("checkTabText")
        element("centerPiece", PuzzlePieceSerializer.descriptor)
        element<List<ConnectedPuzzleInfo>>("puzzlesConnected")
    }

    override fun serialize(encoder: Encoder, value: SolutionConfiguration) {
        // Implement serialization if needed
        throw NotImplementedError("Serialization for SolutionConfiguration is not implemented yet")
    }

    override fun deserialize(decoder: Decoder): SolutionConfiguration {
        val decStructure = decoder.beginStructure(descriptor)
        var checkTabText = false
        var centerPiece: PuzzlePiece? = null
        var connectedPuzzleInfos: List<ConnectedPuzzleInfo> = emptyList()

        loop@ while (true) {
            when (val index = decStructure.decodeElementIndex(descriptor)) {
                0 -> checkTabText = decStructure.decodeBooleanElement(descriptor, 0)
                1 -> centerPiece = decStructure.decodeSerializableElement(descriptor, 1, PuzzlePieceSerializer)
                2 -> connectedPuzzleInfos = decStructure.decodeSerializableElement(descriptor, 2, ListSerializer(ConnectedPuzzleInfo.serializer()))
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unknown index $index in SolutionConfiguration deserialization")
            }
        }
        decStructure.endStructure(descriptor)

        requireNotNull(centerPiece) { "centerPiece cannot be null" }

        val connections = connectedPuzzleInfos.map { info ->
            val connectedPiece = PuzzlePiece(info.text, info.grammaticalRole)
            val viaTab = PuzzleTab(
                side = when (info.grammaticalRole) { // Infer side based on grammatical role - adjust as needed
                    GrammaticalRole.SUBJECT -> Side.TOP
                    GrammaticalRole.OBJECT -> Side.BOTTOM
                    GrammaticalRole.ADVERBIAL -> Side.LEFT
                    else -> Side.RIGHT
                },
                grammaticalRole = info.grammaticalRole,
                text = info.via.text
            )
            Connection(
                puzzlesConnected = setOf(centerPiece, connectedPiece),
                via = viaTab,
                roleOfConnection = info.grammaticalRole // Or potentially centerPiece.grammaticalRole
            )
        }.toSet()

        return SolutionConfiguration(centerPiece, connections, checkTabText)
    }
}

@kotlinx.serialization.Serializable
private data class ConnectedPuzzleInfo(
    val text: String,
    val grammaticalRole: GrammaticalRole,
    val via: ViaInfo
)

@Serializable
private data class ViaInfo(
    val text: String
)
