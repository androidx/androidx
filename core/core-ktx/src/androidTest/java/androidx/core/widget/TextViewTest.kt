/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.widget

import android.widget.TextView
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@LargeTest
class TextViewTest {

    private val context = ApplicationProvider.getApplicationContext() as android.content.Context
    private val view = TextView(context)

    @UiThreadTest
    @Test fun doBeforeTextChanged() {
        view.text = "before"

        val called = AtomicBoolean()
        view.doBeforeTextChanged { text, start, count, after ->
            assertEquals("before", text.toString())
            assertEquals(0, start)
            assertEquals(6, count)
            assertEquals(4, after)
            called.set(true)
        }

        view.text = "text"

        assertTrue(called.get())
    }

    @UiThreadTest
    @Test fun doOnTextChanged() {
        view.text = "before"

        val called = AtomicBoolean()
        view.doOnTextChanged { text, start, before, count ->
            assertEquals("text", text.toString())
            assertEquals(0, start)
            assertEquals(6, before)
            assertEquals(4, count)
            called.set(true)
        }

        view.text = "text"

        assertTrue(called.get())
    }

    @UiThreadTest
    @Test fun doAfterTextChanged() {
        val called = AtomicBoolean()
        view.doAfterTextChanged { text ->
            assertEquals("text", text.toString())
            called.set(true)
        }

        view.text = "text"

        assertTrue(called.get())
    }
}
