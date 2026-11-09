/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget.screenshot

import android.content.Context
import android.content.res.Configuration
import androidx.glance.adaptive.appwidget.R
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that the screenshot tests' palette override in `src/androidTest/res` equals the library's
 * baseline palette, so a change to `values/` or `values-night/` fails here instead of leaving the
 * goldens on stale colors.
 *
 * Runs on API 30, where the library resolves its colors from the baseline `values/` and
 * `values-night/` resources rather than the dynamic colors in `values-v31/`.
 */
@Config(sdk = [30])
@RunWith(RobolectricTestRunner::class)
class ScreenshotPaletteTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun lightOverride_equalsTheBaselinePalette() {
        assertOverrideEqualsBaseline(
            override = "src/androidTest/res/values-v31/remoteviews_colors.xml",
            baseline = "src/main/res/values/remoteviews_colors.xml",
            nightMode = false,
        )
    }

    @Test
    fun darkOverride_equalsTheBaselinePalette() {
        assertOverrideEqualsBaseline(
            override = "src/androidTest/res/values-night-v31/remoteviews_colors.xml",
            baseline = "src/main/res/values-night/remoteviews_colors.xml",
            nightMode = true,
        )
    }

    private fun assertOverrideEqualsBaseline(
        override: String,
        baseline: String,
        nightMode: Boolean,
    ) {
        val overrideColors = readColors(override)
        assertWithMessage("Colors in $override")
            .that(overrideColors.keys)
            .containsExactlyElementsIn(readColors(baseline).keys)

        val resources = themedContext(nightMode).resources
        for ((name, value) in overrideColors) {
            val id = R.color::class.java.getField(name).getInt(null)
            assertWithMessage("$name in $override")
                .that(Integer.toHexString(value))
                .isEqualTo(Integer.toHexString(resources.getColor(id, null)))
        }
    }

    private fun themedContext(nightMode: Boolean): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.uiMode =
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (nightMode) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return context.createConfigurationContext(configuration)
    }

    /**
     * Returns the `<color>` entries of the resource file at [path], relative to the module
     * directory, by name. Values that are not `#AARRGGBB` literals map to 0, so they fail the
     * comparison with the resolved color.
     */
    private fun readColors(path: String): Map<String, Int> {
        val file = File(path)
        assertWithMessage("Expected $path under ${File("").absolutePath}")
            .that(file.isFile)
            .isTrue()
        val colors =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName("color")
        val result = mutableMapOf<String, Int>()
        for (i in 0 until colors.length) {
            val color = colors.item(i)
            val value = color.textContent.trim()
            result[color.attributes.getNamedItem("name").nodeValue] =
                if (value.startsWith("#")) value.substring(1).toLong(16).toInt() else 0
        }
        return result
    }
}
