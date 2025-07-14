package com.getiox.kplist

import kotlinx.datetime.Instant
import kotlin.jvm.JvmSynthetic

sealed class PListValue

data class PListDict(
    private val value: Map<String, PListValue>,
) : PListValue(), Map<String, PListValue> by value {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PListDict) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String {
        return value.entries.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
            transform = { (k, v) ->
                buildString {
                    append(k)
                    append(':')
                    append(v)
                }
            },
        )
    }
}

data class PListArray(
    private val value: List<PListValue>,
) : PListValue(), List<PListValue> by value {
    override fun equals(other: Any?): Boolean = value == other

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.joinToString(prefix = "[", postfix = "]", separator = ",")
}

data class PListString(val value: String) : PListValue() {
    override fun toString(): String = value
}

data class PListInt(val value: Long) : PListValue() {
    override fun toString(): String = value.toString()
}

data class PListReal(val value: Double) : PListValue() {
    override fun toString(): String = value.toString()
}

data class PListBool(val value: Boolean) : PListValue() {
    override fun toString(): String = value.toString()
}

data class PListDate(val value: Instant) : PListValue() {
    constructor(epochMillis: Long) : this(Instant.fromEpochMilliseconds(epochMillis))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PListDate) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}

data class PListData(val value: ByteArray) : PListValue() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PListData) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String {
        val preview = value.take(8).joinToString(", ") { it.toUByte().toString() }
        val suffix = if (value.size > 8) ", ..." else ""
        return "[$preview$suffix]"
    }
}

object PListNull : PListValue() {
    override fun toString(): String = "null"
}

@get:JvmSynthetic
val PListValue.isDict: Boolean get() = this is PListDict

@get:JvmSynthetic
val PListValue.isArray: Boolean get() = this is PListArray

@get:JvmSynthetic
val PListValue.isString: Boolean get() = this is PListString

@get:JvmSynthetic
val PListValue.isInt: Boolean get() = this is PListInt

@get:JvmSynthetic
val PListValue.isReal: Boolean get() = this is PListReal

@get:JvmSynthetic
val PListValue.isBool: Boolean get() = this is PListBool

@get:JvmSynthetic
val PListValue.isDate: Boolean get() = this is PListDate

@get:JvmSynthetic
val PListValue.isData: Boolean get() = this is PListData

@get:JvmSynthetic
val PListValue.isNull: Boolean get() = this === PListNull

@get:JvmSynthetic
val PListValue.dict: PListDict get() = requireValue()

@get:JvmSynthetic
val PListValue.array: PListArray get() = requireValue()

@get:JvmSynthetic
val PListValue.string: String get() = requireValue<PListString>().value

@get:JvmSynthetic
val PListValue.int: Long get() = requireValue<PListInt>().value

@get:JvmSynthetic
val PListValue.real: Double get() = requireValue<PListReal>().value

@get:JvmSynthetic
val PListValue.bool: Boolean get() = requireValue<PListBool>().value

@get:JvmSynthetic
val PListValue.date: Instant get() = requireValue<PListDate>().value

@get:JvmSynthetic
val PListValue.data: ByteArray get() = requireValue<PListData>().value

@get:JvmSynthetic
val String.plistValue: PListString get() = PListString(this)

@get:JvmSynthetic
val Long.plistValue: PListInt get() = PListInt(this)

@get:JvmSynthetic
val Int.plistValue: PListInt get() = PListInt(this.toLong())

@get:JvmSynthetic
val Double.plistValue: PListReal get() = PListReal(this)

@get:JvmSynthetic
val Boolean.plistValue: PListBool get() = PListBool(this)

@get:JvmSynthetic
val ByteArray.plistValue: PListData get() = PListData(this)

@get:JvmSynthetic
val Instant.plistValue: PListDate get() = PListDate(this)

@get:JvmSynthetic
val Map<String, PListValue>.plistValue: PListDict get() = PListDict(this)

@get:JvmSynthetic
val List<PListValue>.plistValue: PListArray get() = PListArray(this)

private inline fun <reified T : PListValue> PListValue.requireValue(): T =
    this as? T ?: error("PListValue is not a ${T::class.simpleName}, actual type: ${this::class}")
