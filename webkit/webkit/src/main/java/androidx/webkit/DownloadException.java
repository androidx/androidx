/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes why a {@link WebDownload} did not complete.
 *
 * <p>Passed to {@link WebViewOutcomeReceiver#onError} by {@link WebDownload#saveTo} when the
 * download fails or is cancelled. Use {@link #getErrorCode()} to find out why.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WebViewCompat.ExperimentalDownloadHandler
public final class DownloadException extends Exception {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            ERROR_UNKNOWN,
            ERROR_CANCELLED,
            ERROR_NETWORK,
            ERROR_DESTINATION_WRITE_FAILED,
            ERROR_INSUFFICIENT_SPACE,
            ERROR_SERVER,
            ERROR_SERVER_ACCESS_DENIED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {}

    /** The download failed for a reason not covered by the other error codes. */
    public static final int ERROR_UNKNOWN = 0;

    /** The download was cancelled, for example with {@link WebDownload#cancel()}. */
    public static final int ERROR_CANCELLED = 1;

    /**
     * The download failed because of a network error, for example because the device is offline,
     * the connection timed out or was reset, or the server sent less data than it announced.
     */
    public static final int ERROR_NETWORK = 2;

    /** The destination could not be created, opened or written to. */
    public static final int ERROR_DESTINATION_WRITE_FAILED = 3;

    /** There is not enough storage space for the download. */
    public static final int ERROR_INSUFFICIENT_SPACE = 4;

    /**
     * The server answered with an error status, for example {@code 404 Not Found},
     * {@code 410 Gone} or a {@code 5xx} status.
     */
    public static final int ERROR_SERVER = 5;

    /**
     * The server refused access to the file, with status {@code 401 Unauthorized},
     * {@code 403 Forbidden} or {@code 407 Proxy Authentication Required}. For example, the user
     * needs to sign in, or the link has expired. If the destination can't be written to, the
     * download fails with {@link #ERROR_DESTINATION_WRITE_FAILED} instead.
     */
    public static final int ERROR_SERVER_ACCESS_DENIED = 6;

    private final @ErrorCode int mErrorCode;

    /** Creates a {@link DownloadException} with the given error code. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public DownloadException(@ErrorCode int errorCode) {
        super("Download failed with error code " + errorCode);
        mErrorCode = errorCode;
    }

    /**
     * Returns why the download did not complete.
     *
     * <p>More error codes may be added in the future. Treat any value not defined in this class
     * as {@link #ERROR_UNKNOWN}.
     *
     * @return the error code, such as {@link #ERROR_NETWORK}.
     */
    public @ErrorCode int getErrorCode() {
        return mErrorCode;
    }
}
