plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    macosX64 {
        binaries {
            executable {
                baseName = "plist"
                entryPoint = "com.getiox.kplist.main"
            }
        }
    }
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kplist"))
                implementation(libs.kotlinx.io)
            }
        }
    }
}