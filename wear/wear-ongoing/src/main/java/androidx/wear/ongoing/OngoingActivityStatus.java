/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.wear.ongoing;

import android.os.Bundle;

import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelize;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used internally by the library to represent the status of and ongoing activity, and to
 * serialize / deserialize.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@VersionedParcelize(isCustom = true)
class OngoingActivityStatus extends CustomVersionedParcelable {
    @ParcelField(value = 1)
    @NonNull List<CharSequence> mTemplates = new ArrayList<>();

    @NonParcelField
    @NonNull Map<String, StatusPart> mParts = new HashMap<>();

    // Used to serialize/deserialize mParts to avoid http://b/132619460
    @ParcelField(value = 2)
    Bundle mPartsAsBundle;

    // Needed By VersionedParcelables.
    OngoingActivityStatus() {
    }

    // Basic constructor used by OngoingActivityStatusApi
    OngoingActivityStatus(
            @Nullable List<CharSequence> templates,
            @NonNull Map<String, StatusPart> parts
    ) {
        mTemplates = templates;
        mParts = parts;
    }

    // Implementation of CustomVersionedParcelable
    /**
     * See {@link androidx.versionedparcelable.CustomVersionedParcelable#onPreParceling(boolean)}
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void onPreParceling(boolean isStream) {
        mPartsAsBundle = new Bundle();
        for (Map.Entry<String, StatusPart> me : mParts.entrySet()) {
            ParcelUtils.putVersionedParcelable(mPartsAsBundle, me.getKey(), me.getValue());
        }

        // VersionedParcel can only serialize Strings inside a List (not arbitrary
        // CharSequences such as Spanned), so convert every template to a plain String.
        // Styling information is intentionally dropped, see b/449189804.
        // A new list is created rather than modifying in place, since the list may be shared
        // with the Status object this was created from.
        if (mTemplates != null) {
            List<CharSequence> templates = new ArrayList<>(mTemplates.size());
            for (CharSequence template : mTemplates) {
                templates.add(template.toString());
            }
            mTemplates = templates;
        }
    }

    /**
     * See {@link androidx.versionedparcelable.CustomVersionedParcelable#onPostParceling()}
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void onPostParceling() {
        mParts = new HashMap<>();
        for (String key : mPartsAsBundle.keySet()) {
            StatusPart part = ParcelUtils.getVersionedParcelable(mPartsAsBundle, key);
            if (part != null) {
                mParts.put(key, part);
            }
        }
    }
}
