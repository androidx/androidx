/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import androidx.media2.common.BaseResult;

/**
 * Base interface for result classes in {@link MediaSession} and {@link MediaController} that may be
 * sent across the processes.
 *
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
interface RemoteResult extends BaseResult {
    /**
     * Result code representing that the session and controller were disconnected.
     */
    int RESULT_ERROR_SESSION_DISCONNECTED = -100;

    /**
     * Result code representing that the authentication has expired.
     */
    int RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED = -102;

    /**
     * Result code representing that a premium account is required.
     */
    int RESULT_ERROR_SESSION_PREMIUM_ACCOUNT_REQUIRED = -103;

    /**
     * Result code representing that too many concurrent streams are detected.
     */
    int RESULT_ERROR_SESSION_CONCURRENT_STREAM_LIMIT = -104;

    /**
     * Result code representing that the content is blocked due to parental controls.
     */
    int RESULT_ERROR_SESSION_PARENTAL_CONTROL_RESTRICTED = -105;

    /**
     * Result code representing that the content is blocked due to being regionally unavailable.
     */
    int RESULT_ERROR_SESSION_NOT_AVAILABLE_IN_REGION = -106;

    /**
     * Result code representing that the application cannot skip any more because the skip limit is
     * reached.
     */
    int RESULT_ERROR_SESSION_SKIP_LIMIT_REACHED = -107;

    /**
     * Result code representing that the session needs user's manual intervention.
     */
    int RESULT_ERROR_SESSION_SETUP_REQUIRED = -108;
}
