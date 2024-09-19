/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UnstableApiUsage") // KotlinMultiplatformAndroidTarget

package androidx.build

import androidx.build.clang.AndroidXClang
import androidx.build.clang.MultiTargetNativeCompilation
import androidx.build.clang.NativeLibraryBundler
import androidx.build.clang.configureCinterop
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.build.api.dsl.KotlinMultiplatformAndroidTarget
import com.android.build.gradle.api.KotlinMultiplatformAndroidPlugin
import groovy.lang.Closure
import java.io.File
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * [AndroidXMultiplatformExtension] is an extension that wraps specific functionality of the Kotlin
 * multiplatform extension, and applies the Kotlin multiplatform plugin when it is used. The purpose
 * of wrapping is to prevent targets from being added when the platform has not been enabled. e.g.
 * the `macosX64` target is gated on a `project.enableMac` check.
 */
open class AndroidXMultiplatformExtension(val project: Project) {

    var enableBinaryCompatibilityValidator = true

    // Kotlin multiplatform plugin is only applied if at least one target / sourceset is added.
    private val kotlinExtensionDelegate = lazy {
        project.validateMultiplatformPluginHasNotBeenApplied()
        project.plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
        project.multiplatformExtension!!
    }
    private val kotlinExtension: KotlinMultiplatformExtension by kotlinExtensionDelegate
    private val agpKmpExtensionDelegate = lazy {
        // make sure to initialize the kotlin extension by accessing the property
        val extension = (kotlinExtension as ExtensionAware)
        project.plugins.apply(KotlinMultiplatformAndroidPlugin::class.java)
        extension.extensions.getByType(KotlinMultiplatformAndroidTarget::class.java)
    }

    val agpKmpExtension: KotlinMultiplatformAndroidTarget by agpKmpExtensionDelegate

    /**
     * The list of platforms that have been declared as supported in the build configuration.
     *
     * This may be a superset of the currently enabled platforms in [targetPlatforms].
     */
    val supportedPlatforms: MutableSet<PlatformIdentifier> = mutableSetOf()

    /**
     * The list of platforms that are currently enabled.
     *
     * This will vary across build environments. For example, a project's build configuration may
     * have requested `mac()` but this is not available when building on Linux.
     */
    val targetPlatforms: List<String>
        get() =
            if (kotlinExtensionDelegate.isInitialized()) {
                kotlinExtension.targets.mapNotNull {
                    if (it.targetName != "metadata") {
                        it.targetName
                    } else {
                        null
                    }
                }
            } else {
                throw GradleException("Kotlin multi-platform extension has not been initialized")
            }

    /**
     * Default platform identifier used for specifying POM dependencies.
     *
     * This platform will be added as a dependency to the multi-platform anchor artifact's POM
     * publication. For example, if the anchor artifact is `collection` and the default platform is
     * `jvm`, then the POM for `collection` will express a dependency on `collection-jvm`. This
     * ensures that developers who are silently upgrade to KMP artifacts but are not using Gradle
     * still see working artifacts.
     *
     * If no default was specified and a single platform is requested (ex. using [jvm]), returns the
     * identifier for that platform.
     */
    var defaultPlatform: String? = null
        get() = field ?: supportedPlatforms.singleOrNull()?.id
        set(value) {
            if (value != null) {
                if (supportedPlatforms.none { it.id == value }) {
                    throw GradleException(
                        "Platform $value has not been requested as a target. " +
                            "Available platforms are: " +
                            supportedPlatforms.joinToString(", ") { it.id }
                    )
                }
                if (targetPlatforms.none { it == value }) {
                    throw GradleException(
                        "Platform $value is not available in this build " +
                            "environment. Available platforms are: " +
                            targetPlatforms.joinToString(", ")
                    )
                }
            }
            field = value
        }

    val targets: NamedDomainObjectCollection<KotlinTarget>
        get() = kotlinExtension.targets

    /** Helper class to access Clang functionality. */
    private val clang = AndroidXClang(project)

    /** Helper class to bundle outputs of clang compilation into an AAR / JAR. */
    private val nativeLibraryBundler = NativeLibraryBundler(project)

    internal fun hasNativeTarget(): Boolean {
        // it is important to check initialized here not to trigger initialization
        return kotlinExtensionDelegate.isInitialized() &&
            targets.any { it.platformType == KotlinPlatformType.native }
    }

