/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.webkit.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebMessageCompat;

import org.chromium.support_lib_boundary.WebMessagePayloadBoundaryInterface;

import java.util.Objects;

/**
 * Adapter between {@link WebMessageCompat} and {@link WebMessagePayloadBoundaryInterface}.
 * This class is used to pass **payload** of a WebMessageCompat to Chromium.
 */
public class WebMessagePayloadAdapter implements WebMessagePayloadBoundaryInterface {
    private final @WebMessagePayloadType int mType;
    private final @Nullable String mString;
    private final @Nullable byte[] mArrayBuffer;

    public WebMessagePayloadAdapter(@Nullable final String data) {
        mType = WebMessageCompat.TYPE_STRING;
        mString = data;
        mArrayBuffer = null;
    }

    public WebMessagePayloadAdapter(@NonNull final byte[] arrayBuffer) {
        mType = WebMessageCompat.TYPE_ARRAY_BUFFER;
        mString = null;
        mArrayBuffer = arrayBuffer;
    }

    @Override
    @NonNull
    public String[] getSupportedFeatures() {
        // getType, getAsString and getAsArrayBuffer are covered by
        // WEB_MESSAGE_ARRAY_BUFFER.
        return new String[0];
    }

    @Override
    public int getType() {
        return mType;
    }

    @Nullable
    @Override
    public String getAsString() {
        checkType(WebMessagePayloadType.TYPE_STRING);
        return mString;
    }

    @NonNull
    @Override
    public byte[] getAsArrayBuffer() {
        checkType(WebMessagePayloadType.TYPE_ARRAY_BUFFER);
        return Objects.requireNonNull(mArrayBuffer);
    }

    /**
     * Check if current message payload type is the {@code expectedType}.
     */
    private void checkType(@WebMessagePayloadType int expectedType) {
        if (mType != expectedType) {
            throw new IllegalStateException("Expected " + expectedType + ", but type is " + mType);
        }
    }

}
