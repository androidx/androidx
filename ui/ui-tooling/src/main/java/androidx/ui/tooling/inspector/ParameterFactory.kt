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

import androidx.compose.foundation.Border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.CrossAxisAlignment
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableParameter
import androidx.ui.tooling.inspector.ParameterType.DimensionDp
import kotlin.math.abs

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

    var density = Density(1.0f)

    init {
        loadFromCompanion(AbsoluteAlignment.Companion)
        loadFromCompanion(Alignment.Companion)
        loadFromInterface(Arrangement::class.java)
        loadFromCompanion(CrossAxisAlignment.Companion)
        loadFromCompanion(FontFamily.Companion)
        loadFromCompanion(FontWeight.Companion, ignore = "getW")
        loadFromCompanion(Shadow.Companion)
        loadFromCompanion(TextDecoration.Companion)
        loadFromCompanion(TextIndent.Companion)
        valueLookup[Color.Unset] = "Unset"
    }

    /**
     * Create a [NodeParameter] from the specified parameter [name] and [value].
     *
     * Attempt to convert the value to a user readable value.
     * For now: return null when a conversion is not possible/found.
     */
    fun create(node: MutableInspectorNode, name: String, value: Any?): NodeParameter? {
        if (value == null) {
            return null
        }
        val text = valueLookup[value]
        if (text != null) {
            return NodeParameter(name, ParameterType.String, text)
        }
        return when (value) {
            is AnnotatedString -> NodeParameter(name, ParameterType.String, value.text)
            is BaselineShift -> createFromBaselineShift(name, value)
            is Boolean -> NodeParameter(name, ParameterType.Boolean, value)
            is Border -> createFromBorder(node, name, value)
            is Brush -> createFromBrush(name, value)
            is Color -> NodeParameter(name, ParameterType.Color, value.toArgb())
            is CornerBasedShape -> createFromCornerBasedShape(node, name, value)
            is CornerSize -> createFromCornerSize(node, name, value)
            is Double -> NodeParameter(name, ParameterType.Double, value)
            is Dp -> NodeParameter(name, DimensionDp, value.value)
            is Enum<*> -> NodeParameter(name, ParameterType.String, value.toString())
            is Float -> NodeParameter(name, ParameterType.Float, value)
            is FontListFontFamily -> createFromFontListFamily(name, value)
            is FontWeight -> NodeParameter(name, ParameterType.Int32, value.weight)
            is InnerPadding -> createFromInnerPadding(node, name, value)
            is Modifier -> createFromModifier(node, name, value)
            is InspectableParameter -> createFromInspectableParameter(node, name, value)
            is Int -> NodeParameter(name, ParameterType.Int32, value)
            is Locale -> NodeParameter(name, ParameterType.String, value.toString())
            is LocaleList -> NodeParameter(name, ParameterType.String,
                value.localeList.joinToString())
            is Long -> NodeParameter(name, ParameterType.Int64, value)
            is Offset -> createFromOffset(name, value)
            is Shadow -> createFromShadow(node, name, value)
            is Shape -> NodeParameter(name, ParameterType.String, Shape::class.java.simpleName)
            is String -> NodeParameter(name, ParameterType.String, value)
            is TextGeometricTransform -> createFromTextGeometricTransform(node, name, value)
            is TextIndent -> createFromTextIndent(node, name, value)
            is TextStyle -> createFromTextStyle(node, name, value)
            is TextUnit -> createFromTextUnit(name, value)
            else -> null
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

    private fun createFromBorder(
        node: MutableInspectorNode,
        name: String,
        value: Border
    ): NodeParameter {
        val parameter = NodeParameter(name, ParameterType.String, "Border")
        val elements = parameter.elements
        create(node, "size", value.size)?.let { elements.add(it) }
        create(node, "brush", value.brush)?.let { elements.add(it) }
        return parameter
    }

    private fun createFromBrush(name: String, value: Brush): NodeParameter =
        when (value) {
            is SolidColor -> NodeParameter(name, ParameterType.Color, value.value.toArgb())
            else -> NodeParameter(name, ParameterType.String, classNameOf(value, Brush::class.java))
        }

    private fun createFromCornerBasedShape(
        node: MutableInspectorNode,
        name: String,
        value: CornerBasedShape
    ): NodeParameter? {
        val parameter = NodeParameter(name, ParameterType.String,
            classNameOf(value, CornerBasedShape::class.java))
        val elements = parameter.elements
        create(node, "topLeft", value.topLeft)?.let { elements.add(it) }
        create(node, "topRight", value.topRight)?.let { elements.add(it) }
        create(node, "bottomLeft", value.bottomLeft)?.let { elements.add(it) }
        create(node, "bottomRight", value.bottomRight)?.let { elements.add(it) }
        return parameter
    }

    private fun createFromCornerSize(
        node: MutableInspectorNode,
        name: String,
        value: CornerSize
    ): NodeParameter {
        val size = Size(node.width.toFloat(), node.height.toFloat())
        val pixels = value.toPx(size, density)
        return NodeParameter(name, DimensionDp, with (density) { pixels.toDp().value })
    }

    // For now: select ResourceFontFont closest to W400 and Normal, and return the resId
    private fun createFromFontListFamily(name: String, value: FontListFontFamily): NodeParameter? =
        findBestResourceFont(value)?.let {
            return NodeParameter(name, ParameterType.Resource, it.resId)
        }

    private fun createFromInnerPadding(
        node: MutableInspectorNode,
        name: String,
        value: InnerPadding
    ): NodeParameter {
        val parameter = NodeParameter(name, ParameterType.String, "InnerPadding")
        val elements = parameter.elements
        create(node, "start", value.start)?.let { elements.add(it) }
        create(node, "end", value.end)?.let { elements.add(it) }
        create(node, "top", value.top)?.let { elements.add(it) }
        create(node, "bottom", value.bottom)?.let { elements.add(it) }
        return parameter
    }

    private fun createFromInspectableParameter(
        node: MutableInspectorNode,
        name: String,
        value: InspectableParameter
    ): NodeParameter {
        val tempValue = value.valueOverride ?: ""
        val parameterName = name.ifEmpty { value.nameFallback } ?: "element"
        val parameterValue = if (tempValue is InspectableParameter) "" else tempValue
        val parameter = create(node, parameterName, parameterValue)
            ?: NodeParameter(parameterName, ParameterType.String, "")
        val elements = parameter.elements
        value.inspectableElements.mapNotNullTo(elements) { create(node, it.name, it.value) }
        return parameter
    }

    private fun createFromModifier(
        node: MutableInspectorNode,
        name: String,
        value: Modifier
    ): NodeParameter? =
        when {
            name.isNotEmpty() -> {
                val parameter = NodeParameter(name, ParameterType.String, "")
                val elements = parameter.elements
                value.foldIn(elements) { acc, m ->
                    create(node, "", m)?.let { param -> acc.apply { add(param) } } ?: acc
                }
                parameter
            }
            value is InspectableParameter -> createFromInspectableParameter(node, name, value)
            else -> null
        }

    private fun createFromOffset(name: String, value: Offset): NodeParameter {
        val parameter = NodeParameter(name, ParameterType.String, Offset::class.java.simpleName)
        val elements = parameter.elements
        elements.add(NodeParameter("x", DimensionDp, with(density) { value.x.toDp().value }))
        elements.add(NodeParameter("y", DimensionDp, with(density) { value.y.toDp().value }))
        return parameter
    }

    private fun createFromShadow(
        node: MutableInspectorNode,
        name: String,
        value: Shadow
    ): NodeParameter {
        val parameter = NodeParameter(name, ParameterType.String, Shadow::class.java.simpleName)
        val elements = parameter.elements
        val blurRadius = with(density) { value.blurRadius.toDp().value }
        create(node, "color", value.color)?.let { elements.add(it) }
        create(node, "offset", value.offset)?.let { elements.add(it) }
        elements.add(NodeParameter("blurRadius", DimensionDp, blurRadius))
        return parameter
    }

    private fun createFromTextGeometricTransform(
        node: MutableInspectorNode,
        name: String,
        value: TextGeometricTransform
    ): NodeParameter {
        val parameter = NodeParameter(name, ParameterType.String,
            TextGeometricTransform::class.java.simpleName)
        val elements = parameter.elements
        create(node, "scaleX", value.scaleX)?.let { elements.add(it) }
        create(node, "skewX", value.skewX)?.let { elements.add(it) }
        return parameter
    }

    private fun createFromTextIndent(
        node: MutableInspectorNode,
        name: String,
        value: TextIndent
    ): NodeParameter {
        val parameter = NodeParameter(name, ParameterType.String, TextIndent::class.java.simpleName)
        val elements = parameter.elements
        create(node, "firstLine", value.firstLine)?.let { elements.add(it) }
        create(node, "restLine", value.restLine)?.let { elements.add(it) }
        return parameter
    }

    private fun createFromTextStyle(
        node: MutableInspectorNode,
        name: String,
        value: TextStyle
    ): NodeParameter {
        val parameter = NodeParameter(name, ParameterType.String, TextStyle::class.java.simpleName)
        val elements = parameter.elements
        create(node, "color", value.color)?.let { elements.add(it) }
        create(node, "fontSize", value.fontSize)?.let { elements.add(it) }
        create(node, "fontWeight", value.fontWeight)?.let { elements.add(it) }
        create(node, "fontStyle", value.fontStyle)?.let { elements.add(it) }
        create(node, "fontSynthesis", value.fontSynthesis)?.let { elements.add(it) }
        create(node, "fontFamily", value.fontFamily)?.let { elements.add(it) }
        create(node, "fontFeatureSettings", value.fontFeatureSettings)?.let { elements.add(it) }
        create(node, "letterSpacing", value.letterSpacing)?.let { elements.add(it) }
        create(node, "baselineShift", value.baselineShift)?.let { elements.add(it) }
        create(node, "textGeometricTransform", value.textGeometricTransform)
            ?.let { elements.add(it) }
        create(node, "localeList", value.localeList)?.let { elements.add(it) }
        create(node, "background", value.background)?.let { elements.add(it) }
        create(node, "textDecoration", value.textDecoration)?.let { elements.add(it) }
        create(node, "shadow", value.shadow)?.let { elements.add(it) }
        create(node, "textAlign", value.textAlign)?.let { elements.add(it) }
        create(node, "textDirection", value.textDirection)?.let { elements.add(it) }
        create(node, "lineHeight", value.lineHeight)?.let { elements.add(it) }
        create(node, "textIndent", value.textIndent)?.let { elements.add(it) }
        return parameter
    }

    private fun createFromTextUnit(name: String, value: TextUnit): NodeParameter? =
        when (value.type) {
            TextUnitType.Sp -> NodeParameter(name, ParameterType.DimensionSp, value.value)
            TextUnitType.Em -> NodeParameter(name, ParameterType.DimensionEm, value.value)
            TextUnitType.Inherit -> NodeParameter(name, ParameterType.String, "Inherit")
        }

    private fun classNameOf(value: Any, default: Class<*>): String =
        value.javaClass.simpleName.ifEmpty { default.simpleName }

    /**
     * Select a resource font among the font in the family to represent the font
     *
     * Prefer the font closest to [FontWeight.Normal] and [FontStyle.Normal]
     */
    private fun findBestResourceFont(value: FontListFontFamily): ResourceFont? =
        value.fonts.asSequence().filterIsInstance<ResourceFont>().minByOrNull {
            abs(it.weight.weight - FontWeight.Normal.weight) + it.style.ordinal
        }

    private fun loadFromInterface(interfaceClass: Class<*>) {
        // REDO: If we decide to add a kotlin reflection dependency
        interfaceClass.classes
            .flatMap { it.fields.asIterable() }
            .filter { it.name == "INSTANCE" }
            .associateByTo(valueLookup, { it[null]!! }, { it.declaringClass.simpleName })
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
}
