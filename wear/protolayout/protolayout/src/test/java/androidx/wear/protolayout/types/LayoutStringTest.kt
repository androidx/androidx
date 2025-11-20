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

package androidx.wear.protolayout.types

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutStringTest {
    @Test
    fun staticString_asLayoutString() {
        val prop: StringProp = STATIC_STRING.layoutString.prop

        assertThat(prop.value).isEqualTo(STATIC_STRING)
        assertThat(prop.dynamicValue).isNull()
    }

    @Test
    fun dynamicString_asLayoutString() {
        val layoutString: LayoutString =
            DYNAMIC_STRING.asLayoutString(
                staticValue = STATIC_STRING,
                layoutConstraint = PATTERN_STRING.asLayoutConstraint(TEXT_ALIGNMENT),
            )
        val prop: StringProp = layoutString.prop

        assertThat(layoutString.staticValue).isEqualTo(STATIC_STRING)
        assertThat(prop.value).isEqualTo(STATIC_STRING)
        assertThat(layoutString.dynamicValue?.toString()).isEqualTo(DYNAMIC_STRING.toString())
        assertThat(prop.dynamicValue.toString()).isEqualTo(DYNAMIC_STRING.toString())
        assertThat(layoutString.layoutConstraint).isEqualTo(LAYOUT_CONSTRAINT)
    }

    @Test
    fun dynamicString_withoutLayoutConstraint_asLayoutString() {
        val layoutString: LayoutString = DYNAMIC_STRING.asLayoutString(staticValue = STATIC_STRING)
        val prop: StringProp = layoutString.prop

        assertThat(layoutString.staticValue).isEqualTo(STATIC_STRING)
        assertThat(prop.value).isEqualTo(STATIC_STRING)
        assertThat(layoutString.dynamicValue?.toString()).isEqualTo(DYNAMIC_STRING.toString())
        assertThat(prop.dynamicValue.toString()).isEqualTo(DYNAMIC_STRING.toString())
        assertThat(layoutString.layoutConstraint).isNull()
    }

    companion object {
        private const val STATIC_STRING = "staticString"
        private const val PATTERN_STRING = "patternString"
        private const val TEXT_ALIGNMENT = LayoutElementBuilders.TEXT_ALIGN_END
        private val LAYOUT_CONSTRAINT =
            StringLayoutConstraint.Builder(PATTERN_STRING).setAlignment(TEXT_ALIGNMENT).build()
        private val DYNAMIC_STRING = DynamicString.constant("dynamicString")
    }
}
