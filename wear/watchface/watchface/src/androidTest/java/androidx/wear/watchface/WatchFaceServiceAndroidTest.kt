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

package androidx.wear.watchface

import android.content.Context
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.watchface.test.SimpleWatchFaceTestService
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceServiceAndroidTest {
    @After
    public fun tearDown() {
        InteractiveInstanceManager.setParameterlessEngine(null)
    }

    @SdkSuppress(maxSdkVersion = 32) // b/275361339
    @Test
    fun measuresWatchFaceIconsFromCustomContext() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val serviceSpy =
            object : SimpleWatchFaceTestService() {
                override val resourcesContext: Context
                    get() =
                        this.createPackageContext(context.packageName, Context.CONTEXT_RESTRICTED)
            }
        val engine = serviceSpy.onCreateEngine() as WatchFaceService.EngineWrapper

        try {
            val schema =
                UserStyleSchema(
                    listOf(
                        UserStyleSetting.ListUserStyleSetting(
                            UserStyleSetting.Id("someId"),
                            "displayName",
                            "description",
                            Icon.createWithResource(
                                context,
                                androidx.wear.watchface.test.R.drawable.example_icon_24
                            ),
                            listOf(
                                UserStyleSetting.ListUserStyleSetting.ListOption(
                                    UserStyleSetting.Option.Id("red_style"),
                                    displayName = "Red",
                                    screenReaderName = "Red watchface style",
                                    icon =
                                        Icon.createWithResource(
                                            context,
                                            androidx.wear.watchface.test.R.drawable.example_icon_24
                                        ),
                                )
                            ),
                            listOf(WatchFaceLayer.BASE)
                        )
                    )
                )

            // expect no exception
            engine.validateSchemaWireSize(schema)
        } finally {
            engine.onDestroy()
            serviceSpy.onDestroy()
        }
    }
}
