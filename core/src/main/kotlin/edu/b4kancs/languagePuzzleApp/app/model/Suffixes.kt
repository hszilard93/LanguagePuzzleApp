package edu.b4kancs.languagePuzzleApp.app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Suffix(
    val text: String,
    @Transient val grammaticalRole: GrammaticalRole = GrammaticalRole.ADVERBIAL
) {
    companion object {
        val predefinedSuffixes = listOf(
            Suffix(""),
            Suffix("-t/\n-at/\n-ot/\n-öt", GrammaticalRole.OBJECT),
            Suffix("-nak/\n-nek"),
            Suffix("-val/\n-vel"),
            Suffix("-tól/\n-től"),
            Suffix("-ban/\n-ben"),
            Suffix("-ba/\n-be"),
            Suffix("-ra/\n-re"),
            Suffix("-hoz/\n-hez/\n-höz"),
            Suffix("-ból/\n-ből"),
            Suffix("-ról/\n-ről"),
            Suffix("-n/\n-on/\n-en/\n-ön"),
            Suffix("-vá/\n-vé"),
            Suffix("-ért"),
            Suffix("-ig"),
        )

        fun identifySuffixFromTabText(tabText: String): Suffix? {
            val tabTextPartials = tabText.replace("\n", "").split("/").map { it.removePrefix("-") }

            for (suffix in predefinedSuffixes) {
                val suffixPartials = suffix.text.replace("\n", "").split("/").map { it.removePrefix("-") }
                for (sp in suffixPartials) {
                    if (tabTextPartials.any { it == sp }) {
                        return suffix
                    }
                }
            }
            return null
        }
    }
}
