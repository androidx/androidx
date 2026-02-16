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

package androidx.compose.remote.core.documentation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.core.operations.ClickArea
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.ColorConstant
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DataDynamicListFloat
import androidx.compose.remote.core.operations.DataListFloat
import androidx.compose.remote.core.operations.DataListIds
import androidx.compose.remote.core.operations.DataMapIds
import androidx.compose.remote.core.operations.DataMapLookup
import androidx.compose.remote.core.operations.DebugMessage
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawBitmap
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapInt
import androidx.compose.remote.core.operations.DrawBitmapScaled
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawContent
import androidx.compose.remote.core.operations.DrawLine
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.DrawSector
import androidx.compose.remote.core.operations.DrawText
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FontData
import androidx.compose.remote.core.operations.HapticFeedback
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.IdLookup
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.MatrixRestore
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.MatrixSave
import androidx.compose.remote.core.operations.MatrixScale
import androidx.compose.remote.core.operations.MatrixSkew
import androidx.compose.remote.core.operations.MatrixTranslate
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.ParticlesCompare
import androidx.compose.remote.core.operations.ParticlesCreate
import androidx.compose.remote.core.operations.ParticlesLoop
import androidx.compose.remote.core.operations.PathAppend
import androidx.compose.remote.core.operations.PathCombine
import androidx.compose.remote.core.operations.PathCreate
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.Rem
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.core.operations.RootContentDescription
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TextLength
import androidx.compose.remote.core.operations.TextLookup
import androidx.compose.remote.core.operations.TextLookupInt
import androidx.compose.remote.core.operations.TextMeasure
import androidx.compose.remote.core.operations.TextMerge
import androidx.compose.remote.core.operations.TextSubtext
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.UpdateDynamicFloatList
import androidx.compose.remote.core.operations.WakeIn
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.ComponentStart
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.ImpulseProcess
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.LayoutComputeOperation
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RunActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.ValueStringChangeActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ZIndexModifierOperation
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.core.types.BooleanConstant
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.remote.core.types.LongConstant
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RemoteComposeDocumentation(val title: String, val intro: String) : DocumentationBuilder {
    var buffer = StringBuilder(preamble())

    fun role(content: String) {
        buffer.append("\n")
        buffer.append(content)
        buffer.append("\n\n")
    }

    fun preamble(): String {
        return """
            <meta charset="utf-8" lang="en"><style class="fallback">
            body{visibility:hidden;}
            table {
              border-collapse: collapse;
            }

            .interpolation td {
              border: 0px solid #888;
              margin: 0;
              padding: 0.5em;
            }

            td {
              border: 1px solid #888;
              margin: 0;
              padding: 0.5em;
            }
            th {
              background-color: #AAA;
              color: #FFF;
              border: 1px solid #888;
              margin: 0;
              padding: 0.5em;
              text-align: left;
            }

            tr:nth-child(even) {
              background-color: #EEE;
            }

            .md h1, .md .nonumberh1 {page-break-before:always}

            @media screen and (min-width: 110em) {
	        .md .longTOC, .md .mediumTOC, .md .shortTOC {
		      max-width: 50%;
		      width: 25%;
              max-height: 100%;
		      float: left;
		      position: fixed;
		      left: 20px;
		      top: 20px;
              overflow-y: auto;
	        }

            .md table {
                margin-left: 0 !important;
                margin-right: auto !important;
            }
            
            .md canvas {
                display: block !important;
                margin-left: auto !important;
                margin-right: auto !important;
                padding-bottom: 20px; /* Optional: adds some breathing room */
            }
}
            </style>
        """
    }

    fun postamble(): String {
        return "<!-- Markdeep: --><style class=\"fallback\">body{visibility:hidden;white-space:pre;font-family:monospace}</style><script src=\"markdeep.min.js\" charset=\"utf-8\"></script><script src=\"https://morgan3d.github.io/markdeep/latest/markdeep.min.js\" charset=\"utf-8\"></script><script>window.alreadyProcessedMarkdeep||(document.body.style.visibility=\"visible\")</script>\n"
    }

    /*
      fun createInterpolatorImage(
        interpolationMethod: CompanionInterpolationMethod,
        name: String = interpolationMethod.name(),
      ) {
        val size = 220
        val margin = 50
        val sizeBox = size - 2 * margin
        val segments = 40
        val factor = sizeBox / 4f

        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.background = Color.red
        g2d.color = Color.white
        g2d.fillRect(0, 0, size, size)

        g2d.color = Color.lightGray
        for (i in 1 until 4) {
          val n = margin + (i * factor).toInt()
          g2d.drawLine(n, margin, n, margin + sizeBox)
          g2d.drawLine(margin, n, margin + sizeBox, n)
        }

        g2d.color = Color.black
        g2d.drawString("0", margin - 10, size - margin + 14)
        g2d.drawString("1", size - margin, size - margin + 14)
        g2d.drawString("1", margin - 10, margin - 2)
        g2d.drawString("f(t)", margin + 52, size - margin + 26)
        g2d.drawRect(margin, margin, sizeBox, sizeBox)

        g2d.color = Color.blue
        var progress = 0f
        val increment = 1f / segments.toFloat()
        val sx = sizeBox / segments.toFloat()
        var prex = margin.toFloat()
        var prey = (margin + sizeBox).toFloat()
        g2d.stroke = BasicStroke(2f)
        for (x in 0..segments) {
          val dx = margin + (x * sx)
          val dy = (size - margin - interpolationMethod.evaluate(progress) * sizeBox)
          g2d.drawLine(prex.toInt(), prey.toInt(), dx.toInt(), dy.toInt())
          progress += increment
          prex = dx
          prey = dy
        }

        g2d.dispose()
        ImageIO.write(image, "png", File("Documentation/images/$name.png"))
      }
    */

    /*
    fun createLayoutImage(
          layout: LayoutManager,
          name: String,
          width: Int = layout.width.toInt(),
          height: Int = layout.height.toInt(),
          drawBoundsOver: Boolean = false,
    ) {
      val component1 = Component(1, 0f, 0f, 100f, 100f, null)
      val component2 = Component(2, 0f, 0f, 100f, 100f, null)
      val component3 = Component(3, 0f, 0f, 100f, 100f, null)
      val components = arrayListOf(component1, component2, component3)
      val context = DebugPaintContext()
      val image = BufferedImage(width + 1, height + 1, BufferedImage.TYPE_INT_RGB)
      val g2d = image.createGraphics()
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2d.color = Color.white
      g2d.fillRect(0, 0, width + 1, height + 1)
      val basicStroke = BasicStroke(2f)
      val dashedStroke =
        BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(9f), 0f)

      g2d.stroke = basicStroke
      g2d.color = Color.lightGray
      g2d.drawRect(0, 0, layout.width.toInt(), layout.height.toInt())

      val measurePass = MeasurePass()
      layout.childrenComponents.clear()
      layout.childrenComponents.addAll(components)
      layout.measure(
        context,
        layout.layoutInfo,
        layout.width,
        layout.width,
        layout.height,
        layout.height,
        measurePass,
      )
      layout.layout(context, measurePass)
      for (component in components) {
        if (component.visibility == Component.Visibility.VISIBLE) {
          g2d.color = Color.lightGray
          g2d.fillRoundRect(
            component.x.toInt(),
            component.y.toInt(),
            component.width.toInt(),
            component.height.toInt(),
            32,
            32,
          )
          g2d.color = Color.darkGray
          g2d.drawRoundRect(
            component.x.toInt(),
            component.y.toInt(),
            component.width.toInt(),
            component.height.toInt(),
            32,
            32,
          )
        } else {
          g2d.color = Color.darkGray
          g2d.stroke = dashedStroke
          g2d.color = Color.darkGray
          g2d.drawRoundRect(
            component.x.toInt(),
            component.y.toInt(),
            component.width.toInt(),
            component.height.toInt(),
            32,
            32,
          )
          g2d.stroke = basicStroke
        }
      }
      if (drawBoundsOver) {
        g2d.stroke = basicStroke
        g2d.color = Color.blue
        g2d.drawRect(0, 0, layout.width.toInt(), layout.height.toInt())
        // FIXME
        // layout.layout(context, layout.layoutInfo, components)
      }
      g2d.dispose()
      ImageIO.write(image, "png", File("Documentation/images/layout-$name.png"))
    }
    */

    val operationsMap = HashMap<Int, DocumentedCompanionOperation>()

    fun listOperations() {
        // Protocol & Metadata
        operationsMap.put(Operations.HEADER, Header::documentation)
        operationsMap.put(Operations.THEME, Theme::documentation)
        operationsMap.put(
            Operations.ROOT_CONTENT_DESCRIPTION,
            RootContentDescription::documentation,
        )
        operationsMap.put(Operations.ROOT_CONTENT_BEHAVIOR, RootContentBehavior::documentation)
        operationsMap.put(Operations.DEBUG_MESSAGE, DebugMessage::documentation)
        operationsMap.put(Operations.WAKE_IN, WakeIn::documentation)
        operationsMap.put(Operations.REM, Rem::documentation)
        // Data & Constants
        operationsMap.put(Operations.DATA_FLOAT, FloatConstant::documentation)
        operationsMap.put(Operations.DATA_INT, IntegerConstant::documentation)
        operationsMap.put(Operations.DATA_BOOLEAN, BooleanConstant::documentation)
        operationsMap.put(Operations.DATA_LONG, LongConstant::documentation)
        operationsMap.put(Operations.NAMED_VARIABLE, NamedVariable::documentation)
        operationsMap.put(Operations.ID_MAP, DataMapIds::documentation)
        operationsMap.put(Operations.ID_LIST, DataListIds::documentation)
        operationsMap.put(Operations.FLOAT_LIST, DataListFloat::documentation)
        operationsMap.put(Operations.DYNAMIC_FLOAT_LIST, DataDynamicListFloat::documentation)
        operationsMap.put(Operations.DATA_MAP_LOOKUP, DataMapLookup::documentation)
        operationsMap.put(
            Operations.UPDATE_DYNAMIC_FLOAT_LIST,
            UpdateDynamicFloatList::documentation,
        )
        operationsMap.put(Operations.DATA_BITMAP, BitmapData::documentation)
        operationsMap.put(Operations.DATA_FONT, FontData::documentation)
        operationsMap.put(Operations.DATA_BITMAP_FONT, BitmapFontData::documentation)
        operationsMap.put(Operations.DATA_TEXT, TextData::documentation)
        // Canvas Operations
        operationsMap.put(Operations.DRAW_RECT, DrawRect::documentation)
        operationsMap.put(Operations.DRAW_ROUND_RECT, DrawRoundRect::documentation)
        operationsMap.put(Operations.DRAW_CIRCLE, DrawCircle::documentation)
        operationsMap.put(Operations.DRAW_OVAL, DrawOval::documentation)
        operationsMap.put(Operations.DRAW_ARC, DrawArc::documentation)
        operationsMap.put(Operations.DRAW_SECTOR, DrawSector::documentation)
        operationsMap.put(Operations.DRAW_LINE, DrawLine::documentation)
        operationsMap.put(Operations.DRAW_PATH, DrawPath::documentation)
        operationsMap.put(Operations.DRAW_TWEEN_PATH, DrawTweenPath::documentation)
        operationsMap.put(Operations.PATH_CREATE, PathCreate::documentation)
        operationsMap.put(Operations.PATH_ADD, PathAppend::documentation)
        operationsMap.put(Operations.PATH_COMBINE, PathCombine::documentation)
        operationsMap.put(Operations.PATH_TWEEN, PathTween::documentation)
        operationsMap.put(Operations.DRAW_BITMAP, DrawBitmap::documentation)
        operationsMap.put(Operations.DRAW_BITMAP_INT, DrawBitmapInt::documentation)
        operationsMap.put(Operations.DRAW_BITMAP_SCALED, DrawBitmapScaled::documentation)
        operationsMap.put(Operations.DRAW_TO_BITMAP, DrawToBitmap::documentation)
        operationsMap.put(Operations.DRAW_TEXT_RUN, DrawText::documentation)
        operationsMap.put(Operations.DRAW_TEXT_ANCHOR, DrawTextAnchored::documentation)
        operationsMap.put(Operations.DRAW_TEXT_ON_PATH, DrawTextOnPath::documentation)
        operationsMap.put(Operations.DRAW_TEXT_ON_CIRCLE, DrawTextOnCircle::documentation)
        operationsMap.put(Operations.DRAW_BITMAP_FONT_TEXT_RUN, DrawBitmapFontText::documentation)
        operationsMap.put(
            Operations.DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH,
            DrawBitmapFontTextOnPath::documentation,
        )
        operationsMap.put(
            Operations.DRAW_BITMAP_TEXT_ANCHORED,
            DrawBitmapTextAnchored::documentation,
        )
        operationsMap.put(Operations.DRAW_CONTENT, DrawContent::documentation)
        operationsMap.put(Operations.CANVAS_OPERATIONS, CanvasOperations::documentation)
        // Paint & Styles
        operationsMap.put(Operations.PAINT_VALUES, PaintData::documentation)
        operationsMap.put(Operations.DATA_SHADER, ShaderData::documentation)
        operationsMap.put(Operations.COLOR_CONSTANT, ColorConstant::documentation)
        operationsMap.put(Operations.COLOR_EXPRESSIONS, ColorExpression::documentation)
        operationsMap.put(Operations.COLOR_THEME, ColorTheme::documentation)
        operationsMap.put(Operations.ATTRIBUTE_COLOR, ColorAttribute::documentation)
        // Matrix Operations
        operationsMap.put(Operations.MATRIX_SAVE, MatrixSave::documentation)
        operationsMap.put(Operations.MATRIX_RESTORE, MatrixRestore::documentation)
        operationsMap.put(Operations.MATRIX_TRANSLATE, MatrixTranslate::documentation)
        operationsMap.put(Operations.MATRIX_SCALE, MatrixScale::documentation)
        operationsMap.put(Operations.MATRIX_ROTATE, MatrixRotate::documentation)
        operationsMap.put(Operations.MATRIX_SKEW, MatrixSkew::documentation)
        operationsMap.put(Operations.MATRIX_FROM_PATH, MatrixFromPath::documentation)
        operationsMap.put(Operations.MATRIX_CONSTANT, MatrixConstant::documentation)
        operationsMap.put(Operations.MATRIX_EXPRESSION, MatrixExpression::documentation)
        operationsMap.put(Operations.MATRIX_VECTOR_MATH, MatrixVectorMath::documentation)
        // Layout Operations
        operationsMap.put(Operations.LAYOUT_ROOT, RootLayoutComponent::documentation)
        operationsMap.put(Operations.LAYOUT_CONTENT, LayoutComponentContent::documentation)
        operationsMap.put(Operations.LAYOUT_BOX, BoxLayout::documentation)
        operationsMap.put(Operations.LAYOUT_FIT_BOX, FitBoxLayout::documentation)
        operationsMap.put(Operations.LAYOUT_ROW, RowLayout::documentation)
        operationsMap.put(Operations.LAYOUT_COLLAPSIBLE_ROW, CollapsibleRowLayout::documentation)
        operationsMap.put(Operations.LAYOUT_COLUMN, ColumnLayout::documentation)
        operationsMap.put(
            Operations.LAYOUT_COLLAPSIBLE_COLUMN,
            CollapsibleColumnLayout::documentation,
        )
        operationsMap.put(Operations.LAYOUT_FLOW, FlowLayout::documentation)

        operationsMap.put(Operations.LAYOUT_CANVAS, CanvasLayout::documentation)
        operationsMap.put(Operations.LAYOUT_TEXT, TextLayout::documentation)
        operationsMap.put(Operations.CORE_TEXT, CoreText::documentation)
        operationsMap.put(Operations.LAYOUT_IMAGE, ImageLayout::documentation)
        operationsMap.put(Operations.LAYOUT_STATE, StateLayout::documentation)
        operationsMap.put(Operations.COMPONENT_START, ComponentStart::documentation)
        operationsMap.put(Operations.CONTAINER_END, ContainerEnd::documentation)
        // Modifier Operations
        operationsMap.put(Operations.MODIFIER_WIDTH, WidthModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_HEIGHT, HeightModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_WIDTH_IN, WidthInModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_HEIGHT_IN, HeightInModifierOperation::documentation)
        operationsMap.put(
            Operations.MODIFIER_COLLAPSIBLE_PRIORITY,
            CollapsiblePriorityModifierOperation::documentation,
        )
        operationsMap.put(Operations.MODIFIER_PADDING, PaddingModifierOperation::documentation)
        operationsMap.put(
            Operations.MODIFIER_BACKGROUND,
            BackgroundModifierOperation::documentation,
        )
        operationsMap.put(Operations.MODIFIER_BORDER, BorderModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_CLIP_RECT, ClipRectModifierOperation::documentation)
        operationsMap.put(
            Operations.MODIFIER_ROUNDED_CLIP_RECT,
            RoundedClipRectModifierOperation::documentation,
        )
        operationsMap.put(Operations.CLICK_AREA, ClickArea::documentation)
        operationsMap.put(Operations.MODIFIER_CLICK, ClickModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_TOUCH_DOWN, TouchDownModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_TOUCH_UP, TouchUpModifierOperation::documentation)
        operationsMap.put(
            Operations.MODIFIER_TOUCH_CANCEL,
            TouchCancelModifierOperation::documentation,
        )
        operationsMap.put(
            Operations.MODIFIER_VISIBILITY,
            ComponentVisibilityOperation::documentation,
        )
        operationsMap.put(Operations.MODIFIER_OFFSET, OffsetModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_ZINDEX, ZIndexModifierOperation::documentation)
        operationsMap.put(
            Operations.MODIFIER_GRAPHICS_LAYER,
            GraphicsLayerModifierOperation::documentation,
        )
        operationsMap.put(Operations.MODIFIER_SCROLL, ScrollModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_MARQUEE, MarqueeModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_RIPPLE, RippleModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_ALIGN_BY, AlignByModifierOperation::documentation)
        operationsMap.put(Operations.MODIFIER_DRAW_CONTENT, DrawContentOperation::documentation)
        operationsMap.put(Operations.LAYOUT_COMPUTE, LayoutComputeOperation::documentation)
        // Logic & Expressions
        operationsMap.put(Operations.ANIMATED_FLOAT, FloatExpression::documentation)
        operationsMap.put(Operations.INTEGER_EXPRESSION, IntegerExpression::documentation)
        operationsMap.put(Operations.CONDITIONAL_OPERATIONS, ConditionalOperations::documentation)
        operationsMap.put(Operations.LOOP_START, LoopOperation::documentation)
        operationsMap.put(Operations.TEXT_FROM_FLOAT, TextFromFloat::documentation)
        operationsMap.put(Operations.TEXT_MERGE, TextMerge::documentation)
        operationsMap.put(Operations.TEXT_LOOKUP, TextLookup::documentation)
        operationsMap.put(Operations.TEXT_LOOKUP_INT, TextLookupInt::documentation)
        operationsMap.put(Operations.TEXT_MEASURE, TextMeasure::documentation)
        operationsMap.put(Operations.TEXT_LENGTH, TextLength::documentation)
        operationsMap.put(Operations.TEXT_SUBTEXT, TextSubtext::documentation)
        operationsMap.put(Operations.TEXT_TRANSFORM, TextTransform::documentation)
        operationsMap.put(Operations.BITMAP_TEXT_MEASURE, BitmapTextMeasure::documentation)
        operationsMap.put(Operations.ID_LOOKUP, IdLookup::documentation)
        operationsMap.put(Operations.ATTRIBUTE_TIME, TimeAttribute::documentation)
        operationsMap.put(Operations.ATTRIBUTE_TEXT, TextAttribute::documentation)
        operationsMap.put(Operations.ATTRIBUTE_IMAGE, ImageAttribute::documentation)
        operationsMap.put(Operations.TOUCH_EXPRESSION, TouchExpression::documentation)
        operationsMap.put(Operations.COMPONENT_VALUE, ComponentValue::documentation)
        // Actions & Events
        operationsMap.put(Operations.RUN_ACTION, RunActionOperation::documentation)
        operationsMap.put(Operations.HOST_ACTION, HostActionOperation::documentation)
        operationsMap.put(Operations.HOST_NAMED_ACTION, HostNamedActionOperation::documentation)
        operationsMap.put(
            Operations.HOST_METADATA_ACTION,
            HostActionMetadataOperation::documentation,
        )
        operationsMap.put(
            Operations.VALUE_INTEGER_CHANGE_ACTION,
            ValueIntegerChangeActionOperation::documentation,
        )
        operationsMap.put(
            Operations.VALUE_STRING_CHANGE_ACTION,
            ValueStringChangeActionOperation::documentation,
        )
        operationsMap.put(
            Operations.VALUE_INTEGER_EXPRESSION_CHANGE_ACTION,
            ValueIntegerExpressionChangeActionOperation::documentation,
        )
        operationsMap.put(
            Operations.VALUE_FLOAT_CHANGE_ACTION,
            ValueFloatChangeActionOperation::documentation,
        )
        operationsMap.put(
            Operations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION,
            ValueFloatExpressionChangeActionOperation::documentation,
        )
        // Animation & Particles
        operationsMap.put(Operations.ANIMATION_SPEC, AnimationSpec::documentation)
        operationsMap.put(Operations.PARTICLE_DEFINE, ParticlesCreate::documentation)
        operationsMap.put(Operations.PARTICLE_LOOP, ParticlesLoop::documentation)
        operationsMap.put(Operations.PARTICLE_COMPARE, ParticlesCompare::documentation)
        operationsMap.put(Operations.IMPULSE_START, ImpulseOperation::documentation)
        operationsMap.put(Operations.IMPULSE_PROCESS, ImpulseProcess::documentation)
        // Accessibility
        operationsMap.put(Operations.ACCESSIBILITY_SEMANTICS, CoreSemantics::documentation)
        // Miscellaneous
        operationsMap.put(Operations.HAPTIC_FEEDBACK, HapticFeedback::documentation)
    }

    fun content(): String {

        listOperations()
        val operations = operationsMap.values
        for (op in operations) {
            // if (op is DocumentedCompanionOperation) {
            op.documentation(this)
            // }
        }

        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        role(
            "**$title v${CoreDocument.MAJOR_VERSION}.${CoreDocument.MINOR_VERSION}.${CoreDocument.PATCH_VERSION}**\n    " +
                formatter.format(LocalDateTime.now())
        )
        role(
            "*Current number of operations: ${numOperations + numWIPOperations} ($numWIPOperations not fully documented)*"
        )
        role(intro)

        //    val interpolationMethods = InterpolationReader.map.values
        //    for (m in interpolationMethods) {
        //      m.documentation(this)
        //    }

        val layoutPart = readPart("rc_layout.md")
        if (layoutPart != null) {
            buffer.append(layoutPart)
        }

        val categories =
            ////            arrayListOf(
            ////                "Protocol Operations",
            ////                "Data Operations",
            ////                "Canvas Operations",
            ////                "Draw Operations",
            ////                "Expressions Operations",
            ////                "Layout Operations",
            ////                "Modifier Operations",
            ////            )
            arrayListOf(
                "Document Protocol Operations",
                "Data Operations",
                "Paint & Styles Operations",
                "Canvas Operations",
                "Text Operations",
                "Layout Operations",
                "Layout Managers",
                "Modifier Operations",
                "Actions & Events Operations",
                "Animation & Particles Operations",
                "Logic & Expressions Operations",
                "Matrix Operations",
                "Accessibility Operations",
                "Miscellaneous Operations",
            )

        val categoriesDescriptions =
            //            hashMapOf(
            //                "Protocol Operations" to "Operations related to the Origami protocol
            // itself.",
            //                "Data operations" to "Operations related to resource loading.",
            //                "Canvas operations" to
            //                    "Canvas state manipulations (save, restore, translate, etc.)",
            //                "Draw operations" to
            //                    "Operations representing canvas draw commands (draw line, etc.)",
            //                "Expressions operations" to "Operations related to expression
            // evaluation.",
            //                "Layout Operations" to "Layout related operations.",
            //            )
            hashMapOf(
                "Document Protocol Operations" to
                    "Core document metadata, versioning, themes, and high-level behaviors.",
                "Data Operations" to
                    "Definitions of static constants, named variables, collections (lists/maps), and resource data (Bitmaps, Fonts).",
                "Canvas Operations" to
                    "Low-level drawing primitives for shapes, paths, text, and bitmaps, as well as clipping and layer management.",
                "Paint & Styles Operations" to
                    "Management of paint properties, shaders, and complex color definitions (themes, expressions).",
                "Matrix Operations" to
                    "Coordinate system transformations, including translation, scaling, rotation, skewing, and matrix math.",
                "Layout Managers" to
                    "Higher-level components that manage the positioning and sizing of their children according to specific algorithms (Row, Column, Box, etc.).",
                "Layout Operations" to
                    "High-level structural components (Box, Row, Column, etc.) used to build the UI hierarchy.",
                "Modifier Operations" to
                    "Augmentations applied to components to change their dimensions, appearance, or behavior.",
                "Logic & Expressions Operations" to
                    "Dynamic calculations, conditional execution, loops, and measurement utilities.",
                "Actions & Events Operations" to
                    "Interactive side effects, including host-side actions and dynamic state updates.",
                "Animation & Particles Operations" to
                    "Specialized systems for time-based transitions, impulses, and complex particle effects.",
                "Accessibility Operations" to
                    "Semantics and properties used to expose UI information to assistive technologies.",
                "Miscellaneous Operations" to
                    "System-level feedback operations like Haptic Feedback.",
                "Text Operations" to
                    "Operations for defining, manipulating, measuring, and rendering text and font data. This includes support for static strings, dynamic formatting, text-based expressions, spatial measurement of text\n" +
                        "  bounds, and high-fidelity rendering using both standard and bitmap fonts across various canvas geometries.",
            )
        //        val categories = categoriesDescriptions.keys

        //        if (documentedOperations.keys.size != categories.size) {
        //            val d1 = documentedOperations.keys.size
        //            val d2 = categories.size
        //            buffer.append("WARNING -- not all categories are generated $d1 != $d2 :")
        //            for (category in documentedOperations.keys) {
        //                val c = documentedOperations[category]
        //                if (c != null && c.size > 0) {
        //
        //                    buffer.append("# Undocumented categories")
        //                    buffer.append("- ${category}\n")
        //                }
        //            }
        //        }
        for (category in categories) {
            buffer.append("# ${category}\n")
            val ops = documentedOperations[category]
            if (ops == null) {
                continue
            }
            val description = categoriesDescriptions[category]
            if (description != null) {
                buffer.append(description + "\n\n")
            }
            buffer.append("Operations in this category:\n")
            buffer.append("    | ID | Name | Version | Size (bytes) \n")
            buffer.append("    | ---- | ---- | ---- | ---- |\n")
            for (op in ops!!) {
                val size =
                    if (op.isWIP) {
                        "Unknown"
                    } else {
                        "${op.sizeFields + 1}${op.varSize}"
                    }
                buffer.append("    | ${op.id} | ${op.name} | v${op.addedVersion} | $size \n")
            }
            for (op in ops!!) {
                var title = "## ${op.name}"
                if (op.isExperimental) {
                    title += " [EXPERIMENTAL]"
                }
                if (op.addedVersion > 6) {
                    buffer.append("\n$title (added in v${op.addedVersion})\n")
                } else {
                    buffer.append("\n$title\n")
                }
                if (op.isExperimental) {
                    buffer.append("!!! WARNING\n    Experimental operation\n\n")
                }
                if (op.isWIP) {
                    buffer.append("!!! WARNING\n    Undocumented operation\n\n")
                    continue
                }
                buffer.append("${op.description}\n\n")
                val numFields = op.fields.size
                val sizeFields = op.sizeFields + 1
                val varSet = op.varSize
                buffer.append("$numFields Fields, total size $sizeFields$varSet bytes\n")

                buffer.append("<br>")
                buffer.append("<table>")
                buffer.append("<tr><th>Type</th><th>Name</th><th>Description</th></tr>\n")
                buffer.append(
                    "<tr><td>${DocumentedOperation.getType(
                    DocumentedOperation.BYTE)}</td><td>${op.name}</td><td>Value: ${op.id}</td></tr>\n"
                )

                for (field in op.fields) {
                    buffer.append(field.toDoc())
                }
                buffer.append("</table>\n\n")

                for (field in op.fields) {
                    if (field is OperationField && field.hasEnumeratedValues()) {
                        buffer.append("### ${field.name}\n")
                        buffer.append("    | Name | Value |\n")
                        buffer.append("    | ---- | ---- |\n")
                        for (v in field.possibleValues) {
                            buffer.append("    | ${v.name} | ${v.value} \n")
                        }
                    }
                }

                val addDoc = op.additionalDocumentation
                if (addDoc != null) {
                    val content = readPart("$addDoc.md")
                    if (content != null) {
                        buffer.append(content)
                        buffer.append("\n\n")
                    }
                }

                if (op.textExamples != null || op.examples.size > 0) {
                    buffer.append("### Examples\n")

                    if (op.textExamples != null) {
                        buffer.append("${op.textExamples}\n")
                    }

                    if (op.examples.size > 0) {
                        buffer.append(
                            """
<table class="interpolation">
<tr>
          """
                        )
                        for (example in op.examples) {
                            if (op.examplesWidth < op.examplesHeight) {
                                buffer.append(
                                    """
<td>
  <table class="interpolation" width="${op.examplesWidth}" height="${op.examplesHeight}">
    <tr><td><img src='images/${example.value}' height="${op.examplesHeight}"></td></tr>
    <tr><td width="${op.examplesWidth}">${example.name}</td></tr>
  </table>
</td>
          """
                                )
                            } else {
                                buffer.append(
                                    """
<tr>
<td width="${op.examplesHeight}">${example.name}</td>
<td>
  <img src='images/${example.value}' width="${op.examplesWidth}">
</td>
</tr>  
          """
                                )
                            }
                        }
                        buffer.append(
                            """
</tr>
</table>            
          """
                        )
                    }
                }
            }
        }
        versionSummary(buffer, 6)
        versionSummary(buffer, 7)
        experimentalSummary(buffer)
        appendix1(buffer)
        appendix2(buffer)
        buffer.append(
            """
            <script>
            window.markdeepOptions = {
                tocDepth: 2,
                detectMath: true
            };
            </script>
        """
        )
        buffer.append(postamble())

        val content = buffer.toString()
        buffer = StringBuilder(preamble())
        return content
    }

    fun versionSummary(buffer: StringBuilder, version: Int) {
        val versionOps = listOps.values.filter { it.addedVersion == version }.sortedBy { it.id }
        if (versionOps.isNotEmpty()) {
            buffer.append("\n# List of Version $version Operations\n\n")
            buffer.append(
                "The following ${versionOps.size} operations were added in version $version of the format.\n\n"
            )
            buffer.append("    | ID | Name | Category | Description |\n")
            buffer.append("    | ---- | ---- | ---- | ---- |\n")
            for (op in versionOps) {
                buffer.append(
                    "    | ${op.id} | ${op.name} | ${op.category} | ${stripLineBreaks(op.description)} |\n"
                )
            }
            buffer.append("\n")
        }
    }

    fun experimentalSummary(buffer: StringBuilder) {
        val experimentalOps = listOps.values.filter { it.isExperimental }.sortedBy { it.id }
        if (experimentalOps.isNotEmpty()) {
            buffer.append("\n# Experimental Operations\n\n")
            buffer.append(
                "The following ${experimentalOps.size} operations are considered experimental and may change in future versions.\n\n"
            )
            buffer.append("    | ID | Name | Version | Category | Description |\n")
            buffer.append("    | ---- | ---- | ---- | ---- | ---- |\n")
            for (op in experimentalOps) {
                buffer.append(
                    "    | ${op.id} | ${op.name} | v${op.addedVersion} | ${op.category} | ${
                    stripLineBreaks(
                        op.description
                    )
                } |\n"
                )
            }
            buffer.append("\n")
        }
    }

    fun stripName(name: String): String {
        return name.replace("\\s".toRegex(), "")
    }

    fun stripLineBreaks(text: String?): String {
        return text?.replace("\n", " ")?.replace("\r", " ") ?: ""
    }

    fun readPart(name: String): String? {
        val paths =
            arrayOf(
                "compose/remote/Documentation/parts/",
                "Documentation/parts/",
                "../Documentation/parts/",
            )
        for (path in paths) {
            val file = File(path + name)
            if (file.exists()) {
                return file.readText()
            }
        }
        return null
    }

    override fun add(value: String) {
        buffer.append(value)
    }

    val documentedOperations = HashMap<String, ArrayList<DocumentedOperation>>()

    var numOperations = 0
    var numWIPOperations = 0

    val listOps = HashMap<Int, DocumentedOperation>()

    override fun operation(category: String, id: Int, name: String): DocumentedOperation {
        numOperations++
        val operation = DocumentedOperation(category, id, name)
        if (listOps[id] != null) {
            println("WTF")
        }
        listOps[id] = operation
        var list = documentedOperations[category]
        if (list == null) {
            list = ArrayList<DocumentedOperation>()
            documentedOperations[category] = list
        }
        list.add(operation)
        return operation
    }

    override fun wipOperation(category: String, id: Int, name: String): DocumentedOperation {
        numWIPOperations++
        val operation = DocumentedOperation(category, id, name, true)
        var list = documentedOperations[category]
        if (list == null) {
            list = ArrayList<DocumentedOperation>()
            documentedOperations[category] = list
        }
        list.add(operation)
        return operation
    }
}

