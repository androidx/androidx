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

package androidx.browser.trusted.sharing;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains data to be delivered to a Web Share Target via a Trusted Web Activity.
 * See {@link androidx.browser.trusted.TrustedWebActivityIntentBuilder#setShareParams}.
 */
public final class ShareData {
    /** Bundle key for {@link #title}. */
    public static final String KEY_TITLE = "androidx.browser.trusted.sharing.KEY_TITLE";

    /** Bundle key for {@link #text}. */
    public static final String KEY_TEXT = "androidx.browser.trusted.sharing.KEY_TEXT";

    /** Bundle key for {@link #uris}. */
    public static final String KEY_URIS = "androidx.browser.trusted.sharing.KEY_URIS";

    /** Title of the shared message. */
    @Nullable
    public final String title;

    /** Text of the shared message. */
    @Nullable
    public final String text;

    /** URIs of files to be shared. */
    @Nullable
    public final List<Uri> uris;

    /**
     * Creates a {@link ShareData} with the given parameters.
     * @param title The {@link #title}.
     * @param text The {@link #text}.
     * @param uris The {@link #uris}.
     */
    public ShareData(@Nullable String title, @Nullable String text, @Nullable List<Uri> uris) {
        this.title = title;
        this.text = text;
        this.uris = uris;
    }

    /** Packs the object into a {@link Bundle} */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_TEXT, text);
        if (uris != null) {
            bundle.putParcelableArrayList(KEY_URIS, new ArrayList<>(uris));
        }
        return bundle;
    }

    /** Unpacks the object from a {@link Bundle}. */
    @NonNull
    @SuppressWarnings("deprecation")
    public static ShareData fromBundle(@NonNull Bundle bundle) {
        return new ShareData(bundle.getString(KEY_TITLE),
                bundle.getString(KEY_TEXT),
                bundle.getParcelableArrayList(KEY_URIS));
    }
}
