/**
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

package android.support.v13.view.inputmethod;

import android.content.ClipDescription;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.BuildCompat;

/**
 * Helper for accessing features in InputContentInfo introduced after API level 13 in a backwards
 * compatible fashion.
 */
public final class InputContentInfoCompat {

    private interface InputContentInfoCompatImpl {
        @NonNull
        Uri getContentUri();

        @NonNull
        ClipDescription getDescription();

        @Nullable
        Uri getLinkUri();

        @Nullable
        Object getInputContentInfo();

        void requestPermission();

        void releasePermission();
    }

    private final static class BaseInputContentInfoCompatImpl
            implements InputContentInfoCompatImpl {
        @NonNull
        private final Uri mContentUri;
        @NonNull
        private final ClipDescription mDescription;
        @Nullable
        private final Uri mLinkUri;

        public BaseInputContentInfoCompatImpl(@NonNull Uri contentUri,
                @NonNull ClipDescription description, @Nullable Uri linkUri) {
            mContentUri = contentUri;
            mDescription = description;
            mLinkUri = linkUri;
        }

        @NonNull
        @Override
        public Uri getContentUri() {
            return mContentUri;
        }

        @NonNull
        @Override
        public ClipDescription getDescription() {
            return mDescription;
        }

        @Nullable
        @Override
        public Uri getLinkUri() {
            return mLinkUri;
        }

        @Nullable
        @Override
        public Object getInputContentInfo() {
            return null;
        }

        @Override
        public void requestPermission() {
            return;
        }

        @Override
        public void releasePermission() {
            return;
        }
    }

    private final static class Api25InputContentInfoCompatImpl
            implements InputContentInfoCompatImpl {
        @NonNull
        final Object mObject;

        public Api25InputContentInfoCompatImpl(@NonNull Object inputContentInfo) {
            mObject = inputContentInfo;
        }

        public Api25InputContentInfoCompatImpl(@NonNull Uri contentUri,
                @NonNull ClipDescription description, @Nullable Uri linkUri) {
            mObject = InputContentInfoCompatApi25.create(contentUri, description, linkUri);
        }

        @Override
        @NonNull
        public Uri getContentUri() {
            return InputContentInfoCompatApi25.getContentUri(mObject);
        }

        @Override
        @NonNull
        public ClipDescription getDescription() {
            return InputContentInfoCompatApi25.getDescription(mObject);
        }

        @Override
        @Nullable
        public Uri getLinkUri() {
            return InputContentInfoCompatApi25.getLinkUri(mObject);
        }

        @Override
        @Nullable
        public Object getInputContentInfo() {
            return mObject;
        }

        @Override
        public void requestPermission() {
            InputContentInfoCompatApi25.requestPermission(mObject);
        }

        @Override
        public void releasePermission() {
            InputContentInfoCompatApi25.releasePermission(mObject);
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
        if (BuildCompat.isAtLeastNMR1()) {
            mImpl = new Api25InputContentInfoCompatImpl(contentUri, description, linkUri);
        } else {
            mImpl = new BaseInputContentInfoCompatImpl(contentUri, description, linkUri);
        }
    }

    private InputContentInfoCompat(@NonNull InputContentInfoCompatImpl impl) {
        mImpl = impl;
    }

    /**
     * @return content URI with which the content can be obtained.
     */
    @NonNull
    public Uri getContentUri() {
        return mImpl.getContentUri();
    }

    /**
     * @return {@link ClipDescription} object that contains the metadata of {@code #getContentUri()}
     * such as MIME type(s). {@link ClipDescription#getLabel()} can be used for accessibility
     * purpose.
     */
    @NonNull
    public ClipDescription getDescription() {
        return mImpl.getDescription();
    }

    /**
     * @return an optional {@code http} or {@code https} URI that is related to this content.
     */
    @Nullable
    public Uri getLinkUri() {
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
    @Nullable
    public static InputContentInfoCompat wrap(@Nullable Object inputContentInfo) {
        if (inputContentInfo == null) {
            return null;
        }
        if (!BuildCompat.isAtLeastNMR1()) {
            return null;
        }
        return new InputContentInfoCompat(new Api25InputContentInfoCompatImpl(inputContentInfo));
    }

    /**
     * Gets the underlying framework android.view.inputmethod.InputContentInfo object.
     *
     * <p>This method always returns {@code null} on API &lt;= 24.</p>
     *
     * @return an equivalent android.view.inputmethod.InputContentInfo object, or {@code null} if
     * not supported.
     */
    @Nullable
    public Object unwrap() {
        return mImpl.getInputContentInfo();
    }

    /**
     * Requests a temporary read-only access permission for content URI associated with this object.
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
