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

package androidx.compose.remote.core

import androidx.compose.remote.core.Operations.ACCESSIBILITY_SEMANTICS
import androidx.compose.remote.core.Operations.ADD_MESH_2D
import androidx.compose.remote.core.Operations.ANIMATED_FLOAT
import androidx.compose.remote.core.Operations.ANIMATION_SPEC
import androidx.compose.remote.core.Operations.ATTRIBUTE_COLOR
import androidx.compose.remote.core.Operations.ATTRIBUTE_IMAGE
import androidx.compose.remote.core.Operations.ATTRIBUTE_TEXT
import androidx.compose.remote.core.Operations.ATTRIBUTE_TIME
import androidx.compose.remote.core.Operations.BINARY_DATA
import androidx.compose.remote.core.Operations.BITMAP_TEXT_MEASURE
import androidx.compose.remote.core.Operations.CANVAS_OPERATIONS
import androidx.compose.remote.core.Operations.CLICK_AREA
import androidx.compose.remote.core.Operations.CLIP_PATH
import androidx.compose.remote.core.Operations.CLIP_RECT
import androidx.compose.remote.core.Operations.COLOR_CONSTANT
import androidx.compose.remote.core.Operations.COLOR_EXPRESSIONS
import androidx.compose.remote.core.Operations.COLOR_THEME
import androidx.compose.remote.core.Operations.COMPONENT_START
import androidx.compose.remote.core.Operations.COMPONENT_VALUE
import androidx.compose.remote.core.Operations.CONDITIONAL_OPERATIONS
import androidx.compose.remote.core.Operations.CONTAINER_END
import androidx.compose.remote.core.Operations.CORE_TEXT
import androidx.compose.remote.core.Operations.DATA_BITMAP
import androidx.compose.remote.core.Operations.DATA_BITMAP_FONT
import androidx.compose.remote.core.Operations.DATA_BOOLEAN
import androidx.compose.remote.core.Operations.DATA_FLOAT
import androidx.compose.remote.core.Operations.DATA_FONT
import androidx.compose.remote.core.Operations.DATA_INT
import androidx.compose.remote.core.Operations.DATA_LONG
import androidx.compose.remote.core.Operations.DATA_MAP_LOOKUP
import androidx.compose.remote.core.Operations.DATA_PATH
import androidx.compose.remote.core.Operations.DATA_SHADER
import androidx.compose.remote.core.Operations.DATA_SOUND
import androidx.compose.remote.core.Operations.DATA_TEXT
import androidx.compose.remote.core.Operations.DEBUG_MESSAGE
import androidx.compose.remote.core.Operations.DRAW_ARC
import androidx.compose.remote.core.Operations.DRAW_BITMAP
import androidx.compose.remote.core.Operations.DRAW_BITMAP_FONT_TEXT_RUN
import androidx.compose.remote.core.Operations.DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH
import androidx.compose.remote.core.Operations.DRAW_BITMAP_INT
import androidx.compose.remote.core.Operations.DRAW_BITMAP_SCALED
import androidx.compose.remote.core.Operations.DRAW_BITMAP_TEXT_ANCHORED
import androidx.compose.remote.core.Operations.DRAW_CIRCLE
import androidx.compose.remote.core.Operations.DRAW_CONTENT
import androidx.compose.remote.core.Operations.DRAW_LINE
import androidx.compose.remote.core.Operations.DRAW_MESH_2D
import androidx.compose.remote.core.Operations.DRAW_OVAL
import androidx.compose.remote.core.Operations.DRAW_PATH
import androidx.compose.remote.core.Operations.DRAW_RECT
import androidx.compose.remote.core.Operations.DRAW_ROUND_RECT
import androidx.compose.remote.core.Operations.DRAW_SECTOR
import androidx.compose.remote.core.Operations.DRAW_TEXT_ANCHOR
import androidx.compose.remote.core.Operations.DRAW_TEXT_ON_PATH
import androidx.compose.remote.core.Operations.DRAW_TEXT_RUN
import androidx.compose.remote.core.Operations.DRAW_TO_BITMAP
import androidx.compose.remote.core.Operations.DRAW_TWEEN_PATH
import androidx.compose.remote.core.Operations.DYNAMIC_FLOAT_LIST
import androidx.compose.remote.core.Operations.EVENT_ACTION
import androidx.compose.remote.core.Operations.FLOAT_LIST
import androidx.compose.remote.core.Operations.FUNCTION_CALL
import androidx.compose.remote.core.Operations.FUNCTION_DEFINE
import androidx.compose.remote.core.Operations.HAPTIC_FEEDBACK
import androidx.compose.remote.core.Operations.HEADER
import androidx.compose.remote.core.Operations.HOST_ACTION
import androidx.compose.remote.core.Operations.HOST_METADATA_ACTION
import androidx.compose.remote.core.Operations.HOST_NAMED_ACTION
import androidx.compose.remote.core.Operations.ID_LIST
import androidx.compose.remote.core.Operations.ID_LOOKUP
import androidx.compose.remote.core.Operations.ID_MAP
import androidx.compose.remote.core.Operations.IMPULSE_PROCESS
import androidx.compose.remote.core.Operations.IMPULSE_START
import androidx.compose.remote.core.Operations.INCLUDE_REFERENCED_OPERATIONS
import androidx.compose.remote.core.Operations.INTEGER_EXPRESSION
import androidx.compose.remote.core.Operations.LAYOUT_BOX
import androidx.compose.remote.core.Operations.LAYOUT_CANVAS
import androidx.compose.remote.core.Operations.LAYOUT_CANVAS_CONTENT
import androidx.compose.remote.core.Operations.LAYOUT_COLLAPSIBLE_COLUMN
import androidx.compose.remote.core.Operations.LAYOUT_COLLAPSIBLE_ROW
import androidx.compose.remote.core.Operations.LAYOUT_COLUMN
import androidx.compose.remote.core.Operations.LAYOUT_COMPUTE
import androidx.compose.remote.core.Operations.LAYOUT_CONTENT
import androidx.compose.remote.core.Operations.LAYOUT_CUSTOM
import androidx.compose.remote.core.Operations.LAYOUT_FIT_BOX
import androidx.compose.remote.core.Operations.LAYOUT_FLOW
import androidx.compose.remote.core.Operations.LAYOUT_IMAGE
import androidx.compose.remote.core.Operations.LAYOUT_RC_DOC
import androidx.compose.remote.core.Operations.LAYOUT_ROOT
import androidx.compose.remote.core.Operations.LAYOUT_ROW
import androidx.compose.remote.core.Operations.LAYOUT_STATE
import androidx.compose.remote.core.Operations.LAYOUT_TEXT
import androidx.compose.remote.core.Operations.LOOP_START
import androidx.compose.remote.core.Operations.MACRO_ARGUMENT
import androidx.compose.remote.core.Operations.MACRO_BLOCK
import androidx.compose.remote.core.Operations.MACRO_CALL
import androidx.compose.remote.core.Operations.MACRO_DEFINE
import androidx.compose.remote.core.Operations.MACRO_FOR_EACH
import androidx.compose.remote.core.Operations.MAP_RC_DOC_VARIABLES
import androidx.compose.remote.core.Operations.MATRIX_CONSTANT
import androidx.compose.remote.core.Operations.MATRIX_EXPRESSION
import androidx.compose.remote.core.Operations.MATRIX_FROM_MESH_2D
import androidx.compose.remote.core.Operations.MATRIX_FROM_PATH
import androidx.compose.remote.core.Operations.MATRIX_RESTORE
import androidx.compose.remote.core.Operations.MATRIX_ROTATE
import androidx.compose.remote.core.Operations.MATRIX_SAVE
import androidx.compose.remote.core.Operations.MATRIX_SCALE
import androidx.compose.remote.core.Operations.MATRIX_SKEW
import androidx.compose.remote.core.Operations.MATRIX_TRANSLATE
import androidx.compose.remote.core.Operations.MATRIX_VECTOR_MATH
import androidx.compose.remote.core.Operations.MODIFIER_ALIGN_BY
import androidx.compose.remote.core.Operations.MODIFIER_BACKGROUND
import androidx.compose.remote.core.Operations.MODIFIER_BORDER
import androidx.compose.remote.core.Operations.MODIFIER_CLICK
import androidx.compose.remote.core.Operations.MODIFIER_CLIP_RECT
import androidx.compose.remote.core.Operations.MODIFIER_COLLAPSIBLE_PRIORITY
import androidx.compose.remote.core.Operations.MODIFIER_DIMENSION_CONSTRAINTS
import androidx.compose.remote.core.Operations.MODIFIER_DRAW_CONTENT
import androidx.compose.remote.core.Operations.MODIFIER_GRAPHICS_LAYER
import androidx.compose.remote.core.Operations.MODIFIER_HEIGHT
import androidx.compose.remote.core.Operations.MODIFIER_HEIGHT_IN
import androidx.compose.remote.core.Operations.MODIFIER_MARQUEE
import androidx.compose.remote.core.Operations.MODIFIER_MULTI_CLICK
import androidx.compose.remote.core.Operations.MODIFIER_OFFSET
import androidx.compose.remote.core.Operations.MODIFIER_PADDING
import androidx.compose.remote.core.Operations.MODIFIER_RIPPLE
import androidx.compose.remote.core.Operations.MODIFIER_ROUNDED_CLIP_RECT
import androidx.compose.remote.core.Operations.MODIFIER_SCROLL
import androidx.compose.remote.core.Operations.MODIFIER_TOUCH_CANCEL
import androidx.compose.remote.core.Operations.MODIFIER_TOUCH_DOWN
import androidx.compose.remote.core.Operations.MODIFIER_TOUCH_UP
import androidx.compose.remote.core.Operations.MODIFIER_VISIBILITY
import androidx.compose.remote.core.Operations.MODIFIER_WIDTH
import androidx.compose.remote.core.Operations.MODIFIER_WIDTH_IN
import androidx.compose.remote.core.Operations.MODIFIER_ZINDEX
import androidx.compose.remote.core.Operations.NAMED_VARIABLE
import androidx.compose.remote.core.Operations.PAINT_VALUES
import androidx.compose.remote.core.Operations.PARTICLE_COMPARE
import androidx.compose.remote.core.Operations.PARTICLE_DEFINE
import androidx.compose.remote.core.Operations.PARTICLE_LOOP
import androidx.compose.remote.core.Operations.PATH_ADD
import androidx.compose.remote.core.Operations.PATH_COMBINE
import androidx.compose.remote.core.Operations.PATH_CREATE
import androidx.compose.remote.core.Operations.PATH_EXPRESSION
import androidx.compose.remote.core.Operations.PATH_TWEEN
import androidx.compose.remote.core.Operations.PLAY_SOUND
import androidx.compose.remote.core.Operations.REFERENCED_OPERATIONS
import androidx.compose.remote.core.Operations.REM
import androidx.compose.remote.core.Operations.ROOT_CONTENT_BEHAVIOR
import androidx.compose.remote.core.Operations.ROOT_CONTENT_DESCRIPTION
import androidx.compose.remote.core.Operations.RUN_ACTION
import androidx.compose.remote.core.Operations.SKIP
import androidx.compose.remote.core.Operations.SOUND_EXPRESSION
import androidx.compose.remote.core.Operations.TEXT_FROM_FLOAT
import androidx.compose.remote.core.Operations.TEXT_LENGTH
import androidx.compose.remote.core.Operations.TEXT_LOOKUP
import androidx.compose.remote.core.Operations.TEXT_LOOKUP_INT
import androidx.compose.remote.core.Operations.TEXT_MEASURE
import androidx.compose.remote.core.Operations.TEXT_MERGE
import androidx.compose.remote.core.Operations.TEXT_STYLE
import androidx.compose.remote.core.Operations.TEXT_SUBTEXT
import androidx.compose.remote.core.Operations.TEXT_TRANSFORM
import androidx.compose.remote.core.Operations.THEME
import androidx.compose.remote.core.Operations.TOUCH_EXPRESSION
import androidx.compose.remote.core.Operations.UPDATE_DYNAMIC_FLOAT_LIST
import androidx.compose.remote.core.Operations.VALUE_FLOAT_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_INTEGER_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_INTEGER_EXPRESSION_CHANGE_ACTION
import androidx.compose.remote.core.Operations.VALUE_STRING_CHANGE_ACTION
import androidx.compose.remote.core.Operations.WAKE_IN
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROID_NATIVE
import androidx.compose.remote.core.RcProfiles.PROFILE_BASELINE
import androidx.compose.remote.core.RcProfiles.PROFILE_DEPRECATED
import androidx.compose.remote.core.RcProfiles.PROFILE_EXPERIMENTAL
import androidx.compose.remote.core.RcProfiles.PROFILE_LOW_POWER
import androidx.compose.remote.core.RcProfiles.PROFILE_OEM
import androidx.compose.remote.core.RcProfiles.PROFILE_WEAR_WIDGETS
import androidx.compose.remote.core.RcProfiles.PROFILE_WIDGETS
import androidx.compose.remote.core.operations.AddMesh2D
import androidx.compose.remote.core.operations.BinaryData
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.core.operations.ClickArea
import androidx.compose.remote.core.operations.ClipPath
import androidx.compose.remote.core.operations.ClipRect
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
import androidx.compose.remote.core.operations.DrawMesh2D
import androidx.compose.remote.core.operations.DrawOval
import androidx.compose.remote.core.operations.DrawPath
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.DrawSector
import androidx.compose.remote.core.operations.DrawText
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.core.operations.DrawToBitmap
import androidx.compose.remote.core.operations.DrawTweenPath
import androidx.compose.remote.core.operations.EventActionOperation
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.FontData
import androidx.compose.remote.core.operations.HapticFeedback
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.IdLookup
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.IncludeReferencedOperations
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.MapRcDocVariables
import androidx.compose.remote.core.operations.MatrixFromMesh2D
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
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.PathExpression
import androidx.compose.remote.core.operations.PathTween
import androidx.compose.remote.core.operations.PlaySound
import androidx.compose.remote.core.operations.ReferencedOperations
import androidx.compose.remote.core.operations.Rem
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.core.operations.RootContentDescription
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Skip
import androidx.compose.remote.core.operations.SoundData
import androidx.compose.remote.core.operations.SoundExpression
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
import androidx.compose.remote.core.operations.layout.CanvasContent
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.ComponentStart
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.ImpulseOperation
import androidx.compose.remote.core.operations.layout.ImpulseProcess
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.MultiClickModifier
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
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.RcDocLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.managers.TextStyle
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BackgroundModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ClipRectModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
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
import androidx.compose.remote.core.operations.loom.PatternArgument
import androidx.compose.remote.core.operations.loom.PatternBlock
import androidx.compose.remote.core.operations.loom.PatternDefine
import androidx.compose.remote.core.operations.loom.PatternForEach
import androidx.compose.remote.core.operations.loom.PatternInflation
import androidx.compose.remote.core.operations.matrix.MatrixConstant
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.matrix.MatrixVectorMath
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.core.types.BooleanConstant
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.remote.core.types.LongConstant
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * Pins the exact operations, opcode and operation class, that [Operations.getOperations] exposes
 * for api level 6 and for every profile combination at api level 7.
 *
 * Each expected map is spelled out in full, on purpose: nothing is derived from another map, so a
 * change in what a profile exposes shows up as a plain one-line difference in the failing test.
 */
