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

package androidx.message.browser;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.versionedparcelable.ParcelUtils;

/**
 * A class for a contact information from {@link MessageLibraryService}.
 * @hide
 */
@RestrictTo(LIBRARY)
public class ContactInfo {
    private static final String KEY_ID = "androidx.message.browser.ContactInfo.ID";
    private static final String KEY_DISPLAY_NAME =
            "androidx.message.browser.ContactInfo.DISPLAY_NAME";
    private static final String KEY_DISPLAY_ICON =
            "androidx.message.browser.ContactInfo.DISPLAY_ICON";
    private static final String KEY_EMAIL = "androidx.message.browser.ContactInfo.EMAIL";
    private static final String KEY_EXTRAS = "androidx.message.browser.ContactInfo.EXTRAS";

    @NonNull
    public String id;
    @Nullable
    public String displayName;
    @Nullable
    public IconCompat displayIcon;
    @Nullable
    public String email;
    @Nullable
    public Bundle extras;

    // TODO: Find a way to remove @SuppressLint
    @SuppressLint("RestrictedApi")
    static ContactInfo fromBundle(Bundle bundle) {
        ContactInfo contact = new ContactInfo();
        contact.id = bundle.getString(KEY_ID);
        contact.displayName = bundle.getString(KEY_DISPLAY_NAME);
        contact.displayIcon = ParcelUtils.fromParcelable(bundle.getParcelable(KEY_DISPLAY_ICON));
        contact.email = bundle.getString(KEY_EMAIL);
        contact.extras = bundle.getBundle(KEY_EXTRAS);
        return contact;
    }

    // TODO: Find a way to remove @SuppressLint
    @SuppressLint("RestrictedApi")
    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ID, id);
        bundle.putString(KEY_DISPLAY_NAME, displayName);
        bundle.putParcelable(KEY_DISPLAY_ICON, ParcelUtils.toParcelable(displayIcon));
        bundle.putString(KEY_EMAIL, email);
        bundle.putBundle(KEY_EXTRAS, bundle);
        return bundle;
    }
}
