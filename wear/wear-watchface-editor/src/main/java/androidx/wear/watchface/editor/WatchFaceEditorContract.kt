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

package androidx.wear.watchface.editor

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.wearable.watchface.Constants
import androidx.activity.result.contract.ActivityResultContract
import androidx.wear.watchface.client.EditorServiceClient
import androidx.wear.watchface.client.EditorState
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.style.UserStyle

internal const val INSTANCE_ID_KEY: String = "INSTANCE_ID_KEY"
internal const val COMPONENT_NAME_KEY: String = "COMPONENT_NAME_KEY"
internal const val USER_STYLE_KEY: String = "USER_STYLE_KEY"
internal const val USER_STYLE_VALUES: String = "USER_STYLE_VALUES"

/**
 * The request sent by [WatchFaceEditorContract.createIntent]. The editing session's result should
 * be reported via [Activity.setWatchRequestResult].
 */
public class EditorRequest(
    /** The [ComponentName] of the watch face being edited. */
    public val watchFaceComponentName: ComponentName,

    /** The [ComponentName] of the watch face editor. */
    public val editorComponentName: ComponentName,

    /**
     * Unique ID for the instance of the watch face being edited, only defined for Android R and
     * beyond, it's `null` on Android P and earlier. Note each distinct [ComponentName] can have
     * multiple instances.
     */
    public val watchFaceInstanceId: String?,

    /** The initial [UserStyle], only required for a headless [EditorSession]. */
    public val initialUserStyle: Map<String, String>?
) {
    public companion object {
        /**
         * Returns an [EditorRequest] saved to a [Intent] by [WatchFaceEditorContract.createIntent]
         * if there is one or `null` otherwise. Intended for use by the watch face editor activity.
         */
        @JvmStatic
        public fun createFromIntent(intent: Intent): EditorRequest? {
            val componentName =
                intent.getParcelableExtra<ComponentName>(COMPONENT_NAME_KEY)
                    ?: intent.getParcelableExtra(Constants.EXTRA_WATCH_FACE_COMPONENT)
            val editorComponentName = intent.component ?: ComponentName("?", "?")
            val instanceId = intent.getStringExtra(INSTANCE_ID_KEY)
            val userStyleKey = intent.getStringArrayExtra(USER_STYLE_KEY)
            val userStyleValue = intent.getStringArrayExtra(USER_STYLE_VALUES)
            return componentName?.let {
                if (userStyleKey != null && userStyleValue != null &&
                    userStyleKey.size == userStyleValue.size
                ) {
                    EditorRequest(
                        componentName,
                        editorComponentName,
                        instanceId,
                        HashMap<String, String>().apply {
                            for (i in userStyleKey.indices) {
                                put(userStyleKey[i], userStyleValue[i])
                            }
                        }
                    )
                } else {
                    EditorRequest(componentName, editorComponentName, instanceId, null)
                }
            }
        }
    }
}

/**
 * An [ActivityResultContract] for invoking a watch face editor. Note watch face editors are invoked
 * by SysUI and the normal activity result isn't used for returning [EditorState] because
 * [Activity.onStop] isn't guaranteed to be called when SysUI UX needs it to. Instead [EditorState]
 * is broadcast by the editor using[EditorSession.close], to observe these broadcasts use
 * [WatchFaceControlClient.getEditorServiceClient] and [EditorServiceClient.registerObserver].
 */
public open class WatchFaceEditorContract : ActivityResultContract<EditorRequest, Unit>() {

    public companion object {
        public const val ACTION_WATCH_FACE_EDITOR: String =
            "androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR"
    }

    // Required for testing.
    internal open fun nullWatchFaceInstanceIdOK() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    override fun createIntent(
        context: Context,
        input: EditorRequest
    ): Intent {
        require(
            input.watchFaceInstanceId != null || nullWatchFaceInstanceIdOK()
        ) {
            "watchFaceInstanceId must be set from Android R and above"
        }
        return Intent(ACTION_WATCH_FACE_EDITOR).apply {
            component = input.editorComponentName
            putExtra(COMPONENT_NAME_KEY, input.watchFaceComponentName)
            putExtra(INSTANCE_ID_KEY, input.watchFaceInstanceId)
            input.initialUserStyle?.let {
                putExtra(USER_STYLE_KEY, it.keys.toTypedArray())
                putExtra(USER_STYLE_VALUES, it.values.toTypedArray())
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {}
}
