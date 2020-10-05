/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import android.support.wearable.watchface.ashmemCompressedImageBundleToBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.data.ImmutableSystemState
import androidx.wear.watchface.samples.ExampleCanvasWatchFaceService
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val API_VERSION = 3

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceControlServiceTest {
    private val complicationProviders = mapOf(
        SystemProviders.DAY_OF_WEEK to
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(ComplicationText.plainText("23rd"))
                .setShortText(ComplicationText.plainText("Mon"))
                .build(),
        SystemProviders.STEP_COUNT to
            ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                .setShortTitle(ComplicationText.plainText("Steps"))
                .setShortText(ComplicationText.plainText("100"))
                .build()
    )

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface")

    @Test
    fun createTestCanvasWatchFaceService() {
        val instanceService = IWatchFaceControlService.Stub.asInterface(
            WatchFaceControlService().apply {
                setContext(ApplicationProvider.getApplicationContext<Context>())
            }.onBind(
                Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE)
            )
        )
        val instance = instanceService.createWatchFaceInstance(
            ComponentName(
                ApplicationProvider.getApplicationContext<Context>(),
                ExampleCanvasWatchFaceService::class.java
            )
        )
        val watchFaceService = WatchFaceServiceStub(API_VERSION, complicationProviders)
        instance.initWithoutSurface(
            watchFaceService,
            ImmutableSystemState(false, false),
            100,
            100
        )

        val bitmap = watchFaceService.watchFaceCommand!!.takeWatchfaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS).toWireFormat(),
            100,
            1234567890,
            null
        ).ashmemCompressedImageBundleToBitmap()

        bitmap!!.assertAgainstGolden(screenshotRule, "service_interactive")

        instance.destroy()
    }
}