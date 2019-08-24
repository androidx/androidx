/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import android.app.Activity
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import androidx.ui.autofill.AndroidAutofill
import androidx.ui.autofill.AutofillNode
import androidx.ui.autofill.AutofillType
import androidx.ui.test.android.fake.FakeViewStructure
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AndroidCraneViewTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<Activity>(Activity::class.java)

    private val PACKAGE_NAME = "androidx.ui.platform.test"
    private lateinit var craneView: AndroidCraneView

    @Before
    fun setup() {
        craneView = AndroidCraneView(activityTestRule.activity)
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun autofillAmbient_belowApi26_isNull() {
        assertThat(craneView.autofill).isNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofillAmbient_isNotNull() {
        assertThat(craneView.autofill).isNotNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofillAmbient_returnsAnInstanceOfAndroidAutofill() {
        assertThat(craneView.autofill).isInstanceOf(AndroidAutofill::class.java)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun onProvideAutofillVirtualStructure_populatesViewStructure() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()
        val autofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.Name),
            boundingBox = Rect(0, 0, 0, 0)
        )
        craneView.autofillTree += autofillNode

        // Act.
        craneView.onProvideAutofillVirtualStructure(viewStructure, 0)

        // Assert.
        assertThat(viewStructure).isEqualTo(FakeViewStructure().apply {
            children.add(FakeViewStructure().apply {
                virtualId = autofillNode.id
                packageName = PACKAGE_NAME
                setAutofillType(View.AUTOFILL_TYPE_TEXT)
                setAutofillHints(arrayOf(View.AUTOFILL_HINT_NAME))
                setDimens(0, 0, 0, 0, 0, 0)
            })
        })
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofill_triggersOnFill() {
        // Arrange.
        val expectedValue = "Name"
        var autofilledValue = ""
        val autofillNode = AutofillNode(
            onFill = { autofilledValue = it },
            autofillTypes = listOf(AutofillType.Name),
            boundingBox = Rect(0, 0, 0, 0)
        )
        val autofillValues = SparseArray<AutofillValue>().apply {
            append(autofillNode.id, AutofillValue.forText(expectedValue))
        }
        craneView.autofillTree += autofillNode

        // Act.
        craneView.autofill(autofillValues)

        // Assert.
        assertThat(autofilledValue).isEqualTo(expectedValue)
    }
}