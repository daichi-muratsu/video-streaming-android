plugins {
    alias(libs.plugins.videostreaming.android.library)
    alias(libs.plugins.videostreaming.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.daichimuratsu.videostreaming.core.data"
}

// スキーマのJSONをコミットし、テーブルを変えるときの自動マイグレーションと、PRでの差分の確認に使う
room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Domain層のRepositoryのinterfaceを実装する（依存関係の逆転）
    implementation(project(":core:domain"))

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    // Retrofit 3が持ち込む4.12.0ではなく、Version Catalogのバージョンにする。画像の読み込み（Coil）と`OkHttpClient`を共有するため
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.sqlite.bundled)
}
