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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Result class to be used with {@link ListenableFuture} for asynchronous calls between
 * {@link MediaSession} and {@link MediaController}.
 */
@VersionedParcelize
public class SessionResult implements RemoteResult, VersionedParcelable {
    /**
     * Result code representing that the command is successfully completed.
     * <p>
     * Interoperability: This code is also used to tell that the command was successfully sent, but
     * the result is unknown when connected with {@link MediaSessionCompat} or
     * {@link MediaControllerCompat}.
     */
    // Redefined to override the Javadoc
    public static final int RESULT_CODE_SUCCESS = 0;

    /**
     * @hide
     */
    @IntDef(flag = false, /*prefix = "RESULT_CODE",*/ value = {
            RESULT_CODE_SUCCESS,
            RESULT_CODE_UNKNOWN_ERROR,
            RESULT_CODE_INVALID_STATE,
            RESULT_CODE_BAD_VALUE,
            RESULT_CODE_PERMISSION_DENIED,
            RESULT_CODE_IO_ERROR,
            RESULT_CODE_SKIPPED,
            RESULT_CODE_DISCONNECTED,
            RESULT_CODE_NOT_SUPPORTED,
            RESULT_CODE_AUTHENTICATION_EXPIRED,
            RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED,
            RESULT_CODE_CONCURRENT_STREAM_LIMIT,
            RESULT_CODE_PARENTAL_CONTROL_RESTRICTED,
            RESULT_CODE_NOT_AVAILABLE_IN_REGION,
            RESULT_CODE_SKIP_LIMIT_REACHED,
            RESULT_CODE_SETUP_REQUIRED})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface ResultCode {}

    @ParcelField(1)
    int mResultCode;
    @ParcelField(2)
    long mCompletionTime;
    @ParcelField(3)
    Bundle mCustomCommandResult;
    @ParcelField(4)
    MediaItem mItem;

    /**
     * Constructor to be used by {@link MediaSession.SessionCallback#onCustomCommand(
     * MediaSession, MediaSession.ControllerInfo, SessionCommand, Bundle)}.
     *
     * @param resultCode result code
     * @param customCommandResult custom command result.
     */
    public SessionResult(@ResultCode int resultCode, @Nullable Bundle customCommandResult) {
        this(resultCode, customCommandResult, null, SystemClock.elapsedRealtime());
    }

    // For versioned-parcelable
    SessionResult() {
        // no-op
    }

    SessionResult(@ResultCode int resultCode) {
        this(resultCode, null);
    }

    SessionResult(@ResultCode int resultCode, Bundle customCommandResult, MediaItem item) {
        this(resultCode, customCommandResult, item, SystemClock.elapsedRealtime());
    }

    SessionResult(@ResultCode int resultCode, @Nullable Bundle customCommandResult,
            @Nullable MediaItem item, long completionTime) {
        mResultCode = resultCode;
        mCustomCommandResult = customCommandResult;
        mItem = item;
        mCompletionTime = completionTime;
    }

    @Nullable
    static SessionResult from(@Nullable SessionPlayer.PlayerResult result) {
        if (result == null) {
            return null;
        }
        return new SessionResult(result.getResultCode(), null, result.getMediaItem(),
                result.getCompletionTime());
    }

    static ListenableFuture<SessionResult> createFutureWithResult(@ResultCode int resultCode) {
        ResolvableFuture<SessionResult> result = ResolvableFuture.create();
        result.set(new SessionResult(resultCode));
        return result;
    }

    /**
     * Gets the result code.
     *
     * @return result code
     * @see #RESULT_CODE_SUCCESS
     * @see #RESULT_CODE_UNKNOWN_ERROR
     * @see #RESULT_CODE_INVALID_STATE
     * @see #RESULT_CODE_BAD_VALUE
     * @see #RESULT_CODE_PERMISSION_DENIED
     * @see #RESULT_CODE_IO_ERROR
     * @see #RESULT_CODE_SKIPPED
     * @see #RESULT_CODE_DISCONNECTED
     * @see #RESULT_CODE_NOT_SUPPORTED
     * @see #RESULT_CODE_AUTHENTICATION_EXPIRED
     * @see #RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED
     * @see #RESULT_CODE_CONCURRENT_STREAM_LIMIT
     * @see #RESULT_CODE_PARENTAL_CONTROL_RESTRICTED
     * @see #RESULT_CODE_NOT_AVAILABLE_IN_REGION
     * @see #RESULT_CODE_SKIP_LIMIT_REACHED
     * @see #RESULT_CODE_SETUP_REQUIRED
     */
    @Override
    @ResultCode
    public int getResultCode() {
        return mResultCode;
    }

    /**
     * Gets the result of
     * {@link MediaSession#sendCustomCommand(MediaSession.ControllerInfo, SessionCommand, Bundle)}
     * and {@link MediaController#sendCustomCommand(SessionCommand, Bundle)} only when this object
     * is returned by one of them.
     * <p>
     * If this object is returned by other methods, this method will be {@code null}.
     *
     * @see MediaSession#sendCustomCommand(MediaSession.ControllerInfo, SessionCommand, Bundle)
     * @see MediaController#sendCustomCommand(SessionCommand, Bundle)
     * @return result of sending custom command
     */
    @Nullable
    public Bundle getCustomCommandResult() {
        return mCustomCommandResult;
    }

    /**
     * Gets the completion time of the command. Being more specific, it's the same as
     * {@link SystemClock#elapsedRealtime()} when the command is completed.
     *
     * @return completion time of the command
     */
    @Override
    public long getCompletionTime() {
        return mCompletionTime;
    }

    /**
     * Gets the {@link MediaItem} for which the command was executed. In other words, this is
     * the current media item when the command was completed.
     * <p>
     * Can be {@code null} for many reasons. For examples,
     * <ul>
     * <li>Error happened.
     * <li>Current media item was {@code null} at that time.
     * <li>Command is irrelevant with the media item (e.g. custom command).
     * </ul>
     *
     * @return media item when the command is completed. Can be {@code null} for an error, the
     *         current media item was {@code null}, or any other reason.
     */
    @Override
    @Nullable
    public MediaItem getMediaItem() {
        return mItem;
    }
}
