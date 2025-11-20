/*
 * Copyright 2017 The Android Open Source Project
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

@file:JvmName("AndroidXConfig")

package androidx.build

import androidx.build.gradle.extraPropertyOrNull
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/** AndroidX configuration backed by Gradle properties. */
abstract class AndroidConfigImpl(private val project: Project) : AndroidConfig {
    override val buildToolsVersion: String = "36.0.0"

    override val compileSdk: Int by lazy {
        val sdkString = project.extraPropertyOrNull(COMPILE_SDK)?.toString()
        check(sdkString != null) { "$COMPILE_SDK is unset" }
        sdkString.toInt()
    }

    override val latestStableCompileSdk: Int by lazy {
        val sdkString = project.extraPropertyOrNull(LATEST_STABLE_COMPILE_SDK)?.toString()
        check(sdkString != null) { "$LATEST_STABLE_COMPILE_SDK is unset" }
        sdkString.toInt()
    }

    override val minSdk: Int = 23

    override val targetSdk: Int by lazy {
        project.providers.gradleProperty(TARGET_SDK_VERSION).get().toInt()
    }

    companion object {
        private const val COMPILE_SDK = "androidx.compileSdk"
        private const val LATEST_STABLE_COMPILE_SDK = "androidx.latestStableCompileSdk"
        private const val TARGET_SDK_VERSION = "androidx.targetSdkVersion"

        /**
         * Implementation detail. This should only be used by AndroidXGradleProperties for property
         * validation.
         */
        val GRADLE_PROPERTIES = listOf(COMPILE_SDK, LATEST_STABLE_COMPILE_SDK, TARGET_SDK_VERSION)
    }
}

/**
 * Configuration values for various aspects of the AndroidX plugin, including default values for
 * [com.android.build.api.dsl.CommonExtension].
 */
interface AndroidConfig {
    /** Build tools version used for AndroidX projects. */
    val buildToolsVersion: String

    /**
     * Default compile SDK version used for AndroidX projects.
     *
     * This may be specified in `gradle.properties` using `androidx.compileSdk`.
     */
    val compileSdk: Int

    /**
     * The latest stable compile SDK version that is available to use for AndroidX projects.
     *
     * This may be specified in `gradle.properties` using `androidx.latestStableCompileSdk`.
     */
    val latestStableCompileSdk: Int

    /** Default minimum SDK version used for AndroidX projects. */
    val minSdk: Int

    /**
     * Default target SDK version used for AndroidX projects.
     *
     * This may be specified in `gradle.properties` using `androidx.targetSdkVersion`.
     */
    val targetSdk: Int
}

/** Default configuration values for Android Gradle Plugin. */
val Project.defaultAndroidConfig: AndroidConfig
    get() =
        extensions.findByType(AndroidConfigImpl::class.java)
            ?: extensions.create("androidx.build.AndroidConfigImpl", AndroidConfigImpl::class.java)

fun Project.getGradlePrebuiltsPath(): File {
    return File(rootProject.projectDir, "../../tools/external/gradle").canonicalFile
}

fun Project.getExternalProjectPath(): File {
    return File(rootProject.projectDir, "../../external").canonicalFile
}

fun Project.getKeystore(): File {
    return File(project.getSupportRootFolder(), "development/keystore/debug.keystore")
}

fun Project.getPrebuiltsRoot(): File {
    return File(project.extraPropertyOrNull("prebuiltsRoot").toString())
}

/** @return the project's Android SDK stub JAR as a File. */
fun Project.getAndroidJar(sdkNum: Int = project.defaultAndroidConfig.compileSdk): FileCollection {
    val compileSdk = "android-${sdkNum}"
    return files(
        arrayOf(
            File(getSdkPath(), "platforms/$compileSdk/android.jar"),
            // Allow using optional android.car APIs
            File(getSdkPath(), "platforms/$compileSdk/optional/android.car.jar"),
            // Allow using optional android.test APIs
            File(getSdkPath(), "platforms/$compileSdk/optional/android.test.base.jar"),
            File(getSdkPath(), "platforms/$compileSdk/optional/android.test.mock.jar"),
            File(getSdkPath(), "platforms/$compileSdk/optional/android.test.runner.jar"),
        )
    )
}
