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

import android.os.MessageQueue
import androidx.core.util.Consumer
import androidx.core.util.Supplier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DeferredObjectHolderTest {

    private lateinit var messageQueue: StubMessageQueue

    private lateinit var errorObject: StubObject
    private lateinit var errorHandler: StubErrorHandler

    @Before
    fun setUp() {
        messageQueue = StubMessageQueue()
        errorObject = StubObject().also(StubObject::initialize)
        errorHandler = StubErrorHandler()
    }

    @After
    fun tearDown() {
        errorHandler.assertNoError()
    }

    @Test
    fun demandObject_beforePreloadObject_createAndInitObject_success() {
        val stubObject = StubObject()
        val deferredObjectHolder = createDeferredObjectHolder(stubObject)

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)
        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)

        // No handler registered as object already created
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isFalse()
    }

    @Test
    fun demandObject_beforePreloadObject_createAndInitObject_createFail() {
        val exceptionOnCreate = RuntimeException("Error during creation")
        demandObject_beforePreloadObject_createAndInitObject_fail(
            createDeferredObjectHolder(exceptionOnCreate = exceptionOnCreate),
            exceptionOnCreate,
        )
    }

    @Test
    fun demandObject_beforePreloadObject_createAndInitObject_initFail() {
        val exceptionOnInit = RuntimeException("Error during initialization")
        demandObject_beforePreloadObject_createAndInitObject_fail(
            createDeferredObjectHolder(exceptionOnInit = exceptionOnInit),
            exceptionOnInit,
        )
    }

    private fun demandObject_beforePreloadObject_createAndInitObject_fail(
        deferredObjectHolder: DeferredObjectHolder<StubObject, StubObject>,
        expectedError: Throwable,
    ) {
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertErrorAndReset(expectedError)

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertNoError()

        // No handler registered as already failed
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isFalse()
    }

    @Test
    fun demandObject_beforeIdle_createAndInitObject_success() {
        val stubObject = StubObject()
        val deferredObjectHolder = createDeferredObjectHolder(stubObject)
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)

        // Handler unregistered during processIdle() as object already created and initialized
        messageQueue.processIdle()
        assertThat(messageQueue.handlerRegistered).isFalse()

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)
    }

    @Test
    fun demandObject_beforeIdle_createAndInitObject_createFail() {
        val exceptionOnCreate = RuntimeException("Error during creation")
        demandObject_beforeIdle_createAndInitObject_fail(
            createDeferredObjectHolder(exceptionOnCreate = exceptionOnCreate),
            exceptionOnCreate,
        )
    }

    @Test
    fun demandObject_beforeIdle_createAndInitObject_initFail() {
        val exceptionOnInit = RuntimeException("Error during initialization")
        demandObject_beforeIdle_createAndInitObject_fail(
            createDeferredObjectHolder(exceptionOnInit = exceptionOnInit),
            exceptionOnInit,
        )
    }

    private fun demandObject_beforeIdle_createAndInitObject_fail(
        deferredObjectHolder: DeferredObjectHolder<StubObject, StubObject>,
        expectedError: Throwable,
    ) {
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertErrorAndReset(expectedError)

        // Handler unregistered during processIdle() as already failed
        messageQueue.processIdle()
        assertThat(messageQueue.handlerRegistered).isFalse()

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertNoError()
    }

    @Test
    fun demandObject_afterIdleCreate_initObject_success() {
        val stubObject = StubObject()
        val deferredObjectHolder = createDeferredObjectHolder(stubObject)
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Created in first idle, but not initialized
        messageQueue.processIdle()
        assertThat(stubObject.isInitCalled()).isFalse()
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Initialize during demandObject()
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)
        assertThat(stubObject.isInitCalled()).isTrue()

        // Handler unregistered during processIdle() as object already created and initialized
        messageQueue.processIdle()
        assertThat(messageQueue.handlerRegistered).isFalse()

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)
    }

    @Test
    fun demandObject_afterIdleCreate_initObject_fail() {
        val exceptionOnInit = RuntimeException("Error during initialization")
        val deferredObjectHolder = createDeferredObjectHolder(exceptionOnInit = exceptionOnInit)
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Created in first idle, but not initialized
        messageQueue.processIdle()
        assertThat(messageQueue.handlerRegistered).isTrue()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertErrorAndReset(exceptionOnInit)

        // Handler unregistered during processIdle() as already failed
        messageQueue.processIdle()
        assertThat(messageQueue.handlerRegistered).isFalse()

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertNoError()
    }

    @Test
    fun demandObject_afterIdleInit_differentCreateAndInitIdles() {
        val stubObject = StubObject()
        val deferredObjectHolder = createDeferredObjectHolder(stubObject)
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Create in first idle, but not initialize
        messageQueue.processIdle(hasMessagesDue = true)
        assertThat(stubObject.isInitCalled()).isFalse()
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Init in idle when no messages due while processing
        messageQueue.processIdle(hasMessagesDue = false)
        assertThat(stubObject.isInitCalled()).isTrue()
        assertThat(messageQueue.handlerRegistered).isFalse()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)
    }

    @Test
    fun demandObject_afterIdleInit_singleCreateAndInitIdle() {
        val stubObject = StubObject()
        val deferredObjectHolder = createDeferredObjectHolder(stubObject)
        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Create and init in first idle (no messages due while processing)
        messageQueue.processIdle(hasMessagesDue = false)
        assertThat(messageQueue.handlerRegistered).isFalse()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(stubObject)
    }

    @Test
    fun demandObject_afterIdleCreateFail_differentCreateAndInitIdles() {
        demandObject_afterIdleCreateFail(hasMessagesDueWhileProcessIdle = true)
    }

    @Test
    fun demandObject_afterIdleCreateFail_singleCreateAndInitIdle() {
        demandObject_afterIdleCreateFail(hasMessagesDueWhileProcessIdle = false)
    }

    private fun demandObject_afterIdleCreateFail(hasMessagesDueWhileProcessIdle: Boolean) {
        val exceptionOnCreate = RuntimeException("Error during creation")
        val deferredObjectHolder = createDeferredObjectHolder(exceptionOnCreate = exceptionOnCreate)

        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Fail object creation in first idle
        messageQueue.processIdle(hasMessagesDue = hasMessagesDueWhileProcessIdle)
        errorHandler.assertErrorAndReset(exceptionOnCreate)
        assertThat(messageQueue.handlerRegistered).isFalse()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertNoError()
    }

    @Test
    fun demandObject_afterIdleInitFail_differentCreateAndInitIdles() {
        val exceptionOnInit = RuntimeException("Error during initialization")
        val deferredObjectHolder = createDeferredObjectHolder(exceptionOnInit = exceptionOnInit)

        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Create in first idle
        messageQueue.processIdle(hasMessagesDue = true)
        errorHandler.assertNoError()
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Init fail in idle when no messages due while processing
        messageQueue.processIdle(hasMessagesDue = false)
        errorHandler.assertErrorAndReset(exceptionOnInit)
        assertThat(messageQueue.handlerRegistered).isFalse()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertNoError()
    }

    @Test
    fun demandObject_afterIdleInitFail_singleCreateAndInitIdle() {
        val exceptionOnInit = RuntimeException("Error during initialization")
        val deferredObjectHolder = createDeferredObjectHolder(exceptionOnInit = exceptionOnInit)

        deferredObjectHolder.preloadObject(messageQueue)
        assertThat(messageQueue.handlerRegistered).isTrue()

        // Successfully create object, but fail init in first idle
        messageQueue.processIdle(hasMessagesDue = false)
        errorHandler.assertErrorAndReset(exceptionOnInit)
        assertThat(messageQueue.handlerRegistered).isFalse()

        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)

        // Check that creation / initialization logic triggered only once
        deferredObjectHolder.demandObjectAndAssertThatSameInstanceAs(errorObject)
        errorHandler.assertNoError()
    }

    private fun createDeferredObjectHolder(
        stubObject: StubObject
    ): DeferredObjectHolder<StubObject, StubObject> =
        createDeferredObjectHolder(StubObjectFactory(stubObject = stubObject))

    private fun createDeferredObjectHolder(
        exceptionOnCreate: Throwable? = null,
        exceptionOnInit: Throwable? = null,
    ): DeferredObjectHolder<StubObject, StubObject> =
        createDeferredObjectHolder(
            StubObjectFactory(
                exceptionOnCreate = exceptionOnCreate,
                stubObject = StubObject(exceptionOnInit = exceptionOnInit),
            )
        )

    private fun createDeferredObjectHolder(
        objectFactory: Supplier<StubObject>
    ): DeferredObjectHolder<StubObject, StubObject> {
        return DeferredObjectHolder(
            objectFactory = objectFactory,
            objectInit = StubObject::initialize,
            errorHandler = errorHandler,
            errorObject = errorObject,
        )
    }

    private class StubObjectFactory(
        private val exceptionOnCreate: Throwable? = null,
        private val stubObject: StubObject = StubObject(),
    ) : Supplier<StubObject> {

        val createMethodCallCounter = AtomicInteger(0)

        override fun get(): StubObject {
            // Check that called only once.
            assertThat(createMethodCallCounter.incrementAndGet()).isEqualTo(1)
            if (exceptionOnCreate != null) {
                throw exceptionOnCreate
            }
            return stubObject
        }
    }

    private class StubObject(private val exceptionOnInit: Throwable? = null) {
        private val initMethodCallCounter = AtomicInteger(0)

        fun initialize() {
            // Check that called only once.
            assertThat(initMethodCallCounter.incrementAndGet()).isEqualTo(1)
            if (exceptionOnInit != null) {
                throw exceptionOnInit
            }
        }

        fun isInitCalled(): Boolean {
            return initMethodCallCounter.get() != 0
        }
    }

    private class StubErrorHandler : Consumer<Throwable> {
        private var error: Throwable? = null

        fun assertErrorAndReset(expected: Throwable) {
            assertThat(error).isEqualTo(expected)
            error = null
        }

        fun assertNoError() {
            assertWithMessage("Unexpected error").that(error).isNull()
        }

        override fun accept(value: Throwable) {
            // Check that error set only once before assertErrorAndReset() call.
            assertThat(this.error).isNull()
            this.error = value
        }
    }

    private class StubMessageQueue : DeferredObjectHolder.MessageQueue {

        val handlerRegistered: Boolean
            get() = handlers.isNotEmpty()

        private var idle: Boolean = false
        private var handlers: MutableList<MessageQueue.IdleHandler> = mutableListOf()

        /**
         * Emulate Idle state - run registered IdleHandlers.
         *
         * If handler calls [isIdle] during processing it will receive ![hasMessagesDue] as result.
         *
         * In real world [hasMessagesDue] will be "true" more often that "false":
         * 1) It will be used during intensive loading process - high chance of new messages arrive.
         * 2) Logic inside IdleHandler takes time - increasing chance of new messages arrive.
         * 3) Other IdleHandlers takes time / etc.
         */
        fun processIdle(hasMessagesDue: Boolean = true) {
            idle = !hasMessagesDue
            val it = handlers.iterator()
            while (it.hasNext()) {
                val handler = it.next()
                val keep = handler.queueIdle()
                if (!keep) {
                    it.remove()
                }
            }
            idle = false
        }

        override fun addIdleHandler(handler: MessageQueue.IdleHandler) {
            handlers.add(handler)
        }

        override fun isIdle(): Boolean {
            return idle
        }
    }

    private fun DeferredObjectHolder<StubObject, StubObject>
        .demandObjectAndAssertThatSameInstanceAs(expected: StubObject) {
        val result = demandObject()
        assertThat(result.isInitCalled()).isTrue()
        assertThat(result).isSameInstanceAs(expected)
    }
}
