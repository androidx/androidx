import androidx.build.jetbrains.ArtifactRedirecting
import androidx.build.jetbrains.artifactRedirecting
import org.jetbrains.compose.internal.publishing.*

plugins {
    signing
}

buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/internal")
        maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
    }
    dependencies {
        val buildHelpersVersion = System.getProperty("BUILD_HELPERS_VERSION") ?: "0.1.16"
        classpath("org.jetbrains.compose.internal.build-helpers:publishing:$buildHelpersVersion")
    }
}

open class ComposePublishingTask : AbstractComposePublishingTask() {
    override fun dependsOnComposeTask(task: String) {
        dependsOn(task)
    }
}

val composeProperties = ComposeProperties(project)

// TODO: Align with other modules
val viewModelPlatforms = ComposePlatforms.ALL_AOSP - ComposePlatforms.WINDOWS_NATIVE

val libraryToComponents = mapOf(
    "CORE_BUNDLE" to listOf(
        ComposeComponent(
            path = ":core:core-bundle",
            supportedPlatforms = ComposePlatforms.ALL_AOSP,
            neverRedirect = true
        ),
    ),
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
        ComposeComponent(":compose:material3:material3"),
        ComposeComponent(":compose:material3:material3-common"),
        ComposeComponent(":compose:material:material-icons-core"),
        ComposeComponent(":compose:material:material-ripple"),
        ComposeComponent(":compose:material3:material3-window-size-class"),
        ComposeComponent(":compose:runtime:runtime", supportedPlatforms = ComposePlatforms.ALL),
        ComposeComponent(":compose:runtime:runtime-saveable", supportedPlatforms = ComposePlatforms.ALL),
        ComposeComponent(":compose:ui:ui"),
        ComposeComponent(":compose:ui:ui-geometry"),
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
        ComposeComponent(
            ":compose:ui:ui-tooling-preview",
            supportedPlatforms = ComposePlatforms.JVM_BASED
        ),
        ComposeComponent(
            ":compose:ui:ui-uikit",
            supportedPlatforms = ComposePlatforms.UI_KIT
        ),
        ComposeComponent(":compose:ui:ui-unit"),
        ComposeComponent(":compose:ui:ui-util"),
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
            supportedPlatforms = viewModelPlatforms
        ),
        ComposeComponent(":lifecycle:lifecycle-viewmodel-savedstate", viewModelPlatforms),
        ComposeComponent(":lifecycle:lifecycle-runtime-compose", supportedPlatforms = ComposePlatforms.ALL),
        ComposeComponent(":lifecycle:lifecycle-viewmodel-compose"),
    ),
    "NAVIGATION" to listOf(
        ComposeComponent(":navigation:navigation-compose"),
        ComposeComponent(":navigation:navigation-common", viewModelPlatforms),
        ComposeComponent(":navigation:navigation-runtime", viewModelPlatforms),
    ),
    "SAVEDSTATE" to listOf(
        ComposeComponent(":savedstate:savedstate", viewModelPlatforms),
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
            "Jvmwindows-x64"
        )
    )
)

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
val iconsComponents =
    listOf(
        ComposeComponent(":compose:material:material-icons-extended"),
    )

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

tasks.register("checkDesktop") {
    dependsOn(allTasksWith(name = "desktopTest"))
    dependsOn(":collection:collection:jvmTest")
    dependsOn(allTasksWith(name = "desktopApiCheck"))
}

tasks.register("testWeb") {
    dependsOn(":compose:runtime:runtime:jsTest")
    dependsOn(":compose:runtime:runtime:wasmJsTest")
    dependsOn(":compose:ui:ui:compileTestKotlinJs")
    // TODO: ideally we want to run all wasm tests that are possible but now we deal only with modules that have skikoTests

    dependsOn(":compose:foundation:foundation:wasmJsBrowserTest")
    dependsOn(":compose:material3:material3:wasmJsBrowserTest")
    dependsOn(":compose:ui:ui-text:wasmJsBrowserTest")
    dependsOn(":compose:ui:ui:wasmJsBrowserTest")
    dependsOn(":collection:collection:wasmJsBrowserTest")
}

