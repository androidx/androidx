/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.camera.media3.effect

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.Matrix
import androidx.annotation.MainThread
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.Threads.checkMainThread
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.core.util.Consumer
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.DebugViewProvider
import androidx.media3.common.Effect
import androidx.media3.common.FrameInfo
import androidx.media3.common.SurfaceInfo
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.VideoFrameProcessor
import androidx.media3.common.util.Assertions.checkState
import androidx.media3.common.util.Log
import androidx.media3.common.util.Size
import androidx.media3.effect.DefaultVideoFrameProcessor
import java.util.concurrent.Executor

/**
 * A [SurfaceProcessor] that wraps around media3's [DefaultVideoFrameProcessor].
 *
 * CameraX’s CameraEffect API handles real-time changes of the input and output buffer, such as
 * resolution, transformation, image format, FPS and/or dynamic range. On the other hand, media3’s
 * DefaultVideoFrameProcessor API must configure the processor prior to its creation and stay
 * unchanged during its lifetime. The adapter layer bridges this gap by listening to the CameraX
 * callbacks [SurfaceProcessor.onOutputSurface] and [SurfaceProcessor.onInputSurface] for
 * configuration changes, and destroys/creates [DefaultVideoFrameProcessor] as needed.
 *
 *            Waiting for CameraX callbacks
 *    ^            |                         ^    ^
 *    |            V        yes              |    |
 *    |        Connected?   -->  Disconnect --    |
 *    |            |  no                          |
 *    |            V              yes             |
 *    |        Ready to connect?  -->  Connect ----
 *    |            |  no
 *    --------------
 */