fun appendix1(buffer: StringBuilder) {

    buffer.append("\n# Appendix 1: FloatExpressions\n\n")
    buffer.append("    | Name | Value | Value | EXAMPLE|\n")
    buffer.append("    | ---- | ---- | ---- | ---- |\n")
    buffer.append("    | ADD  | Nan(1) | addition | 3,2,ADD -> 5|\n")
    buffer.append("    | SUB  | Nan(2) | subtraction | 3,2,SUB -> 1|\n")
    buffer.append("    | MUL  | Nan(3) | multiplication | 3,2,MUL -> 1|\n")
    buffer.append("    | DIV  | Nan(4) | division | 3,2,DIV -> 1.5 |\n")
    buffer.append("    | MOD  | Nan(5) | Modulus | 3,2,DIV -> 1|\n")
    buffer.append("    | MIN  | Nan(6) | Minimum | 3,2,MIN -> 2|\n")
    buffer.append("    | MAX  | Nan(7) | Maximum | 3,2,MAX -> 3 |\n")
    buffer.append("    | POW  | Nan(8) | power | 3,2,POW -> 9 |\n")
    buffer.append("    | SQRT  | Nan(9) | square root | 2,SQRT-> 1.414 |\n")
    buffer.append("    | ABS  | Nan(10) | absolute value | -2,ABS -> 2 |\n")
    buffer.append("    | SIGN  | Nan(11) | sign | -3, SIGN -> -1 |\n")
    buffer.append("    | COPY_SIGN  | Nan(12) | transfer sign |7, -3, COPY_SIGN -> -7 |\n")
    buffer.append("    | EXP  | Nan(13) | exponent  | 1, EXP -> 2.7182 |\n")
    buffer.append("    | FLOOR  | Nan(14) | floor | 32.3,FLOOR -> 32 |\n")
    buffer.append("    | LOG  | Nan(15) | log() | 100, LOG -> 2 |\n")
    buffer.append("    | LN  | Nan(16) | ln() | 100, LN -> 4.605|\n")
    buffer.append("    | ROUND  | Nan(17) | round | 3.5, ROUND -> 4 |\n")
    buffer.append("    | SIN  | Nan(18) | sin | 3.141/2, SIN -> 1.00 |\n")
    buffer.append("    | COS  | Nan(19) | cosine | 3.141/2, COS -> 0.0 |\n")
    buffer.append("    | TAN  | Nan(20) | tan |  3.141/4, TAN -> 1.0 |\n")
    buffer.append("    | ASIN  | Nan(21) | asin |  1, ASIN -> 1.57 |\n")
    buffer.append("    | ACOS  | Nan(22) | acos |  1, ACOS -> 0 |\n")
    buffer.append("    | ATAN  | Nan(23) | atan |  1, ATAN -> 0.785 |\n")
    buffer.append("    | ATAN2  | Nan(24) | atan2 |  x,y,ATAN2 - > atan2(x,y) |\n")
    buffer.append("    | MAD  | Nan(25) | Multiple and add |7, -3, 2, MAD -> -7 |\n")
    buffer.append("    | IFELSE  | Nan(26) | ?: | 1,2,0,IFELES -> 1 |\n")
    buffer.append("    | CLAMP  | Nan(27) | clamp  |v,min,max,CLAMP -> v |\n")
    buffer.append("    | CBRT  | Nan(28) | Cube root | |\n")
    buffer.append("    | DEG | Nan(29) | radians to degree | |\n")
    buffer.append("    | RAD | Nan(30 | degrees to radians | |\n")
    buffer.append("    | CEIL | Nan(31) | Ceiling |3.14, CEIL -> 4 |\n")
    buffer.append("    \n\n **Array operation**\n\n")
    buffer.append("    Array operation examples are for [1,2,3,4,5] \n")
    buffer.append("    | Name | Value | Value | EXAMPLE|\n")
    buffer.append("    | ---- | ---- | ---- | ---- |\n")
    buffer.append("    | A_DEREF | Nan(32) | Get a value from array | 2, a, A_DEREF -> 3|\n")
    buffer.append("    | A_MAX | Nan(33) | Maximum of array | a, A_MAX -> 5|\n")
    buffer.append("    | A_MIN | Nan(34) | Minimum of array | a, A_MIN -> 1|\n")
    buffer.append("    | A_SUM | Nan(35) | Sum of array | a, A_SUM -> 15|\n")
    buffer.append("    | A_AVG | Nan(36) | Average of array | a, A_AVG -> 3|\n")
    buffer.append("    | A_LEN | Nan(37) | Length of array | a, A_LEN -> 5|\n")
}

