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

package androidx.testutils.paparazzi

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Environment
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import java.io.File

/**
 * Creates a [Paparazzi] test rule configured from system properties for AndroidX tests with the
 * `AndroidXPaparazziPlugin` Gradle plugin applied.
 *
 * Golden images used with this framework are expected to have a one-to-one relationship to test
 * functions. This helps ensure isolation between test functions and facilitates updating golden
 * images programmatically via the `:updateGolden` Gradle task or CI.
 *
 * To this end, golden images are named by the qualified name of their test function, instead of
 * a secondary identifier. Additionally, the returned [Paparazzi] instance will enforce a limit of
 * one snapshot per test function.
 */
fun androidxPaparazzi(
    deviceConfig: DeviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false),
    theme: String = "android:Theme.Material.NoActionBar.Fullscreen",
    renderingMode: RenderingMode = RenderingMode.SHRINK,
    imageDiffer: ImageDiffer = ImageDiffer.MSSIMMatcher
) = Paparazzi(
    deviceConfig = deviceConfig,
    theme = theme,
    renderingMode = renderingMode,
    environment = Environment(
        platformDir = systemProperty("platformDir"),
        resDir = systemProperty("resDir"),
        assetsDir = systemProperty("assetsDir"),
        compileSdkVersion = systemProperty("compileSdkVersion").toInt(),
        resourcePackageNames = systemProperty("resourcePackageNames").split(","),
        appTestDir = System.getProperty("user.dir")!!
    ),
    snapshotHandler = GoldenVerifier(
        modulePath = systemProperty("modulePath"),
        goldenRootDirectory = File(systemProperty("goldenRootDir")),
        reportDirectory = File(systemProperty("reportDir")),
        imageDiffer = imageDiffer
    )
)

/** Package name used for resolving system properties */
internal const val PACKAGE_NAME = "androidx.testutils.paparazzi"

/** Name of the internal Gradle plugin */
private const val PLUGIN_NAME = "AndroidXPaparazziPlugin"

/** Name of the module containing this library */
private const val MODULE_NAME = ":internal-testutils-paparazzi"

/** Read a system property with [PACKAGE_NAME] prefix, throwing an exception if missing */
private fun systemProperty(name: String) = checkNotNull(System.getProperty("$PACKAGE_NAME.$name")) {
    if (System.getProperty("$PACKAGE_NAME.gradlePluginApplied").toBoolean()) {
        "Missing required system property: $PACKAGE_NAME.$name. This is likely due to version " +
            "mismatch between $PLUGIN_NAME and $MODULE_NAME."
    } else {
        "Paparazzi system properties not set. Please ensure $PLUGIN_NAME is applied to your build."
    }
}
