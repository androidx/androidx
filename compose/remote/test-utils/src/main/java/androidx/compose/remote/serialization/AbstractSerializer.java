/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.serialization;

import static androidx.compose.remote.core.operations.IntegerExpression.isId;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.fromNaN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.isMathOperator;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.toMathName;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.isOperation;

import androidx.compose.remote.core.operations.PathData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Optional helper class for concrete implementations */
public abstract class AbstractSerializer implements Serializer, SerializeFactory {

    protected Object mValue;

    protected ValueType mValueType = ValueType.NULL;

    @Override
    public ValueType getValueType() {
        return mValueType;
    }

    @Override
    public ArraySerializer serializeArray() {
        if (mValueType == ValueType.ARRAY && mValue instanceof ArraySerializer) {
            return (ArraySerializer) mValue;
        }
        mValueType = ValueType.ARRAY;
        ArraySerializer serializer = newArraySerializer();
        mValue = serializer;
        return serializer;
    }

    @Override
    public <T> ArraySerializer serializeArray(@Nullable List<T> value) {
        ArraySerializer arraySerializer = serializeArray();
        if (value != null) {
            SerializeUtils.serializeArray(arraySerializer, value);
        }
        return arraySerializer;
    }

    @Override
    public MapSerializer serializeMap() {
        if (mValueType == ValueType.MAP && mValue instanceof MapSerializer) {
            return (MapSerializer) mValue;
        }
        mValueType = ValueType.MAP;
        MapSerializer mapSerializer = newMapSerializer();
        mValue = mapSerializer;
        return mapSerializer;
    }

    @Override
    public <T> MapSerializer serializeMap(@Nullable Map<String, T> map) {
        MapSerializer mapSerializer = serializeMap();
        if (map != null) {
            SerializeUtils.serializeMap(mapSerializer, map);
        }
        return mapSerializer;
    }

    @Override
    public void serializeFloatExpressionSrc(float[] values) {
        ArraySerializer arraySerializer = serializeArray();
        for (float v : values) {
            if (Float.isNaN(v)) {
                if (isMathOperator(v)) {
                    arraySerializer.add(
                            new InstructionSerializable(
                                    Objects.requireNonNull(toMathName(v))
                                            .toUpperCase(Locale.ROOT)));
                } else {
                    int id = fromNaN(v);
                    id = (id > NanMap.ID_REGION_ARRAY) ? id & 0xFFFFF : id;
                    arraySerializer.add(new VariableSerializable(id));
                }
            } else {
                arraySerializer.add(new FloatValueSerializable(v));
            }
        }
    }

    @Override
    public void serializeIntExpressionSrc(int[] value, int mask) {
        ArraySerializer arraySerializer = serializeArray();
        for (int i = 0; i < value.length; i++) {
            int v = value[i];
            if (isOperation(mask, i)) {
                if (isId(mask, i, v)) {
                    arraySerializer.add(new VariableSerializable(v));
                } else {
                    arraySerializer.add(
                            new InstructionSerializable(IntegerExpressionEvaluator.toMathName(v)));
                }
            } else {
                arraySerializer.add(new IntValueSerializable(v));
            }
        }
    }

    @Override
    public void serializePath(float[] path) {
        ArraySerializer arraySerializer = serializeArray();
        for (float value : path) {
            if (Float.isNaN(value)) {
                int id = Utils.idFromNan(value);
                if (id <= PathData.DONE) {
                    switch (id) {
                        case PathData.MOVE:
                            arraySerializer.add("M");
                            break;
                        case PathData.LINE:
                            arraySerializer.add("L");
                            break;
                        case PathData.QUADRATIC:
                            arraySerializer.add("Q");
                            break;
                        case PathData.CONIC:
                            arraySerializer.add("R");
                            break;
                        case PathData.CUBIC:
                            arraySerializer.add("C");
                            break;
                        case PathData.CLOSE:
                            arraySerializer.add("Z");
                            break;
                        case PathData.DONE:
                            arraySerializer.add(".");
                            break;
                        default:
                            arraySerializer.add("[" + id + "]");
                            break;
                    }
                } else {
                    arraySerializer.add("(" + id + ")");
                }
            } else {
                arraySerializer.add(value);
            }
        }
    }

    @Override
    public void serialize(@Nullable String value) {
        mValueType = value == null ? ValueType.NULL : ValueType.STRING;
        mValue = value;
    }

    @Override
    public void serialize(@Nullable Byte value) {
        mValueType = value == null ? ValueType.NULL : ValueType.BYTE;
        mValue = value;
    }

    @Override
    public void serialize(@Nullable Short value) {
        mValueType = value == null ? ValueType.NULL : ValueType.SHORT;
        mValue = value;
    }

    @Override
    public void serialize(@Nullable Integer value) {
        mValueType = value == null ? ValueType.NULL : ValueType.INT;
        mValue = value;
    }

    @Override
    public void serialize(@Nullable Long value) {
        mValueType = value == null ? ValueType.NULL : ValueType.LONG;
        mValue = value;
    }

    @Override
    public void serialize(@Nullable Float value) {
        mValueType = value == null ? ValueType.NULL : ValueType.FLOAT;
        mValue = value;
    }

    @Override
    public void serialize(@Nullable Double value) {
        mValueType = value == null ? ValueType.NULL : ValueType.DOUBLE;
        mValue = value;
    }

    @Override
    public void serialize(@Nullable Boolean value) {
        mValueType = value == null ? ValueType.NULL : ValueType.BOOLEAN;
        mValue = value;
    }

    @Override
    public <T extends Enum<T>> void serialize(@Nullable Enum<T> value) {
        mValueType = value == null ? ValueType.NULL : ValueType.STRING;
        if (value != null) {
            mValue = value.toString();
        }
    }

    @Override
    public void serialize(float a, float r, float g, float b) {
        serialize(Utils.colorInt(Utils.toARGB(a, r, g, b)));
    }
}
