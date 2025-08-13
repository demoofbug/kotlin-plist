package com.getiox.plist

import com.getiox.plist.internal.decodeBinary
import com.getiox.plist.internal.decodeXml
import com.getiox.plist.internal.encodeBinary
import com.getiox.plist.internal.encodeXml
import kotlin.jvm.JvmStatic

enum class PListFormat {
    XML,
    BINARY,
}

object PList {
    @JvmStatic
    fun encode(
        data: PListValue,
        format: PListFormat = PListFormat.XML,
    ): ByteArray =
        when (format) {
            PListFormat.XML -> encodeXml(data)
            PListFormat.BINARY -> encodeBinary(data)
        }

    @JvmStatic
    fun decode(data: ByteArray): PListValue =
        detectFormat(data)?.let { format ->
            when (format) {
                PListFormat.XML -> decodeXml(data)
                PListFormat.BINARY -> decodeBinary(data)
            }
        } ?: error("Unknown PList format")

    @JvmStatic
    private fun detectFormat(data: ByteArray): PListFormat? {
        if (data.isEmpty()) return null

        // Check XML plist header ("<?xml")
        if (data.size >= 5 &&
            data[0] == '<'.code.toByte() &&
            data[1] == '?'.code.toByte() &&
            data[2] == 'x'.code.toByte() &&
            data[3] == 'm'.code.toByte() &&
            data[4] == 'l'.code.toByte()
        ) {
            return PListFormat.XML
        }

        // Check binary plist header ("bplist")
        if (data.size >= 8 &&
            data[0] == 'b'.code.toByte() &&
            data[1] == 'p'.code.toByte() &&
            data[2] == 'l'.code.toByte() &&
            data[3] == 'i'.code.toByte() &&
            data[4] == 's'.code.toByte() &&
            data[5] == 't'.code.toByte() &&
            data[6] == '0'.code.toByte() &&
            data[7] == '0'.code.toByte()
        ) {
            return PListFormat.BINARY
        }

        return null
    }
}
