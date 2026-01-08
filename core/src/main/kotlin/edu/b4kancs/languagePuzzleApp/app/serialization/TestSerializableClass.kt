package edu.b4kancs.languagePuzzleApp.app.serialization

import kotlinx.serialization.Serializable

@Serializable
data class TestSerializableClass (
    val text: String,
    val num: Int
)
