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

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.compose.ui.unit.Dp
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import kotlin.test.assertNotNull

internal class ViewSubject<V : View>(
    metaData: FailureMetadata,
    private val actual: V?
) : Subject(metaData, actual) {
    fun hasBackgroundColor(@ColorInt color: Int) {
        isNotNull()
        actual!!
        check("getBackground()").that(actual.background).isInstanceOf(ColorDrawable::class.java)
        val background = actual.background as ColorDrawable
        // Comparing the hex string representation is equivalent to comparing the int, and the
        // error message is a lot more readable with the hex string if this fails.
        check("getBackground().getColor()")
            .that(Integer.toHexString(background.color))
            .isEqualTo(Integer.toHexString(color))
    }

    fun hasBackgroundColor(hexString: String) =
        hasBackgroundColor(android.graphics.Color.parseColor(hexString))

    fun hasLayoutParamsWidth(@Px px: Int) {
        check("getLayoutParams().width").that(actual?.layoutParams?.width).isEqualTo(px)
    }

    fun hasLayoutParamsWidth(dp: Dp) {
        assertNotNull(actual)
        hasLayoutParamsWidth(dp.toPixels(actual.context))
    }

    fun hasLayoutParamsHeight(@Px px: Int) {
        check("getLayoutParams().height").that(actual?.layoutParams?.height).isEqualTo(px)
    }

    fun hasLayoutParamsHeight(dp: Dp) {
        assertNotNull(actual)
        hasLayoutParamsHeight(dp.toPixels(actual.context))
    }

    companion object {
        internal fun <V : View> views(): Factory<ViewSubject<V>, V> {
            return Factory<ViewSubject<V>, V> { metadata, actual -> ViewSubject(metadata, actual) }
        }

        internal fun <V : View> assertThat(view: V) = assertAbout(views()).that(view)
    }
}
