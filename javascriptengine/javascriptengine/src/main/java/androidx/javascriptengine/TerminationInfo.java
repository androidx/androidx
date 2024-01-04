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

package androidx.javascriptengine;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import org.chromium.android_webview.js_sandbox.common.IJsSandboxIsolateClient;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Information about how and why an isolate has terminated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TerminationInfo {
    /**
     * Termination status code for an isolate.
     */
    @IntDef({STATUS_UNKNOWN_ERROR, STATUS_SANDBOX_DEAD, STATUS_MEMORY_LIMIT_EXCEEDED})
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {
    }

    /**
     * The isolate (but not necessarily the sandbox) has crashed for an unknown reason.
     */
    public static final int STATUS_UNKNOWN_ERROR = IJsSandboxIsolateClient.TERMINATE_UNKNOWN_ERROR;
    /**
     * The whole sandbox died (or was closed), taking this isolate with it.
     */
    public static final int STATUS_SANDBOX_DEAD = IJsSandboxIsolateClient.TERMINATE_SANDBOX_DEAD;
    /**
     * The isolate exceeded its heap size limit.
     * <p>
     * The isolate may continue to hold onto resources (even if explicitly closed) until the
     * sandbox has been shutdown. If necessary, restart the sandbox at the earliest opportunity in
     * order to reclaim these resources.
     * <p>
     * Note that memory exhaustion will kill the whole sandbox, so any other isolates within the
     * same sandbox will be terminated with {@link #STATUS_SANDBOX_DEAD}.
     */
    public static final int STATUS_MEMORY_LIMIT_EXCEEDED =
            IJsSandboxIsolateClient.TERMINATE_MEMORY_LIMIT_EXCEEDED;
    @Status
    private final int mStatus;
    @NonNull
    private final String mMessage;

    TerminationInfo(@Status int status, @NonNull String message) {
        mStatus = status;
        mMessage = message;
    }

    /**
     * Get the status code of the termination.
     * <p>
     * New status codes may be added with new JavaScriptEngine versions.
     *
     * @return status code of the termination.
     */
    @Status
    public int getStatus() {
        return mStatus;
    }

    /**
     * Describe the status code of the termination.
     * These strings are not stable between JavaScriptEngine versions.
     *
     * @return description of status code of the termination.
     */
    @NonNull
    public String getStatusString() {
        switch (mStatus) {
            case STATUS_UNKNOWN_ERROR:
                return "unknown error";
            case STATUS_SANDBOX_DEAD:
                return "sandbox dead";
            case STATUS_MEMORY_LIMIT_EXCEEDED:
                return "memory limit exceeded";
            default:
                return "unknown error code " + mStatus;
        }
    }

    /**
     * Get the message associated with this termination.
     * The content or format of these messages is not stable between JavaScriptEngine versions.
     *
     * @return Human-readable message about the termination.
     */
    @NonNull
    public String getMessage() {
        return mMessage;
    }

    /**
     * Describe the termination.
     * The content or format of this description is not stable between JavaScriptEngine versions.
     *
     * @return Human-readable description of the termination.
     */
    @NonNull
    @Override
    public String toString() {
        return getStatusString() + ": " + getMessage();
    }

    @NonNull
    IsolateTerminatedException toJavaScriptException() {
        switch (mStatus) {
            case STATUS_SANDBOX_DEAD:
                return new SandboxDeadException(this.toString());
            case STATUS_MEMORY_LIMIT_EXCEEDED:
                return new MemoryLimitExceededException(this.toString());
            default:
                return new IsolateTerminatedException(this.toString());
        }
    }
}
