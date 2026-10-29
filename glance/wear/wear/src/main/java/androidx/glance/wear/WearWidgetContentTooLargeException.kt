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

package androidx.glance.wear

/**
 * Thrown when the serialized content of a widget is too large to be sent to the host.
 *
 * This is usually caused by large bitmaps in `RemoteImage`, which are embedded losslessly at full
 * resolution in the widget content. To avoid this, scale bitmaps down to their displayed size (e.g.
 * using [android.graphics.Bitmap.createScaledBitmap]) or remove unnecessary images.
 *
 * @property contentSizeBytes the size of the serialized widget content, in bytes.
 */
public class WearWidgetContentTooLargeException
internal constructor(public val contentSizeBytes: Int) :
    RuntimeException(
        "Wear widget content is $contentSizeBytes bytes, which exceeds the maximum of " +
            "$MAX_CONTENT_SIZE_BYTES bytes, so it can't be sent to the host. This is usually " +
            "caused by large bitmaps in RemoteImage: they are embedded losslessly at full " +
            "resolution. Scale them down to their displayed size (e.g. " +
            "Bitmap.createScaledBitmap) or remove unnecessary images."
    ) {

    public companion object {
        /**
         * The maximum size of the serialized widget content that can be sent to the host, in bytes
         * (400 KB).
         *
         * The content is sent inline over oneway Binder calls, whose ~508 KB buffer is shared with
         * other transactions in flight. This limit leaves headroom for those and for the extras and
         * parcel overhead.
         */
        public const val MAX_CONTENT_SIZE_BYTES: Int = 400 * 1024

        /**
         * The size of the serialized widget content, in bytes (300 KB), above which a warning is
         * logged because it is approaching [MAX_CONTENT_SIZE_BYTES].
         */
        internal const val WARNING_CONTENT_SIZE_BYTES: Int = 300 * 1024
    }
}
