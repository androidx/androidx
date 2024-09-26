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

package androidx.wear.watchface.style

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.ServiceTestRule
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.test.IStyleEchoService
import androidx.wear.watchface.style.test.R
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@LargeTest
class OldClientAidlCompatTest {
    @get:Rule val serviceRule = ServiceTestRule()

    companion object {
        private val CONTEXT: Context = ApplicationProvider.getApplicationContext()

        private val COLOR_STYLE_SETTING =
            ListUserStyleSetting.Builder(
                    UserStyleSetting.Id("COLOR_STYLE_SETTING"),
                    options =
                        listOf(
                            ListUserStyleSetting.ListOption.Builder(
                                    Option.Id("RED_STYLE"),
                                    CONTEXT.resources,
                                    R.string.colors_style_red,
                                    R.string.colors_style_red_screen_reader
                                )
                                .setIcon(Icon.createWithResource(CONTEXT, R.drawable.red_style))
                                .build(),
                            ListUserStyleSetting.ListOption.Builder(
                                    Option.Id("GREEN_STYLE"),
                                    CONTEXT.resources,
                                    R.string.colors_style_green,
                                    R.string.colors_style_green_screen_reader
                                )
                                .setIcon(Icon.createWithResource(CONTEXT, R.drawable.green_style))
                                .build(),
                            ListUserStyleSetting.ListOption.Builder(
                                    Option.Id("BLUE_STYLE"),
                                    CONTEXT.resources,
                                    R.string.colors_style_blue,
                                    R.string.colors_style_blue_screen_reader
                                )
                                .setIcon(Icon.createWithResource(CONTEXT, R.drawable.blue_style))
                                .build()
                        ),
                    listOf(
                        WatchFaceLayer.BASE,
                        WatchFaceLayer.COMPLICATIONS,
                        WatchFaceLayer.COMPLICATIONS_OVERLAY
                    ),
                    CONTEXT.resources,
                    R.string.colors_style_setting,
                    R.string.colors_style_setting_description
                )
                .build()

        private val DRAW_HOUR_PIPS_SETTING =
            UserStyleSetting.BooleanUserStyleSetting.Builder(
                    UserStyleSetting.Id("DRAW_HOUR_PIPS_STYLE_SETTING"),
                    listOf(WatchFaceLayer.BASE),
                    true,
                    CONTEXT.resources,
                    R.string.watchface_pips_setting,
                    R.string.watchface_pips_setting_description
                )
                .build()

        private val WATCH_HAND_LENGTH_SETTING =
            UserStyleSetting.DoubleRangeUserStyleSetting.Builder(
                    UserStyleSetting.Id("WATCH_HAND_LENGTH_STYLE_SETTING"),
                    minimumValue = 0.25,
                    maximumValue = 1.0,
                    defaultValue = 0.75,
                    listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
                    CONTEXT.resources,
                    R.string.watchface_hand_length_setting,
                    R.string.watchface_hand_length_setting_description
                )
                .build()

        private val COMPLICATIONS_STYLE_SETTING =
            ComplicationSlotsUserStyleSetting.Builder(
                    UserStyleSetting.Id("COMPLICATIONS_STYLE_SETTING"),
                    listOf(
                        @Suppress("deprecation")
                        ComplicationSlotsOption(
                            Option.Id("LEFT_AND_RIGHT_COMPLICATIONS"),
                            CONTEXT.resources,
                            R.string.watchface_complications_setting_both,
                            null,
                            // NB this list is empty because each [ComplicationSlotOverlay] is
                            // applied on top of the initial config.
                            listOf(),
                            null
                        ),
                        @Suppress("deprecation")
                        ComplicationSlotsOption(
                            Option.Id("NO_COMPLICATIONS"),
                            CONTEXT.resources,
                            R.string.watchface_complications_setting_none,
                            null,
                            listOf(
                                ComplicationSlotOverlay.Builder(complicationSlotId = 1)
                                    .setEnabled(false)
                                    .build(),
                                ComplicationSlotOverlay.Builder(complicationSlotId = 2)
                                    .setEnabled(false)
                                    .build()
                            ),
                            null
                        ),
                        @Suppress("deprecation")
                        ComplicationSlotsOption(
                            Option.Id("LEFT_COMPLICATION"),
                            CONTEXT.resources,
                            R.string.watchface_complications_setting_left,
                            null,
                            listOf(
                                ComplicationSlotOverlay.Builder(complicationSlotId = 1)
                                    .setEnabled(false)
                                    .build()
                            ),
                            null
                        ),
                        @Suppress("deprecation")
                        ComplicationSlotsOption(
                            Option.Id("RIGHT_COMPLICATION"),
                            CONTEXT.resources,
                            R.string.watchface_complications_setting_right,
                            null,
                            listOf(
                                ComplicationSlotOverlay.Builder(complicationSlotId = 2)
                                    .setEnabled(false)
                                    .build()
                            ),
                            null
                        ),
                    ),
                    listOf(WatchFaceLayer.COMPLICATIONS),
                    CONTEXT.resources,
                    R.string.watchface_complications_setting,
                    R.string.watchface_complications_setting_description
                )
                .build()

        private val LONG_RANGE_SETTING =
            UserStyleSetting.LongRangeUserStyleSetting.Builder(
                    UserStyleSetting.Id("HOURS_DRAW_FREQ_STYLE_SETTING"),
                    minimumValue = 0,
                    maximumValue = 4,
                    defaultValue = 1,
                    listOf(WatchFaceLayer.BASE),
                    CONTEXT.resources,
                    R.string.watchface_draw_hours_freq_setting,
                    R.string.watchface_draw_hours_freq_setting_description
                )
                .build()

        private val SCHEMA =
            UserStyleSchema(
                listOf(
                    COLOR_STYLE_SETTING,
                    DRAW_HOUR_PIPS_SETTING,
                    WATCH_HAND_LENGTH_SETTING,
                    COMPLICATIONS_STYLE_SETTING,
                    LONG_RANGE_SETTING
                )
            )

        private const val ACTON = "com.google.android.wearable.action.TEST"
        private const val TEXT_FIXTURE_APK = "watchface-style-old-api-test-service-release.apk"
        private const val PACKAGE_NAME = "androidx.wear.watchface.style.test.oldApiTestService"
    }

    @Before fun setUp() = withPackageName(PACKAGE_NAME) { install(TEXT_FIXTURE_APK) }

    @After fun tearDown() = withPackageName(PACKAGE_NAME) { uninstall() }

    @Test
    @Ignore // TODO(b/265425077): This test is failing on the bots, fix it.
    fun roundTripUserStyleSchema() = runBlocking {
        val service =
            IStyleEchoService.Stub.asInterface(
                bindService(Intent(ACTON).apply { setPackage(PACKAGE_NAME) })
            )

        val result =
            UserStyleSchema(service.roundTripToApiUserStyleSchemaWireFormat(SCHEMA.toWireFormat()))

        // We expect five root settings back. Some of the details will have been clipped which
        // is expected because not all the current features are supported by v1.1.1, the main
        // thing is the service didn't crash!
        assertThat(result.rootUserStyleSettings.size).isEqualTo(5)
    }

    private suspend fun bindService(intent: Intent): IBinder = suspendCoroutine { continuation ->
        val bound =
            CONTEXT.bindService(
                intent,
                object : ServiceConnection {
                    override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
                        continuation.resume(binder)
                    }

                    override fun onServiceDisconnected(p0: ComponentName?) {}
                },
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
            )

        if (!bound) {
            continuation.resumeWithException(IllegalStateException("No service found for $intent."))
        }
    }
}
