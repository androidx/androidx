/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.CaptionBar
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.DisplayCutout
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Ime
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.MandatorySystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.NavigationBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SafeDrawing
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SafeGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.StatusBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SystemBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.TappableElement
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Waterfall

/**
 * Contains rulers used for window insets. The [current] values are available as well as values when
 * the insets are [fully visible][maximum].
 *
 * Other animation properties can be retrieved with [getAnimation].
 */
sealed interface WindowInsetsRulers {
    /**
     * The current values for the window insets RectRulers. Values for some insets may not be
     * provided on platforms that don't support specific Window Insets types. These also may not be
     * provided if no [WindowInsetsRulers] encroach on the content.
     *
     * @sample androidx.compose.ui.samples.WindowInsetsRulersSample
     */
    val current: RectRulers

    /**
     * The values for the insets when the insets are fully visible. The value does not change when
     * the insets are hidden. Values for some insets may not be provided on some platforms. For
     * example, values are never provided for [Ime] on Android. These may not be provided if no
     * [WindowInsetsRulers] encroach on the content.
     *
     * When no animations are active, [maximum] and [current] will have the same value if
     * [WindowInsetsAnimation.isVisible] is `true`. If `false`, then [maximum] will not be changed,
     * while [current] will have values the same as the Window borders. For example, when a status
     * bar is visible, its height may be intrude 100 pixels into the Window and [maximum]'s
     * [top][RectRulers.top] will be at 100 pixels for [StatusBars]. When the status bar is
     * invisible, [maximum] will have the same [top][RectRulers.top] value at 100 pixels, while
     * [current]'s [top][RectRulers.top] will be at 0 pixels.
     *
     * @sample androidx.compose.ui.samples.MaximumSample
     */
    val maximum: RectRulers

    /** Additional properties related to animating this [WindowInsetsRulers]. */
    fun getAnimation(scope: Placeable.PlacementScope): WindowInsetsAnimation

