/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.viewinterop

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emit
import androidx.ui.core.ContextAmbient
import androidx.ui.node.UiApplier

/**
 * Emit a view into the current composition.
 *
 * @sample androidx.ui.core.samples.EmitViewButtonSample
 *
 * @param ctor The constructor of the view with a single [Context] parameter.
 * @param update A function which will execute when the composition is applied, with the emitted
 * view instance passed in. This function is expected to be used to handle the "update" logic to
 * handle any changes.
 */
@Composable
fun <T : View> emitView(
    ctor: (Context) -> T,
    update: (T) -> Unit
) {
    val context = ContextAmbient.current
    emit<T, UiApplier>(
        ctor = { ctor(context) },
        update = {
            reconcile(update)
        }
    )
}

/**
 * Emit a ViewGroup into the current composition, with the emitted nodes of [children] as the
 * content.
 *
 * @sample androidx.ui.core.samples.EmitViewLinearLayoutSample
 *
 * @param ctor The constructor of the view with a single [Context] parameter.
 * @param update A function which will execute when the composition is applied, with the emitted
 * view instance passed in. This function is expected to be used to handle the "update" logic to
 * handle any changes.
 * @param children the composable content that will emit the "children" of this view.
 */
@Composable
fun <T : ViewGroup> emitView(
    ctor: (Context) -> T,
    update: (T) -> Unit,
    children: @Composable () -> Unit
) {
    val context = ContextAmbient.current
    emit<T, UiApplier>(
        ctor = { ctor(context) },
        update = {
            reconcile(update)
        },
        children = children
    )
}