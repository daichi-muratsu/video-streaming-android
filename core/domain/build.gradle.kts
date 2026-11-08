plugins {
    alias(libs.plugins.videostreaming.jvm.library)
}

dependencies {
    // Repositoryのinterfaceが`Flow`を返し、実装する`:core:data`と受け取る`:feature:*`も`Flow`の型を使うため
    api(libs.kotlinx.coroutines.core)
}