    companion object {
        /**
         * Rulers used for caption bar insets.
         *
         * See
         * [WindowInsetsCompat.Type.captionBar](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#captionBar())
         */
        val CaptionBar: WindowInsetsRulers = WindowInsetsRulersImpl("caption bar")

        /**
         * Rulers used for display cutout insets.
         *
         * This is the safe insets that avoid all display cutouts. To get the bounds of the display
         * cutouts themselves, use [Placeable.PlacementScope.getDisplayCutoutBounds].
         *
         * See
         * [WindowInsetsCompat.Type.displayCutout](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#displayCutout())
         */
        val DisplayCutout: WindowInsetsRulers = WindowInsetsRulersImpl("display cutout")

        /**
         * Rulers used for IME insets.
         *
         * See
         * [WindowInsetsCompat.Type.ime](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#ime())
         */
        val Ime: WindowInsetsRulers = WindowInsetsRulersImpl("ime")

        /**
         * Rulers used for mandatory system gestures insets.
         *
         * See
         * [WindowInsetsCompat.Type.mandatorySystemGestures](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#mandatorySystemGestures())
         */
        val MandatorySystemGestures: WindowInsetsRulers =
            WindowInsetsRulersImpl("mandatory system gestures")

        /**
         * Rulers used for navigation bars insets.
         *
         * See
         * [WindowInsetsCompat.Type.navigationBars](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#navigationBars())
         */
        val NavigationBars: WindowInsetsRulers = WindowInsetsRulersImpl("navigation bars")

        /**
         * Rulers used for status bars insets.
         *
         * See
         * [WindowInsetsCompat.Type.statusBars](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#statusBars())
         */
        val StatusBars: WindowInsetsRulers = WindowInsetsRulersImpl("status bars")

        /**
         * Rulers used for system bars insets, including [StatusBars], [NavigationBars], and
         * [CaptionBar].
         *
         * @sample androidx.compose.ui.samples.WindowInsetsRulersSample
         *
         * See
         * [WindowInsetsCompat.Type.systemBars](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#systemBars())
         */
        val SystemBars: WindowInsetsRulers =
            InnermostInsetsRulers("system bars", arrayOf(StatusBars, NavigationBars, CaptionBar))

        /**
         * Rulers used for system gestures insets.
         *
         * See
         * [WindowInsetsCompat.Type.systemGestures](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#systemGestures())
         */
        val SystemGestures: WindowInsetsRulers = WindowInsetsRulersImpl("system gestures")

        /**
         * Rulers used for tappable elements insets.
         *
         * See
         * [WindowInsetsCompat.Type.tappableElement](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type#tappableElement())
         */
        val TappableElement: WindowInsetsRulers = WindowInsetsRulersImpl("tappable element")

        /**
         * Rulers used for waterfall insets.
         *
         * See
         * [DisplayCutoutCompat.getWaterfallInsets](https://developer.android.com/reference/androidx/core/view/DisplayCutoutCompat#getWaterfallInsets())
         */
        val Waterfall: WindowInsetsRulers = WindowInsetsRulersImpl("waterfall")

        /**
         * Rulers used for insets including system bars, IME, and the display cutout.
         *
         * @see SystemBars
         * @see DisplayCutout
         * @see Ime
         * @see TappableElement
         */
        val SafeDrawing: WindowInsetsRulers =
            InnermostInsetsRulers(
                "safe drawing",
                arrayOf(StatusBars, NavigationBars, CaptionBar, DisplayCutout, Ime, TappableElement),
            )

        /**
         * Rulers used for insets that include places where gestures could conflict. This includes
         * [MandatorySystemGestures], [SystemGestures], [TappableElement], and [Waterfall].
         */
        val SafeGestures: WindowInsetsRulers =
            InnermostInsetsRulers(
                "safe gestures",
                arrayOf(MandatorySystemGestures, SystemGestures, TappableElement, Waterfall),
            )

        /**
         * Rulers used for insets that are safe for any content. This includes [SafeGestures] and
         * [SafeDrawing].
         */
        val SafeContent: WindowInsetsRulers =
            InnermostInsetsRulers(
                "safe content",
                arrayOf(
                    StatusBars,
                    NavigationBars,
                    CaptionBar,
                    Ime,
                    SystemGestures,
                    MandatorySystemGestures,
                    TappableElement,
                    DisplayCutout,
                    Waterfall,
                ),
            )

        /**
         * Merges the rulers in [windowInsetsRulers], providing the innermost values from [current]
         * and [maximum]. [getAnimation] will return values with only
         * [WindowInsetsAnimation.isVisible] and [WindowInsetsAnimation.isAnimating] set to
         * meaningful values.
         */
        fun innermostOf(vararg windowInsetsRulers: WindowInsetsRulers): WindowInsetsRulers =
            InnermostInsetsRulers(null, windowInsetsRulers)
    }
}

/**
 * Returns a List of [RectRulers], one [RectRulers] for each display cutout. Each [RectRulers]
 * provides values for the bounds of the display cutout. [WindowInsetsRulers.DisplayCutout] provides
 * the safe inset values for content avoiding all display cutouts.
 */
fun Placeable.PlacementScope.getDisplayCutoutBounds(): List<RectRulers> = findDisplayCutouts(this)

/** Provides properties related to animating [WindowInsetsRulers]. */
sealed interface WindowInsetsAnimation {
    /**
     * The starting insets values of the animation when the insets are animating
     * ([WindowInsetsAnimation.isAnimating] is `true`). When the insets are not animating, no ruler
     * values will be provided.
     *
     * @sample androidx.compose.ui.samples.SourceAndTargetInsetsSample
     */
    val source: RectRulers

    /**
     * The ending insets values of the animation when the insets are animating
     * ([WindowInsetsAnimation.isAnimating] is `true`). When the insets are not animating, no ruler
     * values will be provided.
     *
     * @sample androidx.compose.ui.samples.SourceAndTargetInsetsSample
     */
    val target: RectRulers

