# Changelog

# Latest Changes

Badges: `[UPDATED]`, `[FIXED]`, `[NEW]`, `[DEPRECATED]`, `[REMOVED]`, `[BREAKING]`, `[Experimental]`

---

# [0.0]()

## [0.0.1]()

**Initial release of `kplist` – a Kotlin Multiplatform library for parsing Apple plist files.**

### [New]

- Added support for parsing **XML** and **Binary** (bplist00) plist formats.
- Provided a unified API: `PList.encode(...)` and `PList.decode(...)`.
- Supported all standard plist value types:
    - Dictionary (`PListDict`)
    - Array (`PListArray`)
    - String (`PListString`)
    - Integer (`PListInt`)
    - Real (`PListReal`)
    - Boolean (`PListBool`)
    - Date (`PListDate`)
    - Data (`PListData`)
    - Null (`PListNull`)
- Enabled multiplatform support:
    - **JVM**, **Android**, **iOS**, **macOS**, **Windows**, **Linux**, and **JavaScript**.
- Added Kotlin DSL extensions for building plist trees.
- Provided type-safe accessors (e.g., `.string`, `.int`, `.data`, etc.).
- Included `plistValue` extension properties for primitive types (Kotlin only).
- Auto-detects format when decoding.

### [Experimental]

- Initial Java-friendly API (`PListDict`, `PListArray`, etc.) to support usage in pure Java projects.

## [0.0.2]()
### Refactor
- Rename project from `kplist` to `kotlin-plist`
- Rename `lib` directory to `plist` and update file paths
- Update package from `com.getiox.kplist` to `com.getiox.plist`
- No functional changes; only reorganized files and standardized package naming


