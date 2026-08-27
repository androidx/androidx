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

package androidx.compose.material3.a2ui.catalog

import android.os.Build
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.a2ui.icons.A2uiIcon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class MaterialA2uiBasicCatalogV1IconParameterizedTest(private val iconName: String) {

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.icon),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "icon={0}")
        fun data(): Collection<Array<Any>> = A2uiIcon.AllNames.map { arrayOf(it) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun eachIcon_rendersSuccessfully() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = testCatalog,
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Icon",
                            properties =
                                mapOf(
                                    "name" to iconName,
                                    "accessibility" to mapOf("label" to iconName),
                                ),
                        )
                    ),
            )
        val surface = controller.start()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    A2uiTestSurface(surface = surface)
                    Icon(
                        imageVector = requireNotNull(A2uiIcon.fromName(iconName)),
                        contentDescription = null,
                        modifier = Modifier.testTag("expected_icon"),
                    )
                }
            }
        }

        val actualBitmap = onNodeWithContentDescription(iconName).captureToImage().asAndroidBitmap()
        val expectedBitmap = onNodeWithTag("expected_icon").captureToImage().asAndroidBitmap()

        assertWithMessage("Icon width doesn't match")
            .that(actualBitmap.width)
            .isEqualTo(expectedBitmap.width)
        assertWithMessage("Icon height doesn't match")
            .that(actualBitmap.height)
            .isEqualTo(expectedBitmap.height)
        assertWithMessage("Rendered icon does not match expected icon for token: $iconName")
            .that(actualBitmap.sameAs(expectedBitmap))
            .isTrue()
    }
}
