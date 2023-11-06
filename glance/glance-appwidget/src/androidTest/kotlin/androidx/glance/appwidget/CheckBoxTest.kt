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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class CheckBoxTest {

    @get:Rule
    val mHostRule = AppWidgetHostRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val checkedCheckBox: @Composable () -> Unit = {
        Box {
            CheckBox(checked = true, onCheckedChange = null, text = "Hello world")
        }
    }

    private val uncheckedCheckBox: @Composable () -> Unit = {
        Box {
            CheckBox(checked = false, onCheckedChange = null, text = "Hola mundo")
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

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            val row = assertIs<LinearLayout>(box.notGoneChildren.single())
            val textView = assertIs<TextView>(row.getChildAt(1))
            assertThat(textView.text.toString()).isEqualTo("Hello world")

            val image = assertIs<ImageView>(row.getChildAt(0))
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

        mHostRule.onUnboxedHostView<FrameLayout> { box ->
            val row = assertIs<LinearLayout>(box.notGoneChildren.single())
            val textView = assertIs<TextView>(row.getChildAt(1))
            assertThat(textView.text.toString()).isEqualTo("Hola mundo")

            val image = assertIs<ImageView>(row.getChildAt(0))
            assertThat(image.isEnabled).isFalse()
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun check_box_modifiers() {
        TestGlanceAppWidget.uiDefinition = {
            Box {
                CheckBox(
                    checked = true,
                    onCheckedChange = null,
                    modifier = GlanceModifier.padding(5.dp, 6.dp, 7.dp, 8.dp))
            }
        }

        mHostRule.startHost()

        mHostRule.onUnboxedHostView<FrameLayout> { hostView ->
            assertThat(hostView.notGoneChildCount).isEqualTo(1)

            val checkboxRoot = hostView.notGoneChildren.single()
            assertThat(checkboxRoot.paddingStart).isEqualTo(5.dp.toPx())
            assertThat(checkboxRoot.paddingTop).isEqualTo(6.dp.toPx())
            assertThat(checkboxRoot.paddingEnd).isEqualTo(7.dp.toPx())
            assertThat(checkboxRoot.paddingBottom).isEqualTo(8.dp.toPx())
        }
    }

    private fun Dp.toPx() = toPixels(context.resources.displayMetrics)
}
