/**
 * Script for pushing tags for androidx libraries versions commits
 *
 * Run from command line:
 * 1. Download https://github.com/JetBrains/kotlin/releases/tag/v1.9.22 and add `bin` to PATH
 * 2. Call `kotlin <fileName>`
 *
 * Run from IntelliJ:
 * 1. Right click on the script
 * 2. More Run/Debug
 * 3. Modify Run Configuration...
 * 4. Clear all "Before launch" tasks (you can edit the system-wide template as well)
 * 5. OK
 */

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jsoup:jsoup:1.17.2")

import java.util.concurrent.TimeUnit
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

val libs = listOf(
    "https://developer.android.com/jetpack/androidx/releases/compose-ui",
    "https://developer.android.com/jetpack/androidx/releases/compose-material3",
    "https://developer.android.com/jetpack/androidx/releases/collection",
    "https://developer.android.com/jetpack/androidx/releases/annotation",
    "https://developer.android.com/jetpack/androidx/releases/lifecycle",
    "https://developer.android.com/jetpack/androidx/releases/navigation",
    "https://developer.android.com/jetpack/androidx/releases/window",
    "https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive",
)

val tagPattern = "androidx/*"
fun tagName(libName: String, version: String) = "androidx/$libName/$version"

"git remote add aosp https://android.googlesource.com/platform/frameworks/support".execCommand().apply {
    if (this.isNullOrEmpty()) {
        println("Added aosp https://android.googlesource.com/platform/frameworks/support repo")
    } else {
        check(contains("already exists"))
    }
}
"git fetch aosp".runCommand()
"git fetch".runCommand()

val tags = "git tag -l $tagPattern".execCommand()?.split("\n").orEmpty()

for (lib in libs) {
    println("=== Loading $lib")

    fun Element.extractNameCommitVersion(): Triple<String, String, String> {
        // <code translate="no" dir="ltr">androidx.lifecycle:lifecycle-viewmodel-savedstate:1.0.0-rc03</code>
        // <code translate="no" dir="ltr">androidx.compose.ui:ui-*:1.2.0-alpha08</code>
        val text = toString()
        // androidx.compose.ui:ui-*:1.2.0-alpha08
        val coords = select("code").first()?.text().orEmpty()

        val group = coords.substringBefore(":") // androidx.compose.ui
        val module = coords.substringAfter(":").substringBefore(":") // ui-*

        val shortModule = module.replace("-*", "").replace(".", "-") // ui
        val name = if (group.count { it == '.' } > 1) {
            val shortGroup = group.substringAfter(".").substringBeforeLast(".").replace(".", "-") // compose
            "$shortGroup-$shortModule"
        } else {
            shortModule
        }
        val commit = text.substringAfter("frameworks/support/+log/").run {
            if (contains("..")) {
                substringAfter("..")
            } else {
                this
            }
        }.substringBefore("/").substringBefore("\"").validateCommit()
        val version = text.substringAfter("Version ").substringBefore(" contains").validateVersion()
        return Triple(name, commit, version)
    }

    Jsoup
        .connect(lib)
        .get()
        .select("*")
        .asSequence()
        .filter { it.tagName() == "p" && it.toString().contains("these commits") }
        .map { it.extractNameCommitVersion() }
        .forEach { (name, commit, version) ->
            val tagName = tagName(name, version)
            if (tagName !in tags) {
                println("Tag $tagName on $commit created")
                "git tag -f $tagName $commit".runCommand()
            } else {
                println("Tag $tagName on $commit already exists")
            }
        }

}

println("Pushing tags")
"git push origin tag $tagPattern".runCommand()

fun String.validateCommit() = apply {
    check(isNotEmpty() && all { it.isDigit() || it.isLetter() }) {
        "Commit name isn't correct: $this"
    }
}

fun String.validateVersion() = apply {
    check(isNotEmpty() && all { it.isDigit() || it.isLetter() || it == '.' || it == '-' }) {
        "Version name isn't correct: $this"
    }
}

fun String.validateLibName() = apply {
    check(isNotEmpty() && all { it.isDigit() || it.isLetter() || it == '.' || it == '-' }) {
        "Lib name isn't correct: $this"
    }
}

// from https://stackoverflow.com/a/41495542, redirect output to std
fun String.runCommand() {
    ProcessBuilder(*split(" ").toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(5, TimeUnit.MINUTES)
}

fun String.execCommand(): String? {
    val proc = Runtime.getRuntime().exec(this)
    return proc.inputStream.bufferedReader().readText().ifEmpty { null } ?:
        proc.errorStream.bufferedReader().readText().ifEmpty { null }
}
