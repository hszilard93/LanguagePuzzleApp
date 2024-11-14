package edu.b4kancs.languagePuzzleApp.app.serialization

import com.badlogic.gdx.utils.ObjectSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import ktx.collections.GdxSet
import ktx.collections.toGdxSet

//val module = SerializersModule {
//    // Register GdxSetSerializer for ObjectSet<TestSerializableClass>
//    contextual (
//        ObjectSet::class,
//        GdxSetSerializer.serializer<TestSerializableClass>()
//    )
//}

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
