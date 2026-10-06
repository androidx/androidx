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

package androidx.glance.wear.core

import androidx.annotation.RestrictTo

/**
 * Operation codes supported by Wear Widgets.
 *
 * These opcodes are forked from [androidx.compose.remote.core.Operations].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RcWearOperations {
    public const val ACCESSIBILITY_SEMANTICS: Int = 250
    public const val ANIMATED_FLOAT: Int = 81
    public const val ANIMATION_SPEC: Int = 14
    public const val ATTRIBUTE_COLOR: Int = 180
    public const val ATTRIBUTE_IMAGE: Int = 171
    public const val ATTRIBUTE_TEXT: Int = 170
    public const val ATTRIBUTE_TIME: Int = 172
    public const val BITMAP_TEXT_MEASURE: Int = 183
    public const val CANVAS_OPERATIONS: Int = 173
    public const val CLICK_AREA: Int = 64
    public const val CLIP_RECT: Int = 39
    public const val COLOR_CONSTANT: Int = 138
    public const val COLOR_EXPRESSIONS: Int = 134
    public const val COMPONENT_START: Int = 2
    public const val COMPONENT_VALUE: Int = 150
    public const val CONDITIONAL_OPERATIONS: Int = 178
    public const val CONTAINER_END: Int = 214
    public const val CORE_TEXT: Int = 239
    public const val DATA_BITMAP: Int = 101
    public const val DATA_BOOLEAN: Int = 143
    public const val DATA_FLOAT: Int = 80
    public const val DATA_FONT: Int = 189
    public const val DATA_INT: Int = 140
    public const val DATA_LONG: Int = 148
    public const val DATA_MAP_LOOKUP: Int = 154
    public const val DATA_PATH: Int = 123
    public const val DATA_TEXT: Int = 102
    public const val DEBUG_MESSAGE: Int = 179
    public const val DRAW_ARC: Int = 152
    public const val DRAW_BITMAP: Int = 44
    public const val DRAW_BITMAP_INT: Int = 66
    public const val DRAW_BITMAP_SCALED: Int = 149
    public const val DRAW_BITMAP_TEXT_ANCHORED: Int = 184
    public const val DRAW_CIRCLE: Int = 46
    public const val DRAW_CONTENT: Int = 139
    public const val DRAW_LINE: Int = 47
    public const val DRAW_OVAL: Int = 56
    public const val DRAW_PATH: Int = 124
    public const val DRAW_RECT: Int = 42
    public const val DRAW_ROUND_RECT: Int = 51
    public const val DRAW_SECTOR: Int = 52
    public const val DRAW_TEXT_ANCHOR: Int = 133
    public const val DRAW_TEXT_ON_CIRCLE: Int = 57
    public const val DRAW_TEXT_RUN: Int = 43
    public const val DRAW_TWEEN_PATH: Int = 125
    public const val DYNAMIC_FLOAT_LIST: Int = 197
    public const val FLOAT_LIST: Int = 147
    public const val FUNCTION_CALL: Int = 166
    public const val FUNCTION_DEFINE: Int = 168
    public const val HAPTIC_FEEDBACK: Int = 177
    public const val HEADER: Int = 0
    public const val HOST_ACTION: Int = 209
    public const val HOST_METADATA_ACTION: Int = 216
    public const val HOST_NAMED_ACTION: Int = 210
    public const val ID_LIST: Int = 146
    public const val ID_LOOKUP: Int = 192
    public const val ID_MAP: Int = 145
    public const val IMPULSE_PROCESS: Int = 165
    public const val IMPULSE_START: Int = 164
    public const val INTEGER_EXPRESSION: Int = 144
    public const val LAYOUT_BOX: Int = 202
    public const val LAYOUT_CANVAS: Int = 205
    public const val LAYOUT_CANVAS_CONTENT: Int = 207
    public const val LAYOUT_COLUMN: Int = 204
    public const val LAYOUT_CONTENT: Int = 201
    public const val LAYOUT_FIT_BOX: Int = 176
    public const val LAYOUT_IMAGE: Int = 234
    public const val LAYOUT_ROOT: Int = 200
    public const val LAYOUT_ROW: Int = 203
    public const val LAYOUT_STATE: Int = 217
    public const val LAYOUT_TEXT: Int = 208
    public const val LOOP_START: Int = 215
    public const val MATRIX_CONSTANT: Int = 186
    public const val MATRIX_EXPRESSION: Int = 187
    public const val MATRIX_FROM_PATH: Int = 181
    public const val MATRIX_RESTORE: Int = 131
    public const val MATRIX_ROTATE: Int = 129
    public const val MATRIX_SAVE: Int = 130
    public const val MATRIX_SCALE: Int = 126
    public const val MATRIX_SKEW: Int = 128
    public const val MATRIX_TRANSLATE: Int = 127
    public const val MATRIX_VECTOR_MATH: Int = 188
    public const val MODIFIER_ALIGN_BY: Int = 237
    public const val MODIFIER_BACKGROUND: Int = 55
    public const val MODIFIER_BORDER: Int = 107
    public const val MODIFIER_CLICK: Int = 59
    public const val MODIFIER_CLIP_RECT: Int = 108
    public const val MODIFIER_DRAW_CONTENT: Int = 174
    public const val MODIFIER_GRAPHICS_LAYER: Int = 224
    public const val MODIFIER_HEIGHT: Int = 67
    public const val MODIFIER_HEIGHT_IN: Int = 232
    public const val MODIFIER_MARQUEE: Int = 228
    public const val MODIFIER_OFFSET: Int = 221
    public const val MODIFIER_PADDING: Int = 58
    public const val MODIFIER_RIPPLE: Int = 229
    public const val MODIFIER_ROUNDED_CLIP_RECT: Int = 54
    public const val MODIFIER_VISIBILITY: Int = 211
    public const val MODIFIER_WIDTH: Int = 16
    public const val MODIFIER_WIDTH_IN: Int = 231
    public const val MODIFIER_ZINDEX: Int = 223
    public const val NAMED_VARIABLE: Int = 137
    public const val PAINT_VALUES: Int = 40
    public const val PATH_ADD: Int = 160
    public const val PATH_COMBINE: Int = 175
    public const val PATH_CREATE: Int = 159
    public const val PATH_EXPRESSION: Int = 193
    public const val PATH_TWEEN: Int = 158
    public const val REM: Int = 185
    public const val RUN_ACTION: Int = 236
    public const val TEXT_FROM_FLOAT: Int = 135
    public const val TEXT_LENGTH: Int = 156
    public const val TEXT_LOOKUP: Int = 151
    public const val TEXT_LOOKUP_INT: Int = 153
    public const val TEXT_MEASURE: Int = 155
    public const val TEXT_MERGE: Int = 136
    public const val TEXT_SUBTEXT: Int = 182
    public const val TEXT_TRANSFORM: Int = 199
    public const val UPDATE_DYNAMIC_FLOAT_LIST: Int = 198
    public const val VALUE_FLOAT_CHANGE_ACTION: Int = 222
    public const val VALUE_FLOAT_EXPRESSION_CHANGE_ACTION: Int = 227
    public const val VALUE_INTEGER_CHANGE_ACTION: Int = 212
    public const val VALUE_INTEGER_EXPRESSION_CHANGE_ACTION: Int = 218
    public const val VALUE_STRING_CHANGE_ACTION: Int = 213
}
