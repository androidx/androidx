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

import android.annotation.SuppressLint;
import android.content.ClipDescription;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputContentInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

/**
 * Helper for accessing features in {@link InputConnection} introduced after API level 13 in a
 * backwards compatible fashion.
 */
@SuppressLint("PrivateConstructorForUtilityClass") // Already launched with public constructor
public final class InputConnectionCompat {

    private static final String COMMIT_CONTENT_ACTION =
            "androidx.core.view.inputmethod.InputConnectionCompat.COMMIT_CONTENT";
    private static final String COMMIT_CONTENT_INTEROP_ACTION =
            "android.support.v13.view.inputmethod.InputConnectionCompat.COMMIT_CONTENT";
    private static final String COMMIT_CONTENT_CONTENT_URI_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_URI";
    private static final String COMMIT_CONTENT_CONTENT_URI_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_URI";
    private static final String COMMIT_CONTENT_DESCRIPTION_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_DESCRIPTION";
    private static final String COMMIT_CONTENT_DESCRIPTION_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_DESCRIPTION";
    private static final String COMMIT_CONTENT_LINK_URI_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_LINK_URI";
    private static final String COMMIT_CONTENT_LINK_URI_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_LINK_URI";
    private static final String COMMIT_CONTENT_OPTS_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_OPTS";
    private static final String COMMIT_CONTENT_OPTS_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_OPTS";
    private static final String COMMIT_CONTENT_FLAGS_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_FLAGS";
    private static final String COMMIT_CONTENT_FLAGS_INTEROP_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_FLAGS";
    private static final String COMMIT_CONTENT_RESULT_RECEIVER_KEY =
            "androidx.core.view.inputmethod.InputConnectionCompat.CONTENT_RESULT_RECEIVER";
    private static final String COMMIT_CONTENT_RESULT_INTEROP_RECEIVER_KEY =
            "android.support.v13.view.inputmethod.InputConnectionCompat.CONTENT_RESULT_RECEIVER";

    static boolean handlePerformPrivateCommand(
            @Nullable String action,
            @Nullable Bundle data,
            @NonNull OnCommitContentListener onCommitContentListener) {
        if (data == null) {
            return false;
        }

        final boolean interop;
        if (TextUtils.equals(COMMIT_CONTENT_ACTION, action)) {
            interop = false;
        } else if (TextUtils.equals(COMMIT_CONTENT_INTEROP_ACTION, action)) {
            interop = true;
        } else {
            return false;
        }
        ResultReceiver resultReceiver = null;
        boolean result = false;
        try {
            resultReceiver = data.getParcelable(interop
                    ? COMMIT_CONTENT_RESULT_INTEROP_RECEIVER_KEY
                    : COMMIT_CONTENT_RESULT_RECEIVER_KEY);
            final Uri contentUri = data.getParcelable(interop
                    ? COMMIT_CONTENT_CONTENT_URI_INTEROP_KEY
                    : COMMIT_CONTENT_CONTENT_URI_KEY);
            final ClipDescription description = data.getParcelable(interop
                    ? COMMIT_CONTENT_DESCRIPTION_INTEROP_KEY
                    : COMMIT_CONTENT_DESCRIPTION_KEY);
            final Uri linkUri = data.getParcelable(interop
                    ? COMMIT_CONTENT_LINK_URI_INTEROP_KEY
                    : COMMIT_CONTENT_LINK_URI_KEY);
            final int flags = data.getInt(interop
                    ? COMMIT_CONTENT_FLAGS_INTEROP_KEY
                    : COMMIT_CONTENT_FLAGS_KEY);
            final Bundle opts = data.getParcelable(interop
                    ? COMMIT_CONTENT_OPTS_INTEROP_KEY
                    : COMMIT_CONTENT_OPTS_KEY);
            if (contentUri != null && description != null) {
                final InputContentInfoCompat inputContentInfo =
                        new InputContentInfoCompat(contentUri, description, linkUri);
                result = onCommitContentListener.onCommitContent(inputContentInfo, flags, opts);
            }
        } finally {
            if (resultReceiver != null) {
                resultReceiver.send(result ? 1 : 0, null);
            }
        }
        return result;
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

        if (Build.VERSION.SDK_INT >= 25) {
            return inputConnection.commitContent(
                    (InputContentInfo) inputContentInfo.unwrap(), flags, opts);
        } else {
            final int protocol = EditorInfoCompat.getProtocol(editorInfo);
            final boolean interop;
            switch (protocol) {
                case EditorInfoCompat.Protocol.AndroidX_1_0_0:
                case EditorInfoCompat.Protocol.AndroidX_1_1_0:
                    interop = false;
                    break;
                case EditorInfoCompat.Protocol.SupportLib:
                    interop = true;
                    break;
                default:
                    // Must not reach here.
                    return false;
            }

            final Bundle params = new Bundle();
            params.putParcelable(interop
                            ? COMMIT_CONTENT_CONTENT_URI_INTEROP_KEY
                            : COMMIT_CONTENT_CONTENT_URI_KEY,
                    inputContentInfo.getContentUri());
            params.putParcelable(interop
                            ? COMMIT_CONTENT_DESCRIPTION_INTEROP_KEY
                            : COMMIT_CONTENT_DESCRIPTION_KEY,
                    inputContentInfo.getDescription());
            params.putParcelable(interop
                            ? COMMIT_CONTENT_LINK_URI_INTEROP_KEY
                            : COMMIT_CONTENT_LINK_URI_KEY,
                    inputContentInfo.getLinkUri());
            params.putInt(interop
                            ? COMMIT_CONTENT_FLAGS_INTEROP_KEY
                            : COMMIT_CONTENT_FLAGS_KEY,
                    flags);
            params.putParcelable(interop
                            ? COMMIT_CONTENT_OPTS_INTEROP_KEY
                            : COMMIT_CONTENT_OPTS_KEY,
                    opts);
            // TODO: Support COMMIT_CONTENT_RESULT_RECEIVER_KEY.
            return inputConnection.performPrivateCommand(interop
                            ? COMMIT_CONTENT_INTEROP_ACTION
                            : COMMIT_CONTENT_ACTION,
                    params);
        }
    }

