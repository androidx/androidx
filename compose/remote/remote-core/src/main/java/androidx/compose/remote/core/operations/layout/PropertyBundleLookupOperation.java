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

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.VariableProvider;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.ComponentData;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Operation that looks up an integer property from a component's {@link
 * PropertyBundleModifierOperation} and stores the resolved value (or {@code defaultValue} if
 * absent) into an integer variable in {@link RemoteContext}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PropertyBundleLookupOperation extends Operation
        implements SerializableToString,
                Serializable,
                ComponentData,
                VariableProvider,
                VariableSupport {

    private static final int OP_CODE = Operations.PROPERTY_BUNDLE_LOOKUP;
    private static final int VERSION = 0;

    /** The serializable class name. */
    public static final String CLASS_NAME = "PropertyBundleLookupOperation";

    public static final int SELF_COMPONENT_ID = -1;

    private int mValueId;
    private int mComponentId;
    private final int mPropertyId;
    private final int mDefaultValue;

    private boolean mSourceResolved = false;
    private int mSourceVariableId = -1;
    private int mResolvedValue;

    /**
     * Creates a {@link PropertyBundleLookupOperation}.
     *
     * @param valueId the destination integer variable identifier
     * @param componentId the target component identifier, or {@code -1} for the enclosing component
     * @param propertyId the property identifier to look up
     * @param defaultValue fallback value used when the component, bundle, or property is missing
     */
    public PropertyBundleLookupOperation(
            int valueId, int componentId, int propertyId, int defaultValue) {
        mValueId = valueId;
        mComponentId = componentId;
        mPropertyId = propertyId;
        mDefaultValue = defaultValue;
        mResolvedValue = defaultValue;
    }

    /** Returns the opcode for {@link PropertyBundleLookupOperation}. */
    public static int id() {
        return OP_CODE;
    }

    /** Returns the operation class name. */
    public static @NonNull String name() {
        return CLASS_NAME;
    }

    /** Returns the destination integer variable identifier. */
    public int getValueId() {
        return mValueId;
    }

    @Override
    public int getId() {
        return mValueId;
    }

    @Override
    public void setId(int id) {
        mValueId = id;
    }

    /** Returns the target component identifier. */
    public int getComponentId() {
        return mComponentId;
    }

    /** Updates the target component identifier. */
    public void setComponentId(int componentId) {
        mComponentId = componentId;
        mSourceResolved = false;
    }

    /** Returns the property identifier to look up. */
    public int getPropertyId() {
        return mPropertyId;
    }

    /** Returns the fallback value used when the component, bundle, or property is missing. */
    public int getDefaultValue() {
        return mDefaultValue;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mValueId, mComponentId, mPropertyId, mDefaultValue);
    }

    private void resolveSource(@NonNull RemoteContext context) {
        CoreDocument document = context.getDocument();
        Component component = document != null ? document.getComponent(mComponentId) : null;
        if (component == null
                && context.mLastComponent != null
                && (mComponentId == SELF_COMPONENT_ID
                        || context.mLastComponent.getComponentId() == mComponentId)) {
            component = context.mLastComponent;
        }
        if (component instanceof LayoutComponentContent || component instanceof CanvasContent) {
            component = component.getParent();
        }
        PropertyBundleModifierOperation bundle =
                component != null ? component.getPropertyBundle() : null;
        if (bundle != null && bundle.hasProperty(mPropertyId)) {
            mSourceVariableId = bundle.getValue(mPropertyId, -1);
        } else {
            mSourceVariableId = -1;
            mResolvedValue = mDefaultValue;
        }
        if (document != null || component != null) {
            mSourceResolved = true;
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        mSourceResolved = false;
        resolveSource(context);
        if (mSourceVariableId != -1) {
            context.listensTo(mSourceVariableId, this);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (!mSourceResolved || context.getDocument() != null) {
            resolveSource(context);
        }
        if (mSourceVariableId != -1) {
            mResolvedValue = context.getInteger(mSourceVariableId);
        } else {
            mResolvedValue = mDefaultValue;
        }
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        updateVariables(context);
        context.loadInteger(mValueId, mResolvedValue);
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "PROPERTY_BUNDLE_LOOKUP valueId="
                        + mValueId
                        + " componentId="
                        + mComponentId
                        + " propertyId="
                        + mPropertyId
                        + " defaultValue="
                        + mDefaultValue);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.EXPRESSION)
                .addType(CLASS_NAME)
                .add("valueId", mValueId)
                .add("componentId", mComponentId)
                .add("propertyId", mPropertyId)
                .add("defaultValue", mDefaultValue);
    }

    @Override
    public @NonNull String toString() {
        return CLASS_NAME
                + "("
                + "valueId="
                + mValueId
                + ", componentId="
                + mComponentId
                + ", propertyId="
                + mPropertyId
                + ", defaultValue="
                + mDefaultValue
                + ")";
    }

    /**
     * Populates {@code doc} with documentation for {@link PropertyBundleLookupOperation}.
     *
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Logic & Expressions Operations", OP_CODE, CLASS_NAME)
                .addedVersion(7)
                .description(
                        "Look up an integer property from a component's"
                                + " PropertyBundleModifierOperation")
                .field(INT, "version", "Wire format version")
                .field(INT, "valueId", "The ID of the integer variable to store the result in")
                .field(INT, "componentId", "The ID of the component to reference")
                .field(INT, "propertyId", "The property identifier to look up")
                .field(
                        INT,
                        "defaultValue",
                        "Fallback value stored when the component, bundle, or property is missing");
    }

    /**
     * Writes a {@link PropertyBundleLookupOperation} to {@code buffer}.
     *
     * @param buffer target wire buffer
     * @param valueId the destination integer variable identifier
     * @param componentId the target component identifier, or {@code -1} for the enclosing component
     * @param propertyId the property identifier to look up
     * @param defaultValue fallback value used when the component, bundle, or property is missing
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int valueId,
            int componentId,
            int propertyId,
            int defaultValue) {
        buffer.start(OP_CODE);
        buffer.writeInt(VERSION);
        buffer.writeInt(valueId);
        buffer.writeInt(componentId);
        buffer.writeInt(propertyId);
        buffer.writeInt(defaultValue);
    }

    /**
     * Reads a {@link PropertyBundleLookupOperation} from {@code buffer} and appends it to {@code
     * operations}.
     *
     * @param buffer source wire buffer
     * @param operations target operation list
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int version = buffer.readInt();
        if (version != VERSION) {
            throw new RuntimeException(
                    String.format("Unsupported %s version: %s", CLASS_NAME, version));
        }

        int valueId = buffer.declareId();
        int componentId = buffer.readId();
        int propertyId = buffer.readInt();
        int defaultValue = buffer.readInt();

        operations.add(
                new PropertyBundleLookupOperation(valueId, componentId, propertyId, defaultValue));
    }
}
