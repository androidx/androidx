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

package androidx.camera.testing.impl

import androidx.camera.core.imagecapture.CameraCapturePipeline
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CopyOnWriteArrayList

/** A fake implementation of [CameraCapturePipeline] for testing purposes. */
public class FakeCameraCapturePipeline : CameraCapturePipeline {
    /**
     * Defines the type of invocation.
     *
     * @see InvocationListener
     */
    public enum class InvocationType {
        /** Invoked via [invokePreCapture] function. */
        PRE_CAPTURE,
        /** Invoked via [invokePostCapture] function. */
        POST_CAPTURE,
    }

    /** Listener for any of the functions corresponding to [InvocationType.entries] are called */
    public interface InvocationListener {
        /**
         * Invoked when any of the functions corresponding to [InvocationType.entries] are called.
         */
        public fun onInvoked(invocationType: InvocationType)
    }

    private val invocationListeners = CopyOnWriteArrayList<InvocationListener>()

    /** Adds an [InvocationListener]. */
    public fun addInvocationListener(listener: InvocationListener) {
        invocationListeners.add(listener)
    }

    /** Removes an [InvocationListener]. */
    public fun removeInvocationListener(listener: InvocationListener) {
        invocationListeners.remove(listener)
    }

    private fun notifyInvocationListeners(invocationType: InvocationType) {
        invocationListeners.forEach { it.onInvoked(invocationType) }
    }

    override fun invokePreCapture(): ListenableFuture<Void?> {
        notifyInvocationListeners(InvocationType.PRE_CAPTURE)
        return Futures.immediateFuture(null)
    }

    override fun invokePostCapture(): ListenableFuture<Void?> {
        notifyInvocationListeners(InvocationType.POST_CAPTURE)
        return Futures.immediateFuture(null)
    }
}
