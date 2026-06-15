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

package androidx.compose.remote.integration.view.demos.dsl.graph2d.lib

/**
 * The complete visual style for a chart. Every color, font size, stroke width, spacing, fill
 * opacity and per-family geometry knob the engine uses lives here, so apps can fully restyle
 * charts. Pass a ready-made preset ([GraphTheme.Light] / [GraphTheme.Dark]) or `.copy(...)` one to
 * tweak; every field has a default, so you only override what you care about.
 *
 * ```
 * val house = GraphTheme.Light.copy(
 *     axisColor = 0xFF333333.toInt(), axisStroke = 2f,   // thicker, darker axes
 *     labelSize = 16f, palette = brandColors,            // bigger labels, brand series colors
 *     donutThicknessFrac = 0.5f, gaugeSweepDeg = 240f,   // per-family geometry
 * )
 * barChart(cats, vals, theme = house)
 * ```
 *
 * All colors are ARGB ints; sizes/strokes are document pixels; `*Frac` values are fractions (of a
 * band width, radius, etc.); `*Alpha` are fill opacities in `0..1`. This type is plain data — it
 * never touches the DSL — so the marks read style without a canvas in scope.
 */
@Suppress("ArrayInDataClass")
public data class GraphTheme(
    // ----- Surfaces -----
    val background: Int = 0xFFFFFFFF.toInt(),
    val plotBackground: Int = 0x00000000,
    // ----- Axes / grid / ticks -----
    val axisColor: Int = 0xFF9AA0A6.toInt(),
    val gridColor: Int = 0xFFECEEF1.toInt(),
    val zeroLineColor: Int = 0xFFBDC1C6.toInt(),
    val axisStroke: Float = 1.5f,
    val gridStroke: Float = 1f,
    val tickLen: Float = 5f,
    // ----- Text colors -----
    val titleColor: Int = 0xFF202124.toInt(),
    val subtitleColor: Int = 0xFF5F6368.toInt(),
    val labelColor: Int = 0xFF5F6368.toInt(),
    val axisTitleColor: Int = 0xFF3C4043.toInt(),
    val valueLabelColor: Int = 0xFF3C4043.toInt(),
    // ----- Font sizes (px) -----
    val titleSize: Float = 26f,
    val subtitleSize: Float = 15f,
    val axisTitleSize: Float = 15f,
    val labelSize: Float = 13f,
    val valueLabelSize: Float = 12f,
    val legendSize: Float = 14f,
    // ----- Layout (px) -----
    val outerPad: Float = 24f,
    val labelGap: Float = 8f,
    // ----- Series defaults / palette -----
    val palette: IntArray = DefaultPalette,
    val lineStroke: Float = 2.5f,
    val markerRadius: Float = 4f,
    val areaFillAlpha: Float = 0.28f,
    val bandFillAlpha: Float = 0.22f,
    val splineSamples: Int = 48,
    // ----- Bars -----
    val barPadInner: Float = 0.2f,
    val barPadOuter: Float = 0.1f,
    val barGroupGap: Float = 0.08f,
    val lollipopDotScale: Float = 2.2f,
    // ----- Scatter / bubble -----
    val pointAlpha: Float = 0.85f,
    val bubbleAlpha: Float = 0.55f,
    val bubbleMinRadius: Float = 5f,
    val bubbleMaxRadius: Float = 26f,
    // ----- Distribution -----
    val boxWidthFrac: Float = 0.32f,
    val boxCapFrac: Float = 0.16f,
    val boxFillAlpha: Float = 0.25f,
    val outlierRadius: Float = 2.5f,
    val violinWidthFrac: Float = 0.45f,
    val violinFillAlpha: Float = 0.5f,
    val stripJitterFrac: Float = 0.34f,
    val stripDotRadius: Float = 3f,
    val stripAlpha: Float = 0.7f,
    val densityFillAlpha: Float = 0.3f,
    val ridgelineOverlap: Float = 1.9f,
    val ridgelineFillAlpha: Float = 0.78f,
    // ----- Polar (pie / donut / gauge / radial / rose / radar) -----
    val pieBorderStroke: Float = 2f,
    val pieLabelRadiusFrac: Float = 0.6f,
    val donutThicknessFrac: Float = 0.42f,
    val roseFillAlpha: Float = 0.85f,
    val gaugeStartDeg: Float = 135f,
    val gaugeSweepDeg: Float = 270f,
    val gaugeThicknessFrac: Float = 0.16f,
    val gaugeTrackFrac: Float = 0.84f,
    val radialSweepDeg: Float = 270f,
    val radialRingThicknessFrac: Float = 0.62f,
    val radialTrackAlpha: Float = 0.18f,
    val polarBarSweepFrac: Float = 0.62f,
    val polarGridRings: Int = 4,
    val radarFillAlpha: Float = 0.25f,
    // ----- Financial -----
    val candleBodyFrac: Float = 0.34f,
    val candleWickStroke: Float = 1.5f,
    // ----- Interval / forecast -----
    val fanBaseAlpha: Float = 0.14f,
    val fanAlphaStep: Float = 0.13f,
    val errorBarCapFrac: Float = 0.18f,
    val errorBarPointRadius: Float = 4f,
    val forestMarkerMin: Float = 4f,
    val forestMarkerMax: Float = 11f,
    // ----- Grid / matrix -----
    val sequentialRamp: IntArray = Palette.Blues,
    val cellGap: Float = 1f,
    val inkFlipThreshold: Float = 0.55f,
    val waffleRows: Int = 10,
    val waffleCols: Int = 10,
    val waffleCellGapFrac: Float = 0.12f,
    val treemapGap: Float = 1f,
    // ----- Likert -----
    val likertPalette: IntArray = Palette.Diverging,
    val likertBarFrac: Float = 0.34f,
    // ----- Annotations / reference marks -----
    /** Dash intervals for gridlines; null = solid. */
    val gridDash: FloatArray? = null,
    val refBandAlpha: Float = 0.12f,
    val annotationDotRadius: Float = 3.5f,
    // ----- Interaction (touch crosshair) -----
    val crosshairStroke: Float = 1.5f,
    val crosshairMarkerRadius: Float = 6.5f,
    // ----- Animation -----
    val animateDurationSec: Float = 0.9f,
) {
    /** Series color [i], cycling the palette. */
    public fun seriesColor(i: Int): Int =
        palette[((i % palette.size) + palette.size) % palette.size]

    public companion object {
        /** Curated categorical palette (Material-ish, high-contrast on light + dark). */
        public val DefaultPalette: IntArray =
            intArrayOf(
                0xFF4C78A8.toInt(), // blue
                0xFFF58518.toInt(), // orange
                0xFF54A24B.toInt(), // green
                0xFFE45756.toInt(), // red
                0xFF72B7B2.toInt(), // teal
                0xFFB279A2.toInt(), // purple
                0xFFFF9DA6.toInt(), // pink
                0xFF9D755D.toInt(), // brown
                0xFFEECA3B.toInt(), // yellow
                0xFFBAB0AC.toInt(), // grey
            )

        /** Clean light theme: white surface, dark ink, soft grey gridlines (the field defaults). */
        public val Light: GraphTheme = GraphTheme()

        /** Dark dashboard theme (light theme with surface + ink colors flipped). */
        public val Dark: GraphTheme =
            Light.copy(
                background = 0xFF1C1C1E.toInt(),
                axisColor = 0xFF5F6368.toInt(),
                gridColor = 0xFF2C2C2E.toInt(),
                zeroLineColor = 0xFF48484A.toInt(),
                titleColor = 0xFFFFFFFF.toInt(),
                subtitleColor = 0xFFAEAEB2.toInt(),
                labelColor = 0xFFAEAEB2.toInt(),
                axisTitleColor = 0xFFD1D1D6.toInt(),
                valueLabelColor = 0xFFE5E5EA.toInt(),
            )
    }
}
