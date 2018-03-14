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

package com.example.android.supportv4.view.inputmethod;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.example.android.supportv4.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Demo activity for using {@link InputConnectionCompat}.
 */
public class CommitContentSupport extends Activity {
    private static final String INPUT_CONTENT_INFO_KEY = "COMMIT_CONTENT_INPUT_CONTENT_INFO";
    private static final String COMMIT_CONTENT_FLAGS_KEY = "COMMIT_CONTENT_FLAGS";
    private static final String TAG = "CommitContentSupport";

    private WebView mWebView;
    private TextView mLabel;
    private TextView mContentUri;
    private TextView mLinkUri;
    private TextView mMimeTypes;
    private TextView mFlags;

    private InputContentInfoCompat mCurrentInputContentInfo;
    private int mCurrentFlags;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.commit_content);

        final LinearLayout layout =
                findViewById(R.id.commit_content_sample_edit_boxes);

        // This declares that the IME cannot commit any content with
        // InputConnectionCompat#commitContent().
        layout.addView(createEditTextWithContentMimeTypes(null));

        // This declares that the IME can commit contents with
        // InputConnectionCompat#commitContent() if they match "image/gif".
        layout.addView(createEditTextWithContentMimeTypes(new String[]{"image/gif"}));

        // This declares that the IME can commit contents with
        // InputConnectionCompat#commitContent() if they match "image/png".
        layout.addView(createEditTextWithContentMimeTypes(new String[]{"image/png"}));

        // This declares that the IME can commit contents with
        // InputConnectionCompat#commitContent() if they match "image/jpeg".
        layout.addView(createEditTextWithContentMimeTypes(new String[]{"image/jpeg"}));

        // This declares that the IME can commit contents with
        // InputConnectionCompat#commitContent() if they match "image/webp".
        layout.addView(createEditTextWithContentMimeTypes(new String[]{"image/webp"}));

        // This declares that the IME can commit contents with
        // InputConnectionCompat#commitContent() if they match "image/png", "image/gif",
        // "image/jpeg", or "image/webp".
        layout.addView(createEditTextWithContentMimeTypes(
                new String[]{"image/png", "image/gif", "image/jpeg", "image/webp"}));

        mWebView = findViewById(R.id.commit_content_webview);
        mMimeTypes = findViewById(R.id.text_commit_content_mime_types);
        mLabel = findViewById(R.id.text_commit_content_label);
        mContentUri = findViewById(R.id.text_commit_content_content_uri);
        mLinkUri = findViewById(R.id.text_commit_content_link_uri);
        mFlags = findViewById(R.id.text_commit_content_link_flags);

        if (savedInstanceState != null) {
            final InputContentInfoCompat previousInputContentInfo = InputContentInfoCompat.wrap(
                    savedInstanceState.getParcelable(INPUT_CONTENT_INFO_KEY));
            final int previousFlags = savedInstanceState.getInt(COMMIT_CONTENT_FLAGS_KEY);
            if (previousInputContentInfo != null) {
                onCommitContentInternal(previousInputContentInfo, previousFlags);
            }
        }
    }

    private boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags,
            String[] contentMimeTypes) {
        // Clear the temporary permission (if any).  See below about why we do this here.
        try {
            if (mCurrentInputContentInfo != null) {
                mCurrentInputContentInfo.releasePermission();
            }
        } catch (Exception e) {
            Log.e(TAG, "InputContentInfoCompat#releasePermission() failed.", e);
        } finally {
            mCurrentInputContentInfo = null;
        }

        mWebView.loadUrl("about:blank");
        mMimeTypes.setText("");
        mContentUri.setText("");
        mLabel.setText("");
        mLinkUri.setText("");
        mFlags.setText("");

        boolean supported = false;
        for (final String mimeType : contentMimeTypes) {
            if (inputContentInfo.getDescription().hasMimeType(mimeType)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            return false;
        }

        return onCommitContentInternal(inputContentInfo, flags);
    }

    private boolean onCommitContentInternal(InputContentInfoCompat inputContentInfo, int flags) {
        if ((flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
            try {
                inputContentInfo.requestPermission();
            } catch (Exception e) {
                Log.e(TAG, "InputContentInfoCompat#requestPermission() failed.", e);
                return false;
            }
        }

        mMimeTypes.setText(
                Arrays.toString(inputContentInfo.getDescription().filterMimeTypes("*/*")));
        mContentUri.setText(inputContentInfo.getContentUri().toString());
        mLabel.setText(inputContentInfo.getDescription().getLabel());
        Uri linkUri = inputContentInfo.getLinkUri();
        mLinkUri.setText(linkUri != null ? linkUri.toString() : "null");
        mFlags.setText(flagsToString(flags));
        mWebView.loadUrl(inputContentInfo.getContentUri().toString());
        mWebView.setBackgroundColor(Color.TRANSPARENT);

        // Due to the asynchronous nature of WebView, it is a bit too early to call
        // inputContentInfo.releasePermission() here. Hence we call IC#releasePermission() when this
        // method is called next time.  Note that calling IC#releasePermission() is just to be a
        // good citizen. Even if we failed to call that method, the system would eventually revoke
        // the permission sometime after inputContentInfo object gets garbage-collected.
        mCurrentInputContentInfo = inputContentInfo;
        mCurrentFlags = flags;

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (mCurrentInputContentInfo != null) {
            savedInstanceState.putParcelable(INPUT_CONTENT_INFO_KEY,
                    (Parcelable) mCurrentInputContentInfo.unwrap());
            savedInstanceState.putInt(COMMIT_CONTENT_FLAGS_KEY, mCurrentFlags);
        }
        mCurrentInputContentInfo = null;
        mCurrentFlags = 0;
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Creates a new instance of {@link EditText} that is configured to specify the given content
     * MIME types to {@link EditorInfo#contentMimeTypes} so that developers
     * can locally test how the current input method behaves for such content MIME types.
     *
     * @param contentMimeTypes A {@link String} array that indicates the supported content MIME
     *                         types
     * @return a new instance of {@link EditText}, which specifies
     * {@link EditorInfo#contentMimeTypes} with the given content
     * MIME types
     */
    private EditText createEditTextWithContentMimeTypes(String[] contentMimeTypes) {
        final CharSequence hintText;
        final String[] mimeTypes;  // our own copy of contentMimeTypes.
        if (contentMimeTypes == null || contentMimeTypes.length == 0) {
            hintText = "MIME: []";
            mimeTypes = new String[0];
        } else {
            hintText = "MIME: " + Arrays.toString(contentMimeTypes);
            mimeTypes = Arrays.copyOf(contentMimeTypes, contentMimeTypes.length);
        }
        EditText exitText = new EditText(this) {
            @Override
            public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
                final InputConnection ic = super.onCreateInputConnection(editorInfo);
                EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes);
                final InputConnectionCompat.OnCommitContentListener callback =
                        (inputContentInfo, flags, opts) ->
                                CommitContentSupport.this.onCommitContent(
                                        inputContentInfo, flags, mimeTypes);
                return InputConnectionCompat.createWrapper(ic, editorInfo, callback);
            }
        };
        exitText.setHint(hintText);
        exitText.setTextColor(Color.WHITE);
        exitText.setHintTextColor(Color.WHITE);
        return exitText;
    }

    /**
     * Converts {@code flags} specified in {@link InputConnectionCompat#commitContent(
     *InputConnection, EditorInfo, InputContentInfoCompat, int, Bundle)} to a human readable
     * string.
     *
     * @param flags the 2nd parameter of
     *              {@link InputConnectionCompat#commitContent(InputConnection, EditorInfo,
     *              InputContentInfoCompat, int, Bundle)}
     * @return a human readable string that corresponds to the given {@code flags}
     */
    private static String flagsToString(int flags) {
        final ArrayList<String> tokens = new ArrayList<>();
        if ((flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
            tokens.add("INPUT_CONTENT_GRANT_READ_URI_PERMISSION");
            flags &= ~InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
        }
        if (flags != 0) {
            tokens.add("0x" + Integer.toHexString(flags));
        }
        return TextUtils.join(" | ", tokens);
    }

}
