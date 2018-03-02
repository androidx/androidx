/*
 * Copyright 2018 Google Inc. All rights reserved.
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

package androidx.heifwriter;

import android.graphics.Rect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * This class represents a viewport-sized sprite that will be rendered with
 * a subrect from a texture.
 *
 * @hide
 */
public class EglRectBlt {
    private static final int SIZEOF_FLOAT = 4;

    /**
     * A "full" square, extending from -1 to +1 in both dimensions. When the
     * model/view/projection matrix is identity, this will exactly cover the viewport.
     */
    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
             1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
             1.0f,  1.0f,   // 3 top right
    };

    private static final FloatBuffer FULL_RECTANGLE_BUF =
            createFloatBuffer(FULL_RECTANGLE_COORDS);

    private final float mTexCoords[] = new float[8];
    private final FloatBuffer mTexCoordArray = createFloatBuffer(mTexCoords);
    private final int mTexWidth;
    private final int mTexHeight;

    private Texture2dProgram mProgram;

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Prepares the object.
     *
     * @param program The program to use. EglRectBlitter takes ownership, and will release
     *     the program when no longer needed.
     */
    public EglRectBlt(Texture2dProgram program, int texWidth, int texHeight) {
        mProgram = program;

        mTexWidth = texWidth;
        mTexHeight = texHeight;
    }

    /**
     * Releases resources.
     * <p>
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called). If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    public void release(boolean doEglCleanup) {
        if (mProgram != null) {
            if (doEglCleanup) {
                mProgram.release();
            }
            mProgram = null;
        }
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object and rect.
     */
    public void copyRect(int textureId, float[] texMatrix, Rect texRect) {
        setTexRect(texRect);

        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mProgram.draw(Texture2dProgram.IDENTITY_MATRIX, FULL_RECTANGLE_BUF, 0,
                4, 2, 2 * SIZEOF_FLOAT,
                texMatrix, mTexCoordArray, textureId, 2 * SIZEOF_FLOAT);
    }

    void setTexRect(Rect rect) {
        mTexCoords[0] = rect.left / (float)mTexWidth;
        mTexCoords[1] = 1.0f - rect.bottom / (float)mTexHeight;
        mTexCoords[2] = rect.right / (float)mTexWidth;
        mTexCoords[3] = 1.0f - rect.bottom / (float)mTexHeight;
        mTexCoords[4] = rect.left / (float)mTexWidth;
        mTexCoords[5] = 1.0f - rect.top / (float)mTexHeight;
        mTexCoords[6] = rect.right / (float)mTexWidth;
        mTexCoords[7] = 1.0f - rect.top / (float)mTexHeight;

        mTexCoordArray.put(mTexCoords);
        mTexCoordArray.position(0);
    }
}
