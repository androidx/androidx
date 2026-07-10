/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose

import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.Rc.Time.TIME_IN_HR
import androidx.compose.remote.player.compose.test.util.getCoreDocument
import androidx.compose.remote.player.compose.test.utils.RemoteDocScreenshotTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class TimeTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteDocScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun clockOverriddenInCoreDocument_usedInTimeAttributes() {
        val document =
            getCoreDocument(
                clock =
                    SystemClock(Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC))
            ) {
                val secs =
                    mRemoteWriter.floatExpression(TIME_IN_SEC, 86400f, AnimatedFloatExpression.MOD)
                val nowMins =
                    mRemoteWriter.floatExpression(
                        secs,
                        60f,
                        AnimatedFloatExpression.DIV,
                        AnimatedFloatExpression.FLOOR,
                        60f,
                        AnimatedFloatExpression.MOD,
                    )
                val hoursString =
                    mRemoteWriter.createTextFromFloat(TIME_IN_HR, 2, 0, TextFromFloat.PAD_PRE_ZERO)
                val minsString =
                    mRemoteWriter.createTextFromFloat(nowMins, 2, 0, TextFromFloat.PAD_PRE_ZERO)
                val separator = mRemoteWriter.addText(":")
                val nowString =
                    mRemoteWriter.textMerge(
                        mRemoteWriter.textMerge(hoursString, separator),
                        minsString,
                    )

                val otherTime = writer.addLong(Instant.parse("2025-01-01T11:00:00Z").toEpochMilli())
                val hoursFromNow =
                    mRemoteWriter.floatExpression(
                        mRemoteWriter.timeAttribute(otherTime, TimeAttribute.TIME_FROM_NOW_HR)
                    )
                val hoursFromNowString =
                    mRemoteWriter.createTextFromFloat(
                        hoursFromNow,
                        1,
                        0,
                        TextFromFloat.PAD_PRE_NONE,
                    )

                mRemoteWriter.drawTextAnchored(nowString, 0f, 100f, -1f, 0f, 0)
                mRemoteWriter.drawTextAnchored(hoursFromNowString, 0f, 200f, -1f, 0f, 0)
            }

        remoteComposeTestRule.runScreenshotTest(
            coreDocument = document,
            context = ApplicationProvider.getApplicationContext(),
        )
    }
}
