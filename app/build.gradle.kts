import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.io.ByteArrayOutputStream
import java.nio.file.Paths

val localProperties = gradleLocalProperties(rootDir)

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-android")
}

android {
    val buildTime = System.currentTimeMillis()
    val baseVersionName = "0.1-wip"

    compileSdk = compile_sdk

    defaultConfig {
        applicationId = "com.duzhaokun123.bilibilihd2"
        minSdk = min_sdk
        targetSdk = target_sdk
        versionCode = 1
        versionName = "$baseVersionName-git.$gitHash"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("long", "BUILD_TIME", buildTime.toString())
        buildConfigField(
            "String",
            "PROJECT_HOME",
            "\"https://github.com/duzhaokun123/BilibiliHD2\""
        )
        buildConfigField(
            "String",
            "DONATE_LINK",
            "\"https://duzhaokun123.github.io/donate.html\""
        )

        var appSecret = System.getenv("APP_SECRET")?.takeIf { it.isNotEmpty() }
                ?: localProperties.getProperty("app.secret")?.takeIf { it.isNotEmpty() }
                ?: ""
        if (localProperties.getProperty("analytics.enabled", "true") != "true")
            appSecret = ""
        buildConfigField("String", "APP_SECRET", "\"$appSecret\"")
    }
    packagingOptions {
        resources.excludes.addAll( arrayOf(
            "META-INF/**",
            "kotlin/**",
            "okhttp3/**",
            "google/**",
            "bilibili/**",
            "github.com/**"
        ))
    }
    signingConfigs {
        create("release") {
            storeFile = file("../releaseKey.jks")
            storePassword = System.getenv("REL_KEY")
            keyAlias = "key0"
            keyPassword = System.getenv("REL_KEY")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("REL_KEY") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        getByName("debug") {
            val minifyEnabled = localProperties.getProperty("minify.enabled", "false")
            isMinifyEnabled = minifyEnabled.toBoolean()
            isShrinkResources = minifyEnabled.toBoolean()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
    }
    lint {
        abortOnError = false
    }
    namespace = "com.duzhaokun123.bilibilihd2"
}

dependencies {
    implementation("androidx.core:core-ktx:$androidx_core_ktx_version")
    implementation("androidx.appcompat:appcompat:$androidx_appcompat_version")
    implementation("com.google.android.material:material:$material_version")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.1.3")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    //bilibili-api
    implementation(project(":bilibili-api"))

    //preferencex
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("com.takisoft.preferencex:preferencex:1.1.0")
    implementation("com.takisoft.preferencex:preferencex-simplemenu:1.1.0")

    //kotlinx-coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    //AnnotationProcessor
    kapt(project(":annotation-processor"))
    compileOnly(project(":annotation-processor"))

    //lifecycle
    val lifecycleVersion = "2.3.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    //gson
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    //glide
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")

    //nav
    val navVersion = "2.4.0-alpha01"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("androidx.drawerlayout:drawerlayout:1.1.1")

    //dataBinding
    kapt("com.android.databinding:compiler:3.2.0-alpha10")

    //SmartRefreshLayout
    val srlVersion = "2.0.3"
    implementation("com.scwang.smart:refresh-layout-kernel:$srlVersion")
    implementation("com.scwang.smart:refresh-header-material:$srlVersion")
    implementation("com.scwang.smart:refresh-footer-classics:$srlVersion")

    //browser
    implementation("androidx.browser:browser:1.4.0")

    //StfalconImageViewer
    implementation(project(":imageviewer"))

    //BiliPlayer
    implementation(project(":bili-player"))

    //grpc
    implementation(project(":grpc"))

    //qr
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    //appcenter
    val appCenterSdkVersion = "4.3.1"
    implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    //core-splashscreen
    implementation("androidx.core:core-splashscreen:1.0.0-beta02")
}

val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
    val aapt2 = Paths.get(
        project.android.sdkDirectory.path,
        "build-tools", project.android.buildToolsVersion, "aapt2"
    )
    val zip = Paths.get(
        project.buildDir.path, "intermediates",
        "optimized_processed_res", "release", "resources-release-optimize.ap_"
    )
    val optimized = File("${zip}.opt")
    val cmd = exec {
        commandLine(aapt2, "optimize", "--collapse-resource-names", "-o", optimized, zip)
        isIgnoreExitValue = true
    }
    if (cmd.exitValue == 0) {
        delete(zip)
        optimized.renameTo(zip.toFile())
    }
}
tasks.whenTaskAdded {
    when (name) {
        "optimizeReleaseResources" -> {
            finalizedBy(optimizeReleaseRes)
        }
    }
}

val gitHash: String
    get() {
        val out = ByteArrayOutputStream()
        val cmd = exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = out
            isIgnoreExitValue = true
        }
        return if (cmd.exitValue == 0)
            out.toString().trim()
        else
            "(error)"
    }
