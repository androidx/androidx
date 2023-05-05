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

import android.annotation.SuppressLint
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
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.ComplicationDataSourceChooserIntent
import androidx.wear.watchface.ComplicationHelperActivity
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.client.asApiEditorState
import androidx.wear.watchface.complications.ComplicationDataSourceInfo
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

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
    @OptIn(ComplicationExperimental::class)
    private val leftComplication =
        @Suppress("DEPRECATION")
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                LEFT_COMPLICATION_ID,
                { _, _,
                    ->
                    mockLeftCanvasComplication
                },
                listOf(
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.LONG_TEXT,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    ComplicationType.SMALL_IMAGE
                ),
                DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET),
                ComplicationSlotBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            )
            .setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
            .build()

    private val mockRightCanvasComplication =
        CanvasComplicationDrawable(
            ComplicationDrawable(),
            placeholderWatchState,
            mockInvalidateCallback
        )
    @OptIn(ComplicationExperimental::class)
    private val rightComplication =
        @Suppress("DEPRECATION")
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                RIGHT_COMPLICATION_ID,
                { _, _,
                    ->
                    mockRightCanvasComplication
                },
                listOf(
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.LONG_TEXT,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    ComplicationType.SMALL_IMAGE
                ),
                DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_DAY_OF_WEEK),
                ComplicationSlotBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
            )
            .setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
            .build()

    private val backgroundHandlerThread = HandlerThread("TestBackgroundThread").apply { start() }

    private val backgroundHandler = Handler(backgroundHandlerThread.looper)

    @SuppressLint("NewApi")
    private fun createOnWatchFaceEditingTestActivity(
        userStyleSettings: List<UserStyleSetting>,
        complicationSlots: List<ComplicationSlot>,
        watchFaceId: WatchFaceId = testInstanceId,
        previewReferenceInstant: Instant = Instant.ofEpochMilli(12345)
    ): ActivityScenario<OnWatchFaceEditingTestActivity> {
        val userStyleRepository = CurrentUserStyleRepository(UserStyleSchema(userStyleSettings))
        val complicationSlotsManager =
            ComplicationSlotsManager(complicationSlots, userStyleRepository)
        complicationSlotsManager.watchState = placeholderWatchState

        WatchFace.registerEditorDelegate(testComponentName, editorDelegate)
        Mockito.`when`(editorDelegate.complicationSlotsManager).thenReturn(complicationSlotsManager)
        Mockito.`when`(editorDelegate.userStyleSchema).thenReturn(userStyleRepository.schema)
        Mockito.`when`(editorDelegate.userStyle).thenReturn(userStyleRepository.userStyle.value)
        Mockito.`when`(editorDelegate.screenBounds).thenReturn(screenBounds)
        Mockito.`when`(editorDelegate.previewReferenceInstant).thenReturn(previewReferenceInstant)
        Mockito.`when`(editorDelegate.backgroundThreadHandler).thenReturn(backgroundHandler)

        OnWatchFaceEditingTestActivity.complicationDataSourceInfoRetrieverProvider =
            TestComplicationDataSourceInfoRetrieverProvider()

        return ActivityScenario.launch(
            WatchFaceEditorContract()
                .createIntent(
                    ApplicationProvider.getApplicationContext<Context>(),
                    EditorRequest(
                        testComponentName,
                        testEditorPackageName,
                        null,
                        watchFaceId,
                        null,
                        null
                    )
                )
                .apply {
                    component =
                        ComponentName(
                            ApplicationProvider.getApplicationContext<Context>(),
                            OnWatchFaceEditingTestActivity::class.java
                        )
                }
        )
    }

    @After
    public fun tearDown() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = false
        ComplicationHelperActivity.useTestComplicationDataSourceChooserActivity = false
        ComplicationHelperActivity.skipPermissionCheck = false
        WatchFace.clearAllEditorDelegates()
        backgroundHandlerThread.quitSafely()
    }

    @Test
    @Ignore("b/281083901")
    public fun listenableOpenComplicationDataSourceChooser() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationDataSourceInfo =
            ComplicationDataSourceInfo(
                "TestDataSource3App",
                "TestDataSource3",
                Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
                ComplicationType.LONG_TEXT,
                dataSource3
            )
        TestComplicationHelperActivity.resultIntent =
            CompletableDeferred(
                Intent().apply {
                    putExtra(
                        ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                        chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
                    )
                }
            )
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var listenableEditorSession: ListenableEditorSession
        scenario.onActivity { activity ->
            listenableEditorSession = activity.listenableEditorSession
        }

        /**
         * Invoke [TestComplicationHelperActivity] which will change the data source (and hence the
         * preview data) for [LEFT_COMPLICATION_ID].
         */
        val chosenComplicationDataSource =
            listenableEditorSession
                .listenableOpenComplicationDataSourceChooser(LEFT_COMPLICATION_ID)
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertThat(chosenComplicationDataSource).isNotNull()
        checkNotNull(chosenComplicationDataSource)
        assertThat(chosenComplicationDataSource.complicationSlotId).isEqualTo(LEFT_COMPLICATION_ID)
        assertEquals(
            chosenComplicationDataSourceInfo,
            chosenComplicationDataSource.complicationDataSourceInfo
        )

        // This should update the preview data to point to the updated dataSource3 data.
        val previewComplication =
            listenableEditorSession.complicationsPreviewData.value[LEFT_COMPLICATION_ID]
                as LongTextComplicationData

        assertThat(
                previewComplication.text.getTextAt(
                    ApplicationProvider.getApplicationContext<Context>().resources,
                    Instant.EPOCH
                )
            )
            .isEqualTo("DataSource3")
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun doNotCommitChangesOnClose() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList()
            )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        lateinit var listenableEditorSession: ListenableEditorSession
        scenario.onActivity { activity ->
            listenableEditorSession = activity.listenableEditorSession

            assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
                .isEqualTo(redStyleOption.id.value)
            assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
                .isEqualTo(classicStyleOption.id.value)

            // Select [blueStyleOption] and [gothicStyleOption], which are the last options in the
            // corresponding setting definitions.
            listenableEditorSession.userStyle.value =
                listenableEditorSession.userStyle.value
                    .toMutableUserStyle()
                    .apply {
                        listenableEditorSession.userStyleSchema.userStyleSettings.forEach {
                            this[it] = it.options.last()
                        }
                    }
                    .toUserStyle()

            // This should cause the style on the to be reverted back to the initial style.
            listenableEditorSession.commitChangesOnClose = false
            listenableEditorSession.close()
            activity.finish()
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .asApiEditorState()
        assertThat(result.userStyle.userStyleMap[colorStyleSetting.id.value])
            .isEqualTo(blueStyleOption.id.value)
        assertThat(result.userStyle.userStyleMap[watchHandStyleSetting.id.value])
            .isEqualTo(gothicStyleOption.id.value)
        Assert.assertFalse(result.shouldCommitChanges)
        Assert.assertNull(result.previewImage)

        // The original style should be applied to the watch face however because
        // commitChangesOnClose is false.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(redStyleOption.id.value)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(classicStyleOption.id.value)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }
}
