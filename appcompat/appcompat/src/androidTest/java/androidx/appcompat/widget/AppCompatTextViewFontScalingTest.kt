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

package androidx.appcompat.widget

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.test.R
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.AndroidFontScaleHelper.resetSystemFontScale
import androidx.testutils.AndroidFontScaleHelper.setSystemFontScale
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test {@link AppCompatTextView} under non-linear font scaling.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class AppCompatTextViewFontScalingTest {
    @get:Rule
    val scenarioRule = ActivityScenarioRule(TextViewFontScalingActivity::class.java)

    @After
    fun teardown() {
        // Have to manually check the version here because if we try to rely on the assumeTrue() in
        // resetSystemFontScale(), it is called twice (again in setSystemFontScale()) and the test
        // fails with a "TestCouldNotBeSkippedException: Test could not be skipped due to other
        // failures" because it thinks the second assumeTrue() was a separate error.
        // tl;dr avoids a bug in jUnit when multiple assumeTrue()s happen in @Test and @After
        if (Build.VERSION.SDK_INT >= 29) {
            resetSystemFontScale(scenarioRule.scenario)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNonLinearFontScaling_testSetLineHeightSpAndSetTextSizeSp() {
        setSystemFontScale(2f, scenarioRule.scenario)
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.resources.configuration.fontScale).isWithin(0.02f).of(2f)

            val textView = AppCompatTextView(activity)
            val textSizeSp = 20f
            val lineHeightSp = 40f

            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            textView.setLineHeight(TypedValue.COMPLEX_UNIT_SP, lineHeightSp)

            verifyLineHeightIsIntendedProportions(lineHeightSp, textSizeSp, activity, textView)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNonLinearFontScaling_overwriteXml_testSetLineHeightSpAndSetTextSizeSp() {
        setSystemFontScale(2f, scenarioRule.scenario)
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.resources.configuration.fontScale).isWithin(0.02f).of(2f)

            val textView = findTextView(activity, R.id.textview_lineheight2x)
            val textSizeSp = 20f
            val lineHeightSp = 40f

            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            textView.setLineHeight(TypedValue.COMPLEX_UNIT_SP, lineHeightSp)

            verifyLineHeightIsIntendedProportions(lineHeightSp, textSizeSp, activity, textView)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNonLinearFontScaling_xml_testLineHeightAttrSpAndTextSizeAttrSp() {
        setSystemFontScale(2f, scenarioRule.scenario)
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.resources.configuration.fontScale).isWithin(0.02f).of(2f)

            val textView = findTextView(activity, R.id.textview_lineheight2x)
            val textSizeSp = 20f
            val lineHeightSp = 40f

            verifyLineHeightIsIntendedProportions(lineHeightSp, textSizeSp, activity, textView)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNonLinearFontScaling_dimenXml_testLineHeightAttrSpAndTextSizeAttrSp() {
        setSystemFontScale(2f, scenarioRule.scenario)
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.resources.configuration.fontScale).isWithin(0.02f).of(2f)

            val textView = findTextView(activity, R.id.textview_lineheight_dimen3x)
            val textSizeSp = 20f
            val lineHeightSp = 60f

            verifyLineHeightIsIntendedProportions(lineHeightSp, textSizeSp, activity, textView)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNonLinearFontScaling_styleXml_testLineHeightAttrSpAndTextSizeAttrSp() {
        setSystemFontScale(2f, scenarioRule.scenario)
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.resources.configuration.fontScale).isWithin(0.02f).of(2f)

            val textView = findTextView(activity, R.id.textview_lineheight_style3x)
            val textSizeSp = 20f
            val lineHeightSp = 60f

            verifyLineHeightIsIntendedProportions(lineHeightSp, textSizeSp, activity, textView)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNonLinearFontScaling_dimenXml_testSetLineHeightSpAndTextSizeAttrSp() {
        setSystemFontScale(2f, scenarioRule.scenario)
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.resources.configuration.fontScale).isWithin(0.02f).of(2f)

            val textView = findTextView(activity, R.id.textview_lineheight_dimen3x)
            val textSizeSp = 20f
            val lineHeightSp = 30f

            textView.setLineHeight(TypedValue.COMPLEX_UNIT_SP, lineHeightSp)

            verifyLineHeightIsIntendedProportions(lineHeightSp, textSizeSp, activity, textView)
        }
    }

    private fun findTextView(activity: Activity, id: Int): AppCompatTextView {
        return activity.findViewById(id)!!
    }

    class TextViewFontScalingActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.appcompat_textview_fontscaling_activity)
        }
    }

    companion object {
        /**
         * Tolerance for comparing expected float lineHeight to the integer one returned by
         * getLineHeight(). It is pretty lenient to account for integer rounding when text size is
         * loaded from an attribute. (When loading an SP resource from an attribute for textSize,
         * it is rounded to the nearest pixel, which can throw off calculations quite a lot. Not
         * enough to make much of a difference to the user, but enough to need a wide tolerance in
         * tests. See b/279456702 for more details.)
         */
        private const val TOLERANCE = 5f

        private fun verifyLineHeightIsIntendedProportions(
            lineHeightSp: Float,
            textSizeSp: Float,
            activity: Activity,
            textView: TextView
        ) {
            val lineHeightMultiplier = lineHeightSp / textSizeSp
            // Calculate what line height would be without non-linear font scaling compressing it.
            // The trick is multiplying afterwards (by the pixel value) instead of before (by the SP
            // value)
            val expectedLineHeightPx = lineHeightMultiplier * TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp,
                activity.resources.displayMetrics
            )
            assertThat(textView.lineHeight.toFloat())
                .isWithin(TOLERANCE)
                .of(expectedLineHeightPx)
        }
    }
}
