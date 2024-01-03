/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tracing.perfetto.internal.handshake.protocol

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

// Keep these two packages in sync:
// - `androidx.tracing.perfetto.handshake.protocol` in the tracing/tracing-perfetto-handshake folder
// - `androidx.tracing.perfetto.internal.handshake.protocol` in the tracing/tracing-perfetto folder
//
// This is a part of a WIP refactor to decouple tracing-perfetto and tracing-perfetto-handshake
// tracked under TODO(243405142)

@RestrictTo(LIBRARY_GROUP)
internal object RequestKeys {
    public const val RECEIVER_CLASS_NAME: String = "androidx.tracing.perfetto.TracingReceiver"

    /**
     * Request to enable tracing in an app.
     *
     * The action is performed straight away allowing for warm / hot tracing. For cold start
     * tracing see [ACTION_ENABLE_TRACING_COLD_START]
     *
     * Request can include [KEY_PATH] as an optional extra.
     *
     * Response to the request is a JSON string (to allow for CLI support) with the following:
     * - [ResponseKeys.KEY_RESULT_CODE] (always)
     * - [ResponseKeys.KEY_REQUIRED_VERSION] (always)
     * - [ResponseKeys.KEY_MESSAGE] (optional)
     */
    public const val ACTION_ENABLE_TRACING: String =
        "androidx.tracing.perfetto.action.ENABLE_TRACING"

    /**
     * Request to enable cold start tracing in an app.
     *
     * For warm / hot tracing, see [ACTION_ENABLE_TRACING].
     *
     * The action must be performed in the following order, otherwise its effects are
     * unspecified:
     * - the app process must be killed before performing the action
     * - the action must then follow
     * - the app process must be killed after performing the action
     *
     * Request can include [KEY_PATH] as an optional extra.
     * Request can include [KEY_PERSISTENT] as an optional extra.
     *
     * Response to the request is a JSON string (to allow for CLI support) with the following:
     * - [ResponseKeys.KEY_RESULT_CODE] (always)
     * - [ResponseKeys.KEY_REQUIRED_VERSION] (always)
     * - [ResponseKeys.KEY_MESSAGE] (optional)
     */
    public const val ACTION_ENABLE_TRACING_COLD_START: String =
        "androidx.tracing.perfetto.action.ENABLE_TRACING_COLD_START"

    /**
     * Request to disable cold start tracing (previously enabled with
     * [ACTION_ENABLE_TRACING_COLD_START]).
     *
     * The action is particularly useful when cold start tracing was enabled in
     * [KEY_PERSISTENT] mode.
     *
     * The action must be performed in the following order, otherwise its effects are
     * unspecified:
     * - the app process must be killed before performing the action
     * - the action must then follow
     * - the app process must be killed after performing the action
     *
     * Request can include [KEY_PATH] as an optional extra.
     * Request can include [KEY_PERSISTENT] as an optional extra.
     *
     * Response to the request is a JSON string (to allow for CLI support) with the following:
     * - [ResponseKeys.KEY_RESULT_CODE] (always)
     */
    public const val ACTION_DISABLE_TRACING_COLD_START: String =
        "androidx.tracing.perfetto.action.DISABLE_TRACING_COLD_START"

    /** Path to tracing native binary file */
    public const val KEY_PATH: String = "path"

    /**
     * Boolean flag to signify whether the operation should be persistent between runs
     * (or only performed once).
     *
     * Applies to [ACTION_ENABLE_TRACING_COLD_START]
     */
    public const val KEY_PERSISTENT: String = "persistent"
}

@RestrictTo(LIBRARY_GROUP)
internal object ResponseKeys {
    /**
     * Result code as listed in [ResponseResultCodes].
     *
     * Note: the value of the string ("exitCode") is kept unchanged to maintain backwards
     * compatibility.
     */
    public const val KEY_RESULT_CODE: String = "exitCode"

    /**
     * Required version of the binaries. Java and binary library versions have to match to
     * ensure compatibility. In the Maven format, e.g. 1.2.3-beta01.
     */
    public const val KEY_REQUIRED_VERSION: String = "requiredVersion"

    /**
     * Message string that gives more information about the response, e.g. recovery steps
     * if applicable.
     */
    public const val KEY_MESSAGE: String = "message"
}

internal object ResponseResultCodes {
    /**
     * Indicates that the broadcast resulted in `result=0`, which is an equivalent
     * of [android.app.Activity.RESULT_CANCELED].
     *
     * This most likely means that the app does not expose a [PerfettoSdkHandshake] compatible
     * receiver.
     */
    @Suppress("KDocUnresolvedReference")
    public const val RESULT_CODE_CANCELLED: Int = 0

    public const val RESULT_CODE_SUCCESS: Int = 1
    public const val RESULT_CODE_ALREADY_ENABLED: Int = 2

    /**
     * Required version described in [Response.requiredVersion].
     * A follow-up [androidx.tracing.perfetto.handshake.PerfettoSdkHandshake.enableTracingImmediate]
     * request expected with binaries to sideload specified.
     */
    public const val RESULT_CODE_ERROR_BINARY_MISSING: Int = 11

    /** Required version described in [Response.requiredVersion]. */
    public const val RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH: Int = 12

    /**
     * Could be a result of a stale version of the binary cached locally.
     * Retrying with a freshly downloaded library likely to fix the issue.
     * More specific information in [Response.message]
     */
    public const val RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR: Int = 13

    /** More specific information in [Response.message] */
    public const val RESULT_CODE_ERROR_OTHER: Int = 99
}

@Retention(AnnotationRetention.SOURCE)
@IntDef(
    ResponseResultCodes.RESULT_CODE_CANCELLED,
    ResponseResultCodes.RESULT_CODE_SUCCESS,
    ResponseResultCodes.RESULT_CODE_ALREADY_ENABLED,
    ResponseResultCodes.RESULT_CODE_ERROR_BINARY_MISSING,
    ResponseResultCodes.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH,
    ResponseResultCodes.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR,
    ResponseResultCodes.RESULT_CODE_ERROR_OTHER
)
private annotation class ResultCode

internal class Response @RestrictTo(LIBRARY_GROUP) constructor(
    @ResultCode public val resultCode: Int,

    /**
     * This can be `null` iff we cannot communicate with the broadcast receiver of the target
     * process (e.g. app does not offer Perfetto tracing) or if we cannot parse the response
     * from the receiver. In either case, tracing is unlikely to work under these circumstances,
     * and more context on how to proceed can be found in [resultCode] or [message] properties.
     */
    public val requiredVersion: String?,

    public val message: String?
)
