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

package androidx.glance.appwidget

import android.widget.TextView
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.Text
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class GlanceAppWidgetReceiverTest {
    @get:Rule
    val mHostRule = AppWidgetHostRule()

    @Test
    fun createSimpleAppWidget() {
        TestGlanceAppWidget.uiDefinition = {
            Text(
                "text",
                style = TextStyle(
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                )
            )
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.findChild<TextView> { it.text.toString() == "text" })
                .isNotNull()
        }
    }
}
