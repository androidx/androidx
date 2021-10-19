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

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.layout.CheckBox
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.unit.Dp
import androidx.glance.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class CheckBoxTest {

    @get:Rule
    val mHostRule = AppWidgetHostRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val checkedCheckBox: @Composable () -> Unit = {
        Box {
            CheckBox(checked = true, text = "Hello world")
        }
    }

    private val uncheckedCheckBox: @Composable () -> Unit = {
        Box {
            CheckBox(checked = false, text = "Hola mundo")
        }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun check_box_checked_31() {
        TestGlanceAppWidget.uiDefinition = checkedCheckBox

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = hostView.findChildByType<android.widget.CheckBox>()
            assertNotNull(child)
            assertThat(child.text.toString()).isEqualTo("Hello world")
            assertThat(child.isChecked).isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 30)
    @Test
    fun check_box_checked_29_30() {
        TestGlanceAppWidget.uiDefinition = checkedCheckBox

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<ViewGroup>(hostView.getChildAt(0))
            val textView = child.findViewById<TextView>(R.id.checkBoxText)
            assertThat(textView.text.toString()).isEqualTo("Hello world")

            val image = child.findViewById<ImageView>(R.id.checkBoxIcon)
            assertThat(image.isEnabled).isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun check_box_unchecked_31() {
        TestGlanceAppWidget.uiDefinition = uncheckedCheckBox

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = hostView.findChildByType<android.widget.CheckBox>()
            assertNotNull(child)
            assertThat(child.text.toString()).isEqualTo("Hola mundo")
            assertThat(child.isChecked).isFalse()
        }
    }

    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 30)
    @Test
    fun check_box_unchecked_29_30() {
        TestGlanceAppWidget.uiDefinition = uncheckedCheckBox

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)
            val child = assertIs<ViewGroup>(hostView.getChildAt(0))
            val textView = child.findViewById<TextView>(R.id.checkBoxText)
            assertThat(textView.text.toString()).isEqualTo("Hola mundo")

            val image = child.findViewById<ImageView>(R.id.checkBoxIcon)
            assertThat(image.isEnabled).isFalse()
        }
    }

    @Test
    fun check_box_modifiers() {
        TestGlanceAppWidget.uiDefinition = {
            Box {
                CheckBox(checked = true, modifier = GlanceModifier.padding(5.dp, 6.dp, 7.dp, 8.dp))
            }
        }

        mHostRule.startHost()

        mHostRule.onHostView { hostView ->
            assertThat(hostView.childCount).isEqualTo(1)

            val checkboxRoot = (hostView.getChildAt(0) as ViewGroup).getChildAt(0)
            assertThat(checkboxRoot.paddingStart).isEqualTo(5.dp.toPx())
            assertThat(checkboxRoot.paddingTop).isEqualTo(6.dp.toPx())
            assertThat(checkboxRoot.paddingEnd).isEqualTo(7.dp.toPx())
            assertThat(checkboxRoot.paddingBottom).isEqualTo(8.dp.toPx())
        }
    }

    private fun Dp.toPx() = toPixels(context.resources.displayMetrics)
}