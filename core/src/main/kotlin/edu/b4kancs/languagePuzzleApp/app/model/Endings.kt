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
            val tabTextPartials = tabText
                .replace("\n", "")
                .split("/")
                .map { it.removePrefix("-") }
                .map { if (it.contains("al") || it.contains("el")) it.drop(1) else it } // helps with "-val/-vel" and its variants

            for (suffix in predefinedSuffixes) {
                val suffixPartials = suffix.text
                    .replace("\n", "")
                    .replace("val", "al")
                    .replace("vel", "el")
                    .split("/")
                    .map { it.removePrefix("-") }
                    .map { if (it.contains("al") || it.contains("el")) it.drop(1) else it }

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

// Névutók
@Serializable
data class Postposition(
    val text: String,
    @Transient val grammaticalRole: GrammaticalRole = GrammaticalRole.ADVERBIAL
) {
    companion object {
        val predefinedPostpositions = listOf(
            Postposition("miatt"),
            Postposition("elől"),
            Postposition("után"),
            Postposition("mellett"),
            Postposition("közül"),
            Postposition("felől"),
            Postposition("ellen"),
            Postposition("felé")
        )

        fun splitPredefinedPostposition(postposition: Postposition): Postposition {
            return when (postposition.text) {
                "miatt" -> Postposition("mi-\natt")
                "mellett" -> Postposition("mel-\nlett")
                "közül" -> Postposition("kö-\nzül")
                "felől" -> Postposition("fe-\nlől")
                else -> postposition
            }
        }
    }
}

// Jelentéscímkék
@Serializable
data class IndefinitePronoun(
    val text: String,
    @Transient val grammaticalRole: GrammaticalRole = GrammaticalRole.ADVERBIAL
) {
    companion object {
        val predefinedIndPronouns = listOf(
            IndefinitePronoun("valahová"),
            IndefinitePronoun("valamerre"),
            IndefinitePronoun("valahonnan"),
            IndefinitePronoun("valahol"),
            IndefinitePronoun("valahogyan"),
            IndefinitePronoun("valamennyibe")
        )

        fun shortenPredefinedIndPronoun(pronoun: IndefinitePronoun): IndefinitePronoun {
            return when(pronoun.text) {
                "valamennyibe" -> IndefinitePronoun("vmeny-\nnyibe")
                else -> IndefinitePronoun(pronoun.text.replace("vala", "v"))
            }
        }

        fun splitShortenedIndPronoun(pronoun: IndefinitePronoun): IndefinitePronoun {
            return when (pronoun.text) {
                "valahová" -> IndefinitePronoun("vho-\nvá")
                "valamerre" -> IndefinitePronoun("vmer-\nre")
                "valahonnan" -> IndefinitePronoun("vhon-\nnan")
                "valahol" -> IndefinitePronoun("vala-\nhol")
                "valahogyan" -> IndefinitePronoun("vho-\ngyan")
                "valamennyibe" -> IndefinitePronoun("vmeny-\nnyibe")
                else -> pronoun
            }
        }
    }
}



