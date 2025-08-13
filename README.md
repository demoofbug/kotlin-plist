# kplist

![Maven Central](https://img.shields.io/maven-metadata/v.svg?label=maven-central&metadataUrl=https://repo1.maven.org/maven2/com/getiox/kplist/maven-metadata.xml&style=for-the-badge)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-green.svg?style=for-the-badge)
![License](https://img.shields.io/badge/License-Mit-green.svg?style=for-the-badge)

**kotlin-plist** is a Kotlin Multiplatform library for reading and writing Apple's plist (XML and binary) files.

---

## Table of Contents

* [Features](#features)
* [Platform Support](#platform-support)
* [Installation](#installation)

    * [Gradle](#gradle)
    * [Maven](#maven)
* [Usage Examples](#usage-examples)

    * [Kotlin Example](#kotlin-example)
    * [Java Example](#java-example)
* [Samples](#samples)
* [API Reference](#api-reference)

    * [Data Types](#data-types)
    * [Encoding and Decoding](#encoding-and-decoding)
    * [Extension Functions](#extension-functions)
* [License](#license)

---

## Features

- Read/write Apple plist files in XML and binary formats (bplist00)
- Type-safe, intuitive API for Kotlin and Java
- Multiplatform: JVM, Android, iOS, macOS, Linux, Windows, Node.js


---

## Platform Support

* Kotlin/JVM (Java 17+)
* Kotlin/Android (API 23+, ARM64 and x86_64)
* Kotlin/JS (Node.js)
* Kotlin/Native for iOS (ARM64, x64, Simulator)
* Kotlin/Native for macOS (ARM64, x64)
* Kotlin/Native for Windows (x64)
* Kotlin/Native for Linux (ARM64, x64)


---

## Installation

### Gradle

<details>
<summary>build.gradle.kts</summary>

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.getiox.plist:plist:0.0.2")
}
```

</details>

### Maven

<details>
<summary>pom.xml</summary>

```xml
<dependency>
    <groupId>com.getiox.plist</groupId>
    <artifactId>plist-jvm</artifactId>
    <version>0.0.2</version>
</dependency>
```

</details>

---

## Usage Examples

### Kotlin Example

<details>
<summary>Kotlin encode/decode</summary>

```kotlin
import com.getiox.kplist.*
import kotlinx.datetime.Clock

val plistValue = mapOf(
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

val binaryData = PList.encode(plistValue, PListFormat.BINARY)
val xmlData = PList.encode(plistValue, PListFormat.XML)
val decoded = PList.decode(binaryData)

if (decoded.isDict) {
    val dict = decoded.dict
    val stringValue = dict["stringKey"]?.string
    val intValue = dict["intKey"]?.int
    println("String value: $stringValue, Int value: $intValue")
}
```

</details>

### Java Example

<details>
<summary>Java encode/decode</summary>

```java
import com.getiox.plist.*;

import java.util.List;
import java.util.Map;

PListValue root = new PListDict(Map.of(
    "stringKey", new PListString("stringValue"),
    "intKey", new PListInt(123),
    "boolKey", new PListBool(true),
    "realKey", new PListReal(123.456),
    "dateKey", new PListDate(System.currentTimeMillis()),
    "dataKey", new PListData(new byte[]{1, 2, 3}),
    "arrayKey", new PListArray(List.of(
        new PListInt(1),
        new PListBool(false),
        new PListReal(3.1415926)
    )),
    "dictKey", new PListDict(Map.of(
        "stringKey", new PListString("stringValue"),
        "intKey", new PListInt(123)
    ))
));

byte[] binaryData = PList.encode(root, PListFormat.BINARY);
byte[] xmlData = PList.encode(root, PListFormat.XML);
PListValue decoded = PList.decode(binaryData);

if(decoded instanceof
PListDict dict){
PListValue stringValue = dict.get("stringKey");
PListValue intValue = dict.get("intKey");

    if(stringValue instanceof
PListString str &&intValue instanceof
PListInt num){
    System.out.

println("String value: "+str.getValue() +", Int value: "+num.

getValue());
    }
    }
```

</details>

---  

---

## Samples

Explore example projects to see how `kplist` is used in real applications:

- [`kplist-java`](https://github.com/demoofbug/kplist/tree/main/samples/kplist-java): A Java-based sample showing how to read and write plist files in both XML and binary formats.
- [`kplist-kotlin-multiplatform`](https://github.com/demoofbug/kplist/tree/main/samples/kplist-kotlin-multiplatform): A full-stack Kotlin Multiplatform sample covering JVM, Android, iOS, and Native platforms.





## API Reference

### Data Types

| Plist Type | Kotlin Class  | Description          |
| ---------- | ------------- | -------------------- |
| Dictionary | `PListDict`   | Key-value mapping    |
| Array      | `PListArray`  | Ordered collection   |
| String     | `PListString` | Text string          |
| Integer    | `PListInt`    | Integer number       |
| Real       | `PListReal`   | Floating-point value |
| Boolean    | `PListBool`   | Boolean true/false   |
| Date       | `PListDate`   | Timestamps           |
| Data       | `PListData`   | Raw binary data      |
| Null       | `PListNull`   | Null value           |

### Encoding and Decoding

```kotlin
object PList {
    fun encode(data: PListValue, format: PListFormat = PListFormat.XML): ByteArray
    fun decode(data: ByteArray): PListValue
}

enum class PListFormat {
    XML,
    BINARY
}
```

### Extension Functions

Kotlin extension utilities for convenient conversion and type access:

```kotlin
fun PListValue.encodeToByteArray(format: PListFormat = PListFormat.XML): ByteArray
fun ByteArray.decodeToPListValue(): PListValue

val PListValue.isDict: Boolean
val PListValue.isArray: Boolean
val PListValue.isString: Boolean
val PListValue.isInt: Boolean
val PListValue.isReal: Boolean
val PListValue.isBool: Boolean
val PListValue.isDate: Boolean
val PListValue.isData: Boolean
val PListValue.isNull: Boolean

val PListValue.dict: PListDict
val PListValue.array: PListArray
val PListValue.string: String
val PListValue.int: Long
val PListValue.real: Double
val PListValue.bool: Boolean
val PListValue.date: Instant
val PListValue.data: ByteArray

val String.plistValue: PListString
val Long.plistValue: PListInt
val Int.plistValue: PListInt
val Double.plistValue: PListReal
val Boolean.plistValue: PListBool
val ByteArray.plistValue: PListData
val Instant.plistValue: PListDate
val Map<String, PListValue>.plistValue: PListDict
val List<PListValue>.plistValue: PListArray
```

---

## License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE.txt) for full details.
