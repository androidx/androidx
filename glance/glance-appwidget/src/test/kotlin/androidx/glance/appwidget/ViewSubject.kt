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

import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.core.view.children
import androidx.glance.layout.Alignment
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.robolectric.Shadows.shadowOf

internal open class ViewSubject(metaData: FailureMetadata, private val actual: View?) :
    Subject(metaData, actual) {
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
        fun views(): Factory<ViewSubject, View> {
            return Factory<ViewSubject, View> { metadata, actual -> ViewSubject(metadata, actual) }
        }

        fun assertThat(view: View?): ViewSubject = assertAbout(views()).that(view)
    }
}

internal open class TextViewSubject(metaData: FailureMetadata, private val actual: TextView?) :
    ViewSubject(metaData, actual) {
    fun hasTextColor(@ColorInt color: Int) {
        isNotNull()
        actual!!
        // Comparing the hex string representation is equivalent to comparing the int, and the
        // error message is a lot more readable with the hex string if this fails.
        check("getCurrentTextColor()")
            .that(Integer.toHexString(actual.currentTextColor))
            .isEqualTo(Integer.toHexString(color))
    }

    fun hasTextColor(hexString: String) = hasTextColor(android.graphics.Color.parseColor(hexString))

    companion object {
        fun textViews(): Factory<TextViewSubject, TextView> {
            return Factory<TextViewSubject, TextView> { metadata, actual ->
                TextViewSubject(metadata, actual)
            }
        }

        fun assertThat(view: TextView?): TextViewSubject = assertAbout(textViews()).that(view)
    }
}

internal open class ImageViewSubject(metaData: FailureMetadata, private val actual: ImageView?) :
    ViewSubject(metaData, actual) {
    fun hasColorFilter(@ColorInt color: Int) {
        assertNotNull(actual)
        val colorFilter = actual.colorFilter
        assertIs<PorterDuffColorFilter>(colorFilter)

        check("getColorFilter().getColor()")
            .that(Integer.toHexString(shadowOf(colorFilter).color))
            .isEqualTo(Integer.toHexString(color))
    }

    fun hasColorFilter(color: Color) {
        hasColorFilter(color.toArgb())
    }

    fun hasColorFilter(color: String) {
        hasColorFilter(android.graphics.Color.parseColor(color))
    }

    companion object {
        fun imageViews(): Factory<ImageViewSubject, ImageView> {
            return Factory<ImageViewSubject, ImageView> { metadata, actual ->
                ImageViewSubject(metadata, actual)
            }
        }

        fun assertThat(view: ImageView?): ImageViewSubject = assertAbout(imageViews()).that(view)
    }
}

internal open class FrameLayoutSubject(
    metaData: FailureMetadata,
    private val actual: FrameLayout?,
) : ViewSubject(metaData, actual) {
    fun hasContentAlignment(alignment: Alignment) {
        assertNotNull(actual)
        if (actual.childCount == 0) {
            return
        }
        check("children.getLayoutParams().gravity")
            .that(
                actual.children
                    .map { view -> assertIs<FrameLayout.LayoutParams>(view.layoutParams).gravity }
                    .toSet()
            )
            .containsExactly(alignment.toGravity())
    }

    companion object {
        fun frameLayouts(): Factory<FrameLayoutSubject, FrameLayout> {
            return Factory<FrameLayoutSubject, FrameLayout> { metadata, actual ->
                FrameLayoutSubject(metadata, actual)
            }
        }

        fun assertThat(view: FrameLayout?): FrameLayoutSubject =
            assertAbout(frameLayouts()).that(view)
    }
}

internal open class LinearLayoutSubject(
    metaData: FailureMetadata,
    private val actual: LinearLayout?,
) : ViewSubject(metaData, actual) {
    fun hasContentAlignment(alignment: Alignment.Vertical) {
        assertNotNull(actual)

        // On S+ the ViewStub child views aren't used for rows and columns, so the alignment is set
        // only on the outer layout.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            check("getGravity()")
                .that(actual.gravity and Gravity.VERTICAL_GRAVITY_MASK)
                .isEqualTo(alignment.toGravity())
            return
        }

        if (actual.orientation == LinearLayout.VERTICAL) {
            // LinearLayout.getGravity was introduced on Android N, prior to that, you could set the
            // gravity, but not read it back.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                check("getGravity()")
                    .that(actual.gravity and Gravity.VERTICAL_GRAVITY_MASK)
                    .isEqualTo(alignment.toGravity())
            }
            return
        }
        if (actual.childCount == 0) {
            return
        }
        check("children.getLayoutParams().gravity")
            .that(
                actual.children
                    .map { view ->
                        assertIs<LinearLayout.LayoutParams>(view.layoutParams).gravity and
                            Gravity.VERTICAL_GRAVITY_MASK
                    }
                    .toSet()
            )
            .containsExactly(alignment.toGravity())
    }

    fun hasContentAlignment(alignment: Alignment.Horizontal) {
        assertNotNull(actual)

        // On S+ the ViewStub child views aren't used for rows and columns, so the alignment is set
        // only on the outer layout.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            check("getGravity()")
                .that(actual.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
                .isEqualTo(alignment.toGravity())
            return
        }

        if (actual.orientation == LinearLayout.HORIZONTAL) {
            // LinearLayout.getGravity was introduced on Android N, prior to that, you could set the
            // gravity, but not read it back.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                check("getGravity()")
                    .that(actual.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
                    .isEqualTo(alignment.toGravity())
            }
            return
        }
        if (actual.childCount == 0) {
            return
        }
        check("children.getLayoutParams().gravity")
            .that(
                actual.children
                    .map { view ->
                        assertIs<LinearLayout.LayoutParams>(view.layoutParams).gravity and
                            Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK
                    }
                    .toSet()
            )
            .containsExactly(alignment.toGravity())
    }

    fun hasContentAlignment(alignment: Alignment) {
        hasContentAlignment(alignment.horizontal)
        hasContentAlignment(alignment.vertical)
    }

    companion object {
        fun linearLayouts(): Factory<LinearLayoutSubject, LinearLayout> {
            return Factory<LinearLayoutSubject, LinearLayout> { metadata, actual ->
                LinearLayoutSubject(metadata, actual)
            }
        }

        fun assertThat(view: LinearLayout?): LinearLayoutSubject =
            assertAbout(linearLayouts()).that(view)
    }
}

internal class ColorSubject(metaData: FailureMetadata, private val actual: Color?) :
    Subject(metaData, actual) {
    fun isSameColorAs(string: String) {
        isEqualTo(Color(android.graphics.Color.parseColor(string)))
    }

    fun isSameColorAs(color: Color) {
        isEqualTo(color)
    }

    override fun isEqualTo(expected: Any?) {
        assertThat(actual.toHexString()).isEqualTo(expected.toHexString())
    }

    override fun isNotEqualTo(unexpected: Any?) {
        assertThat(actual.toHexString()).isNotEqualTo(unexpected.toHexString())
    }

    private fun Any?.toHexString(): Any? = if (this is Color?) this.toHexString() else this

    private fun Color?.toHexString(): String? = this?.let { Integer.toHexString(it.toArgb()) }

    companion object {
        fun colors(): Factory<ColorSubject, Color> {
            return Factory<ColorSubject, Color> { metadata, actual ->
                ColorSubject(metadata, actual)
            }
        }

        fun assertThat(color: Color?): ColorSubject = assertAbout(colors()).that(color)
    }
}
