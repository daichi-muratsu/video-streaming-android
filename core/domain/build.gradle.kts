plugins {
    alias(libs.plugins.videostreaming.jvm.library)
    // RepositoryのFakeを`src/testFixtures`に置き、featureモジュールのテストでも使うため。
    // `src/test`の中身は、ほかのモジュールから参照できない
    `java-test-fixtures`
}

dependencies {
    // Repositoryのinterfaceが`Flow`を返し、実装する`:core:data`と受け取る`:feature:*`も`Flow`の型を使うため
    api(libs.kotlinx.coroutines.core)
    // UseCaseをHiltで作れるようにするため。Hiltそのものではなく、Javaの標準の注釈だけを使う
    implementation(libs.javax.inject)

    testImplementation(libs.turbine)
}
