/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.compose.remote.core.testing;

import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.DrawMesh2D;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.paint.PaintChangeAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

/**
 * A {@link PaintContext} that rasterises 2D meshes into an in-memory ARGB framebuffer, so mesh
 * geometry can be checked by looking at it rather than only by reading numbers.
 *
 * <p>Meshes are the one part of RemoteCompose where a plausible-looking set of vertices and a
 * correct one are hard to tell apart from assertions alone: winding, uv orientation and the local
 * frame extracted by {@code matrixFromMesh2D} all produce geometry that passes a bounds check while
 * being visibly wrong. This class exists so a test can assert on pixels, and so a human can open
 * the PNG when an assertion is surprising.
 *
 * <p>It is deliberately a scanline triangle filler with Gouraud interpolation and nothing else - no
 * anti-aliasing, no clipping beyond the framebuffer, no texture filtering. Exact pixel values are
 * therefore reproducible across machines, which is what makes pixel assertions safe.
 */
public class SoftwareRasterPaintContext extends PaintContext {

    private final int mWidth;
    private final int mHeight;
    private final int[] mPixels;

    /** Active target buffer (either the main framebuffer or an offscreen bitmap). */
    private int mTargetWidth;

    private int mTargetHeight;
    private int[] mTargetPixels;

    /** The 2x3 affine applied to mesh vertices, row major: [a, b, c, d, tx, ty]. */
    private float[] mMatrix = {1f, 0f, 0f, 1f, 0f, 0f};

    private final java.util.ArrayDeque<float[]> mMatrixStack = new java.util.ArrayDeque<>();

    private int mPaintColor = 0xFFFFFFFF;
    private int mPaintStyle = PaintBundle.STYLE_FILL;
    private float mStrokeWidth = 1f;
    private float mTextSize = 16f;
    private final java.util.ArrayDeque<int[]> mPaintStack = new java.util.ArrayDeque<>();

    private final PaintChangeAdapter mPaintAdapter =
            new PaintChangeAdapter() {
                @Override
                public void setColor(int color) {
                    mPaintColor = color;
                }

                @Override
                public void setStyle(int style) {
                    mPaintStyle = style;
                }

                @Override
                public void setStrokeWidth(float width) {
                    mStrokeWidth = width;
                }

                @Override
                public void setTextSize(float size) {
                    mTextSize = size;
                }
            };

    private final HashMap<Integer, Mesh> mMeshes = new HashMap<>();

    /** A mesh as the backend receives it: flat arrays and topology metadata. */
    private static class Mesh {
        final int mLayout;
        final int mUCount;
        final int mVCount;
        final float[] mVerts;
        final float[] mUv;
        final int[] mColors;
        final int[] mIndices;

        Mesh(
                int layout,
                int uCount,
                int vCount,
                float[] verts,
                float[] uv,
                int[] colors,
                int[] indices) {
            this.mLayout = layout;
            this.mUCount = uCount;
            this.mVCount = vCount;
            this.mVerts = verts;
            this.mUv = uv;
            this.mColors = colors;
            this.mIndices = indices;
        }
    }

    public SoftwareRasterPaintContext(@NonNull RemoteContext context, int width, int height) {
        super(context);
        context.mWidth = width;
        context.mHeight = height;
        mWidth = width;
        mHeight = height;
        mPixels = new int[width * height];
        mTargetWidth = width;
        mTargetHeight = height;
        mTargetPixels = mPixels;
        clear(0xFF000000);
    }

    /** Fill the framebuffer with a single colour. */
    public void clear(int argb) {
        java.util.Arrays.fill(mPixels, argb);
    }

    /** The colour at a pixel, as packed ARGB. */
    public int pixelAt(int x, int y) {
        if (x < 0 || y < 0 || x >= mWidth || y >= mHeight) {
            return 0;
        }
        return mPixels[y * mWidth + x];
    }

    /** How many pixels are not the background colour - a cheap "did anything draw" check. */
    public int coveredPixels(int background) {
        int count = 0;
        for (int pixel : mPixels) {
            if (pixel != background) {
                count++;
            }
        }
        return count;
    }

