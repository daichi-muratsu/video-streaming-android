plugins {
    alias(libs.plugins.videostreaming.android.library)
    alias(libs.plugins.videostreaming.android.compose)
}

android {
    namespace = "io.github.daichimuratsu.videostreaming.core.ui"
}

dependencies {
    implementation(libs.androidx.compose.material3)
}
