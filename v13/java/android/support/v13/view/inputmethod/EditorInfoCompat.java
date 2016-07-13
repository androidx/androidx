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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.BuildCompat;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public final class EditorInfoCompat {

    private interface EditorInfoCompatImpl {
        void setContentMimeTypes(@NonNull EditorInfo editorInfo,
                @Nullable String[] contentMimeTypes);
        @NonNull
        String[] getContentMimeTypes(@NonNull EditorInfo editorInfo);
    }

    private final static String[] EMPTY_STRING_ARRAY = new String[0];

    private final static class BaseEditorInfoCompatImpl implements EditorInfoCompatImpl {
        private static String CONTENT_MIME_TYPES_KEY =
                "android.support.v13.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";

        @Override
        public void setContentMimeTypes(@NonNull EditorInfo editorInfo,
                @Nullable String[] contentMimeTypes) {
            if (editorInfo.extras == null) {
                editorInfo.extras = new Bundle();
            }
            editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_KEY, contentMimeTypes);
        }

        @NonNull
        @Override
        public String[] getContentMimeTypes(@NonNull EditorInfo editorInfo) {
            if (editorInfo.extras == null) {
                return EMPTY_STRING_ARRAY;
            }
            String[] result = editorInfo.extras.getStringArray(CONTENT_MIME_TYPES_KEY);
            return result != null ? result : EMPTY_STRING_ARRAY;
        }
    }

    private final static class Api25EditorInfoCompatImpl implements EditorInfoCompatImpl {
        @Override
        public void setContentMimeTypes(@NonNull EditorInfo editorInfo,
                @Nullable String[] contentMimeTypes) {
            EditorInfoCompatApi25.setContentMimeTypes(editorInfo, contentMimeTypes);
        }

        @NonNull
        @Override
        public String[] getContentMimeTypes(@NonNull EditorInfo editorInfo) {
            String[] result = EditorInfoCompatApi25.getContentMimeTypes(editorInfo);
            return result != null ? result : EMPTY_STRING_ARRAY;
        }
    }

    private static final EditorInfoCompatImpl IMPL;
    static {
        if (BuildCompat.isAtLeastNMR1()) {
            IMPL = new Api25EditorInfoCompatImpl();
        } else {
            IMPL = new BaseEditorInfoCompatImpl();
        }
    }

    /**
     * Sets MIME types that can be accepted by the target editor if the IME calls
     * {@link InputConnectionCompat#commitContent(InputConnection, EditorInfo,
     * InputContentInfoCompat, int, Bundle)}.
     *
     * @param editorInfo the editor with which we associate supported MIME types
     * @param contentMimeTypes an array of MIME types. {@code null} and an empty array means that
     *                         {@link InputConnectionCompat#commitContent(
     *                         InputConnection, EditorInfo, InputContentInfoCompat, int, Bundle)
     *                         is not supported on this Editor
     */
    public static void setContentMimeTypes(@NonNull EditorInfo editorInfo,
            @Nullable String[] contentMimeTypes) {
        IMPL.setContentMimeTypes(editorInfo, contentMimeTypes);
    }

    /**
     * Gets MIME types that can be accepted by the target editor if the IME calls
     * {@link InputConnectionCompat#commitContent(InputConnection, EditorInfo,
     * InputContentInfoCompat, int, Bundle)}
     *
     * @param editorInfo the editor from which we get the MIME types
     * @return an array of MIME types. An empty array means that {@link
     * InputConnectionCompat#commitContent(InputConnection, EditorInfo, InputContentInfoCompat,
     * int, Bundle)} is not supported on this editor
     */
    @NonNull
    public static String[] getContentMimeTypes(EditorInfo editorInfo) {
        return IMPL.getContentMimeTypes(editorInfo);
    }

}
