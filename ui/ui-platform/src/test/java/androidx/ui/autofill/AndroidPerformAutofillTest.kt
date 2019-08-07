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
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.test.filters.SmallTest
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class AndroidPerformAutofillTest {
    private val autofillTree = AutofillTree()
    private lateinit var androidAutofill: AndroidAutofill

    @Before
    fun setup() {
        val autofillManager: AutofillManager = mock()

        val context: Context = mock()
        whenever(context.getSystemService(eq(AutofillManager::class.java)))
            .thenReturn(autofillManager)

        val view: View = mock()
        whenever(view.context).thenReturn(context)

        androidAutofill = AndroidAutofill(view, autofillTree)
    }

    @Test
    fun performAutofill_name() {
        // Arrange.
        val onFill: (String) -> Unit = mock()
        val autofillNode = AutofillNode(
            onFill = onFill,
            autofillTypes = listOf(AutofillType.Name),
            boundingBox = Rect(0, 0, 0, 0)
        )
        autofillTree += autofillNode

        val autofillValue: AutofillValue = mock()
        whenever(autofillValue.isText).thenReturn(true)
        whenever(autofillValue.textValue).thenReturn("First Name")

        val autofillValues = FakeSparseArray().apply { append(autofillNode.id, autofillValue) }

        // Act.
        androidAutofill.performAutofill(autofillValues)

        // Assert.
        verify(onFill, times(1)).invoke("First Name")
    }

    @Test
    fun performAutofill_email() {
        // Arrange.
        val onFill: (String) -> Unit = mock()
        val autofillNode = AutofillNode(
            onFill = onFill,
            autofillTypes = listOf(AutofillType.EmailAddress),
            boundingBox = Rect(0, 0, 0, 0)
        )
        autofillTree += autofillNode

        val autofillValue: AutofillValue = mock()
        whenever(autofillValue.isText).thenReturn(true)
        whenever(autofillValue.textValue).thenReturn("email@google.com")

        val autofillValues = FakeSparseArray().apply { append(autofillNode.id, autofillValue) }

        // Act.
        androidAutofill.performAutofill(autofillValues)

        // Assert.
        verify(onFill, times(1)).invoke("email@google.com")
    }
}