fun appendix2(buffer: StringBuilder) {
    buffer.append("WIP")
    buffer.append("\n# Appendix 2: IntegerExpressions\n\n")
    buffer.append("    | Name | Value | Value | EXAMPLE|\n")
    buffer.append("    | ---- | ---- | ---- | ---- |\n")
    buffer.append("    | ADD  | Nan(1) | addition | 3,2,ADD -> 5|\n")
    buffer.append("    | SUB  | Nan(2) | subtraction | 3,2,SUB -> 1|\n")
    buffer.append("    | MUL  | Nan(3) | multiplication | 3,2,MUL -> 1|\n")
    buffer.append("    | DIV  | Nan(4) | division | 3,2,DIV -> 1 |\n")
    buffer.append("    | MOD  | Nan(5) | Modulus | 3,2,DIV -> 1|\n")
    buffer.append("    | SHL  | Nan(5) | Modulus | 3,2,SHL -> 1|\n")
    buffer.append("    | SHR  | Nan(5) | Modulus | 3,2,SHR -> 1|\n")
    buffer.append("    | USHR  | Nan(5) | Modulus | 3,2,USHR -> 1|\n")
    buffer.append("    | OR  | Nan(5) | Modulus | 3,2,OR -> 1|\n")
    buffer.append("    | AND  | Nan(5) | Modulus | 3,2,AND -> 1|\n")
    buffer.append("    | XOR  | Nan(5) | Modulus | 3,2,XOR -> 1|\n")
    buffer.append("    | COPY_SIGN  | Nan(5) | Modulus | 3,2,COPY_SIGN -> 1|\n")
    buffer.append("    | MIN  | Nan(5) | Modulus | 3,2,MIN -> 2|\n")
    buffer.append("    | MAX  | Nan(5) | Modulus | 3,2,MAX -> 3|\n")
    buffer.append("    | NEG  | Nan(5) | Modulus | 2,NEG -> -2|\n")
    buffer.append("    | ABS  | Nan(5) | Modulus | -2,ABS -> 2|\n")
    buffer.append("    | INCR  | Nan(5) | Modulus | 2,INCR -> 3|\n")
    buffer.append("    | DECR  | Nan(5) | Modulus | 2,DECR -> 1|\n")
    buffer.append("    | NOT  | Nan(5) | Modulus |  2,NOT -> FFFFFFFD|\n")
    buffer.append("    | SIGN  | Nan(5) | Modulus | 2,SIGN -> 1|\n")
    buffer.append("    | CLAMP  | Nan(5) | Modulus | 4,2,3CLAMP -> 3|\n")
    buffer.append("    | IFELSE  | Nan(5) | Modulus | 3,2,IFELSE -> 1|\n")
    buffer.append("    | MAD  | Nan(5) | Modulus | 4,3,2,MAD -> 1|\n")
}
