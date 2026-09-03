/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.collection.mutableScatterMapOf
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocal
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalFoundationStyleApi::class)
class StylePropertyTest {
    @Test
    fun can_create_and_use_int_property() {
        val intProperty = stylePropertyOf("intProperty") { 0 }
        scope {
            assertEquals(0, intProperty.value)
            intProperty.provide(10)
            assertEquals(10, intProperty.value)
        }
    }

    @Test
    fun can_animate_an_int_property() {
        val intProperty = stylePropertyOf("int", ::lerp) { 0 }
        val a = 0
        val b = 100
        val start = intProperty.lerp(a, b, 0f)
        assertEquals(a, start)
        val end = intProperty.lerp(a, b, 1f)
        assertEquals(b, end)
        val middle = intProperty.lerp(a, b, 0.5f)
        val tolerance = 1
        assertTrue(middle > 50 - tolerance && middle < 50 + tolerance)
    }

    @Test
    fun can_create_and_use_long_property() {
        val longProperty = stylePropertyOf("longProperty") { 0L }
        scope {
            assertEquals(0L, longProperty.value)
            longProperty.provide(10L)
            assertEquals(10L, longProperty.value)
        }
    }

    @Test
    fun can_animate_a_long_property() {
        val longProperty = stylePropertyOf("long", ::lerp) { 0L }
        val a = 0L
        val b = 100L
        val start = longProperty.lerp(a, b, 0f)
        assertEquals(a, start)
        val end = longProperty.lerp(a, b, 1f)
        assertEquals(b, end)
        val middle = longProperty.lerp(a, b, 0.5f)
        val tolerance = 1L
        assertTrue(middle > 50 - tolerance && middle < 50 + tolerance)
    }

    @Test
    fun can_create_and_use_float_property() {
        val floatProperty = stylePropertyOf("floatProperty") { 0f }
        scope {
            assertEquals(0f, floatProperty.value)
            floatProperty.provide(10f)
            assertEquals(10f, floatProperty.value)
        }
    }

    @Test
    fun can_animate_a_float_property() {
        val floatProperty = stylePropertyOf("float", ::lerp) { 0f }
        val a = 0f
        val b = 100f
        val start = floatProperty.lerp(a, b, 0f)
        assertEquals(a, start)
        val end = floatProperty.lerp(a, b, 1f)
        assertEquals(b, end)
        val middle = floatProperty.lerp(a, b, 0.5f)
        val tolerance = 1f
        assertTrue(middle > 50f - tolerance && middle < 50f + tolerance)
    }

    @Test
    fun can_animate_a_float_with_default_lerp() {
        val floatProperty = stylePropertyOf("test property") { 0f }
        val a = 0f
        val b = 100f
        val start = floatProperty.lerp(a, b, 0f)
        assertEquals(a, start)
        val end = floatProperty.lerp(a, b, 1f)
        assertEquals(b, end)
        val middle = floatProperty.lerp(a, b, 0.5f)
        val tolerance = 1f
        assertTrue(middle > 50f - tolerance && middle < 50f + tolerance)
    }

    @Test
    fun can_animate_a_dp_with_default_lerp() {
        val dpProperty = stylePropertyOf("test property") { 0.dp }
        val a = 0.dp
        val b = 100.dp
        val start = dpProperty.lerp(a, b, 0f)
        assertEquals(a, start)
        val end = dpProperty.lerp(a, b, 1f)
        assertEquals(b, end)
        val middle = dpProperty.lerp(a, b, 0.5f)
        val tolerance = 1.dp
        assertTrue(middle > 50.dp - tolerance && middle < 50.dp + tolerance)
    }

    @Test
    fun can_create_and_use_object_property() {
        val shapeProperty = stylePropertyOf("shape") { CircleShape as Shape }
        scope {
            assertEquals(CircleShape, shapeProperty.value)
            shapeProperty.provide(RectangleShape)
            assertEquals(RectangleShape, shapeProperty.value)
        }
    }

    @Test
    fun can_animate_an_shape_property() {
        val shapeProperty = stylePropertyOf("shape") { CircleShape as Shape }
        val a = CircleShape
        val b = RectangleShape

        val start = shapeProperty.lerp(a, b, 0f)
        assertTrue(a.isEquivalent(start))

        val end = shapeProperty.lerp(a, b, 1f)
        assertTrue(b.isEquivalent(end))

        val middle = shapeProperty.lerp(a, b, 0.5f)
        assertFalse(middle.isEquivalent(start))
        assertFalse(middle.isEquivalent(end))
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
internal interface SimpleScope : CommonStyleScope, StylePropertyAccessorScope

internal fun scope(block: SimpleScope.() -> Unit) {
    with(simpleScope()) { block() }
}

@OptIn(ExperimentalFoundationStyleApi::class)
internal fun simpleScope() =
    object : SimpleScope {
        val values = mutableScatterMapOf<StyleProperty<*>, Any?>()

        @Suppress("UNCHECKED_CAST")
        override val <T> StyleProperty<T>.value: T
            get() = values.getOrElse(this@value) { this@value.defaultValue() as Any } as T

        override val StyleProperty<*>.isSet: Boolean
            get() = this in values

        @Suppress("UNCHECKED_CAST")
        override fun <T> getOrNull(property: StyleProperty<T>): T? = values[property] as T?

        override fun anySet(properties: Set<StyleProperty<*>>): Boolean =
            values.any { property, _ ->
                property in values
            }

        override fun <T> ProvidableStyleProperty<T>.provide(value: T) {
            values[this] = value
        }

        override val density: Float
            get() = 100f

        override val fontScale: Float
            get() = 100f

        override val <T> CompositionLocal<T>.currentValue: T
            get() = error("Composition locals not supported")

        override fun animate(
            toSpec: AnimationSpec<Float>,
            fromSpec: AnimationSpec<Float>,
            block: () -> Unit,
        ) {
            block()
        }

        override val state: StyleState = MutableStyleState(null)

        override fun <T> state(
            key: StyleStateKey<T>,
            block: () -> Unit,
            active: (key: StyleStateKey<T>, state: StyleState) -> Boolean,
        ) {
            if (active(key, state)) block()
        }
    }

private fun Shape.toTestOutline(): Outline {
    val size = Size(1000f, 1000f)
    val density = Density(10f)
    return createOutline(size, LayoutDirection.Ltr, density)
}

private fun Shape.isEquivalent(other: Shape) =
    this == other || toTestOutline().isEquivalent(other.toTestOutline())

private fun Outline.isEquivalent(other: Outline): Boolean =
    this == other ||
        when (this) {
            is Outline.Rectangle ->
                when (other) {
                    is Outline.Rectangle -> rect == other.rect
                    is Outline.Rounded -> rect == other.bounds && other.isRectEquivalent()
                    is Outline.Generic -> false
                }
            is Outline.Rounded ->
                when (other) {
                    is Outline.Rectangle -> isRectEquivalent() && bounds == other.rect
                    is Outline.Rounded -> roundRect == other.roundRect
                    is Outline.Generic -> false
                }
            is Outline.Generic -> false
        }

private fun Outline.Rounded.isRectEquivalent() =
    with(roundRect) {
        bottomRightCornerRadius.isZero() &&
            bottomLeftCornerRadius.isZero() &&
            topRightCornerRadius.isZero() &&
            topLeftCornerRadius.isZero()
    }
