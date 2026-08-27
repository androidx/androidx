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

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class MaterialA2uiBasicCatalogV1ImageFitTest(private val fitTestParam: FitTestParam) {

    var capturedUrl: String? = null
    var capturedContentScale: ContentScale? = null

    private val fakeImageRenderer = A2uiImageRenderer { url, _, contentScale, _, _ ->
        capturedUrl = url
        capturedContentScale = contentScale
    }

    private val testCatalog =
        A2uiCatalog(
            catalogId = "test_catalog",
            components = listOf(MaterialA2uiBasicCatalogV1Defaults.image(fakeImageRenderer)),
            functions = listOf(A2uiFormatStringFunction.INSTANCE),
        )

    @Test
    fun fitProperty_passedToRendererAsContentScale() = runComposeUiTest {
        val payload =
            A2uiComponentPayload(
                id = "root",
                type = "Image",
                properties =
                    mapOf("url" to "https://test.com/img.png", "fit" to fitTestParam.fitToken),
            )
        val controller =
            A2uiTestController(catalog = testCatalog, initialComponents = listOf(payload))
        val surface = controller.start()

        setContent { MaterialTheme { A2uiTestSurface(surface) } }

        waitForIdle()

        assertThat(capturedUrl).isEqualTo("https://test.com/img.png")
        assertThat(capturedContentScale).isEqualTo(fitTestParam.expectedContentScale)
    }

    enum class FitTestParam(val fitToken: String, val expectedContentScale: ContentScale) {
        Cover("cover", ContentScale.Crop),
        Contain("contain", ContentScale.Fit),
        Fill("fill", ContentScale.FillBounds),
        None("none", ContentScale.None),
        ScaleDown("scaleDown", ContentScale.Inside),
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<FitTestParam> {
            return listOf(
                FitTestParam.Cover,
                FitTestParam.Contain,
                FitTestParam.Fill,
                FitTestParam.None,
                FitTestParam.ScaleDown,
            )
        }
    }
}
