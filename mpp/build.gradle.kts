import org.jetbrains.androidx.build.ComposePublishingTask
import org.jetbrains.androidx.build.ArtifactRedirection
import org.jetbrains.androidx.build.ComposePlatforms
import org.jetbrains.androidx.build.JetBrainsPublication
import org.jetbrains.androidx.build.artifactRedirection
import org.jetbrains.androidx.build.hasRedirection

// this module depends on all other modules info, so we need to initialize them first
(rootProject.allprojects - project).forEach {
    evaluationDependsOn(it.path)
}

val libraryToComponents = JetBrainsPublication.libraryToComponents

val pathToComposeComponent = libraryToComponents.values.flatten().associateBy { it.path }
val Project.composeComponent get() = pathToComposeComponent[path]

tasks.register("publishComposeJb", ComposePublishingTask::class) {
    group = "Compose Multiplatform"
    repository = "MavenRepository"

    libraries.forEach {
        libraryToComponents[it]?.forEach(::publish)
    }
}

tasks.register("publishComposeJbToMavenLocal", ComposePublishingTask::class) {
    group = "Compose Multiplatform"
    repository = "MavenLocal"

    libraries.forEach {
        libraryToComponents[it]?.forEach(::publish)
    }
}

val libraries = project.findProperty("jetbrains.publication.libraries")
    ?.toString()?.split(",")
    ?: libraryToComponents.keys


tasks.register("testDesktop") {
    group = "Compose Multiplatform"
    dependsOn(allTasksWith(name = "desktopTest"))
    dependsOn(":collection:collection:jvmTest")
}

tasks.register("testWeb") {
    group = "Compose Multiplatform"
    dependsOn(testWebJs)
    dependsOn(testWebWasm)
}

val testWebJs = tasks.register("testWebJs") {
    dependsOn(":collection:collection:compileTestKotlinJs")
    dependsOn(":compose:foundation:foundation:jsTest")
    dependsOn(":compose:material3:material3:jsTest")
    dependsOn(":compose:runtime:runtime:jsTest")
    dependsOn(":compose:ui:ui-text:jsTest")
    dependsOn(":compose:ui:ui:jsTest")
    dependsOn(":compose:ui:ui-test:jsTest")
    dependsOn(":navigation:navigation-runtime:jsTest")
}

val testWebWasm = tasks.register("testWebWasm") {
    // TODO: ideally we want to run all wasm tests that are possible but now we deal only with modules that have skikoTests
    dependsOn(":collection:collection:wasmJsTest")
    dependsOn(":compose:foundation:foundation:wasmJsTest")
    dependsOn(":compose:material3:material3:wasmJsTest")
    dependsOn(":compose:runtime:runtime:wasmJsTest")
    dependsOn(":compose:ui:ui-text:wasmJsTest")
    dependsOn(":compose:ui:ui:wasmJsTest")
    dependsOn(":compose:ui:ui-test:wasmJsTest")
    dependsOn(":navigation:navigation-runtime:wasmJsTest")
}

tasks.register("testIos") {
    group = "Compose Multiplatform"
    val suffix = if (System.getProperty("os.arch") == "aarch64") "SimulatorArm64Test" else "X64Test"
    val iosTestSubtaskName = "ios$suffix"

    dependsOn(":compose:runtime:runtime:$iosTestSubtaskName")
    dependsOn(":compose:ui:ui-text:$iosTestSubtaskName")
    dependsOn(":compose:ui:ui:$iosTestSubtaskName")
    dependsOn(":compose:material3:material3:$iosTestSubtaskName")
    dependsOn(":compose:foundation:foundation:$iosTestSubtaskName")
    dependsOn(":collection:collection:$iosTestSubtaskName")
}

tasks.register("testRuntimeNative") {
    group = "Compose Multiplatform"
    dependsOn(":compose:runtime:runtime:macosX64Test")
}

