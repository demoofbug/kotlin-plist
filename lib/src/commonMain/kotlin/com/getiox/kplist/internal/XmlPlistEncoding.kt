package com.getiox.kplist.internal

import com.getiox.kplist.*
import kotlinx.datetime.Instant
import nl.adaptivity.xmlutil.EventType.*
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.newWriter
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.jvm.JvmSynthetic

@JvmSynthetic
internal fun encodeXml(value: PListValue): ByteArray = XmlPListEncoder.encode(value)

@JvmSynthetic
internal fun decodeXml(bytes: ByteArray): PListValue = XmlPListDecoder.decode(bytes)

private object XmlPListDecoder {
    private sealed class ParsingScope {
        class Dict(
            val map: LinkedHashMap<String, PListValue> = linkedMapOf(),
            var currentKey: String? = null,
        ) : ParsingScope()

        class Arr(
            val list: MutableList<PListValue> = mutableListOf(),
        ) : ParsingScope()
    }

    private fun handleStartElement(
        elementName: String,
        parsingStack: ArrayDeque<ParsingScope>,
        textContent: StringBuilder,
    ) {
        textContent.clear()
        when (elementName) {
            "dict" -> parsingStack.addLast(ParsingScope.Dict())
            "array" -> parsingStack.addLast(ParsingScope.Arr())
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun handleEndElement(
        elementName: String,
        parsingStack: ArrayDeque<ParsingScope>,
        textContent: StringBuilder,
        emitRoot: (PListValue) -> Unit,
    ) {
        val text = textContent.toString()
        val plistValue: PListValue? =
            when (elementName) {
                "key" -> {
                    val scope = parsingStack.lastOrNull() ?: error("No container for key")
                    if (scope !is ParsingScope.Dict) error("Key outside dict")
                    scope.currentKey = text
                    null
                }

                "string" -> PListString(text)
                "integer" -> PListInt(text.toLong())
                "real" -> PListReal(text.toDouble())
                "true" -> PListBool(true)
                "false" -> PListBool(false)
                "date" -> PListDate(Instant.parse(text))
                "data" -> PListData(Base64.decode(text))
                "array", "dict" -> {
                    val finishedScope = parsingStack.removeLast()
                    when (finishedScope) {
                        is ParsingScope.Arr -> PListArray(finishedScope.list)
                        is ParsingScope.Dict -> PListDict(finishedScope.map)
                    }
                }

                else -> null
            }

        plistValue?.let {
            if (parsingStack.isEmpty()) {
                emitRoot(it)
            } else {
                addPListValue(parsingStack, it)
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decode(bytes: ByteArray): PListValue {
        val reader = xmlStreaming.newReader(bytes.decodeToString())
        val parsingStack = ArrayDeque<ParsingScope>()
        val textContent = StringBuilder()
        var root: PListValue? = null

        with(reader) {
            forEach { event ->
                when (event) {
                    START_ELEMENT -> handleStartElement(localName, parsingStack, textContent)
                    TEXT, CDSECT -> textContent.append(text.trim())
                    END_ELEMENT -> handleEndElement(localName, parsingStack, textContent) { root = it }
                    else -> {}
                }
            }
            close()
        }

        return root ?: error("Missing <plist> root element")
    }

    private fun addPListValue(
        parsingStack: ArrayDeque<ParsingScope>,
        value: PListValue,
    ) {
        when (val scope = parsingStack.lastOrNull()) {
            is ParsingScope.Dict ->
                scope.apply {
                    val key = currentKey ?: error("Missing <key> before value $value")
                    map[key] = value
                    currentKey = null
                }

            is ParsingScope.Arr -> scope.list.add(value)
            else -> error("Unexpected parsing scope: cannot add value $value")
        }
    }
}

private object XmlPListEncoder {
    fun encode(value: PListValue): ByteArray =
        buildString {
            xmlStreaming.newWriter(this, true)
                .apply { indent = 4 }
                .apply { document { encodeValue(value) } }
                .apply { flush() }
                .apply { close() }
        }.encodeToByteArray()

    private fun XmlWriter.document(block: XmlWriter.() -> Unit) {
        startDocument(version = "1.0", encoding = "UTF-8")
        docdecl("""plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd"""")
        element("plist", attributes = mapOf("version" to "1.0")) {
            block()
        }
        endDocument()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun XmlWriter.encodeValue(plistValue: PListValue) =
        when (plistValue) {
            is PListDict -> dictElement(plistValue)
            is PListArray -> arrayElement(plistValue)
            is PListString -> element("string") { text(plistValue.string) }
            is PListInt -> element("integer") { text(plistValue.int.toString()) }
            is PListReal -> element("real") { text(plistValue.real.toString()) }
            is PListBool -> if (plistValue.bool) emptyElement("true") else emptyElement("false")
            is PListData -> element("data") { text(Base64.encode(plistValue.value)) }
            is PListDate -> element("date") { text(plistValue.value.toApplePlistString()) }
            PListNull -> Unit
        }

    private fun XmlWriter.arrayElement(array: PListArray) {
        element("array") {
            array.forEach { encodeValue(it) }
        }
    }

    private fun XmlWriter.dictElement(dict: PListDict) {
        element("dict") {
            dict.forEach { (key, value) ->
                element("key") { text(key) }
                encodeValue(value)
            }
        }
    }

    // DSL extension functions
    private inline fun XmlWriter.element(
        name: String,
        attributes: Map<String, String> = emptyMap(),
        block: XmlWriter.() -> Unit,
    ) {
        startTag(name)
        attributes.forEach { (name, value) -> attribute(name, value) }
        block()
        endTag(name)
    }

    private fun XmlWriter.emptyElement(name: String) {
        startTag(name)
        endTag(name)
    }

    // Base XML writer extensions
    private fun XmlWriter.startTag(name: String) = startTag(null, name, "")

    private fun XmlWriter.endTag(name: String) = endTag(null, name, "")

    private fun XmlWriter.attribute(
        name: String,
        value: String,
    ) = attribute(null, name, "", value)

    private fun Instant.toApplePlistString(): String =
        this.toString()
            .replace(Regex("\\.\\d+"), "") // Remove milliseconds
}
