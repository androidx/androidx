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

import android.support.annotation.RequiresApi;
import android.content.ClipDescription;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputContentInfo;

/**
 * Helper for accessing features in {@link InputConnection} introduced after API level 13 in a
 * backwards compatible fashion.
 */
public final class InputConnectionCompat {

    private interface InputConnectionCompatImpl {
        boolean commitContent(@NonNull InputConnection inputConnection,
                @NonNull InputContentInfoCompat inputContentInfo, int flags, @Nullable Bundle opts);

        @NonNull
        InputConnection createWrapper(@NonNull InputConnection ic,
                @NonNull EditorInfo editorInfo, @NonNull OnCommitContentListener callback);
    }

    static final class InputContentInfoCompatBaseImpl implements InputConnectionCompatImpl {

        private static String COMMIT_CONTENT_ACTION =
                "android.support.v13.view.inputmethod.InputConnectionCompat.COMMIT_CONTENT";
        private static String COMMIT_CONTENT_CONTENT_URI_KEY =
                "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_URI";
        private static String COMMIT_CONTENT_DESCRIPTION_KEY =
                "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_DESCRIPTION";
        private static String COMMIT_CONTENT_LINK_URI_KEY =
                "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_LINK_URI";
        private static String COMMIT_CONTENT_OPTS_KEY =
                "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_OPTS";
        private static String COMMIT_CONTENT_FLAGS_KEY =
                "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_FLAGS";
        private static String COMMIT_CONTENT_RESULT_RECEIVER =
                "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_RESULT_RECEIVER";

        @Override
        public boolean commitContent(@NonNull InputConnection inputConnection,
                @NonNull InputContentInfoCompat inputContentInfo, int flags,
                @Nullable Bundle opts) {
            final Bundle params = new Bundle();
            params.putParcelable(COMMIT_CONTENT_CONTENT_URI_KEY, inputContentInfo.getContentUri());
            params.putParcelable(COMMIT_CONTENT_DESCRIPTION_KEY, inputContentInfo.getDescription());
            params.putParcelable(COMMIT_CONTENT_LINK_URI_KEY, inputContentInfo.getLinkUri());
            params.putInt(COMMIT_CONTENT_FLAGS_KEY, flags);
            params.putParcelable(COMMIT_CONTENT_OPTS_KEY, opts);
            // TODO: Support COMMIT_CONTENT_RESULT_RECEIVER.
            return inputConnection.performPrivateCommand(COMMIT_CONTENT_ACTION, params);
        }

        @NonNull
        @Override
        public InputConnection createWrapper(@NonNull InputConnection ic,
                @NonNull EditorInfo editorInfo,
                @NonNull OnCommitContentListener onCommitContentListener) {
            String[] contentMimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);
            if (contentMimeTypes.length == 0) {
                return ic;
            }
            final OnCommitContentListener listener = onCommitContentListener;
            return new InputConnectionWrapper(ic, false /* mutable */) {
                @Override
                public boolean performPrivateCommand(String action, Bundle data) {
                    if (InputContentInfoCompatBaseImpl.handlePerformPrivateCommand(action, data,
                            listener)) {
                        return true;
                    }
                    return super.performPrivateCommand(action, data);
                }
            };
        }

