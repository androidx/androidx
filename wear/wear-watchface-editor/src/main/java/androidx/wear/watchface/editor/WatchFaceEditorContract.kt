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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.wear.watchface.client.EditorServiceClient
import androidx.wear.watchface.client.EditorState
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.jvm.Throws

internal const val INSTANCE_ID_KEY: String = "INSTANCE_ID_KEY"
internal const val COMPONENT_NAME_KEY: String = "COMPONENT_NAME_KEY"
internal const val USER_STYLE_KEY: String = "USER_STYLE_KEY"
internal const val USER_STYLE_VALUES: String = "USER_STYLE_VALUES"

/**
 * The request sent by [WatchFaceEditorContract.createIntent].
 *
 * @param watchFaceComponentName The [ComponentName] of the watch face being edited.
 * @param editorPackageName The package name of the watch face editor APK.
 * @param initialUserStyle The initial [UserStyle] stored as a [UserStyleData] or `null`. Only
 * required for a headless [EditorSession].
 * @param watchFaceId Unique ID for the instance of the watch face being edited, only defined for
 * Android R and beyond, it's `null` on Android P and earlier. Note each distinct [ComponentName]
 * can have multiple instances.
 */
public class EditorRequest @RequiresApi(Build.VERSION_CODES.R) constructor(
    public val watchFaceComponentName: ComponentName,
    public val editorPackageName: String,
    public val initialUserStyle: UserStyleData?,

    @get:RequiresApi(Build.VERSION_CODES.R)
    @RequiresApi(Build.VERSION_CODES.R)
    public val watchFaceId: WatchFaceId
) {
    /**
     * Constructs an [EditorRequest] without a [WatchFaceId]. This is for use pre-android R.
     *
     * @param watchFaceComponentName The [ComponentName] of the watch face being edited.
     * @param editorPackageName The package name of the watch face editor APK.
     * @param initialUserStyle The initial [UserStyle] stored as a [UserStyleData] or `null`. Only
     * required for a headless [EditorSession].
     * [EditorSession].
     */
    @SuppressLint("NewApi")
    public constructor(
        watchFaceComponentName: ComponentName,
        editorPackageName: String,
        initialUserStyle: UserStyleData?
    ) : this(
        watchFaceComponentName,
        editorPackageName,
        initialUserStyle,
        WatchFaceId("")
    )

    public companion object {
        /**
         * Returns an [EditorRequest] saved to a [Intent] by [WatchFaceEditorContract.createIntent]
         * if there is one or `null` otherwise. Intended for use by the watch face editor activity.
         * @throws [TimeoutCancellationException] in case of en error.
         */
        @SuppressLint("NewApi")
        @JvmStatic
        @Throws(TimeoutCancellationException::class)
        public fun createFromIntent(intent: Intent): EditorRequest {
            val componentName =
                intent.getParcelableExtra<ComponentName>(COMPONENT_NAME_KEY)!!
            val editorPackageName = intent.getPackage() ?: ""
            val instanceId = WatchFaceId(intent.getStringExtra(INSTANCE_ID_KEY) ?: "")
            val userStyleKey = intent.getStringArrayExtra(USER_STYLE_KEY)
            return componentName.let {
                if (userStyleKey != null) {
                    EditorRequest(
                        componentName,
                        editorPackageName,
                        UserStyleData(
                            HashMap<String, ByteArray>().apply {
                                for (i in userStyleKey.indices) {
                                    val userStyleValue =
                                        intent.getByteArrayExtra(USER_STYLE_VALUES + i)!!
                                    put(userStyleKey[i], userStyleValue)
                                }
                            }
                        ),
                        instanceId
                    )
                }
                EditorRequest(componentName, editorPackageName, null, instanceId)
            }
        }
    }
}

/**
 * An [ActivityResultContract] for invoking a watch face editor. Note watch face editors are invoked
 * by SysUI and the normal activity result isn't used for returning [EditorState] because
 * [Activity.onStop] isn't guaranteed to be called when SysUI UX needs it to. Instead [EditorState]
 * is broadcast by the editor using[EditorSession.close], to observe these broadcasts use
 * [WatchFaceControlClient.getEditorServiceClient] and [EditorServiceClient.addListener].
 */
public open class WatchFaceEditorContract : ActivityResultContract<EditorRequest, Unit>() {

    public companion object {
        public const val ACTION_WATCH_FACE_EDITOR: String =
            "androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR"
    }

    override fun createIntent(
        context: Context,
        input: EditorRequest
    ): Intent {
        return Intent(ACTION_WATCH_FACE_EDITOR).apply {
            setPackage(input.editorPackageName)
            putExtra(COMPONENT_NAME_KEY, input.watchFaceComponentName)
            putExtra(INSTANCE_ID_KEY, input.watchFaceId.id)
            input.initialUserStyle?.let {
                putExtra(USER_STYLE_KEY, it.userStyleMap.keys.toTypedArray())
                for ((index, value) in it.userStyleMap.values.withIndex()) {
                    putExtra(USER_STYLE_VALUES + index, value)
                }
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {}
}
