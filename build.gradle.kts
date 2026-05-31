plugins {
    id("com.android.application") version "9.2.0" apply false
    id("com.github.triplet.play") version "4.0.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
}

fun bumpAppVersion(bumpMajor: Boolean) {
    val versionFile = rootProject.layout.projectDirectory.file("app/build.gradle.kts").asFile
    val versionCodePattern = Regex("""(versionCode\s*=\s*)(\d+)""")
    val versionNamePattern = Regex("""(versionName\s*=\s*")(\d+)\.(\d+)(")""")

    val original = versionFile.readText()
    var versionCodeUpdated = false
    var versionNameUpdated = false

    val updated = original.lineSequence().joinToString("\n") { line ->
        var next = line

        next = versionCodePattern.replace(next) { match ->
            versionCodeUpdated = true
            val nextVersionCode = match.groupValues[2].toInt() + 1
            "${match.groupValues[1]}$nextVersionCode"
        }

        next = versionNamePattern.replace(next) { match ->
            versionNameUpdated = true
            val major = match.groupValues[2].toInt()
            val minor = match.groupValues[3].toInt()
            val nextVersionName = if (bumpMajor) {
                "${major + 1}.0"
            } else {
                "$major.${minor + 1}"
            }
            "${match.groupValues[1]}$nextVersionName${match.groupValues[4]}"
        }

        next
    }

    require(versionCodeUpdated) { "Could not find versionCode in ${versionFile.path}" }
    require(versionNameUpdated) { "Could not find versionName in ${versionFile.path}" }

    versionFile.writeText(updated)
}

tasks.register("bumpVersionMajor") {
    group = "versioning"
    description = "Increment app versionCode by 1 and bump versionName major, e.g. 1.0 -> 2.0."

    doLast {
        bumpAppVersion(bumpMajor = true)
    }
}

tasks.register("bumpVersionMinor") {
    group = "versioning"
    description = "Increment app versionCode by 1 and bump versionName minor, e.g. 1.0 -> 1.1."

    doLast {
        bumpAppVersion(bumpMajor = false)
    }
}

tasks.register("deployInternalTesting") {
    group = "publishing"
    description = "Build and publish the release App Bundle to the Google Play internal testing track."
    dependsOn(":app:publishReleaseBundle")
}

tasks.register("promoteInternalTestingToProduction") {
    group = "publishing"
    description = "Promote the current Google Play internal testing release to production."
    dependsOn(":app:promoteArtifact")
}