        static boolean handlePerformPrivateCommand(
                @Nullable String action,
                @NonNull Bundle data,
                @NonNull OnCommitContentListener onCommitContentListener) {
            if (!TextUtils.equals(COMMIT_CONTENT_ACTION, action)) {
                return false;
            }
            if (data == null) {
                return false;
            }
            ResultReceiver resultReceiver = null;
            boolean result = false;
            try {
                resultReceiver = data.getParcelable(COMMIT_CONTENT_RESULT_RECEIVER);
                final Uri contentUri = data.getParcelable(COMMIT_CONTENT_CONTENT_URI_KEY);
                final ClipDescription description = data.getParcelable(
                        COMMIT_CONTENT_DESCRIPTION_KEY);
                final Uri linkUri = data.getParcelable(COMMIT_CONTENT_LINK_URI_KEY);
                final int flags = data.getInt(COMMIT_CONTENT_FLAGS_KEY);
                final Bundle opts = data.getParcelable(COMMIT_CONTENT_OPTS_KEY);
                final InputContentInfoCompat inputContentInfo =
                        new InputContentInfoCompat(contentUri, description, linkUri);
                result = onCommitContentListener.onCommitContent(inputContentInfo, flags, opts);
            } finally {
                if (resultReceiver != null) {
                    resultReceiver.send(result ? 1 : 0, null);
                }
            }
            return result;
        }
    }

    @RequiresApi(25)
    private static final class InputContentInfoCompatApi25Impl
            implements InputConnectionCompatImpl {
        @Override
        public boolean commitContent(@NonNull InputConnection inputConnection,
                @NonNull InputContentInfoCompat inputContentInfo, int flags,
                @Nullable Bundle opts) {
            return inputConnection.commitContent((InputContentInfo) inputContentInfo.unwrap(),
                    flags, opts);
        }

        @Nullable
        @Override
        public InputConnection createWrapper(
                @Nullable InputConnection inputConnection, @NonNull EditorInfo editorInfo,
                @Nullable OnCommitContentListener onCommitContentListener) {
            final OnCommitContentListener listener = onCommitContentListener;
            return new InputConnectionWrapper(inputConnection, false /* mutable */) {
                @Override
                public boolean commitContent(InputContentInfo inputContentInfo, int flags,
                        Bundle opts) {
                    if (listener.onCommitContent(InputContentInfoCompat.wrap(inputContentInfo),
                            flags, opts)) {
                        return true;
                    }
                    return super.commitContent(inputContentInfo, flags, opts);
                }
            };
        }
    }

    private static final InputConnectionCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 25) {
            IMPL = new InputContentInfoCompatApi25Impl();
        } else {
            IMPL = new InputContentInfoCompatBaseImpl();
        }
    }

    /**
     * Calls commitContent API, in a backwards compatible fashion.
     *
     * @param inputConnection {@link InputConnection} with which commitContent API will be called
     * @param editorInfo {@link EditorInfo} associated with the given {@code inputConnection}
     * @param inputContentInfo content information to be passed to the editor
     * @param flags {@code 0} or {@link #INPUT_CONTENT_GRANT_READ_URI_PERMISSION}
     * @param opts optional bundle data. This can be {@code null}
     * @return {@code true} if this request is accepted by the application, no matter if the request
     * is already handled or still being handled in background
     */
    public static boolean commitContent(@NonNull InputConnection inputConnection,
            @NonNull EditorInfo editorInfo, @NonNull InputContentInfoCompat inputContentInfo,
            int flags, @Nullable Bundle opts) {
        final ClipDescription description = inputContentInfo.getDescription();
        boolean supported = false;
        for (String mimeType : EditorInfoCompat.getContentMimeTypes(editorInfo)) {
            if (description.hasMimeType(mimeType)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            return false;
        }

        return IMPL.commitContent(inputConnection, inputContentInfo, flags, opts);
    }

    /**
     * When this flag is used, the editor will be able to request temporary access permissions to
     * the content URI contained in the {@link InputContentInfoCompat} object, in a similar manner
     * that has been recommended in
     * <a href="{@docRoot}training/secure-file-sharing/index.html">Sharing Files</a>.
     *
     * <p>Make sure that the content provider owning the Uri sets the
     * {@link android.R.attr#grantUriPermissions grantUriPermissions} attribute in its manifest or
     * included the {@code &lt;grant-uri-permissions&gt;} tag.</p>
     *
     * <p>Supported only on API &gt;= 25.</p>
     *
     * <p>On API &lt;= 24 devices, IME developers need to ensure that the content URI is accessible
     * only from the target application, for example, by generating a URL with a unique name that
     * others cannot guess. IME developers can also rely on the following information of the target
     * application to do additional access checks in their {@link android.content.ContentProvider}.
     * </p>
     * <ul>
     *     <li>On API &gt;= 23 {@link EditorInfo#packageName} is guaranteed to not be spoofed, which
     *     can later be compared with {@link android.content.ContentProvider#getCallingPackage()} in
     *     the {@link android.content.ContentProvider}.
     *     </li>
     *     <li>{@link android.view.inputmethod.InputBinding#getUid()} is guaranteed to not be
     *     spoofed, which can later be compared with {@link android.os.Binder#getCallingUid()} in
     *     the {@link android.content.ContentProvider}.</li>
     * </ul>
     */
    public static int INPUT_CONTENT_GRANT_READ_URI_PERMISSION = 0x00000001;

    /**
     * Listener for commitContent method call, in a backwards compatible fashion.
     */
    public interface OnCommitContentListener {
        /**
         * Intercepts InputConnection#commitContent API calls.
         *
         * @param inputContentInfo content to be committed
         * @param flags {@code 0} or {@link #INPUT_CONTENT_GRANT_READ_URI_PERMISSION}
         * @param opts optional bundle data. This can be {@code null}
         * @return {@code true} if this request is accepted by the application, no matter if the
         * request is already handled or still being handled in background. {@code false} to use the
         * default implementation
         */
        boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts);
    }

    /**
     * Creates a wrapper {@link InputConnection} object from an existing {@link InputConnection}
     * and {@link OnCommitContentListener} that can be returned to the system.
     *
     * <p>By returning the wrapper object to the IME, the editor can be notified by
     * {@link OnCommitContentListener#onCommitContent(InputContentInfoCompat, int, Bundle)}
     * when the IME calls
     * {@link InputConnectionCompat#commitContent(InputConnection, EditorInfo,
     * InputContentInfoCompat, int, Bundle)} and the corresponding Framework API that is available
     * on API &gt;= 25.</p>
     *
     * @param inputConnection {@link InputConnection} to be wrapped
     * @param editorInfo {@link EditorInfo} associated with the given {@code inputConnection}
     * @param onCommitContentListener the listener that the wrapper object will call
     * @return a wrapper {@link InputConnection} object that can be returned to the IME
     * @throws IllegalArgumentException when {@code inputConnection}, {@code editorInfo}, or
     * {@code onCommitContentListener} is {@code null}
     */
    @NonNull
    public static InputConnection createWrapper(@NonNull InputConnection inputConnection,
            @NonNull EditorInfo editorInfo,
            @NonNull OnCommitContentListener onCommitContentListener) {
        if (inputConnection == null) {
            throw new IllegalArgumentException("inputConnection must be non-null");
        }
        if (editorInfo == null) {
            throw new IllegalArgumentException("editorInfo must be non-null");
        }
        if (onCommitContentListener == null) {
            throw new IllegalArgumentException("onCommitContentListener must be non-null");
        }
        return IMPL.createWrapper(inputConnection, editorInfo, onCommitContentListener);
    }

}
