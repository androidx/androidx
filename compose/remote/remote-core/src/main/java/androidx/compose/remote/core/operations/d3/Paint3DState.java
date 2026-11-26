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

import java.util.List;

/**
 * Consolidated 3D render-state command: one op carrying a {@code sub}-selector and a small float
 * payload, replacing several single-purpose state ops. Sub-commands:
 *
 * <ul>
 *   <li>{@link #CLEAR_DEPTH} — clear the depth buffer (no params)
 *   <li>{@link #MATERIAL} — set the specular material ({@code specStrength, shininess}). More
 *       params may follow (reserved for PBR fields): readers use the first two and keep the rest
 *       for {@link #write}.
 *   <li>{@link #DEPTH_BIAS} — set the polygon-offset depth bias ({@code constant, slope})
 * </ul>
 *
 * <p>Reading is tolerant: params beyond what a sub-command uses are ignored, and an unknown
 * sub-command or one with too few params is a no-op (logged in debug builds).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Paint3DState extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.PAINT_3D_STATE;
    private static final String CLASS_NAME = "Paint3DState";
    private static final boolean DEBUG = false;

    /** Most params an operation may carry; more means a corrupt document. */
    private static final int MAX_PARAMS = 256;

    public static final int CLEAR_DEPTH = 0;
    public static final int MATERIAL = 1;
    public static final int DEPTH_BIAS = 2;

    private static final float[] EMPTY = new float[0];

    private final int mSub;
    private final float @NonNull [] mParams; // source: each may be a NaN-encoded variable id
    private final float @NonNull [] mResolved; // resolved values used at paint time

    public Paint3DState(int sub, float @NonNull [] params) {
        mSub = sub;
        mParams = params;
        mResolved = params.clone();
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        for (int i = 0; i < mParams.length; i++) {
            float v = mParams[i];
            mResolved[i] = Utils.isVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
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
        apply(buffer, mSub, mParams);
    }

    /**
     * Write a {@link Paint3DState} to a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param sub sub-command id
     * @param params sub-command parameters
     */
    public static void apply(@NonNull WireBuffer buffer, int sub, float @NonNull [] params) {
        buffer.start(OP_CODE);
        buffer.writeInt(sub);
        buffer.writeInt(params.length);
        for (float v : params) {
            buffer.writeFloat(v);
        }
    }

    /**
     * Read a {@link Paint3DState} from a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param operations list to add the operation to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int sub = buffer.readInt();
        int len = buffer.readInt();
        if (len < 0 || len > MAX_PARAMS) {
            throw new RuntimeException("Paint3DState: bad params length " + len);
        }
        float[] params = len == 0 ? EMPTY : new float[len];
        for (int i = 0; i < len; i++) {
            params[i] = buffer.readNanId();
        }
        if (DEBUG) {
            int needed = sub == MATERIAL || sub == DEPTH_BIAS ? 2 : 0;
            if (sub < CLEAR_DEPTH || sub > DEPTH_BIAS) {
                Utils.log(CLASS_NAME + ": unknown sub-command " + sub + ", ignored");
            } else if (len < needed) {
                Utils.log(CLASS_NAME + ": sub-command " + sub + " has " + len + " params, needs "
                        + needed + ", ignored");
            }
        }
        operations.add(new Paint3DState(sub, params));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (!(context instanceof Paint3DContext)) {
            return;
        }
        Paint3DContext p3d = (Paint3DContext) context;
        switch (mSub) {
            case CLEAR_DEPTH:
                p3d.clearDepth3D();
                break;
            case MATERIAL:
                if (mResolved.length >= 2) {
                    p3d.setMaterial3D(mResolved[0], mResolved[1]);
                }
                break;
            case DEPTH_BIAS:
                if (mResolved.length >= 2) {
                    p3d.setDepthBias3D(mResolved[0], mResolved[1]);
                }
                break;
            default:
                // Unknown sub-command (from a newer format): ignored.
                break;
        }
    }

    /**
     * Documentation for {@link Paint3DState}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description(
                        "3D render-state command: sub 0=clearDepth, 1=material(specStrength,"
                                + " shininess, plus optional extra params reserved for PBR, kept"
                                + " but ignored), 2=depthBias(constant, slope). Params beyond what"
                                + " a sub-command uses are ignored; an unknown sub-command or too"
                                + " few params is a no-op.")
                .field(DocumentedOperation.INT, "sub", "Sub-command id")
                .field(DocumentedOperation.INT, "count", "Number of float params")
                .field(DocumentedOperation.FLOAT_ARRAY, "params", "Sub-command parameters");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("sub", mSub).add("count", mParams.length);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(sub=" + mSub + ", params=" + mParams.length + ")";
    }
}
