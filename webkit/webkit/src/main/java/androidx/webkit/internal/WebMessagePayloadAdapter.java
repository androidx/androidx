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

/**
 * Adapter between {@link WebMessageCompat} and {@link WebMessagePayloadBoundaryInterface}.
 * This class is used to pass **payload** of a WebMessageCompat to Chromium.
 */
public class WebMessagePayloadAdapter implements WebMessagePayloadBoundaryInterface {
    private final @NonNull WebMessageCompat mMessageCompat;

    public WebMessagePayloadAdapter(@NonNull WebMessageCompat webMessageCompat) {
        mMessageCompat = webMessageCompat;
    }

    @Override
    @NonNull
    public String[] getSupportedFeatures() {
        // getType, getAsString and getAsArrayBuffer are covered by
        // WEB_MESSAGE_GET_MESSAGE_PAYLOAD.
        return new String[0];
    }

    @Override
    public int getType() {
        switch (mMessageCompat.getType()) {
            case WebMessageCompat.TYPE_STRING:
                return WebMessagePayloadType.TYPE_STRING;
            case WebMessageCompat.TYPE_ARRAY_BUFFER:
                return WebMessagePayloadType.TYPE_ARRAY_BUFFER;
        }
        // Should never happen.
        throw WebViewFeatureInternal.getUnsupportedOperationException();
    }

    @Nullable
    @Override
    public String getAsString() {
        return mMessageCompat.getData();
    }

    @NonNull
    @Override
    public byte[] getAsArrayBuffer() {
        return mMessageCompat.getArrayBuffer();
    }
}
