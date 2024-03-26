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

val mainComponents =
    listOf(
        ComposeComponent(":annotation:annotation", supportedPlatforms = ComposePlatforms.ALL - ComposePlatforms.ANDROID),
        ComposeComponent(":collection:collection", supportedPlatforms = ComposePlatforms.ALL - ComposePlatforms.ANDROID),
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

        ComposeComponent(
            path = ":core:core-bundle",
            supportedPlatforms = ComposePlatforms.ALL_AOSP,
            neverRedirect = true
        ),
        ComposeComponent(":savedstate:savedstate", ComposePlatforms.ALL_AOSP),
        ComposeComponent(":lifecycle:lifecycle-viewmodel-savedstate", ComposePlatforms.ALL_AOSP),

        ComposeComponent(":navigation:navigation-common", ComposePlatforms.ALL_AOSP),
        ComposeComponent(":navigation:navigation-runtime", ComposePlatforms.ALL_AOSP),

        //To be added later: (also don't forget to add gradle.properties see in lifecycle-runtime for an example)
        ComposeComponent(":lifecycle:lifecycle-runtime-compose"),
        ComposeComponent(":lifecycle:lifecycle-viewmodel-compose"),
        ComposeComponent(":navigation:navigation-compose"),

        ComposeComponent(":compose:animation:animation"),
        ComposeComponent(":compose:animation:animation-core"),
        ComposeComponent(":compose:animation:animation-graphics"),
        ComposeComponent(":compose:foundation:foundation"),
        ComposeComponent(":compose:foundation:foundation-layout"),
        ComposeComponent(":compose:material:material"),
        ComposeComponent(":compose:material3:material3"),
        ComposeComponent(":compose:material:material-icons-core"),
        ComposeComponent(":compose:material:material-ripple"),
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
    )

val iconsComponents =
    listOf(
        ComposeComponent(":compose:material:material-icons-extended"),
    )

fun ComposePublishingTask.mainPublications() {
    publish(
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

    mainComponents.forEach { publishMultiplatform(it) }
}

fun ComposePublishingTask.iconsPublications() {
    iconsComponents.forEach { publishMultiplatform(it) }
}

// To show all projects which use `xxx` task, run:
// ./gradlew -p frameworks/support help --task xxx
tasks.register("publishComposeJb", ComposePublishingTask::class) {
    repository = "MavenRepository"
    mainPublications()
}

tasks.register("publishComposeJbToMavenLocal", ComposePublishingTask::class) {
    repository = "MavenLocal"
    mainPublications()
}

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
    dependsOn(allTasksWith(name = "desktopApiCheck"))
}

tasks.register("testWeb") {
    dependsOn(":compose:runtime:runtime:jsTest")
    // TODO: ideally we want to run all wasm tests that are possible but now we deal only with modules that have skikoTests

    // Unfortunately, the CI (TC) behaviour is not determined with these tests:
    // The agents become stuck, cancelled, etc.
    // Only one in 3 runs passes. It spoils the development of other Compose parts.
//    dependsOn(":compose:foundation:foundation:wasmJsBrowserTest")
//    dependsOn(":compose:material3:material3:wasmJsBrowserTest")
//    dependsOn(":compose:ui:ui-text:wasmJsBrowserTest")
//    dependsOn(":compose:ui:ui-text:wasmJsBrowserTest")
//    dependsOn(":compose:ui:ui:wasmJsBrowserTest")
}

tasks.register("testUIKit") {
    val subtaskName =
        if (System.getProperty("os.arch") == "aarch64") "uikitSimArm64Test" else "uikitX64Test"
    dependsOn(":compose:ui:ui-text:$subtaskName")
    dependsOn(":compose:ui:ui:$subtaskName")
// TODO fix it properly and uncomment https://youtrack.jetbrains.com/issue/COMPOSE-950/Reenable-material3-tests-on-iOS
//  Commented because it crashes on CI.
//    dependsOn(":compose:material3:material3:$subtaskName") 
    dependsOn(":compose:foundation:foundation:$subtaskName")
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
    stagingProfileName.set(mavenCentralGroup)
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