tasks.register("testComposeModules") { // used in https://github.com/JetBrains/androidx/tree/jb-main/.github/workflows
    group = "Compose Multiplatform"
    // TODO: download robolectrict to run ui:ui:test
    // dependsOn(":compose:ui:ui:test")

    dependsOn(":compose:ui:ui-graphics:test")
    dependsOn(":compose:ui:ui-geometry:test")
    dependsOn(":compose:ui:ui-unit:test")
    dependsOn(":compose:ui:ui-util:test")
    dependsOn(":compose:runtime:runtime:test")
    dependsOn(":compose:runtime:runtime-saveable:test")
    dependsOn(":compose:material:material:test")
    dependsOn(":compose:material:material-ripple:test")
    dependsOn(":compose:foundation:foundation:test")
    dependsOn(":compose:animation:animation:test")
    dependsOn(":compose:animation:animation-core:test")
    dependsOn(":compose:animation:animation-core:test")

    // TODO: enable ui:ui-text:test
    // dependsOn(":compose:ui:ui-text:test")
    // compose/out/androidx/compose/ui/ui-text/build/intermediates/tmp/manifest/test/debug/tempFile1ProcessTestManifest10207049054096217572.xml Error:
    // android:exported needs to be explicitly specified for <activity>. Apps targeting Android 12 and higher are required to specify an explicit value for `android:exported` when the corresponding component has an intent filter defined.
}

tasks.register("jbApiDump") {
    group = "Compose Multiplatform"
    dependsOn(apiValidationTasks(suffix = "ApiDump"))
}

tasks.register("jbApiCheck") {
    group = "Compose Multiplatform"
    dependsOn(apiValidationTasks(suffix = "ApiCheck"))
}

fun apiValidationTasks(suffix: String) = buildSet<Task> {
    fun Iterable<Task>.filterComposePlatforms(vararg platforms: ComposePlatforms) =
        filter { task ->
            val project = task.project
            val component = project.composeComponent
            platforms.any {
                component != null
                    && it in component.supportedPlatforms
                    && !project.hasRedirection(it)
            }
        }

    fun Iterable<Task>.filterComposePlatforms(platforms: Set<ComposePlatforms>) =
        filterComposePlatforms(*platforms.toTypedArray())

    this += allTasksWith(name = "desktop$suffix")
        .filterComposePlatforms(ComposePlatforms.Desktop)

    this += allTasksWith(name = "android$suffix")
        .filterComposePlatforms(ComposePlatforms.ANDROID)

    val klibPlatforms = if (System.getProperty("os.name") == "Mac OS X") {
        ComposePlatforms.GENERATE_KLIB
    } else {
        ComposePlatforms.GENERATE_KLIB - ComposePlatforms.DARWIN
    }
    this += allTasksWith(name = "klib$suffix")
        .filterComposePlatforms(klibPlatforms)
}

fun allTasksWith(name: String): List<Task> =
    rootProject.subprojects.mapNotNull { project ->
        project.tasks.findByName(name)
    }

// ./gradlew printAllArtifactRedirectionVersions -PfilterProjectPath=lifecycle
// or just ./gradlew printAllArtifactRedirectionVersions
tasks.register("printAllArtifactRedirectionVersions") {
    val filter = project.properties["filterProjectPath"] as? String ?: ""
    doLast {
        val map = libraryToComponents.values.flatten().filter { it.path.contains(filter) }
            .joinToString("\n\n", prefix = "\n") {
            val p = rootProject.findProject(it.path)!!
            it.path + " --> \n" + (p.artifactRedirection().prettyText())
        }

        println(map)
    }
}

fun ArtifactRedirection?.prettyText(): String {
    val allLines = if (this != null) {
        arrayOf(
            "redirectGroupId = ${this.groupId}",
            "redirectDefaultVersion = ${this.defaultVersion}",
            "redirectForTargets = [${this.targetNames.joinToString().takeIf { it.isNotBlank() } ?: "android"}]",
            "redirectTargetVersions = ${this.targetVersions}"
        )
    } else {
        arrayOf("disabled")
    }

    return allLines.joinToString("") { " ".repeat(3) + "$it\n" }
}
