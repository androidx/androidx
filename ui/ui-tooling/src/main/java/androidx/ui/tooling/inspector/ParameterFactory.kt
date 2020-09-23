/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.tooling.inspector

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.ui.tooling.inspector.ParameterType.DimensionDp
import kotlin.math.abs
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private const val MAX_RECURSIONS = 10
private const val MAX_ITERABLE = 25

/**
 * Factory of [NodeParameter]s.
 *
 * Each parameter value is converted to a user readable value.
 */
internal class ParameterFactory {
    /**
     * A map from known values to a user readable string representation.
     */
    private val valueLookup = mutableMapOf<Any, String>()
    private var creatorCache: ParameterCreator? = null
    private val kotlinReflectionSupported = try {
        Class.forName("kotlin.reflect.full.KClasses")
        true
    } catch (ex: Exception) {
        false
    }

    var density = Density(1.0f)

    init {
        val textDecorationCombination = TextDecoration.combine(
            listOf(TextDecoration.LineThrough, TextDecoration.Underline))
        loadFromCompanion(AbsoluteAlignment.Companion)
        loadFromCompanion(Alignment.Companion)
        loadFromInterface(Arrangement::class.java)
        loadFromCompanion(FontFamily.Companion)
        loadFromCompanion(FontWeight.Companion, ignore = "getW")
        loadFromCompanion(Shadow.Companion)
        loadFromCompanion(TextDecoration.Companion)
        loadFromCompanion(TextIndent.Companion)
        valueLookup[Color.Unset] = "Unset"
        valueLookup[RectangleShape] = "RectangleShape"
        valueLookup[textDecorationCombination] = "LineThrough+Underline"
    }

    /**
     * Create a [NodeParameter] from the specified parameter [name] and [value].
     *
     * Attempt to convert the value to a user readable value.
     * For now: return null when a conversion is not possible/found.
     */
    fun create(node: MutableInspectorNode, name: String, value: Any?): NodeParameter? {
        val creator = creatorCache ?: ParameterCreator()
        try {
            return creator.create(node, name, value)
        } finally {
            creatorCache = creator
        }
    }

    private fun loadFromInterface(interfaceClass: Class<*>) {
        // REDO: If we decide to add a kotlin reflection dependency
        interfaceClass.declaredFields
            .filter { it.name != "INSTANCE" }
            .associateByTo(valueLookup, { it.isAccessible = true; it[null]!! }, { it.name })
    }

    private fun loadFromCompanion(companionInstance: Any, ignore: String? = null) {
        // REDO: If we decide to add a kotlin reflection dependency
        companionInstance::class.java.declaredMethods.asSequence()
            .filter {
                java.lang.reflect.Modifier.isPublic(it.modifiers) &&
                        it.returnType != Void.TYPE &&
                        it.parameterTypes.isEmpty() &&
                        it.name.startsWith("get") &&
                        (ignore == null || !it.name.startsWith(ignore))
            }
            .associateByTo(valueLookup, { it(companionInstance)!! }, { it.name.substring(3) })
    }

