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

import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/** AndroidX configuration backed by Gradle properties. */
abstract class AndroidConfigImpl(private val project: Project) : AndroidConfig {
    override val buildToolsVersion: String = "34.0.0"

    override val compileSdk: String by lazy { project.findProperty(COMPILE_SDK_VERSION).toString() }

    override val minSdk: Int = 14
    override val ndkVersion: String = "23.1.7779620"

    override val targetSdk: Int by lazy {
        project.findProperty(TARGET_SDK_VERSION).toString().toInt()
    }

    companion object {
        private const val COMPILE_SDK_VERSION = "androidx.compileSdkVersion"
        private const val TARGET_SDK_VERSION = "androidx.targetSdkVersion"

        /**
         * Implementation detail. This should only be used by AndroidXGradleProperties for property
         * validation.
         */
        val GRADLE_PROPERTIES =
            listOf(
                COMPILE_SDK_VERSION,
                TARGET_SDK_VERSION,
            )
    }
}

/**
 * Configuration values for various aspects of the AndroidX plugin, including default values for
 * [com.android.build.gradle.BaseExtension].
 */
interface AndroidConfig {
    /** Build tools version used for AndroidX projects. */
    val buildToolsVersion: String

    /**
     * Default compile SDK version used for AndroidX projects.
     *
     * This may be specified in `gradle.properties` using `androidx.compileSdkVersion`.
     */
    val compileSdk: String

    /** Default minimum SDK version used for AndroidX projects. */
    val minSdk: Int

    /** NDK version used for AndroidX projects. */
    val ndkVersion: String

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

fun Project.getExternalProjectPath(): File {
    return File(rootProject.projectDir, "../../external").canonicalFile
}

fun Project.getKeystore(): File {
    return File(project.getSupportRootFolder(), "development/keystore/debug.keystore")
}

fun Project.getPrebuiltsRoot(): File {
    return File(project.rootProject.property("prebuiltsRoot").toString())
}

/** @return the project's Android SDK stub JAR as a File. */
fun Project.getAndroidJar(): FileCollection {
    val compileSdk = project.defaultAndroidConfig.compileSdk
    return files(
        arrayOf(
            File(getSdkPath(), "platforms/$compileSdk/android.jar"),
            // Allow using optional android.car APIs
            File(getSdkPath(), "platforms/$compileSdk/optional/android.car.jar"),
            // Allow using optional android.test APIs
            File(getSdkPath(), "platforms/$compileSdk/optional/android.test.base.jar"),
            File(getSdkPath(), "platforms/$compileSdk/optional/android.test.mock.jar"),
            File(getSdkPath(), "platforms/$compileSdk/optional/android.test.runner.jar")
        )
    )
}