    internal fun hasAndroidMultiplatform(): Boolean {
        return agpKmpExtensionDelegate.isInitialized()
    }

    fun sourceSets(closure: Closure<*>) {
        if (kotlinExtensionDelegate.isInitialized()) {
            kotlinExtension.sourceSets.configure(closure).also {
                kotlinExtension.sourceSets.configureEach { sourceSet ->
                    if (sourceSet.name == "main" || sourceSet.name == "test") {
                        throw Exception(
                            "KMP-enabled projects must use target-prefixed " +
                                "source sets, e.g. androidMain or commonTest, rather than main or test"
                        )
                    }
                }
            }
        }
    }

    /**
     * Creates a multi-target native compilation with the given [archiveName].
     *
     * The given [configure] action can be used to add targets, sources, includes etc.
     *
     * The outputs of this compilation is not added to any artifact by default.
     * * To use the outputs via cinterop (kotlin native), use the [createCinterop] function.
     * * To bundle the outputs inside a JAR (to be loaded at runtime), use the
     *   [addNativeLibrariesToResources] function.
     * * To bundle the outputs inside an AAR (to be loaded at runtime), use the
     *   [addNativeLibrariesToJniLibs] function.
     *
     * @param archiveName The archive file name for the native artifacts (.so, .a or .o)
     * @param configure Action block to configure the compilation.
     */
    fun createNativeCompilation(
        archiveName: String,
        configure: Action<MultiTargetNativeCompilation>
    ): MultiTargetNativeCompilation {
        return clang.createNativeCompilation(archiveName = archiveName, configure = configure)
    }

    /**
     * Creates a Kotlin Native cinterop configuration for the given [nativeTarget] main compilation
     * from the outputs of [nativeCompilation].
     *
     * @param nativeTarget The kotlin native target for which a new cinterop will be added on the
     *   main compilation.
     * @param nativeCompilation The [MultiTargetNativeCompilation] which will be embedded into the
     *   generated cinterop klib.
     * @param cinteropName The name of the cinterop definition. A matching "<cinteropName.def>" file
     *   needs to be present in the default cinterop location
     *   (src/nativeInterop/cinterop/<cinteropName.def>).
     */
    @JvmOverloads
    fun createCinterop(
        nativeTarget: KotlinNativeTarget,
        nativeCompilation: MultiTargetNativeCompilation,
        cinteropName: String = nativeCompilation.archiveName
    ) {
        createCinterop(
            kotlinNativeCompilation =
                nativeTarget.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                    as KotlinNativeCompilation,
            nativeCompilation = nativeCompilation,
            cinteropName = cinteropName
        )
    }

    /**
     * Creates a Kotlin Native cinterop configuration for the given [kotlinNativeCompilation] from
     * the outputs of [nativeCompilation].
     *
     * @param kotlinNativeCompilation The kotlin native compilation for which a new cinterop will be
     *   added
     * @param nativeCompilation The [MultiTargetNativeCompilation] which will be embedded into the
     *   generated cinterop klib.
     * @param cinteropName The name of the cinterop definition. A matching "<cinteropName.def>" file
     *   needs to be present in the default cinterop location
     *   (src/nativeInterop/cinterop/<cinteropName.def>).
     */
    @JvmOverloads
    fun createCinterop(
        kotlinNativeCompilation: KotlinNativeCompilation,
        nativeCompilation: MultiTargetNativeCompilation,
        cinteropName: String = nativeCompilation.archiveName
    ) {
        nativeCompilation.configureCinterop(
            kotlinNativeCompilation = kotlinNativeCompilation,
            cinteropName = cinteropName
        )
    }

    /**
     * Creates a Kotlin Native cinterop configuration for the given [kotlinNativeCompilation] from
     * the single output of a configuration.
     *
     * @param kotlinNativeCompilation The kotlin native compilation for which a new cinterop will be
     *   added
     * @param configuration The configuration to resolve. It is expected for the configuration to
     *   contain a single file of the archive file to be referenced in the C interop definition
     *   file.
     */
    fun createCinteropFromArchiveConfiguration(
        kotlinNativeCompilation: KotlinNativeCompilation,
        configuration: Configuration
    ) {
        configureCinterop(project, kotlinNativeCompilation, configuration)
    }

