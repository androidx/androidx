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
package androidx.compose.remote.core.operations.d3;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * Define the entire 3D light set in one op (replacing per-light add + clear). Each light has a
 * {@code type} ({@code DIRECTIONAL=0} or {@code POINT=1}), a {@code colorArgb} (RGB used), and 4
 * {@code params} ({@code dirX,dirY,dirZ,intensity} for directional, {@code posX,posY,posZ,
 * intensity} for point). An empty set means "no lights" (ambient only). Param floats may be
 * variables (animated). See {@link Paint3DContext#setLights3D(int[], int[], float[])}.
 *
 * <p>A type word is {@code kind | (extra << 8)}: the kind ({@link
 * Paint3DContext#LIGHT_DIRECTIONAL}, {@link Paint3DContext#LIGHT_POINT} or {@link
 * Paint3DContext#LIGHT_AMBIENT}, whose params are {@code 0,0,0, intensity}) in bits 0–7 and a count
 * of extra params in bits 8–15, so light {@code i} has {@code 4 + extra_i} params. {@link #paint}
 * hands the renderer normalized arrays: the kind as the type, exactly 4 params per light, and no
 * lights of a kind it does not know (logged in debug builds). {@link #write} reproduces the
 * operation as read.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SetLights3D extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.SET_LIGHTS_3D;
    private static final String CLASS_NAME = "SetLights3D";
    private static final boolean DEBUG = false;

    /** Most lights an operation may carry; more means a corrupt document. */
    private static final int MAX_LIGHTS = 256;

    /** Params the renderer gets per light. */
    private static final int PARAMS_PER_LIGHT = 4;

    private final int @NonNull [] mTypes;
    private final int @NonNull [] mColors;
    private final float @NonNull [] mParams; // 4 + extra per light
    private final float @NonNull [] mResolved;

    // What paint() hands the renderer. For a light set the renderer can take as is (known kinds,
    // no extra params, 4 params per light) these are mTypes, mColors and mResolved themselves;
    // otherwise they are normalized copies, mSourceOffsets[j] being the offset in mResolved of
    // the params of renderer light j.
    private final int @NonNull [] mPaintTypes;
    private final int @NonNull [] mPaintColors;
    private final float @NonNull [] mPaintParams;
    private final int @NonNull [] mSourceOffsets;

    public SetLights3D(int @NonNull [] types, int @NonNull [] colors, float @NonNull [] params) {
        mTypes = types;
        mColors = colors;
        mParams = params;
        mResolved = params.clone();
        if (isNormalized(types, colors, params)) {
            mPaintTypes = types;
            mPaintColors = colors;
            mPaintParams = mResolved;
            mSourceOffsets = new int[0];
            return;
        }
        int n = Math.min(types.length, colors.length);
        int[] kinds = new int[n];
        int[] lightColors = new int[n];
        int[] offsets = new int[n];
        int count = 0;
        int p = 0;
        for (int i = 0; i < n; i++) {
            int kind = types[i] & Paint3DContext.LIGHT_KIND_MASK;
            int extra = (types[i] >> Paint3DContext.LIGHT_EXTRA_SHIFT)
                    & Paint3DContext.LIGHT_EXTRA_MASK;
            if (p + PARAMS_PER_LIGHT > params.length) {
                if (DEBUG) {
                    Utils.log(CLASS_NAME + ": params end before light " + i + ", "
                            + (n - i) + " light(s) dropped");
                }
                break;
            }
            if (kind <= Paint3DContext.LIGHT_AMBIENT) {
                kinds[count] = kind;
                lightColors[count] = colors[i];
                offsets[count] = p;
                count++;
            } else if (DEBUG) {
                Utils.log(CLASS_NAME + ": light " + i + " has unknown kind " + kind + ", dropped");
            }
            p += PARAMS_PER_LIGHT + extra;
        }
        mPaintTypes = Arrays.copyOf(kinds, count);
        mPaintColors = Arrays.copyOf(lightColors, count);
        mSourceOffsets = Arrays.copyOf(offsets, count);
        mPaintParams = new float[count * PARAMS_PER_LIGHT];
        copyPaintParams();
    }

    /**
     * True if the renderer can take the light set as is: every type word is a known kind with no
     * extra params, and there are 4 params and one color per light.
     */
    private static boolean isNormalized(int[] types, int[] colors, float[] params) {
        if (colors.length != types.length || params.length != types.length * PARAMS_PER_LIGHT) {
            return false;
        }
        for (int type : types) {
            if (type < 0 || type > Paint3DContext.LIGHT_AMBIENT) {
                return false;
            }
        }
        return true;
    }

    /** Refreshes the normalized params from {@link #mResolved} (no-op when they are the same). */
    private void copyPaintParams() {
        for (int j = 0; j < mSourceOffsets.length; j++) {
            System.arraycopy(
                    mResolved, mSourceOffsets[j], mPaintParams, j * PARAMS_PER_LIGHT,
                    PARAMS_PER_LIGHT);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        for (int i = 0; i < mParams.length; i++) {
            float v = mParams[i];
            mResolved[i] = Utils.isVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
        copyPaintParams();
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (float v : mParams) {
            if (Utils.isVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTypes, mColors, mParams);
    }

    /**
     * Apply a {@link SetLights3D} to a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param types light types
     * @param colors light colors
     * @param params light params
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int @NonNull [] types,
            int @NonNull [] colors,
            float @NonNull [] params) {
        buffer.start(OP_CODE);
        int n = types.length;
        buffer.writeInt(n);
        for (int i = 0; i < n; i++) {
            buffer.writeInt(types[i]);
            buffer.writeInt(colors[i]);
        }
        buffer.writeInt(params.length);
        for (float v : params) {
            buffer.writeFloat(v);
        }
    }

    /**
     * Read a {@link SetLights3D} from a {@link WireBuffer}. Only a negative or absurd light count
     * or params length fails; unknown kinds and missing params are dealt with at paint time.
     * @param buffer wire buffer
     * @param operations list to add the operation to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int n = buffer.readInt();
        if (n < 0 || n > MAX_LIGHTS) {
            throw new RuntimeException("SetLights3D: bad light count " + n);
        }
        int[] types = new int[n];
        int[] colors = new int[n];
        for (int i = 0; i < n; i++) {
            types[i] = buffer.readInt();
            colors[i] = buffer.readInt();
        }
        int len = buffer.readInt();
        // Up to 4 params per possible light whatever the count, as the original format allowed
        // (params past the lights are ignored), or more if extra params need them.
        int maxLen =
                Math.max(
                        MAX_LIGHTS * PARAMS_PER_LIGHT,
                        n * (PARAMS_PER_LIGHT + Paint3DContext.LIGHT_EXTRA_MASK));
        if (len < 0 || len > maxLen) {
            throw new RuntimeException("SetLights3D: bad params length " + len);
        }
        float[] params = new float[len];
        for (int i = 0; i < len; i++) {
            params[i] = buffer.readNanId();
        }
        operations.add(new SetLights3D(types, colors, params));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (context instanceof Paint3DContext) {
            ((Paint3DContext) context).setLights3D(mPaintTypes, mPaintColors, mPaintParams);
        }
    }

    /**
     * Documentation for {@link SetLights3D}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description(
                        "Define the whole 3D light set. Per light: type (0=directional,1=point,"
                                + "2=ambient), colorArgb, and 4 params (xyz + intensity). The type"
                                + " word is kind | (extra << 8), and the light has 4 + extra"
                                + " params; players ignore the extra params and lights of an"
                                + " unknown kind.")
                .field(DocumentedOperation.INT, "count", "Number of lights")
                .field(DocumentedOperation.INT, "typeColor", "count*2", "type,colorArgb per light")
                .field(
                        DocumentedOperation.INT,
                        "paramsLen",
                        "Length of params (count*4, or sum of 4 + extra)")
                .field(
                        DocumentedOperation.FLOAT_ARRAY,
                        "params",
                        "4 + extra per light (xyz + intensity, then the extra params)");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("count", mTypes.length);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(count=" + mTypes.length + ")";
    }
}
