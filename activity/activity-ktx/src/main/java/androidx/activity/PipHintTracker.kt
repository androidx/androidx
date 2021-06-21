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

package androidx.activity

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

/**
 * Sets the [View] that will be used as reference to set the
 * [PictureInPictureParams.Builder.setSourceRectHint].
 *
 * Anytime the view position changes, [Activity.setPictureInPictureParams] will be called with
 * the updated view's position in window coordinates as the
 * [PictureInPictureParams.Builder.setSourceRectHint].
 *
 * @param view the view to use as the reference for the source rect hint
 */
@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.O)
public suspend fun Activity.trackPipAnimationHintView(view: View) {
    // Returns a rect of the window coordinates of a view.
    fun View.positionInWindow(): Rect {
        val position = Rect()
        getGlobalVisibleRect(position)
        return position
    }

    // Create a cold flow that will emit the most updated position of the view in the form of a
    // rect as long as the view is attached to the window.
    @Suppress("DEPRECATION")
    val flow = callbackFlow<Rect> {
        // Emit a new hint rect any time the view moves.
        val layoutChangeListener = View.OnLayoutChangeListener { v, l, t, r, b, oldLeft, oldTop,
            oldRight, oldBottom ->
            if (l != oldLeft || r != oldRight || t != oldTop || b != oldBottom) {
                offer(v.positionInWindow())
            }
        }
        val scrollChangeListener = ViewTreeObserver.OnScrollChangedListener {
            offer(view.positionInWindow())
        }
        // When the view is attached, emit the current position and start listening for layout
        // changes to track movement.
        val attachStateChangeListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                offer(view.positionInWindow())
                view.viewTreeObserver.addOnScrollChangedListener(scrollChangeListener)
                view.addOnLayoutChangeListener(layoutChangeListener)
            }

            override fun onViewDetachedFromWindow(v: View) {
                v.viewTreeObserver.removeOnScrollChangedListener(scrollChangeListener)
                v.removeOnLayoutChangeListener(layoutChangeListener)
            }
        }
        // Check if the view is already attached to the window, if it is then emit the current
        // position and start listening for layout changes to track movement.
        if (Api19Impl.isAttachedToWindow(view)) {
            offer(view.positionInWindow())
            view.viewTreeObserver.addOnScrollChangedListener(scrollChangeListener)
            view.addOnLayoutChangeListener(layoutChangeListener)
        }
        view.addOnAttachStateChangeListener(attachStateChangeListener)

        awaitClose {
            view.viewTreeObserver.removeOnScrollChangedListener(scrollChangeListener)
            view.removeOnLayoutChangeListener(layoutChangeListener)
            view.removeOnAttachStateChangeListener(attachStateChangeListener)
        }
    }
    flow.collect { hint ->
        Api26Impl.setPipParamsSourceRectHint(this, hint)
    }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
internal object Api19Impl {
    fun isAttachedToWindow(view: View): Boolean = view.isAttachedToWindow
}

@RequiresApi(Build.VERSION_CODES.O)
internal object Api26Impl {
    fun setPipParamsSourceRectHint(activity: Activity, hint: Rect) {
        activity.setPictureInPictureParams(
            PictureInPictureParams.Builder()
                .setSourceRectHint(hint)
                .build()
        )
    }
}
