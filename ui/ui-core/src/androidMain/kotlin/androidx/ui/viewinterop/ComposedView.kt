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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
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

/**
 * Composes an Android [View].
 *
 * @param view The [View] to compose.
 */
@Composable
// TODO(popam): support modifiers here
fun AndroidView(view: View) {
    AndroidViewHolder(view = view)
}

// Open to be mockable in tests.
internal open class AndroidViewHolder(context: Context) : ViewGroup(context) {
    var view: View? = null
        set(value) {
            if (value !== field) {
                field = value
                removeAllViews()
                if (value != null) {
                    addView(value)
                }
            }
        }

    var postInflationCallback: (View) -> Unit = {}

    var resId: Int? = null
        set(value) {
            if (value != field) {
                field = value
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(resId!!, this, false)
                this.view = view
                postInflationCallback(view)
            }
        }

    var onRequestDisallowInterceptTouchEvent: ((Boolean) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        view?.measure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(view?.measuredWidth ?: 0, view?.measuredHeight ?: 0)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        view?.layout(l, t, r, b)
    }

    override fun getLayoutParams(): LayoutParams? {
        return view?.layoutParams ?: LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        onRequestDisallowInterceptTouchEvent?.invoke(disallowIntercept)
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        view = null
    }
}
