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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Information regarding a road, street, highway, etc.
 */
@VersionedParcelize
public class Segment implements VersionedParcelable {
    @ParcelField(1)
    String mName;

    /**
     * Used by {@link VersionedParcelable}
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    Segment() {
    }

    /**
     * Creates a new {@link Segment} with the given name.
     *
     * @param name Name of the segment.
     */
    public Segment(@NonNull String name) {
        mName = Preconditions.checkNotNull(name);
    }

    /**
     * Returns the name of the segment (e.g.: "Wallaby Way", "US-101", "Charleston Rd", etc.)
     */
    @NonNull
    public String getName() {
        return Common.nonNullOrEmpty(mName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Segment segment = (Segment) o;
        return Objects.equals(getName(), segment.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return String.format("{name: %s}", getName());
    }
}
