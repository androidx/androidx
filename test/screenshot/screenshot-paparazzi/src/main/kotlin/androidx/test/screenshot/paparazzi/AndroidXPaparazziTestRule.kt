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

package androidx.test.screenshot.paparazzi

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Environment
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import java.io.File

/**
 * Creates a [Paparazzi] test rule configured from system properties for AndroidX tests.
 */
fun AndroidXPaparazziTestRule(
    deviceConfig: DeviceConfig = DeviceConfig.NEXUS_5.copy(softButtons = false),
    theme: String = "android:Theme.Material.NoActionBar.Fullscreen",
    renderingMode: RenderingMode = RenderingMode.NORMAL,
    imageDiffer: ImageDiffer = ImageDiffer.PixelPerfect
) = Paparazzi(
    deviceConfig = deviceConfig,
    theme = theme,
    renderingMode = renderingMode,
    environment = Environment(
        platformDir = systemProperty("platformDir").toFile().path,
        resDir = systemProperty("resDir").toFile().path,
        assetsDir = systemProperty("assetsDir").toFile().path,
        packageName = systemProperty("packageName"),
        compileSdkVersion = systemProperty("compileSdkVersion").toInt(),
        platformDataDir = systemProperty("platformDataDir").toFile().path,
        resourcePackageNames = systemProperty("resourcePackageNames").split(","),
        appTestDir = System.getProperty("user.dir")!!
    ),
    snapshotHandler = GoldenVerifier(
        modulePath = systemProperty("modulePath"),
        goldenRootDirectory = systemProperty("goldenRootDir").toFile(),
        reportDirectory = systemProperty("reportDir").toFile(),
        imageDiffer = imageDiffer
    )
)

/** Package name used for resolving system properties */
private const val PACKAGE_NAME = "androidx.test.screenshot.paparazzi"

/** Read a system property with [PACKAGE_NAME] prefix, throwing an exception if missing */
private fun systemProperty(name: String) =
    requireNotNull(System.getProperty("$PACKAGE_NAME.$name")) {
        "System property $PACKAGE_NAME.$name is not set. You may need to apply " +
            "AndroidXPaparazziPlugin to your Gradle build."
    }

/** Little helper to convert string path to [File] to improve readability */
@Suppress("NOTHING_TO_INLINE")
private inline fun String.toFile() = File(this).canonicalFile