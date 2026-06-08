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
package androidx.compose.remote.creation;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.operations.d3.MeshExpression3D;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 3D extension writer for {@link RemoteComposeWriter}, accessed via
 * {@link RemoteComposeWriter#getRc3DWriter()} ({@code mRc3DWriter}).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeWriter3D {
    private final @NonNull RemoteComposeWriter mWriter;

    /**
     * Bit 30 of a {@link #defineMesh3D} id or a {@link #setTexture3D} bitmap id: the advanced form
     * of the operation, defined by a later 3D format version. This version defines none, so the
     * writer refuses it.
     */
    private static final int ID_ADVANCED_BIT = 1 << 30;

    /** {@link #drawMesh3D} mode bits that are reserved: 4–7, 17–29 and 31. */
    private static final int MODE_RESERVED_BITS = 0xF0 | 0x3FFE0000 | 0x80000000;

    /** {@link #meshPrimitive} flag bits that are reserved: 9–31. */
    private static final int PRIMITIVE_RESERVED_BITS = 0xFFFFFE00;

    /** Light type word bits that must be 0: bits 0–7 are the kind, 8–15 the extra-param count. */
    private static final int LIGHT_TYPE_RESERVED_BITS = 0xFFFF0000;

    /**
     * {@link #meshExpression} flags: {@link MeshExpression3D#FLAG_FLIP_WINDING} and {@link
     * MeshExpression3D#FLAG_FLIP_NORMALS}; bits 2 and up are reserved.
     */
    private static final int MESH_EXPRESSION_FLAGS =
            MeshExpression3D.FLAG_FLIP_WINDING | MeshExpression3D.FLAG_FLIP_NORMALS;

    private static final float[] NO_ARGS = new float[0];

    RemoteComposeWriter3D(@NonNull RemoteComposeWriter writer) {
        mWriter = writer;
    }

    /** Refuses an id with {@link #ID_ADVANCED_BIT} set: this version defines no advanced form. */
    private static void checkNoAdvancedBit(@NonNull String what, int id) {
        if ((id & ID_ADVANCED_BIT) != 0) {
            throw new IllegalArgumentException(
                    what
                            + " "
                            + id
                            + " has bit 30 set, the advanced form of a later format version;"
                            + " this version defines none");
        }
    }

    /** Define a cached 3D mesh keyed by {@code id}. Normals may be null (flat shaded). */
    public void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals) {
        defineMesh3D(id, indices, verts, normals, null);
    }

    /**
     * Define a cached 3D mesh with optional per-vertex UV (u,v pairs) for texturing.
     *
     * @throws IllegalArgumentException if {@code id} has bit 30 set (the advanced form of a later
     *     format version)
     */
    public void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals,
            float @Nullable [] uv) {
        checkNoAdvancedBit("defineMesh3D id", id);
        mWriter.mBuffer.addDefineMesh3D(id, indices, verts, normals, uv);
    }

    /**
     * Set 3D projection + view in one call. Perspective: {@code projParams = {fovYRad,
     * aspect, near, far}}. Ortho: {@code projParams = {l, r, b, t, n, f}}. {@code
     * viewParams = {eyeX,eyeY,eyeZ, centerX,centerY,centerZ, upX,upY,upZ}}.
     *
     * @throws IllegalArgumentException if {@code projection} is neither
     *     {@link Paint3DContext#PROJECTION_PERSPECTIVE} nor {@link Paint3DContext#PROJECTION_ORTHO}
     */
    public void setCamera3D(
            int projection, float @NonNull [] projParams, float @NonNull [] viewParams) {
        if (projection != Paint3DContext.PROJECTION_PERSPECTIVE
                && projection != Paint3DContext.PROJECTION_ORTHO) {
            throw new IllegalArgumentException("Unknown setCamera3D projection " + projection
                    + " (expected 0 = perspective or 1 = ortho)");
        }
        mWriter.mBuffer.addSetCamera3D(projection, projParams, viewParams);
    }

    /**
     * Post-multiply current 3D modelview. {@code sub}: 0=IDENTITY, 1=TRANSLATE(x,y,z),
     * 2=SCALE(x,y,z), 3=ROTATE_AXIS(angleRad,ax,ay,az), 4=MULTIPLY(col-major 4×4), 5=PUSH and
     * 6=POP (no arguments, see {@link #matrix3DPush()} and {@link #matrix3DPop()}).
     *
     * @throws IllegalArgumentException for an unknown sub-op, or PUSH/POP with arguments
     */
    public void matrix3DOp(int sub, float @NonNull [] args) {
        if (sub < Paint3DContext.M3_IDENTITY || sub > Paint3DContext.M3_POP) {
            throw new IllegalArgumentException(
                    "Unknown matrix3DOp sub-op " + sub + " (expected 0..6)");
        }
        if (sub == Paint3DContext.M3_PUSH || sub == Paint3DContext.M3_POP) {
            String name = sub == Paint3DContext.M3_PUSH ? "matrix3DOp PUSH" : "matrix3DOp POP";
            if (args.length != 0) {
                throw new IllegalArgumentException(
                        name + " takes no arguments, got " + args.length);
            }
        }
        mWriter.mBuffer.addMatrix3DOp(sub, args);
    }

    /**
     * Save a copy of the current 3D model matrix, to be restored by the matching {@link
     * #matrix3DPop()}. The camera (projection and view) is not saved.
     */
    public void matrix3DPush() {
        matrix3DOp(Paint3DContext.M3_PUSH, NO_ARGS);
    }

    /** Restore the 3D model matrix saved by the matching {@link #matrix3DPush()}. */
    public void matrix3DPop() {
        matrix3DOp(Paint3DContext.M3_POP, NO_ARGS);
    }

    /**
     * Draw a cached 3D mesh with the current paint color and current matrices.
     *
     * @param meshId mesh id from {@link #defineMesh3D}
     * @param mode   render mode (backend + shading), optionally with the wireframe bits and the
     *     flags {@link Paint3DContext#MODE_TRANSPARENT}, {@link Paint3DContext#MODE_GPU}, {@link
     *     Paint3DContext#MODE_PBR}, {@link Paint3DContext#MODE_DOUBLE_SIDED} and {@link
     *     Paint3DContext#MODE_FRONT_CW}
     * @throws IllegalArgumentException if {@code mode} sets a reserved bit (4–7, 17–29, 31) or
     *     {@link Paint3DContext#MODE_ADVANCED} (this version defines no advanced form)
     */
    public void drawMesh3D(int meshId, int mode) {
        if ((mode & Paint3DContext.MODE_ADVANCED) != 0) {
            throw new IllegalArgumentException(
                    "drawMesh3D mode 0x"
                            + Integer.toHexString(mode)
                            + " sets MODE_ADVANCED (bit 30), the advanced form of a later format"
                            + " version; this version defines none");
        }
        if ((mode & MODE_RESERVED_BITS) != 0) {
            throw new IllegalArgumentException(
                    "drawMesh3D mode 0x"
                            + Integer.toHexString(mode)
                            + " sets reserved bits 0x"
                            + Integer.toHexString(mode & MODE_RESERVED_BITS)
                            + " (bits 4-7, 17-29 and 31 are reserved)");
        }
        mWriter.mBuffer.addDrawMesh3D(meshId, mode);
    }

    /** Clear the 3D depth buffer. Required to start a 3D pass cleanly. */
    public void clearDepth3D() {
        mWriter.mBuffer.addClearDepth3D();
    }

    /**
     * Define the whole 3D light set in one op. {@code types[i]}: 0=directional
     * {@code {dirX,dirY,dirZ,intensity}}, 1=point {@code {posX,posY,posZ,intensity}} (world space)
     * and 2=ambient {@code {0,0,0,intensity}}; {@code colors[i]} is the light color; {@code params}
     * holds 4 floats per light. An empty set means ambient only.
     *
     * <p>A type word may also carry a count of extra params in bits 8–15 ({@link
     * Paint3DContext#LIGHT_EXTRA_SHIFT}); such a light has {@code 4 + extra} floats in {@code
     * params}, and players ignore the extra params they do not know.
     *
     * @throws IllegalArgumentException for an unknown light kind, a type word with bits 16–31
     *     set, or {@code colors} or {@code params} that do not match the type words
     */
    public void setLights3D(int @NonNull [] types, int @NonNull [] colors,
            float @NonNull [] params) {
        int expectedParams = 0;
        for (int type : types) {
            if ((type & LIGHT_TYPE_RESERVED_BITS) != 0) {
                throw new IllegalArgumentException(
                        "setLights3D type word 0x"
                                + Integer.toHexString(type)
                                + " sets reserved bits 16-31");
            }
            int kind = type & Paint3DContext.LIGHT_KIND_MASK;
            int extra =
                    (type >> Paint3DContext.LIGHT_EXTRA_SHIFT) & Paint3DContext.LIGHT_EXTRA_MASK;
            if (kind > Paint3DContext.LIGHT_AMBIENT) {
                throw new IllegalArgumentException(
                        "Unknown setLights3D light kind "
                                + kind
                                + " (expected 0=directional, 1=point, 2=ambient)");
            }
            expectedParams += 4 + extra;
        }
        if (colors.length != types.length || params.length != expectedParams) {
            throw new IllegalArgumentException(
                    "setLights3D: "
                            + types.length
                            + " lights need "
                            + types.length
                            + " colors and "
                            + expectedParams
                            + " params, got "
                            + colors.length
                            + " and "
                            + params.length);
        }
        mWriter.mBuffer.addSetLights3D(types, colors, params);
    }

    /**
     * Set the active 3D texture (image id) for subsequent {@link #drawMesh3D} calls; 0 clears.
     *
     * @throws IllegalArgumentException if {@code bitmapId} has bit 30 set (the advanced form of a
     *     later format version)
     */
    public void setTexture3D(int bitmapId) {
        checkNoAdvancedBit("setTexture3D bitmapId", bitmapId);
        mWriter.mBuffer.addSetTexture3D(bitmapId);
    }

    /**
     * Set the specular material for subsequent {@link #drawMesh3D} calls. {@code specStrength}
     * scales an additive highlight in each light's color; {@code shininess} is its exponent
     * (higher = tighter). {@code specStrength == 0} restores the default matte material.
     */
    public void setMaterial3D(float specStrength, float shininess) {
        mWriter.mBuffer.addSetMaterial3D(specStrength, shininess);
    }

    /** Checks a {@link #meshPrimitive} type and flags: unknown types and flag bits 9–31 fail. */
    private static void checkMeshPrimitive(int type, int flags) {
        if (type < 0 || type > Primitive3D.INFINITE_PLANE) {
            throw new IllegalArgumentException(
                    "Unknown meshPrimitive type "
                            + type
                            + " (expected 0.."
                            + Primitive3D.INFINITE_PLANE
                            + ")");
        }
        if ((flags & PRIMITIVE_RESERVED_BITS) != 0) {
            throw new IllegalArgumentException(
                    "meshPrimitive flags 0x"
                            + Integer.toHexString(flags)
                            + " set reserved bits 0x"
                            + Integer.toHexString(flags & PRIMITIVE_RESERVED_BITS)
                            + " (bits 9-31 are reserved)");
        }
    }

    /**
     * Define a 3D mesh by id from a parametric primitive.
     *
     * @throws IllegalArgumentException for an unknown primitive type or a reserved flag bit (9–31)
     */
    public void meshPrimitive(
            int id, int type, float segments, int flags, float @NonNull [] params) {
        checkMeshPrimitive(type, flags);
        mWriter.mBuffer.addMeshPrimitive(id, type, segments, flags, params);
    }

    /**
     * Multi-channel variant: {@code data[0]} = scalar params, channels {@code 1+} carry geometry
     * streams (profile / path points) for the multi-channel primitive types (lathe, sweep, …).
     *
     * @throws IllegalArgumentException for an unknown primitive type or a reserved flag bit (9–31)
     */
    public void meshPrimitive(
            int id, int type, float segments, int flags, float @NonNull [][] data) {
        checkMeshPrimitive(type, flags);
        mWriter.mBuffer.addMeshPrimitive(id, type, segments, flags, data);
    }

    /**
     * Set a polygon-offset depth bias (window-z units) for subsequent {@link #drawMesh3D} calls.
     */
    public void setDepthBias3D(float constant, float slope) {
        mWriter.mBuffer.addSetDepthBias3D(constant, slope);
    }

    /**
     * Define a procedural 3D mesh by id from float-expressions over a (u,v) grid. {@code flags}
     * may set {@link MeshExpression3D#FLAG_FLIP_WINDING} and {@link
     * MeshExpression3D#FLAG_FLIP_NORMALS}.
     *
     * @throws IllegalArgumentException for an unknown surface type (players skip such a mesh) or
     *     a flag bit other than those two
     */
    public void meshExpression(
            int id,
            int type,
            int flags,
            float @NonNull [] params,
            float @NonNull [] @NonNull [] pos,
            float @NonNull [] @NonNull [] normal,
            float @NonNull [] @NonNull [] uv) {
        if (type < MeshExpression3D.SURFACE_GENERAL || type > MeshExpression3D.SURFACE_CYLINDER) {
            throw new IllegalArgumentException(
                    "Unknown meshExpression surface type "
                            + type
                            + " (expected "
                            + MeshExpression3D.SURFACE_GENERAL
                            + ".."
                            + MeshExpression3D.SURFACE_CYLINDER
                            + ")");
        }
        if ((flags & ~MESH_EXPRESSION_FLAGS) != 0) {
            throw new IllegalArgumentException(
                    "meshExpression flags 0x"
                            + Integer.toHexString(flags)
                            + " set reserved bits 0x"
                            + Integer.toHexString(flags & ~MESH_EXPRESSION_FLAGS)
                            + " (bits 2-31 are reserved)");
        }
        mWriter.mBuffer.addMeshExpression(id, type, flags, params, pos, normal, uv);
    }
}