    /**
     * When this flag is used, the editor will be able to request temporary access permissions to
     * the content URI contained in the {@link InputContentInfoCompat} object, in a similar manner
     * that has been recommended in
     * <a href="{@docRoot}training/secure-file-sharing/index.html">Sharing Files</a>.
     *
     * <p>Make sure that the content provider owning the Uri sets the
     * {@link android.R.attr#grantUriPermissions grantUriPermissions} attribute in its manifest or
     * included the {@code <grant-uri-permissions>} tag.</p>
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
    public static final int INPUT_CONTENT_GRANT_READ_URI_PERMISSION = 0x00000001;

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
        @SuppressWarnings("NullableProblems") // Not useful here
        boolean onCommitContent(@NonNull InputContentInfoCompat inputContentInfo, int flags,
                @Nullable Bundle opts);
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
        ObjectsCompat.requireNonNull(inputConnection, "inputConnection must be non-null");
        ObjectsCompat.requireNonNull(editorInfo, "editorInfo must be non-null");
        ObjectsCompat.requireNonNull(onCommitContentListener,
                "onCommitContentListener must be non-null");

        if (Build.VERSION.SDK_INT >= 25) {
            final OnCommitContentListener listener = onCommitContentListener;
            return new InputConnectionWrapper(inputConnection, false /* mutable */) {
                @SuppressWarnings("ConstantConditions") // Incorrect warning
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
        } else {
            String[] contentMimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);
            if (contentMimeTypes.length == 0) {
                return inputConnection;
            }
            final OnCommitContentListener listener = onCommitContentListener;
            return new InputConnectionWrapper(inputConnection, false /* mutable */) {
                @Override
                public boolean performPrivateCommand(String action, Bundle data) {
                    if (InputConnectionCompat.handlePerformPrivateCommand(action, data, listener)) {
                        return true;
                    }
                    return super.performPrivateCommand(action, data);
                }
            };
        }
    }

    /** @deprecated This type should not be instantiated as it contains only static methods. */
    @Deprecated
    public InputConnectionCompat() {
    }
}
