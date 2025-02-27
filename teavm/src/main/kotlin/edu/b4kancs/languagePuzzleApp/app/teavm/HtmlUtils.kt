package edu.b4kancs.languagePuzzleApp.app.teavm

import org.teavm.jso.dom.html.HTMLDocument
import org.teavm.jso.dom.html.HTMLLinkElement

object HtmlUtils {

    fun setFavicon(url: String) {
        val link: HTMLLinkElement = HTMLDocument.current().createElement("link") as HTMLLinkElement
        link.rel = "icon"
        link.type = "image/png"
        link.href = url
        HTMLDocument.current().head.appendChild(link)
    }
}
