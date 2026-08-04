import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.github.triplet.play")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("jacoco")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localSecret(name: String): String = localProperties.getProperty(name).orEmpty()

fun requireEmtCredentials() {
    val hasEmailCredentials = localSecret("EMT_EMAIL").isNotBlank() && localSecret("EMT_PASSWORD").isNotBlank()
    val hasPassKeyCredentials = localSecret("EMT_CLIENT_ID").isNotBlank() && localSecret("EMT_PASS_KEY").isNotBlank()
    if (!hasEmailCredentials && !hasPassKeyCredentials) {
        throw GradleException(
            "Missing EMT credentials. Add EMT_EMAIL and EMT_PASSWORD, or EMT_CLIENT_ID and EMT_PASS_KEY, to local.properties."
        )
    }
}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun secret(name: String): String = localSecret(name).ifBlank { System.getenv(name).orEmpty() }

fun hasReleaseSigningSecrets(): Boolean = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD",
).all { secret(it).isNotBlank() }

requireEmtCredentials()

android {
    namespace = "org.m0skit0.android.emtmadridalarms"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.m0skit0.android.emtmadridalarms"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "EMT_EMAIL", localSecret("EMT_EMAIL").asBuildConfigString())
        buildConfigField("String", "EMT_PASSWORD", localSecret("EMT_PASSWORD").asBuildConfigString())
        buildConfigField("String", "EMT_CLIENT_ID", localSecret("EMT_CLIENT_ID").asBuildConfigString())
        buildConfigField("String", "EMT_PASS_KEY", localSecret("EMT_PASS_KEY").asBuildConfigString())
    }

    signingConfigs {
        if (hasReleaseSigningSecrets()) {
            create("release") {
                storeFile = rootProject.file(secret("RELEASE_STORE_FILE"))
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigningSecrets()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
    }
}

play {
    defaultToAppBundles.set(true)
    track.set("internal")
    fromTrack.set("internal")
    promoteTrack.set("production")
    releaseStatus.set(ReleaseStatus.COMPLETED)

    val credentialsFile = secret("PLAY_SERVICE_ACCOUNT_JSON")
    if (credentialsFile.isNotBlank()) {
        serviceAccountCredentials.set(rootProject.file(credentialsFile))
    }
}

tasks.register("checkReleaseSigning") {
    group = "verification"
    description = "Verify release signing secrets are configured before publishing to Google Play."

    doLast {
        val missing = listOf(
            "RELEASE_STORE_FILE",
            "RELEASE_STORE_PASSWORD",
            "RELEASE_KEY_ALIAS",
            "RELEASE_KEY_PASSWORD",
        ).filter { secret(it).isBlank() }

        if (missing.isNotEmpty()) {
            throw GradleException("Missing release signing values: ${missing.joinToString()}")
        }

        val keystore = rootProject.file(secret("RELEASE_STORE_FILE"))
        if (!keystore.isFile) {
            throw GradleException("Release keystore not found: ${keystore.path}")
        }
    }
}

tasks.matching { it.name == "publishReleaseBundle" }.configureEach {
    dependsOn("checkReleaseSigning")
}

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(false)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        // Compose UI screens and theme
        "**/ui/*Screen*.*", "**/ui/*Theme*.*", "**/ui/AppChrome*.*", "**/ui/ScreenLayout*.*",
        "**/ui/EmtMadridAlarmsApp*.*", "**/ui/SelectionScreens*.*",
        "**/ui/Routes*.*", "**/ui/AlarmMvi*.*", "**/ui/AlarmStatusScreens*.*",
        "**/ui/ComposableSingletons*.*",
        // Android framework entry points
        "**/ui/MainActivity*.*", "**/BusAlarmApplication*.*", "**/MainActivity*.*",
        // DI wiring
        "**/di/**/*.*",
        // Service layer — requires Android device (Service, Vibrator, Ringtone, NotificationManager)
        "**/service/AlarmMonitorService*.*",
        "**/service/AlarmMonitorController*.*",
        "**/service/AlarmSignalPlayer*.*",
        "**/service/AlarmNotificationFactory*.*",
        "**/service/AlarmServiceIntents*.*",
        "**/service/AlarmServiceCommands*.*",
        // Network + serialisation DTOs — integration-level, no unit test surface
        "**/data/EmtApi*.*",
        "**/data/EmtApiModels*.*",
        "**/data/EmtArrivalService*.*",
        "**/data/EmtLineService*.*",
        "**/data/EmtStopService*.*",
        "**/data/EmtDateProvider*.*",
        "**/data/EmtResponseValidator*.*",
        "**/data/ApiLoggingInterceptor*.*",
        // AlarmController — sends Android Intents, not unit-testable
        "**/ui/AlarmController*.*",
        // RateAppPrompter — launches Google Play In-App Review flow, requires Activity
        "**/ui/RateAppPrompter*.*",
    )
    val debugTree = fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(fileFilter)
    }
    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.4.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    implementation("io.insert-koin:koin-android:4.2.2")
    implementation("io.insert-koin:koin-androidx-compose:4.2.2")

    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("com.google.android.play:review-ktx:2.0.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("io.kotest:kotest-assertions-core:6.2.3")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
