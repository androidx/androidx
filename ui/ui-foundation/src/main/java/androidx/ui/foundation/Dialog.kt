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

package androidx.ui.foundation

import android.app.Dialog
import android.content.Context
import android.view.MotionEvent
import android.view.Window
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.Recomposer
import androidx.compose.currentComposer
import androidx.compose.onActive
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.ContextAmbient
import androidx.ui.core.setContent
import androidx.ui.foundation.semantics.dialog
import androidx.ui.semantics.Semantics

/**
 * Opens a dialog with the given content.
 *
 * The dialog is visible as long as it is part of the composition hierarchy.
 * In order to let the user dismiss the Dialog, the implementation of [onCloseRequest] should
 * contain a way to remove to remove the dialog from the composition hierarchy.
 *
 * Example usage:
 *
 * @sample androidx.ui.foundation.samples.DialogSample
 *
 * @param onCloseRequest Executes when the user tries to dismiss the Dialog.
 * @param children The content to be displayed inside the dialog.
 */
@Composable
fun Dialog(onCloseRequest: () -> Unit, children: @Composable () -> Unit) {
    val context = ContextAmbient.current

    val recomposer = currentComposer.recomposer
    // The recomposer can't change.
    val dialog = remember(context) { DialogWrapper(context, recomposer) }
    dialog.onCloseRequest = onCloseRequest

    onActive {
        dialog.show()

        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    onCommit {
        dialog.setContent {
            Semantics(container = true, properties = { this.dialog = true }, children = children)
        }
    }
}

private class DialogWrapper(
    context: Context,
    private val recomposer: Recomposer
) : Dialog(context) {
    lateinit var onCloseRequest: () -> Unit

    private val frameLayout = FrameLayout(context)
    private var composition: Composition? = null

    init {
        window!!.requestFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(frameLayout)
    }

    fun setContent(children: @Composable () -> Unit) {
        // TODO: This should probably create a child composition of the original
        composition = frameLayout.setContent(recomposer, children)
    }

    fun disposeComposition() {
        composition?.dispose()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (result) {
            onCloseRequest()
        }

        return result
    }

    override fun cancel() {
        // Prevents the dialog from dismissing itself
        return
    }

    override fun onBackPressed() {
        onCloseRequest()
    }
}
