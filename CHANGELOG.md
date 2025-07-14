# Changelog

# Latest Changes

Badges: `[UPDATED]`, `[FIXED]`, `[NEW]`, `[DEPRECATED]`, `[REMOVED]`, `[BREAKING]`, `[Experimental]`

---

# [0.0]()

## [0.0.1]()

**Initial release of `kplist` – a Kotlin Multiplatform library for parsing Apple plist files.**

### [NEW]

- [NEW] Added support for parsing **XML** and **Binary** (bplist00) plist formats.
- [NEW] Provided a unified API: `PList.encode(...)` and `PList.decode(...)`.
- [NEW] Supported all standard plist value types:
    - Dictionary (`PListDict`)
    - Array (`PListArray`)
    - String (`PListString`)
    - Integer (`PListInt`)
    - Real (`PListReal`)
    - Boolean (`PListBool`)
    - Date (`PListDate`)
    - Data (`PListData`)
    - Null (`PListNull`)
- [NEW] Enabled multiplatform support:
    - **JVM**, **Android**, **iOS**, **macOS**, **Windows**, **Linux**, and **JavaScript**.
- [NEW] Added Kotlin DSL extensions for building plist trees.
- [NEW] Provided type-safe accessors (e.g., `.string`, `.int`, `.data`, etc.).
- [NEW] Included `plistValue` extension properties for primitive types (Kotlin only).
- [NEW] Auto-detects format when decoding.

### [Experimental]

- [Experimental] Initial Java-friendly API (`PListDict`, `PListArray`, etc.) to support usage in pure Java projects.
