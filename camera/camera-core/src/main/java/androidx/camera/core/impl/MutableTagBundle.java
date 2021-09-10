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

import android.util.ArrayMap;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * A mutable {@link TagBundle} which allows insertion/removal.
 */
public class MutableTagBundle extends TagBundle {

    private MutableTagBundle(Map<String, Object> source) {
        super(source);
    }

    /**
     * Creates an empty MutableTagBundle.
     *
     * @return an empty MutableTagBundle containing no tag.
     */
    @NonNull
    public static MutableTagBundle create() {
        return new MutableTagBundle(new ArrayMap<>());
    }

    /**
     * Creates a MutableTagBundle from an existing TagBundle.
     *
     * @param otherTagBundle TagBundle to insert.
     * @return a MutableTagBundle prepopulated with TagBundle.
     */
    @NonNull
    public static MutableTagBundle from(@NonNull TagBundle otherTagBundle) {
        Map<String, Object> tags = new ArrayMap<>();
        for (String key : otherTagBundle.listKeys()) {
            tags.put(key, otherTagBundle.getTag(key));
        }

        return new MutableTagBundle(tags);
    }

    /** Adds a tag with specified key. */
    public void putTag(@NonNull String key, @NonNull Object value) {
        // If the key exists, its value will be replaced.
        mTagMap.put(key, value);
    }

    /** Merges the given bundle into current bundle. */
    public void addTagBundle(@NonNull TagBundle bundle) {
        if (mTagMap != null && bundle.mTagMap != null) {
            mTagMap.putAll(bundle.mTagMap);
        }
    }
}
