/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.wear.watchface.complications.datasource

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.RemoteException
import android.support.wearable.complications.ComplicationData as WireComplicationData
import android.support.wearable.complications.IComplicationManager
import android.support.wearable.complications.IComplicationProvider
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.DynamicComplicationText
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks

/** Tests for [ComplicationDataSourceService]. */
@RunWith(ComplicationsTestRunner::class)
@DoNotInstrument
class ComplicationDataSourceServiceTest {
    @get:Rule val expect = Expect.create()

    private var mPretendMainThread = HandlerThread("testThread")
    private lateinit var mPretendMainThreadHandler: Handler

    private val mRemoteManager = mock<IComplicationManager>()
    private val mUpdateComplicationDataLatch = CountDownLatch(1)
    private val mLocalManager: IComplicationManager.Stub =
        object : IComplicationManager.Stub() {
            override fun updateComplicationData(
                complicationSlotId: Int,
                data: WireComplicationData?
            ) {
                try {
                    mRemoteManager.updateComplicationData(complicationSlotId, data)
                } finally {
                    mUpdateComplicationDataLatch.countDown()
                }
            }
        }
    private lateinit var mProvider: IComplicationProvider.Stub

    /**
     * Mock implementation of ComplicationDataSourceService.
     *
     * Can't use Mockito because it doesn't like partially implemented classes.
     */
    private inner class MockComplicationDataSourceService : ComplicationDataSourceService() {
        var respondWithTimeline = false

        /**
         * Will be used to invoke [.ComplicationRequestListener.onComplicationData] on
         * [onComplicationRequest].
         */
        var responseData: ComplicationData? = null

        /**
         * Will be used to invoke [.ComplicationRequestListener.onComplicationDataTimeline] on
         * [onComplicationRequest], if [respondWithTimeline] is true.
         */
        var responseDataTimeline: ComplicationDataTimeline? = null

        /** Last request provided to [onComplicationRequest]. */
        var lastRequest: ComplicationRequest? = null

        /** Will be returned from [previewData]. */
        var previewData: ComplicationData? = null

        /** Last type provided to [previewData]. */
        var lastPreviewType: ComplicationType? = null

        val lastPreviewOrErrorLatch = CountDownLatch(1)

        override fun createMainThreadHandler(): Handler = mPretendMainThreadHandler

        override fun onComplicationRequest(
            request: ComplicationRequest,
            listener: ComplicationRequestListener
        ) {
            lastRequest = request
            try {
                if (respondWithTimeline) {
                    listener.onComplicationDataTimeline(responseDataTimeline)
                } else {
                    listener.onComplicationData(responseData)
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "onComplicationRequest failed with error: ", e)
            }
        }

        override fun getPreviewData(type: ComplicationType): ComplicationData? {
            lastPreviewType = type
            lastPreviewOrErrorLatch.countDown()
            return previewData
        }
    }

    private val mService = MockComplicationDataSourceService()

    @Before
    fun setUp() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
        mProvider =
            mService.onBind(
                Intent(ComplicationDataSourceService.ACTION_COMPLICATION_UPDATE_REQUEST)
            ) as IComplicationProvider.Stub

