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

package androidx.compose.remote.creation.modifiers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Modifier element that writes a {@code PropertyBundleModifierOperation}. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PropertyBundleModifier implements RecordingModifier.Element {

    private final int mBundleId;
    private final int mParentBundleId;
    private final int @NonNull [] mPropertyIds;
    private final int @Nullable [] mValues;
    private final long @Nullable [] mLongValues;

    /**
     * Creates a {@link PropertyBundleModifier} with literal integer values.
     *
     * @param bundleId the bundle identifier
     * @param parentBundleId the parent bundle identifier
     * @param propertyIds parallel array of property identifiers
     * @param values parallel array of literal property values
     */
    public PropertyBundleModifier(
            int bundleId,
            int parentBundleId,
            int @NonNull [] propertyIds,
            int @NonNull [] values) {
        mBundleId = bundleId;
        mParentBundleId = parentBundleId;
        mPropertyIds = propertyIds;
        mValues = values;
        mLongValues = null;
    }

    /**
     * Creates a {@link PropertyBundleModifier} with literal integers or {@code
     * 0x100000000L}-tagged variable identifiers.
     *
     * @param bundleId the bundle identifier
     * @param parentBundleId the parent bundle identifier
     * @param propertyIds parallel array of property identifiers
     * @param values parallel array of literal integers or {@code 0x100000000L}-tagged variable IDs
     */
    public PropertyBundleModifier(
            int bundleId,
            int parentBundleId,
            int @NonNull [] propertyIds,
            long @NonNull [] values) {
        mBundleId = bundleId;
        mParentBundleId = parentBundleId;
        mPropertyIds = propertyIds;
        mValues = null;
        mLongValues = values;
    }

    /** Returns the bundle identifier. */
    public int getBundleId() {
        return mBundleId;
    }

    /** Returns the parent bundle identifier. */
    public int getParentBundleId() {
        return mParentBundleId;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        if (mLongValues != null) {
            writer.addPropertyBundleModifier(
                    mBundleId, mParentBundleId, mPropertyIds, mLongValues);
        } else if (mValues != null) {
            writer.addPropertyBundleModifier(
                    mBundleId, mParentBundleId, mPropertyIds, mValues);
        }
    }
}
