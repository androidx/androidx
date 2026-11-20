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

package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Modifier operation carrying a bundle of property identifier and value pairs for a component. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PropertyBundleModifierOperation extends Operation implements ModifierOperation {

    private static final int OP_CODE = Operations.MODIFIER_PROPERTY_BUNDLE;
    private static final int VERSION = 0;

    /** The serializable class name. */
    public static final String CLASS_NAME = "PropertyBundleModifierOperation";

    private final int mBundleId;
    private final int mParentBundleId;
    private final @NonNull Map<Integer, Integer> mProperties;

    /**
     * Creates a {@link PropertyBundleModifierOperation}.
     *
     * @param bundleId the bundle identifier
     * @param parentBundleId the parent bundle identifier
     * @param propertyIds parallel array of property identifiers
     * @param values parallel array of property values
     */
    public PropertyBundleModifierOperation(
            int bundleId, int parentBundleId, int @NonNull [] propertyIds, int @NonNull [] values) {
        int count = propertyIds != null ? propertyIds.length : 0;
        int valuesLen = values != null ? values.length : 0;
        validatePropertyIdLength(count, valuesLen);

        mBundleId = bundleId;
        mParentBundleId = parentBundleId;
        mProperties = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            mProperties.put(propertyIds[i], values[i]);
        }
    }

    private PropertyBundleModifierOperation(
            int bundleId, int parentBundleId, @NonNull Map<Integer, Integer> properties) {
        mBundleId = bundleId;
        mParentBundleId = parentBundleId;
        mProperties = properties;
    }

    /** Returns the bundle identifier. */
    public int getBundleId() {
        return mBundleId;
    }

    /** Returns the parent bundle identifier. */
    public int getParentBundleId() {
        return mParentBundleId;
    }

    /**
     * Returns {@code true} if a value is present for {@code propertyId}.
     *
     * @param propertyId the property identifier
     * @return {@code true} if this bundle contains {@code propertyId}
     */
    public boolean hasProperty(int propertyId) {
        return mProperties.containsKey(propertyId);
    }

    /**
     * Returns the property value for {@code propertyId}, or {@code null} if not present.
     *
     * @param propertyId the property identifier
     * @return the property value, or {@code null} if not set
     */
    public @Nullable Integer getValue(int propertyId) {
        return mProperties.get(propertyId);
    }

    /**
     * Returns the property value for {@code propertyId}, or {@code defaultValue} if not present.
     *
     * @param propertyId the property identifier
     * @param defaultValue fallback value returned when {@code propertyId} is not present
     * @return the property value, or {@code defaultValue} if not set
     */
    public int getValue(int propertyId, int defaultValue) {
        Integer value = mProperties.get(propertyId);
        return value != null ? value : defaultValue;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
        buffer.writeInt(VERSION);
        buffer.writeInt(mBundleId);
        buffer.writeInt(mParentBundleId);
        buffer.writeInt(mProperties.size());

        for (Map.Entry<Integer, Integer> entry : mProperties.entrySet()) {
            buffer.writeInt(entry.getKey());
            buffer.writeInt(entry.getValue());
        }
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Attached directly to Component during inflate().
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "PROPERTY_BUNDLE_MODIFIER bundleId="
                        + mBundleId
                        + " parentBundleId="
                        + mParentBundleId
                        + " properties="
                        + mProperties);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        LinkedHashMap<String, Integer> serializedProperties =
                new LinkedHashMap<>(mProperties.size());
        for (Map.Entry<Integer, Integer> entry : mProperties.entrySet()) {
            serializedProperties.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType(CLASS_NAME)
                .add("bundleId", mBundleId)
                .add("parentBundleId", mParentBundleId)
                .add("properties", serializedProperties);
    }

    @Override
    public @NonNull String toString() {
        return CLASS_NAME
                + "("
                + "bundleId="
                + mBundleId
                + ", parentBundleId="
                + mParentBundleId
                + ", properties="
                + mProperties
                + ")";
    }

    private static void validatePropertyIdLength(int count, int valuesLen) {
        if (count != valuesLen) {
            throw new IllegalArgumentException(
                    String.format(
                            "Parallel arrays must have identical lengths:\n "
                                    + "propertyIds=%d\nvalues=%d",
                            count, valuesLen));
        }
        if (count > Limits.MAX_PROPERTY_BUNDLE_ENTRIES) {
            throw new IllegalArgumentException(
                    String.format(
                            "Too many property bundle entries: %d > %d",
                            count, Limits.MAX_PROPERTY_BUNDLE_ENTRIES));
        }
    }

    /**
     * Writes a {@link PropertyBundleModifierOperation} to {@code buffer}.
     *
     * @param buffer target wire buffer
     * @param bundleId the bundle identifier
     * @param parentBundleId the parent bundle identifier
     * @param propertyIds parallel array of property identifiers
     * @param values parallel array of property values
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int bundleId,
            int parentBundleId,
            int @NonNull [] propertyIds,
            int @NonNull [] values) {

        int count = propertyIds != null ? propertyIds.length : 0;
        int valuesLen = values != null ? values.length : 0;
        validatePropertyIdLength(count, valuesLen);

        buffer.start(OP_CODE);
        buffer.writeInt(VERSION);
        buffer.writeInt(bundleId);
        buffer.writeInt(parentBundleId);
        buffer.writeInt(count);

        for (int i = 0; i < count; i++) {
            int propId = propertyIds[i];
            int value = values[i];
            buffer.writeInt(propId);
            buffer.writeInt(value);
        }
    }

    /**
     * Reads a {@link PropertyBundleModifierOperation} from {@code buffer} and appends it to {@code
     * operations}.
     *
     * @param buffer source wire buffer
     * @param operations target operation list
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int version = buffer.readInt();
        if (version != 0) {
            throw new RuntimeException(
                    String.format("Unsupported %s version: %s", CLASS_NAME, version));
        }

        int bundleId = buffer.readInt();
        int parentBundleId = buffer.readInt();

        int count = buffer.readInt();
        if (count < 0 || count > Limits.MAX_PROPERTY_BUNDLE_ENTRIES) {
            throw new RuntimeException("Invalid PropertyBundle entry count: " + count);
        }

        LinkedHashMap<Integer, Integer> properties = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            int propertyId = buffer.readInt();
            int value = buffer.readInt();
            properties.put(propertyId, value);
        }

        operations.add(new PropertyBundleModifierOperation(bundleId, parentBundleId, properties));
    }
}
