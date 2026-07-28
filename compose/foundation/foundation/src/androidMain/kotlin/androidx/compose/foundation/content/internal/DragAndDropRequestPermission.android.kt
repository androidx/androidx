/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.content.internal

import android.app.Activity
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireView
import androidx.core.view.DragAndDropPermissionsCompat

internal actual fun DelegatableNode.dragAndDropRequestPermission(event: DragAndDropEvent) {
    if (Build.VERSION.SDK_INT < 24) return
    if (!node.isAttached) return

    val dragEvent = event.toAndroidDragEvent()
    // If there is no clip data or content URI, there's no need to request permissions
    val clipData = dragEvent.clipData ?: return
    if (!clipData.containsContentUri()) return

    val view = requireView()
    val activity = tryGetActivity(view) ?: return
    DragAndDropPermissionsCompat.request(activity, dragEvent)
}

private fun ClipData.containsContentUri(): Boolean {
    for (i in 0 until itemCount) {
        val uri = getItemAt(i)?.uri
        if (uri != null && ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
            return true
        }
    }
    return false
}

/**
 * Attempts to find the activity for the given view by unwrapping the view's context. This is a
 * "best effort" approach that's not guaranteed to get the activity, since a view's context is not
 * necessarily an activity.
 *
 * @param view The target view.
 * @return The activity if found; null otherwise.
 */
private fun tryGetActivity(view: View): Activity? {
    var context = view.context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
