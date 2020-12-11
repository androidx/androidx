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
import android.support.wearable.watchface.Constants
import androidx.activity.result.contract.ActivityResultContract
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.data.UserStyleWireFormat

/**
 * The request sent by [WatchFaceEditorContract.createIntent]. The editing session's result should
 * be reported via [Activity.setWatchRequestResult].
 */
public class EditorRequest(
    /** The [ComponentName] on the watch face being edited. */
    public val watchFaceComponentName: ComponentName,

    /** A unique ID for the instance of the watch face being edited. */
    public val watchFaceInstanceId: String,

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
            val instanceId = intent.getStringExtra(INSTANCE_ID)!!
            val userStyleKey = intent.getStringArrayExtra(USER_STYLE_KEYS)
            val userStyleValue = intent.getStringArrayExtra(USER_STYLE_VALUES)
            return componentName?.let {
                if (userStyleKey != null && userStyleValue != null &&
                    userStyleKey.size == userStyleValue.size
                ) {
                    EditorRequest(
                        componentName,
                        instanceId,
                        HashMap<String, String>().apply {
                            for (i in userStyleKey.indices) {
                                put(userStyleKey[i], userStyleValue[i])
                            }
                        }
                    )
                } else {
                    EditorRequest(componentName, instanceId, null)
                }
            }
        }

        internal const val COMPONENT_NAME_KEY: String = "COMPONENT_NAME_KEY"
        internal const val INSTANCE_ID: String = "INSTANCE_ID"
        internal const val USER_STYLE_KEYS: String = "USER_STYLE_KEYS"
        internal const val USER_STYLE_VALUES: String = "USER_STYLE_VALUES"
    }
}

/**
 * The result for a successful [EditorRequest], to be returned via [Activity.setWatchRequestResult].
 */
public class EditorResult(
    /** The updated style, see [UserStyle]. */
    public val userStyle: Map<String, String>
) {
    internal companion object {
        internal const val USER_STYLE_KEY: String = "USER_STYLE_KEY"
    }
}

/** An [ActivityResultContract] for invoking a watch face editor. */
public open class WatchFaceEditorContract : ActivityResultContract<EditorRequest, EditorResult>() {

    public companion object {
        public const val ACTION_WATCH_FACE_EDITOR: String =
            "androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR"
    }

    override fun createIntent(context: Context, input: EditorRequest): Intent =
        Intent(ACTION_WATCH_FACE_EDITOR).apply {
            putExtra(EditorRequest.COMPONENT_NAME_KEY, input.watchFaceComponentName)
            putExtra(EditorRequest.INSTANCE_ID, input.watchFaceInstanceId)
            input.initialUserStyle?.let {
                putExtra(EditorRequest.USER_STYLE_KEYS, it.keys.toTypedArray())
                putExtra(EditorRequest.USER_STYLE_VALUES, it.values.toTypedArray())
            }
        }

    override fun parseResult(resultCode: Int, intent: Intent?): EditorResult {
        val extras = intent!!.extras!!
        extras.classLoader = this::class.java.classLoader
        return EditorResult(
            ParcelUtils.fromParcelable<UserStyleWireFormat>(
                extras.getParcelable(EditorResult.USER_STYLE_KEY)!!
            ).mUserStyle
        )
    }
}
