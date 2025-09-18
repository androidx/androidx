/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.modifiers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.AnimatableValue;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

/** Represents a graphics layer modifier. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GraphicsLayerModifierOperation extends DecoratorModifierOperation {
    private static final int OP_CODE = Operations.MODIFIER_GRAPHICS_LAYER;
    public static final String CLASS_NAME = "GraphicsLayerModifierOperation";

    public static final int SCALE_X = 0;
    public static final int SCALE_Y = 1;
    public static final int ROTATION_X = 2;
    public static final int ROTATION_Y = 3;
    public static final int ROTATION_Z = 4;
    public static final int TRANSFORM_ORIGIN_X = 5;
    public static final int TRANSFORM_ORIGIN_Y = 6;
    public static final int TRANSLATION_X = 7;
    public static final int TRANSLATION_Y = 8;
    public static final int TRANSLATION_Z = 9;
    public static final int SHADOW_ELEVATION = 10;
    public static final int ALPHA = 11;
    public static final int CAMERA_DISTANCE = 12;
    public static final int COMPOSITING_STRATEGY = 13;
    public static final int SPOT_SHADOW_COLOR = 14;
    public static final int AMBIENT_SHADOW_COLOR = 15;
    public static final int HAS_BLUR = 16;
    public static final int BLUR_RADIUS_X = 17;
    public static final int BLUR_RADIUS_Y = 18;
    public static final int BLUR_TILE_MODE = 19;
    public static final int SHAPE = 20;
    public static final int SHAPE_RADIUS = 21;

    public static final int SHAPE_RECT = 0;
    public static final int SHAPE_ROUND_RECT = 1;
    public static final int SHAPE_CIRCLE = 2;

    public static final int TILE_MODE_CLAMP = 0;
    public static final int TILE_MODE_REPEATED = 1;
    public static final int TILE_MODE_MIRROR = 2;
    public static final int TILE_MODE_DECAL = 3;

    /** The object is an integer */
    private static final short DATA_TYPE_INT = 0;

    /** The object is an float */
    private static final short DATA_TYPE_FLOAT = 1;

    AttributeValue[] mValues = {
        new AttributeValue(SCALE_X, "SCALE_X", 1f),
        new AttributeValue(SCALE_Y, "SCALE_Y", 1f),
        new AttributeValue(ROTATION_X, "ROTATION_X", 0f),
        new AttributeValue(ROTATION_Y, "ROTATION_Y", 0f),
        new AttributeValue(ROTATION_Z, "ROTATION_Z", 0f),
        new AttributeValue(TRANSFORM_ORIGIN_X, "TRANSFORM_ORIGIN_X", 0f),
        new AttributeValue(TRANSFORM_ORIGIN_Y, "TRANSFORM_ORIGIN_Y", 0f),
        new AttributeValue(TRANSLATION_X, "TRANSLATION_X", 0f),
        new AttributeValue(TRANSLATION_Y, "TRANSLATION_Y", 0f),
        new AttributeValue(TRANSLATION_Z, "TRANSLATION_Z", 0f),
        new AttributeValue(SHADOW_ELEVATION, "SHADOW_ELEVATION", 0f),
        new AttributeValue(ALPHA, "ALPHA", 1f),
        new AttributeValue(CAMERA_DISTANCE, "CAMERA_DISTANCE", 8f),
        new AttributeValue(COMPOSITING_STRATEGY, "COMPOSITING_STRATEGY", 0),
        new AttributeValue(SPOT_SHADOW_COLOR, "SPOT_SHADOW_COLOR", 0),
        new AttributeValue(AMBIENT_SHADOW_COLOR, "AMBIENT_SHADOW_COLOR", 0),
        new AttributeValue(HAS_BLUR, "HAS_BLUR", 0),
        new AttributeValue(BLUR_RADIUS_X, "BLUR_RADIUS_X", 0f),
        new AttributeValue(BLUR_RADIUS_Y, "BLUR_RADIUS_Y", 0f),
        new AttributeValue(BLUR_TILE_MODE, "BLUR_TILE_MODE", TILE_MODE_CLAMP),
        new AttributeValue(SHAPE, "SHAPE", -1),
        new AttributeValue(SHAPE_RADIUS, "SHAPE_RADIUS", 0f),
    };

    boolean mHasBlurEffect = false;

    /**
     * Fill in the hashmap with the attributes values
     *
     * @param attributes
     */
    public void fillInAttributes(@NonNull HashMap<Integer, Object> attributes) {
        for (int i = 0; i < mValues.length; i++) {
            if (mValues[i].needsToWrite()) {
                attributes.put(i, mValues[i].getObjectValue());
            }
        }
    }

    static final int FLOAT_VALUE = 0;
    static final int INT_VALUE = 1;

    /** Utility class to manage attributes */
    static class AttributeValue {
        String mName;
        int mId;
        @Nullable AnimatableValue mAnimatableValue;
        float mDefaultValue = 0f;
        int mIntValue = 0;
        int mIntDefaultValue = 0;
        int mType = FLOAT_VALUE;

        AttributeValue(int id, String name, float defaultValue) {
            mId = id;
            mName = name;
            mDefaultValue = defaultValue;
            mType = FLOAT_VALUE;
        }

        AttributeValue(int id, String name, int defaultValue) {
            mId = id;
            mName = name;
            mIntDefaultValue = defaultValue;
            mType = INT_VALUE;
        }

        public float getValue() {
            if (mType == FLOAT_VALUE) {
                if (mAnimatableValue != null) {
                    return mAnimatableValue.getValue();
                }
                return mDefaultValue;
            } else {
                return mIntValue;
            }
        }

        public int getIntValue() {
            if (mType == FLOAT_VALUE) {
                if (mAnimatableValue != null) {
                    return (int) mAnimatableValue.getValue();
                }
            } else if (mType == INT_VALUE) {
                return mIntValue;
            }
            return 0;
        }

        public void evaluate(PaintContext context) {
            if (mAnimatableValue != null) {
                mAnimatableValue.evaluate(context);
            }
        }

        public boolean needsToWrite() {
            if (mType == FLOAT_VALUE) {
                if (mAnimatableValue != null) {
                    return mAnimatableValue.getValue() != mDefaultValue;
                }
                return false;
            } else if (mType == INT_VALUE) {
                return mIntValue != mIntDefaultValue;
            }
            return false;
        }

        public void write(WireBuffer buffer) {
            buffer.writeInt(mId);
            if (mType == FLOAT_VALUE) {
                buffer.writeFloat(getValue());
            } else if (mType == INT_VALUE) {
                buffer.writeInt(getIntValue());
            }
        }

        public Object getObjectValue() {
            if (mType == FLOAT_VALUE) {
                return getValue();
            }
            return getIntValue();
        }

        public void setValue(float value) {
            mAnimatableValue = new AnimatableValue(value);
        }

        public void setValue(int value) {
            mIntValue = value;
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
        buffer.writeInt(mValues.length);
        for (int i = 0; i < mValues.length; i++) {
            AttributeValue value = mValues[i];
            if (value.needsToWrite()) {
                value.write(buffer);
            }
        }
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "GRAPHICS_LAYER = ["
                        + mValues[SCALE_X].getValue()
                        + ", "
                        + mValues[SCALE_Y].getValue()
                        + "]");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        for (int i = 0; i < mValues.length; i++) {
            AttributeValue v = mValues[i];
            v.evaluate(context);
        }
    }

    @Override
    public String toString() {
        return "GraphicsLayerModifierOperation("
                + mValues[SCALE_X].getValue()
                + ", "
                + mValues[SCALE_Y].getValue()
                + ")";
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param values attributes of the layer
     */
    public static void apply(@NonNull WireBuffer buffer, @NonNull HashMap<Integer, Object> values) {
        buffer.start(OP_CODE);
        int size = values.size();
        buffer.writeInt(size);
        for (Integer key : values.keySet()) {
            Object value = values.get(key);
            if (value instanceof Integer) {
                writeIntAttribute(buffer, key, (Integer) value);
            } else if (value instanceof Float) {
                writeFloatAttribute(buffer, key, (Float) value);
            }
        }
    }

    /**
     * Utility to write an integer attribute
     *
     * @param buffer
     * @param type
     * @param value
     */
    private static void writeIntAttribute(@NonNull WireBuffer buffer, int type, int value) {
        int tag = type | (DATA_TYPE_INT << 10);
        buffer.writeInt(tag);
        buffer.writeInt(value);
    }

    /**
     * Utility to write a float attribute
     *
     * @param buffer
     * @param type
     * @param value
     */
    private static void writeFloatAttribute(@NonNull WireBuffer buffer, int type, float value) {
        int tag = type | (DATA_TYPE_FLOAT << 10);
        buffer.writeInt(tag);
        buffer.writeFloat(value);
    }

    /**
     * Read the operation from the buffer
     *
     * @param buffer a WireBuffer
     * @param operations the list of operations read so far
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int length = buffer.readInt();
        GraphicsLayerModifierOperation op = new GraphicsLayerModifierOperation();
        for (int i = 0; i < length; i++) {
            op.readAttributeValue(buffer);
        }
        operations.add(op);
    }

    /**
     * Read a single attribute value from the buffer
     *
     * @param buffer
     */
    private void readAttributeValue(@NonNull WireBuffer buffer) {
        int tag = buffer.readInt();
        int dataType = tag >> 10;
        int index = (short) (tag & 0x3F);
        if (index == BLUR_RADIUS_X || index == BLUR_RADIUS_Y) {
            mHasBlurEffect = true;
            mValues[HAS_BLUR].setValue(1);
        }
        if (dataType == DATA_TYPE_FLOAT) {
            float value = buffer.readFloat();
            mValues[index].setValue(value);
        } else if (dataType == DATA_TYPE_INT) {
            int value = buffer.readInt();
            mValues[index].setValue(value);
        }
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .description("define the GraphicsLayer Modifier");
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {}

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("GraphicsLayerModifierOperation")
                .add("scaleX", mValues[SCALE_X].getValue())
                .add("scaleY", mValues[SCALE_Y].getValue())
                .add("rotationX", mValues[ROTATION_X].getValue())
                .add("rotationY", mValues[ROTATION_Y].getValue())
                .add("rotationZ", mValues[ROTATION_Z].getValue())
                .add("shadowElevation", mValues[SHADOW_ELEVATION].getValue())
                .add("transformOriginX", mValues[TRANSFORM_ORIGIN_X].getValue())
                .add("transformOriginY", mValues[TRANSFORM_ORIGIN_Y].getValue())
                .add("translationX", mValues[TRANSLATION_X].getValue())
                .add("translationY", mValues[TRANSLATION_Y].getValue())
                .add("translationZ", mValues[TRANSLATION_Z].getValue())
                .add("alpha", mValues[ALPHA].getValue())
                .add("cameraDistance", mValues[CAMERA_DISTANCE].getValue())
                .add("compositingStrategy", mValues[COMPOSITING_STRATEGY].getIntValue())
                .add("spotShadowColorId", mValues[SPOT_SHADOW_COLOR].getIntValue())
                .add("ambientShadowColorId", mValues[AMBIENT_SHADOW_COLOR].getIntValue());
    }
}
