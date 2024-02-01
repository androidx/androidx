/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Class describing the structure of the data contained within a slice.
 * <p>
 * A data version contains a string which describes the type of structure
 * and a revision which denotes this specific implementation. Revisions are expected
 * to be backwards compatible and monotonically increasing. Meaning if a
 * SliceSpec has the same type and an equal or lesser revision,
 * it is expected to be compatible.
 * <p>
 * Apps rendering slices will provide a list of supported versions to the OS which
 * will also be given to the app. Apps should only return a {@link Slice} with a
 * {@link SliceSpec} that one of the supported {@link SliceSpec}s provided
 * {@link #canRender}.
 *
 * @see Slice
 * @see SliceProvider#onBindSlice(Uri)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize(allowSerialization = true)
@Deprecated
public final class SliceSpec implements VersionedParcelable {

    @ParcelField(1)
    String mType;
    @ParcelField(value = 2, defaultValue = "1")
    int mRevision = 1;

    /**
     * Used for VersionedParcelable
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SliceSpec() {
    }

    public SliceSpec(@NonNull String type, int revision) {
        mType = type;
        mRevision = revision;
    }

    /**
     * Gets the type of the version.
     */
    @NonNull
    public String getType() {
        return mType;
    }

    /**
     * Gets the revision of the version.
     */
    public int getRevision() {
        return mRevision;
    }

    /**
     * Indicates that this spec can be used to render the specified spec.
     * <p>
     * Rendering support is not bi-directional (e.g. Spec v3 can render
     * Spec v2, but Spec v2 cannot render Spec v3).
     *
     * @param candidate candidate format of data.
     * @return true if versions are compatible.
     * @see androidx.slice.widget.SliceView
     */
    public boolean canRender(@NonNull SliceSpec candidate) {
        if (!mType.equals(candidate.mType)) return false;
        return mRevision >= candidate.mRevision;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SliceSpec)) return false;
        SliceSpec other = (SliceSpec) obj;
        return mType.equals(other.mType) && mRevision == other.mRevision;
    }

    @Override
    public int hashCode() {
        return mType.hashCode() + mRevision;
    }

    @Override
    public String toString() {
        return String.format("SliceSpec{%s,%d}", mType, mRevision);
    }
}
