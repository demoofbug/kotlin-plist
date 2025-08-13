package com.getiox.plist.internal

import com.getiox.plist.*
import kotlinx.datetime.Instant
import kotlin.jvm.JvmSynthetic

@JvmSynthetic
internal fun encodeBinary(data: PListValue): ByteArray = BinaryPListEncoder.encode(data)

@JvmSynthetic
internal fun decodeBinary(data: ByteArray): PListValue = BinaryPListDecoder.decode(data)

private object BinaryPListDecoder {
    fun decode(data: ByteArray): PListValue {
        require(data.decodeToString(0, 8) == "bplist00")

        val trailerOffset = data.size - 32
        val trailer =
            BinaryPlistTrailer {
                offsetSize = data.toUByteAt(trailerOffset + 6)
                objectRefSize = data.toUByteAt(trailerOffset + 7)
                numObjects = data.toLongBE(trailerOffset + 8).toInt()
                topObject = data.toLongBE(trailerOffset + 16).toInt()
                offsetTableOffset = data.toLongBE(trailerOffset + 24)
                offsetTable =
                    (0 until numObjects).map {
                        val start = offsetTableOffset.toInt() + it * offsetSize
                        data.toIntBE(start, offsetSize)
                    }
            }

        return readObject(data, trailer, trailer.topObject)
    }

    private class BinaryPlistTrailer(block: Builder.() -> Unit) {
        val offsetSize: Int
        val objectRefSize: Int
        val numObjects: Int
        val topObject: Int
        val offsetTableOffset: Long
        val offsetTable: List<Int>

        init {
            Builder().apply(block).also {
                offsetSize = it.offsetSize
                objectRefSize = it.objectRefSize
                numObjects = it.numObjects
                topObject = it.topObject
                offsetTableOffset = it.offsetTableOffset
                offsetTable = it.offsetTable
            }
        }

        class Builder {
            var offsetSize: Int = 0
            var objectRefSize: Int = 0
            var numObjects: Int = 0
            var topObject: Int = 0
            var offsetTableOffset: Long = 0
            var offsetTable: List<Int> = emptyList()
        }
    }

    private fun readObject(
        data: ByteArray,
        state: BinaryPlistTrailer,
        objRef: Int,
    ): PListValue {
        val offset = state.offsetTable[objRef]
        val marker = data.toUByteAt(offset)
        val objType = marker shr 4
        val objInfo = marker and 0x0F

        return when (objType) {
            0x0 -> data.decodeSimple(marker, objInfo)
            0x1 -> data.decodeInt(offset, objInfo)
            0x2 -> data.decodeReal(offset, objInfo)
            0x3 -> data.decodeDate(offset)
            0x4 -> data.decodeData(offset, objInfo)
            0x5 -> data.decodeString(offset, objInfo, isUtf16 = false)
            0x6 -> data.decodeString(offset, objInfo, isUtf16 = true)
            0xA -> data.decodeArray(offset, objInfo, state)
            0xD -> data.decodeDict(offset, objInfo, state)
            else -> error("Unsupported object type: 0x${objType.toString(16)}")
        }
    }

    private fun ByteArray.decodeSimple(
        marker: Int,
        objInfo: Int,
    ): PListValue =
        when (objInfo) {
            0x0 -> PListNull
            0x8 -> PListBool(false)
            0x9 -> PListBool(true)
            else -> error("Unsupported simple type: 0x${marker.toString(16)}")
        }

    private fun ByteArray.decodeInt(
        offset: Int,
        objInfo: Int,
    ): PListInt {
        val length = 1 shl objInfo
        val value = this.toLongBE(offset + 1, length)
        return PListInt(value)
    }

    private fun ByteArray.decodeReal(
        offset: Int,
        objInfo: Int,
    ): PListReal {
        val size = 1 shl objInfo
        val bits = this.toLongBE(offset + 1, size)
        val value =
            when (size) {
                4 -> Float.fromBits(bits.toInt()).toDouble()
                8 -> Double.fromBits(bits)
                else -> error("Unsupported float size: $size")
            }
        return PListReal(value)
    }

