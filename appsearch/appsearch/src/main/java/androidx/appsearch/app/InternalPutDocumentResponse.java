/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.app;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CurrentTimeMillisLong;

/**
 * An internal class for put document response.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InternalPutDocumentResponse {
    private final @CurrentTimeMillisLong long mDocumentExpirationTimestampMillis;

    public InternalPutDocumentResponse(
            @CurrentTimeMillisLong long documentExpirationTimestampMillis) {
        mDocumentExpirationTimestampMillis = documentExpirationTimestampMillis;
    }

    /**
     * Returns the expiration timestamp (in millis) of the new document added by the put document
     * API.
     *
     * <p>The value is in the {@link System#currentTimeMillis} time base.
     *
     * <p>The expiration timestamp is calculated by the document's creation timestamp and
     * time-to-live (TTL).
     *
     * <p>If the TTL was not set, then the document won't expire, so this value will be {@link
     * Long#MAX_VALUE}.
     *
     * @see GenericDocument#getCreationTimestampMillis
     * @see GenericDocument#getTtlMillis()
     */
    @CurrentTimeMillisLong
    public long getDocumentExpirationTimestampMillis() {
        return mDocumentExpirationTimestampMillis;
    }
}