    /**
     * True when the Window Insets are visible. For example, for [StatusBars], when a status bar is
     * shown, [isVisible] will be `true`. When the status bar is hidden, [isVisible] will be
     * `false`. [isVisible] remains `true` during animations.
     */
    val isVisible: Boolean

    /** True when the Window Insets are currently being animated. */
    val isAnimating: Boolean

    /**
     * The current fraction of the animation if the Window Insets are being animated or `0` if
     * [isAnimating] is `false`. When animating, [fraction] typically ranges between `0` at the
     * start to `1` at the end, but it may go out of that range if an interpolator causes the
     * fraction to overshoot the range.
     */
    val fraction: Float

    /** The duration of the animation in milliseconds. */
    @get:IntRange(from = 0) val durationMillis: Long

    /**
     * The translucency of the animating window. This is used when Window Insets animate by fading
     * and can be used to have content match the fade.
     *
     * @sample androidx.compose.ui.samples.InsetsRulersAlphaSample
     */
    @get:FloatRange(from = 0.0, to = 1.0) val alpha: Float
}

internal expect fun findDisplayCutouts(placementScope: Placeable.PlacementScope): List<RectRulers>

internal expect fun findInsetsAnimationProperties(
    placementScope: Placeable.PlacementScope,
    windowInsetsRulers: WindowInsetsRulers,
): WindowInsetsAnimation

private class WindowInsetsRulersImpl(val name: String) : WindowInsetsRulers {
    override val current = RectRulers(name)

    override val maximum = RectRulers("$name maximum")

    /** Additional properties related to animating this InsetsRulers. */
    override fun getAnimation(scope: Placeable.PlacementScope): WindowInsetsAnimation =
        findInsetsAnimationProperties(scope, this)

    override fun toString(): String = name
}

private class InnermostInsetsRulers(val name: String?, val rulers: Array<out WindowInsetsRulers>) :
    WindowInsetsRulers {
    override val current: RectRulers =
        RectRulers.innermostOf(*rulers.map { it.current }.toTypedArray())
    override val maximum: RectRulers =
        RectRulers.innermostOf(*rulers.map { it.maximum }.toTypedArray())

    override fun getAnimation(scope: Placeable.PlacementScope): WindowInsetsAnimation =
        InnermostAnimationProperties(scope, rulers)

    override fun toString(): String =
        name ?: rulers.joinToString(prefix = "innermostOf(", postfix = ")")
}

/**
 * This interface is here to work around having the `sealed interface` in common, but having the
 * implementation in platform-specific code.
 */
internal interface PlatformWindowInsetsAnimation : WindowInsetsAnimation

internal val NeverProvidedRectRulers = RectRulers()

internal object NoWindowInsetsAnimation : WindowInsetsAnimation {
    override val source: RectRulers
        get() = NeverProvidedRectRulers

    override val target: RectRulers
        get() = NeverProvidedRectRulers

    override val isVisible: Boolean
        get() = true

    override val isAnimating: Boolean
        get() = false

    override val fraction: Float
        get() = 0f

    override val durationMillis: Long
        get() = 0L

    override val alpha: Float
        get() = 1f
}

private class InnermostAnimationProperties(
    val scope: Placeable.PlacementScope,
    val rulers: Array<out WindowInsetsRulers>,
) : WindowInsetsAnimation {
    override val source: RectRulers
        get() = NeverProvidedRectRulers

    override val target: RectRulers
        get() = NeverProvidedRectRulers

    override val isVisible: Boolean
        get() = rulers.any { it.getAnimation(scope).isVisible }

    override val isAnimating: Boolean
        get() = rulers.any { it.getAnimation(scope).isAnimating }

    override val fraction: Float
        get() = 0f

    override val durationMillis: Long
        get() = 0L

    override val alpha: Float
        get() = 1f
}