    private fun ByteArray.decodeDate(offset: Int): PListDate {
        val secondsSince2001 = Double.fromBits(this.toLongBE(offset + 1, 8))
        val value = Instant.fromEpochSeconds((secondsSince2001 + 978307200).toLong())
        return PListDate(value)
    }

    private fun ByteArray.decodeData(
        offset: Int,
        objInfo: Int,
    ): PListData {
        val (length, delta) = this.decodeLengthWithHeaderSize(offset, objInfo)
        val value = this.copyOfRange(offset + delta, offset + delta + length)
        return PListData(value)
    }

    private fun ByteArray.decodeString(
        offset: Int,
        objInfo: Int,
        isUtf16: Boolean,
    ): PListString {
        val (length, delta) = decodeLengthWithHeaderSize(offset, objInfo)
        val value =
            if (isUtf16) {
                decodeAsUtf16BE(offset + delta, offset + delta + length * 2)
            } else {
                decodeToString(offset + delta, offset + delta + length)
            }
        return PListString(value)
    }

    private fun ByteArray.decodeArray(
        offset: Int,
        objInfo: Int,
        state: BinaryPlistTrailer,
    ): PListArray {
        val (count, delta) = this.decodeLengthWithHeaderSize(offset, objInfo)
        val start = offset + delta
        val refs =
            (0 until count).map {
                val i = start + it * state.objectRefSize
                this.toIntBE(i, state.objectRefSize)
            }
        val value = refs.map { readObject(this, state, it) }
        return PListArray(value)
    }

    private fun ByteArray.decodeDict(
        offset: Int,
        objInfo: Int,
        state: BinaryPlistTrailer,
    ): PListDict {
        val (count, delta) = this.decodeLengthWithHeaderSize(offset, objInfo)
        val start = offset + delta
        val keys =
            (0 until count).map {
                val i = start + it * state.objectRefSize
                this.toIntBE(i, state.objectRefSize)
            }
        val values =
            (0 until count).map {
                val i = start + count * state.objectRefSize + it * state.objectRefSize
                this.toIntBE(i, state.objectRefSize)
            }
        val value = LinkedHashMap<String, PListValue>()
        for (i in keys.indices) {
            val key =
                readObject(this, state, keys[i]) as? PListString
                    ?: error("Dict key must be a string")
            value[key.value] = readObject(this, state, values[i])
        }
        return PListDict(value)
    }

    private fun ByteArray.decodeLengthWithHeaderSize(
        offset: Int,
        objInfo: Int,
    ): Pair<Int, Int> {
        return if (objInfo == 0xF) {
            val marker = this.toUByteAt(offset + 1)
            require(marker shr 4 == 0x1)
            val size = 1 shl (marker and 0x0F)
            val value = this.toIntBE(offset + 2, size)
            value to 2 + size
        } else {
            objInfo to 1
        }
    }

    private fun ByteArray.decodeAsUtf16BE(
        start: Int,
        end: Int,
    ): String {
        val chars = CharArray((end - start) / 2)
        for (i in chars.indices) {
            val high = this[start + i * 2].toInt() and 0xFF
            val low = this[start + i * 2 + 1].toInt() and 0xFF
            chars[i] = ((high shl 8) or low).toChar()
        }
        return chars.concatToString()
    }

    private fun ByteArray.toIntBE(
        start: Int,
        size: Int,
    ): Int = (0 until size).fold(0) { acc, i -> (acc shl 8) or (this[start + i].toInt() and 0xFF) }

    private fun ByteArray.toLongBE(
        start: Int,
        size: Int = 8,
    ): Long = (0 until size).fold(0L) { acc, i -> (acc shl 8) or (this[start + i].toLong() and 0xFF) }

