/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.view.annotation

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import com.google.android.material.R as MaterialR

/**
 * Returns this [Context] if its theme already resolves Material 3 attributes, or wraps it with
 * `Theme.Material3.DayNight` as a fallback (e.g., when hosted in a Compose-only `ComponentActivity`
 * that uses a framework theme).
 */
internal fun Context.withMaterial3ThemeFallback(): Context {
    val typedValue = TypedValue()
    val hasMaterial3Theme =
        theme.resolveAttribute(
            /* resid = */ MaterialR.attr.colorSurfaceContainer,
            /* outValue = */ typedValue,
            /* resolveRefs = */ true,
        )
    return if (hasMaterial3Theme) {
        this
    } else {
        ContextThemeWrapper(this, MaterialR.style.Theme_Material3_DayNight)
    }
}

/** Helper function to check if a touch event is within the bounds of a given view. */
internal fun View.isTouchInView(event: MotionEvent): Boolean {
    val viewRect = Rect()
    getHitRect(viewRect)
    return viewRect.contains(event.x.toInt(), event.y.toInt())
}

/**
 * Finds the highest ancestor [View] in the hierarchy that currently has a pending layout request
 * ([View.isLayoutRequested] == `true`).
 *
 * When hosted inside Compose `AndroidView`, layout requests bubble up to the interop holder view
 * (`AndroidViewHolder`), while its parent (`AndroidViewsHandler`) clears its own layout flag.
 */
internal fun View.findHighestLayoutRequestedAncestor(): View {
    var rootDirtyView: View = this
    while (true) {
        val parentView = rootDirtyView.parent as? View ?: break
        if (!parentView.isLayoutRequested) break
        rootDirtyView = parentView
    }
    return rootDirtyView
}

/**
 * Propagates a layout request past any interop container (such as Compose's `AndroidViewsHandler`)
 * that intercepts [View.requestLayout] without calling `super.requestLayout()`.
 *
 * Starts from the parent of [findHighestLayoutRequestedAncestor] (where standard propagation
 * stopped) and requests layout up to the root `ViewRootImpl` so a traversal frame is scheduled.
 */
internal fun View.propagateRequestLayoutToRoot() {
    var parentView = findHighestLayoutRequestedAncestor().parent
    while (parentView != null) {
        if (!parentView.isLayoutRequested) {
            parentView.requestLayout()
        }
        parentView = parentView.parent
    }
}
