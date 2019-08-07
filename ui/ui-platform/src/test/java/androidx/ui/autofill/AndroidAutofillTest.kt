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
import androidx.test.filters.SmallTest
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class AndroidAutofillTest {

    @get:Rule
    val expectedException = ExpectedException.none()!!

    private lateinit var androidAutofill: AndroidAutofill
    private lateinit var autofillManager: AutofillManager
    private lateinit var view: View
    private val autofillTree = AutofillTree()

    @Before
    fun setup() {
        autofillManager = mock()

        val context: Context = mock()
        whenever(context.getSystemService(eq(AutofillManager::class.java)))
            .thenReturn(autofillManager)

        view = mock()
        whenever(view.context).thenReturn(context)

        androidAutofill = AndroidAutofill(view, autofillTree)
    }

    @Test
    fun importantForAutofill_is_yes() {
        verify(view).setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES)
    }

    @Test
    fun requestAutofillForNode_calls_notifyViewEntered() {
        // Arrange.
        val autofillNode = AutofillNode(onFill = {}, boundingBox = Rect(0, 0, 0, 0))

        // Act.
        androidAutofill.requestAutofillForNode(autofillNode)

        // Assert.
        verify(autofillManager, times(1))
            .notifyViewEntered(view, autofillNode.id, autofillNode.boundingBox!!)
    }

    @Test
    fun requestAutofillForNode_beforeComposableIsPositioned_throwsError() {
        // Arrange - Before the composable is positioned, the boundingBox is null.
        val autofillNode = AutofillNode(onFill = {})

        // Assert.
        expectedException.expectMessage("requestAutofill called before onChildPositioned()")

        // Act.
        androidAutofill.requestAutofillForNode(autofillNode)
    }

    @Test
    fun cancelAutofillForNode_calls_notifyViewExited() {
        // Arrange.
        val autofillNode = AutofillNode(onFill = {})

        // Act.
        androidAutofill.cancelAutofillForNode(autofillNode)

        // Assert.
        verify(autofillManager, times(1)).notifyViewExited(view, autofillNode.id)
    }
}