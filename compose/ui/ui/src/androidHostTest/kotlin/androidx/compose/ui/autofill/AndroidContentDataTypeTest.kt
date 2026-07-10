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

package androidx.compose.ui.autofill

import android.view.View.AUTOFILL_TYPE_DATE
import android.view.View.AUTOFILL_TYPE_LIST
import android.view.View.AUTOFILL_TYPE_NONE
import android.view.View.AUTOFILL_TYPE_TEXT
import android.view.View.AUTOFILL_TYPE_TOGGLE
import androidx.compose.ui.autofill.ContentDataType.Companion.Date
import androidx.compose.ui.autofill.ContentDataType.Companion.List
import androidx.compose.ui.autofill.ContentDataType.Companion.None
import androidx.compose.ui.autofill.ContentDataType.Companion.Text
import androidx.compose.ui.autofill.ContentDataType.Companion.Toggle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class AndroidContentDataTypeTest {
    @Test
    fun assertTextTypeEquals() {
        assertThat(Text.dataType).isEqualTo(ContentDataType(AUTOFILL_TYPE_TEXT).dataType)
    }

    @Test
    fun assertListTypeEquals() {
        assertThat(List.dataType).isEqualTo(ContentDataType(AUTOFILL_TYPE_LIST).dataType)
    }

    @Test
    fun assertDateTypeEquals() {
        assertThat(Date.dataType).isEqualTo(ContentDataType(AUTOFILL_TYPE_DATE).dataType)
    }

    @Test
    fun assertToggleTypeEquals() {
        assertThat(Toggle.dataType).isEqualTo(ContentDataType(AUTOFILL_TYPE_TOGGLE).dataType)
    }

    @Test
    fun assertNoneTypeEquals() {
        assertThat(None.dataType).isEqualTo(ContentDataType(AUTOFILL_TYPE_NONE).dataType)
    }
}
