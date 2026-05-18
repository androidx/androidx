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

package androidx.pdf.autofill

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AutofillUtilsTest {

    @Test
    fun getVirtualFormWidgetId_packsCorrectly() {
        val pageNum = 10
        val widgetIndex = 20
        val virtualId = getVirtualFormWidgetId(pageNum, widgetIndex)

        // pageNum 10 is 0x000A
        // widgetIndex 20 is 0x0014
        // Expected virtualId: 0x000A0014 = 655380
        assertThat(virtualId).isEqualTo(0x000A0014)
    }

    @Test
    fun getPageNumber_extractsCorrectly() {
        val virtualId = 0x000A0014
        assertThat(getPageNumber(virtualId)).isEqualTo(10)
    }

    @Test
    fun getWidgetIndex_extractsCorrectly() {
        val virtualId = 0x000A0014
        assertThat(getWidgetIndex(virtualId)).isEqualTo(20)
    }

    @Test
    fun getVirtualFormWidgetId_withZero_packsCorrectly() {
        val virtualId = getVirtualFormWidgetId(0, 0)

        assertThat(getPageNumber(virtualId)).isEqualTo(0)
        assertThat(getWidgetIndex(virtualId)).isEqualTo(0)
    }

    @Test
    fun getVirtualFormWidgetId_withMaxValues_packsCorrectly() {
        val pageNum = 0xFFFF
        val widgetIndex = 0xFFFF
        val virtualId = getVirtualFormWidgetId(pageNum, widgetIndex)

        assertThat(getPageNumber(virtualId)).isEqualTo(pageNum)
        assertThat(getWidgetIndex(virtualId)).isEqualTo(widgetIndex)
    }
}
