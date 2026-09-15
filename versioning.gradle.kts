import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

val versionFile = rootProject.file("version.properties")
val props = Properties()
if (versionFile.exists()) {
    FileInputStream(versionFile).use { props.load(it) }
} // ponytail: clean clones (CI) have no version file; fall back to 0.1.0+1 so versionCode stays > 0. CI never ships.

fun p(name: String): Int = (props.getProperty(name) ?: "0").toIntOrNull() ?: 0

var major = p("major")
var minor = p("minor")
var patch = p("patch")
var build = p("build")
if (!versionFile.exists() && major == 0 && minor == 0 && patch == 0 && build == 0) {
    minor = 1
    build = 1
}

val type = project.findProperty("versionType") as? String
when (type) {
    "major" -> { major++; minor = 0; patch = 0 }
    "minor" -> { minor++; patch = 0 }
    "patch" -> { patch++ }
    "build" -> { build++ }
}

project.findProperty("major")?.toString()?.toIntOrNull()?.let { major = it }
project.findProperty("minor")?.toString()?.toIntOrNull()?.let { minor = it }
project.findProperty("patch")?.toString()?.toIntOrNull()?.let { patch = it }
project.findProperty("build")?.toString()?.toIntOrNull()?.let { build = it }

if (type in setOf("major", "minor", "patch", "build")) {
    props["major"] = major.toString()
    props["minor"] = minor.toString()
    props["patch"] = patch.toString()
    props["build"] = build.toString()
    FileOutputStream(versionFile).use { props.store(it, "App version - bump with -PversionType=major|minor|patch|build") }
}

extra.set("appVersionName", "$major.$minor.$patch")
// ponytail: 2-digit slots for patch/build (0-99), minor (0-99), major up to 2100 (Play max 2,100,000,000)
extra.set("appVersionCode", major * 1000000 + minor * 10000 + patch * 100 + build)
