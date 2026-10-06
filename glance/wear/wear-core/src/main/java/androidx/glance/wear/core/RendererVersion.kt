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
    @IntRange(from = 1) public val major: Int,
    @IntRange(from = 0) public val minor: Int,
    @IntRange(from = 0) public val revision: Int,
    public val supportedOperations: IntSet,
) : Comparable<RendererVersion> {

    public override fun compareTo(other: RendererVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.revision })

    public companion object {
        /**
         * The safe fallback major version, describing the renderer Host offering initial
         * RemoteCompose support.
         */
        public const val SAFE_FALLBACK_MAJOR: Int = 1

        /**
         * The safe fallback minor version, describing the renderer Host offering initial
         * RemoteCompose support.
         */
        public const val SAFE_FALLBACK_MINOR: Int = 6

        /**
         * The safe fallback revision version, describing the renderer Host offering initial
         * RemoteCompose support.
         */
        public const val SAFE_FALLBACK_REVISION: Int = 0

        /** The set of operations supported by initial version (1.6.0). */
        public val SAFE_FALLBACK_SUPPORTED_OPERATIONS: IntSet =
            intSetOf(
                RcWearOperations.HEADER,
                RcWearOperations.COMPONENT_START,
                RcWearOperations.ANIMATION_SPEC,
                RcWearOperations.MODIFIER_WIDTH,
                RcWearOperations.CLIP_RECT,
                RcWearOperations.PAINT_VALUES,
                RcWearOperations.DRAW_RECT,
                RcWearOperations.DRAW_TEXT_RUN,
                RcWearOperations.DRAW_BITMAP,
                RcWearOperations.DRAW_CIRCLE,
                RcWearOperations.DRAW_LINE,
                RcWearOperations.DRAW_ROUND_RECT,
                RcWearOperations.DRAW_SECTOR,
                RcWearOperations.MODIFIER_ROUNDED_CLIP_RECT,
                RcWearOperations.MODIFIER_BACKGROUND,
                RcWearOperations.DRAW_OVAL,
                RcWearOperations.DRAW_TEXT_ON_CIRCLE,
                RcWearOperations.MODIFIER_PADDING,
                RcWearOperations.MODIFIER_CLICK,
                RcWearOperations.CLICK_AREA,
                RcWearOperations.DRAW_BITMAP_INT,
                RcWearOperations.MODIFIER_HEIGHT,
                RcWearOperations.DATA_FLOAT,
                RcWearOperations.ANIMATED_FLOAT,
                RcWearOperations.DATA_BITMAP,
                RcWearOperations.DATA_TEXT,
                RcWearOperations.MODIFIER_BORDER,
                RcWearOperations.MODIFIER_CLIP_RECT,
                RcWearOperations.DATA_PATH,
                RcWearOperations.DRAW_PATH,
                RcWearOperations.DRAW_TWEEN_PATH,
                RcWearOperations.MATRIX_SCALE,
                RcWearOperations.MATRIX_TRANSLATE,
                RcWearOperations.MATRIX_SKEW,
                RcWearOperations.MATRIX_ROTATE,
                RcWearOperations.MATRIX_SAVE,
                RcWearOperations.MATRIX_RESTORE,
                RcWearOperations.DRAW_TEXT_ANCHOR,
                RcWearOperations.COLOR_EXPRESSIONS,
                RcWearOperations.TEXT_FROM_FLOAT,
                RcWearOperations.TEXT_MERGE,
                RcWearOperations.NAMED_VARIABLE,
                RcWearOperations.COLOR_CONSTANT,
                RcWearOperations.DRAW_CONTENT,
                RcWearOperations.DATA_INT,
                RcWearOperations.DATA_BOOLEAN,
                RcWearOperations.INTEGER_EXPRESSION,
                RcWearOperations.ID_MAP,
                RcWearOperations.ID_LIST,
                RcWearOperations.FLOAT_LIST,
                RcWearOperations.DATA_LONG,
                RcWearOperations.DRAW_BITMAP_SCALED,
                RcWearOperations.COMPONENT_VALUE,
                RcWearOperations.TEXT_LOOKUP,
                RcWearOperations.DRAW_ARC,
                RcWearOperations.TEXT_LOOKUP_INT,
                RcWearOperations.DATA_MAP_LOOKUP,
                RcWearOperations.TEXT_MEASURE,
                RcWearOperations.TEXT_LENGTH,
                RcWearOperations.PATH_TWEEN,
                RcWearOperations.PATH_CREATE,
                RcWearOperations.PATH_ADD,
                RcWearOperations.IMPULSE_START,
                RcWearOperations.IMPULSE_PROCESS,
                RcWearOperations.FUNCTION_CALL,
                RcWearOperations.FUNCTION_DEFINE,
                RcWearOperations.ATTRIBUTE_TEXT,
                RcWearOperations.ATTRIBUTE_IMAGE,
                RcWearOperations.ATTRIBUTE_TIME,
                RcWearOperations.CANVAS_OPERATIONS,
                RcWearOperations.MODIFIER_DRAW_CONTENT,
                RcWearOperations.PATH_COMBINE,
                RcWearOperations.LAYOUT_FIT_BOX,
                RcWearOperations.HAPTIC_FEEDBACK,
                RcWearOperations.CONDITIONAL_OPERATIONS,
                RcWearOperations.DEBUG_MESSAGE,
                RcWearOperations.ATTRIBUTE_COLOR,
                RcWearOperations.MATRIX_FROM_PATH,
                RcWearOperations.TEXT_SUBTEXT,
                RcWearOperations.BITMAP_TEXT_MEASURE,
                RcWearOperations.DRAW_BITMAP_TEXT_ANCHORED,
                RcWearOperations.REM,
                RcWearOperations.MATRIX_CONSTANT,
                RcWearOperations.MATRIX_EXPRESSION,
                RcWearOperations.MATRIX_VECTOR_MATH,
                RcWearOperations.DATA_FONT,
                RcWearOperations.ID_LOOKUP,
                RcWearOperations.PATH_EXPRESSION,
                RcWearOperations.DYNAMIC_FLOAT_LIST,
                RcWearOperations.UPDATE_DYNAMIC_FLOAT_LIST,
                RcWearOperations.TEXT_TRANSFORM,
                RcWearOperations.LAYOUT_ROOT,
                RcWearOperations.LAYOUT_CONTENT,
                RcWearOperations.LAYOUT_BOX,
                RcWearOperations.LAYOUT_ROW,
                RcWearOperations.LAYOUT_COLUMN,
                RcWearOperations.LAYOUT_CANVAS,
                RcWearOperations.LAYOUT_CANVAS_CONTENT,
                RcWearOperations.LAYOUT_TEXT,
                RcWearOperations.HOST_ACTION,
                RcWearOperations.HOST_NAMED_ACTION,
                RcWearOperations.MODIFIER_VISIBILITY,
                RcWearOperations.VALUE_INTEGER_CHANGE_ACTION,
                RcWearOperations.VALUE_STRING_CHANGE_ACTION,
                RcWearOperations.CONTAINER_END,
                RcWearOperations.LOOP_START,
                RcWearOperations.HOST_METADATA_ACTION,
                RcWearOperations.LAYOUT_STATE,
                RcWearOperations.VALUE_INTEGER_EXPRESSION_CHANGE_ACTION,
                RcWearOperations.MODIFIER_OFFSET,
                RcWearOperations.VALUE_FLOAT_CHANGE_ACTION,
                RcWearOperations.MODIFIER_ZINDEX,
                RcWearOperations.MODIFIER_GRAPHICS_LAYER,
                RcWearOperations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION,
                RcWearOperations.MODIFIER_MARQUEE,
                RcWearOperations.MODIFIER_RIPPLE,
                RcWearOperations.MODIFIER_WIDTH_IN,
                RcWearOperations.MODIFIER_HEIGHT_IN,
                RcWearOperations.LAYOUT_IMAGE,
                RcWearOperations.RUN_ACTION,
                RcWearOperations.MODIFIER_ALIGN_BY,
                RcWearOperations.ACCESSIBILITY_SEMANTICS,
            )

        /**
         * The safe, default renderer version, describing the renderer Host offering initial
         * RemoteCompose support, from when 3P widgets were first supported.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val SAFE_FALLBACK_VERSION: RendererVersion =
            RendererVersion(
                major = SAFE_FALLBACK_MAJOR,
                minor = SAFE_FALLBACK_MINOR,
                revision = SAFE_FALLBACK_REVISION,
                supportedOperations = SAFE_FALLBACK_SUPPORTED_OPERATIONS,
            )

        /**
         * Defines artificial max renderer version, i.e. the version that will always correspond to
         * the latest Host and set of supported operations.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val MAX_RENDERER_VERSION: RendererVersion =
            RendererVersion(
                major = 999,
                minor = 999,
                revision = 999,
                supportedOperations = intSetOf(),
            )

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

                return RendererVersion(major, minor, revision, SAFE_FALLBACK_SUPPORTED_OPERATIONS)
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
        @VisibleForTesting
        internal val PL_RENDERER_INITIAL_VERSION =
            RendererVersion(
                major = 1,
                minor = 0,
                revision = 0,
                supportedOperations = SAFE_FALLBACK_SUPPORTED_OPERATIONS,
            )
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
