/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.Build
import android.view.autofill.AutofillValue
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class AndroidFillableDataTest {
    private val testString = "TEST_STRING"
    private val testBoolean = true
    private val testInt = 123

    @Test
    fun getCharSequence_whenValueIsText_returnsText() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getCharSequence()).isEqualTo(testString)
    }

    @Test
    fun getBool_whenValueIsText_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getBool()).isNull()
    }

    @Test
    fun getInt_whenValueIsText_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getInt()).isNull()
    }

    @Test
    fun getBool_whenValueIsToggle_returnsBoolean() {
        // Arrange
        val autofillValue = AutofillValue.forToggle(testBoolean)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getBool()).isEqualTo(testBoolean)
    }

    @Test
    fun getCharSequence_whenValueIsToggle_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forToggle(testBoolean)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getCharSequence()).isNull()
    }

    @Test
    fun getInt_whenValueIsToggle_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forToggle(testBoolean)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getInt()).isNull()
    }

    @Test
    fun getInt_whenValueIsList_returnsInt() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getInt()).isEqualTo(testInt)
    }

    @Test
    fun getCharSequence_whenValueIsList_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getCharSequence()).isNull()
    }

    @Test
    fun getBool_whenValueIsList_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData: FillableData = AndroidFillableData(autofillValue)

        // Act & Assert
        assertThat(fillableData.getBool()).isNull()
    }
}