tasks.register("testUIKit") {
    val suffix = if (System.getProperty("os.arch") == "aarch64") "SimArm64Test" else "X64Test"
    val uikitTestSubtaskName = "uikit$suffix"
    val instrumentedTestSubtaskName = "uikitInstrumented$suffix"

    dependsOn(":compose:ui:ui-text:$uikitTestSubtaskName")
    dependsOn(":compose:ui:ui:$uikitTestSubtaskName")
    dependsOn(":compose:ui:ui:$instrumentedTestSubtaskName")
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

val mavenCentral = MavenCentralProperties(project)
val mavenCentralGroup = project.providers.gradleProperty("maven.central.group")
val mavenCentralStage = project.providers.gradleProperty("maven.central.stage")
if (mavenCentral.signArtifacts) {
    signing.useInMemoryPgpKeys(
        mavenCentral.signArtifactsKey.get(),
        mavenCentral.signArtifactsPassword.get()
    )
}

val publishingDir = project.layout.buildDirectory.dir("publishing")
val originalArtifactsRoot = publishingDir.map { it.dir("original") }
val preparedArtifactsRoot = publishingDir.map { it.dir("prepared") }
val modulesFile = publishingDir.map { it.file("modules.txt") }

val findComposeModules by tasks.registering(FindModulesInSpaceTask::class) {
    requestedGroupId.set(mavenCentralGroup)
    requestedVersion.set(mavenCentral.version)
    spaceInstanceUrl.set("https://public.jetbrains.space")
    spaceClientId.set(System.getenv("COMPOSE_REPO_USERNAME") ?: "")
    spaceClientSecret.set(System.getenv("COMPOSE_REPO_KEY") ?: "")
    spaceProjectId.set(System.getenv("COMPOSE_DEV_REPO_PROJECT_ID") ?: "")
    spaceRepoId.set(System.getenv("COMPOSE_DEV_REPO_REPO_ID") ?: "")
    modulesTxtFile.set(modulesFile)
}

val downloadArtifactsFromComposeDev by tasks.registering(DownloadFromSpaceMavenRepoTask::class) {
    dependsOn(findComposeModules)
    modulesToDownload.set(project.provider {
        readComposeModules(
            modulesFile,
            originalArtifactsRoot
        )
    })
    spaceRepoUrl.set("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val fixModulesBeforePublishing by tasks.registering(FixModulesBeforePublishingTask::class) {
    dependsOn(downloadArtifactsFromComposeDev)
    inputRepoDir.set(originalArtifactsRoot)
    outputRepoDir.set(preparedArtifactsRoot)
}

val reuploadArtifactsToMavenCentral by tasks.registering(UploadToSonatypeTask::class) {
    dependsOn(fixModulesBeforePublishing)

    version.set(mavenCentral.version)
    modulesToUpload.set(project.provider { readComposeModules(modulesFile, preparedArtifactsRoot) })

    sonatypeServer.set("https://oss.sonatype.org")
    user.set(mavenCentral.user)
    password.set(mavenCentral.password)
    autoCommitOnSuccess.set(mavenCentral.autoCommitOnSuccess)
    stagingProfileName.set(mavenCentralStage)
}

fun readComposeModules(
    modulesFile: Provider<out FileSystemLocation>,
    repoRoot: Provider<out FileSystemLocation>
): List<ModuleToUpload> =
    modulesFile.get().asFile.readLines()
        .filter { it.isNotBlank() }
        .map { line ->
            val (group, artifact, version) = line.split(":")
            ModuleToUpload(
                groupId = group,
                artifactId = artifact,
                version = version,
                localDir = repoRoot.get().asFile.resolve("$group/$artifact/$version")
            )
        }

fun allTasksWith(name: String) =
    rootProject.subprojects.flatMap { it.tasks.filter { it.name == name } }


// ./gradlew printAllArtifactRedirectingVersions -PfilterProjectPath=lifecycle
// or just ./gradlew printAllArtifactRedirectingVersions
val printAllArtifactRedirectingVersions = tasks.register("printAllArtifactRedirectingVersions") {
    val filter = project.properties["filterProjectPath"] as? String ?: ""
    doLast {
        val map = libraryToComponents.values.flatten().filter { it.path.contains(filter) }
            .joinToString("\n\n", prefix = "\n") {
            val p = rootProject.findProject(it.path)!!
            it.path + " --> \n" + p.artifactRedirecting().prettyText()
        }

        println(map)
    }
}

fun ArtifactRedirecting.prettyText(): String {
    val allLines = arrayOf(
        "redirectGroupId = ${this.groupId}",
        "redirectDefaultVersion = ${this.defaultVersion}",
        "redirectForTargets = [${this.targetNames.joinToString().takeIf { it.isNotBlank() } ?: "android"}]",
        "redirectTargetVersions = ${this.targetVersions}"
    )

    return allLines.joinToString("") { " ".repeat(3) + "$it\n" }
}
