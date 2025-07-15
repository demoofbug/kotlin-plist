plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}
dependencies {
    api(project(":kplist"))
}