    /**
     * Write the framebuffer to a PNG so a failure can be looked at.
     *
     * @return the file written, for a test to name in its failure message
     */
    public @NonNull File writePng(@NonNull String name) throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), "remotecompose-mesh-tests");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create " + dir);
        }
        BufferedImage image = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, mWidth, mHeight, mPixels, 0, mWidth);
        File file = new File(dir, name + ".png");
        ImageIO.write(image, "png", file);
        return file;
    }

    // ---------------------------------------------------------------- mesh

    @Override
    public void setMesh(
            int meshId,
            int layout,
            int uCount,
            int vCount,
            float @NonNull [] verts,
            float @NonNull [] uv,
            int @NonNull [] colors,
            int @NonNull [] indices) {
        // The contract is non-null but possibly empty: an empty uv or colours array means the mesh
        // carries no such channel, which is not the same as every vertex being black.
        mMeshes.put(
                meshId,
                new Mesh(
                        layout,
                        uCount,
                        vCount,
                        verts.clone(),
                        uv.length == 0 ? null : uv.clone(),
                        colors.length == 0 ? null : colors.clone(),
                        indices.clone()));
    }

    @Override
    public void drawMesh(int meshId, int blend, int imageId) {
        Mesh mesh = mMeshes.get(meshId);
        if (mesh == null) {
            return;
        }
        HeadlessRemoteContext.BitmapBuffer texture = null;
        if (imageId != DrawMesh2D.NO_IMAGE
                && blend == DrawMesh2D.BLEND_MODULATE
                && mContext instanceof HeadlessRemoteContext) {
            texture = ((HeadlessRemoteContext) mContext).getBitmap(imageId);
        }
        for (int i = 0; i + 2 < mesh.mIndices.length; i += 3) {
            int a = mesh.mIndices[i];
            int b = mesh.mIndices[i + 1];
            int c = mesh.mIndices[i + 2];
            triangle(mesh, a, b, c, texture);
        }
    }

    @Override
    public void matrixFromMesh(int meshId, float u, float v, int flags) {
        Mesh mesh = mMeshes.get(meshId);
        if (mesh == null) {
            return;
        }
        float[] affine = new float[6];
        if (androidx.compose.remote.core.operations.utilities.Mesh2DGenerator.computeMatrixFromMesh(
                mesh.mLayout, mesh.mUCount, mesh.mVCount, mesh.mVerts, u, v, flags, affine)) {
            mMatrix = multiply(mMatrix, affine);
        }
    }

    /** The current 2x3 affine, so a test can assert on what a mesh's local frame produced. */
    public float @NonNull [] currentMatrix() {
        return mMatrix.clone();
    }

    // ------------------------------------------------------------ matrices

    private static float[] multiply(float[] m, float[] n) {
        // Both are row major [a, b, c, d, tx, ty] representing
        //   | a c tx |
        //   | b d ty |
        return new float[]{
                m[0] * n[0] + m[2] * n[1],
                m[1] * n[0] + m[3] * n[1],
                m[0] * n[2] + m[2] * n[3],
                m[1] * n[2] + m[3] * n[3],
                m[0] * n[4] + m[2] * n[5] + m[4],
                m[1] * n[4] + m[3] * n[5] + m[5],
        };
    }

    private void mapPoint(float x, float y, float[] out) {
        out[0] = mMatrix[0] * x + mMatrix[2] * y + mMatrix[4];
        out[1] = mMatrix[1] * x + mMatrix[3] * y + mMatrix[5];
    }

    @Override
    public void matrixSave() {
        mMatrixStack.push(mMatrix.clone());
    }

    @Override
    public void matrixRestore() {
        if (!mMatrixStack.isEmpty()) {
            mMatrix = mMatrixStack.pop();
        }
    }

    @Override
    public void matrixTranslate(float translateX, float translateY) {
        mMatrix = multiply(mMatrix, new float[]{1f, 0f, 0f, 1f, translateX, translateY});
    }

    @Override
    public void translate(float translateX, float translateY) {
        matrixTranslate(translateX, translateY);
    }

    @Override
    public void matrixScale(float scaleX, float scaleY, float centerX, float centerY) {
        if (Float.isNaN(centerX) || Float.isNaN(centerY)) {
            mMatrix = multiply(mMatrix, new float[]{scaleX, 0f, 0f, scaleY, 0f, 0f});
            return;
        }
        matrixTranslate(centerX, centerY);
        mMatrix = multiply(mMatrix, new float[]{scaleX, 0f, 0f, scaleY, 0f, 0f});
        matrixTranslate(-centerX, -centerY);
    }

    @Override
    public void scale(float scaleX, float scaleY) {
        mMatrix = multiply(mMatrix, new float[]{scaleX, 0f, 0f, scaleY, 0f, 0f});
    }

    @Override
    public void matrixRotate(float rotate, float pivotX, float pivotY) {
        double radians = Math.toRadians(rotate);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        boolean pivoted = !Float.isNaN(pivotX) && !Float.isNaN(pivotY);
        if (pivoted) {
            matrixTranslate(pivotX, pivotY);
        }
        mMatrix = multiply(mMatrix, new float[]{cos, sin, -sin, cos, 0f, 0f});
        if (pivoted) {
            matrixTranslate(-pivotX, -pivotY);
        }
    }

    @Override
    public void matrixSkew(float skewX, float skewY) {
        mMatrix = multiply(mMatrix, new float[]{1f, skewY, skewX, 1f, 0f, 0f});
    }

    // ---------------------------------------------------------- rasteriser

    private final float[] mP0 = new float[2];
    private final float[] mP1 = new float[2];
    private final float[] mP2 = new float[2];

    private void triangle(
            Mesh mesh,
            int ia,
            int ib,
            int ic,
            HeadlessRemoteContext.@Nullable BitmapBuffer texture) {
        mapPoint(mesh.mVerts[ia * 2], mesh.mVerts[ia * 2 + 1], mP0);
        mapPoint(mesh.mVerts[ib * 2], mesh.mVerts[ib * 2 + 1], mP1);
        mapPoint(mesh.mVerts[ic * 2], mesh.mVerts[ic * 2 + 1], mP2);

        float x0 = mP0[0];
        float y0 = mP0[1];
        float x1 = mP1[0];
        float y1 = mP1[1];
        float x2 = mP2[0];
        float y2 = mP2[1];

        float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        if (Math.abs(area) < 1e-9f) {
            return;
        }

        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
        int maxX = Math.min(mTargetWidth - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
        int maxY = Math.min(mTargetHeight - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));

        int colorA = mesh.mColors == null ? 0xFFFFFFFF : mesh.mColors[ia];
        int colorB = mesh.mColors == null ? 0xFFFFFFFF : mesh.mColors[ib];
        int colorC = mesh.mColors == null ? 0xFFFFFFFF : mesh.mColors[ic];

        boolean hasUv = texture != null && mesh.mUv != null;
        float u0 = hasUv ? mesh.mUv[ia * 2] : 0f;
        float v0 = hasUv ? mesh.mUv[ia * 2 + 1] : 0f;
        float u1 = hasUv ? mesh.mUv[ib * 2] : 0f;
        float v1 = hasUv ? mesh.mUv[ib * 2 + 1] : 0f;
        float u2 = hasUv ? mesh.mUv[ic * 2] : 0f;
        float v2 = hasUv ? mesh.mUv[ic * 2 + 1] : 0f;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                // Barycentrics, normalised by the signed area so either winding fills.
                float w0 = ((x1 - px) * (y2 - py) - (x2 - px) * (y1 - py)) / area;
                float w1 = ((x2 - px) * (y0 - py) - (x0 - px) * (y2 - py)) / area;
                float w2 = 1f - w0 - w1;
                if (w0 < 0f || w1 < 0f || w2 < 0f) {
                    continue;
                }
                int src = blend(colorA, colorB, colorC, w0, w1, w2);
                if (hasUv) {
                    float u = u0 * w0 + u1 * w1 + u2 * w2;
                    float v = v0 * w0 + v1 * w1 + v2 * w2;
                    int tx =
                            Math.max(
                                    0,
                                    Math.min(
                                            texture.width - 1,
                                            Math.round(u * (texture.width - 1))));
                    int ty =
                            Math.max(
                                    0,
                                    Math.min(
                                            texture.height - 1,
                                            Math.round(v * (texture.height - 1))));
                    int texel = texture.pixels[ty * texture.width + tx];
                    src = modulate(src, texel);
                }
                int idx = y * mTargetWidth + x;
                mTargetPixels[idx] = composite(mTargetPixels[idx], src);
            }
        }
    }

    private void fillSolidTriangle(
            float lx0, float ly0, float lx1, float ly1, float lx2, float ly2, int color) {
        mapPoint(lx0, ly0, mP0);
        mapPoint(lx1, ly1, mP1);
        mapPoint(lx2, ly2, mP2);

        float x0 = mP0[0];
        float y0 = mP0[1];
        float x1 = mP1[0];
        float y1 = mP1[1];
        float x2 = mP2[0];
        float y2 = mP2[1];

        float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        if (Math.abs(area) < 1e-9f) {
            return;
        }

        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
        int maxX = Math.min(mTargetWidth - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
        int maxY = Math.min(mTargetHeight - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float w0 = ((x1 - px) * (y2 - py) - (x2 - px) * (y1 - py)) / area;
                float w1 = ((x2 - px) * (y0 - py) - (x0 - px) * (y2 - py)) / area;
                float w2 = 1f - w0 - w1;
                if (w0 < 0f || w1 < 0f || w2 < 0f) {
                    continue;
                }
                int idx = y * mTargetWidth + x;
                mTargetPixels[idx] = composite(mTargetPixels[idx], color);
            }
        }
    }

    private static int modulate(int c1, int c2) {
        int a = (((c1 >>> 24) & 0xFF) * ((c2 >>> 24) & 0xFF) + 127) / 255;
        int r = (((c1 >>> 16) & 0xFF) * ((c2 >>> 16) & 0xFF) + 127) / 255;
        int g = (((c1 >>> 8) & 0xFF) * ((c2 >>> 8) & 0xFF) + 127) / 255;
        int b = ((c1 & 0xFF) * (c2 & 0xFF) + 127) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int composite(int dst, int src) {
        int sa = (src >>> 24) & 0xFF;
        if (sa == 255) {
            return src;
        }
        if (sa == 0) {
            return dst;
        }
        int invA = 255 - sa;
        int r = (((src >>> 16) & 0xFF) * sa + ((dst >>> 16) & 0xFF) * invA + 127) / 255;
        int g = (((src >>> 8) & 0xFF) * sa + ((dst >>> 8) & 0xFF) * invA + 127) / 255;
        int b = ((src & 0xFF) * sa + (dst & 0xFF) * invA + 127) / 255;
        int a = Math.min(255, sa + (((dst >>> 24) & 0xFF) * invA + 127) / 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blend(int ca, int cb, int cc, float w0, float w1, float w2) {
        int a = channel(ca, 24, cb, cc, w0, w1, w2);
        int r = channel(ca, 16, cb, cc, w0, w1, w2);
        int g = channel(ca, 8, cb, cc, w0, w1, w2);
        int b = channel(ca, 0, cb, cc, w0, w1, w2);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int ca, int shift, int cb, int cc, float w0, float w1, float w2) {
        float value =
                ((ca >> shift) & 0xFF) * w0
                        + ((cb >> shift) & 0xFF) * w1
                        + ((cc >> shift) & 0xFF) * w2;
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    // ------------------------------------------------- primitives & bitmaps

    @Override
    public void drawBitmap(
            int imageId,
            int srcLeft,
            int srcTop,
            int srcRight,
            int srcBottom,
            int dstLeft,
            int dstTop,
            int dstRight,
            int dstBottom,
            int cdId) {
    }

    @Override
    public void drawArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
    }

    @Override
    public void drawSector(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
    }

    @Override
    public void drawBitmap(int id, float left, float top, float right, float bottom) {
    }

    @Override
    public void drawCircle(float centerX, float centerY, float radius) {
        int steps = 36;
        if (mPaintStyle == PaintBundle.STYLE_STROKE) {
            float halfW = Math.max(0.5f, mStrokeWidth * 0.5f);
            float rIn = Math.max(0f, radius - halfW);
            float rOut = radius + halfW;
            for (int i = 0; i < steps; i++) {
                double a0 = (i * 2.0 * Math.PI) / steps;
                double a1 = ((i + 1) * 2.0 * Math.PI) / steps;
                float c0 = (float) Math.cos(a0);
                float s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float s1 = (float) Math.sin(a1);
                fillSolidTriangle(
                        centerX + c0 * rIn,
                        centerY + s0 * rIn,
                        centerX + c0 * rOut,
                        centerY + s0 * rOut,
                        centerX + c1 * rOut,
                        centerY + s1 * rOut,
                        mPaintColor);
                fillSolidTriangle(
                        centerX + c0 * rIn,
                        centerY + s0 * rIn,
                        centerX + c1 * rOut,
                        centerY + s1 * rOut,
                        centerX + c1 * rIn,
                        centerY + s1 * rIn,
                        mPaintColor);
            }
        } else {
            for (int i = 0; i < steps; i++) {
                double a0 = (i * 2.0 * Math.PI) / steps;
                double a1 = ((i + 1) * 2.0 * Math.PI) / steps;
                fillSolidTriangle(
                        centerX,
                        centerY,
                        centerX + (float) Math.cos(a0) * radius,
                        centerY + (float) Math.sin(a0) * radius,
                        centerX + (float) Math.cos(a1) * radius,
                        centerY + (float) Math.sin(a1) * radius,
                        mPaintColor);
            }
        }
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 1e-5f) {
            return;
        }
        float halfW = Math.max(0.5f, mStrokeWidth * 0.5f);
        float nx = (-dy / len) * halfW;
        float ny = (dx / len) * halfW;
        fillSolidTriangle(x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 + nx, y2 + ny, mPaintColor);
        fillSolidTriangle(x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny, mPaintColor);
    }

    @Override
    public void drawOval(float left, float top, float right, float bottom) {
    }

    @Override
    public void drawPath(int id, float start, float end) {
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom) {
        if (mPaintStyle == PaintBundle.STYLE_STROKE) {
            drawLine(left, top, right, top);
            drawLine(right, top, right, bottom);
            drawLine(right, bottom, left, bottom);
            drawLine(left, bottom, left, top);
        } else {
            fillSolidTriangle(left, top, right, top, right, bottom, mPaintColor);
            fillSolidTriangle(left, top, right, bottom, left, bottom, mPaintColor);
        }
    }

    @Override
    public void savePaint() {
        mPaintStack.push(
                new int[]{
                        mPaintColor,
                        mPaintStyle,
                        Float.floatToRawIntBits(mStrokeWidth),
                        Float.floatToRawIntBits(mTextSize),
                });
    }

    @Override
    public void restorePaint() {
        if (!mPaintStack.isEmpty()) {
            int[] state = mPaintStack.pop();
            mPaintColor = state[0];
            mPaintStyle = state[1];
            mStrokeWidth = Float.intBitsToFloat(state[2]);
            mTextSize = Float.intBitsToFloat(state[3]);
        }
    }

    @Override
    public void replacePaint(@NonNull PaintBundle paintBundle) {
        paintBundle.applyPaintChange(this, mPaintAdapter);
    }

    @Override
    public void drawRoundRect(
            float left, float top, float right, float bottom, float radiusX, float radiusY) {
        drawRect(left, top, right, bottom);
    }

    @Override
    public void drawTextOnPath(int textId, int pathId, float hOffset, float vOffset) {
    }

    @Override
    public void getTextBounds(int textId, int start, int end, int flags, float[] bounds) {
        String text = mContext.getText(textId);
        int len = text == null ? 0 : text.length();
        bounds[0] = 0f;
        bounds[1] = -mTextSize * 0.8f;
        bounds[2] = len * mTextSize * 0.56f;
        bounds[3] = mTextSize * 0.2f;
    }

    @Override
    public RcPlatformServices.@Nullable ComputedTextLayout layoutComplexText(
            int textId,
            int start,
            int end,
            int alignment,
            int overflow,
            int maxLines,
            float maxWidth,
            float maxHeight,
            float letterSpacing,
            float lineHeightAdd,
            float lineHeightMultiplier,
            int lineBreakStrategy,
            int hyphenationFrequency,
            int justificationMode,
            boolean useUnderline,
            boolean strikethrough,
            int flags) {
        return null;
    }

    @Override
    public void drawTextRun(
            int textID,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            boolean rtl) {
        String text = mContext.getText(textID);
        if (text == null || text.isEmpty()) {
            return;
        }
        int s = Math.max(0, start);
        int e = (end < 0 || end > text.length()) ? text.length() : end;
        if (s >= e) {
            return;
        }
        String sub = text.substring(s, e);
        BufferedImage img =
                new BufferedImage(mTargetWidth, mTargetHeight, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, mTargetWidth, mTargetHeight, mTargetPixels, 0, mTargetWidth);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setTransform(
                new AffineTransform(
                        mMatrix[0], mMatrix[1], mMatrix[2], mMatrix[3], mMatrix[4], mMatrix[5]));
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(1, Math.round(mTextSize))));
        g2d.setColor(new Color(mPaintColor, true));
        g2d.drawString(sub, x, y);
        g2d.dispose();
        img.getRGB(0, 0, mTargetWidth, mTargetHeight, mTargetPixels, 0, mTargetWidth);
    }

    @Override
    public void drawComplexText(
            RcPlatformServices.@Nullable ComputedTextLayout computedTextLayout) {
    }

    @Override
    public void drawTweenPath(int path1Id, int path2Id, float tween, float start, float stop) {
    }

    @Override
    public void tweenPath(int path1Id, int path2Id, int path3Id, float tween) {
    }

    @Override
    public void combinePath(int outPathId, int path1Id, int path2Id, byte operation) {
    }

    @Override
    public void applyPaint(@NonNull PaintBundle paintBundle) {
        paintBundle.applyPaintChange(this, mPaintAdapter);
    }

    @Override
    public void clipRect(float left, float top, float right, float bottom) {
    }

    @Override
    public void clipPath(int pathId, int regionOp) {
    }

    @Override
    public void roundedClipRect(
            float width,
            float height,
            float topStart,
            float topEnd,
            float bottomStart,
            float bottomEnd) {
    }

    @Override
    public void reset() {
    }

    @Override
    public void startGraphicsLayer(int id, int flag) {
    }

    @Override
    public void setGraphicsLayer(@NonNull HashMap<Integer, Object> map) {
    }

    @Override
    public void endGraphicsLayer() {
    }

    @Override
    public @Nullable String getText(int id) {
        return mContext.getText(id);
    }

    @Override
    public void matrixFromPath(int pathId, float progress, float distance, int flags) {
    }

    @Override
    public void drawToBitmap(int bitmapId, int mode, int color) {
        if (bitmapId == 0) {
            mTargetWidth = mWidth;
            mTargetHeight = mHeight;
            mTargetPixels = mPixels;
            return;
        }
        if (mContext instanceof HeadlessRemoteContext) {
            HeadlessRemoteContext.BitmapBuffer bmp =
                    ((HeadlessRemoteContext) mContext).getBitmap(bitmapId);
            if (bmp != null) {
                mTargetWidth = bmp.width;
                mTargetHeight = bmp.height;
                mTargetPixels = bmp.pixels;
                if ((mode & 1) == 0) {
                    java.util.Arrays.fill(mTargetPixels, color);
                }
            }
        }
    }
}
