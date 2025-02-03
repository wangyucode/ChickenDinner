// filepath: /C:/Users/www_w/code/AndroidWorkspace/Control/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdkVersion(34)
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "cn.wycode.control"
        minSdkVersion(28)
        targetSdkVersion(34)
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    namespace = "cn.wycode.control"
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.54")
}