        mPretendMainThread.start()
        mPretendMainThreadHandler = Handler(mPretendMainThread.looper)
    }

    @After
    fun tearDown() {
        mPretendMainThread.quitSafely()
    }

    @Test
    fun testOnComplicationRequest() {
        mService.responseData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello").build(),
                    ComplicationText.EMPTY
                )
                .build()
        val id = 123
        mProvider.onUpdate(id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager)
        runUiThreadTasksWhileAwaitingDataLatch(1000)

        val data = argumentCaptor<WireComplicationData>()
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture())
        assertThat(data.firstValue.longText!!.getTextAt(Resources.getSystem(), 0))
            .isEqualTo("hello")
        @Suppress("NewApi") // isForSafeWatchFace
        assertThat(mService.lastRequest!!.isForSafeWatchFace)
            .isEqualTo(TargetWatchFaceSafety.UNKNOWN)
    }

    @Test
    fun testOnComplicationRequest_isForSafeWatchFace() {
        mService.responseData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello").build(),
                    ComplicationText.EMPTY
                )
                .build()
        val id = 123

        @Suppress("NewApi") // onUpdate2
        mProvider.onUpdate2(
            id,
            ComplicationType.LONG_TEXT.toWireComplicationType(),
            mLocalManager,
            Bundle().apply {
                putInt(
                    IComplicationProvider.BUNDLE_KEY_IS_SAFE_FOR_WATCHFACE,
                    TargetWatchFaceSafety.SAFE
                )
            }
        )

        runUiThreadTasksWhileAwaitingDataLatch(1000)
        @Suppress("NewApi") // isForSafeWatchFace
        assertThat(mService.lastRequest!!.isForSafeWatchFace).isEqualTo(TargetWatchFaceSafety.SAFE)
    }

    @Test
    fun testOnComplicationRequest_isForSafeWatchFace_malformedBundle() {
        mService.responseData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello").build(),
                    ComplicationText.EMPTY
                )
                .build()
        val id = 123

        @Suppress("NewApi") // onUpdate2
        mProvider.onUpdate2(
            id,
            ComplicationType.LONG_TEXT.toWireComplicationType(),
            mLocalManager,
            Bundle()
        )

        runUiThreadTasksWhileAwaitingDataLatch(1000)
        @Suppress("NewApi") // isForSafeWatchFace
        assertThat(mService.lastRequest!!.isForSafeWatchFace)
            .isEqualTo(TargetWatchFaceSafety.UNKNOWN)
    }

    @Test
    fun testOnComplicationRequestWrongType() {
        mService.responseData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello").build(),
                    ComplicationText.EMPTY
                )
                .build()
        val id = 123
        val exception = AtomicReference<Throwable>()
        val exceptionLatch = CountDownLatch(1)

        mPretendMainThread.uncaughtExceptionHandler =
            Thread.UncaughtExceptionHandler { _, throwable ->
                exception.set(throwable)
                exceptionLatch.countDown()
            }
        mProvider.onUpdate(id, ComplicationType.SHORT_TEXT.toWireComplicationType(), mLocalManager)

        assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(exception.get()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.get())
            .hasMessageThat()
            .isEqualTo(
                "Complication data should match the requested type. " +
                    "Expected SHORT_TEXT got LONG_TEXT."
            )
    }

    @Test
    fun testOnComplicationRequest_invalidData() {
        mService.responseData = INVALID_DATA
        val id = 123
        val exception = AtomicReference<Throwable>()
        val exceptionLatch = CountDownLatch(1)

        mPretendMainThread.uncaughtExceptionHandler =
            Thread.UncaughtExceptionHandler { _, throwable ->
                exception.set(throwable)
                exceptionLatch.countDown()
            }
        mProvider.onUpdate(id, INVALID_DATA.type.toWireComplicationType(), mLocalManager)

        assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(exception.get()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.get()).hasMessageThat().isEqualTo(INVALID_DATA_ERROR_MESSAGE)
    }

    @Test
    fun testOnComplicationRequestNoUpdateRequired() {
        mService.responseData = null

        val id = 123
        mProvider.onUpdate(id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager)
        runUiThreadTasksWhileAwaitingDataLatch(1000)

        val data = argumentCaptor<WireComplicationData>()
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture())
        assertThat(data.allValues).containsExactly(null)
    }

    @Test
    fun testGetComplicationPreviewData() {
        mService.previewData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello preview").build(),
                    ComplicationText.EMPTY
                )
                .build()

        assertThat(
                mProvider
                    .getComplicationPreviewData(ComplicationType.LONG_TEXT.toWireComplicationType())
                    .longText!!
                    .getTextAt(Resources.getSystem(), 0)
            )
            .isEqualTo("hello preview")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    enum class GetComplicationPreviewDataInvalidScenario(
        val data: ComplicationData,
        val message: String
    ) {
        INVALID_PREVIEW_DATA(INVALID_DATA, INVALID_DATA_ERROR_MESSAGE),
        DYNAMIC_RANGED_VALUE(
            RangedValueComplicationData.Builder(
                    dynamicValue = DynamicFloat.constant(1f),
                    fallbackValue = 0f,
                    min = 0f,
                    max = 10f,
                    contentDescription = ComplicationText.EMPTY
                )
                .setText(ComplicationText.EMPTY)
                .build(),
            "Preview data must not have dynamic values."
        ),
        DYNAMIC_LONG_TEXT(
            LongTextComplicationData.Builder(
                    text = DynamicComplicationText(DynamicString.constant("Long Text"), "fallback"),
                    contentDescription = ComplicationText.EMPTY
                )
                .build(),
            "Preview data must not have dynamic values."
        ),
        DYNAMIC_LONG_TITLE(
            LongTextComplicationData.Builder(
                    text = ComplicationText.EMPTY,
                    contentDescription = ComplicationText.EMPTY
                )
                .setTitle(DynamicComplicationText(DynamicString.constant("Long Title"), "fallback"))
                .build(),
            "Preview data must not have dynamic values."
        ),
        DYNAMIC_SHORT_TEXT(
            ShortTextComplicationData.Builder(
                    text =
                        DynamicComplicationText(DynamicString.constant("Short Text"), "fallback"),
                    contentDescription = ComplicationText.EMPTY
                )
                .build(),
            "Preview data must not have dynamic values."
        ),
        DYNAMIC_SHORT_TITLE(
            ShortTextComplicationData.Builder(
                    text = ComplicationText.EMPTY,
                    contentDescription = ComplicationText.EMPTY
                )
                .setTitle(
                    DynamicComplicationText(DynamicString.constant("Short Title"), "fallback")
                )
                .build(),
            "Preview data must not have dynamic values."
        ),
        DYNAMIC_CONTENT_DESCRIPTION(
            LongTextComplicationData.Builder(
                    text = ComplicationText.EMPTY,
                    contentDescription =
                        DynamicComplicationText(DynamicString.constant("Long Text"), "fallback"),
                )
                .build(),
            "Preview data must not have dynamic values."
        ),
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    fun testGetComplicationPreviewData_invalid_fails() {
        for (scenario in GetComplicationPreviewDataInvalidScenario.values()) {
            mService.previewData = scenario.data

            val exception =
                assertFailsWith<IllegalArgumentException> {
                    mProvider.getComplicationPreviewData(
                        scenario.data.type.toWireComplicationType()
                    )
                }

            expect
                .withMessage(scenario.name)
                .that(exception)
                .hasMessageThat()
                .isEqualTo(scenario.message)
        }
    }

    @Test
    fun testTimeline() {
        mService.respondWithTimeline = true
        val timeline = ArrayList<TimelineEntry>()
        timeline.add(
            TimelineEntry(
                TimeInterval(Instant.ofEpochSecond(1000), Instant.ofEpochSecond(4000)),
                LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("A").build(),
                        ComplicationText.EMPTY
                    )
                    .build()
            )
        )
        timeline.add(
            TimelineEntry(
                TimeInterval(Instant.ofEpochSecond(6000), Instant.ofEpochSecond(8000)),
                LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("B").build(),
                        ComplicationText.EMPTY
                    )
                    .build()
            )
        )
        mService.responseDataTimeline =
            ComplicationDataTimeline(
                LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("default").build(),
                        ComplicationText.EMPTY
                    )
                    .build(),
                timeline
            )

        val id = 123
        mProvider.onUpdate(id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager)
        runUiThreadTasksWhileAwaitingDataLatch(1000)
        val data = argumentCaptor<WireComplicationData>()
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture())
        assertThat(data.firstValue.longText!!.getTextAt(Resources.getSystem(), 0))
            .isEqualTo("default")
        val timeLineEntries: List<WireComplicationData?> = data.firstValue.timelineEntries!!
        assertThat(timeLineEntries.size).isEqualTo(2)
        assertThat(timeLineEntries[0]!!.timelineStartEpochSecond).isEqualTo(1000)
        assertThat(timeLineEntries[0]!!.timelineEndEpochSecond).isEqualTo(4000)
        assertThat(timeLineEntries[0]!!.longText!!.getTextAt(Resources.getSystem(), 0))
            .isEqualTo("A")

        assertThat(timeLineEntries[1]!!.timelineStartEpochSecond).isEqualTo(6000)
        assertThat(timeLineEntries[1]!!.timelineEndEpochSecond).isEqualTo(8000)
        assertThat(timeLineEntries[1]!!.longText!!.getTextAt(Resources.getSystem(), 0))
            .isEqualTo("B")
    }

    @Test
    fun testTimeline_invalidData() {
        mService.respondWithTimeline = true
        mService.responseDataTimeline = ComplicationDataTimeline(INVALID_DATA, listOf())
        val id = 123
        val exception = AtomicReference<Throwable>()
        val exceptionLatch = CountDownLatch(1)

        mPretendMainThread.uncaughtExceptionHandler =
            Thread.UncaughtExceptionHandler { _, throwable ->
                exception.set(throwable)
                exceptionLatch.countDown()
            }
        mProvider.onUpdate(id, INVALID_DATA.type.toWireComplicationType(), mLocalManager)

        assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(exception.get()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.get()).hasMessageThat().isEqualTo(INVALID_DATA_ERROR_MESSAGE)
    }

    @Test
    fun testImmediateRequest() {
        mService.responseData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello").build(),
                    ComplicationText.EMPTY
                )
                .build()
        val thread = HandlerThread("testThread")

        try {
            thread.start()
            val threadHandler = Handler(thread.looper)
            val response = AtomicReference<WireComplicationData>()
            val doneLatch = CountDownLatch(1)

            threadHandler.post {
                try {
                    response.set(
                        mProvider.onSynchronousComplicationRequest(
                            123,
                            ComplicationType.LONG_TEXT.toWireComplicationType()
                        )
                    )
                    doneLatch.countDown()
                } catch (e: RemoteException) {
                    // Should not happen
                }
            }

            assertThat(doneLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(response.get().longText!!.getTextAt(Resources.getSystem(), 0))
                .isEqualTo("hello")
            @Suppress("NewApi") // isForSafeWatchFace
            assertThat(mService.lastRequest!!.isForSafeWatchFace)
                .isEqualTo(TargetWatchFaceSafety.UNKNOWN)
        } finally {
            thread.quitSafely()
        }
    }

    @Test
    @Suppress("NewApi") // onSynchronousComplicationRequest2
    fun testImmediateRequest_isForSafeWatchFace() {
        val id = 123
        mService.responseData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello").build(),
                    ComplicationText.EMPTY
                )
                .build()
        val thread = HandlerThread("testThread")
        try {
            thread.start()
            val threadHandler = Handler(thread.looper)
            val response =
                AtomicReference<android.support.wearable.complications.ComplicationData>()
            val doneLatch = CountDownLatch(1)
            threadHandler.post {
                try {
                    @Suppress("NewApi") // onSynchronousComplicationRequest2
                    response.set(
                        mProvider.onSynchronousComplicationRequest2(
                            id,
                            ComplicationType.LONG_TEXT.toWireComplicationType(),
                            Bundle().apply {
                                putInt(
                                    IComplicationProvider.BUNDLE_KEY_IS_SAFE_FOR_WATCHFACE,
                                    TargetWatchFaceSafety.SAFE
                                )
                            }
                        )
                    )
                    doneLatch.countDown()
                } catch (e: RemoteException) {
                    // Should not happen
                }
            }

            assertThat(doneLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            @Suppress("NewApi") // isForSafeWatchFace
            assertThat(mService.lastRequest!!.isForSafeWatchFace)
                .isEqualTo(TargetWatchFaceSafety.SAFE)
        } finally {
            thread.quitSafely()
        }
    }

    @Test
    @Suppress("NewApi") // onSynchronousComplicationRequest2
    fun testImmediateRequest_isForSafeWatchFace_malformedBundle() {
        val id = 123
        mService.responseData =
            LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("hello").build(),
                    ComplicationText.EMPTY
                )
                .build()
        val thread = HandlerThread("testThread")
        try {
            thread.start()
            val threadHandler = Handler(thread.looper)
            val response =
                AtomicReference<android.support.wearable.complications.ComplicationData>()
            val doneLatch = CountDownLatch(1)
            threadHandler.post {
                try {
                    @Suppress("NewApi") // onSynchronousComplicationRequest2
                    response.set(
                        mProvider.onSynchronousComplicationRequest2(
                            id,
                            ComplicationType.LONG_TEXT.toWireComplicationType(),
                            Bundle()
                        )
                    )
                    doneLatch.countDown()
                } catch (e: RemoteException) {
                    // Should not happen
                }
            }

            assertThat(doneLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            @Suppress("NewApi") // isForSafeWatchFace
            assertThat(mService.lastRequest!!.isForSafeWatchFace)
                .isEqualTo(TargetWatchFaceSafety.UNKNOWN)
        } finally {
            thread.quitSafely()
        }
    }

    @Test
    fun testImmediateRequest_invalidData() {
        mService.responseData = INVALID_DATA
        val thread = HandlerThread("testThread")

        try {
            thread.start()
            val threadHandler = Handler(thread.looper)
            val response = AtomicReference<WireComplicationData>()
            val exception = AtomicReference<Throwable>()
            val exceptionLatch = CountDownLatch(1)

            mPretendMainThread.uncaughtExceptionHandler =
                Thread.UncaughtExceptionHandler { _, throwable ->
                    exception.set(throwable)
                    exceptionLatch.countDown()
                }
            threadHandler.post {
                try {
                    response.set(
                        mProvider.onSynchronousComplicationRequest(
                            123,
                            INVALID_DATA.type.toWireComplicationType()
                        )
                    )
                } catch (e: RemoteException) {
                    // Should not happen
                }
            }

            assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(exception.get()).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(exception.get()).hasMessageThat().isEqualTo(INVALID_DATA_ERROR_MESSAGE)
        } finally {
            thread.quitSafely()
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testImmediateRequest_invalidTimelineData() {
        mService.respondWithTimeline = true
        mService.responseDataTimeline = ComplicationDataTimeline(INVALID_DATA, listOf())
        val thread = HandlerThread("testThread")

        try {
            thread.start()
            val threadHandler = Handler(thread.looper)
            val response = AtomicReference<WireComplicationData>()
            val exception = AtomicReference<Throwable>()
            val exceptionLatch = CountDownLatch(1)

            mPretendMainThread.uncaughtExceptionHandler =
                Thread.UncaughtExceptionHandler { _, throwable ->
                    exception.set(throwable)
                    exceptionLatch.countDown()
                }
            threadHandler.post {
                try {
                    response.set(
                        mProvider.onSynchronousComplicationRequest(
                            123,
                            INVALID_DATA.type.toWireComplicationType()
                        )
                    )
                } catch (e: RemoteException) {
                    // Should not happen
                }
            }

            assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(exception.get()).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(exception.get()).hasMessageThat().isEqualTo(INVALID_DATA_ERROR_MESSAGE)
        } finally {
            thread.quitSafely()
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun testImmediateRequest_invalidTimelineData_preT() {
        mService.respondWithTimeline = true
        mService.responseDataTimeline = ComplicationDataTimeline(INVALID_DATA, listOf())
        val thread = HandlerThread("testThread")

        try {
            thread.start()
            val threadHandler = Handler(thread.looper)
            val response = AtomicReference<WireComplicationData>()
            val exception = AtomicReference<Throwable>()
            val exceptionLatch = CountDownLatch(1)

            mPretendMainThread.uncaughtExceptionHandler =
                Thread.UncaughtExceptionHandler { _, throwable ->
                    exception.set(throwable)
                    exceptionLatch.countDown()
                }
            threadHandler.post {
                try {
                    response.set(
                        mProvider.onSynchronousComplicationRequest(
                            123,
                            INVALID_DATA.type.toWireComplicationType()
                        )
                    )
                } catch (e: RemoteException) {
                    // Should not happen
                }
            }

            assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isFalse()
        } finally {
            thread.quitSafely()
        }
    }

    private fun runUiThreadTasksWhileAwaitingDataLatch(timeout: Long) {
        // Allowing UI thread to execute while we wait for the data latch.
        var attempts: Long = 0
        while (!mUpdateComplicationDataLatch.await(1, TimeUnit.MILLISECONDS)) {
            runUiThreadTasks()
            assertThat(attempts++).isLessThan(timeout) // In total waiting ~timeout.
        }
    }

    companion object {
        private const val TAG = "ComplicationDataSourceServiceTest"

        private val INVALID_DATA =
            RangedValueComplicationData.Builder(
                    value = 100f, // Higher than max.
                    min = 0f,
                    max = 10f,
                    contentDescription = ComplicationText.EMPTY
                )
                .setText(ComplicationText.EMPTY)
                .build()
        private val INVALID_DATA_ERROR_MESSAGE =
            "From T API onwards, value must be between min and max"
    }
}
