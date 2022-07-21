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

package androidx.camera.core.impl;

import android.hardware.camera2.CaptureRequest;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Map;
import java.util.Set;

/**
 * A TagBundle is an immutable tags collection which does not allow insertion/removal.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class TagBundle {
    /** It is used to store all the keys and Tags */
    protected final Map<String, Object> mTagMap;

    protected TagBundle(@NonNull Map<String, Object> tagMap) {
        mTagMap = tagMap;
    }

    private static final TagBundle EMPTY_TAGBUNDLE = new TagBundle(new ArrayMap<>());

    private static final String USER_TAG_PREFIX = "android.hardware.camera2.CaptureRequest.setTag.";

    private static final String CAMERAX_USER_TAG_PREFIX = USER_TAG_PREFIX + "CX";
    /**
     * Creates an empty TagBundle.
     *
     * @return an empty TagBundle containing no tag.
     */
    @NonNull
    public static TagBundle emptyBundle() {
        return EMPTY_TAGBUNDLE;
    }

    /**
     * Creates a TagBundle with one entry.
     *
     * @return a TagBundle containing one tag.
     */
    @NonNull
    public static TagBundle create(@NonNull Pair<String, Object> source) {
        Map<String, Object> map = new ArrayMap<>();
        map.put(source.first, source.second);
        return new TagBundle(map);
    }

    /**
     * Creates a TagBundle from another TagBundle.
     *
     * <p>This will copy keys and values from the provided TagBundle.
     *
     * @param otherTagBundle TagBundle containing keys/values to be copied.
     * @return A new TagBundle pre-populated with keys/values.
     */
    @NonNull
    public static TagBundle from(@NonNull TagBundle otherTagBundle) {
        Map<String, Object> tags = new ArrayMap<>();
        for (String key: otherTagBundle.listKeys()) {
            tags.put(key, otherTagBundle.getTag(key));
        }

        return new TagBundle(tags);
    }

    /**
     * Gets the tag associated with the key.
     *
     * @param key      The key for query.
     * @return The tag associated with the key.
     */
    @Nullable
    public Object getTag(@NonNull String key) {
        return mTagMap.get(key);
    }

    /**
     * Lists all keys contained within this TagBundle.
     *
     * @return A {@link Set} of keys contained within this configuration. It returns an empty set
     * if there are no keys in this TagBundle.
     */
    @NonNull
    public Set<String> listKeys() {
        return mTagMap.keySet();
    }

    /**
     * Produces a string that can be used to identify CameraX usage in a Camera2
     * {@link CaptureRequest}.
     *
     * <p>In Android 13 or later, Camera2 will log the string representation of any
     * tag set on {@link CaptureRequest.Builder#setTag(Object)}. Since
     * tag bundles are always set internally by CameraX as the tag in a capture
     * request, the constant string value returned here can be used to identify
     * usage of CameraX versus application usage of Camera2.
     *
     * <p>Note: Doesn't return an actual string representation of the tag bundle.
     *
     * @return Returns a constant string value used to identify usage of CameraX.
     */
    @NonNull
    @Override
    public final String toString() {
        return CAMERAX_USER_TAG_PREFIX;
    }
}