    /** @see NativeLibraryBundler.addNativeLibrariesToJniLibs */
    @JvmOverloads
    fun addNativeLibrariesToJniLibs(
        androidTarget: KotlinAndroidTarget,
        nativeCompilation: MultiTargetNativeCompilation,
        forTest: Boolean = false
    ) =
        nativeLibraryBundler.addNativeLibrariesToJniLibs(
            androidTarget = androidTarget,
            nativeCompilation = nativeCompilation,
            forTest = forTest
        )

    /**
     * Convenience method to add bundle native libraries with a test jar.
     *
     * @see addNativeLibrariesToResources
     */
    fun addNativeLibrariesToTestResources(
        jvmTarget: KotlinJvmTarget,
        nativeCompilation: MultiTargetNativeCompilation
    ) =
        addNativeLibrariesToResources(
            jvmTarget = jvmTarget,
            nativeCompilation = nativeCompilation,
            compilationName = KotlinCompilation.TEST_COMPILATION_NAME
        )

    /** @see NativeLibraryBundler.addNativeLibrariesToResources */
    @JvmOverloads
    fun addNativeLibrariesToResources(
        jvmTarget: KotlinJvmTarget,
        nativeCompilation: MultiTargetNativeCompilation,
        compilationName: String = KotlinCompilation.MAIN_COMPILATION_NAME
    ) =
        nativeLibraryBundler.addNativeLibrariesToResources(
            jvmTarget = jvmTarget,
            nativeCompilation = nativeCompilation,
            compilationName = compilationName
        )

    /**
     * Sets the default target platform.
     *
     * The default target platform *must* be enabled in all build environments. For projects which
     * request multiple target platforms, this method *must* be called to explicitly specify a
     * default target platform.
     *
     * See [defaultPlatform] for details on how the value is used.
     */
    fun defaultPlatform(value: PlatformIdentifier) {
        defaultPlatform = value.id
    }

