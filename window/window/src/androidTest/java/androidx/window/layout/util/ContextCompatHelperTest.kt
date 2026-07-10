/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.layout.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.inputmethodservice.InputMethodService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.layout.util.ContextCompatHelper.unwrapContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

/** Instrumentation tests for [ContextCompatHelper]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class ContextCompatHelperTest {

    @Test
    fun testUnwrapContext_noContextWrapper_activity() {
        val context = mock(Activity::class.java)
        assertEquals(context, unwrapContext(context))
    }

    @Test
    fun testUnwrapContext_noContextWrapper_inputMethodService() {
        val context = mock(InputMethodService::class.java)
        assertEquals(context, unwrapContext(context))
    }

    @Test
    fun testUnwrapContext_noContextWrapper_application() {
        val context = mock(Application::class.java)
        assertEquals(context, unwrapContext(context))
    }

    @Test
    fun testUnwrapContext_contextWrapper_inputMethodService() {
        val context = mock(InputMethodService::class.java)
        val contextWrapper = ContextWrapper(context)
        assertEquals(context, unwrapContext(contextWrapper))
    }

    @Test
    fun testUnwrapContext_contextWrapper_activity_noBaseContext() {
        val context = mock(Activity::class.java)
        val contextWrapper = ContextWrapper(context)
        assertEquals(context, unwrapContext(contextWrapper))
    }

    @Test
    fun testUnwrapContext_contextWrapper_application_noBaseContext() {
        val context = mock(Application::class.java)
        val contextWrapper = ContextWrapper(context)
        assertEquals(context, unwrapContext(contextWrapper))
    }

    @Test
    fun testUnwrapContext_contextWrapper_activity_baseContext() {
        val context = mock(Activity::class.java)
        // Activity typically has a real context as a base
        val baseContext = mock(Context::class.java)
        whenever(context.baseContext).thenReturn(baseContext)
        val contextWrapper = ContextWrapper(context)
        // We should stop when we find the Activity context
        assertEquals(context, unwrapContext(contextWrapper))
    }

    @Test
    fun testUnwrapContext_contextWrapper_application_baseContext() {
        val context = mock(Application::class.java)
        // Application typically has a real context as a base
        val baseContext = mock(Context::class.java)
        whenever(context.baseContext).thenReturn(baseContext)
        val contextWrapper = ContextWrapper(context)
        // We should stop when we find the Application context
        assertEquals(context, unwrapContext(contextWrapper))
    }

    @Test
    fun testUnwrapContext_contextWrapper_null() {
        val contextWrapper = ContextWrapper(null)
        assertEquals(contextWrapper, unwrapContext(contextWrapper))
    }
}
