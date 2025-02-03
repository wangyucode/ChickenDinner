// filepath: /C:/Users/www_w/code/AndroidWorkspace/Control/common/build.gradle.kts
plugins {
    kotlin("jvm")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}