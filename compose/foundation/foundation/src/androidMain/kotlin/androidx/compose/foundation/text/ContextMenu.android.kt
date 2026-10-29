/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit,
) {
    CommonContextMenuArea(manager, content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    CommonContextMenuArea(selectionState, enabled, content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(manager: SelectionManager, content: @Composable () -> Unit) {
    CommonContextMenuArea(manager, content)
}

internal fun TextContextMenuBuilderScope.textItem(
    resources: Resources,
    item: TextContextMenuItems,
    enabled: Boolean,
    onClick: TextContextMenuSession.() -> Unit,
) {
    if (enabled) {
        item(
            key = item.key,
            label = resources.getString(item.stringId.value),
            leadingIcon = item.drawableId.value,
            onClick = onClick,
        )
    }
}

/**
 * Returns whether the "Look Up" context menu item should be shown for [selectedText] on the current
 * device.
 *
 * The item is shown on Android 17 (API 37) and above when [selectedText] is not blank and an
 * activity can resolve the [LookUpText.ACTION_LOOK_UP_TEXT] intent.
 *
 * @param context the [Context] used to resolve the Look Up activity via [Context.getPackageManager]
 * @param selectedText the currently selected text to look up
 */
internal fun canShowLookUp(context: Context, selectedText: String): Boolean {
    if (Build.VERSION.SDK_INT < 37) return false
    if (selectedText.isBlank()) return false
    val intent = createLookUpTextIntent(selectedText)
    return LookUpText.canResolveLookUpActivity(context, intent)
}

/**
 * Starts the activity that handles the [LookUpText.ACTION_LOOK_UP_TEXT] intent for [selectedText]
 * if [selectedText] is not blank.
 *
 * @param context the [Context] used to launch the Look Up activity via [Context.startActivity].
 *   Must be an Activity [Context] (such as the one provided by
 *   [androidx.compose.ui.platform.LocalContext] in a Compose hierarchy) since the intent does not
 *   set [Intent.FLAG_ACTIVITY_NEW_TASK].
 * @param selectedText the currently selected text to pass in [Intent.EXTRA_TEXT]
 */
internal fun startLookUpActivity(context: Context, selectedText: String) {
    if (selectedText.isNotBlank()) {
        val intent = createLookUpTextIntent(selectedText)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // The Look Up handler may have been disabled after the menu was shown.
            Log.w(LookUpText.TAG, "No activity found to handle $intent", e)
        }
    }
}

private fun createLookUpTextIntent(selectedText: String): Intent {
    return Intent(LookUpText.ACTION_LOOK_UP_TEXT).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, selectedText)
    }
}

internal object LookUpText {
    internal const val TAG = "LookUpText"

    // TODO(b/578168517): Replace with Intent.ACTION_LOOK_UP_TEXT once it is available in the
    //  compile SDK.
    internal const val ACTION_LOOK_UP_TEXT = "android.intent.action.LOOK_UP_TEXT"

    /** Returns whether an activity can handle the given Look Up [Intent]. */
    @VisibleForTesting
    var canResolveLookUpActivity: (context: Context, intent: Intent) -> Boolean =
        { context, intent ->
            intent.resolveActivity(context.packageManager) != null
        }
}
