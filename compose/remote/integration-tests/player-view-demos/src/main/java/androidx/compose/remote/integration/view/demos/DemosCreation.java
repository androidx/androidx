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

package androidx.compose.remote.integration.view.demos;

import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.get;
import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.getp;
import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.getpc;
import static androidx.compose.remote.integration.view.demos.examples.RcTickerKt.RcTicker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;

import androidx.compose.remote.integration.view.demos.dsl.DslCollapsiblePriorityDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslCustomComponentDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslCustomComposeDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizActivityRingsKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizBatteryRadialGaugeKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizCalendarHeatmapKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizHeartRateKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizHydrationWaveKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizMoonPhaseDialKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizSleepQualityRingsKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizStepProgressArcKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizStockSparklineKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDataVizWeatherForecastKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDemoAnchorTextKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDemoAttributedStringKt;
import androidx.compose.remote.integration.view.demos.dsl.DslDrawWithContentDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslExampleTimerKt;
import androidx.compose.remote.integration.view.demos.dsl.DslFontAxisDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslHostileActorKt;
import androidx.compose.remote.integration.view.demos.dsl.DslLayoutComputeDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslModernShowcaseDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslPieChartKt;
import androidx.compose.remote.integration.view.demos.dsl.DslPlotDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.DslPressureGaugeKt;
import androidx.compose.remote.integration.view.demos.dsl.DslRcSimpleSwitchKt;
import androidx.compose.remote.integration.view.demos.dsl.DslServerSideKt;
import androidx.compose.remote.integration.view.demos.dsl.DslSmallAnimatedKt;
import androidx.compose.remote.integration.view.demos.dsl.DslSpreadSheetKt;
import androidx.compose.remote.integration.view.demos.dsl.DslStopwatchKt;
import androidx.compose.remote.integration.view.demos.dsl.DslSysVarKt;
import androidx.compose.remote.integration.view.demos.dsl.DslTextDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.DslTouchDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslClockKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslCountdownKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslEnumsDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslTickerKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dAnnotationDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dBarDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dDistributionDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dExtraDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dFinancialDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dIntervalDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dLineDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dMoreDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dPolarDemosKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dRelationDemosKt;
import androidx.compose.remote.integration.view.demos.examples.BadExamples.DemoMemorySkipKt;
import androidx.compose.remote.integration.view.demos.examples.BadExamples.MemoryKt;
import androidx.compose.remote.integration.view.demos.examples.ColorCheckKt;
import androidx.compose.remote.integration.view.demos.examples.ColorThemeCheckKt;
import androidx.compose.remote.integration.view.demos.examples.CountdownKt;
import androidx.compose.remote.integration.view.demos.examples.Cube3DKt;
import androidx.compose.remote.integration.view.demos.examples.CubeSphereTransitionShader2Kt;
import androidx.compose.remote.integration.view.demos.examples.CubeSphereTransitionShaderKt;
import androidx.compose.remote.integration.view.demos.examples.DataVizDemosKt;
import androidx.compose.remote.integration.view.demos.examples.DemoAnchorText;
import androidx.compose.remote.integration.view.demos.examples.DemoAttributedString;
import androidx.compose.remote.integration.view.demos.examples.DemoColorKt;
import androidx.compose.remote.integration.view.demos.examples.DemoGlobalKt;
import androidx.compose.remote.integration.view.demos.examples.DemoGraphsKt;
import androidx.compose.remote.integration.view.demos.examples.DemoKt;
import androidx.compose.remote.integration.view.demos.examples.DemoMetalClockKt;
import androidx.compose.remote.integration.view.demos.examples.DemoParticlesKt;
import androidx.compose.remote.integration.view.demos.examples.DemoPaths;
import androidx.compose.remote.integration.view.demos.examples.DemoTextTransformKt;
import androidx.compose.remote.integration.view.demos.examples.DemoThemeKt;
import androidx.compose.remote.integration.view.demos.examples.DemoTouchKt;
import androidx.compose.remote.integration.view.demos.examples.DemotSystemVarKt;
import androidx.compose.remote.integration.view.demos.examples.ExampleNumbersKt;
import androidx.compose.remote.integration.view.demos.examples.ExampleTimerKt;
import androidx.compose.remote.integration.view.demos.examples.FontCheckKt;
import androidx.compose.remote.integration.view.demos.examples.HostileActor;
import androidx.compose.remote.integration.view.demos.examples.LinearRegressionKt;
import androidx.compose.remote.integration.view.demos.examples.MClockKt;
import androidx.compose.remote.integration.view.demos.examples.MoonPhasesKt;
import androidx.compose.remote.integration.view.demos.examples.ParticleDotsKt;
import androidx.compose.remote.integration.view.demos.examples.ParticleSphereKt;
import androidx.compose.remote.integration.view.demos.examples.Particles3Kt;
import androidx.compose.remote.integration.view.demos.examples.PieChartKt;
import androidx.compose.remote.integration.view.demos.examples.PlotWaveKt;
import androidx.compose.remote.integration.view.demos.examples.PressureGaugeKt;
import androidx.compose.remote.integration.view.demos.examples.RCPlayerInfoKt;
import androidx.compose.remote.integration.view.demos.examples.ServerSideKt;
import androidx.compose.remote.integration.view.demos.examples.ShaderCalendarKt;
import androidx.compose.remote.integration.view.demos.examples.SimpleShader2Kt;
import androidx.compose.remote.integration.view.demos.examples.SimpleShaderKt;
import androidx.compose.remote.integration.view.demos.examples.SmallAnimated;
import androidx.compose.remote.integration.view.demos.examples.SphereTimeShaderKt;
import androidx.compose.remote.integration.view.demos.examples.TextKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoBoxKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoCollapsibleColumnKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoCollapsibleRowKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoColumnKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoFitBoxKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoFlowKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoImageKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierAlignByBaselineKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierBackgroundIdKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierBackgroundKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierBorderKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierClipCircleKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierClipRectKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierClipRoundedRectKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierCollapsiblePriorityKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierComponentIdKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierComputeMeasureKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierComputePositionKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierDynamicBorderKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierFillMaxHeightKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierFillMaxSizeKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierFillMaxWidthKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierFillParentMaxHeightKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierFillParentMaxSizeKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierFillParentMaxWidthKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierHeightInKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierHeightKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierHorizontalScrollKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierHorizontalWeightKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierOnClickKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierOnTouchCancelKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierOnTouchDownKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierOnTouchUpKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierPaddingKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierSizeKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierSpacedByKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierVerticalScrollKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierVerticalWeightKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierVisibilityKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierWidthInKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierWidthKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierWrapContentHeightKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierWrapContentSizeKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierWrapContentWidthKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoModifierZIndexKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoRowKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoStateLayoutKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoTextAutoSizeKt;
import androidx.compose.remote.integration.view.demos.examples.components.DemoTextKt;
import androidx.compose.remote.integration.view.demos.examples.old.BasicProceduralDemos;
import androidx.compose.remote.integration.view.demos.examples.old.BitmapFontWatch;
import androidx.compose.remote.integration.view.demos.examples.old.ClockDemo1;
import androidx.compose.remote.integration.view.demos.examples.old.ClockDemo2;
import androidx.compose.remote.integration.view.demos.examples.old.DemoBitmapDrawing;
import androidx.compose.remote.integration.view.demos.examples.old.DemoFlick;
import androidx.compose.remote.integration.view.demos.examples.old.DemoPathExpression;
import androidx.compose.remote.integration.view.demos.examples.old.DemoWindingRule;
import androidx.compose.remote.integration.view.demos.examples.old.FancyClocks;
import androidx.compose.remote.integration.view.demos.examples.old.FlowControlChecks;
import androidx.compose.remote.integration.view.demos.examples.old.Graph;
import androidx.compose.remote.integration.view.demos.examples.old.HapticDemo;
import androidx.compose.remote.integration.view.demos.examples.old.ImpulseDemo;
import androidx.compose.remote.integration.view.demos.examples.old.IndexingDemo;
import androidx.compose.remote.integration.view.demos.examples.old.PathDemo;
import androidx.compose.remote.integration.view.demos.examples.old.PathProceduralChecks;
import androidx.compose.remote.integration.view.demos.examples.old.SensorDemo;
import androidx.compose.remote.integration.view.demos.examples.old.SplineDemo;
import androidx.compose.remote.integration.view.demos.examples.old.TextureDemo;
import androidx.compose.remote.integration.view.demos.examples.old.WakeDemo;
import androidx.compose.remote.integration.view.demos.utils.RCDoc;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class DemosCreation {
    /**
     * get the demos written at the creational or procedural level
     *
     * @return a list of RCDoc
     */
    public static @NonNull ArrayList<RCDoc> getDemos(@NonNull Activity activity, int types) {
        Bitmap bitmap = simpleBitmap(100);
        boolean graph2d = true;
        if (graph2d) {
            return new ArrayList<>(Arrays.asList(
                    // Annotation Demos
                    get("1/000/annotations/basic", Graph2dAnnotationDemosKt::graph2dAnnotations),
                    get("1/001/annotations/dualAxis", Graph2dAnnotationDemosKt::graph2dDualAxis),
                    get("1/002/annotations/logScale", Graph2dAnnotationDemosKt::graph2dLogScale),
                    get("1/003/annotations/timeAxis", Graph2dAnnotationDemosKt::graph2dTimeAxis),
                    get("1/004/annotations/styleExtras",
                            Graph2dAnnotationDemosKt::graph2dStyleExtras),

                    // Bar Demos
                    get("1/005/bar/basic", Graph2dBarDemosKt::graph2dBarBasic),
                    get("1/006/bar/horizontal", Graph2dBarDemosKt::graph2dBarHorizontal),
                    get("1/007/bar/grouped", Graph2dBarDemosKt::graph2dBarGrouped),
                    get("1/008/bar/stacked", Graph2dBarDemosKt::graph2dBarStacked),
                    get("1/009/bar/percentStacked", Graph2dBarDemosKt::graph2dBarPercentStacked),
                    get("1/000/bar/lollipop", Graph2dBarDemosKt::graph2dBarLollipop),
                    get("1/011/bar/diverging", Graph2dBarDemosKt::graph2dBarDiverging),
                    get("1/012/bar/dark", Graph2dBarDemosKt::graph2dBarDark),
                    get("1/013/bar/animated", Graph2dBarDemosKt::graph2dBarAnimated),
                    get("1/014/bar/themeCustom", Graph2dBarDemosKt::graph2dThemeCustom),
                    get("1/015/bar/combo", Graph2dBarDemosKt::graph2dCombo),
                    get("1/016/bar/showcase", Graph2dBarDemosKt::graph2dBarShowcase),

                    // Distribution Demos
                    get("1/017/distribution/histogram",
                            Graph2dDistributionDemosKt::graph2dHistogram),
                    get("1/018/distribution/density", Graph2dDistributionDemosKt::graph2dDensity),
                    get("1/019/distribution/ecdf", Graph2dDistributionDemosKt::graph2dEcdf),
                    get("1/020/distribution/boxPlot", Graph2dDistributionDemosKt::graph2dBoxPlot),
                    get("1/021/distribution/violin", Graph2dDistributionDemosKt::graph2dViolin),
                    get("1/022/distribution/strip", Graph2dDistributionDemosKt::graph2dStrip),
                    // Extra Demos
                    get("1/023/extra/heatmap", Graph2dExtraDemosKt::graph2dHeatmap),
                    get("1/024/extra/waffle", Graph2dExtraDemosKt::graph2dWaffle),
                    get("1/025/extra/dotPlot", Graph2dExtraDemosKt::graph2dDotPlot),
                    get("1/026/extra/dumbbell", Graph2dExtraDemosKt::graph2dDumbbell),
                    get("1/027/extra/slope", Graph2dExtraDemosKt::graph2dSlope),
                    get("1/028/extra/pyramid", Graph2dExtraDemosKt::graph2dPyramid),
                    get("1/029/extra/candlestick", Graph2dExtraDemosKt::graph2dCandlestick),
                    get("1/030/extra/regression", Graph2dExtraDemosKt::graph2dRegression),
                    get("1/031/extra/roc", Graph2dExtraDemosKt::graph2dRoc),

                    // Financial Demos
                    get("1/032/financial/waterfall", Graph2dFinancialDemosKt::graph2dWaterfall),
                    get("1/033/financial/funnel", Graph2dFinancialDemosKt::graph2dFunnel),
                    get("1/034/financial/bullet", Graph2dFinancialDemosKt::graph2dBullet),
                    get("1/035/financial/pareto", Graph2dFinancialDemosKt::graph2dPareto),
                    // Interval Demos
                    get("1/036/interval/band", Graph2dIntervalDemosKt::graph2dBand),
                    get("1/037/interval/fan", Graph2dIntervalDemosKt::graph2dFan),
                    get("1/038/interval/errorBar", Graph2dIntervalDemosKt::graph2dErrorBar),
                    get("1/039/interval/forest", Graph2dIntervalDemosKt::graph2dForest),

                    // Line Demos
                    get("1/030/line/basic", Graph2dLineDemosKt::graph2dLineBasic),
                    get("1/031/line/multi", Graph2dLineDemosKt::graph2dLineMulti),
                    get("1/032/line/spline", Graph2dLineDemosKt::graph2dLineSpline),
                    get("1/033/line/step", Graph2dLineDemosKt::graph2dLineStep),
                    get("1/034/line/areaBasic", Graph2dLineDemosKt::graph2dAreaBasic),
                    get("1/035/line/areaStacked", Graph2dLineDemosKt::graph2dAreaStacked),
                    get("1/036/line/areaPercent", Graph2dLineDemosKt::graph2dAreaPercent),
                    get("1/037/line/dark", Graph2dLineDemosKt::graph2dLineDark),
                    get("1/038/line/animated", Graph2dLineDemosKt::graph2dLineAnimated),
                    get("1/039/line/interactive", Graph2dLineDemosKt::graph2dLineInteractive),

                    // More Demos
                    get("1/040/more/treemap", Graph2dMoreDemosKt::graph2dTreemap),
                    get("1/041/more/confusion", Graph2dMoreDemosKt::graph2dConfusion),
                    get("1/042/more/quadrant", Graph2dMoreDemosKt::graph2dQuadrant),
                    get("1/043/more/function", Graph2dMoreDemosKt::graph2dFunction),
                    get("1/044/more/qq", Graph2dMoreDemosKt::graph2dQQ),
                    get("1/045/more/gantt", Graph2dMoreDemosKt::graph2dGantt),
                    get("1/046/more/ridgeline", Graph2dMoreDemosKt::graph2dRidgeline),
                    get("1/047/more/likert", Graph2dMoreDemosKt::graph2dLikert),

                    // Polar Demos
                    get("1/048/polar/gauge", Graph2dPolarDemosKt::graph2dGauge),
                    get("1/049/polar/radialBar", Graph2dPolarDemosKt::graph2dRadialBar),
                    get("1/050/polar/rose", Graph2dPolarDemosKt::graph2dRose),
                    get("1/051/polar/bar", Graph2dPolarDemosKt::graph2dPolarBar),

                    // Relation Demos
                    get("1/052/relation/pie", Graph2dRelationDemosKt::graph2dPie),
                    get("1/053/relation/donut", Graph2dRelationDemosKt::graph2dDonut),
                    get("1/054/relation/radar", Graph2dRelationDemosKt::graph2dRadar),
                    get("1/055/relation/scatter", Graph2dRelationDemosKt::graph2dScatter),
                    get("1/056/relation/bubble", Graph2dRelationDemosKt::graph2dBubble),
                    get("1/057/relation/connectedScatter",
                            Graph2dRelationDemosKt::graph2dConnectedScatter)
            ));
        }
        ArrayList<RCDoc> demos = new ArrayList<>();

        boolean customCompose = (types & 1) != 0;
        if (customCompose) {
            demos.addAll(Arrays.asList(
                    get("0/001/DslCustomComposeDemo",
                            DslCustomComposeDemoKt::dslCustomComposeDemo)
            ));
        }
        boolean customViews = (types & 2) != 0;

        if (customViews) {
            demos.addAll(Arrays.asList(
                    get("0/000a/DslCustomComponentDemo",
                            DslCustomComponentDemoKt::dslCustomComponentDemo)
            ));
        }


        boolean dsl1 = (types & 4) != 0;
        if (dsl1) {
            demos.addAll(Arrays.asList(
                    get("0/000/ModernShowcaseDemo",
                            DslModernShowcaseDemoKt::dslModernShowcaseDemo),
                    get("1/0001/dslStopwatch", DslStopwatchKt::dslStopwatchDemo),

                    getpc("1/002/CubeSphereTransitionShader",
                            CubeSphereTransitionShader2Kt::createCubeSphereTransitionShader2),
                    getpc("1/003/CubeSphereTransitionShader",
                            CubeSphereTransitionShaderKt::createCubeSphereTransitionShader),
                    get("1/001/DslCustomComposeDemo",
                            DslCustomComposeDemoKt::dslCustomComposeDemo),
                    get("1/004/DslDrawWithContentDemo",
                            DslDrawWithContentDemoKt::dslDrawWithContentDemo),
                    get("1/005/DslCollapsiblePriorityDemo",
                            DslCollapsiblePriorityDemoKt::dslCollapsiblePriorityDemo),
                    get("1/006/DslLayoutComputeDemo", DslLayoutComputeDemoKt::dslLayoutComputeDemo),
                    get("1/007/DslFontAxisDemo", DslFontAxisDemoKt::dslFontAxisDemo),
                    getpc("1/008/SphereTimeShader", SphereTimeShaderKt::sphereTimeShader),
                    get("1/009/Ticker", RcDslTickerKt::dslTicker),
                    get("1/010/DslTouchDemo", DslTouchDemoKt::dslTouchDemo),
                    getpc("1/011/particleSphere", ParticleSphereKt::particleSphere),

                    get("1/02/DataVizActivityRings",
                            DslDataVizActivityRingsKt::dslDemoActivityRings),
                    get("1/03/DataVizBatteryRadialGauge",
                            DslDataVizBatteryRadialGaugeKt::dslDemoBatteryRadialGauge),
                    getp("1/03/DataVizBatteryRadialGauge_og",
                            DataVizDemosKt::demoBatteryRadialGauge),

                    get("1/04/DataVizCalendarHeatmap",
                            DslDataVizCalendarHeatmapKt::dslDemoCalendarHeatmap),
                    getp("1/04/DataVizCalendarHeatmap_og", DataVizDemosKt::demoCalendarHeatmapGrid),

                    get("1/05/DataVizHeartRate", DslDataVizHeartRateKt::dslDemoHeartRateTimeline),
                    get("1/06/DataVizHydrationWave",
                            DslDataVizHydrationWaveKt::dslDemoHydrationWave),
                    get("1/07/DataVizMoonPhaseDial",
                            DslDataVizMoonPhaseDialKt::dslDemoMoonPhaseDial),
                    get("1/08/DataVizSleepQualityRings",
                            DslDataVizSleepQualityRingsKt::dslDemoSleepQualityRings),
                    get("1/09/DataVizStepProgressArc",
                            DslDataVizStepProgressArcKt::dslDemoStepProgressArc),
                    get("1/10/DataVizStockSparkline",
                            DslDataVizStockSparklineKt::dslDemoStockSparkline),
                    get("1/11/DataVizWeatherForecast",
                            DslDataVizWeatherForecastKt::dslDemoWeatherForecast),
                    get("1/12/DemoAnchorText", DslDemoAnchorTextKt::demoAnchorText),
                    get("1/13/DemoAttributedString",
                            DslDemoAttributedStringKt::demoAttributedString),
                    getp("1/15/DemoGraphs_og", DemoGraphsKt::demoGraphs),
                    getp("1/16/DemoGraphs2_og", DemoGraphsKt::demoGraphs2),
//                    get("1/17/SimpleJavaAnim", DslDemoTouchKt::dslSimpleJavaAnim),
                    getp("1/17/simpleJavaAnim_og", DemoTouchKt::simpleJavaAnim),
//
//                    get("1/18/DemoTouch1", DslDemoTouchKt::dslDemoTouch1),
//                    get("1/19/DemoTouch2", DslDemoTouchKt::dslDemoTouch2),
//                    get("1/20/TouchStopGently", DslDemoTouchKt::dslTouchStopGently),
//                    get("1/21/TouchStopEnds", DslDemoTouchKt::dslTouchStopEnds),
//                    get("1/22/TouchStopInstantly", DslDemoTouchKt::dslTouchStopInstantly),
//                    get("1/23/TouchStopNotchesEven", DslDemoTouchKt::dslTouchStopNotchesEven),
//                    get("1/24/TouchStopNotchesPercents",
//                    DslDemoTouchKt::dslTouchStopNotchesPercents),
//                    get("1/25/TouchStopNotchesAbsolute",
//                    DslDemoTouchKt::dslTouchStopNotchesAbsolute),
//                    get("1/26/TouchStopAbsolutePos", DslDemoTouchKt::dslTouchStopAbsolutePos),
                    get("1/27/BasicTimer", DslExampleTimerKt::dslBasicTimer),
                    getpc("1/28/LinearRegression3", LinearRegressionKt::demoLinearRegression),

                    get("1/29/PieChart", DslPieChartKt::dslPieChart),
                    getpc("1/29/PieChart_og", PieChartKt::demoPieChart),

                    get("1/30/Plot3", DslPlotDemosKt::dslPlot3),
                    get("1/31/Plot4", DslPlotDemosKt::dslPlot4),
                    get("1/32/PressureGauge", DslPressureGaugeKt::dslDemoPressureGauge),

//                    get("1/0/RcScrollview", DslRcScrollviewKt::dslRcScrollview),
                    get("1/37/RcSimpleSwitch", DslRcSimpleSwitchKt::dslRcSimpleSwitchDemo),
                    get("1/38/ServerClock", DslServerSideKt::dslServerClock),
//                    getpc("1/0/SimpleShader2", () -> DslSimpleShader2Kt.dslSimpleShader2(bitmap)),
//                    get("1/0/SlantedButton", DslSlantedButtonKt::dslSlantedButtonDemo),
                    get("1/40/SmallAnimated", DslSmallAnimatedKt::smallAnimated),
                    get("1/41/SpreadSheet", DslSpreadSheetKt::dslSpreadSheet),
                    getp("1/41/SpreadSheet_og", ExampleNumbersKt::spreadSheet),

                    get("1/42/SysVar", DslSysVarKt::dslSysVar),
                    getp("1/42/SysVar_og", DemotSystemVarKt::sysVar),

                    get("1/43/RcTextDemo", DslTextDemoKt::dslRcTextDemo),
                    getpc("1/43/RcTextDemo_og", TextKt::RcTextDemo),

                    get("1/44/Clock", RcDslClockKt::dslClock),
                    get("1/45/Countdown", RcDslCountdownKt::dslCountdown),
                    get("1/46/Demo", RcDslDemoKt::dslDemo),
                    get("1/47/Theme1", RcDslDemoKt::dslTheme1),
                    get("1/48/Theme2", RcDslDemoKt::dslTheme2),
                    get("1/49/SimpleDemo", RcDslDemoKt::dslSimpleDemo),
                    get("1/50/SimpleClock", RcDslDemoKt::dslSimpleClock),
                    get("1/51/EnumsDemo", RcDslEnumsDemoKt::enumsDemo),
                    get("1/53/Ticker", RcDslTickerKt::dslTicker),
                    get("1/54/DslTouchDemo", DslTouchDemoKt::dslTouchDemo),
                    get("1/55/DslFontAxisDemo", DslFontAxisDemoKt::dslFontAxisDemo),
                    get("1/56/dslAttributeString", DslDemoAttributedStringKt::demoAttributedString),
                    get("1/57/00enumsDemo", RcDslEnumsDemoKt::enumsDemo),
                    get("1/58/01dslClock", RcDslClockKt::dslClock),
                    get("1/59/02dslDemo", RcDslDemoKt::dslDemo),
                    get("1/60/03dslTickerPreview", RcDslTickerKt::dslTicker),

                    get("1/61/dslTheme1", RcDslDemoKt::dslTheme1),
                    get("1/62/dslTheme2", RcDslDemoKt::dslTheme2)
            ));
        }
        boolean dsl2 = (types & 8) != 0;
        if (dsl2) {
            demos.addAll(Arrays.asList(
                    getp("2/01/AttributeString", DemoAttributedString::demo),
                    get("2/02/demoAnchorText", DslDemoAnchorTextKt::demoAnchorText),
                    get("2/03/demoImageColor", DslHostileActorKt::demoImageColor),
                    get("2/04/demoImage", DslHostileActorKt::demoImage),
                    get("2/05/smallAnimated", DslSmallAnimatedKt::smallAnimated),

                    get("2/08/00dslCountdown1", RcDslCountdownKt::dslCountdown),
                    getpc("2/09/00dslCountdown2", CountdownKt::countDown),
                    get("2/10/00dslTheme1", RcDslDemoKt::dslTheme1),
                    get("2/11/00dslTheme2", RcDslDemoKt::dslTheme2),
                    get("2/12/dslSimpleClock", RcDslDemoKt::dslSimpleClock),
                    get("2/13/dslSimpleDemo", RcDslDemoKt::dslSimpleDemo),

                    //get("2/0/00dslCalenda", RcDslCalendarDemoKt::dslCalendarDayAgenda),
                    //get("2/0/00dslDemoGraphs2", DslDemoGraphsKt::dslDemoGraphs2),
                    // get("2/0/00dslDemoGraphs", DslDemoGraphsKt::dslDemoGraphs),
                    // get("2/0/00dslLinearRegression", DslLinearRegressionKt::dslLinearRegression),

                    getp("1/1/04DemotSystemVarKt", DemotSystemVarKt::sysVar),
                    getp("1/1/skip", DemoMemorySkipKt::skip1),
                    getp("8/1/DemoTheme", DemoThemeKt::theme1),
                    getpc("1/1/0Shade1", SimpleShaderKt::createShaderDoc1),
                    getpc("1/1/0Shade2", () -> SimpleShaderKt.createShaderDoc2(bitmap)),
                    getpc("1/1/0Shade3", () -> SimpleShader2Kt.createShaderDoc3(bitmap)),
                    getpc("1/1/0Shade4", () -> SimpleShader2Kt.createShaderDoc4(bitmap)),
                    getp("1/1/colorCheck1", ColorCheckKt::colorCheck1),
                    getp("1/1/colorCheck2", ColorCheckKt::colorCheck2),
                    getp("1/1/colorCheck3", ColorCheckKt::colorCheck3),
                    getp("1/1/colorCheck4", ColorCheckKt::colorCheck4),
                    getp("1/1/colorTable", ColorCheckKt::colorTable),
//                getp("2/0/FooDemo", FooDemoKt::FooDemo),
//                getp("8/0/DemoPaging1", DemoPagingKt::paging1),
                    getp("8/particles/ball", DemoParticlesKt::ball),
                    getp("8/particles/confettiDemo", ImpulseDemo::confettiDemo),
                    getp("8/particles/heartsDemo", ImpulseDemo::heartsDemo),
                    getp("8/particles/maze", DemoParticlesKt::maze),
                    getp("8/particles/maze", DemoParticlesKt::pmaze),
                    getp("8/particles/maze1", DemoParticlesKt::pmaze1),
                    getpc("8/particles/ParticleDots", ParticleDotsKt::particleDots),
                    getpc("8/particles/ParticleDots2", ParticleDotsKt::particleDots2),
                    getpc("8/particles/fireworks", Particles3Kt::fireworks),
                    getpc("8/particles/fireworks2", Particles3Kt::fireworks2),
                    getpc("8/particles/particleSphere", ParticleSphereKt::particleSphere),
                    getpc("8/particles/rain1", Particles3Kt::rain1),
                    getpc("8/particles/warp", Particles3Kt::warp),
                    getp("8/particles/maze2", DemoParticlesKt::pmaze2),

                    getp("2/4/BitmapFontWatch", () -> BitmapFontWatch.watch1(activity)),
                    getp("2/4/Procedure__Version", BasicProceduralDemos::version),
                    getp("2/4/Procedure_centerText1", BasicProceduralDemos::centerText1),
                    getp("2/4/Procedure_gradient1", BasicProceduralDemos::gradient1),
                    getp("2/4/Procedure_gradient2", BasicProceduralDemos::gradient2),
                    getp("2/4/Procedure_gradient3", BasicProceduralDemos::gradient3),
                    getp("2/4/Procedure_gradient4", BasicProceduralDemos::gradient4),
                    getp("2/4/Procedure_lookUp1", BasicProceduralDemos::lookUp1),
                    getp("2/4/Procedure_simple1", BasicProceduralDemos::simple1),
                    getp("2/4/Procedure_simple2", BasicProceduralDemos::simple2),
                    getp("2/4/Procedure_simple3", BasicProceduralDemos::simple3),
                    getp("2/4/Procedure_simple4", BasicProceduralDemos::simple4),
                    getp("2/4/Procedure_simple5", BasicProceduralDemos::simple5),
                    getp("2/4/Procedure_simple6", BasicProceduralDemos::simple6),
                    getp("2/4/Procedure_simpleClockFast", BasicProceduralDemos::simpleClockFast),
                    getp("2/4/Procedure_simpleClockSlow", BasicProceduralDemos::simpleClockSlow),
                    getp("2/4/Procedure_textPathEffects", BasicProceduralDemos::textPathEffects),

                    getp("2/4/DemoBitmapDrawing_bitDraw1", DemoBitmapDrawing::bitDraw1),
                    getp("2/4/DemoBitmapDrawing_bitDraw2", DemoBitmapDrawing::bitDraw2),
                    getp("2/4/DemoFlick_flickTest", DemoFlick::flickTest),
                    getp("2/4/DemoPathExpression_pathTest1", DemoPathExpression::pathTest1),
                    getp("2/4/DemoPathExpression_pathTest2", DemoPathExpression::pathTest2),
                    getp("2/4/DemoPathExpression_pathTest3", DemoPathExpression::pathTest3),
                    getp("2/4/DemoWindingRule_pathWinding", DemoWindingRule::pathWinding),

                    getp("2/4/flowControlChecks1", FlowControlChecks::flowControlChecks1),
                    getp("2/4/flowControlChecks2", FlowControlChecks::flowControlChecks2),
                    getp("2/4/testConditional", FlowControlChecks::testConditional),
                    getp("2/4/Graph_graph1", Graph::graph1),
                    getp("2/4/Graph_graph2", Graph::graph2),
                    getp("2/4/HapticDemo_demoHaptic1", HapticDemo::demoHaptic1),
                    getp("2/4/IndexingDemo_pathIndex", IndexingDemo::pathIndex),
                    getp("2/4/PathDemo_path2", PathDemo::path2),
                    getp("2/4/PathDemo_pathTweenDemo", PathDemo::pathTweenDemo),
                    getp("2/4/PathDemo_remoteConstruction", PathDemo::remoteConstruction),
                    getp("2/4/PathProceduralChecks_allPath", PathProceduralChecks::allPath),
                    getp("2/4/PathProceduralChecks_basicPath", PathProceduralChecks::basicPath),
                    getp("2/4/SensorDemo_Compass", SensorDemo::compass),
                    getp("2/4/SensorDemo_accSensor1", SensorDemo::accSensor1),
                    getp("2/4/SensorDemo_gyroSensor1", SensorDemo::gyroSensor1),
                    getp("2/4/SensorDemo_lightSensor1", SensorDemo::lightSensor1),
                    getp("2/4/SensorDemo_magSensor1", SensorDemo::magSensor1),
                    getp("2/4/SplineDemo_splineDemo1", SplineDemo::splineDemo1),
                    getp("2/4/TextureDemo_basicTexture", TextureDemo::basicTexture),
                    getp("2/4/TextureDemo_textureClock", TextureDemo::textureClock),
                    getp("2/4/TextureDemo_textureClockTest", TextureDemo::textureClockTest),
                    getp("2/4/WakeDemo_wakeClock", WakeDemo::wakeClock),
                    getp("2/4/SmallAnimated", SmallAnimated::small),
                    getp("2/A/PlayerInfo", RCPlayerInfoKt::info),
                    getp("2/A/PressureGauge", PressureGaugeKt::demoPressureGauge),
                    getp("2/og/serverClock", ServerSideKt::serverClock),
                    getp("2/A/demoGraphs1", DemoGraphsKt::demoGraphs),
                    getp("2/A/demoGraphs0", DemoGraphsKt::demoGraphs2),
                    getpc("2/A/pieChart", PieChartKt::demoPieChart),
                    getpc("2/A/goodPieChart", PieChartKt::demoPieChart_good),
                    getpc("2/A/PieChart2", PieChartKt::demoPieChart2),
                    getpc("2/A/LinearRegression", LinearRegressionKt::demoLinearRegression),
                    getpc("2/A/MoonPhases", MoonPhasesKt::demoMoonPhases),
                    getp("2/v/badMemory", MemoryKt::fillMemory),
                    getp("3/DataViz/ActivityRings", DataVizDemosKt::demoActivityRings),
                    getp("3/DataViz/HeartRateTimeline", DataVizDemosKt::demoHeartRateTimeline),
                    getp("3/DataViz/StepProgressArc", DataVizDemosKt::demoStepProgressArc),
                    getp("3/DataViz/WeatherForecastBars", DataVizDemosKt::demoWeatherForecastBars),
                    getp("3/DataViz/SleepQualityRings", DataVizDemosKt::demoSleepQualityRings),
                    getp("3/DataViz/BatteryRadialGauge", DataVizDemosKt::demoBatteryRadialGauge),
                    getp("3/DataViz/CalendarHeatmapGrid", DataVizDemosKt::demoCalendarHeatmapGrid),
                    getp("3/DataViz/StockSparkline", DataVizDemosKt::demoStockSparkline),
                    getp("3/DataViz/MoonPhaseDial", DataVizDemosKt::demoMoonPhaseDial),
                    getp("3/DataViz/HydrationWave", DataVizDemosKt::demoHydrationWave),

                    getp("5/Server/serverClock", ServerSideKt::serverClock),
                    getp("2/Example/spreadSheet", ExampleNumbersKt::spreadSheet),
                    getp("1/Example/color", DemoColorKt::colorButtons),
//                getp("1/Example/FooDemo", FooDemoKt::FooDemo),
                    getp("0/Color/colorTable", ColorCheckKt::colorTable),
                    getp("0/Alt/ColorTheme", ColorThemeCheckKt::themeList),
                    getpc("0/alt/stock", () -> {
                        return RcTicker(activity.getApplicationContext());
                    }),

                    getp("0/font/base", FontCheckKt::fontList),
                    getp("1/font/colorTable", ColorCheckKt::colorTable),
                    getp("1/font/colorList", ColorCheckKt::colorList),

                    getp("1/Example/spreadSheet", ExampleNumbersKt::spreadSheet),
                    getp("1/Example/demoUseOfGlobal", DemoGlobalKt::demoUseOfGlobal),
                    getp("1/Example/demoTextTransform", DemoTextTransformKt::demoTextTransform),
                    getp("1/Example/HostileActor1", HostileActor::demoImage),
                    getp("1/Example/HostileActor1", HostileActor::demoImageColor),

                    getp("1/ThemedPlot1", DemoKt::plot1), getp("1/plot2", DemoKt::plot2),
                    getp("1/plot3", DemoKt::plot3),
                    getp("1/plot4", DemoKt::plot4),
                    getp("2/touch/00Touch1", DemoTouchKt::demoTouch1),
                    getp("2/touch/01Touch2", DemoTouchKt::demoTouch2),
                    getp("2/touch/02TouchWrap", DemoTouchKt::demoTouchWrap),

                    getp("2/touch/03StopGently", DemoTouchKt::touchStopGently),
                    getp("2/touch/04StopEnds", DemoTouchKt::touchStopEnds),
                    getp("2/touch/05StopInstantly", DemoTouchKt::touchStopInstantly),

                    getp("2/touch/06StopNotchesEven", DemoTouchKt::touchStopNotchesEven),
                    getp("2/touch/07StopNotchesPercents", DemoTouchKt::touchStopNotchesPercents),
                    getp("2/touch/08StopNotchesAbsolute", DemoTouchKt::touchStopNotchesAbsolute),
                    getp("2/touch/09StopAbsolutePos", DemoTouchKt::touchStopAbsolutePos),
                    getp("2/touch/10simpleJavaAnim", DemoTouchKt::simpleJavaAnim),

                    getp("2/touch/11ThumbWheel1", DemoTouchKt::demoTouchThumbWheel1),
                    getp("2/touch/12ThumbWheel2", DemoTouchKt::demoTouchThumbWheel2),

                    getp("5/Server/serverClock", ServerSideKt::serverClock),

                    getp("6/Procedural/plotWave", PlotWaveKt::basicPlot),
                    getp("6/Procedural/plotWave", PlotWaveKt::plotWave),
                    getpc("6/Procedural/CountDown", CountdownKt::countDown),
                    getpc("6/Procedural/Cube3D", Cube3DKt::cube3d),
                    getpc("6/Procedural/ShaderCalendar", ShaderCalendarKt::ShaderCalendar),
                    getp("6/Procedural/countdown", ExampleTimerKt::basicTimer),
                    getpc("6/Procedural/TextBaseline", TextKt::RcTextDemo),

                    getp("7/Java/AttributeString", DemoAttributedString::demo),
                    getp("7/Java/anchoredText", DemoAnchorText::anchoredText),
                    getp("7/Java/pathsDemos", DemoPaths::pathTest),


                    getp("0/clock/fancyClock1", FancyClocks::fancyClock1),
                    getp("0/clock/fancyClock2", FancyClocks::fancyClock2),
                    getp("0/clock/fancyClock3", FancyClocks::fancyClock3),
                    getp("8/clock/clock1", ClockDemo1::clock1),
                    getp("8/clock/clock2", ClockDemo2::clock2),
                    getp("8/clock/fancyClock2", ClockDemo2::fancyClock2),
                    getp("8/clock/fancyClock2", DemoMetalClockKt::fancyClock2),
                    getp("8/clock/mclock", MClockKt::MClock),
                    // Group 1: Foundation & Sizing
                    getp("9/Comp/C_Box", DemoBoxKt::DemoBox),
                    getp("9/Comp/C_Row", DemoRowKt::DemoRow),
                    getp("9/Comp/C_Column", DemoColumnKt::DemoColumn),
                    getp("9/Comp/C_ModifierWidth", DemoModifierWidthKt::DemoModifierWidth),
                    getp("9/Comp/C_ModifierHeight", DemoModifierHeightKt::DemoModifierHeight),
                    getp("9/Comp/C_ModifierSize", DemoModifierSizeKt::DemoModifierSize),
                    getp("9/Comp/C_ModifierFillMaxWidth",
                            DemoModifierFillMaxWidthKt::DemoModifierFillMaxWidth),
                    getp("9/Comp/C_ModifierFillMaxHeight",
                            DemoModifierFillMaxHeightKt::DemoModifierFillMaxHeight),
                    getp("9/Comp/C_ModifierFillMaxSize",
                            DemoModifierFillMaxSizeKt::DemoModifierFillMaxSize),
                    getp("9/Comp/CM_odifierWidthIn", DemoModifierWidthInKt::DemoModifierWidthIn),
                    getp("9/Comp/C_ModifierHeightIn", DemoModifierHeightInKt::DemoModifierHeightIn),
                    getp("9/Comp/C_ModifierWrapContentWidth",
                            DemoModifierWrapContentWidthKt::DemoModifierWrapContentWidth),
                    getp("9/Comp/C_ModifierWrapContentHeight",
                            DemoModifierWrapContentHeightKt::DemoModifierWrapContentHeight),
                    getp("9/Comp/C_ModifierWrapContentSize",
                            DemoModifierWrapContentSizeKt::DemoModifierWrapContentSize),
                    // Group 2: Parent-Relative & Layout Logic
                    getp("9/Comp/C_ModifierFillParentMaxWidth",
                            DemoModifierFillParentMaxWidthKt::DemoModifierFillParentMaxWidth),
                    getp("9/Comp/C_ModifierFillParentMaxHeight",
                            DemoModifierFillParentMaxHeightKt::DemoModifierFillParentMaxHeight),
                    getp("9/Comp/C_ModifierFillParentMaxSize",
                            DemoModifierFillParentMaxSizeKt::DemoModifierFillParentMaxSize),
                    getp("9/Comp/C_ModifierHorizontalWeight",
                            DemoModifierHorizontalWeightKt::DemoModifierHorizontalWeight),
                    getp("9/Comp/C_ModifierVerticalWeight",
                            DemoModifierVerticalWeightKt::DemoModifierVerticalWeight),
                    getp("9/Comp/C_ModifierSpacedBy",
                            DemoModifierSpacedByKt::DemoModifierSpacedBy),
                    getp("9/Comp/C_ModifierAlignByBaseline",
                            DemoModifierAlignByBaselineKt::DemoModifierAlignByBaseline),
                    getp("9/Comp/C_ModifierPadding",
                            DemoModifierPaddingKt::DemoModifierPadding),
                    getp("9/Comp/C_ModifierZIndex",
                            DemoModifierZIndexKt::DemoModifierZIndex),
                    getp("9/Comp/C_ModifierComponentId",
                            DemoModifierComponentIdKt::DemoModifierComponentId),

                    // Group 3: Visual & Clipping
                    getp("9/Comp/C_ModifierBackground",
                            DemoModifierBackgroundKt::DemoModifierBackground),
                    getp("9/Comp/C_ModifierBackgroundId",
                            DemoModifierBackgroundIdKt::DemoModifierBackgroundId),
                    getp("9/Comp/C_ModifierBorder",
                            DemoModifierBorderKt::DemoModifierBorder),
                    getp("9/Comp/C_ModifierDynamicBorder",
                            DemoModifierDynamicBorderKt::DemoModifierDynamicBorder),
                    getp("9/Comp/C_ModifierClipRect",
                            DemoModifierClipRectKt::DemoModifierClipRect),
                    getp("9/Comp/C_ModifierClipRoundedRect",
                            DemoModifierClipRoundedRectKt::DemoModifierClipRoundedRect),
                    getp("9/Comp/C_ModifierClipCircle",
                            DemoModifierClipCircleKt::DemoModifierClipCircle),
                    getp("9/Comp/C_ModifierVisibility",
                            DemoModifierVisibilityKt::DemoModifierVisibility),
                    getp("9/Comp/C_ModifierComputeMeasure",
                            DemoModifierComputeMeasureKt::DemoModifierComputeMeasure),
                    getp("9/Comp/C_ModifierComputePosition",
                            DemoModifierComputePositionKt::DemoModifierComputePosition),

                    // Group 4: Specialized Layouts & Content
                    getp("9/Comp/C_Flow",
                            DemoFlowKt::DemoFlow),
                    getp("9/Comp/C_FitBox",
                            DemoFitBoxKt::DemoFitBox),
                    getp("9/Comp/C_CollapsibleColumn",
                            DemoCollapsibleColumnKt::DemoCollapsibleColumn),
                    getp("9/Comp/C_CollapsibleRow",
                            DemoCollapsibleRowKt::DemoCollapsibleRow),
                    getp("9/Comp/C_ModifierCollapsiblePriority",
                            DemoModifierCollapsiblePriorityKt::DemoModifierCollapsiblePriority),
                    getp("9/Comp/C_StateLayout",
                            DemoStateLayoutKt::DemoStateLayout),
                    getp("9/Comp/C_Text",
                            DemoTextKt::DemoText),
                    getp("9/Comp/C_TextAutoSize",
                            DemoTextAutoSizeKt::DemoTextAutoSize),
                    getp("9/Comp/C_Image",
                            DemoImageKt::DemoImage),

                    // Group 5: Interactive & Scrolling
                    getp("9/Comp/C_ModifierVerticalScroll",
                            DemoModifierVerticalScrollKt::DemoModifierVerticalScroll),
                    getp("9/Comp/C_ModifierHorizontalScroll",
                            DemoModifierHorizontalScrollKt::DemoModifierHorizontalScroll),
                    getp("9/Comp/C_ModifierOnClick",
                            DemoModifierOnClickKt::DemoModifierOnClick),
                    getp("9/Comp/C_ModifierOnTouchDown",
                            DemoModifierOnTouchDownKt::DemoModifierOnTouchDown),
                    getp("9/Comp/C_ModifierOnTouchUp",
                            DemoModifierOnTouchUpKt::DemoModifierOnTouchUp),
                    getp("9/Comp/C_ModifierOnTouchCancel",
                            DemoModifierOnTouchCancelKt::DemoModifierOnTouchCancel)

            ));
        }
        return demos;
    }

    @SuppressLint("RestrictedApiAndroidX")
    static @NonNull Bitmap simpleBitmap(int size) {
        int color = 0xFF7722;
        Bitmap ball = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        int w = ball.getWidth();
        int h = ball.getHeight();
        float cx = w / 2;
        float cy = h / 2;
        float radius = cx * 0.9f;
        float radius2 = radius * radius;
        int[] data = new int[w * h];
        for (int i = 0; i < data.length; i++) {
            int x = i % w;
            int y = i / w;
            float dx = x - cx;
            float dy = y - cy;
            float dist2 = dx * dx + dy * dy;
            if (dist2 > radius2) {
                continue;
            }
            float norm2 = radius * radius - dist2;
            int bright = (int) (norm2 * 255 / radius2);
            data[i] = 0x33000000 + color * bright;
        }
        ball.setPixels(data, 0, w, 0, 0, w, h);
        return ball;
    }
}
