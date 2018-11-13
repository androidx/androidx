/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

/**
 * Base interface for result classes in {@link MediaSession} and {@link MediaController} that may
 * be sent across the processes.
 **/
interface RemoteResult extends BaseResult {
    /**
     * Result code representing that the session and controller were disconnected.
     */
    int RESULT_CODE_DISCONNECTED = -100;

    /**
     * Result code representing that the authentication has expired.
     */
    int RESULT_CODE_AUTHENTICATION_EXPIRED = -102;

    /**
     * Result code representing that a premium account is required.
     */
    int RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED = -103;

    /**
     * Result code representing that too many concurrent streams are detected.
     */
    int RESULT_CODE_CONCURRENT_STREAM_LIMIT = -104;

    /**
     * Result code representing that the content is blocked due to parental controls.
     */
    int RESULT_CODE_PARENTAL_CONTROL_RESTRICTED = -105;

    /**
     * Result code representing that the content is blocked due to being regionally unavailable.
     */
    int RESULT_CODE_NOT_AVAILABLE_IN_REGION = -106;

    /**
     * Result code representing that the application cannot skip any more because the skip limit is
     * reached.
     */
    int RESULT_CODE_SKIP_LIMIT_REACHED = -107;

    /**
     * Result code representing that the session needs user's manual intervention.
     */
    int RESULT_CODE_SETUP_REQUIRED = -108;
}
