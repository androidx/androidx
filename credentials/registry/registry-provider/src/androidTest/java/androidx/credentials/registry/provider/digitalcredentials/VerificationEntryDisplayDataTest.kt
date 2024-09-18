/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.provider.digitalcredentials

import android.graphics.Bitmap
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry.Companion.DISPLAY_TYPE_VERIFICATION
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class VerificationEntryDisplayDataTest {
    @Test
    fun construction_success() {
        val icon = Bitmap.createBitmap(4, 4, Bitmap.Config.ALPHA_8)
        val data =
            VerificationEntryDisplayProperties(
                title = "test-title",
                subtitle = "subtitle",
                icon = icon,
                explainer = "explainer",
                warning = "warning"
            )

        assertThat(data.displayType).isEqualTo(DISPLAY_TYPE_VERIFICATION)
        assertThat(data.title).isEqualTo("test-title")
        assertThat(data.subtitle).isEqualTo("subtitle")
        assertThat(data.icon).isEqualTo(icon)
        assertThat(data.explainer).isEqualTo("explainer")
        assertThat(data.warning).isEqualTo("warning")
    }
}
