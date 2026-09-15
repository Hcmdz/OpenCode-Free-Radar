import java.util.Properties
import org.gradle.api.tasks.testing.Test

apply(from = rootProject.file("versioning.gradle.kts"))

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
}

val keystoreFichier = file("/home/hicham/My Android Projects/My keystore/gradle.properties")
// Public clones have no keystore: build stays configurable, release just ships unsigned.
val keystorePresent = keystoreFichier.exists()
val keystoreProperties = Properties().apply {
    if (keystorePresent) keystoreFichier.inputStream().use { load(it) }
}

fun proprieteKeystore(nom: String): String =
    keystoreProperties.getProperty(nom, "")

android {
    namespace = "com.opencode.freeradar"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.opencode.freeradar"
        minSdk = 29
        targetSdk = 36
        versionCode = project.extra["appVersionCode"] as Int
        versionName = project.extra["appVersionName"] as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    signingConfigs {
        create("release") {
            storeFile = file(proprieteKeystore("RELEASE_STORE_FILE"))
            storePassword = proprieteKeystore("RELEASE_STORE_PASSWORD")
            keyAlias = proprieteKeystore("RELEASE_KEY_ALIAS")
            keyPassword = proprieteKeystore("RELEASE_KEY_PASSWORD")
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (keystorePresent) signingConfig = releaseSigning
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        localeFilters += listOf("en", "fr", "ar")
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom.alpha))
    implementation(libs.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.coroutines.core)
    implementation(libs.datastore.preferences)
    implementation(libs.material.kolor)
    implementation(libs.graphics.shapes)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room3.runtime)
    ksp(libs.room3.compiler)
    implementation(libs.work.runtime.ktx)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.assertk)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.mock)
    debugImplementation(libs.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom.alpha))
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.room3.testing)
    androidTestImplementation(libs.sqlite.driver)
    androidTestUtil(libs.test.orchestrator)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
