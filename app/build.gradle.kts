import org.gradle.api.tasks.testing.Test

apply(from = rootProject.file("versioning.gradle.kts"))

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licensee)
}

// Allow-list is the measured licence set of the resolved graph; THIRD_PARTY.md
// carries the inventory and the reason for each entry.
licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-3-Clause")
    // Material Kolor declares "The MIT License" in its POM but as a URL, not SPDX.
    allowUrl("https://github.com/jordond/materialkolor/blob/master/LICENSE")
    // SLF4J API declares MIT in slf4j-parent as a URL, not SPDX.
    allowUrl("https://opensource.org/license/mit")
}

ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
}

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
            val storeFilePath = providers.gradleProperty("RELEASE_STORE_FILE").getOrElse("")
            if (storeFilePath.isNotEmpty()) {
                storeFile = file(storeFilePath)
                storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").getOrElse("")
                keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").get()
                enableV3Signing = true
            }
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
            if (releaseSigning.storeFile != null) signingConfig = releaseSigning
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
    implementation(libs.lifecycle.viewmodel.navigation3)
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
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.room3.testing)
    androidTestImplementation(libs.work.testing)
    androidTestImplementation(libs.sqlite.driver)
    androidTestUtil(libs.test.orchestrator)
    lintChecks(libs.security.lint)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Fail the build when a dependency introduces a licence outside the allow-list.
// `licensee` aggregates every Android variant the plugin registers.
tasks.named("check") {
    dependsOn("licensee")
}

/**
 * Builds res/raw/third_party.json from the graph licensee already resolved, so the
 * in-app notices screen cannot drift from the dependency set.
 *
 * One entry per runtime artifact, each pointing at one of the few distinct
 * licence bodies in res/raw/licences/. Only the bodies are embedded — the full
 * text is shared by every artifact under the same SPDX id, so 273 artifacts cost
 * 3 texts, not 273.
 *
 * Fails the build on an unmapped licence rather than shipping a row with no body:
 * a silent licence regression is exactly what the gate above exists to catch.
 */
val licensesDir = layout.projectDirectory.dir("src/main/res/raw")
val noticesOutput = licensesDir.file("third_party.json")

// POMs that declare their licence by URL instead of an SPDX id. Keys are the
// URLs licensee records in artifacts.json; values are the body we ship.
val licenseByUrl = mapOf(
    "https://github.com/jordond/materialkolor/blob/master/LICENSE" to "mit.txt",
    "https://opensource.org/license/mit" to "mit.txt"
)

val generateLicenseJson by tasks.registering {
    description = "Generates the third-party notices JSON from the resolved dependency graph."
    group = "build"

    val report = layout.buildDirectory.file("reports/licensee/androidDebug/artifacts.json")
    val out = noticesOutput
    val urlMap = licenseByUrl

    inputs.file(report)
    // Only the licence bodies: the output itself must not be its own input.
    inputs.files("src/main/res/raw/apache_2_0.txt", "src/main/res/raw/mit.txt",
        "src/main/res/raw/bsd_3_clause.txt")
    outputs.file(out)

    doLast {
        @Suppress("UNCHECKED_CAST")
        val parsed = groovy.json.JsonSlurper().parse(report.get().asFile)
        val entries = parsed as List<Map<String, Any?>>
        val rows = entries
            .sortedBy { "${it["groupId"]}:${it["artifactId"]}" }
            .map { a ->
                val group = a["groupId"] as? String ?: ""
                val name = a["artifactId"] as? String ?: ""
                val version = a["version"] as? String ?: ""
                val label = a["name"] as? String ?: name
                @Suppress("UNCHECKED_CAST")
                val spdx = (a["spdxLicenses"] as? List<Map<String, Any?>>) ?: emptyList()
                // licensee files POMs that name a licence but give no SPDX id under
                // "unknownLicenses", keyed by the URL the POM declared.
                @Suppress("UNCHECKED_CAST")
                val unknown = (a["unknownLicenses"] as? List<Map<String, Any?>>) ?: emptyList()
                val body = when {
                    spdx.isNotEmpty() -> when (val id = spdx[0]["identifier"] as? String) {
                        // Values are res/raw filenames, not SPDX ids: Android resource
                        // identifiers forbid the dash, so R.raw.apache_2_0 is the only spelling.
                        "Apache-2.0" -> "apache_2_0.txt"
                        "MIT" -> "mit.txt"
                        "BSD-3-Clause" -> "bsd_3_clause.txt"
                        else -> throw GradleException(
                            "Licence '$id' from $group:$name has no body in res/raw/licences. " +
                                "Add it there, or map it in generateLicenseJson."
                        )
                    }
                    unknown.any { it["url"] in urlMap } ->
                        urlMap[unknown.first { it["url"] in urlMap }["url"]]
                    else -> throw GradleException(
                        "$group:$name declares no SPDX id and no known licence URL " +
                            "(${unknown.map { it["url"] }}). Add it to licenseByUrl in " +
                            "app/build.gradle.kts."
                    )
                }
                val safe = { v: String? -> (v ?: "").replace("\\", "\\\\").replace("\"", "\\\"") }
                "  {\"group\": \"${safe(group)}\", \"artifact\": \"${safe(name)}\", " +
                    "\"version\": \"${safe(version)}\", \"label\": \"${safe(label)}\", " +
                    "\"license\": \"$body\"}"
            }
        out.asFile.writeText(rows.joinToString(",\n", prefix = "[\n", postfix = "\n]\n"), Charsets.UTF_8)
        logger.lifecycle("Wrote ${rows.size} third-party notices to $out")
    }
}

// artifacts.json only exists once licensee has run.
generateLicenseJson.configure { dependsOn("licenseeAndroidDebug") }

// Keep the generated JSON in step with the dependency graph on every build.
// Hooked on preBuild rather than a generate*Assets task: AGP's task names for
// asset generation are not stable across versions, and a missing name here fails
// configuration outright.
tasks.matching { it.name.endsWith("PreBuild") }.configureEach {
    dependsOn(generateLicenseJson)
}
