/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.EGL14
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.EGLVersion
import org.junit.Assert

fun drawSquares(
    canvas: Canvas,
    width: Int,
    height: Int,
    topLeft: Int,
    topRight: Int,
    bottomLeft: Int,
    bottomRight: Int
) {
    val paint = Paint()
    val widthF = width.toFloat()
    val heightF = height.toFloat()
    val halfWidth = widthF / 2f
    val halfHeight = heightF / 2f
    canvas.drawRect(0f, 0f, halfWidth, halfHeight,
        paint.apply { color = topLeft })
    canvas.drawRect(halfWidth, 0f, widthF, halfHeight,
        paint.apply { color = topRight })
    canvas.drawRect(0f, halfHeight, halfWidth, heightF,
        paint.apply { color = bottomLeft })
    canvas.drawRect(halfWidth, halfHeight, widthF, heightF,
        paint.apply { color = bottomRight })
}

fun Bitmap.verifyQuadrants(
    topLeft: Int,
    topRight: Int,
    bottomLeft: Int,
    bottomRight: Int
) {
    Assert.assertEquals(topLeft, getPixel(1, 1))
    Assert.assertEquals(topLeft, getPixel(width / 2 - 2, 1))
    Assert.assertEquals(topLeft, getPixel(width / 2 - 2, height / 2 - 2))
    Assert.assertEquals(topLeft, getPixel(1, height / 2 - 2))

    Assert.assertEquals(topRight, getPixel(width / 2 + 2, 1))
    Assert.assertEquals(topRight, getPixel(width - 2, 1))
    Assert.assertEquals(topRight, getPixel(width - 2, height / 2 - 2))
    Assert.assertEquals(topRight, getPixel(width / 2 + 2, height / 2 - 2))

    Assert.assertEquals(bottomLeft, getPixel(1, height / 2 + 2))
    Assert.assertEquals(bottomLeft, getPixel(width / 2 - 2, height / 2 + 2))
    Assert.assertEquals(bottomLeft, getPixel(width / 2 - 2, height - 2))
    Assert.assertEquals(bottomLeft, getPixel(1, height - 2))

    Assert.assertEquals(bottomRight, getPixel(width / 2 + 2, height / 2 + 2))
    Assert.assertEquals(bottomRight, getPixel(width - 2, height / 2 + 2))
    Assert.assertEquals(bottomRight, getPixel(width - 2, height - 2))
    Assert.assertEquals(bottomRight, getPixel(width / 2 + 2, height - 2))
}

fun Bitmap.isAllColor(targetColor: Int): Boolean {
    for (i in 0 until width) {
        for (j in 0 until height) {
            if (getPixel(i, j) != targetColor) {
                return false
            }
        }
    }
    return true
}

fun withEgl(block: (egl: EGLManager) -> Unit) {
    val egl = createAndSetupEGLManager(EGLSpec.V14)
    try {
        block(egl)
    } finally {
        releaseEGLManager(egl)
    }
}

// Helper method to create and initialize an EGLManager
private fun createAndSetupEGLManager(eglSpec: EGLSpec = EGLSpec.V14): EGLManager {
    val egl = EGLManager(eglSpec)
    Assert.assertEquals(EGLVersion.Unknown, egl.eglVersion)
    Assert.assertEquals(EGL14.EGL_NO_CONTEXT, egl.eglContext)

    egl.initialize()

    val config = egl.loadConfig(EGLConfigAttributes.RGBA_8888)
    if (config == null) {
        Assert.fail("Config 888 should be supported")
    }

    egl.createContext(config!!)
    return egl
}

// Helper method to release EGLManager
private fun releaseEGLManager(egl: EGLManager) {
    egl.release()
    Assert.assertEquals(EGLVersion.Unknown, egl.eglVersion)
    Assert.assertEquals(EGL14.EGL_NO_CONTEXT, egl.eglContext)
}
