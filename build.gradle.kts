plugins {
   alias(libs.plugins.kotlin.multiplatform) apply false
   alias(libs.plugins.kotlin.jvm) apply false
   alias(libs.plugins.dokka) apply false
   alias(libs.plugins.ktlint) apply false
   alias(libs.plugins.android.library) apply false
   alias(libs.plugins.maven.publish) apply false
}
