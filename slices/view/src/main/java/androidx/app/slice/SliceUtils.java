/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.app.slice;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SliceUtils {

    private SliceUtils() {
    }

    /**
     */
    public static void serializeSlice(Slice s, Context context, OutputStream output,
            String encoding, SerializeOptions options) throws IOException {
        SliceXml.serializeSlice(s, context, output, encoding, options);
    }

    /**
     */
    public static Slice parseSlice(InputStream input, String encoding)
            throws IOException {
        return SliceXml.parseSlice(input, encoding);
    }

    /**
     * Holds options for how to handle SliceItems that cannot be serialized.
     */
    public static class SerializeOptions {
        /**
         * Constant indicating that the an {@link IllegalArgumentException} should be thrown
         * when this format is encountered.
         */
        public static final int MODE_THROW = 0;
        /**
         * Constant indicating that the SliceItem should be removed when this format is encountered.
         */
        public static final int MODE_REMOVE = 1;
        /**
         * Constant indicating that the SliceItem should be serialized as much as possible.
         * <p>
         * For images this means it will be replaced with an empty image. For actions, the
         * action will be removed but the content of the action will be serialized.
         */
        public static final int MODE_DISABLE = 2;

        @IntDef({MODE_THROW, MODE_REMOVE, MODE_DISABLE})
        @interface FormatMode {
        }

        private int mActionMode = MODE_THROW;
        private int mImageMode = MODE_THROW;

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void checkThrow(String format) {
            switch (format) {
                case FORMAT_ACTION:
                case FORMAT_REMOTE_INPUT:
                    if (mActionMode != MODE_THROW) return;
                    break;
                case FORMAT_IMAGE:
                    if (mImageMode != MODE_THROW) return;
                    break;
                default:
                    return;
            }
            throw new IllegalArgumentException(format + " cannot be serialized");
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getActionMode() {
            return mActionMode;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getImageMode() {
            return mImageMode;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_ACTION} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         * @param mode The desired mode.
         */
        public SerializeOptions setActionMode(@FormatMode int mode) {
            mActionMode = mode;
            return this;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_IMAGE} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         * @param mode The desired mode.
         */
        public SerializeOptions setImageMode(@FormatMode int mode) {
            mImageMode = mode;
            return this;
        }
    }
}
