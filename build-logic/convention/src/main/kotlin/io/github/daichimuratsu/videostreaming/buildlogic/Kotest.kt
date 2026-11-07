package io.github.daichimuratsu.videostreaming.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * ユニットテストをKotestで書けるようにする。
 * KotestはJUnit Platformの上で動くため、テストのタスクでJUnit Platformを使う。
 * Androidモジュールのユニットテストのタスクも`Test`型なので、同じ設定で済む
 */
internal fun Project.configureKotest() {
    dependencies {
        add("testImplementation", libs.library("kotest-runner-junit5"))
        add("testImplementation", libs.library("kotest-assertions-core"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
