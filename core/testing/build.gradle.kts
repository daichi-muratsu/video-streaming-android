plugins {
    alias(libs.plugins.videostreaming.jvm.library)
}

dependencies {
    // `api`にする。`MainDispatcherExtension`の公開する型（Kotestのリスナー・`TestDispatcher`）を、使う側のテストでも参照するため
    api(libs.kotest.framework.engine)
    api(libs.kotlinx.coroutines.test)
}