    /**
     * Convenience class for building [NodeParameter]s.
     */
    private inner class ParameterCreator {
        private var node: MutableInspectorNode? = null
        private var recursions = 0

        fun create(node: MutableInspectorNode, name: String, value: Any?): NodeParameter? =
            try {
                this.node = node
                recursions = 0
                create(name, value)
            } finally {
                this.node = null
            }

        private fun create(name: String, value: Any?): NodeParameter? {
            if (value == null || recursions >= MAX_RECURSIONS) {
                return null
            }
            try {
                recursions++
                val text = valueLookup[value]
                if (text != null) {
                    return NodeParameter(name, ParameterType.String, text)
                }
                return when (value) {
                    is AnnotatedString -> NodeParameter(name, ParameterType.String, value.text)
                    is BaselineShift -> createFromBaselineShift(name, value)
                    is Boolean -> NodeParameter(name, ParameterType.Boolean, value)
                    is Color -> NodeParameter(name, ParameterType.Color, value.toArgb())
                    is CornerSize -> createFromCornerSize(name, value)
                    is Double -> NodeParameter(name, ParameterType.Double, value)
                    is Dp -> NodeParameter(name, DimensionDp, value.value)
                    is Enum<*> -> NodeParameter(name, ParameterType.String, value.toString())
                    is Float -> NodeParameter(name, ParameterType.Float, value)
                    is FontListFontFamily -> createFromFontListFamily(name, value)
                    is FontWeight -> NodeParameter(name, ParameterType.Int32, value.weight)
                    is Modifier -> createFromModifier(name, value)
                    is InspectableValue -> createFromInspectableValue(name, value)
                    is Int -> NodeParameter(name, ParameterType.Int32, value)
                    is Iterable<*> -> createFromIterable(name, value)
                    is Locale -> NodeParameter(name, ParameterType.String, value.toString())
                    is LocaleList ->
                        NodeParameter(name, ParameterType.String, value.localeList.joinToString())
                    is Long -> NodeParameter(name, ParameterType.Int64, value)
                    is Offset -> createFromOffset(name, value)
                    is Shadow -> createFromShadow(name, value)
                    is SolidColor -> NodeParameter(name, ParameterType.Color, value.value.toArgb())
                    is String -> NodeParameter(name, ParameterType.String, value)
                    is TextUnit -> createFromTextUnit(name, value)
                    else -> createFromKotlinReflection(name, value)
                }
            } finally {
                recursions--
            }
        }

        private fun createFromBaselineShift(name: String, value: BaselineShift): NodeParameter {
            val converted = when (value.multiplier) {
                BaselineShift.None.multiplier -> "None"
                BaselineShift.Subscript.multiplier -> "Subscript"
                BaselineShift.Superscript.multiplier -> "Superscript"
                else -> return NodeParameter(name, ParameterType.Float, value.multiplier)
            }
            return NodeParameter(name, ParameterType.String, converted)
        }

        private fun createFromCornerSize(name: String, value: CornerSize): NodeParameter {
            val size = Size(node!!.width.toFloat(), node!!.height.toFloat())
            val pixels = value.toPx(size, density)
            return NodeParameter(name, DimensionDp, with(density) { pixels.toDp().value })
        }

        // For now: select ResourceFontFont closest to W400 and Normal, and return the resId
        private fun createFromFontListFamily(
            name: String,
            value: FontListFontFamily
        ): NodeParameter? =
            findBestResourceFont(value)?.let {
                NodeParameter(name, ParameterType.Resource, it.resId)
            }

        private fun createFromKotlinReflection(
            name: String,
            value: Any
        ): NodeParameter? {
            val kClass = value::class
            if (kClass.simpleName == null || !kotlinReflectionSupported) {
                // internal synthetic class or kotlin reflection library not available
                return null
            }
            val parameter = NodeParameter(name, ParameterType.String, kClass.simpleName)
            val properties = mutableMapOf<String, KProperty1<Any, *>>()
            try {
                sequenceOf(kClass).plus(kClass.allSuperclasses.asSequence())
                    .flatMap { it.declaredMemberProperties.asSequence() }
                    .filterIsInstance<KProperty1<Any, *>>()
                    .associateByTo(properties) { it.name }
            } catch (ex: Throwable) {
                Log.w("Compose", "Could not decompose ${kClass.simpleName}")
                return parameter
            }
            properties.values.mapNotNullTo(parameter.elements) {
                create(it.name, valueOf(it, value))
            }
            return parameter
        }

        private fun valueOf(property: KProperty1<Any, *>, instance: Any): Any? =
            try {
                property.isAccessible = true
                // Bug in kotlin reflection API: if the type is a nullable inline type with a null
                // value, we get an IllegalArgumentException in this line:
                property.get(instance)
            } catch (ex: Throwable) {
                // TODO: Remove this warning since this is expected with nullable inline types
                Log.w("Compose", "Could not get value of ${property.name}")
                null
            }

        private fun createFromInspectableValue(
            name: String,
            value: InspectableValue
        ): NodeParameter {
            val tempValue = value.valueOverride ?: ""
            val parameterName = name.ifEmpty { value.nameFallback } ?: "element"
            val parameterValue = if (tempValue is InspectableValue) "" else tempValue
            val parameter = create(parameterName, parameterValue)
                ?: NodeParameter(parameterName, ParameterType.String, "")
            val elements = parameter.elements
            value.inspectableElements.mapNotNullTo(elements) { create(it.name, it.value) }
            return parameter
        }

        private fun createFromIterable(name: String, value: Iterable<*>): NodeParameter? {
            val parameter = NodeParameter(name, ParameterType.String, "")
            val elements = parameter.elements
            value.asSequence()
                .mapNotNull { create(elements.size.toString(), it) }
                .takeWhile { elements.size < MAX_ITERABLE }
                .toCollection(elements)
            return parameter
        }

        private fun createFromModifier(name: String, value: Modifier): NodeParameter? =
            when {
                name.isNotEmpty() -> {
                    val parameter = NodeParameter(name, ParameterType.String, "")
                    val elements = parameter.elements
                    value.foldIn(elements) { acc, m ->
                        create("", m)?.let { param -> acc.apply { add(param) } } ?: acc
                    }
                    parameter
                }
                value is InspectableValue -> createFromInspectableValue(name, value)
                else -> null
            }

        private fun createFromOffset(name: String, value: Offset): NodeParameter {
            val parameter = NodeParameter(name, ParameterType.String, Offset::class.java.simpleName)
            val elements = parameter.elements
            elements.add(NodeParameter("x", DimensionDp, with(density) { value.x.toDp().value }))
            elements.add(NodeParameter("y", DimensionDp, with(density) { value.y.toDp().value }))
            return parameter
        }

        // Special handling of blurRadius: convert to dp:
        private fun createFromShadow(name: String, value: Shadow): NodeParameter? {
            val parameter = createFromKotlinReflection(name, value) ?: return null
            val elements = parameter.elements
            val index = elements.indexOfFirst { it.name == "blurRadius" }
            if (index >= 0) {
                val blurRadius = with(density) { value.blurRadius.toDp().value }
                elements[index] = NodeParameter("blurRadius", DimensionDp, blurRadius)
            }
            return parameter
        }

        private fun createFromTextUnit(name: String, value: TextUnit): NodeParameter? =
            when (value.type) {
                TextUnitType.Sp -> NodeParameter(name, ParameterType.DimensionSp, value.value)
                TextUnitType.Em -> NodeParameter(name, ParameterType.DimensionEm, value.value)
                TextUnitType.Inherit -> NodeParameter(name, ParameterType.String, "Inherit")
            }

        /**
         * Select a resource font among the font in the family to represent the font
         *
         * Prefer the font closest to [FontWeight.Normal] and [FontStyle.Normal]
         */
        private fun findBestResourceFont(value: FontListFontFamily): ResourceFont? =
            value.fonts.asSequence().filterIsInstance<ResourceFont>().minByOrNull {
                abs(it.weight.weight - FontWeight.Normal.weight) + it.style.ordinal
            }
    }
}