    @JvmOverloads
    fun jvm(block: Action<KotlinJvmTarget>? = null): KotlinJvmTarget? {
        supportedPlatforms.add(PlatformIdentifier.JVM)
        return if (project.enableJvm()) {
            kotlinExtension.jvm { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun jvmStubs(
        runTests: Boolean = false,
        block: Action<KotlinJvmTarget>? = null
    ): KotlinJvmTarget? {
        supportedPlatforms.add(PlatformIdentifier.JVM_STUBS)
        return if (project.enableJvm()) {
            kotlinExtension.jvm("jvmStubs") {
                block?.execute(this)
                project.tasks.named("jvmStubsTest").configure {
                    // don't try running common tests for stubs target if disabled
                    it.enabled = runTests
                }
            }
        } else {
            null
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @JvmOverloads
    fun android(block: Action<KotlinAndroidTarget>? = null): KotlinAndroidTarget? {
        supportedPlatforms.add(PlatformIdentifier.ANDROID)
        return if (project.enableJvm()) {
            kotlinExtension.androidTarget {
                // we need to allow instrumented test to depend on commonTest/jvmTest, which is not
                // default.
                // see https://youtrack.jetbrains.com/issue/KT-62594
                instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
                block?.execute(this)
            }
        } else {
            null
        }
    }

    @JvmOverloads
    fun androidNative(block: Action<KotlinNativeTarget>? = null): List<KotlinNativeTarget> {
        return listOfNotNull(
            androidNativeX86(block),
            androidNativeX64(block),
            androidNativeArm64(block),
            androidNativeArm32(block)
        )
    }

    @JvmOverloads
    fun androidNativeX86(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.ANDROID_NATIVE_X86)
        return if (project.enableAndroidNative()) {
            kotlinExtension.androidNativeX86 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun androidNativeX64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.ANDROID_NATIVE_X64)
        return if (project.enableAndroidNative()) {
            kotlinExtension.androidNativeX64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun androidNativeArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.ANDROID_NATIVE_ARM64)
        return if (project.enableAndroidNative()) {
            kotlinExtension.androidNativeArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun androidNativeArm32(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.ANDROID_NATIVE_ARM32)
        return if (project.enableAndroidNative()) {
            kotlinExtension.androidNativeArm32 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun androidLibrary(
        block: Action<KotlinMultiplatformAndroidTarget>? = null
    ): KotlinMultiplatformAndroidTarget? {
        supportedPlatforms.add(PlatformIdentifier.ANDROID)
        return if (project.enableJvm()) {
            agpKmpExtension.also { block?.execute(it) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun desktop(block: Action<KotlinJvmTarget>? = null): KotlinJvmTarget? {
        supportedPlatforms.add(PlatformIdentifier.DESKTOP)
        return if (project.enableDesktop()) {
            kotlinExtension.jvm("desktop") { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun mingwX64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTargetWithHostTests? {
        supportedPlatforms.add(PlatformIdentifier.MINGW_X_64)
        return if (project.enableWindows()) {
            kotlinExtension.mingwX64 { block?.execute(this) }
        } else {
            null
        }
    }

    /** Configures all mac targets supported by AndroidX. */
    @JvmOverloads
    fun mac(block: Action<KotlinNativeTarget>? = null): List<KotlinNativeTarget> {
        return listOfNotNull(macosX64(block), macosArm64(block))
    }

    @JvmOverloads
    fun macosX64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTargetWithHostTests? {
        supportedPlatforms.add(PlatformIdentifier.MAC_OSX_64)
        return if (project.enableMac()) {
            kotlinExtension.macosX64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun macosArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTargetWithHostTests? {
        supportedPlatforms.add(PlatformIdentifier.MAC_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.macosArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    /** Configures all ios targets supported by AndroidX. */
    @JvmOverloads
    fun ios(block: Action<KotlinNativeTarget>? = null): List<KotlinNativeTarget> {
        return listOfNotNull(iosX64(block), iosArm64(block), iosSimulatorArm64(block))
    }

    @JvmOverloads
    fun iosArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.IOS_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.iosArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun iosX64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.IOS_X_64)
        return if (project.enableMac()) {
            kotlinExtension.iosX64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun iosSimulatorArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.IOS_SIMULATOR_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.iosSimulatorArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    /** Configures all watchos targets supported by AndroidX. */
    @JvmOverloads
    fun watchos(block: Action<KotlinNativeTarget>? = null): List<KotlinNativeTarget> {
        return listOfNotNull(
            watchosX64(block),
            watchosArm32(block),
            watchosArm64(block),
            // TODO: enable this once all the libraries are ready to use it.
            // watchosDeviceArm64(block),
            watchosSimulatorArm64(block)
        )
    }

    @JvmOverloads
    fun watchosArm32(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.WATCHOS_ARM_32)
        return if (project.enableMac()) {
            kotlinExtension.watchosArm32 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun watchosArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.WATCHOS_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.watchosArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun watchosDeviceArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.WATCHOS_DEVICE_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.watchosDeviceArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun watchosX64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.WATCHOS_X_64)
        return if (project.enableMac()) {
            kotlinExtension.watchosX64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun watchosSimulatorArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.WATCHOS_SIMULATOR_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.watchosSimulatorArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    /** Configures all tvos targets supported by AndroidX. */
    @JvmOverloads
    fun tvos(block: Action<KotlinNativeTarget>? = null): List<KotlinNativeTarget> {
        return listOfNotNull(tvosX64(block), tvosArm64(block), tvosSimulatorArm64(block))
    }

    @JvmOverloads
    fun tvosArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.TVOS_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.tvosArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun tvosX64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.TVOS_X_64)
        return if (project.enableMac()) {
            kotlinExtension.tvosX64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun tvosSimulatorArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.TVOS_SIMULATOR_ARM_64)
        return if (project.enableMac()) {
            kotlinExtension.tvosSimulatorArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun linux(block: Action<KotlinNativeTarget>? = null): List<KotlinNativeTarget> {
        return listOfNotNull(
            linuxArm64(block),
            linuxX64(block),
        )
    }

    @JvmOverloads
    fun linuxArm64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.LINUX_ARM_64)
        return if (project.enableLinux()) {
            kotlinExtension.linuxArm64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun linuxX64(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        supportedPlatforms.add(PlatformIdentifier.LINUX_X_64)
        return if (project.enableLinux()) {
            kotlinExtension.linuxX64 { block?.execute(this) }
        } else {
            null
        }
    }

    @JvmOverloads
    fun linuxX64Stubs(block: Action<KotlinNativeTarget>? = null): KotlinNativeTarget? {
        // don't enable binary compatibility validator for stubs
        enableBinaryCompatibilityValidator = false
        supportedPlatforms.add(PlatformIdentifier.LINUX_X_64_STUBS)
        return if (project.enableLinux()) {
            kotlinExtension.linuxX64("linuxx64Stubs") {
                block?.execute(this)
                project.tasks.named("linuxx64StubsTest").configure {
                    // don't try running common tests for stubs target
                    it.enabled = false
                }
            }
        } else {
            null
        }
    }

    @JvmOverloads
    fun js(block: Action<KotlinJsTargetDsl>? = null): KotlinJsTargetDsl? {
        supportedPlatforms.add(PlatformIdentifier.JS)
        return if (project.enableJs()) {
            kotlinExtension.js { block?.execute(this) }
        } else {
            null
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @JvmOverloads
    fun wasmJs(block: Action<KotlinJsTargetDsl>? = null): KotlinWasmTargetDsl? {
        supportedPlatforms.add(PlatformIdentifier.WASM_JS)
        return if (project.enableWasmJs()) {
            kotlinExtension.wasmJs("wasmJs") {
                block?.execute(this)
                binaries.executable()
                browser {}
                project.configureWasm()
            }
        } else {
            null
        }
    }

    companion object {
        const val EXTENSION_NAME = "androidXMultiplatform"
    }
}

private fun Project.configureWasm() {
    rootProject.extensions.findByType<NodeJsRootExtension>()?.version = getVersionByName("node")
    rootProject.extensions.findByType(YarnRootExtension::class.java)?.let {
        it.version = getVersionByName("yarn")
        it.lockFileDirectory =
            File(project.getPrebuiltsRoot(), "androidx/external/wasm/yarn-offline-mirror")
        it.yarnLockMismatchReport = YarnLockMismatchReport.WARNING
    }

    val offlineMirrorStorage =
        File(getPrebuiltsRoot(), "androidx/external/wasm/yarn-offline-mirror")
    val createYarnRcFileTask =
        rootProject.tasks.register("createYarnRcFile", CreateYarnRcFileTask::class.java) {
            it.offlineMirrorStorage.set(offlineMirrorStorage)
            it.yarnrcFile.set(rootProject.layout.buildDirectory.file("js/.yarnrc"))
        }
    rootProject.tasks.withType<KotlinNpmInstallTask>().configureEach {
        it.dependsOn(createYarnRcFileTask)
        it.args.addAll(listOf("--ignore-engines", "--verbose"))

        println(
            """
             Yarn packages will be fetched from the offline mirror: ${offlineMirrorStorage.path}.
             If yarn has a dependency that is not there, your build will fail. To fix, re-run your
             Gradle task with -Pandroidx.yarnOfflineMode=false to download the dependencies from the
             internet into the offline mirror. Don't forget to upload the changes from that repo to
             Gerrit as well!   
            """
                .trimIndent()
                .replace("\n", " ")
        )

        if (project.useYarnOffline()) {
            it.args.add("--offline")
        }
    }

    // Use DSL API when https://youtrack.jetbrains.com/issue/KT-70029 is closed for all tasks below
    tasks.named("wasmJsDevelopmentExecutableCompileSync", DefaultIncrementalSyncTask::class.java) {
        it.destinationDirectory.set(
            file(layout.buildDirectory.dir("js/packages/wasm-js/dev/kotlin"))
        )
    }
    tasks.named("wasmJsProductionExecutableCompileSync", DefaultIncrementalSyncTask::class.java) {
        it.destinationDirectory.set(
            file(layout.buildDirectory.dir("js/packages/wasm-js/prod/kotlin"))
        )
    }
    tasks.named(
        "wasmJsTestTestDevelopmentExecutableCompileSync",
        DefaultIncrementalSyncTask::class.java
    ) {
        it.destinationDirectory.set(
            file(layout.buildDirectory.dir("js/packages/wasm-js-test/dev/kotlin"))
        )
    }
    tasks.named(
        "wasmJsTestTestProductionExecutableCompileSync",
        DefaultIncrementalSyncTask::class.java
    ) {
        it.destinationDirectory.set(
            file(layout.buildDirectory.dir("js/packages/wasm-js-test/prod/kotlin"))
        )
    }
}

fun Project.validatePublishedMultiplatformHasDefault() {
    val extension = project.extensions.getByType(AndroidXMultiplatformExtension::class.java)
    if (extension.defaultPlatform == null && extension.supportedPlatforms.isNotEmpty()) {
        throw GradleException(
            "Project is published and multiple platforms are requested. You " +
                "must explicitly specify androidXMultiplatform.defaultPlatform as one of: " +
                extension.targetPlatforms.joinToString(", ") {
                    "PlatformIdentifier.${PlatformIdentifier.fromId(it)!!.name}"
                }
        )
    }
}

/**
 * Ensures that multiplatform sources are suffixed with their target platform, ex. `MyClass.jvm.kt`.
 *
 * Must be called in afterEvaluate().
 */
fun Project.registerValidateMultiplatformSourceSetNamingTask() {
    val targets = multiplatformExtension?.targets?.filterNot { target -> target.name == "metadata" }
    if (targets == null || targets.size <= 1) {
        // We only care about multiplatform projects with more than one target platform.
        return
    }

    tasks
        .register(
            "validateMultiplatformSourceSetNaming",
            ValidateMultiplatformSourceSetNaming::class.java
        ) { task ->
            targets
                .filterNot { target -> target.platformType.name == "common" }
                .forEach { target -> task.addTarget(project, target) }
            task.rootDir.set(rootDir.path)
            task.cacheEvenIfNoOutputs()
        }
        .also { validateTask ->
            // Multiplatform projects with no enabled platforms do not actually apply the Kotlin
            // plugin
            // and therefore do not have the check task. They are skipped unless a platform is
            // enabled.
            if (project.tasks.findByName("check") != null) {
                project.addToCheckTask(validateTask)
                project.addToBuildOnServer(validateTask)
            }
        }
}

@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class ValidateMultiplatformSourceSetNaming : DefaultTask() {

    @get:Input abstract val rootDir: Property<String>

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getInputFiles(): Collection<FileCollection> = sourceSetMap.values

    private val sourceSetMap: MutableMap<String, FileCollection> = mutableMapOf()

    @set:Option(
        option = "autoFix",
        description = "Whether to automatically rename files instead of throwing an exception",
    )
    @get:Input
    var autoFix: Boolean = false

    @TaskAction
    fun validate() {
        // Files or entire source sets may duplicated shared across compilations, but it's more
        // expensive to de-dupe them than to check the suffixes for everything multiple times.
        for ((sourceFileSuffix, kotlinSourceSet) in sourceSetMap) {
            for (fileOrDir in kotlinSourceSet) {
                for (file in fileOrDir.walk()) {
                    // Kotlin source files must be uniquely-named across platforms.
                    if (
                        file.isFile &&
                            file.name.endsWith(".kt") &&
                            !file.name.endsWith(".$sourceFileSuffix.kt")
                    ) {
                        val actualPath = file.toRelativeString(File(rootDir.get()))
                        val expectedName = "${file.name.substringBefore('.')}.$sourceFileSuffix.kt"
                        if (autoFix) {
                            val destFile = File(file.parentFile, expectedName)
                            file.renameTo(destFile)
                            logger.info("Applied fix: $actualPath -> $expectedName")
                        } else {
                            throw GradleException(
                                "Source files for non-common platforms must be suffixed with " +
                                    "their target platform. Found '$actualPath' but expected " +
                                    "'$expectedName'."
                            )
                        }
                    }
                }
            }
        }
    }

    fun addTarget(project: Project, target: KotlinTarget) {
        sourceSetMap[target.preferredSourceFileSuffix] =
            project.files(
                target.compilations
                    .filterNot { compilation ->
                        // Don't enforce suffixes for test source sets.
                        compilation.name == "test" || compilation.name.endsWith("Test")
                    }
                    .flatMap { compilation -> compilation.kotlinSourceSets }
                    .map { kotlinSourceSet -> kotlinSourceSet.kotlin.sourceDirectories }
                    .toTypedArray()
            )
    }

    /**
     * List of Kotlin target names which may be used as source file suffixes. Any target whose name
     * does not appear in this list will use its [KotlinPlatformType] name.
     */
    private val allowedTargetNameSuffixes =
        setOf("android", "desktop", "jvm", "commonStubs", "jvmStubs", "linuxx64Stubs", "wasmJs")

    /** The preferred source file suffix for the target's platform type. */
    private val KotlinTarget.preferredSourceFileSuffix: String
        get() =
            if (allowedTargetNameSuffixes.contains(name)) {
                name
            } else {
                platformType.name
            }
}

/**
 * Set of targets are there to serve as stubs, but are not expected to be consumed by library
 * consumers.
 */
internal val setOfStubTargets = setOf("commonStubs", "jvmStubs", "linuxx64Stubs")