    private fun ByteArray.toUByteAt(index: Int): Int = this[index].toInt() and 0xFF
}

private object BinaryPListEncoder {
    private val HEADER = "bplist00".encodeToByteArray()

    private class ObjectTable {
        private val objects = mutableListOf<PListValue>()
        private val objectMap = mutableMapOf<PListValue, Int>()

        fun addObject(obj: PListValue): Int =
            objectMap.getOrPut(obj) {
                val index = objects.size
                objects.add(obj)
                index
            }

        fun getObjectRef(obj: PListValue): Int = objectMap[obj] ?: error("Object not found in table: $obj")

        fun getObjects(): List<PListValue> = objects
    }

    fun encode(root: PListValue): ByteArray {
        val objectTable = ObjectTable().also { collectObjects(root, it) }
        val objects = objectTable.getObjects()
        val topRef = objectTable.getObjectRef(root)
        val objectRefSize = calculateMinimalIntSize(objects.size.toLong())

        val (encodedObjects, offsets) = encodeObjects(objects, objectTable, objectRefSize)
        val offsetTableOffset = HEADER.size + encodedObjects.sumOf { it.size }

        val offsetSize = calculateMinimalIntSize(offsetTableOffset.toLong())

        return buildBinaryPlist(
            objectData = encodedObjects,
            offsets = offsets,
            offsetSize = offsetSize,
            objectRefSize = objectRefSize,
            topObjectRef = topRef,
            numObjects = objects.size,
            offsetTableOffset = offsetTableOffset.toLong(),
        )
    }

    private fun collectObjects(
        value: PListValue,
        table: ObjectTable,
    ) {
        if (shouldUnique(value) && value in table.getObjects()) return
        table.addObject(value)
        when (value) {
            is PListDict -> {
                value.keys.forEach { collectObjects(PListString(it), table) }
                value.values.forEach { collectObjects(it, table) }
            }

            is PListArray -> value.forEach { collectObjects(it, table) }
            else -> {}
        }
    }

    private fun shouldUnique(value: PListValue): Boolean =
        when (value) {
            is PListString, is PListInt, is PListReal, is PListDate, is PListData -> true
            else -> false
        }

    private fun encodeObjects(
        objects: List<PListValue>,
        table: ObjectTable,
        refSize: Int,
    ): Pair<List<ByteArray>, List<Long>> {
        val encoded = mutableListOf<ByteArray>()
        val offsets = mutableListOf<Long>()
        var offset = HEADER.size.toLong()

        for (obj in objects) {
            offsets += offset
            val data = obj.encode(table, refSize)
            encoded += data
            offset += data.size
        }
        return encoded to offsets
    }

    private fun PListNull.encode(): ByteArray = byteArrayOf(0x00)

    private fun PListBool.encode(): ByteArray = byteArrayOf(if (value) 0x09 else 0x08)

    private fun PListInt.encode(): ByteArray {
        val size: Int
        val sizeMarker: Int

        if (value < 0) {
            size = 8
            sizeMarker = 3
        } else {
            size = calculateMinimalIntSize(value)
            sizeMarker =
                when (size) {
                    1 -> 0
                    2 -> 1
                    4 -> 2
                    8 -> 3
                    else -> error("Invalid int size")
                }
        }
        val marker = (0x1 shl 4) or sizeMarker
        return byteArrayOf(marker.toByte()) + value.toBigEndianBytes(size)
    }

    private fun PListReal.encode(): ByteArray {
        val marker = (0x2 shl 4) or 3 // 8-byte double
        val data = value.toBits().toBigEndianBytes(8)
        return byteArrayOf(marker.toByte()) + data
    }

    private fun PListDate.encode(): ByteArray {
        val secondsSince2001 = value.epochSeconds - 978307200.0
        val data = secondsSince2001.toBits().toBigEndianBytes(8)
        return byteArrayOf(0x33.toByte()) + data
    }

