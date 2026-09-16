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

package androidx.compose.remote.core.events;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.Nullable;

/** Representation of an event dispatched through the system. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Event {
    private final int mType;
    private final int mMetadata;
    private final float[] mData;

    /**
     * Constructs a new Event.
     *
     * @param type type identifying the event
     * @param metadata metadata value associated with the event
     * @param data optional payload data containing event properties
     */
    public Event(int type, int metadata, float @Nullable [] data) {
        mType = type;
        mMetadata = metadata;
        mData = data;
    }

    /**
     * Returns the event type.
     *
     * @return the integer event type
     */
    public int getType() {
        return mType;
    }

    /**
     * Returns the event metadata.
     *
     * @return the integer metadata
     */
    public int getMetadata() {
        return mMetadata;
    }

    /**
     * Returns the event data payload.
     *
     * @return the float array payload or null if empty
     */
    public float @Nullable [] getData() {
        return mData;
    }
}
