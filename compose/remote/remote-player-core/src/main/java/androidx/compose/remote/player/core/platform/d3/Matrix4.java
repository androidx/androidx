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
package androidx.compose.remote.player.core.platform.d3;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * 4×4 matrix helpers using {@code float[16]} in column-major order — the same layout
 * OpenGL uses. Indexing: {@code m[col*4 + row]}.
 *
 * <p>Right-handed coordinates, +Y up, −Z forward.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Matrix4 {
    private Matrix4() {
    }

    /** Write the identity matrix into {@code out}. */
    public static void identity(float @NonNull [] out) {
        out[0] = 1;
        out[1] = 0;
        out[2] = 0;
        out[3] = 0;
        out[4] = 0;
        out[5] = 1;
        out[6] = 0;
        out[7] = 0;
        out[8] = 0;
        out[9] = 0;
        out[10] = 1;
        out[11] = 0;
        out[12] = 0;
        out[13] = 0;
        out[14] = 0;
        out[15] = 1;
    }

    /** Copy {@code src} into {@code dst}. */
    public static void copy(float @NonNull [] dst, float @NonNull [] src) {
        System.arraycopy(src, 0, dst, 0, 16);
    }

    /**
     * {@code out = a × b} (column-major; equivalent to {@code a} applied after {@code b} to
     * a column vector). {@code out} may alias {@code a} or {@code b}: both operands are read
     * into locals before anything is written, so no scratch buffer is allocated.
     */
    public static void multiply(float @NonNull [] out, float @NonNull [] a, float @NonNull [] b) {
        float a0 = a[0], a1 = a[1], a2 = a[2], a3 = a[3];
        float a4 = a[4], a5 = a[5], a6 = a[6], a7 = a[7];
        float a8 = a[8], a9 = a[9], a10 = a[10], a11 = a[11];
        float a12 = a[12], a13 = a[13], a14 = a[14], a15 = a[15];
        float b0 = b[0], b1 = b[1], b2 = b[2], b3 = b[3];
        float b4 = b[4], b5 = b[5], b6 = b[6], b7 = b[7];
        float b8 = b[8], b9 = b[9], b10 = b[10], b11 = b[11];
        float b12 = b[12], b13 = b[13], b14 = b[14], b15 = b[15];
        // Element (row, col) = Σk a[k*4 + row] * b[col*4 + k], summed in k order 0..3.
        out[0] = a0 * b0 + a4 * b1 + a8 * b2 + a12 * b3;
        out[1] = a1 * b0 + a5 * b1 + a9 * b2 + a13 * b3;
        out[2] = a2 * b0 + a6 * b1 + a10 * b2 + a14 * b3;
        out[3] = a3 * b0 + a7 * b1 + a11 * b2 + a15 * b3;
        out[4] = a0 * b4 + a4 * b5 + a8 * b6 + a12 * b7;
        out[5] = a1 * b4 + a5 * b5 + a9 * b6 + a13 * b7;
        out[6] = a2 * b4 + a6 * b5 + a10 * b6 + a14 * b7;
        out[7] = a3 * b4 + a7 * b5 + a11 * b6 + a15 * b7;
        out[8] = a0 * b8 + a4 * b9 + a8 * b10 + a12 * b11;
        out[9] = a1 * b8 + a5 * b9 + a9 * b10 + a13 * b11;
        out[10] = a2 * b8 + a6 * b9 + a10 * b10 + a14 * b11;
        out[11] = a3 * b8 + a7 * b9 + a11 * b10 + a15 * b11;
        out[12] = a0 * b12 + a4 * b13 + a8 * b14 + a12 * b15;
        out[13] = a1 * b12 + a5 * b13 + a9 * b14 + a13 * b15;
        out[14] = a2 * b12 + a6 * b13 + a10 * b14 + a14 * b15;
        out[15] = a3 * b12 + a7 * b13 + a11 * b14 + a15 * b15;
    }

    /**
     * Post-multiply {@code m} by a translation ({@code m := m × T}), so subsequent
     * vertices are translated in local space — {@code glTranslate} semantics.
     */
    public static void translate(float @NonNull [] m, float tx, float ty, float tz) {
        m[12] += m[0] * tx + m[4] * ty + m[8] * tz;
        m[13] += m[1] * tx + m[5] * ty + m[9] * tz;
        m[14] += m[2] * tx + m[6] * ty + m[10] * tz;
        m[15] += m[3] * tx + m[7] * ty + m[11] * tz;
    }

    /** Post-multiply {@code m} by a non-uniform scale — {@code glScale} semantics. */
    public static void scale(float @NonNull [] m, float sx, float sy, float sz) {
        m[0] *= sx;
        m[1] *= sx;
        m[2] *= sx;
        m[3] *= sx;
        m[4] *= sy;
        m[5] *= sy;
        m[6] *= sy;
        m[7] *= sy;
        m[8] *= sz;
        m[9] *= sz;
        m[10] *= sz;
        m[11] *= sz;
    }

    /**
     * Post-multiply {@code m} by a rotation of {@code angleRadians} around the (possibly
     * unnormalized) axis {@code (x, y, z)} — {@code glRotate} semantics, but with the angle
     * in radians.
     */
    public static void rotateAxis(float @NonNull [] m, float angleRadians, float x, float y,
            float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0f) {
            return;
        }
        float ix = x / len;
        float iy = y / len;
        float iz = z / len;
        float c = (float) Math.cos(angleRadians);
        float s = (float) Math.sin(angleRadians);
        float omc = 1f - c;

        float r00 = ix * ix * omc + c;
        float r01 = iy * ix * omc + iz * s;
        float r02 = iz * ix * omc - iy * s;

        float r10 = ix * iy * omc - iz * s;
        float r11 = iy * iy * omc + c;
        float r12 = iz * iy * omc + ix * s;

        float r20 = ix * iz * omc + iy * s;
        float r21 = iy * iz * omc - ix * s;
        float r22 = iz * iz * omc + c;

        float a00 = m[0], a01 = m[1], a02 = m[2], a03 = m[3];
        float a10 = m[4], a11 = m[5], a12 = m[6], a13 = m[7];
        float a20 = m[8], a21 = m[9], a22 = m[10], a23 = m[11];

        m[0] = a00 * r00 + a10 * r01 + a20 * r02;
        m[1] = a01 * r00 + a11 * r01 + a21 * r02;
        m[2] = a02 * r00 + a12 * r01 + a22 * r02;
        m[3] = a03 * r00 + a13 * r01 + a23 * r02;

        m[4] = a00 * r10 + a10 * r11 + a20 * r12;
        m[5] = a01 * r10 + a11 * r11 + a21 * r12;
        m[6] = a02 * r10 + a12 * r11 + a22 * r12;
        m[7] = a03 * r10 + a13 * r11 + a23 * r12;

        m[8] = a00 * r20 + a10 * r21 + a20 * r22;
        m[9] = a01 * r20 + a11 * r21 + a21 * r22;
        m[10] = a02 * r20 + a12 * r21 + a22 * r22;
        m[11] = a03 * r20 + a13 * r21 + a23 * r22;
    }

    /**
     * Write a {@code gluPerspective} projection into {@code out}. {@code fovYRadians} is
     * the full vertical field of view in radians.
     */
    public static void perspective(
            float @NonNull [] out, float fovYRadians, float aspect, float near, float far) {
        float f = (float) (1.0 / Math.tan(fovYRadians * 0.5));
        float nf = 1f / (near - far);
        out[0] = f / aspect;
        out[1] = 0;
        out[2] = 0;
        out[3] = 0;

        out[4] = 0;
        out[5] = f;
        out[6] = 0;
        out[7] = 0;

        out[8] = 0;
        out[9] = 0;
        out[10] = (far + near) * nf;
        out[11] = -1;

        out[12] = 0;
        out[13] = 0;
        out[14] = 2f * far * near * nf;
        out[15] = 0;
    }

    /** Write a {@code glOrtho} projection into {@code out}. */
    public static void ortho(
            float @NonNull [] out,
            float left,
            float right,
            float bottom,
            float top,
            float near,
            float far) {
        float lr = 1f / (left - right);
        float bt = 1f / (bottom - top);
        float nf = 1f / (near - far);
        out[0] = -2f * lr;
        out[1] = 0;
        out[2] = 0;
        out[3] = 0;

        out[4] = 0;
        out[5] = -2f * bt;
        out[6] = 0;
        out[7] = 0;

        out[8] = 0;
        out[9] = 0;
        out[10] = 2f * nf;
        out[11] = 0;

        out[12] = (left + right) * lr;
        out[13] = (top + bottom) * bt;
        out[14] = (far + near) * nf;
        out[15] = 1;
    }

    /**
     * Write a {@code gluLookAt} view matrix into {@code out}. Right-handed; the camera
     * looks from {@code eye} toward {@code center} with the given {@code up} reference.
     */
    public static void lookAt(
            float @NonNull [] out,
            float eyeX,
            float eyeY,
            float eyeZ,
            float centerX,
            float centerY,
            float centerZ,
            float upX,
            float upY,
            float upZ) {
        float fx = centerX - eyeX;
        float fy = centerY - eyeY;
        float fz = centerZ - eyeZ;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl == 0f) {
            identity(out);
            return;
        }
        fx /= fl;
        fy /= fl;
        fz /= fl;

        // s = f × up
        float sx = fy * upZ - fz * upY;
        float sy = fz * upX - fx * upZ;
        float sz = fx * upY - fy * upX;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl == 0f) {
            identity(out);
            return;
        }
        sx /= sl;
        sy /= sl;
        sz /= sl;

        // u = s × f
        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        out[0] = sx;
        out[1] = ux;
        out[2] = -fx;
        out[3] = 0;

        out[4] = sy;
        out[5] = uy;
        out[6] = -fy;
        out[7] = 0;

        out[8] = sz;
        out[9] = uz;
        out[10] = -fz;
        out[11] = 0;

        out[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ);
        out[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
        out[14] = fx * eyeX + fy * eyeY + fz * eyeZ;
        out[15] = 1;
    }

    /**
     * Transform a homogeneous point {@code (x, y, z, 1)} by {@code m}, writing 4
     * floats {@code (x', y', z', w')} into {@code out4}.
     */
    public static void transformPoint(
            float @NonNull [] m, float x, float y, float z, float @NonNull [] out4) {
        out4[0] = m[0] * x + m[4] * y + m[8] * z + m[12];
        out4[1] = m[1] * x + m[5] * y + m[9] * z + m[13];
        out4[2] = m[2] * x + m[6] * y + m[10] * z + m[14];
        out4[3] = m[3] * x + m[7] * y + m[11] * z + m[15];
    }

    /**
     * Transform the direction {@code (x, y, z, 0)} by {@code m} (ignores translation),
     * writing 3 floats into {@code out3}.
     */
    public static void transformDirection(
            float @NonNull [] m, float x, float y, float z, float @NonNull [] out3) {
        out3[0] = m[0] * x + m[4] * y + m[8] * z;
        out3[1] = m[1] * x + m[5] * y + m[9] * z;
        out3[2] = m[2] * x + m[6] * y + m[10] * z;
    }
}
