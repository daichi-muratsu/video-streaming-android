plugins {
    alias(libs.plugins.videostreaming.android.library)
    alias(libs.plugins.videostreaming.hilt)
}

android {
    namespace = "io.github.daichimuratsu.videostreaming.core.data"
}

dependencies {
    // Domain層のRepositoryのinterfaceを実装する（依存関係の逆転）
    implementation(project(":core:domain"))
}