@SuppressLint("UnsafeOptInUsageError")
internal class Media3SurfaceProcessor(
    private val context: Context,
    private val listenerExecutor: Executor,
    private val errorListener: Consumer<Throwable>,
) : SurfaceProcessor {
    private var effects = emptyList<Effect>()

    // The input and output that are pending to be connected.
    private var pendingInput: SurfaceRequest? = null
    private var pendingOutput: SurfaceOutput? = null

    // The input, output and the processor that are currently connected.
    private var connectedInput: SurfaceRequest? = null
    private var connectedOutput: SurfaceOutput? = null
    private var connectedProcessor: DefaultVideoFrameProcessor? = null

    // The active processors. A processor remains active until the input/output are closed.
    private val activeProcessors: MutableSet<DefaultVideoFrameProcessor> = HashSet()
    private var isReleased = false

    @MainThread
    override fun onInputSurface(request: SurfaceRequest) {
        checkMainThread()
        Log.d(TAG, "onInputSurface $request")
        if (isReleased) {
            Log.w(TAG, "onInputSurface() called after release.")
            return
        }
        pendingInput?.willNotProvideSurface()
        pendingInput = request
        disconnectProcessor(connectedProcessor)
        tryConnectProcessor()
    }

    @MainThread
    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        checkMainThread()
        Log.d(TAG, "onOutputSurface $surfaceOutput")
        if (isReleased) {
            Log.w(TAG, "onOutputSurface() called after release.")
            return
        }
        connectedInput?.invalidate()
        pendingOutput = surfaceOutput
        disconnectProcessor(connectedProcessor)
        tryConnectProcessor()
    }

    /** Internal API called by [Media3Effect] to update the current set of effects. */
    @MainThread
    fun setEffects(effects: List<Effect>) {
        checkMainThread()
        this.effects = effects.toList()
        if (connectedInput != null || connectedOutput != null || connectedProcessor != null) {
            configureProcessor(connectedInput!!, connectedOutput!!, connectedProcessor!!)
        }
    }

    /** Internal API called by [Media3Effect] to release the processor. */
    @MainThread
    fun release() {
        Log.d(TAG, "Release")
        checkMainThread()
        checkState(!isReleased, "The Media3Effect has already been released.")
        isReleased = true
        pendingInput?.willNotProvideSurface()
        disconnectProcessor(connectedProcessor)
    }

    @MainThread
    private fun disconnectProcessor(processor: DefaultVideoFrameProcessor?) {
        checkMainThread()
        Log.d(TAG, "disconnectProcessor: $processor")
        if (processor != null && activeProcessors.contains(processor)) {
            activeProcessors.remove(processor)
            processor.release()
        }
    }

    @MainThread
    private fun tryConnectProcessor() {
        checkMainThread()
        Log.d(TAG, "tryConnectPendingProcessor")
        val input = pendingInput
        // SurfaceOutput can be recycled. It's OK to used a connected output.
        val output = pendingOutput ?: connectedOutput
        if (input != null && output != null) {
            connectInputAndOutput(input, output)
        }
    }

    /** Creates a media3 [ColorInfo] based on the CameraX [DynamicRange]. */
    @MainThread
    private fun createColorInfo(dynamicRange: DynamicRange): ColorInfo {
        if (dynamicRange.is10BitHdr) {
            val builder =
                ColorInfo.Builder()
                    .setColorRange(C.COLOR_RANGE_LIMITED)
                    .setColorSpace(C.COLOR_SPACE_BT2020)
            if (dynamicRange.encoding == DynamicRange.ENCODING_HLG) {
                builder.setColorTransfer(C.COLOR_TRANSFER_HLG)
            } else {
                builder.setColorTransfer(C.COLOR_TRANSFER_ST2084)
            }
            return builder.build()
        } else {
            return ColorInfo.Builder()
                .setColorSpace(C.COLOR_SPACE_BT601)
                .setColorRange(C.COLOR_RANGE_FULL)
                .setColorTransfer(C.COLOR_TRANSFER_SDR)
                .build()
        }
    }

    /**
     * Configures the [processor] based on CameraX's input config, output config and the current set
     * of media3 effects.
     */
    @MainThread
    private fun configureProcessor(
        input: SurfaceRequest,
        output: SurfaceOutput,
        processor: DefaultVideoFrameProcessor
    ) {
        // Gets user configured transformation from CameraX Effects API, and build a media3 effect
        // that applies that transformation.
        val identityMatrix = FloatArray(16)
        Matrix.setIdentityM(identityMatrix, 0)
        val cameraXTransform = FloatArray(16)
        output.updateTransformMatrix(cameraXTransform, identityMatrix)
        val cameraXTransformEffect =
            CameraXGlTransformation(cameraXTransform, Size(output.size.width, output.size.height))
        // Configure the processor's input format
        val frameInfo =
            FrameInfo.Builder(
                    createColorInfo(input.dynamicRange),
                    input.resolution.width,
                    input.resolution.height
                )
                .build()
        processor.registerInputStream(
            VideoFrameProcessor.INPUT_TYPE_SURFACE_AUTOMATIC_FRAME_REGISTRATION,
            listOf(cameraXTransformEffect, *effects.toTypedArray()),
            frameInfo
        )
    }

    @MainThread
    private fun connectInputAndOutput(input: SurfaceRequest, output: SurfaceOutput) {
        checkMainThread()
        Log.d(TAG, "connectInputAndOutput input: $input output: $output")
        val wrappingListener =
            object : VideoFrameProcessor.Listener {
                override fun onInputStreamRegistered(
                    inputType: Int,
                    effects: MutableList<Effect>,
                    frameInfo: FrameInfo
                ) {}

                override fun onOutputSizeChanged(width: Int, height: Int) {}

                override fun onOutputFrameAvailableForRendering(presentationTimeUs: Long) {}

                override fun onError(exception: VideoFrameProcessingException) {
                    listenerExecutor.execute { errorListener.accept(exception) }
                }

                override fun onEnded() {}
            }
        val newProcessor =
            DefaultVideoFrameProcessor.Factory.Builder()
                .build()
                .create(
                    context,
                    DebugViewProvider.NONE,
                    createColorInfo(input.dynamicRange),
                    /*renderFramesAutomatically=*/ true,
                    directExecutor(),
                    wrappingListener
                )
        Log.d(TAG, "Created processor $newProcessor")
        configureProcessor(input, output, newProcessor)
        activeProcessors.add(newProcessor)
        // Prove input service when ready
        newProcessor.setOnInputSurfaceReadyListener {
            input.provideSurface(newProcessor.inputSurface, mainThreadExecutor()) {
                Log.d(TAG, "Input surface life ends $input")
                disconnectProcessor(newProcessor)
                if (connectedInput == input) {
                    connectedInput = null
                }
            }
        }
        connectedInput = input
        // Bind the output surface to frame processor
        val outputSurface =
            output.getSurface(mainThreadExecutor()) {
                Log.d(TAG, "Output surface life ends $output")
                disconnectProcessor(newProcessor)
                output.close()
                if (connectedOutput == output) {
                    connectedOutput = null
                }
            }
        newProcessor.setOutputSurfaceInfo(
            SurfaceInfo(
                outputSurface,
                output.size.width,
                output.size.height,
                /* orientationDegrees= */ 0
            )
        )
        connectedOutput = output
        // Pending values have been used
        pendingInput = null
        pendingOutput = null
        connectedProcessor = newProcessor
    }
}
