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

package androidx.compose.foundation.text2.input.internal

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ComposeInputMethodManagerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun restartInput_startsNewInputConnection() {
        var calledCreateInputConnection: EditorInfo? = null
        var imm: ComposeInputMethodManager? = null
        var view: View? = null
        rule.setContent {
            AndroidView(factory = { context ->
                TestView(context) { editorInfo ->
                    calledCreateInputConnection = editorInfo
                    null
                }.also {
                    view = it
                    imm = ComposeInputMethodManager(it)
                }
            })
        }

        rule.runOnUiThread {
            view?.requestFocus()
            imm?.restartInput()
        }

        rule.runOnIdle {
            assertThat(calledCreateInputConnection).isNotNull()
        }
    }

    @Test
    fun everyRestartInput_createsNewInputConnection() {
        var createInputConnectionCalled = 0
        var imm: ComposeInputMethodManager? = null
        var view: View? = null
        rule.setContent {
            AndroidView(factory = { context ->
                TestView(context) {
                    createInputConnectionCalled++
                    null
                }.also {
                    view = it
                    imm = ComposeInputMethodManager(it)
                }
            })
        }

        rule.runOnUiThread {
            view?.requestFocus()
            imm?.restartInput()
        }

        rule.runOnIdle {
            // when first time we start input, checkFocus in platform code causes
            // onCreateInputConnection to be called twice.
            assertThat(createInputConnectionCalled).isEqualTo(2)
        }

        rule.runOnUiThread {
            imm?.restartInput()
        }

        rule.runOnIdle {
            assertThat(createInputConnectionCalled).isEqualTo(3)
        }
    }
}

private class TestView(
    context: Context,
    val createInputConnection: (EditorInfo?) -> InputConnection? = { null }
) : View(context) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isEnabled = true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        return createInputConnection(outAttrs)
    }

    override fun isInEditMode(): Boolean {
        return true
    }
}