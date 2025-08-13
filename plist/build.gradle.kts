import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvmToolchain(17)
}

kotlin {
    // macos
    macosArm64()
    macosX64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()

    // linux
    linuxArm64()
    linuxX64()

    // mingwX64
    mingwX64()

    // android
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    js(IR) {
        nodejs {}
        compilations.all {
        }
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.stdlib)
            api(libs.kotlinx.datetime)
            api(libs.xmlutil)
        }
        commonTest.dependencies {
            api(kotlin("test"))
        }
    }
}
android {
    namespace =  project.group.toString()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf {
        System.getenv("CI") == "true"
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    // or when publishing to https://s01.oss.sonatype.org
//    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)

    signAllPublications()

    coordinates(groupId = project.group.toString(), version = project.version.toString())

    pom {
        name = project.name
        description = "Multiplatform Kotlin library for Apple plist (Property List) serialization."
        url = "https://github.com/demoofbug/kotlin-plist"
        licenses {
            license {
                name = "The MIT License"
                url = "https://github.com/demoofbug/kotlin-plist/blob/master/LICENSE"
            }
        }
        developers {
            developer {
                name = "demoofbug"
                email = "demoofbug@gmail.com"
            }
        }
        scm {
            url = "https://github.com/demoofbug/kotlin-plist"
            connection = "scm:git:git://github.com/demoofbug/kotlin-plist.git"
            developerConnection = "scm:git:ssh://github.com/demoofbug/kotlin-plist.git"
        }
    }
}
