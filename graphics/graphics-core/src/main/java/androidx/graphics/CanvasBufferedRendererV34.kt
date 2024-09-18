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

import android.annotation.SuppressLint
import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.HardwareBufferRenderer
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.graphics.surface.JniBindings
import androidx.hardware.BufferPool
import androidx.hardware.FileDescriptorMonitor
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class CanvasBufferedRendererV34(
    private val mWidth: Int,
    private val mHeight: Int,
    private val mFormat: Int,
    private val mUsage: Long,
    maxBuffers: Int,
    private val mFdMonitor: SharedFileDescriptorMonitor? = obtainSharedFdMonitor()
) : CanvasBufferedRenderer.Impl {

    private data class HardwareBufferProvider(
        private val buffer: HardwareBuffer,
        val renderer: HardwareBufferRenderer
    ) : BufferPool.BufferProvider {
        override val hardwareBuffer: HardwareBuffer
            get() = buffer

        override fun release() {
            renderer.close()
            buffer.close()
        }
    }

    init {
        mFdMonitor?.incrementRef()
    }

    private val mPool = BufferPool<HardwareBufferProvider>(maxBuffers)

    private val mRootNode =
        RenderNode("rootNode").apply {
            setPosition(0, 0, mWidth, mHeight)
            clipToBounds = false
        }

    private var mContentNode: RenderNode? = null
    private var mLightX: Float = 0f
    private var mLightY: Float = 0f
    private var mLightZ: Float = 0f
    private var mLightRadius: Float = 0f
    private var mAmbientShadowAlpha: Float = 0f
    private var mSpotShadowAlpha: Float = 0f
    private var mPreserveContents = false
    private var mCurrentBufferProvider: HardwareBufferProvider? = null

    private fun obtainBufferEntry(): HardwareBufferProvider =
        mPool.obtain {
            val hardwareBuffer = HardwareBuffer.create(mWidth, mHeight, mFormat, 1, mUsage)
            HardwareBufferProvider(hardwareBuffer, HardwareBufferRenderer(hardwareBuffer))
        }

    override fun close() {
        mPool.close()
        mFdMonitor?.decrementRef()
        mCurrentBufferProvider?.renderer?.close()
    }

    override fun isClosed(): Boolean = mPool.isClosed

    @SuppressLint("WrongConstant")
    override fun draw(
        request: CanvasBufferedRenderer.RenderRequest,
        executor: Executor,
        callback: Consumer<CanvasBufferedRenderer.RenderResult>
    ) {
        val contentNode = mContentNode
        val shouldDraw =
            !mRootNode.hasDisplayList() || mPreserveContents != request.preserveContents
        if (shouldDraw && contentNode != null) {
            val canvas = mRootNode.beginRecording()
            canvas.save()
            if (!request.preserveContents) {
                canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
            }
            canvas.drawRenderNode(contentNode)
            canvas.restore()
            mRootNode.endRecording()
            mPreserveContents = request.preserveContents
        }
        val renderNode = mRootNode
        val lightX = mLightX
        val lightY = mLightY
        val lightZ = mLightZ
        val lightRadius = mLightRadius
        val ambientShadowAlpha = mAmbientShadowAlpha
        val spotShadowAlpha = mSpotShadowAlpha
        val colorSpace = request.colorSpace
        val transform = request.transform
        executor.execute {
            if (!isClosed()) {
                val bufferProvider = obtainBufferEntry()
                mCurrentBufferProvider = bufferProvider
                with(bufferProvider) {
                    renderer.apply {
                        setLightSourceAlpha(ambientShadowAlpha, spotShadowAlpha)
                        setLightSourceGeometry(lightX, lightY, lightZ, lightRadius)
                        setContentRoot(renderNode)
                        obtainRenderRequest()
                            .apply {
                                setColorSpace(colorSpace)
                                setBufferTransform(transform)
                            }
                            .draw(executor) { result ->
                                callback.accept(
                                    CanvasBufferedRenderer.RenderResult(
                                        hardwareBuffer,
                                        SyncFenceCompat(result.fence),
                                        result.status
                                    )
                                )
                            }
                    }
                }
            }
        }
    }

    override fun releaseBuffer(hardwareBuffer: HardwareBuffer, syncFence: SyncFenceCompat?) {
        mPool.release(hardwareBuffer, syncFence)
    }

    override fun setContentRoot(renderNode: RenderNode) {
        mContentNode = renderNode
        mRootNode.discardDisplayList()
    }

    override fun setLightSourceAlpha(ambientShadowAlpha: Float, spotShadowAlpha: Float) {
        mAmbientShadowAlpha = ambientShadowAlpha
        mSpotShadowAlpha = spotShadowAlpha
    }

    override fun setLightSourceGeometry(
        lightX: Float,
        lightY: Float,
        lightZ: Float,
        lightRadius: Float
    ) {
        mLightX = lightX
        mLightY = lightY
        mLightZ = lightZ
        mLightRadius = lightRadius
    }

    internal companion object {
        private val monitorLock = ReentrantLock()
        private var sharedFdMonitor: SharedFileDescriptorMonitor? = null

        fun obtainSharedFdMonitor(): SharedFileDescriptorMonitor? {
            val isVulkan = JniBindings.nIsHwuiUsingVulkanRenderer()
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE && isVulkan) {
                // See b/295332012
                monitorLock.withLock {
                    var monitor = sharedFdMonitor
                    if (monitor == null || !monitor.isMonitoring) {
                        monitor =
                            SharedFileDescriptorMonitor(
                                FileDescriptorMonitor().apply { startMonitoring() }
                            )
                        sharedFdMonitor = monitor
                    }
                    return monitor
                }
            } else {
                return null
            }
        }
    }
}

internal class SharedFileDescriptorMonitor(
    private val fileDescriptorMonitor: FileDescriptorMonitor
) {

    private val mRefCount = AtomicInteger(0)

    fun incrementRef() {
        mRefCount.incrementAndGet()
    }

    val isMonitoring: Boolean
        get() = fileDescriptorMonitor.isMonitoring

    fun decrementRef() {
        if (mRefCount.decrementAndGet() <= 0) {
            fileDescriptorMonitor.stopMonitoring()
        }
    }
}
