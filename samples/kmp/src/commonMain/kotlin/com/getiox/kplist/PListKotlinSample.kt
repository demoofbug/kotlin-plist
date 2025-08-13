package com.getiox.kplist

import kotlinx.datetime.Clock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

fun main() {
    val root = buildSamplePList()

    writeAndVerifyPList(root, "plist-bin.plist", PListFormat.BINARY)
    writeAndVerifyPList(root, "plist-xml.plist", PListFormat.XML)
}

fun buildSamplePList(): PListValue = mapOf(
    "stringKey" to "stringValue".plistValue,
    "intKey" to 123.plistValue,
    "boolKey" to true.plistValue,
    "realKey" to 123.456.plistValue,
    "dateKey" to Clock.System.now().plistValue,
    "dataKey" to byteArrayOf(1, 2, 3).plistValue,
    "arrayKey" to listOf(1.plistValue, 2.plistValue, 3.plistValue).plistValue,
    "dictKey" to mapOf(
        "stringKey" to "stringValue".plistValue,
        "intKey" to 123.plistValue
    ).plistValue
).plistValue

fun writeAndVerifyPList(root: PListValue, fileName: String, format: PListFormat) {
    val path = Path("build/generated/$fileName")

    // ✅ create parent directory if not exists
    if (!SystemFileSystem.exists(path.parent!!)) {
        SystemFileSystem.createDirectories(path.parent!!, true)
    }

    // ✅ encode and write to file
    val encoded = PList.encode(root, format)
    SystemFileSystem.sink(path).buffered().use {
        it.write(encoded)
    }
    println("✅ Wrote $fileName")

    // ✅ read from file and decode
    val decodedBytes = SystemFileSystem.source(path).buffered().use {
        it.readByteArray()
    }
    val decoded = PList.decode(decodedBytes)

    println("🔍 Decoded from $fileName:\n$decoded\n")
}
