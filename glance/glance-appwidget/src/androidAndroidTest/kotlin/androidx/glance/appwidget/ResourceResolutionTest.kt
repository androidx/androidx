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
import androidx.glance.Modifier
import androidx.glance.appwidget.test.R
import androidx.glance.layout.Column
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotNull

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class ResourceResolutionTest {

    @get:Rule
    val mHostRule = AppWidgetHostRule()

    @Test
    fun resolveFromResources() {
        TestGlanceAppWidget.uiDefinition = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "dimension",
                    modifier = Modifier.width(R.dimen.testDimension)
                )
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            val textView =
                assertNotNull(hostView.findChild<TextView> { it.text.toString() == "dimension" })
            assertThat(textView.measuredWidth).isEqualTo(
                textView.context.resources.getDimensionPixelSize(
                    R.dimen.testDimension
                )
            )
        }
    }
}