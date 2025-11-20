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

package androidx.privacysandbox.ui.provider.impl

import android.os.Looper
import android.os.MessageQueue.IdleHandler
import androidx.core.util.Consumer
import androidx.core.util.Supplier
import org.jetbrains.annotations.TestOnly

/**
 * Tries to postpone object creation/initialization until Idle state.
 *
 * Uses [objectFactory] to create object and [objectInit] to initialize it later. In case of
 * creation/initialization failure reports error to [errorHandler] and uses [errorObject] as return
 * value for [demandObject]
 *
 * @param objectFactory Creates uninitialized object. Called only once.
 * @param objectInit Initialize object created by [objectFactory]. Called only once.
 * @param errorHandler Handler for error during creation/initialization. Called only once.
 * @param errorObject Return value for [demandObject] in case of creation/initialization errors.
 */
internal class DeferredObjectHolder<BaseClass : Any, ImplementationClass : BaseClass>(
    private val objectFactory: Supplier<ImplementationClass>,
    private val objectInit: Consumer<ImplementationClass>,
    private val errorHandler: Consumer<Throwable>,
    private val errorObject: BaseClass,
) : ObjectHolder<BaseClass> {

    private lateinit var impl: ImplementationClass
    private var state: ObjectState = ObjectState.NONE

    override fun demandObject(): BaseClass {
        createObjectIfNeeded()
        initializeObjectIfNeeded()
        return if (state == ObjectState.INITIALIZED) {
            impl
        } else {
            errorObject
        }
    }

    /**
     * Schedule object preloading in upcoming Idles.
     *
     * Creates object in a first Idle and tries to initialize it in one of subsequent Idles
     * (depending on how busy/utilized main thread).
     *
     * This combination provides good balance between Idle time utilization and interference with
     * other tasks scheduled for main thread.
     *
     * Although checking (or not) Idle each time could give better utilization for some use cases,
     * using combination of 2 different approaches should give better average results for more
     * scenarios by covering weak points of single approaches.
     */
    override fun preloadObject() {
        preloadObject(AndroidMessageQueue)
    }

    @TestOnly
    fun preloadObject(messageQueue: MessageQueue) {
        if (state.isFinalState()) {
            // Already initialized or failed - no further steps required.
            return
        }
        messageQueue.addIdleHandler {
            createObjectIfNeeded()

            if (messageQueue.isIdle()) {
                initializeObjectIfNeeded()
            }

            return@addIdleHandler !state.isFinalState()
        }
    }

    private fun createObjectIfNeeded() = tryWithErrorHandling {
        if (state == ObjectState.NONE) {
            impl = objectFactory.get()
            state = ObjectState.CREATED
        }
    }

    private fun initializeObjectIfNeeded() = tryWithErrorHandling {
        if (state == ObjectState.CREATED) {
            objectInit.accept(impl)
            state = ObjectState.INITIALIZED
        }
    }

    private inline fun tryWithErrorHandling(function: () -> Unit) {
        try {
            function()
        } catch (exception: Throwable) {
            state = ObjectState.FAILED
            errorHandler.accept(exception)
        }
    }

    private enum class ObjectState {
        NONE,
        CREATED,
        INITIALIZED,
        FAILED;

        fun isFinalState(): Boolean {
            return this == INITIALIZED || this == FAILED
        }
    }

    interface MessageQueue {
        fun addIdleHandler(handler: IdleHandler)

        fun isIdle(): Boolean
    }

    private object AndroidMessageQueue : MessageQueue {
        override fun addIdleHandler(handler: IdleHandler) = Looper.myQueue().addIdleHandler(handler)

        override fun isIdle() = Looper.myQueue().isIdle
    }
}
