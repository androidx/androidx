/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.view.inputmethod;

import android.content.ClipDescription;
import android.net.Uri;
import android.os.Build;
import android.view.inputmethod.InputContentInfo;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing features in InputContentInfo introduced after API level 13 in a backwards
 * compatible fashion.
 */
public final class InputContentInfoCompat {

    private interface InputContentInfoCompatImpl {
        @NonNull Uri getContentUri();

        @NonNull ClipDescription getDescription();

        @Nullable Uri getLinkUri();

        @Nullable Object getInputContentInfo();

        void requestPermission();

        void releasePermission();
    }

    private static final class InputContentInfoCompatBaseImpl
            implements InputContentInfoCompatImpl {
        private final @NonNull Uri mContentUri;
        private final @NonNull ClipDescription mDescription;
        private final @Nullable Uri mLinkUri;

        InputContentInfoCompatBaseImpl(@NonNull Uri contentUri,
                @NonNull ClipDescription description, @Nullable Uri linkUri) {
            mContentUri = contentUri;
            mDescription = description;
            mLinkUri = linkUri;
        }

        @Override
        public @NonNull Uri getContentUri() {
            return mContentUri;
        }

        @Override
        public @NonNull ClipDescription getDescription() {
            return mDescription;
        }

        @Override
        public @Nullable Uri getLinkUri() {
            return mLinkUri;
        }

        @Override
        public @Nullable Object getInputContentInfo() {
            return null;
        }

        @Override
        public void requestPermission() {
        }

        @Override
        public void releasePermission() {
        }
    }

    @RequiresApi(25)
    private static final class InputContentInfoCompatApi25Impl
            implements InputContentInfoCompatImpl {
        final @NonNull InputContentInfo mObject;

        InputContentInfoCompatApi25Impl(@NonNull Object inputContentInfo) {
            mObject = (InputContentInfo) inputContentInfo;
        }

        InputContentInfoCompatApi25Impl(@NonNull Uri contentUri,
                @NonNull ClipDescription description, @Nullable Uri linkUri) {
            mObject = new InputContentInfo(contentUri, description, linkUri);
        }

        @Override
        public @NonNull Uri getContentUri() {
            return mObject.getContentUri();
        }

        @Override
        public @NonNull ClipDescription getDescription() {
            return mObject.getDescription();
        }

        @Override
        public @Nullable Uri getLinkUri() {
            return mObject.getLinkUri();
        }

        @Override
        public @NonNull Object getInputContentInfo() {
            return mObject;
        }

        @Override
        public void requestPermission() {
            mObject.requestPermission();
        }

        @Override
        public void releasePermission() {
            mObject.releasePermission();
        }
    }

    private final InputContentInfoCompatImpl mImpl;

    /**
     * Constructs {@link InputContentInfoCompat}.
     *
     * @param contentUri content URI to be exported from the input method. This cannot be
     *                   {@code null}.
     * @param description a {@link ClipDescription} object that contains the metadata of
     *                    {@code contentUri} such as MIME type(s). This object cannot be
     *                    {@code null}. Also {@link ClipDescription#getLabel()} should be describing
     *                    the content specified by {@code contentUri} for accessibility reasons.
     * @param linkUri an optional {@code http} or {@code https} URI. The editor author may provide
     *                a way to navigate the user to the specified web page if this is not
     *                {@code null}.
     */
    public InputContentInfoCompat(@NonNull Uri contentUri,
            @NonNull ClipDescription description, @Nullable Uri linkUri) {
        if (Build.VERSION.SDK_INT >= 25) {
            mImpl = new InputContentInfoCompatApi25Impl(contentUri, description, linkUri);
        } else {
            mImpl = new InputContentInfoCompatBaseImpl(contentUri, description, linkUri);
        }
    }

    private InputContentInfoCompat(@NonNull InputContentInfoCompatImpl impl) {
        mImpl = impl;
    }

    /**
     * Returns the content URI with which the content can be obtained.
     */
    public @NonNull Uri getContentUri() {
        return mImpl.getContentUri();
    }

    /**
     * Returns the {@link ClipDescription} object that contains the metadata of
     * {@code #getContentUri()} such as MIME type(s). {@link ClipDescription#getLabel()} can be used
     * for accessibility purpose.
     */
    public @NonNull ClipDescription getDescription() {
        return mImpl.getDescription();
    }

    /**
     * Returns an optional {@code http} or {@code https} URI that is related to this content.
     */
    public @Nullable Uri getLinkUri() {
        return mImpl.getLinkUri();
    }

    /**
     * Creates an instance from a framework android.view.inputmethod.InputContentInfo object.
     *
     * <p>This method always returns {@code null} on API &lt;= 24.</p>
     *
     * @param inputContentInfo an android.view.inputmethod.InputContentInfo object, or {@code null}
     *                         if none.
     * @return an equivalent {@link InputContentInfoCompat} object, or {@code null} if not
     * supported.
     */
    public static @Nullable InputContentInfoCompat wrap(@Nullable Object inputContentInfo) {
        if (inputContentInfo == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT < 25) {
            return null;
        }
        return new InputContentInfoCompat(new InputContentInfoCompatApi25Impl(inputContentInfo));
    }

    /**
     * Gets the underlying framework android.view.inputmethod.InputContentInfo object.
     *
     * <p>This method always returns {@code null} on API &lt;= 24.</p>
     *
     * @return an equivalent android.view.inputmethod.InputContentInfo object, or {@code null} if
     * not supported.
     */
    public @Nullable Object unwrap() {
        return mImpl.getInputContentInfo();
    }

    /**
     * Requests a temporary read-only access permission for content URI associated with this object.
     *
     * <p>The lifecycle of the permission granted here is tied to this object instance. If the
     * permission is not released explicitly via {@link #releasePermission()}, it will be
     * released automatically when there are no more references to this object.</p>
     *
     * <p>Does nothing if the temporary permission is already granted.</p>
     */
    public void requestPermission() {
        mImpl.requestPermission();
    }

    /**
     * Releases a temporary read-only access permission for content URI associated with this object.
     *
     * <p>Does nothing if the temporary permission is not granted.</p>
     */
    public void releasePermission() {
        mImpl.releasePermission();
    }
}
