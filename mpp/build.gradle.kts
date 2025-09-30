import org.jetbrains.androidx.build.AbstractComposePublishingTask
import org.jetbrains.androidx.build.ArtifactRedirection
import org.jetbrains.androidx.build.ComposeComponent
import org.jetbrains.androidx.build.ComposePlatforms
import org.jetbrains.androidx.build.artifactRedirection
import org.jetbrains.androidx.build.hasRedirection

buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/internal")
        maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
    }
}

// this module depends on all other modules info, so we need to initialize them first
(rootProject.allprojects - project).forEach {
    evaluationDependsOn(it.path)
}

open class ComposePublishingTask : AbstractComposePublishingTask() {
    override fun dependsOnComposeTask(task: String) {
        dependsOn(task)
    }
}

val libraryToComponents = mapOf(
    "COMPOSE" to listOf(
        // TODO https://youtrack.jetbrains.com/issue/CMP-1604/Publish-public-collection-annotation-libraries-with-a-separate-version
        // They are part of COMPOSE versioning
        ComposeComponent(":annotation:annotation", supportedPlatforms = ComposePlatforms.ALL - ComposePlatforms.ANDROID),
        ComposeComponent(":collection:collection", supportedPlatforms = ComposePlatforms.ALL - ComposePlatforms.ANDROID),

        ComposeComponent(":compose:animation:animation"),
        ComposeComponent(":compose:animation:animation-core"),
        ComposeComponent(":compose:animation:animation-graphics"),
        ComposeComponent(":compose:foundation:foundation"),
        ComposeComponent(":compose:foundation:foundation-layout"),
        ComposeComponent(":compose:material:material"),
        //ComposeComponent(":compose:material:material-icons-core"),
        ComposeComponent(":compose:material:material-ripple"),
        ComposeComponent(":compose:runtime:runtime", supportedPlatforms = ComposePlatforms.ALL),
        ComposeComponent(":compose:runtime:runtime-saveable", supportedPlatforms = ComposePlatforms.ALL),
        ComposeComponent(":compose:ui:ui"),
        ComposeComponent(":compose:ui:ui-geometry"),
        ComposeComponent(
            path = ":compose:ui:ui-backhandler",
            supportedPlatforms = ComposePlatforms.SKIKO_SUPPORT,
        ),
        ComposeComponent(":compose:ui:ui-graphics"),
        ComposeComponent(":compose:ui:ui-test"),
        ComposeComponent(
            ":compose:ui:ui-test-junit4",
            supportedPlatforms = ComposePlatforms.JVM_BASED
        ),
        ComposeComponent(":compose:ui:ui-text"),
        ComposeComponent(":compose:ui:ui-tooling", supportedPlatforms = ComposePlatforms.JVM_BASED),
        ComposeComponent(
            ":compose:ui:ui-tooling-data",
            supportedPlatforms = ComposePlatforms.JVM_BASED
        ),
        ComposeComponent(":compose:ui:ui-tooling-preview"),
        ComposeComponent(
            ":compose:ui:ui-uikit",
            supportedPlatforms = ComposePlatforms.UI_KIT
        ),
        ComposeComponent(":compose:ui:ui-unit"),
        ComposeComponent(":compose:ui:ui-util"),
    ),
    "COMPOSE_MATERIAL_NAVIGATION" to listOf(
        ComposeComponent(":compose:material:material-navigation"),
    ),
    "COMPOSE_MATERIAL3" to listOf(
        ComposeComponent(":compose:material3:material3"),
        ComposeComponent(":compose:material3:material3-window-size-class"),
        ComposeComponent(":compose:material3:material3-adaptive-navigation-suite"),
    ),
    "COMPOSE_MATERIAL3_ADAPTIVE" to listOf(
        ComposeComponent(":compose:material3:adaptive:adaptive"),
        ComposeComponent(":compose:material3:adaptive:adaptive-layout"),
        ComposeComponent(":compose:material3:adaptive:adaptive-navigation"),
    ),
    "LIFECYCLE" to listOf(
        ComposeComponent(
            path = ":lifecycle:lifecycle-common",
            // No android target here - jvm artefact will be used for android apps as well
            supportedPlatforms = ComposePlatforms.ALL_AOSP - ComposePlatforms.ANDROID
        ),
        ComposeComponent(
            path = ":lifecycle:lifecycle-runtime",
            supportedPlatforms = ComposePlatforms.ALL_AOSP
        ),
        ComposeComponent(
            path = ":lifecycle:lifecycle-viewmodel",
            supportedPlatforms = ComposePlatforms.ALL_AOSP
        ),
        ComposeComponent(":lifecycle:lifecycle-viewmodel-savedstate", supportedPlatforms = ComposePlatforms.ALL_AOSP),
        ComposeComponent(":lifecycle:lifecycle-runtime-compose", supportedPlatforms = ComposePlatforms.ALL),
        ComposeComponent(":lifecycle:lifecycle-viewmodel-compose"),
    ),
    "NAVIGATION" to listOf(
        ComposeComponent(":navigation:navigation-compose"),
        ComposeComponent(":navigation:navigation-common", supportedPlatforms = ComposePlatforms.ALL_AOSP - ComposePlatforms.WINDOWS_NATIVE),
        ComposeComponent(":navigation:navigation-runtime", supportedPlatforms = ComposePlatforms.ALL_AOSP - ComposePlatforms.WINDOWS_NATIVE),
    ),
    "NAVIGATION_3" to listOf(
        ComposeComponent(":navigation3:navigation3-ui"),
    ),
    "NAVIGATION_EVENT" to listOf(
        ComposeComponent(":navigationevent:navigationevent-compose"),
    ),
    "SAVEDSTATE" to listOf(
        ComposeComponent(":savedstate:savedstate", supportedPlatforms = ComposePlatforms.ALL_AOSP),
        ComposeComponent(":savedstate:savedstate-compose", supportedPlatforms = ComposePlatforms.ALL),
    ),
    "WINDOW" to listOf(
        ComposeComponent(":window:window-core", supportedPlatforms = ComposePlatforms.ALL_AOSP - ComposePlatforms.WINDOWS_NATIVE),
    ),
)

