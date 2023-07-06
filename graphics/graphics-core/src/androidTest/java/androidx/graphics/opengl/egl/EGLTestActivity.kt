/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.graphics.opengl.egl

import android.animation.ValueAnimator
import android.app.Activity
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.widget.LinearLayout
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.GLRenderer.RenderTarget
import java.util.concurrent.atomic.AtomicInteger

const val TAG: String = "EGLTestActivity"

@Suppress("AcronymName")
class EGLTestActivity : Activity() {

    private val mGLRenderer = GLRenderer()
    private val mParam = AtomicInteger()
    private val mRenderer1 = object : GLRenderer.RenderCallback {
        override fun onSurfaceCreated(
            spec: EGLSpec,
            config: EGLConfig,
            surface: Surface,
            width: Int,
            height: Int
        ): EGLSurface {
            val attrs = EGLConfigAttributes {
                EGL14.EGL_RENDER_BUFFER to EGL14.EGL_SINGLE_BUFFER
            }
            return spec.eglCreateWindowSurface(config, surface, attrs)
        }

        override fun onDrawFrame(eglManager: EGLManager) {
            val red = mParam.toFloat() / 100f
            GLES20.glClearColor(red, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }
    }

    private val mRenderer2 = object : GLRenderer.RenderCallback {
        override fun onDrawFrame(eglManager: EGLManager) {
            val blue = mParam.toFloat() / 100f
            GLES20.glClearColor(0.0f, 0.0f, blue, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }
    }

    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mTextureView: TextureView
    private lateinit var mRenderTarget1: RenderTarget
    private lateinit var mRenderTarget2: RenderTarget

    private var mAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGLRenderer.start()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            weightSum = 2f
        }
        mSurfaceView = SurfaceView(this)
        mTextureView = TextureView(this)

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0).apply {
            weight = 1f
        }

        mRenderTarget1 = mGLRenderer.attach(mSurfaceView, mRenderer1)
        mRenderTarget2 = mGLRenderer.attach(mTextureView, mRenderer2)

        container.addView(mSurfaceView, params)
        container.addView(mTextureView, params)

        setContentView(container)

        mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                mParam.set(((it.animatedValue as Float) * 100).toInt())
                mRenderTarget1.requestRender()
                mRenderTarget2.requestRender()
            }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mRenderTarget1.isAttached()) {
            mRenderTarget1 = mGLRenderer.attach(mSurfaceView, mRenderer1)
        }

        if (!mRenderTarget2.isAttached()) {
            mRenderTarget2 = mGLRenderer.attach(mTextureView, mRenderer2)
        }
    }

    override fun onPause() {
        super.onPause()
        mRenderTarget1.detach(true)
        mRenderTarget2.detach(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        mAnimator?.cancel()
        mGLRenderer.stop(true)
    }
}
