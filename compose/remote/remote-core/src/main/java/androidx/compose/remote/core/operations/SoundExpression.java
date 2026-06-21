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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT_ARRAY;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableProvider;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.utilities.ToneSynthesizer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * Defines a sound synthesis recipe stored as a resource. The {@code params} float array
 * uses the same NaN-encoding convention as {@link PathCreate}/{@link PathAppend}:
 * a NaN-encoded type tag at index 0 identifies the synthesis type, followed by
 * type-specific parameter floats. Parameter floats may themselves be NaN-encoded
 * variable references for dynamic binding.
 *
 * <p>Currently supported types:
 * <ul>
 *   <li>{@link #TYPE_TONE} — a pure waveform tone. Params: frequency (Hz),
 *       duration (seconds), waveform ({@link #WAVEFORM_SINE} etc.).</li>
 * </ul>
 *
 * <p>Volume and rate are stored as separate fields (also NaN-encodable for variable binding).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SoundExpression extends Operation implements VariableSupport, Serializable,
        ComponentData, VariableProvider {
    private static final int OP_CODE = Operations.SOUND_EXPRESSION;
    private static final String CLASS_NAME = "SoundExpression";

    /** Maximum number of floats in the params array. */
    private static final int MAX_PARAMS = 64;

    // ---- synthesis type tags (NaN-encoded at params[0]) ----------------------------
    // Values in [10, 40] are not treated as variable references by Utils.isVariable().

    /** Simple waveform tone: params = [TYPE_TONE, frequency, durationSeconds, waveform] */
    public static final int TYPE_TONE = 10;

    // ---- waveform kinds (plain float value at params[3]) ---------------------------

    /** Sine wave (default, smooth). */
    public static final float WAVEFORM_SINE = 0f;
    /** Square wave (buzzy). */
    public static final float WAVEFORM_SQUARE = 1f;
    /** Sawtooth wave (harsh). */
    public static final float WAVEFORM_SAWTOOTH = 2f;
    /** Triangle wave (soft). */
    public static final float WAVEFORM_TRIANGLE = 3f;

    // ---- pre-encoded NaN type tags -------------------------------------------------
    public static final float TYPE_TONE_NAN = Utils.asNan(TYPE_TONE);

    int mId;
    float mLeftVolume;
    float mRightVolume;
    float mRate;
    float[] mParams;
    float[] mOutputParams;

    public SoundExpression(
            int id,
            float leftVolume,
            float rightVolume,
            float rate,
            float @NonNull [] params) {
        mId = id;
        mLeftVolume = leftVolume;
        mRightVolume = rightVolume;
        mRate = rate;
        mParams = params;
        mOutputParams = Arrays.copyOf(params, params.length);
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutputParams = resolveVariables(mParams, context);
    }

    private static float[] resolveVariables(float[] src, @NonNull RemoteContext context) {
        float[] out = new float[src.length];
        for (int i = 0; i < src.length; i++) {
            float v = src[i];
            if (Utils.isVariable(v)) {
                out[i] = context.getFloat(Utils.idFromNan(v));
            } else {
                out[i] = v;
            }
        }
        return out;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (float v : mParams) {
            if (Utils.isVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
        if (Utils.isVariable(mLeftVolume)) {
            context.listensTo(Utils.idFromNan(mLeftVolume), this);
        }
        if (Utils.isVariable(mRightVolume)) {
            context.listensTo(Utils.idFromNan(mRightVolume), this);
        }
        if (Utils.isVariable(mRate)) {
            context.listensTo(Utils.idFromNan(mRate), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mLeftVolume, mRightVolume, mRate, mParams);
    }

    /**
     * Write a SOUND_EXPRESSION operation to the buffer.
     *
     * @param buffer      the wire buffer
     * @param id          the expression id
     * @param leftVolume  left-channel volume (0.0–1.0; NaN-encoded variable ref allowed)
     * @param rightVolume right-channel volume (0.0–1.0; NaN-encoded variable ref allowed)
     * @param rate        playback rate (1.0 = normal; NaN-encoded variable ref allowed)
     * @param params      synthesis params: NaN(type) followed by type-specific floats
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            float leftVolume,
            float rightVolume,
            float rate,
            float @NonNull [] params) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeFloat(leftVolume);
        buffer.writeFloat(rightVolume);
        buffer.writeFloat(rate);
        buffer.writeInt(params.length);
        for (float p : params) {
            buffer.writeFloat(p);
        }
    }

    /**
     * Read a SOUND_EXPRESSION operation from the buffer and append it to the list.
     *
     * @param buffer     the wire buffer
     * @param operations the list to append to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readId();
        float leftVolume = buffer.readFloat();
        float rightVolume = buffer.readFloat();
        float rate = buffer.readFloat();
        int len = buffer.readInt();
        if (len > MAX_PARAMS) {
            throw new RuntimeException("SoundExpression params too long: " + len);
        }
        float[] params = new float[len];
        for (int i = 0; i < len; i++) {
            params[i] = buffer.readFloat();
        }
        operations.add(new SoundExpression(id, leftVolume, rightVolume, rate, params));
    }

    /** Populate documentation for this operation. */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Define a sound synthesis recipe (tone, etc.)")
                .field(INT, "id", "The ID of the sound expression")
                .field(FLOAT, "leftVolume", "Left-channel volume (0.0–1.0)")
                .field(FLOAT, "rightVolume", "Right-channel volume (0.0–1.0)")
                .field(FLOAT, "rate", "Playback rate (1.0 = normal pitch)")
                .field(INT, "paramsLength", "Number of params elements")
                .field(FLOAT_ARRAY, "params",
                        "NaN-encoded type tag at [0], then type-specific floats");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        if (mOutputParams.length == 0) return;
        int typeId = Utils.idFromNan(mOutputParams[0]);
        if (typeId == TYPE_TONE && mOutputParams.length >= 4) {
            byte[] wav = ToneSynthesizer.synthesizeWav(
                    mOutputParams[1], mOutputParams[2], mOutputParams[3]);
            context.loadSound(mId, wav);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "[" + mId + "] params=" + mParams.length;
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME)
                .add("id", mId)
                .add("leftVolume", mLeftVolume)
                .add("rightVolume", mRightVolume)
                .add("rate", mRate)
                .add("paramsLength", mParams.length);
    }
}
