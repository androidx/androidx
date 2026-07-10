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

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.collection.IntSet
import androidx.collection.buildIntSet
import androidx.collection.intSetOf

/**
 * The version information of the renderer supported by the Host.
 *
 * @property major Major version. Incremented on breaking changes (i.e. compatibility is not
 *   guaranteed across major versions).
 * @property minor Minor version. Incremented on non-breaking changes (e.g. feature additions).
 *   Anything consuming a payload can safely consume anything with a lower minor version.
 * @property revision Revision version. Incremented on non-breaking changes.
 * @property supportedOperations The set of operations supported by the renderer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RendererVersion(
    @IntRange(from = 1) public val major: Int = DEFAULT_RENDERER_VERSION_MAJOR,
    @IntRange(from = 0) public val minor: Int = DEFAULT_RENDERER_VERSION_MINOR,
    @IntRange(from = 0) public val revision: Int = DEFAULT_RENDERER_VERSION_REVISION,
    public val supportedOperations: IntSet = DEFAULT_SUPPORTED_OPERATIONS,
) : Comparable<RendererVersion> {

    public override fun compareTo(other: RendererVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.revision })

    public companion object {
        /** The set of operations supported by initial version (1.6.0). */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val DEFAULT_SUPPORTED_OPERATIONS: IntSet =
            // TODO: b/529699478 - Use proper API constants once they are available.
            intSetOf(
                0 /* HEADER */,
                2 /* COMPONENT_START */,
                14 /* ANIMATION_SPEC */,
                16 /* MODIFIER_WIDTH */,
                39 /* CLIP_RECT */,
                40 /* PAINT_VALUES */,
                42 /* DRAW_RECT */,
                43 /* DRAW_TEXT_RUN */,
                44 /* DRAW_BITMAP */,
                46 /* DRAW_CIRCLE */,
                47 /* DRAW_LINE */,
                51 /* DRAW_ROUND_RECT */,
                52 /* DRAW_SECTOR */,
                54 /* MODIFIER_ROUNDED_CLIP_RECT */,
                55 /* MODIFIER_BACKGROUND */,
                56 /* DRAW_OVAL */,
                57 /* DRAW_TEXT_ON_CIRCLE */,
                58 /* MODIFIER_PADDING */,
                59 /* MODIFIER_CLICK */,
                64 /* CLICK_AREA */,
                66 /* DRAW_BITMAP_INT */,
                67 /* MODIFIER_HEIGHT */,
                80 /* DATA_FLOAT */,
                81 /* ANIMATED_FLOAT */,
                101 /* DATA_BITMAP */,
                102 /* DATA_TEXT */,
                107 /* MODIFIER_BORDER */,
                108 /* MODIFIER_CLIP_RECT */,
                123 /* DATA_PATH */,
                124 /* DRAW_PATH */,
                125 /* DRAW_TWEEN_PATH */,
                126 /* MATRIX_SCALE */,
                127 /* MATRIX_TRANSLATE */,
                128 /* MATRIX_SKEW */,
                129 /* MATRIX_ROTATE */,
                130 /* MATRIX_SAVE */,
                131 /* MATRIX_RESTORE */,
                133 /* DRAW_TEXT_ANCHOR */,
                134 /* COLOR_EXPRESSIONS */,
                135 /* TEXT_FROM_FLOAT */,
                136 /* TEXT_MERGE */,
                137 /* NAMED_VARIABLE */,
                138 /* COLOR_CONSTANT */,
                139 /* DRAW_CONTENT */,
                140 /* DATA_INT */,
                143 /* DATA_BOOLEAN */,
                144 /* INTEGER_EXPRESSION */,
                145 /* ID_MAP */,
                146 /* ID_LIST */,
                147 /* FLOAT_LIST */,
                148 /* DATA_LONG */,
                149 /* DRAW_BITMAP_SCALED */,
                150 /* COMPONENT_VALUE */,
                151 /* TEXT_LOOKUP */,
                152 /* DRAW_ARC */,
                153 /* TEXT_LOOKUP_INT */,
                154 /* DATA_MAP_LOOKUP */,
                155 /* TEXT_MEASURE */,
                156 /* TEXT_LENGTH */,
                158 /* PATH_TWEEN */,
                159 /* PATH_CREATE */,
                160 /* PATH_ADD */,
                164 /* IMPULSE_START */,
                165 /* IMPULSE_PROCESS */,
                166 /* FUNCTION_CALL */,
                168 /* FUNCTION_DEFINE */,
                170 /* ATTRIBUTE_TEXT */,
                171 /* ATTRIBUTE_IMAGE */,
                172 /* ATTRIBUTE_TIME */,
                173 /* CANVAS_OPERATIONS */,
                174 /* MODIFIER_DRAW_CONTENT */,
                175 /* PATH_COMBINE */,
                176 /* LAYOUT_FIT_BOX */,
                177 /* HAPTIC_FEEDBACK */,
                178 /* CONDITIONAL_OPERATIONS */,
                179 /* DEBUG_MESSAGE */,
                180 /* ATTRIBUTE_COLOR */,
                181 /* MATRIX_FROM_PATH */,
                182 /* TEXT_SUBTEXT */,
                183 /* BITMAP_TEXT_MEASURE */,
                184 /* DRAW_BITMAP_TEXT_ANCHORED */,
                185 /* REM */,
                186 /* MATRIX_CONSTANT */,
                187 /* MATRIX_EXPRESSION */,
                188 /* MATRIX_VECTOR_MATH */,
                189 /* DATA_FONT */,
                192 /* ID_LOOKUP */,
                193 /* PATH_EXPRESSION */,
                197 /* DYNAMIC_FLOAT_LIST */,
                198 /* UPDATE_DYNAMIC_FLOAT_LIST */,
                199 /* TEXT_TRANSFORM */,
                200 /* LAYOUT_ROOT */,
                201 /* LAYOUT_CONTENT */,
                202 /* LAYOUT_BOX */,
                203 /* LAYOUT_ROW */,
                204 /* LAYOUT_COLUMN */,
                205 /* LAYOUT_CANVAS */,
                207 /* LAYOUT_CANVAS_CONTENT */,
                208 /* LAYOUT_TEXT */,
                209 /* HOST_ACTION */,
                210 /* HOST_NAMED_ACTION */,
                211 /* MODIFIER_VISIBILITY */,
                212 /* VALUE_INTEGER_CHANGE_ACTION */,
                213 /* VALUE_STRING_CHANGE_ACTION */,
                214 /* CONTAINER_END */,
                215 /* LOOP_START */,
                216 /* HOST_METADATA_ACTION */,
                217 /* LAYOUT_STATE */,
                218 /* VALUE_INTEGER_EXPRESSION_CHANGE_ACTION */,
                221 /* MODIFIER_OFFSET */,
                222 /* VALUE_FLOAT_CHANGE_ACTION */,
                223 /* MODIFIER_ZINDEX */,
                224 /* MODIFIER_GRAPHICS_LAYER */,
                227 /* VALUE_FLOAT_EXPRESSION_CHANGE_ACTION */,
                228 /* MODIFIER_MARQUEE */,
                229 /* MODIFIER_RIPPLE */,
                231 /* MODIFIER_WIDTH_IN */,
                232 /* MODIFIER_HEIGHT_IN */,
                234 /* LAYOUT_IMAGE */,
                236 /* RUN_ACTION */,
                237 /* MODIFIER_ALIGN_BY */,
                250, /* ACCESSIBILITY_SEMANTICS */
            )

        /**
         * The default major version, describing the renderer Host offering initial RemoteCompose
         * support.
         */
        public const val DEFAULT_RENDERER_VERSION_MAJOR: Int = 1

        /**
         * The default minor version, describing the renderer Host offering initial RemoteCompose
         * support.
         */
        public const val DEFAULT_RENDERER_VERSION_MINOR: Int = 6

        /**
         * The default revision version, describing the renderer Host offering initial RemoteCompose
         * support.
         */
        public const val DEFAULT_RENDERER_VERSION_REVISION: Int = 0

        /**
         * Resolves the [RendererVersion] supported by the Wear OS Host by parsing the version name
         * of the ProtoLayout renderer package.
         *
         * If the package is not installed or parsing fails, it will fallback to the default version
         * (`1.000`).
         *
         * @param context The Android Context.
         * @return The resolved [RendererVersion].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromPlHostPackage(context: Context): RendererVersion {
            try {
                val packageInfo =
                    context.packageManager.getPackageInfo(PL_RENDERER_HOST_PACKAGE, /* flags= */ 0)

                val versionName: String? = packageInfo.versionName
                if (versionName.isNullOrEmpty()) return PL_RENDERER_INITIAL_VERSION

                val parts: List<String> = versionName.split(".")
                if (parts.size < 2) return PL_RENDERER_INITIAL_VERSION

                val major =
                    parts[0].toIntOrNull()?.takeIf { it >= 1 } ?: return PL_RENDERER_INITIAL_VERSION

                val minor =
                    parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: return PL_RENDERER_INITIAL_VERSION
                val revision =
                    if (parts.size >= 3) {
                        parts[2].toIntOrNull()?.takeIf { it >= 0 }
                            ?: return PL_RENDERER_INITIAL_VERSION
                    } else {
                        0
                    }

                return RendererVersion(major, minor, revision)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "ProtoLayout renderer package not installed", e)
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected parsing error", e)
            }
            return PL_RENDERER_INITIAL_VERSION
        }

        @VisibleForTesting
        internal const val PL_RENDERER_HOST_PACKAGE: String =
            "com.google.android.wearable.protolayout.renderer"
        @VisibleForTesting internal val PL_RENDERER_INITIAL_VERSION = RendererVersion(1, 0, 0)
        private const val TAG = "RendererVersion"
    }
}

/** Maps [IntSet] to a [List], using the provided function to transform each element. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T> IntSet.mapToList(transform: (Int) -> T): List<T> = buildList {
    this@mapToList.forEach { add(transform(it)) }
}

/**
 * Creates an [IntSet] from a given [List], using the provided function to extract the integer value
 * from each element.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <T> List<T>.toIntSet(crossinline getOpCode: (T) -> Int): IntSet = buildIntSet {
    this@toIntSet.forEach { add(getOpCode(it)) }
}
