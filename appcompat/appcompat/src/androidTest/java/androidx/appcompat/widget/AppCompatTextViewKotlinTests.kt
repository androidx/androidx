/*
 * Copyright 2024 The Android Open Source Project
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

import android.graphics.Typeface
import android.widget.TextView
import androidx.appcompat.test.R
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

// Note: the fact that this extends AppCompatBaseViewTest means that it will duplicate some tests
// from AppCompatTextViewTest; however, it is few enough relatively lightweight tests that it
// should be fine.
class AppCompatTextViewKotlinTests :
    AppCompatBaseViewTest<AppCompatTextViewActivity, AppCompatTextView>(
        AppCompatTextViewActivity::class.java
    ) {

    @Test
    fun setFontVariationSettings_sameSettings_doesNotCreateMultipleTypefaces() = runBlocking {
        val textView1: AppCompatTextView =
            mActivity.findViewById(R.id.textview_fontVariation_textView)
        val textView2: AppCompatTextView =
            mActivity.findViewById(R.id.textview_fontVariation_textView_2)
        assertThat(textView1.typeface).isSameInstanceAs(textView2.typeface)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getPaint_setTypeface_worksWith_setFontVariationSettings() =
        runBlocking(Dispatchers.Main) {
            // This is based on code that an app used that violates the contract of getPaint()
            // (i.e., that you shouldn't change the values), but that we want to provide best-effort
            // support for.
            val textView: AppCompatTextView =
                mActivity.findViewById(R.id.textview_fontresource_fontfamily_string_direct)
            // We do the same thing to a normal TextView to ensure we're accurately mimicking
            // platform behavior, which can be unintuitive (for example, setting the Typeface on
            // Paint will *not* change the result of TextView's getTypeface(), despite it having
            // changed).
            val plainTextView = TextView(mActivity)
            assertThat(textView.paint.typeface.systemFontFamilyName).isEqualTo("sans-serif")
            assertThat(textView.typeface!!.systemFontFamilyName).isEqualTo("sans-serif")

            // This is the thing the app did that caused the problem
            textView.paint.typeface = Typeface.create("cursive", Typeface.NORMAL)
            plainTextView.paint.typeface = Typeface.create("cursive", Typeface.NORMAL)

            assertThat(textView.paint.typeface.systemFontFamilyName).isEqualTo("cursive")
            assertThat(textView.typeface!!.systemFontFamilyName)
                .isEqualTo(plainTextView.typeface!!.systemFontFamilyName)

            // Perform the set, which needs to detect that it happened
            assertThat(textView.setFontVariationSettings("'wght' 200")).isTrue()

            assertThat(textView.paint.typeface.systemFontFamilyName).isEqualTo("cursive")
            assertThat(textView.typeface!!.systemFontFamilyName)
                .isEqualTo(plainTextView.typeface!!.systemFontFamilyName)
        }
}
