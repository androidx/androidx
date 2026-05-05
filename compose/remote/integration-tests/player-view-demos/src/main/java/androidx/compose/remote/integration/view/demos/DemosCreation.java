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

import androidx.compose.remote.integration.view.demos.dsl.DslDemoGraphsKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslClockKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslCountdownKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslEnumsDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslTickerKt;
import androidx.compose.remote.integration.view.demos.examples.BadExamples.DemoMemorySkipKt;
import androidx.compose.remote.integration.view.demos.examples.BadExamples.MemoryKt;
import androidx.compose.remote.integration.view.demos.examples.ColorCheckKt;
import androidx.compose.remote.integration.view.demos.examples.ColorThemeCheckKt;
import androidx.compose.remote.integration.view.demos.examples.CountdownKt;
import androidx.compose.remote.integration.view.demos.examples.Cube3DKt;
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
    public static @NonNull ArrayList<RCDoc> getDemos(@NonNull Activity activity) {
        Bitmap bitmap = simpleBitmap(100);

        return new ArrayList<>(Arrays.asList(


                get("0/0/00dslDemoGraphs2", DslDemoGraphsKt::dslDemoGraphs2),
                get("0/1/00dslDemoGraphs", DslDemoGraphsKt::dslDemoGraphs),
                get("0/2/00dslCountdown1", RcDslCountdownKt::dslCountdown),
                getpc("0/3/00dslCountdown2", CountdownKt::countDown),

                get("0/4/00dslTheme1", RcDslDemoKt::dslTheme1),
                get("0/5/00dslTheme2", RcDslDemoKt::dslTheme2),
                get("0/6/dslSimpleClock", RcDslDemoKt::dslSimpleClock),

                get("0/7/dslSimpleDemo", RcDslDemoKt::dslSimpleDemo),

                //get("0/0/00dslCalenda", RcDslCalendarDemoKt::dslCalendarDayAgenda),
                //get("0/0/00dslDemoGraphs2", DslDemoGraphsKt::dslDemoGraphs2),
               // get("0/0/00dslDemoGraphs", DslDemoGraphsKt::dslDemoGraphs),
              // get("0/0/00dslLinearRegression", DslLinearRegressionKt::dslLinearRegression),
                get("1/1/00enumsDemo", RcDslEnumsDemoKt::enumsDemo),
                get("1/1/01dslClock", RcDslClockKt::dslClock),
                get("1/1/02dslDemo", RcDslDemoKt::dslDemo),
                get("1/1/03dslTickerPreview", RcDslTickerKt::dslTicker),

                get("1/1/dslTheme1", RcDslDemoKt::dslTheme1),
                get("1/1/dslTheme2", RcDslDemoKt::dslTheme2),
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
//                getp("0/0/FooDemo", FooDemoKt::FooDemo),
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

                getp("0/4/BitmapFontWatch", () -> BitmapFontWatch.watch1(activity)),
                getp("0/4/Procedure__Version", BasicProceduralDemos::version),
                getp("0/4/Procedure_centerText1", BasicProceduralDemos::centerText1),
                getp("0/4/Procedure_gradient1", BasicProceduralDemos::gradient1),
                getp("0/4/Procedure_gradient2", BasicProceduralDemos::gradient2),
                getp("0/4/Procedure_gradient3", BasicProceduralDemos::gradient3),
                getp("0/4/Procedure_gradient4", BasicProceduralDemos::gradient4),
                getp("0/4/Procedure_lookUp1", BasicProceduralDemos::lookUp1),
                getp("0/4/Procedure_simple1", BasicProceduralDemos::simple1),
                getp("0/4/Procedure_simple2", BasicProceduralDemos::simple2),
                getp("0/4/Procedure_simple3", BasicProceduralDemos::simple3),
                getp("0/4/Procedure_simple4", BasicProceduralDemos::simple4),
                getp("0/4/Procedure_simple5", BasicProceduralDemos::simple5),
                getp("0/4/Procedure_simple6", BasicProceduralDemos::simple6),
                getp("0/4/Procedure_simpleClockFast", BasicProceduralDemos::simpleClockFast),
                getp("0/4/Procedure_simpleClockSlow", BasicProceduralDemos::simpleClockSlow),
                getp("0/4/Procedure_textPathEffects", BasicProceduralDemos::textPathEffects),

                getp("0/4/DemoBitmapDrawing_bitDraw1", DemoBitmapDrawing::bitDraw1),
                getp("0/4/DemoBitmapDrawing_bitDraw2", DemoBitmapDrawing::bitDraw2),
                getp("0/4/DemoFlick_flickTest", DemoFlick::flickTest),
                getp("0/4/DemoPathExpression_pathTest1", DemoPathExpression::pathTest1),
                getp("0/4/DemoPathExpression_pathTest2", DemoPathExpression::pathTest2),
                getp("0/4/DemoPathExpression_pathTest3", DemoPathExpression::pathTest3),
                getp("0/4/DemoWindingRule_pathWinding", DemoWindingRule::pathWinding),

                getp("0/4/flowControlChecks1", FlowControlChecks::flowControlChecks1),
                getp("0/4/flowControlChecks2", FlowControlChecks::flowControlChecks2),
                getp("0/4/testConditional", FlowControlChecks::testConditional),
                getp("0/4/Graph_graph1", Graph::graph1),
                getp("0/4/Graph_graph2", Graph::graph2),
                getp("0/4/HapticDemo_demoHaptic1", HapticDemo::demoHaptic1),
                getp("0/4/IndexingDemo_pathIndex", IndexingDemo::pathIndex),
                getp("0/4/PathDemo_path2", PathDemo::path2),
                getp("0/4/PathDemo_pathTweenDemo", PathDemo::pathTweenDemo),
                getp("0/4/PathDemo_remoteConstruction", PathDemo::remoteConstruction),
                getp("0/4/PathProceduralChecks_allPath", PathProceduralChecks::allPath),
                getp("0/4/PathProceduralChecks_basicPath", PathProceduralChecks::basicPath),
                getp("0/4/SensorDemo_Compass", SensorDemo::compass),
                getp("0/4/SensorDemo_accSensor1", SensorDemo::accSensor1),
                getp("0/4/SensorDemo_gyroSensor1", SensorDemo::gyroSensor1),
                getp("0/4/SensorDemo_lightSensor1", SensorDemo::lightSensor1),
                getp("0/4/SensorDemo_magSensor1", SensorDemo::magSensor1),
                getp("0/4/SplineDemo_splineDemo1", SplineDemo::splineDemo1),
                getp("0/4/TextureDemo_basicTexture", TextureDemo::basicTexture),
                getp("0/4/TextureDemo_textureClock", TextureDemo::textureClock),
                getp("0/4/TextureDemo_textureClockTest", TextureDemo::textureClockTest),
                getp("0/4/WakeDemo_wakeClock", WakeDemo::wakeClock),
                getp("0/4/SmallAnimated", SmallAnimated::small),
                getp("0/A/PlayerInfo", RCPlayerInfoKt::info),
                getp("0/A/PressureGauge", PressureGaugeKt::demoPressureGauge),
                getp("0/og/serverClock", ServerSideKt::serverClock),
                getp("0/A/demoGraphs1", DemoGraphsKt::demoGraphs),
                getp("0/A/demoGraphs0", DemoGraphsKt::demoGraphs2),
                getpc("0/A/pieChart", PieChartKt::demoPieChart),
                getpc("0/A/goodPieChart", PieChartKt::demoPieChart_good),
                getpc("0/A/PieChart2", PieChartKt::demoPieChart2),
                getpc("0/A/LinearRegression", LinearRegressionKt::demoLinearRegression),
                getpc("0/A/MoonPhases", MoonPhasesKt::demoMoonPhases),
                getp("0/v/badMemory", MemoryKt::fillMemory),
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
