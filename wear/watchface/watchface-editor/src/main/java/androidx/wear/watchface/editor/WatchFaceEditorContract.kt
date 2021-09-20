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
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.EditorServiceClient
import androidx.wear.watchface.client.EditorState
import androidx.wear.watchface.client.WatchFaceControlClient
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.client.asApiDeviceConfig
import androidx.wear.watchface.data.RenderParametersWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import kotlinx.coroutines.TimeoutCancellationException
import java.time.Instant

internal const val INSTANCE_ID_KEY: String = "INSTANCE_ID_KEY"
internal const val COMPONENT_NAME_KEY: String = "COMPONENT_NAME_KEY"
internal const val HEADLESS_DEVICE_CONFIG_KEY: String = "HEADLESS_DEVICE_CONFIG_KEY"
internal const val RENDER_PARAMETERS_KEY: String = "RENDER_PARAMETERS_KEY"
internal const val RENDER_TIME_MILLIS_KEY: String = "RENDER_TIME_MILLIS_KEY"
internal const val USER_STYLE_KEY: String = "USER_STYLE_KEY"
internal const val USER_STYLE_VALUES: String = "USER_STYLE_VALUES"

typealias WireDeviceConfig = androidx.wear.watchface.data.DeviceConfig

/**
 * Parameters for an optional final screenshot taken by [EditorSession] upon exit and reported via
 * [EditorState].
 *
 * @param renderParameters The [RenderParameters] to use when rendering the screen shot
 * @param instant The [Instant] to render with.
 */
public class PreviewScreenshotParams(
    public val renderParameters: RenderParameters,
    public val instant: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreviewScreenshotParams

        if (renderParameters != other.renderParameters) return false
        if (instant != other.instant) return false

        return true
    }

    override fun hashCode(): Int {
        var result = renderParameters.hashCode()
        result = 31 * result + instant.hashCode()
        return result
    }
}

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
 * @param headlessDeviceConfig If `non-null` then this is the [DeviceConfig] to use when creating
 * a headless instance to back the [EditorSession]. If `null` then the current interactive instance
 * will be used. If there isn't one then the [EditorSession] won't launch until it's been created.
 * Note [supportsWatchFaceHeadlessEditing] can be used to determine if this feature is supported.
 * If it's not supported this parameter will be ignored.
 * @param previewScreenshotParams If `non-null` then [EditorSession] upon
 * closing will render a screenshot with [PreviewScreenshotParams] using the existing interactive
 * or headless instance which will be sent in [EditorState] to any registered clients.
 */
public class EditorRequest @RequiresApi(Build.VERSION_CODES.R) constructor(
    public val watchFaceComponentName: ComponentName,
    public val editorPackageName: String,
    public val initialUserStyle: UserStyleData?,

    @get:RequiresApi(Build.VERSION_CODES.R)
    @RequiresApi(Build.VERSION_CODES.R)
    public val watchFaceId: WatchFaceId,
    public val headlessDeviceConfig: DeviceConfig?,
    public val previewScreenshotParams: PreviewScreenshotParams?
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
        WatchFaceId(""),
        null,
        null
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
        public fun createFromIntent(intent: Intent): EditorRequest = EditorRequest(
            watchFaceComponentName = intent.getParcelableExtra<ComponentName>(COMPONENT_NAME_KEY)!!,
            editorPackageName = intent.getPackage() ?: "",
            initialUserStyle = intent.getStringArrayExtra(USER_STYLE_KEY)?.let {
                UserStyleData(
                    HashMap<String, ByteArray>().apply {
                        for (i in it.indices) {
                            val userStyleValue =
                                intent.getByteArrayExtra(USER_STYLE_VALUES + i)!!
                            put(it[i], userStyleValue)
                        }
                    }
                )
            },
            watchFaceId = WatchFaceId(intent.getStringExtra(INSTANCE_ID_KEY) ?: ""),
            headlessDeviceConfig = intent.getParcelableExtra<WireDeviceConfig>(
                HEADLESS_DEVICE_CONFIG_KEY
            )?.asApiDeviceConfig(),
            previewScreenshotParams = intent.getParcelableExtra<RenderParametersWireFormat>(
                RENDER_PARAMETERS_KEY
            )?.let {
                PreviewScreenshotParams(
                    RenderParameters(it),
                    Instant.ofEpochMilli(intent.getLongExtra(RENDER_TIME_MILLIS_KEY, 0))
                )
            }
        )

        internal const val ANDROIDX_WATCHFACE_API_VERSION = "androidx.wear.watchface.api_version"

        /**
         * Intended to be used in conjunction with [EditorRequest], inspects the watchface's
         * manifest to determine whether or not it supports headless editing.
         *
         * @param packageManager The [PackageManager].
         * @param watchfacePackageName The package name of the watchface, see
         * [ComponentName.getPackageName].
         * @throws [PackageManager.NameNotFoundException] if watchfacePackageName is not recognized.
         */
        @JvmStatic
        @Throws(PackageManager.NameNotFoundException::class)
        public fun supportsWatchFaceHeadlessEditing(
            packageManager: PackageManager,
            watchfacePackageName: String
        ): Boolean {
            val metaData = packageManager.getApplicationInfo(
                watchfacePackageName, PackageManager.GET_META_DATA
            ).metaData
            val apiVersion =
                metaData.getString(ANDROIDX_WATCHFACE_API_VERSION)?.toInt() ?: return false
            return apiVersion >= 4
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
            putExtra(HEADLESS_DEVICE_CONFIG_KEY, input.headlessDeviceConfig?.asWireDeviceConfig())
            input.previewScreenshotParams?.let {
                putExtra(RENDER_PARAMETERS_KEY, it.renderParameters.toWireFormat())
                putExtra(RENDER_TIME_MILLIS_KEY, it.instant.toEpochMilli())
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {}
}