class OperationsProfilesTest {

    @Test
    fun `api level 6`() {
        assertThat(operations(6, PROFILE_BASELINE))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DATA_SHADER to ShaderData::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    ROOT_CONTENT_BEHAVIOR to RootContentBehavior::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 baseline`() {
        assertThat(operations(7, PROFILE_BASELINE))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 profiles without built-in operations expose the baseline`() {
        val expected =
            mapOf(
                HEADER to Header::class.java,
                COMPONENT_START to ComponentStart::class.java,
                ANIMATION_SPEC to AnimationSpec::class.java,
                MODIFIER_WIDTH to WidthModifierOperation::class.java,
                CLIP_PATH to ClipPath::class.java,
                CLIP_RECT to ClipRect::class.java,
                PAINT_VALUES to PaintData::class.java,
                DRAW_RECT to DrawRect::class.java,
                DRAW_TEXT_RUN to DrawText::class.java,
                DRAW_BITMAP to DrawBitmap::class.java,
                DRAW_CIRCLE to DrawCircle::class.java,
                DRAW_LINE to DrawLine::class.java,
                DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                DRAW_ROUND_RECT to DrawRoundRect::class.java,
                DRAW_SECTOR to DrawSector::class.java,
                DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                DRAW_OVAL to DrawOval::class.java,
                MODIFIER_PADDING to PaddingModifierOperation::class.java,
                MODIFIER_CLICK to ClickModifierOperation::class.java,
                THEME to Theme::class.java,
                CLICK_AREA to ClickArea::class.java,
                DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                DATA_FLOAT to FloatConstant::class.java,
                ANIMATED_FLOAT to FloatExpression::class.java,
                DATA_BITMAP to BitmapData::class.java,
                DATA_TEXT to TextData::class.java,
                ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                MODIFIER_BORDER to BorderModifierOperation::class.java,
                MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                DATA_PATH to PathData::class.java,
                DRAW_PATH to DrawPath::class.java,
                DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                MATRIX_SCALE to MatrixScale::class.java,
                MATRIX_TRANSLATE to MatrixTranslate::class.java,
                MATRIX_SKEW to MatrixSkew::class.java,
                MATRIX_ROTATE to MatrixRotate::class.java,
                MATRIX_SAVE to MatrixSave::class.java,
                MATRIX_RESTORE to MatrixRestore::class.java,
                DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                COLOR_EXPRESSIONS to ColorExpression::class.java,
                TEXT_FROM_FLOAT to TextFromFloat::class.java,
                TEXT_MERGE to TextMerge::class.java,
                NAMED_VARIABLE to NamedVariable::class.java,
                COLOR_CONSTANT to ColorConstant::class.java,
                DRAW_CONTENT to DrawContent::class.java,
                DATA_INT to IntegerConstant::class.java,
                DATA_BOOLEAN to BooleanConstant::class.java,
                INTEGER_EXPRESSION to IntegerExpression::class.java,
                ID_MAP to DataMapIds::class.java,
                ID_LIST to DataListIds::class.java,
                FLOAT_LIST to DataListFloat::class.java,
                DATA_LONG to LongConstant::class.java,
                DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                COMPONENT_VALUE to ComponentValue::class.java,
                TEXT_LOOKUP to TextLookup::class.java,
                DRAW_ARC to DrawArc::class.java,
                TEXT_LOOKUP_INT to TextLookupInt::class.java,
                DATA_MAP_LOOKUP to DataMapLookup::class.java,
                TEXT_MEASURE to TextMeasure::class.java,
                TEXT_LENGTH to TextLength::class.java,
                TOUCH_EXPRESSION to TouchExpression::class.java,
                PATH_TWEEN to PathTween::class.java,
                PATH_CREATE to PathCreate::class.java,
                PATH_ADD to PathAppend::class.java,
                PARTICLE_DEFINE to ParticlesCreate::class.java,
                PARTICLE_LOOP to ParticlesLoop::class.java,
                IMPULSE_START to ImpulseOperation::class.java,
                IMPULSE_PROCESS to ImpulseProcess::class.java,
                FUNCTION_CALL to FloatFunctionCall::class.java,
                DATA_BITMAP_FONT to BitmapFontData::class.java,
                FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                ATTRIBUTE_TEXT to TextAttribute::class.java,
                ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                ATTRIBUTE_TIME to TimeAttribute::class.java,
                CANVAS_OPERATIONS to CanvasOperations::class.java,
                MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                PATH_COMBINE to PathCombine::class.java,
                LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                HAPTIC_FEEDBACK to HapticFeedback::class.java,
                CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                DEBUG_MESSAGE to DebugMessage::class.java,
                ATTRIBUTE_COLOR to ColorAttribute::class.java,
                REM to Rem::class.java,
                MATRIX_CONSTANT to MatrixConstant::class.java,
                MATRIX_EXPRESSION to MatrixExpression::class.java,
                MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                LAYOUT_ROOT to RootLayoutComponent::class.java,
                LAYOUT_CONTENT to LayoutComponentContent::class.java,
                LAYOUT_BOX to BoxLayout::class.java,
                LAYOUT_ROW to RowLayout::class.java,
                LAYOUT_COLUMN to ColumnLayout::class.java,
                LAYOUT_CANVAS to CanvasLayout::class.java,
                LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                LAYOUT_TEXT to TextLayout::class.java,
                HOST_ACTION to HostActionOperation::class.java,
                HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                CONTAINER_END to ContainerEnd::class.java,
                LOOP_START to LoopOperation::class.java,
                HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                LAYOUT_STATE to StateLayout::class.java,
                VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                    ValueIntegerExpressionChangeActionOperation::class.java,
                MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                    ValueFloatExpressionChangeActionOperation::class.java,
                MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                LAYOUT_IMAGE to ImageLayout::class.java,
                MODIFIER_COLLAPSIBLE_PRIORITY to CollapsiblePriorityModifierOperation::class.java,
                RUN_ACTION to RunActionOperation::class.java,
                ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
            )

        assertThat(operations(7, PROFILE_EXPERIMENTAL)).containsExactlyEntriesIn(expected)
        assertThat(operations(7, PROFILE_DEPRECATED)).containsExactlyEntriesIn(expected)
        assertThat(operations(7, PROFILE_EXPERIMENTAL or PROFILE_DEPRECATED))
            .containsExactlyEntriesIn(expected)
        assertThat(operations(7, PROFILE_OEM)).containsExactlyEntriesIn(expected)
        assertThat(operations(7, PROFILE_LOW_POWER)).containsExactlyEntriesIn(expected)
        assertThat(operations(7, PROFILE_ANDROID_NATIVE)).containsExactlyEntriesIn(expected)
        assertThat(operations(7, PROFILE_WEAR_WIDGETS)).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `api level 7 androidx`() {
        assertThat(operations(7, PROFILE_ANDROIDX))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DATA_SHADER to ShaderData::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DATA_FONT to FontData::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 androidx with experimental`() {
        assertThat(operations(7, PROFILE_ANDROIDX or PROFILE_EXPERIMENTAL))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DATA_SHADER to ShaderData::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    MODIFIER_MULTI_CLICK to MultiClickModifier::class.java,
                    LAYOUT_CUSTOM to Custom::class.java,
                    EVENT_ACTION to EventActionOperation::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    ADD_MESH_2D to AddMesh2D::class.java,
                    DRAW_MESH_2D to DrawMesh2D::class.java,
                    MATRIX_FROM_MESH_2D to MatrixFromMesh2D::class.java,
                    LAYOUT_RC_DOC to RcDocLayout::class.java,
                    MAP_RC_DOC_VARIABLES to MapRcDocVariables::class.java,
                    BINARY_DATA to BinaryData::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    PLAY_SOUND to PlaySound::class.java,
                    REFERENCED_OPERATIONS to ReferencedOperations::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    DATA_SOUND to SoundData::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DATA_FONT to FontData::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    SOUND_EXPRESSION to SoundExpression::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    LAYOUT_COMPUTE to LayoutComputeOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    LAYOUT_FLOW to FlowLayout::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    MODIFIER_DIMENSION_CONSTRAINTS to
                        DimensionConstraintsModifierOperation::class.java,
                    MACRO_FOR_EACH to PatternForEach::class.java,
                    INCLUDE_REFERENCED_OPERATIONS to IncludeReferencedOperations::class.java,
                    MACRO_DEFINE to PatternDefine::class.java,
                    MACRO_CALL to PatternInflation::class.java,
                    MACRO_ARGUMENT to PatternArgument::class.java,
                    MACRO_BLOCK to PatternBlock::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 androidx with deprecated`() {
        assertThat(operations(7, PROFILE_ANDROIDX or PROFILE_DEPRECATED))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DATA_SHADER to ShaderData::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    ROOT_CONTENT_BEHAVIOR to RootContentBehavior::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DATA_FONT to FontData::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 androidx with experimental and deprecated`() {
        assertThat(operations(7, PROFILE_ANDROIDX or PROFILE_EXPERIMENTAL or PROFILE_DEPRECATED))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DATA_SHADER to ShaderData::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    ROOT_CONTENT_BEHAVIOR to RootContentBehavior::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    MODIFIER_MULTI_CLICK to MultiClickModifier::class.java,
                    LAYOUT_CUSTOM to Custom::class.java,
                    EVENT_ACTION to EventActionOperation::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    ADD_MESH_2D to AddMesh2D::class.java,
                    DRAW_MESH_2D to DrawMesh2D::class.java,
                    MATRIX_FROM_MESH_2D to MatrixFromMesh2D::class.java,
                    LAYOUT_RC_DOC to RcDocLayout::class.java,
                    MAP_RC_DOC_VARIABLES to MapRcDocVariables::class.java,
                    BINARY_DATA to BinaryData::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    PLAY_SOUND to PlaySound::class.java,
                    REFERENCED_OPERATIONS to ReferencedOperations::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    DATA_SOUND to SoundData::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DATA_FONT to FontData::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    SOUND_EXPRESSION to SoundExpression::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    LAYOUT_COMPUTE to LayoutComputeOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    LAYOUT_FLOW to FlowLayout::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    MODIFIER_DIMENSION_CONSTRAINTS to
                        DimensionConstraintsModifierOperation::class.java,
                    MACRO_FOR_EACH to PatternForEach::class.java,
                    INCLUDE_REFERENCED_OPERATIONS to IncludeReferencedOperations::class.java,
                    MACRO_DEFINE to PatternDefine::class.java,
                    MACRO_CALL to PatternInflation::class.java,
                    MACRO_ARGUMENT to PatternArgument::class.java,
                    MACRO_BLOCK to PatternBlock::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 widgets`() {
        assertThat(operations(7, PROFILE_WIDGETS))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 widgets with experimental`() {
        assertThat(operations(7, PROFILE_WIDGETS or PROFILE_EXPERIMENTAL))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    MODIFIER_MULTI_CLICK to MultiClickModifier::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    ADD_MESH_2D to AddMesh2D::class.java,
                    DRAW_MESH_2D to DrawMesh2D::class.java,
                    MATRIX_FROM_MESH_2D to MatrixFromMesh2D::class.java,
                    LAYOUT_RC_DOC to RcDocLayout::class.java,
                    MAP_RC_DOC_VARIABLES to MapRcDocVariables::class.java,
                    BINARY_DATA to BinaryData::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    PLAY_SOUND to PlaySound::class.java,
                    REFERENCED_OPERATIONS to ReferencedOperations::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    DATA_SOUND to SoundData::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    SOUND_EXPRESSION to SoundExpression::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    LAYOUT_COMPUTE to LayoutComputeOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    LAYOUT_FLOW to FlowLayout::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    MODIFIER_DIMENSION_CONSTRAINTS to
                        DimensionConstraintsModifierOperation::class.java,
                    MACRO_FOR_EACH to PatternForEach::class.java,
                    INCLUDE_REFERENCED_OPERATIONS to IncludeReferencedOperations::class.java,
                    MACRO_DEFINE to PatternDefine::class.java,
                    MACRO_CALL to PatternInflation::class.java,
                    MACRO_ARGUMENT to PatternArgument::class.java,
                    MACRO_BLOCK to PatternBlock::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 widgets with deprecated`() {
        assertThat(operations(7, PROFILE_WIDGETS or PROFILE_DEPRECATED))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    ROOT_CONTENT_BEHAVIOR to RootContentBehavior::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 widgets with experimental and deprecated`() {
        assertThat(operations(7, PROFILE_WIDGETS or PROFILE_EXPERIMENTAL or PROFILE_DEPRECATED))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    ROOT_CONTENT_BEHAVIOR to RootContentBehavior::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    MODIFIER_MULTI_CLICK to MultiClickModifier::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    ADD_MESH_2D to AddMesh2D::class.java,
                    DRAW_MESH_2D to DrawMesh2D::class.java,
                    MATRIX_FROM_MESH_2D to MatrixFromMesh2D::class.java,
                    LAYOUT_RC_DOC to RcDocLayout::class.java,
                    MAP_RC_DOC_VARIABLES to MapRcDocVariables::class.java,
                    BINARY_DATA to BinaryData::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    PLAY_SOUND to PlaySound::class.java,
                    REFERENCED_OPERATIONS to ReferencedOperations::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    DATA_SOUND to SoundData::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    SOUND_EXPRESSION to SoundExpression::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    LAYOUT_COMPUTE to LayoutComputeOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    LAYOUT_FLOW to FlowLayout::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    MODIFIER_DIMENSION_CONSTRAINTS to
                        DimensionConstraintsModifierOperation::class.java,
                    MACRO_FOR_EACH to PatternForEach::class.java,
                    INCLUDE_REFERENCED_OPERATIONS to IncludeReferencedOperations::class.java,
                    MACRO_DEFINE to PatternDefine::class.java,
                    MACRO_CALL to PatternInflation::class.java,
                    MACRO_ARGUMENT to PatternArgument::class.java,
                    MACRO_BLOCK to PatternBlock::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 androidx and widgets`() {
        assertThat(operations(7, PROFILE_ANDROIDX or PROFILE_WIDGETS))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 androidx and widgets with experimental`() {
        assertThat(operations(7, PROFILE_ANDROIDX or PROFILE_WIDGETS or PROFILE_EXPERIMENTAL))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    MODIFIER_MULTI_CLICK to MultiClickModifier::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    ADD_MESH_2D to AddMesh2D::class.java,
                    DRAW_MESH_2D to DrawMesh2D::class.java,
                    MATRIX_FROM_MESH_2D to MatrixFromMesh2D::class.java,
                    LAYOUT_RC_DOC to RcDocLayout::class.java,
                    MAP_RC_DOC_VARIABLES to MapRcDocVariables::class.java,
                    BINARY_DATA to BinaryData::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    PLAY_SOUND to PlaySound::class.java,
                    REFERENCED_OPERATIONS to ReferencedOperations::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    DATA_SOUND to SoundData::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    SOUND_EXPRESSION to SoundExpression::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    LAYOUT_COMPUTE to LayoutComputeOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    LAYOUT_FLOW to FlowLayout::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    MODIFIER_DIMENSION_CONSTRAINTS to
                        DimensionConstraintsModifierOperation::class.java,
                    MACRO_FOR_EACH to PatternForEach::class.java,
                    INCLUDE_REFERENCED_OPERATIONS to IncludeReferencedOperations::class.java,
                    MACRO_DEFINE to PatternDefine::class.java,
                    MACRO_CALL to PatternInflation::class.java,
                    MACRO_ARGUMENT to PatternArgument::class.java,
                    MACRO_BLOCK to PatternBlock::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 androidx and widgets with deprecated`() {
        assertThat(operations(7, PROFILE_ANDROIDX or PROFILE_WIDGETS or PROFILE_DEPRECATED))
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    ROOT_CONTENT_BEHAVIOR to RootContentBehavior::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    @Test
    fun `api level 7 androidx and widgets with experimental and deprecated`() {
        assertThat(
                operations(
                    7,
                    PROFILE_ANDROIDX or
                        PROFILE_WIDGETS or
                        PROFILE_EXPERIMENTAL or
                        PROFILE_DEPRECATED,
                )
            )
            .containsExactlyEntriesIn(
                mapOf(
                    HEADER to Header::class.java,
                    COMPONENT_START to ComponentStart::class.java,
                    ANIMATION_SPEC to AnimationSpec::class.java,
                    MODIFIER_WIDTH to WidthModifierOperation::class.java,
                    CLIP_PATH to ClipPath::class.java,
                    CLIP_RECT to ClipRect::class.java,
                    PAINT_VALUES to PaintData::class.java,
                    DRAW_RECT to DrawRect::class.java,
                    DRAW_TEXT_RUN to DrawText::class.java,
                    DRAW_BITMAP to DrawBitmap::class.java,
                    DRAW_CIRCLE to DrawCircle::class.java,
                    DRAW_LINE to DrawLine::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN to DrawBitmapFontText::class.java,
                    DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH to DrawBitmapFontTextOnPath::class.java,
                    DRAW_ROUND_RECT to DrawRoundRect::class.java,
                    DRAW_SECTOR to DrawSector::class.java,
                    DRAW_TEXT_ON_PATH to DrawTextOnPath::class.java,
                    MODIFIER_ROUNDED_CLIP_RECT to RoundedClipRectModifierOperation::class.java,
                    MODIFIER_BACKGROUND to BackgroundModifierOperation::class.java,
                    DRAW_OVAL to DrawOval::class.java,
                    MODIFIER_PADDING to PaddingModifierOperation::class.java,
                    MODIFIER_CLICK to ClickModifierOperation::class.java,
                    THEME to Theme::class.java,
                    CLICK_AREA to ClickArea::class.java,
                    ROOT_CONTENT_BEHAVIOR to RootContentBehavior::class.java,
                    DRAW_BITMAP_INT to DrawBitmapInt::class.java,
                    MODIFIER_HEIGHT to HeightModifierOperation::class.java,
                    DATA_FLOAT to FloatConstant::class.java,
                    ANIMATED_FLOAT to FloatExpression::class.java,
                    MODIFIER_MULTI_CLICK to MultiClickModifier::class.java,
                    DATA_BITMAP to BitmapData::class.java,
                    DATA_TEXT to TextData::class.java,
                    ROOT_CONTENT_DESCRIPTION to RootContentDescription::class.java,
                    ADD_MESH_2D to AddMesh2D::class.java,
                    DRAW_MESH_2D to DrawMesh2D::class.java,
                    MATRIX_FROM_MESH_2D to MatrixFromMesh2D::class.java,
                    LAYOUT_RC_DOC to RcDocLayout::class.java,
                    MAP_RC_DOC_VARIABLES to MapRcDocVariables::class.java,
                    BINARY_DATA to BinaryData::class.java,
                    MODIFIER_BORDER to BorderModifierOperation::class.java,
                    MODIFIER_CLIP_RECT to ClipRectModifierOperation::class.java,
                    DATA_PATH to PathData::class.java,
                    DRAW_PATH to DrawPath::class.java,
                    DRAW_TWEEN_PATH to DrawTweenPath::class.java,
                    MATRIX_SCALE to MatrixScale::class.java,
                    MATRIX_TRANSLATE to MatrixTranslate::class.java,
                    MATRIX_SKEW to MatrixSkew::class.java,
                    MATRIX_ROTATE to MatrixRotate::class.java,
                    MATRIX_SAVE to MatrixSave::class.java,
                    MATRIX_RESTORE to MatrixRestore::class.java,
                    DRAW_TEXT_ANCHOR to DrawTextAnchored::class.java,
                    COLOR_EXPRESSIONS to ColorExpression::class.java,
                    TEXT_FROM_FLOAT to TextFromFloat::class.java,
                    TEXT_MERGE to TextMerge::class.java,
                    NAMED_VARIABLE to NamedVariable::class.java,
                    COLOR_CONSTANT to ColorConstant::class.java,
                    DRAW_CONTENT to DrawContent::class.java,
                    DATA_INT to IntegerConstant::class.java,
                    PLAY_SOUND to PlaySound::class.java,
                    REFERENCED_OPERATIONS to ReferencedOperations::class.java,
                    DATA_BOOLEAN to BooleanConstant::class.java,
                    INTEGER_EXPRESSION to IntegerExpression::class.java,
                    ID_MAP to DataMapIds::class.java,
                    ID_LIST to DataListIds::class.java,
                    FLOAT_LIST to DataListFloat::class.java,
                    DATA_LONG to LongConstant::class.java,
                    DRAW_BITMAP_SCALED to DrawBitmapScaled::class.java,
                    COMPONENT_VALUE to ComponentValue::class.java,
                    TEXT_LOOKUP to TextLookup::class.java,
                    DRAW_ARC to DrawArc::class.java,
                    TEXT_LOOKUP_INT to TextLookupInt::class.java,
                    DATA_MAP_LOOKUP to DataMapLookup::class.java,
                    TEXT_MEASURE to TextMeasure::class.java,
                    TEXT_LENGTH to TextLength::class.java,
                    TOUCH_EXPRESSION to TouchExpression::class.java,
                    PATH_TWEEN to PathTween::class.java,
                    PATH_CREATE to PathCreate::class.java,
                    PATH_ADD to PathAppend::class.java,
                    PARTICLE_DEFINE to ParticlesCreate::class.java,
                    PARTICLE_LOOP to ParticlesLoop::class.java,
                    IMPULSE_START to ImpulseOperation::class.java,
                    IMPULSE_PROCESS to ImpulseProcess::class.java,
                    FUNCTION_CALL to FloatFunctionCall::class.java,
                    DATA_BITMAP_FONT to BitmapFontData::class.java,
                    FUNCTION_DEFINE to FloatFunctionDefine::class.java,
                    DATA_SOUND to SoundData::class.java,
                    ATTRIBUTE_TEXT to TextAttribute::class.java,
                    ATTRIBUTE_IMAGE to ImageAttribute::class.java,
                    ATTRIBUTE_TIME to TimeAttribute::class.java,
                    CANVAS_OPERATIONS to CanvasOperations::class.java,
                    MODIFIER_DRAW_CONTENT to DrawContentOperation::class.java,
                    PATH_COMBINE to PathCombine::class.java,
                    LAYOUT_FIT_BOX to FitBoxLayout::class.java,
                    HAPTIC_FEEDBACK to HapticFeedback::class.java,
                    CONDITIONAL_OPERATIONS to ConditionalOperations::class.java,
                    DEBUG_MESSAGE to DebugMessage::class.java,
                    ATTRIBUTE_COLOR to ColorAttribute::class.java,
                    MATRIX_FROM_PATH to MatrixFromPath::class.java,
                    TEXT_SUBTEXT to TextSubtext::class.java,
                    BITMAP_TEXT_MEASURE to BitmapTextMeasure::class.java,
                    DRAW_BITMAP_TEXT_ANCHORED to DrawBitmapTextAnchored::class.java,
                    REM to Rem::class.java,
                    MATRIX_CONSTANT to MatrixConstant::class.java,
                    MATRIX_EXPRESSION to MatrixExpression::class.java,
                    MATRIX_VECTOR_MATH to MatrixVectorMath::class.java,
                    DRAW_TO_BITMAP to DrawToBitmap::class.java,
                    WAKE_IN to WakeIn::class.java,
                    ID_LOOKUP to IdLookup::class.java,
                    PATH_EXPRESSION to PathExpression::class.java,
                    PARTICLE_COMPARE to ParticlesCompare::class.java,
                    COLOR_THEME to ColorTheme::class.java,
                    DYNAMIC_FLOAT_LIST to DataDynamicListFloat::class.java,
                    UPDATE_DYNAMIC_FLOAT_LIST to UpdateDynamicFloatList::class.java,
                    TEXT_TRANSFORM to TextTransform::class.java,
                    LAYOUT_ROOT to RootLayoutComponent::class.java,
                    LAYOUT_CONTENT to LayoutComponentContent::class.java,
                    LAYOUT_BOX to BoxLayout::class.java,
                    LAYOUT_ROW to RowLayout::class.java,
                    LAYOUT_COLUMN to ColumnLayout::class.java,
                    LAYOUT_CANVAS to CanvasLayout::class.java,
                    SOUND_EXPRESSION to SoundExpression::class.java,
                    LAYOUT_CANVAS_CONTENT to CanvasContent::class.java,
                    LAYOUT_TEXT to TextLayout::class.java,
                    HOST_ACTION to HostActionOperation::class.java,
                    HOST_NAMED_ACTION to HostNamedActionOperation::class.java,
                    MODIFIER_VISIBILITY to ComponentVisibilityOperation::class.java,
                    VALUE_INTEGER_CHANGE_ACTION to ValueIntegerChangeActionOperation::class.java,
                    VALUE_STRING_CHANGE_ACTION to ValueStringChangeActionOperation::class.java,
                    CONTAINER_END to ContainerEnd::class.java,
                    LOOP_START to LoopOperation::class.java,
                    HOST_METADATA_ACTION to HostActionMetadataOperation::class.java,
                    LAYOUT_STATE to StateLayout::class.java,
                    VALUE_INTEGER_EXPRESSION_CHANGE_ACTION to
                        ValueIntegerExpressionChangeActionOperation::class.java,
                    MODIFIER_TOUCH_DOWN to TouchDownModifierOperation::class.java,
                    MODIFIER_TOUCH_UP to TouchUpModifierOperation::class.java,
                    MODIFIER_OFFSET to OffsetModifierOperation::class.java,
                    VALUE_FLOAT_CHANGE_ACTION to ValueFloatChangeActionOperation::class.java,
                    MODIFIER_ZINDEX to ZIndexModifierOperation::class.java,
                    MODIFIER_GRAPHICS_LAYER to GraphicsLayerModifierOperation::class.java,
                    MODIFIER_TOUCH_CANCEL to TouchCancelModifierOperation::class.java,
                    MODIFIER_SCROLL to ScrollModifierOperation::class.java,
                    VALUE_FLOAT_EXPRESSION_CHANGE_ACTION to
                        ValueFloatExpressionChangeActionOperation::class.java,
                    MODIFIER_MARQUEE to MarqueeModifierOperation::class.java,
                    MODIFIER_RIPPLE to RippleModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_ROW to CollapsibleRowLayout::class.java,
                    MODIFIER_WIDTH_IN to WidthInModifierOperation::class.java,
                    MODIFIER_HEIGHT_IN to HeightInModifierOperation::class.java,
                    LAYOUT_COLLAPSIBLE_COLUMN to CollapsibleColumnLayout::class.java,
                    LAYOUT_IMAGE to ImageLayout::class.java,
                    MODIFIER_COLLAPSIBLE_PRIORITY to
                        CollapsiblePriorityModifierOperation::class.java,
                    RUN_ACTION to RunActionOperation::class.java,
                    MODIFIER_ALIGN_BY to AlignByModifierOperation::class.java,
                    LAYOUT_COMPUTE to LayoutComputeOperation::class.java,
                    CORE_TEXT to CoreText::class.java,
                    LAYOUT_FLOW to FlowLayout::class.java,
                    SKIP to Skip::class.java,
                    TEXT_STYLE to TextStyle::class.java,
                    MODIFIER_DIMENSION_CONSTRAINTS to
                        DimensionConstraintsModifierOperation::class.java,
                    MACRO_FOR_EACH to PatternForEach::class.java,
                    INCLUDE_REFERENCED_OPERATIONS to IncludeReferencedOperations::class.java,
                    MACRO_DEFINE to PatternDefine::class.java,
                    MACRO_CALL to PatternInflation::class.java,
                    MACRO_ARGUMENT to PatternArgument::class.java,
                    MACRO_BLOCK to PatternBlock::class.java,
                    ACCESSIBILITY_SEMANTICS to CoreSemantics::class.java,
                )
            )
    }

    /**
     * Returns, for every opcode in the map [Operations.getOperations] returns for the given api
     * level and profiles, the class of the operation its reader creates.
     */
    private fun operations(apiLevel: Int, profiles: Int): Map<Int, Class<*>> {
        val map = Operations.getOperations(apiLevel, profiles)!!
        return map.keySet().associateWith { operationClass(map.get(it)!!) }
    }

    /**
     * Identifies the operation class behind a [CompanionOperation].
     *
     * The reader is a `Foo::read` method reference, which cannot be inspected directly. It is
     * therefore invoked with a buffer that throws on the first read: the frame that called into the
     * buffer is then `Foo.read`. Readers of operations without payload never touch the buffer and
     * instead add their operation to the list, whose class is used.
     */
    private fun operationClass(reader: CompanionOperation): Class<*> {
        val operations = ArrayList<Operation>()
        try {
            reader.read(ProbeBuffer(), operations)
        } catch (e: IllegalStateException) {
            if (e.message != ProbeBuffer.READ_ATTEMPTED) throw e
            val frames = e.stackTrace
            val here = frames.indexOfFirst {
                it.className == javaClass.name && it.methodName == "operationClass"
            }
            return Class.forName(frames[here - 1].className)
        }
        return operations.single().javaClass
    }

    /** A [WireBuffer] that fails with [READ_ATTEMPTED] on any read. */
    private class ProbeBuffer : WireBuffer() {
        override fun available(): Boolean = error(READ_ATTEMPTED)

        override fun getIndex(): Int = error(READ_ATTEMPTED)

        override fun readId(): Int = error(READ_ATTEMPTED)

        override fun readNanId(): Float = error(READ_ATTEMPTED)

        override fun readLongNanId(): Long = error(READ_ATTEMPTED)

        override fun readOperationType(): Int = error(READ_ATTEMPTED)

        override fun readBoolean(): Boolean = error(READ_ATTEMPTED)

        override fun readByte(): Int = error(READ_ATTEMPTED)

        override fun readShort(): Int = error(READ_ATTEMPTED)

        override fun peekInt(): Int = error(READ_ATTEMPTED)

        override fun readInt(): Int = error(READ_ATTEMPTED)

        override fun readLong(): Long = error(READ_ATTEMPTED)

        override fun readFloat(): Float = error(READ_ATTEMPTED)

        override fun readDouble(): Double = error(READ_ATTEMPTED)

        override fun readBuffer(): ByteArray = error(READ_ATTEMPTED)

        override fun readBuffer(maxSize: Int): ByteArray = error(READ_ATTEMPTED)

        override fun readUTF8(): String = error(READ_ATTEMPTED)

        override fun readUTF8(maxSize: Int): String = error(READ_ATTEMPTED)

        companion object {
            const val READ_ATTEMPTED =
                "Reader tried to read from the probe buffer; the calling frame identifies it"
        }
    }
}
