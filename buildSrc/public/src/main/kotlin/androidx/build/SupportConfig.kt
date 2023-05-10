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

package androidx.build

import androidx.build.SupportConfig.COMPILE_SDK_VERSION
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

object SupportConfig {
    const val DEFAULT_MIN_SDK_VERSION = 14
    const val INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
    private const val INTERNAL_BUILD_TOOLS_VERSION = "34.0.0-rc3"
    private const val PUBLIC_BUILD_TOOLS_VERSION = "34.0.0-rc4"
    const val NDK_VERSION = "23.1.7779620"

    /**
     * The Android SDK version to use for compilation.
     * <p>
     * Either an integer value or a pre-release platform code, prefixed with "android-" (ex.
     * "android-28" or "android-Q") as you would see within the SDK's platforms directory.
     */
    const val COMPILE_SDK_VERSION = "android-33-ext5"

    /**
     * The Android SDK version to use for targetSdkVersion meta-data.
     * <p>
     * Either an integer value (ex. 28), a pre-release platform code (ex. "Q") as you would see
     * within the SDK's platforms directory as android-<version>, or a released platform version
     * code as you would see within Build.VERSIONS.VERSION_CODE (ex. "HONEYCOMB" or "P").
     * <p>
     * <strong>Note:</strong> This must be set to an integer value or released platform version in
     * order for tests to run on devices running released versions of the Android OS. If this is
     * set to a pre-release version, tests will only be able to run on pre-release devices.
     */
    const val TARGET_SDK_VERSION = 33

    /**
     * Returns the build tools version that should be used for the project.
     *
     * Note that the value might be different between the internal and external (github) builds.
     */
    @JvmStatic
    fun buildToolsVersion(project: Project): String {
        return if (ProjectLayoutType.isPlayground(project)) {
            PUBLIC_BUILD_TOOLS_VERSION
        } else {
            INTERNAL_BUILD_TOOLS_VERSION
        }
    }
}

fun Project.getExternalProjectPath(): File {
    return File(rootProject.projectDir, "../../external").canonicalFile
}

fun Project.getKeystore(): File {
    return File(project.getSupportRootFolder(), "development/keystore/debug.keystore")
}

fun Project.getPrebuiltsRoot(): File {
    return File(project.rootProject.property("prebuiltsRoot").toString())
}

/**
 * @return the project's Android SDK stub JAR as a File.
 */
fun Project.getAndroidJar(): FileCollection =
    files(
        arrayOf(
            File(
                getSdkPath(),
                "platforms/$COMPILE_SDK_VERSION/android.jar"
            ),
            // Allow using optional android.car APIs
            File(
                getSdkPath(),
                "platforms/$COMPILE_SDK_VERSION/optional/android.car.jar"
            ),
            // Allow using optional android.test APIs
            File(getSdkPath(), "platforms/$COMPILE_SDK_VERSION/optional/android.test.base.jar"),
            File(getSdkPath(), "platforms/$COMPILE_SDK_VERSION/optional/android.test.mock.jar"),
            File(getSdkPath(), "platforms/$COMPILE_SDK_VERSION/optional/android.test.runner.jar")
        )
    )
