/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget

import android.widget.ImageView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.appwidget.test.R
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.size
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class ImageTest {

    @get:Rule
    val mHostRule = AppWidgetHostRule()

    @Test
    fun colorFilter_toggle() = runBlocking {
        val shouldTintImageFlow = MutableStateFlow(true)
        TestGlanceAppWidget.uiDefinition = {
            val shouldTint by shouldTintImageFlow.collectAsState()
            Column(modifier = GlanceModifier.size(100.dp).background(Color.DarkGray)) {
                Image(
                    provider = androidx.glance.ImageProvider(R.drawable.oval),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = if (shouldTint) {
                        ColorFilter.tint(GlanceTheme.colors.onSurface)
                    } else {
                        null
                    }
                )
            }
        }
        mHostRule.startHost()

        mHostRule.waitAndTestForCondition(
            errorMessage = "No ImageView with colorFilter != null was found"
        ) { hostView ->
            val child = hostView.findChildByType<ImageView>()
            child != null && child.colorFilter != null
        }

        shouldTintImageFlow.emit(false)

        mHostRule.waitAndTestForCondition(
            errorMessage = "No ImageView with colorFilter == null was found"
        ) { hostView ->
            val child = hostView.findChildByType<ImageView>()
            child != null && child.colorFilter == null
        }
    }
}
