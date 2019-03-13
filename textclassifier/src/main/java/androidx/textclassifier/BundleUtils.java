/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.app.RemoteActionCompat;
import androidx.core.os.LocaleListCompat;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utilities for (un)marshalling with Bundle.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
/* package */ final class BundleUtils {

    private BundleUtils() {}

    /** Compat wrapper for deepCopy. */
    static Bundle deepCopy(Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return bundle.deepCopy();
        } else {
            // TODO: actually perform a deep copy.
            return (Bundle) bundle.clone();
        }
    }

    /** Serializes a string keyed map to a bundle, or clears it if null is passed. */
    static void putMap(
            @NonNull Bundle bundle, @NonNull String key, @Nullable Map<String, Float> map) {
        if (map == null) {
            bundle.remove(key);
            return;
        }
        final Bundle mapBundle = new Bundle();
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            mapBundle.putFloat(entry.getKey(), entry.getValue());
        }
        bundle.putBundle(key, mapBundle);
    }

    /** @throws IllegalArgumentException if key can't be found in the bundle */
    static Map<String, Float> getFloatStringMapOrThrow(
            @NonNull Bundle bundle, @NonNull String key) {
        final Bundle mapBundle = bundle.getBundle(key);
        if (mapBundle == null) {
            throw new IllegalArgumentException("Missing " + key);
        }
        final Map<String, Float> map = new ArrayMap<>();
        for (String mapKey : mapBundle.keySet()) {
            map.put(mapKey, mapBundle.getFloat(mapKey));
        }
        return map;
    }

    /** Serializes a list of actions to a bundle, or clears it if null is passed. */
    static void putRemoteActionList(
            @NonNull Bundle bundle, @NonNull String key,
            @NonNull List<RemoteActionCompat> actions) {
        ParcelUtils.putVersionedParcelableList(bundle, key, actions);
    }

    static List<RemoteActionCompat> getRemoteActionListOrThrow(
            @NonNull Bundle bundle, @NonNull String key) {
        return ParcelUtils.getVersionedParcelableList(bundle, key);
    }

    /** Serializes a list of TextLinks to a bundle, or clears it if null is passed. */
    static void putTextLinkList(
            @NonNull Bundle bundle, @NonNull String key, @Nullable List<TextLinks.TextLink> links) {
        if (links == null) {
            bundle.remove(key);
            return;
        }
        final ArrayList<Bundle> linkBundles = new ArrayList<>(links.size());
        for (TextLinks.TextLink link : links) {
            linkBundles.add(link.toBundle());
        }
        bundle.putParcelableArrayList(key, linkBundles);
    }

    /** @throws IllegalArgumentException if key can't be found in the bundle */
    static List<TextLinks.TextLink> getTextLinkListOrThrow(
            @NonNull Bundle bundle, @NonNull String key) {
        final List<Bundle> linkBundles = bundle.getParcelableArrayList(key);
        if (linkBundles == null) {
            throw new IllegalArgumentException("Missing " + key);
        }
        final List<TextLinks.TextLink> links = new ArrayList<>(linkBundles.size());
        for (Bundle linkBundle : linkBundles) {
            links.add(TextLinks.TextLink.createFromBundle(linkBundle));
        }
        return links;
    }

    /** Serializes a locale list to a bundle, or clears it if null is passed. */
    static void putLocaleList(
            @NonNull Bundle bundle, @NonNull String key, @Nullable LocaleListCompat localeList) {
        if (localeList == null) {
            bundle.remove(key);
            return;
        }
        bundle.putString(key, localeList.toLanguageTags());
    }

    static @Nullable LocaleListCompat getLocaleList(@NonNull Bundle bundle, @NonNull String key) {
        final String localeTags = bundle.getString(key);
        if (localeTags == null) {
            return null;
        }
        return LocaleListCompat.forLanguageTags(localeTags);
    }

    static void putLong(@NonNull Bundle bundle, @NonNull String key, @Nullable Long value) {
        if (value == null) {
            bundle.remove(key);
            return;
        }
        bundle.putLong(key, value);
    }

    @Nullable
    static Long getLong(@NonNull Bundle bundle, @NonNull String key) {
        if (!bundle.containsKey(key)) {
            return null;
        }
        return bundle.getLong(key);
    }
}
