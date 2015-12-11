/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v4.content;

import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;

public final class SharedPreferencesCompat {

    public final static class EditorCompat {

        private static EditorCompat sInstance;

        private interface Helper {
            void apply(@NonNull SharedPreferences.Editor editor);
        }

        private static class EditorHelperBaseImpl implements Helper {

            @Override
            public void apply(@NonNull SharedPreferences.Editor editor) {
                editor.commit();
            }
        }

        private static class EditorHelperApi9Impl implements Helper {

            @Override
            public void apply(@NonNull SharedPreferences.Editor editor) {
                EditorCompatGingerbread.apply(editor);
            }
        }

        private final Helper mHelper;

        private EditorCompat() {
            if (Build.VERSION.SDK_INT >= 9) {
                mHelper = new EditorHelperApi9Impl();
            } else {
                mHelper = new EditorHelperBaseImpl();
            }
        }

        public static EditorCompat getInstance() {
            if (sInstance == null) {
                sInstance = new EditorCompat();
            }
            return sInstance;
        }

        public void apply(@NonNull SharedPreferences.Editor editor) {
            mHelper.apply(editor);
        }
    }

    private SharedPreferencesCompat() {}

}