val libraryToTasks = mapOf(
    "COMPOSE" to fun AbstractComposePublishingTask.() = publish(
        ":compose:desktop:desktop",
        onlyWithPlatforms = setOf(ComposePlatforms.Desktop),
        publications = listOf(
            "KotlinMultiplatform",
            "Jvm",
            "Jvmlinux-x64",
            "Jvmlinux-arm64",
            "Jvmmacos-x64",
            "Jvmmacos-arm64",
            "Jvmwindows-x64",
            "Jvmwindows-arm64"
        )
    )
)

val pathToComposeComponent = libraryToComponents.values.flatten().associateBy { it.path }
val Project.composeComponent get() = pathToComposeComponent[path]

tasks.register("publishComposeJb", ComposePublishingTask::class) {
    repository = "MavenRepository"

    libraries.forEach {
        libraryToComponents[it]?.forEach(::publishMultiplatform)
        libraryToTasks[it]?.invoke(this)
    }
}

tasks.register("publishComposeJbToMavenLocal", ComposePublishingTask::class) {
    repository = "MavenLocal"

    libraries.forEach {
        libraryToComponents[it]?.forEach(::publishMultiplatform)
        libraryToTasks[it]?.invoke(this)
    }
}

// isn't included in libraryToComponents for easy conflict resolution
// (it is changed in integration and should be removed in 1.8)
// TODO remove this and CI tasks after merging Jetpack Compose 1.8 to jb-main
val iconsComponents =
    emptyList<ComposeComponent>()

fun ComposePublishingTask.iconsPublications() {
    iconsComponents.forEach { publishMultiplatform(it) }
}

val libraries = project.findProperty("jetbrains.publication.libraries")
    ?.toString()?.split(",")
    ?: libraryToComponents.keys

// separate task that cannot be built in parallel (because it requires too much RAM).
// should be run with "--max-workers=1"
tasks.register("publishComposeJbExtendedIcons", ComposePublishingTask::class) {
    repository = "MavenRepository"
    iconsPublications()
}

tasks.register("publishComposeJbExtendedIconsToMavenLocal", ComposePublishingTask::class) {
    repository = "MavenLocal"
    iconsPublications()
}

// TODO deprecated, kept for CI compatibility, remove after Compose Multiplatform 1.8.0 is released
tasks.register("checkDesktop") {
    dependsOn(allTasksWith(name = "desktopTest"))
    dependsOn(":collection:collection:jvmTest")
    dependsOn(allTasksWith(name = "desktopApiCheck"))
}

tasks.register("testDesktop") {
    dependsOn(allTasksWith(name = "desktopTest"))
    dependsOn(":collection:collection:jvmTest")
}

tasks.register("testWeb") {
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

tasks.register("testUIKit") {
    val suffix = if (System.getProperty("os.arch") == "aarch64") "SimArm64Test" else "X64Test"
    val uikitTestSubtaskName = "uikit$suffix"

    dependsOn(":compose:runtime:runtime:$uikitTestSubtaskName")
    dependsOn(":compose:ui:ui-text:$uikitTestSubtaskName")
    dependsOn(":compose:ui:ui:$uikitTestSubtaskName")
    dependsOn(":compose:material3:material3:$uikitTestSubtaskName")
    dependsOn(":compose:foundation:foundation:$uikitTestSubtaskName")
    dependsOn(":collection:collection:$uikitTestSubtaskName")
}

tasks.register("testRuntimeNative") {
    dependsOn(":compose:runtime:runtime:macosX64Test")
}

tasks.register("testComposeModules") { // used in https://github.com/JetBrains/androidx/tree/jb-main/.github/workflows
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
    dependsOn(apiValidationTasks(suffix = "ApiDump"))
}

tasks.register("jbApiCheck") {
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
