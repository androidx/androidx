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
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.view.SoftwareKeyboardControllerCompat
import org.jetbrains.annotations.TestOnly

/**
 * Compatibility interface for [InputMethodManager] to use in Compose text input systems.
 *
 * This interface is responsible for handling the calls made to platform InputMethodManager in
 * Android. There are different ways to show and hide software keyboard depending on API level.
 *
 * This interface also allows us to fake out the IMM for testing. For that reason, it should match
 * the relevant platform [InputMethodManager] APIs as closely as possible.
 */
internal interface ComposeInputMethodManager {
    fun restartInput()

    fun showSoftInput()

    fun hideSoftInput()

    fun updateExtractedText(
        token: Int,
        extractedText: ExtractedText
    )

    fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    )

    /**
     * Sends a [KeyEvent] originated from an InputMethod to the Window. This is a necessary
     * delegation when the InputConnection itself does not handle the received event.
     */
    fun sendKeyEvent(event: KeyEvent)
}

/**
 * Creates a new instance of [ComposeInputMethodManager].
 *
 * The value returned by this function can be changed for tests by calling
 * [overrideComposeInputMethodManagerFactoryForTests].
 */
internal fun ComposeInputMethodManager(view: View): ComposeInputMethodManager =
    ComposeInputMethodManagerFactory(view)

/** This lets us swap out the implementation in our own tests. */
private var ComposeInputMethodManagerFactory: (View) -> ComposeInputMethodManager = { view ->
    when {
        Build.VERSION.SDK_INT >= 24 -> ComposeInputMethodManagerImplApi24(view)
        else -> ComposeInputMethodManagerImplApi21(view)
    }
}

/**
 * Sets the factory used by [ComposeInputMethodManager] to create instances and returns the previous
 * factory.
 *
 * Any test that calls this should call it again to restore the factory after the test finishes, to
 * avoid breaking unrelated tests.
 */
@TestOnly
@VisibleForTesting
internal fun overrideComposeInputMethodManagerFactoryForTests(
    factory: (View) -> ComposeInputMethodManager
): (View) -> ComposeInputMethodManager {
    val oldFactory = ComposeInputMethodManagerFactory
    ComposeInputMethodManagerFactory = factory
    return oldFactory
}

private abstract class ComposeInputMethodManagerImpl(protected val view: View) :
    ComposeInputMethodManager {

    private var imm: InputMethodManager? = null

    private val softwareKeyboardControllerCompat =
        SoftwareKeyboardControllerCompat(view)

    override fun restartInput() {
        requireImm().restartInput(view)
    }

    override fun showSoftInput() {
        softwareKeyboardControllerCompat.show()
    }

    override fun hideSoftInput() {
        softwareKeyboardControllerCompat.hide()
    }

    override fun updateExtractedText(
        token: Int,
        extractedText: ExtractedText
    ) {
        requireImm().updateExtractedText(view, token, extractedText)
    }

    override fun updateSelection(
        selectionStart: Int,
        selectionEnd: Int,
        compositionStart: Int,
        compositionEnd: Int
    ) {
        requireImm().updateSelection(
            view,
            selectionStart,
            selectionEnd,
            compositionStart,
            compositionEnd
        )
    }

    protected fun requireImm(): InputMethodManager = imm ?: createImm().also { imm = it }

    private fun createImm() =
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}

private open class ComposeInputMethodManagerImplApi21(view: View) :
    ComposeInputMethodManagerImpl(view) {

    /**
     * Prior to API24, the safest way to delegate IME originated KeyEvents to the window was
     * through BaseInputConnection.
     */
    private var baseInputConnection: BaseInputConnection? = null

    override fun sendKeyEvent(event: KeyEvent) {
        val baseInputConnection = baseInputConnection
            ?: BaseInputConnection(view, false).also { baseInputConnection = it }
        baseInputConnection.sendKeyEvent(event)
    }
}

@RequiresApi(24)
private open class ComposeInputMethodManagerImplApi24(view: View) :
    ComposeInputMethodManagerImplApi21(view) {

    override fun sendKeyEvent(event: KeyEvent) {
        requireImm().dispatchKeyEventFromInputMethod(view, event)
    }
}
