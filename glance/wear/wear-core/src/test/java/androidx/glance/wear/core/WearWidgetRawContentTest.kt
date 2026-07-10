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

package androidx.glance.wear.core

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class WearWidgetRawContentTest {

    @Test
    fun toParcel_fromParcel_matchesOriginalContent_withTitle() {
        val testTitle = "Test Title"
        val testKey = "key"
        val testValue = "value"
        val originalContent =
            WearWidgetRawContent(
                rcDocument = byteArrayOf(1, 2, 3),
                extras = Bundle().apply { putString(testKey, testValue) },
                widgetTitle = testTitle,
            )

        val parcel = originalContent.toParcel()
        val restoredContent = WearWidgetRawContent.fromParcel(parcel)

        assertThat(restoredContent.rcDocument).isEqualTo(originalContent.rcDocument)
        assertThat(restoredContent.extras.getString(testKey)).isEqualTo(testValue)
        assertThat(restoredContent.widgetTitle).isEqualTo(testTitle)
    }

    @Test
    fun toParcel_fromParcel_matchesOriginalContent_withoutTitle() {
        val originalContent =
            WearWidgetRawContent(
                rcDocument = byteArrayOf(4, 5, 6),
                extras = Bundle.EMPTY,
                widgetTitle = null,
            )

        val parcel = originalContent.toParcel()
        val restoredContent = WearWidgetRawContent.fromParcel(parcel)

        assertThat(restoredContent.rcDocument).isEqualTo(originalContent.rcDocument)
        assertThat(restoredContent.widgetTitle).isNull()
    }
}
