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
    private val testLong = 946684800000 // Jan 1, 2000 in milliseconds

    @Test
    fun textValue_whenValueIsText_returnsText() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.textValue).isEqualTo(testString)
    }

    @Test
    fun booleanValue_whenValueIsText_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.booleanValue).isNull()
    }

    @Test
    fun listIndexValue_whenValueIsText_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.listIndexValue).isNull()
    }

    @Test
    fun booleanValue_whenValueIsToggle_returnsBoolean() {
        // Arrange
        val autofillValue = AutofillValue.forToggle(testBoolean)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.booleanValue).isEqualTo(testBoolean)
    }

    @Test
    fun textValue_whenValueIsToggle_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forToggle(testBoolean)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.textValue).isNull()
    }

    @Test
    fun listIndexValue_whenValueIsToggle_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forToggle(testBoolean)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.listIndexValue).isNull()
    }

    @Test
    fun listIndexValue_whenValueIsList_returnsInt() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.listIndexValue).isEqualTo(testInt)
    }

    @Test
    fun textValue_whenValueIsList_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.textValue).isNull()
    }

    @Test
    fun booleanValue_whenValueIsList_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.booleanValue).isNull()
    }

    @Test
    fun getListIndexOrDefault_whenValueIsList_returnsInt() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.getListIndexOrDefault(defaultValue = -1)).isEqualTo(testInt)
    }

    @Test
    fun getListIndexOrDefault_whenValueIsText_returnsDefault() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))
        val defaultValue = -1

        // Act & Assert
        assertThat(fillableData.getListIndexOrDefault(defaultValue)).isEqualTo(defaultValue)
    }

    @Test
    fun dateMillisValue_whenValueIsList_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forList(testInt)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.dateMillisValue).isNull()
    }

    @Test
    fun textValue_whenValueIsDate_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forDate(testLong)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.textValue).isNull()
    }

    @Test
    fun booleanValue_whenValueIsDate_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forDate(testLong)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.booleanValue).isNull()
    }

    @Test
    fun listIndexValue_whenValueIsDate_returnsNull() {
        // Arrange
        val autofillValue = AutofillValue.forDate(testLong)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.listIndexValue).isNull()
    }

    @Test
    fun dateMillisValue_whenValueIsDate_returnsLong() {
        // Arrange
        val autofillValue = AutofillValue.forDate(testLong)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.dateMillisValue).isEqualTo(testLong)
    }

    @Test
    fun getDateMillisOrDefault_whenValueIsDate_returnsLong() {
        // Arrange
        val autofillValue = AutofillValue.forDate(testLong)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))

        // Act & Assert
        assertThat(fillableData.getDateMillisOrDefault(defaultValue = -1L)).isEqualTo(testLong)
    }

    @Test
    fun getDateMillisOrDefault_whenValueIsText_returnsDefault() {
        // Arrange
        val autofillValue = AutofillValue.forText(testString)
        val fillableData = checkNotNull(FillableData.createFrom(autofillValue))
        val defaultValue = -1L

        // Act & Assert
        assertThat(fillableData.getDateMillisOrDefault(defaultValue)).isEqualTo(defaultValue)
    }
}
