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

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialA2uiBasicCatalogV1Test {

    private val fakeUrlOpener = A2uiUrlOpener { _ -> }
    private val fakeMessageFormatter = A2uiMessageFormatter { _, _, _ -> "" }
    private val fakeLocaleProvider = A2uiLocaleProvider { Locale.US }

    @Test
    fun factory_withDefaults_createsCatalogWithMaterialTextAndBasicFunctions() {
        val catalog =
            MaterialA2uiBasicCatalogV1(
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
            )

        assertThat(catalog.id).isEqualTo(A2uiBasicCatalogV1.CatalogId)
        assertThat(catalog.themeSchema).isEqualTo(A2uiBasicCatalogV1.ThemeSchema)

        // Verifies the default Material text component is used
        assertThat(catalog.components["Text"])
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.text)

        // Verifies standard basic functions are successfully populated
        assertThat(catalog.functions["formatString"]).isNotNull()
        assertThat(catalog.functions["openUrl"]).isNotNull()
    }

    @Test
    fun factory_withCustomTextComponent_overridesDefaultMaterialText() {
        val customText =
            object : A2uiBasicCatalogV1.Text {
                @Composable
                override fun A2uiComponentScope.TypedContent(
                    text: String,
                    variant: A2uiBasicCatalogV1.Text.Variant,
                    modifier: Modifier,
                ) {}
            }

        val catalog =
            MaterialA2uiBasicCatalogV1(
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = fakeLocaleProvider,
                text = customText,
            )

        assertThat(catalog.components["Text"]).isSameInstanceAs(customText)
        assertThat(catalog.components["Text"])
            .isNotSameInstanceAs(MaterialA2uiBasicCatalogV1Defaults.text)
    }

    @Test
    fun materialA2uiBasicCatalogV1Defaults_text_isMaterialA2uiBasicCatalogV1Text() {
        assertThat(MaterialA2uiBasicCatalogV1Defaults.text)
            .isSameInstanceAs(MaterialA2uiBasicCatalogV1Text)
    }
}
