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

package androidx.ui.autofill

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillManager
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class AndroidPopulateViewStructureTest {
    private val autofillTree = AutofillTree()
    private lateinit var androidAutofill: AndroidAutofill

    @Before
    fun setup() {
        val autofillManager: AutofillManager = mock()

        val context: Context = mock()
        whenever(context.getSystemService(eq(AutofillManager::class.java)))
            .thenReturn(autofillManager)
        whenever(context.packageName).thenReturn("com.google.testpackage")

        val view: View = mock()
        whenever(view.context).thenReturn(context)

        androidAutofill = AndroidAutofill(view, autofillTree)
    }

    @Test
    fun populateViewStructure_emptyAutofillTree() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()

        // Act.
        androidAutofill.populateViewStructure(viewStructure)

        // Assert.
        assertThat(viewStructure.childCount).isEqualTo(0)
    }

    @Test
    fun populateViewStructure_oneChild() {
        // Arrange.
        val autofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.Name),
            boundingBox = Rect(0, 0, 0, 0)
        )
        autofillTree += autofillNode

        // Act.
        val viewStructure = FakeViewStructure()
        androidAutofill.populateViewStructure(viewStructure)

        // Assert.
        assertThat(viewStructure).isEqualTo(FakeViewStructure().apply {
            children.add(FakeViewStructure().apply {
                virtualId = autofillNode.id
                packageName = "com.google.testpackage"
                setAutofillType(View.AUTOFILL_TYPE_TEXT)
                setAutofillHints(arrayOf(View.AUTOFILL_HINT_NAME))
                setDimens(0, 0, 0, 0, 0, 0)
            })
        })
    }

    @Test
    fun populateViewStructure_twoChildren() {
        // Arrange.
        val nameAutofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.Name),
            boundingBox = Rect(0, 0, 0, 0)
        )
        autofillTree += nameAutofillNode

        val emailAutofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.EmailAddress),
            boundingBox = Rect(0, 0, 0, 0)
        )
        autofillTree += emailAutofillNode

        // Act.
        val viewStructure: ViewStructure = FakeViewStructure()
        androidAutofill.populateViewStructure(viewStructure)

        // Assert.
        assertThat(viewStructure).isEqualTo(FakeViewStructure().apply {
            children.add(FakeViewStructure().apply {
                virtualId = nameAutofillNode.id
                packageName = "com.google.testpackage"
                setAutofillType(View.AUTOFILL_TYPE_TEXT)
                setAutofillHints(arrayOf(View.AUTOFILL_HINT_NAME))
                setDimens(0, 0, 0, 0, 0, 0)
            })
            children.add(FakeViewStructure().apply {
                virtualId = emailAutofillNode.id
                packageName = "com.google.testpackage"
                setAutofillType(View.AUTOFILL_TYPE_TEXT)
                setAutofillHints(arrayOf(View.AUTOFILL_HINT_EMAIL_ADDRESS))
                setDimens(0, 0, 0, 0, 0, 0)
            })
        })
    }
}