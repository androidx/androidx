/*
 * Copyright 2019 The Android Open Source Project
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

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

final class EditorInfoTestUtils {
    private static final String CONTENT_MIME_TYPES_KEY =
            "androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";
    private static final String CONTENT_MIME_TYPES_INTEROP_KEY =
            "android.support.v13.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES";

    static EditorInfo createEditorInfoForTest(String[] mimeTypes, int protocol) {
        EditorInfo editorInfo = new EditorInfo();
        if (editorInfo.extras == null) {
            editorInfo.extras = new Bundle();
        }
        switch (protocol) {
            case EditorInfoCompat.Protocol.SupportLib:
                editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_INTEROP_KEY, mimeTypes);
                break;
            case EditorInfoCompat.Protocol.AndroidX_1_0_0:
                editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_KEY, mimeTypes);
                break;
            case EditorInfoCompat.Protocol.AndroidX_1_1_0:
                editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_INTEROP_KEY, mimeTypes);
                editorInfo.extras.putStringArray(CONTENT_MIME_TYPES_KEY, mimeTypes);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported protocol=" + protocol);
        }
        return editorInfo;
    }
}
