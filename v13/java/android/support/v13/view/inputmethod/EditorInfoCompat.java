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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.inputmethod.EditorInfo;

/**
 * Helper for accessing features in {@link EditorInfo} introduced after API level 13 in a backwards
 * compatible fashion.
 */
public final class EditorInfoCompat {

    /**
     * Flag of {@link EditorInfo#imeOptions}: used to request that the IME does not update any
     * personalized data such as typing history and personalized language model based on what the
     * user typed on this text editing object.  Typical use cases are:
     * <ul>
     *     <li>When the application is in a special mode, where user's activities are expected to be
     *     not recorded in the application's history.  Some web browsers and chat applications may
     *     have this kind of modes.</li>
     *     <li>When storing typing history does not make much sense.  Specifying this flag in typing
     *     games may help to avoid typing history from being filled up with words that the user is
     *     less likely to type in their daily life.  Another example is that when the application
     *     already knows that the expected input is not a valid word (e.g. a promotion code that is
     *     not a valid word in any natural language).</li>
     * </ul>
     *
     * <p>Applications need to be aware that the flag is not a guarantee, and some IMEs may not
     * respect it.</p>
     */
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 0x1000000;

    /**
     * Flag of {@link EditorInfo#imeOptions}: used to request an IME that is capable of inputting
     * ASCII characters.
     *
     * <p>The intention of this flag is to ensure that the user can type Roman alphabet characters
     * in a {@link android.widget.TextView}. It is typically used for an account ID or password
     * input.</p>
     *
     * <p>In many cases, IMEs are already able to input ASCII even without being told so (such IMEs
     * already respect this flag in a sense), but there are cases when this is not the default. For
     * instance, users of languages using a different script like Arabic, Greek, Hebrew or Russian
     * typically have a keyboard that can't input ASCII characters by default.</p>
     *
     * <p>Applications need to be aware that the flag is not a guarantee, and some IMEs may not
     * respect it. However, it is strongly recommended for IME authors to respect this flag
     * especially when their IME could end up with a state where only languages using non-ASCII are
     * enabled.</p>
     */
    public static final int IME_FLAG_FORCE_ASCII = 0x80000000;

    private interface EditorInfoCompatImpl {
        void setContentMimeTypes(@NonNull EditorInfo editorInfo,
                @Nullable String[] contentMimeTypes);
        @NonNull
        String[] getContentMimeTypes(@NonNull EditorInfo editorInfo);
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final class EditorInfoCompatBaseImpl implements EditorInfoCompatImpl {
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

    @RequiresApi(25)
    private static final class EditorInfoCompatApi25Impl implements EditorInfoCompatImpl {
        @Override
        public void setContentMimeTypes(@NonNull EditorInfo editorInfo,
                @Nullable String[] contentMimeTypes) {
            editorInfo.contentMimeTypes = contentMimeTypes;
        }

        @NonNull
        @Override
        public String[] getContentMimeTypes(@NonNull EditorInfo editorInfo) {
            final String[] result = editorInfo.contentMimeTypes;
            return result != null ? result : EMPTY_STRING_ARRAY;
        }
    }

    private static final EditorInfoCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 25) {
            IMPL = new EditorInfoCompatApi25Impl();
        } else {
            IMPL = new EditorInfoCompatBaseImpl();
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
     *                         InputConnection, EditorInfo, InputContentInfoCompat, int, Bundle)}
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
