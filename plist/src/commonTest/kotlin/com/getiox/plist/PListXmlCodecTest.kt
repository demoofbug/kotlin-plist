package com.getiox.plist

import com.getiox.plist.internal.decodeXml
import com.getiox.plist.internal.encodeXml
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PListXmlCodecTest {
    @Test
    fun testEncodeDict() {
        val original =
            PListDict(
                mapOf(
                    "string" to PListString("value"),
                    "int" to PListInt(100),
                    "real" to PListReal(99.99),
                    "boolTrue" to PListBool(true),
                    "boolFalse" to PListBool(false),
                    "date" to PListDate(Instant.parse("2025-01-01T12:00:00Z")),
                    "data" to PListData("BinaryData".encodeToByteArray()),
                    "array" to PListArray(listOf(PListInt(1), PListInt(2))),
                    "dict" to PListDict(mapOf("nestedKey" to PListString("nestedValue"))),
                ),
            )
        val xml = encodeXml(original).decodeToString()
        assertTrue(xml.contains("<dict>"))
        assertTrue(xml.contains("<key>string</key>"))
        assertTrue(xml.contains("<string>value</string>"))
        assertTrue(xml.contains("<integer>100</integer>"))
        assertTrue(xml.contains("<real>99.99</real>"))
        assertTrue(xml.contains("<true />"))
        assertTrue(xml.contains("<false />"))
        assertTrue(xml.contains("<date>"))
        assertTrue(xml.contains("<data>"))
        assertTrue(xml.contains("<array>"))
        assertTrue(xml.contains("<dict>"))
    }

    @Test
    fun testDecodeDict() {
        val xml =
            """
        <plist version="1.0">
          <dict>
            <key>string</key><string>value</string>
            <key>int</key><integer>100</integer>
            <key>real</key><real>99.99</real>
            <key>boolTrue</key><true/>
            <key>boolFalse</key><false/>
            <key>date</key><date>2025-01-01T12:00:00Z</date>
            <key>data</key><data>QmluYXJ5RGF0YQ==</data>
            <key>array</key>
              <array><integer>1</integer><integer>2</integer></array>
            <key>dict</key>
              <dict><key>nestedKey</key><string>nestedValue</string></dict>
          </dict>
        </plist>
    """.encodeToByteArray()

        val decoded = decodeXml(xml).dict
        assertEquals("value", decoded["string"]?.string)
        assertEquals(100, decoded["int"]?.int)
        assertEquals(99.99, decoded["real"]?.real)
        assertEquals(true, decoded["boolTrue"]?.bool)
        assertEquals(false, decoded["boolFalse"]?.bool)
        assertEquals(Instant.parse("2025-01-01T12:00:00Z"), decoded["date"]?.date)
        assertEquals("BinaryData", decoded["data"]?.data?.decodeToString())
        assertEquals(1, decoded["array"]?.array[0]?.int)
        assertEquals("nestedValue", decoded["dict"]?.dict?.get("nestedKey")?.string)
    }

    @Test
    fun testEncodeArray() {
        val original =
            PListArray(
                listOf(
                    PListString("abc"),
                    PListInt(42),
                    PListReal(3.14),
                    PListBool(true),
                    PListDate(Instant.parse("2025-01-01T00:00:00Z")),
                    PListData("Hello World".encodeToByteArray()),
                    PListArray(listOf(PListBool(true), PListInt(1))),
                    PListDict(mapOf("key" to PListString("value"))),
                ),
            )
        val xml = encodeXml(original).decodeToString()
        assertTrue(xml.contains("<array>"))
        assertTrue(xml.contains("<string>abc</string>"))
        assertTrue(xml.contains("<integer>42</integer>"))
        assertTrue(xml.contains("<real>3.14</real>"))
        assertTrue(xml.contains("<true />"))
        assertTrue(xml.contains("<date>"))
        assertTrue(xml.contains("<data>"))
        assertTrue(xml.contains("<array>"))
        assertTrue(xml.contains("<dict>"))
    }

    @Test
    fun testDecodeArray() {
        val xml =
            """
        <plist version="1.0">
          <array>
            <string>abc</string>
            <integer>42</integer>
            <real>3.14</real>
            <true/>
            <date>2025-01-01T00:00:00Z</date>
            <data>SGVsbG8gV29ybGQ=</data>
            <array><true/><integer>1</integer></array>
            <dict><key>key</key><string>value</string></dict>
          </array>
        </plist>
    """.encodeToByteArray()

        val decoded = decodeXml(xml).array
        assertEquals("abc", decoded[0].string)
        assertEquals(42, decoded[1].int)
        assertEquals(3.14, decoded[2].real)
        assertEquals(true, decoded[3].bool)
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), decoded[4].date)
        assertEquals("Hello World", decoded[5].data.decodeToString())
        assertEquals(true, decoded[6].array[0].bool)
        assertEquals(1, decoded[6].array[1].int)
        assertEquals("value", decoded[7].dict["key"]?.string)
    }

    @Test
    fun testRoundtripArray() {
        val original =
            PListArray(
                listOf(
                    PListString("abc"),
                    PListInt(42),
                    PListReal(3.14),
                    PListBool(true),
                    PListDate(Instant.parse("2025-01-01T00:00:00Z")),
                    PListData("Hello World".encodeToByteArray()),
                    PListArray(listOf(PListBool(true), PListInt(1))),
                    PListDict(mapOf("key" to PListString("value"))),
                ),
            )
        val xml = encodeXml(original)
        val decoded = decodeXml(xml).array
        assertEquals(original.size, decoded.size)
        assertEquals(original[0].string, decoded[0].string)
        assertEquals(original[1].int, decoded[1].int)
        assertEquals(original[2].real, decoded[2].real)
        assertEquals(original[3].bool, decoded[3].bool)
        assertEquals(original[4].date, decoded[4].date)
        assertEquals(original[5].data.decodeToString(), decoded[5].data.decodeToString())
        assertEquals(original[6].array.size, decoded[6].array.size)
        assertEquals(original[6].array[0].bool, decoded[6].array[0].bool)
        assertEquals(original[6].array[1].int, decoded[6].array[1].int)
        assertEquals(original[7].dict.size, decoded[7].dict.size)
        assertEquals(original[7].dict["key"]?.string, decoded[7].dict["key"]?.string)
    }

    @Test
    fun testRoundtripDict() {
        val original =
            PListDict(
                mapOf(
                    "string" to PListString("value"),
                    "int" to PListInt(100),
                    "real" to PListReal(99.99),
                    "boolTrue" to PListBool(true),
                    "boolFalse" to PListBool(false),
                    "date" to PListDate(Instant.parse("2025-01-01T12:00:00Z")),
                    "data" to PListData("BinaryData".encodeToByteArray()),
                    "array" to PListArray(listOf(PListInt(1), PListInt(2))),
                    "dict" to PListDict(mapOf("nestedKey" to PListString("nestedValue"))),
                ),
            )
        val xml = encodeXml(original)
        val decoded = decodeXml(xml).dict

        assertEquals(original.size, decoded.size)
        assertEquals(original["string"]?.string, decoded["string"]?.string)
        assertEquals(original["int"]?.int, decoded["int"]?.int)
        assertEquals(original["real"]?.real, decoded["real"]?.real)
        assertEquals(original["boolTrue"]?.bool, decoded["boolTrue"]?.bool)
        assertEquals(original["boolFalse"]?.bool, decoded["boolFalse"]?.bool)
        assertEquals(original["date"]?.date, decoded["date"]?.date)
        assertEquals(
            original["data"]?.data?.decodeToString(),
            decoded["data"]?.data?.decodeToString(),
        )

        // Nested array
        val origArray = original["array"]?.array
        val decodedArray = decoded["array"]?.array
        assertEquals(origArray?.size, decodedArray?.size)
        assertEquals(origArray?.get(0)?.int, decodedArray?.get(0)?.int)
        assertEquals(origArray?.get(1)?.int, decodedArray?.get(1)?.int)

        // Nested dict
        val origNested = original["dict"]?.dict
        val decodedNested = decoded["dict"]?.dict
        assertEquals(origNested?.size, decodedNested?.size)
        assertEquals(
            origNested?.get("nestedKey")?.string,
            decodedNested?.get("nestedKey")?.string,
        )
    }
}
