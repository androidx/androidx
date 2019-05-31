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

package androidx.core.net;

import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * Helper for accessing function in {@link Uri} in a backwards compatible fashion.
 */
public final class UriCompat {

    private UriCompat() {}

    /**
     * Return a string representation of this URI that has common forms of PII redacted,
     * making it safer to use for logging purposes.  For example, {@code tel:800-466-4411} is
     * returned as {@code tel:xxx-xxx-xxxx} and {@code http://example.com/path/to/item/} is
     * returned as {@code http://example.com/...}.
     *
     * @param uri The uri for converted to string.
     * @return Return a string representation of this URI that has common forms of PII redacted.
     */
    public static @NonNull String toSafeString(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        String ssp = uri.getSchemeSpecificPart();
        if (scheme != null) {
            if (scheme.equalsIgnoreCase("tel") || scheme.equalsIgnoreCase("sip")
                    || scheme.equalsIgnoreCase("sms") || scheme.equalsIgnoreCase("smsto")
                    || scheme.equalsIgnoreCase("mailto") || scheme.equalsIgnoreCase("nfc")) {
                StringBuilder builder = new StringBuilder(64);
                builder.append(scheme);
                builder.append(':');
                if (ssp != null) {
                    for (int i = 0; i < ssp.length(); i++) {
                        char c = ssp.charAt(i);
                        if (c == '-' || c == '@' || c == '.') {
                            builder.append(c);
                        } else {
                            builder.append('x');
                        }
                    }
                }
                return builder.toString();
            } else if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")
                    || scheme.equalsIgnoreCase("ftp") || scheme.equalsIgnoreCase("rtsp")) {
                ssp = "//" + ((uri.getHost() != null) ? uri.getHost() : "")
                        + ((uri.getPort() != -1) ? (":" + uri.getPort()) : "")
                        + "/...";
            }
        }
        // Not a sensitive scheme, but let's still be conservative about
        // the data we include -- only the ssp, not the query params or
        // fragment, because those can often have sensitive info.
        StringBuilder builder = new StringBuilder(64);
        if (scheme != null) {
            builder.append(scheme);
            builder.append(':');
        }
        if (ssp != null) {
            builder.append(ssp);
        }
        return builder.toString();
    }
}
