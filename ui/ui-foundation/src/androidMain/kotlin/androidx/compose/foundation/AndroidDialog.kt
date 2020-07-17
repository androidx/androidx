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

package androidx.compose.foundation

import android.app.Dialog
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.ExperimentalComposeApi
import androidx.compose.Recomposer
import androidx.compose.compositionReference
import androidx.compose.currentComposer
import androidx.compose.onActive
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.ui.core.Modifier
import androidx.ui.core.ViewAmbient
import androidx.ui.core.semantics.semantics
import androidx.ui.core.setContent
import androidx.compose.foundation.semantics.dialog

/**
 * Opens a dialog with the given content.
 *
 * The dialog is visible as long as it is part of the composition hierarchy.
 * In order to let the user dismiss the Dialog, the implementation of [onCloseRequest] should
 * contain a way to remove to remove the dialog from the composition hierarchy.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.samples.DialogSample
 *
 * @param onCloseRequest Executes when the user tries to dismiss the Dialog.
 * @param children The content to be displayed inside the dialog.
 */
@Composable
actual fun Dialog(onCloseRequest: () -> Unit, children: @Composable () -> Unit) {
    val view = ViewAmbient.current

    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    // The recomposer can't change.
    val dialog = remember(view) { DialogWrapper(view, recomposer) }
    dialog.onCloseRequest = onCloseRequest

    onActive {
        dialog.show()

        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    val composition = compositionReference()
    onCommit {
        dialog.setContent(composition) {
            // TODO(b/159900354): draw a scrim and add margins around the Compose Dialog, and
            //  consume clicks so they can't pass through to the underlying UI
            Box(Modifier.semantics { this.dialog() }, children = children)
        }
    }
}

private class DialogWrapper(
    composeView: View,
    private val recomposer: Recomposer
) : Dialog(composeView.context) {
    lateinit var onCloseRequest: () -> Unit

    private val frameLayout = FrameLayout(context)
    private var composition: Composition? = null

    init {
        window!!.requestFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(frameLayout)
        ViewTreeLifecycleOwner.set(frameLayout, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(frameLayout, ViewTreeViewModelStoreOwner.get(composeView))
    }

    // TODO(b/159900354): Make the Android Dialog full screen and the scrim fully transparent

    fun setContent(parentComposition: CompositionReference, children: @Composable () -> Unit) {
        // TODO: This should probably create a child composition of the original
        composition = frameLayout.setContent(recomposer, parentComposition, children)
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
