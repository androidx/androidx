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

package androidx.wear.watchface.editor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.HandlerThread
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.complications.ComplicationHelperActivity
import androidx.wear.complications.ComplicationSlotBounds
import androidx.wear.complications.ComplicationProviderInfo
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.util.concurrent.TimeUnit

private const val TIMEOUT_MS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
public class EditorSessionGuavaTest {
    private val testComponentName = ComponentName("test.package", "test.class")
    private val testEditorPackageName = "test.package"
    private val testInstanceId = WatchFaceId("TEST_INSTANCE_ID")
    private var editorDelegate = Mockito.mock(WatchFace.EditorDelegate::class.java)
    private val screenBounds = Rect(0, 0, 400, 400)

    private val mockInvalidateCallback =
        Mockito.mock(CanvasComplication.InvalidateCallback::class.java)
    private val placeholderWatchState = MutableWatchState().asWatchState()
    private val mockLeftCanvasComplication =
        CanvasComplicationDrawable(
            ComplicationDrawable(),
            placeholderWatchState,
            mockInvalidateCallback
        )
    private val leftComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
            LEFT_COMPLICATION_ID,
            { _, _, -> mockLeftCanvasComplication },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_SUNRISE_SUNSET),
            ComplicationSlotBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val mockRightCanvasComplication =
        CanvasComplicationDrawable(
            ComplicationDrawable(),
            placeholderWatchState,
            mockInvalidateCallback
        )
    private val rightComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
            RIGHT_COMPLICATION_ID,
            { _, _, -> mockRightCanvasComplication },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
            ComplicationSlotBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val backgroundHandlerThread = HandlerThread("TestBackgroundThread").apply {
        start()
    }

    private val backgroundHandler = Handler(backgroundHandlerThread.looper)

    private fun createOnWatchFaceEditingTestActivity(
        userStyleSettings: List<UserStyleSetting>,
        complicationSlots: List<ComplicationSlot>,
        watchFaceId: WatchFaceId = testInstanceId,
        previewReferenceTimeMillis: Long = 12345
    ): ActivityScenario<OnWatchFaceEditingTestActivity> {
        val userStyleRepository = CurrentUserStyleRepository(UserStyleSchema(userStyleSettings))
        val complicationSlotsManager =
            ComplicationSlotsManager(complicationSlots, userStyleRepository)
        complicationSlotsManager.watchState = placeholderWatchState

        WatchFace.registerEditorDelegate(testComponentName, editorDelegate)
        Mockito.`when`(editorDelegate.complicationSlotsManager).thenReturn(complicationSlotsManager)
        Mockito.`when`(editorDelegate.userStyleSchema).thenReturn(userStyleRepository.schema)
        Mockito.`when`(editorDelegate.userStyle).thenReturn(userStyleRepository.userStyle)
        Mockito.`when`(editorDelegate.screenBounds).thenReturn(screenBounds)
        Mockito.`when`(editorDelegate.previewReferenceTimeMillis)
            .thenReturn(previewReferenceTimeMillis)
        Mockito.`when`(editorDelegate.backgroundThreadHandler).thenReturn(backgroundHandler)

        OnWatchFaceEditingTestActivity.providerInfoRetrieverProvider =
            TestProviderInfoRetrieverProvider()

        return ActivityScenario.launch(
            WatchFaceEditorContract().createIntent(
                ApplicationProvider.getApplicationContext<Context>(),
                EditorRequest(testComponentName, testEditorPackageName, null, watchFaceId)
            ).apply {
                component = ComponentName(
                    ApplicationProvider.getApplicationContext<Context>(),
                    OnWatchFaceEditingTestActivity::class.java
                )
            }
        )
    }

    @After
    public fun tearDown() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = false
        ComplicationHelperActivity.useTestComplicationProviderChooserActivity = false
        ComplicationHelperActivity.skipPermissionCheck = false
        WatchFace.clearAllEditorDelegates()
        backgroundHandlerThread.quitSafely()
    }

    @Test
    public fun getListenableComplicationPreviewData() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var listenableEditorSession: ListenableEditorSession
        scenario.onActivity { activity ->
            listenableEditorSession = activity.listenableEditorSession
        }

        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val future = listenableEditorSession.getListenableComplicationPreviewData()
        val previewData = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        val leftComplicationData = previewData[LEFT_COMPLICATION_ID] as
            ShortTextComplicationData
        assertThat(
            leftComplicationData.text.getTextAt(resources, 0)
        ).isEqualTo("Left")

        val rightComplicationData = previewData[RIGHT_COMPLICATION_ID] as
            LongTextComplicationData
        assertThat(
            rightComplicationData.text.getTextAt(resources, 0)
        ).isEqualTo("Right")
    }

    @Test
    public fun listenableOpenComplicationProviderChooser() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationProviderInfo = ComplicationProviderInfo(
            "TestProvider3App",
            "TestProvider3",
            Icon.createWithBitmap(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            ),
            ComplicationType.LONG_TEXT,
            provider3
        )
        TestComplicationHelperActivity.resultIntent = CompletableDeferred(
            Intent().apply {
                putExtra(
                    "android.support.wearable.complications.EXTRA_PROVIDER_INFO",
                    chosenComplicationProviderInfo.toWireComplicationProviderInfo()
                )
            }
        )
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var listenableEditorSession: ListenableEditorSession
        scenario.onActivity { activity ->
            listenableEditorSession = activity.listenableEditorSession
        }

        /**
         * Invoke [TestComplicationHelperActivity] which will change the provider (and hence
         * the preview data) for [LEFT_COMPLICATION_ID].
         */
        val chosenComplicationProvider =
            listenableEditorSession.listenableOpenComplicationProviderChooser(
                LEFT_COMPLICATION_ID
            ).get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertThat(chosenComplicationProvider).isNotNull()
        checkNotNull(chosenComplicationProvider)
        assertThat(chosenComplicationProvider.complicationSlotId).isEqualTo(LEFT_COMPLICATION_ID)
        assertEquals(
            chosenComplicationProviderInfo,
            chosenComplicationProvider.complicationProviderInfo
        )

        // This should update the preview data to point to the updated provider3 data.
        val previewComplication =
            listenableEditorSession.getListenableComplicationPreviewData()
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)[LEFT_COMPLICATION_ID]
                as LongTextComplicationData

        assertThat(
            previewComplication.text.getTextAt(
                ApplicationProvider.getApplicationContext<Context>().resources,
                0
            )
        ).isEqualTo("Provider3")
    }
}
