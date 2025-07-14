package com.getiox.kplist

import com.getiox.kplist.internal.decodeBinary
import com.getiox.kplist.internal.encodeBinary
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class PListBinaryCodecTest {
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
        val binaryData = encodeBinary(original)
        val decoded = decodeBinary(binaryData).array
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
        val binaryData = encodeBinary(original)
        val decoded = decodeBinary(binaryData).dict

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