    private fun PListData.encode(): ByteArray {
        val lengthPrefix = encodeLengthAndMarker(value.size, 0x4)
        return lengthPrefix + value
    }

    private fun PListString.encode(): ByteArray {
        val utf8 = value.encodeToByteArray()
        val lengthPrefix = encodeLengthAndMarker(utf8.size, 0x5)
        return lengthPrefix + utf8
    }

    private fun PListArray.encode(
        table: ObjectTable,
        refSize: Int,
    ): ByteArray {
        val lengthPrefix = encodeLengthAndMarker(size, 0xA)
        val refs =
            flatMap {
                val ref = table.getObjectRef(it)
                ref.toRefBytes(refSize).asList()
            }
        return lengthPrefix + refs.toByteArray()
    }

    private fun PListDict.encode(
        table: ObjectTable,
        refSize: Int,
    ): ByteArray {
        val lengthPrefix = encodeLengthAndMarker(size, 0xD)

        val keyRefs =
            keys.flatMap { key ->
                val ref = table.getObjectRef(PListString(key))
                ref.toRefBytes(refSize).asList()
            }

        val valueRefs =
            values.flatMap { value ->
                val ref = table.getObjectRef(value)
                ref.toRefBytes(refSize).asList()
            }

        return lengthPrefix + keyRefs.toByteArray() + valueRefs.toByteArray()
    }

    private fun PListValue.encode(
        table: ObjectTable,
        refSize: Int,
    ): ByteArray =
        when (this) {
            is PListNull -> this.encode()
            is PListBool -> this.encode()
            is PListInt -> this.encode()
            is PListReal -> this.encode()
            is PListDate -> this.encode()
            is PListData -> this.encode()
            is PListString -> this.encode()
            is PListArray -> this.encode(table, refSize)
            is PListDict -> this.encode(table, refSize)
        }

    private fun encodeLengthAndMarker(
        length: Int,
        objType: Int,
    ): ByteArray {
        return if (length < 0xF) {
            val marker = ((objType shl 4) or length).toByte()
            byteArrayOf(marker)
        } else {
            val marker = ((objType shl 4) or 0xF).toByte()
            val size = calculateMinimalIntSize(length.toLong())
            val sizeMarker = (
                (0x1 shl 4) or when (size) {
                    1 -> 0
                    2 -> 1
                    4 -> 2
                    8 -> 3
                    else -> error("Invalid length size")
                }
                ).toByte()
            byteArrayOf(marker, sizeMarker) + length.toRefBytes(size)
        }
    }

    private fun buildBinaryPlist(
        objectData: List<ByteArray>,
        offsets: List<Long>,
        offsetSize: Int,
        objectRefSize: Int,
        topObjectRef: Int,
        numObjects: Int,
        offsetTableOffset: Long,
    ): ByteArray {
        val result = mutableListOf<Byte>()

        result += HEADER.toList()
        objectData.forEach { result += it.toList() }
        offsets.forEach { result += it.toBigEndianBytes(offsetSize).toList() }

        val trailer =
            ByteArray(32).apply {
                this[6] = offsetSize.toByte()
                this[7] = objectRefSize.toByte()
                numObjects.toRefBytes(8).copyInto(this, 8)
                topObjectRef.toRefBytes(8).copyInto(this, 16)
                offsetTableOffset.toBigEndianBytes(8).copyInto(this, 24)
            }

        result += trailer.toList()
        return result.toByteArray()
    }

    private fun calculateMinimalIntSize(value: Long): Int =
        when {
            value <= 0xFF -> 1
            value <= 0xFFFF -> 2
            value <= 0xFFFFFFFF -> 4
            else -> 8
        }

    private fun Long.toBigEndianBytes(size: Int): ByteArray =
        ByteArray(size) { i -> ((this shr (8 * (size - i - 1))) and 0xFF).toByte() }

    private fun Int.toRefBytes(refSize: Int) = this.toLong().toBigEndianBytes(refSize)
}
