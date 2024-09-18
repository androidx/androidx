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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DigitalCredentialEntryTest {
    companion object {
        val DISPLAY_DATA =
            VerificationEntryDisplayData(
                title = "test-title",
                subtitle = null,
                icon = Bitmap.createBitmap(4, 4, Bitmap.Config.ALPHA_8)
            )
    }

    class TestFormatDigitalCredentialEntry(val subProperty: String) :
        DigitalCredentialEntry(id = "test-id", entryDisplayData = setOf(DISPLAY_DATA))

    @Test
    fun subclassConstruction_success() {
        val entry = TestFormatDigitalCredentialEntry("subProperty")

        assertThat(entry.id).isEqualTo("test-id")
        assertThat(entry.entryDisplayData).containsExactly(DISPLAY_DATA)
        assertThat(entry.subProperty).isEqualTo("subProperty")
    }
}
