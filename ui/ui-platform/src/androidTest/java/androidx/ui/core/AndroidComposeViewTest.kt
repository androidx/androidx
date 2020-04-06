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

import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.autofill.AndroidAutofill
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillNode
import androidx.ui.autofill.AutofillTree
import androidx.ui.autofill.AutofillType
import androidx.ui.test.android.fake.FakeViewStructure
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class AndroidComposeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val PACKAGE_NAME = "androidx.ui.platform.test"
    private var autofill: Autofill? = null
    private lateinit var autofillTree: AutofillTree
    private lateinit var ownerView: ViewGroup

    @Before
    fun setup() {
        composeTestRule.setContent {
            @Suppress("DEPRECATION")
            ownerView = OwnerAmbient.current as ViewGroup
            autofill = AutofillAmbient.current
            autofillTree = AutofillTreeAmbient.current
        }
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun autofillAmbient_belowApi26_isNull() {
        assertThat(autofill).isNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofillAmbient_isNotNull() {
        assertThat(autofill).isNotNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofillAmbient_returnsAnInstanceOfAndroidAutofill() {
        assertThat(autofill).isInstanceOf(AndroidAutofill::class.java)
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
        autofillTree += autofillNode

        // Act.
        ownerView.onProvideAutofillVirtualStructure(viewStructure, 0)

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
        autofillTree += autofillNode

        // Act.
        ownerView.autofill(autofillValues)

        // Assert.
        assertThat(autofilledValue).isEqualTo(expectedValue)
    }
}