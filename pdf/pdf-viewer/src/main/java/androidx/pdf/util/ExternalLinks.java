/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for describing and opening external links.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ExternalLinks {
    private static final String TAG = "ExternalLinks";

    // Allow to open any of the allow-listed URL schemes
    private static final Set<String> ALLOWED_SCHEMES = new HashSet<String>();

    static {
        ALLOWED_SCHEMES.add("http");
        ALLOWED_SCHEMES.add("https");
        ALLOWED_SCHEMES.add("mailto");
        ALLOWED_SCHEMES.add("tel");
    }

    private static final int SHORTEN_LENGTH = 40;

    /** Open the given link in a browser or similar, if it is safe to do so. */
    public static void open(@NonNull String url, @NonNull Context context) {
        open(Uri.parse(url), context);
    }

    /** Open the given URI. */
    public static void open(@NonNull Uri uri, @NonNull Context context) {
        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = uri.buildUpon().scheme("http").build();
        }
        if (ALLOWED_SCHEMES.contains(uri.getScheme())) {
            PackageManager pm = context.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (!pm.queryIntentActivities(intent, 0).isEmpty()) {
                Intents.startActivity(context, TAG, intent);
            }
            // TODO: Track hyperlink click.
        }
    }

    /**
     * A short description of an external link suitable for reading aloud - eg
     * {@code Link: www.example.com/page1.html} or
     * {@code Link: webpage at www.example.com}
     */
    @NonNull
    public static String getDescription(@NonNull String url, @NonNull Context context) {
        return getDescription(Uri.parse(url), context);
    }

    private static String getDescription(Uri uri, Context context) {
        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = uri.buildUpon().scheme("http").build();
        }
        String scheme = uri.getScheme();
        boolean isWebUri = "http".equals(scheme) || "https".equals(scheme);
        String nonScheme = uri.getSchemeSpecificPart().replaceFirst("^//", "");
        if (nonScheme == null) {  // URL must be bad, but we'll try to describe it anyway:
            return context.getString(R.string.desc_web_link, uri.toString());
        }

        String host = uri.getHost();
        if (nonScheme.length() > SHORTEN_LENGTH && isWebUri
                && host != null && host.length() < nonScheme.length()) {
            // Don't describe the entire URL, just say "webpage at [domain]"
            return context.getString(R.string.desc_web_link_shortened_to_domain, host);
        } else if (isWebUri) {
            return context.getString(R.string.desc_web_link, nonScheme);
        } else if ("mailto".equals(scheme)) {
            return context.getString(R.string.desc_email_link, nonScheme);
        } else if ("tel".equals(scheme)) {
            return context.getString(R.string.desc_phone_link, nonScheme);
        }
        return context.getString(R.string.desc_web_link, uri.toString());
    }

    private ExternalLinks() {
        // Static utility.
    }
}