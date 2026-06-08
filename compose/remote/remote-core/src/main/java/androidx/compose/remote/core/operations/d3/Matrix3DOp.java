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
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Post-multiply the current 3D modelview matrix. Sub-ops: {@code IDENTITY=0, TRANSLATE=1,
 * SCALE=2, ROTATE_AXIS=3, MULTIPLY=4, PUSH=5, POP=6}. See {@link
 * Paint3DContext#matrix3DOp(int, float[])} for argument arities.
 *
 * <p>Reading is tolerant so that documents written by a newer version still play: arguments
 * beyond the sub-op's arity are ignored, and an operation with an unknown sub-op or too few
 * arguments draws nothing (it is not passed to the renderer).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Matrix3DOp extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.MATRIX_3D_OP;
    private static final String CLASS_NAME = "Matrix3DOp";
    private static final boolean DEBUG = false;

    /** Maximum number of argument floats a reader accepts. */
    static final int MAX_ARGS = 256;

    /** Number of arguments each sub-op takes, indexed by sub-op. */
    private static final int[] ARITY = {0, 3, 3, 4, 16, 0, 0};

    private final int mSub;
    private final float @NonNull [] mArgs;

    /**
     * The resolved arguments passed to the renderer: exactly the sub-op's arity, or {@code null}
     * when the sub-op is unknown or has too few arguments (the operation is then skipped).
     */
    private final float @Nullable [] mResolved;

    public Matrix3DOp(int sub, float @NonNull [] args) {
        mSub = sub;
        mArgs = args;
        int arity = arity(sub);
        if (arity < 0 || args.length < arity) {
            mResolved = null;
        } else if (args.length == arity) {
            mResolved = args.clone();
        } else {
            mResolved = Arrays.copyOf(args, arity);
        }
    }

    /**
     * Returns the number of arguments a sub-op takes, or -1 if the sub-op is unknown.
     *
     * @param sub sub-op
     * @return argument count, or -1
     */
    static int arity(int sub) {
        return (sub < 0 || sub >= ARITY.length) ? -1 : ARITY[sub];
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        float[] resolved = mResolved;
        if (resolved == null) {
            return;
        }
        for (int i = 0; i < resolved.length; i++) {
            float v = mArgs[i];
            resolved[i] =
                    Utils.isVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (float v : mArgs) {
            if (Utils.isVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mSub, mArgs);
    }

    /**
     * Write a {@link Matrix3DOp} to a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param sub sub-op
     * @param args arguments
     */
    public static void apply(@NonNull WireBuffer buffer, int sub, float @NonNull [] args) {
        buffer.start(OP_CODE);
        buffer.writeInt(sub);
        buffer.writeInt(args.length);
        for (float v : args) {
            buffer.writeFloat(v);
        }
    }

    /**
     * Read a {@link Matrix3DOp} from a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param operations list to add the operation to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int sub = buffer.readInt();
        int len = buffer.readInt();
        if (len < 0 || len > MAX_ARGS) {
            throw new RuntimeException("Matrix3DOp: bad args length " + len);
        }
        float[] args = new float[len];
        for (int i = 0; i < len; i++) {
            args[i] = buffer.readNanId();
        }
        if (DEBUG) {
            int arity = arity(sub);
            if (arity < 0) {
                Utils.log("Matrix3DOp: unknown sub-op " + sub + ", skipped");
            } else if (len < arity) {
                Utils.log("Matrix3DOp: sub-op " + sub + " needs " + arity + " args, got " + len
                        + ", skipped");
            }
        }
        operations.add(new Matrix3DOp(sub, args));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        float[] resolved = mResolved;
        if (resolved != null && context instanceof Paint3DContext) {
            ((Paint3DContext) context).matrix3DOp(mSub, resolved);
        }
    }

    /**
     * Documentation for {@link Matrix3DOp}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description(
                        "Post-multiply current 3D modelview. Sub: 0=identity, 1=translate(x,y,z),"
                                + " 2=scale(x,y,z), 3=rotateAxis(angRad,ax,ay,az),"
                                + " 4=multiply(col-major 4x4), 5=push, 6=pop."
                                + " A reader ignores args beyond the sub-op's arity and skips an"
                                + " op with an unknown sub-op or too few args.")
                .field(DocumentedOperation.INT, "sub", "Sub-op")
                .field(DocumentedOperation.INT, "argsLen",
                        "Length of args (0,3,3,4,16,0,0); readers accept up to 256")
                .field(DocumentedOperation.FLOAT_ARRAY, "args", "Arguments");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("sub", mSub).add("argsLen", mArgs.length);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(sub=" + mSub + ", argsLen=" + mArgs.length + ")";
    }
}
