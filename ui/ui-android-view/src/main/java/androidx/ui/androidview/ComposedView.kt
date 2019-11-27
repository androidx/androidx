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

package androidx.ui.androidview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.compose.Composable

/**
 * Composes an Android [View] given a layout resource [resId]. The method handles the inflation
 * of the [View] and will call the [postInflationCallback] after this happens. Note that the
 * callback will always be invoked on the main thread.
 *
 * @param resId The id of the layout resource to be inflated.
 * @param postInflationCallback The callback to be invoked after the layout is inflated.
 */
@Composable
// TODO(popam): support modifiers here
fun AndroidView(@LayoutRes resId: Int, postInflationCallback: (View) -> Unit = { _ -> }) {
    AndroidViewHolder(
        postInflationCallback = postInflationCallback,
        resId = resId
    )
}

private class AndroidViewHolder(context: Context) : FrameLayout(context) {
    var view: View? = null
        set(value) {
            if (value != field) {
                field = value
                removeAllViews()
                addView(view)
            }
        }

    var postInflationCallback: (View) -> Unit = {}

    var resId: Int? = null
        set(value) {
            if (value != field) {
                field = value
                val inflater = LayoutInflater.from(context)
                // TODO(popam): handle layout params
                val view = inflater.inflate(resId!!, null)
                this.view = view
                postInflationCallback(view)
            }
        }
}
