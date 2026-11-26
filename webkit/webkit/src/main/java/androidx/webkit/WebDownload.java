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

import android.net.Uri;
import android.webkit.WebView;

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.WebDownloadBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A download started in a {@link WebView} that has a {@link DownloadHandler}. WebView carries out
 * the download on the app's behalf.
 *
 * <p>WebView passes each download to {@link DownloadHandler#onDownloadRequested}. The app must then
 * call either {@link #saveTo} to choose where WebView saves the file, or {@link #cancel()} to
 * reject the download. It may do so after {@link DownloadHandler#onDownloadRequested} has
 * returned, for example after asking the user where to save the file. Until then, WebView keeps
 * downloading into a temporary file that it manages and deletes itself. If this object becomes
 * unreachable before either method is called, the download is eventually cancelled.
 *
 * <p>All methods of this class may be called on any thread.
 *
 * <p>The download is not tied to the {@link WebView} that started it, and continues if that
 * {@link WebView} is destroyed. WebView does not keep track of unfinished downloads across app
 * process restarts.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WebViewCompat.ExperimentalDownloadHandler
public final class WebDownload {
    private final @NonNull WebDownloadBoundaryInterface mBoundaryInterface;

    /** Creates a {@link WebDownload} that wraps the given boundary {@link InvocationHandler}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public WebDownload(@NonNull InvocationHandler invocationHandler) {
        mBoundaryInterface =
                Objects.requireNonNull(
                        BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                                WebDownloadBoundaryInterface.class, invocationHandler));
    }

    /**
     * Returns the URL of the download, after any redirects.
     *
     * @return the final URL of the download.
     */
    @AnyThread
    public @NonNull String getUrl() {
        return mBoundaryInterface.getUrl();
    }

    /**
     * Returns the MIME type of the download, for example {@code "application/pdf"}.
     *
     * @return the MIME type, or an empty string if it is unknown.
     */
    @AnyThread
    public @NonNull String getMimeType() {
        return mBoundaryInterface.getMimeType();
    }

    /**
     * Returns the expected size of the download in bytes, usually taken from the
     * {@code Content-Length} response header.
     *
     * @return the expected size in bytes, or {@code -1} if it is unknown.
     */
    @AnyThread
    public long getContentLength() {
        return mBoundaryInterface.getContentLength();
    }

    /**
     * Returns the file name that WebView suggests for the download, based on the
     * {@code Content-Disposition} response header, the URL and the MIME type.
     *
     * @return the suggested file name.
     */
    @AnyThread
    public @NonNull String getSuggestedFilename() {
        return mBoundaryInterface.getSuggestedFilename();
    }

    /**
     * Saves the download to {@code destination}.
     *
     * <p>The following destinations are supported:
     * <ul>
     *   <li>A {@code file://} {@link Uri} with an absolute path that the app can write to.
     *       WebView creates the file, and any missing parent directories.
     *   <li>A {@code content://} {@link android.provider.MediaStore} item that the app inserted,
     *       for example in {@link android.provider.MediaStore.Downloads}, usually with
     *       {@link android.provider.MediaStore.MediaColumns#IS_PENDING} set to {@code 1}. When the
     *       download succeeds, WebView publishes the item by setting {@code IS_PENDING} to
     *       {@code 0}.
     * </ul>
     * Any existing content in {@code destination} is overwritten. If WebView can't write to
     * {@code destination}, for example because the app doesn't have write access to it,
     * {@code callback} receives a {@link DownloadException} with
     * {@link DownloadException#ERROR_DESTINATION_WRITE_FAILED}.
     *
     * <p>This method may be called at most once. {@code callback} is then called exactly once, on
     * {@code executor}:
     * <ul>
     *   <li>If the download succeeds, {@link WebViewOutcomeReceiver#onResult} receives the size
     *       of the saved file in bytes.
     *   <li>If the download fails or is cancelled, {@link WebViewOutcomeReceiver#onError}
     *       receives a {@link DownloadException} that describes why.
     * </ul>
     *
     * <p>From this call until {@code callback} is called, WebView owns {@code destination}, and the
     * app must not modify it. If the download succeeds, {@code destination} holds the complete
     * file, and ownership returns to the app. If the download fails or is cancelled, WebView makes
     * a best-effort attempt to delete {@code destination}, so the app should check for and remove
     * anything left behind. If the app process dies before {@code callback} is called, the app is
     * responsible for cleaning up {@code destination}.
     *
     * <p>WebView keeps the download alive until {@code callback} has been called, even if the app
     * drops this object. WebView carries out the download in the app's process, so the download is
     * subject to the same background restrictions as the rest of the app, and ends if the process
     * is killed. To keep a long download running after the user leaves the app, keep the app's
     * process running until {@code callback} is called, for example with a user-initiated data
     * transfer job (see {@link android.app.job.JobInfo.Builder#setUserInitiated(boolean)}) or a
     * foreground service (see
     * {@link android.app.Service#startForeground(int, android.app.Notification)}).
     *
     * @param destination the {@link Uri} to save the download to.
     * @param executor    the {@link Executor} on which {@code callback} is called.
     * @param callback    receives the outcome of the download.
     * @throws IllegalArgumentException if {@code destination} is not one of the supported
     *                                  destinations.
     * @throws IllegalStateException    if {@link #saveTo} or {@link #cancel()} has already been
     *                                  called.
     */
    @AnyThread
    public void saveTo(@NonNull Uri destination, @NonNull Executor executor,
            @NonNull WebViewOutcomeReceiver<@NonNull Long, @NonNull DownloadException> callback) {
        mBoundaryInterface.saveTo(destination, executor, callback::onResult,
                errorCode -> callback.onError(new DownloadException(toErrorCode(errorCode))));
    }

    /**
     * Cancels the download.
     *
     * <p>May be called before or after {@link #saveTo}. If it is called after {@link #saveTo} and
     * before the download has finished, the {@link #saveTo} callback receives a
     * {@link DownloadException} with {@link DownloadException#ERROR_CANCELLED}. Calling this
     * method again, or after the download has finished, has no effect. {@link #saveTo} must not be
     * called after this method.
     */
    @AnyThread
    public void cancel() {
        mBoundaryInterface.cancel();
    }

    /** Converts an error code reported by WebView into a {@link DownloadException} error code. */
    private static @DownloadException.ErrorCode int toErrorCode(
            @WebDownloadBoundaryInterface.ErrorCode int errorCode) {
        switch (errorCode) {
            case WebDownloadBoundaryInterface.ErrorCode.CANCELLED:
                return DownloadException.ERROR_CANCELLED;
            case WebDownloadBoundaryInterface.ErrorCode.NETWORK:
                return DownloadException.ERROR_NETWORK;
            case WebDownloadBoundaryInterface.ErrorCode.FILE:
                return DownloadException.ERROR_DESTINATION_WRITE_FAILED;
            case WebDownloadBoundaryInterface.ErrorCode.INSUFFICIENT_SPACE:
                return DownloadException.ERROR_INSUFFICIENT_SPACE;
            case WebDownloadBoundaryInterface.ErrorCode.SERVER:
                return DownloadException.ERROR_SERVER;
            case WebDownloadBoundaryInterface.ErrorCode.SERVER_ACCESS_DENIED:
                return DownloadException.ERROR_SERVER_ACCESS_DENIED;
            default:
                // ErrorCode.UNKNOWN, and any error code added in a newer WebView version.
                return DownloadException.ERROR_UNKNOWN;
        }
    }
}
