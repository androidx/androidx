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
 * Set both projection and view matrices in one call. {@code projection} is one of
 * {@link Paint3DContext#PROJECTION_PERSPECTIVE} / {@link Paint3DContext#PROJECTION_ORTHO}.
 *
 * <p>Reading is tolerant so that documents written by a newer version still play: parameters
 * beyond what the projection (4 perspective, 6 ortho) or the view (9) needs are ignored, and an
 * operation with too few parameters sets no camera (it is not passed to the renderer). The
 * parameters of an unknown projection are passed to the renderer unchanged.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SetCamera3D extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.SET_CAMERA_3D;
    private static final String CLASS_NAME = "SetCamera3D";
    private static final boolean DEBUG = false;

    /** Maximum number of projection or view floats a reader accepts. */
    static final int MAX_PARAMS = 256;

    /** Number of view parameters: eye, center and up vectors. */
    private static final int VIEW_PARAMS = 9;

    private final int mProjection;
    private final float @NonNull [] mProjParams;
    private final float @NonNull [] mViewParams;

    /**
     * The resolved parameters passed to the renderer, trimmed to what the projection and view
     * need, or {@code null} when there are too few (the operation is then skipped).
     */
    private final float @Nullable [] mProjResolved;
    private final float @Nullable [] mViewResolved;

    public SetCamera3D(
            int projection, float @NonNull [] projParams, float @NonNull [] viewParams) {
        mProjection = projection;
        mProjParams = projParams;
        mViewParams = viewParams;
        int projCount = projectionParamCount(projection, projParams.length);
        if (projParams.length < projCount || viewParams.length < VIEW_PARAMS) {
            mProjResolved = null;
            mViewResolved = null;
        } else {
            mProjResolved = trim(projParams, projCount);
            mViewResolved = trim(viewParams, VIEW_PARAMS);
        }
    }

    /**
     * Returns the number of parameters a projection takes; for an unknown projection, all
     * {@code length} parameters are passed through.
     */
    private static int projectionParamCount(int projection, int length) {
        if (projection == Paint3DContext.PROJECTION_PERSPECTIVE) {
            return 4;
        }
        if (projection == Paint3DContext.PROJECTION_ORTHO) {
            return 6;
        }
        return length;
    }

    /** Returns a copy of the first {@code count} values of {@code params}. */
    private static float @NonNull [] trim(float @NonNull [] params, int count) {
        return params.length == count ? params.clone() : Arrays.copyOf(params, count);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        float[] proj = mProjResolved;
        float[] view = mViewResolved;
        if (proj == null || view == null) {
            return;
        }
        for (int i = 0; i < proj.length; i++) {
            float v = mProjParams[i];
            proj[i] = Utils.isVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
        for (int i = 0; i < view.length; i++) {
            float v = mViewParams[i];
            view[i] = Utils.isVariable(v) ? context.getFloat(Utils.idFromNan(v)) : v;
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (float v : mProjParams) {
            if (Utils.isVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
        for (float v : mViewParams) {
            if (Utils.isVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mProjection, mProjParams, mViewParams);
    }

    /**
     * Write a {@link SetCamera3D} to a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param projection 0=perspective, 1=ortho
     * @param projParams projection parameters
     * @param viewParams view parameters
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int projection,
            float @NonNull [] projParams,
            float @NonNull [] viewParams) {
        buffer.start(OP_CODE);
        buffer.writeInt(projection);
        buffer.writeInt(projParams.length);
        for (float v : projParams) {
            buffer.writeFloat(v);
        }
        buffer.writeInt(viewParams.length);
        for (float v : viewParams) {
            buffer.writeFloat(v);
        }
    }

    /**
     * Read a {@link SetCamera3D} from a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param operations list to add the operation to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int projection = buffer.readInt();
        int projLen = buffer.readInt();
        if (projLen < 0 || projLen > MAX_PARAMS) {
            throw new RuntimeException("SetCamera3D: bad projParams length " + projLen);
        }
        float[] projParams = new float[projLen];
        for (int i = 0; i < projLen; i++) {
            projParams[i] = buffer.readNanId();
        }
        int viewLen = buffer.readInt();
        if (viewLen < 0 || viewLen > MAX_PARAMS) {
            throw new RuntimeException("SetCamera3D: bad viewParams length " + viewLen);
        }
        float[] viewParams = new float[viewLen];
        for (int i = 0; i < viewLen; i++) {
            viewParams[i] = buffer.readNanId();
        }
        if (DEBUG && (projLen < projectionParamCount(projection, projLen)
                || viewLen < VIEW_PARAMS)) {
            Utils.log("SetCamera3D: too few params (projection " + projection + ", " + projLen
                    + " projection, " + viewLen + " view), skipped");
        }
        operations.add(new SetCamera3D(projection, projParams, viewParams));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        float[] proj = mProjResolved;
        float[] view = mViewResolved;
        if (proj != null && view != null && context instanceof Paint3DContext) {
            ((Paint3DContext) context).setCamera3D(mProjection, proj, view);
        }
    }

    /**
     * Documentation for {@link SetCamera3D}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description(
                        "Set both projection and view in one call. Perspective:"
                                + " fovYRad,aspect,near,far. Ortho: l,r,b,t,n,f. View:"
                                + " eyeXYZ,centerXYZ,upXYZ. A reader ignores extra trailing"
                                + " params and skips an op with too few.")
                .field(DocumentedOperation.INT, "projection", "0=perspective, 1=ortho")
                .field(DocumentedOperation.INT, "projLen",
                        "Length of projParams (4 or 6); readers accept up to 256")
                .field(DocumentedOperation.FLOAT_ARRAY, "projParams", "Projection parameters")
                .field(DocumentedOperation.INT, "viewLen",
                        "Length of viewParams (9); readers accept up to 256")
                .field(DocumentedOperation.FLOAT_ARRAY, "viewParams", "lookAt parameters");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("projection", mProjection)
                .add("projLen", mProjParams.length)
                .add("viewLen", mViewParams.length);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(proj=" + mProjection + ")";
    }
}
