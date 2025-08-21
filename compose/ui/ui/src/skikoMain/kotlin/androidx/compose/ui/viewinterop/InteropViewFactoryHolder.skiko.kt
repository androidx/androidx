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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.ComposeNodeLifecycleCallback

// TODO: Rename the expect to "InteropViewHolder" (it's not a factory) and then
//       `androidx.compose.ui.viewinterop.InteropViewHolder` can be the actual.
internal actual open class InteropViewFactoryHolder : ComposeNodeLifecycleCallback {
    actual open fun getInteropView(): InteropView? {
        abstractInvocationError("fun getInteropView(): InteropView?")
    }

    actual override fun onDeactivate() {
        abstractInvocationError("fun onDeactivate(): Unit")
    }

    actual override fun onRelease() {
        abstractInvocationError("fun onRelease(): Unit")
    }

    actual override fun onReuse() {
        abstractInvocationError("fun onReuse(): Unit")
    }
}

private fun abstractInvocationError(name: String): Nothing {
    throw NotImplementedError("Abstract `$name` must be implemented by platform-specific subclass of `InteropViewHolder`")
}
