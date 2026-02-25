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

import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.getp;
import static androidx.compose.remote.integration.view.demos.ExperimentRecyclerActivity.getpc;
import static androidx.compose.remote.integration.view.demos.examples.RcTickerKt.RcTicker;

import android.app.Activity;

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
import androidx.compose.remote.integration.view.demos.examples.DemoTouchKt;
import androidx.compose.remote.integration.view.demos.examples.ExampleNumbersKt;
import androidx.compose.remote.integration.view.demos.examples.ExampleTimerKt;
import androidx.compose.remote.integration.view.demos.examples.FontCheckKt;
import androidx.compose.remote.integration.view.demos.examples.HostileActor;
import androidx.compose.remote.integration.view.demos.examples.LinearRegressionKt;
import androidx.compose.remote.integration.view.demos.examples.MClockKt;
import androidx.compose.remote.integration.view.demos.examples.MoonPhasesKt;
import androidx.compose.remote.integration.view.demos.examples.PieChartKt;
import androidx.compose.remote.integration.view.demos.examples.PlotWaveKt;
import androidx.compose.remote.integration.view.demos.examples.PressureGaugeKt;
import androidx.compose.remote.integration.view.demos.examples.RCPlayerInfoKt;
import androidx.compose.remote.integration.view.demos.examples.ServerSideKt;
import androidx.compose.remote.integration.view.demos.examples.ShaderCalendarKt;
import androidx.compose.remote.integration.view.demos.examples.SmallAnimated;
import androidx.compose.remote.integration.view.demos.examples.TextKt;
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

        return new ArrayList<>(Arrays.asList(
                getp("0/0/skip", DemoMemorySkipKt::skip1),
                getp("0/4/BitmapFontWatch", () -> BitmapFontWatch.watch1(activity)),
                getp("0/4/Procedure__Version", BasicProceduralDemos::version),
                getp("0/4/Procedure__centerText1", BasicProceduralDemos::centerText1),
                getp("0/4/Procedure__gradient1", BasicProceduralDemos::gradient1),
                getp("0/4/Procedure__gradient2", BasicProceduralDemos::gradient2),
                getp("0/4/Procedure__gradient3", BasicProceduralDemos::gradient3),
                getp("0/4/Procedure__gradient4", BasicProceduralDemos::gradient4),
                getp("0/4/Procedure__lookUp1", BasicProceduralDemos::lookUp1),
                getp("0/4/Procedure__simple1", BasicProceduralDemos::simple1),
                getp("0/4/Procedure__simple2", BasicProceduralDemos::simple2),
                getp("0/4/Procedure__simple3", BasicProceduralDemos::simple3),
                getp("0/4/Procedure__simple4", BasicProceduralDemos::simple4),
                getp("0/4/Procedure__simple5", BasicProceduralDemos::simple5),
                getp("0/4/Procedure__simple6", BasicProceduralDemos::simple6),
                getp("0/4/Procedure__simpleClockFast", BasicProceduralDemos::simpleClockFast),
                getp("0/4/Procedure__simpleClockSlow", BasicProceduralDemos::simpleClockSlow),
                getp("0/4/Procedure__textPathEffects", BasicProceduralDemos::textPathEffects),
                getp("0/4/ClockDemo1_clock1", ClockDemo1::clock1),
                getp("0/4/ClockDemo2_jancyClock2", ClockDemo2::fancyClock2),
                getp("0/4/ClockDemo2_jclock2", ClockDemo2::clock2),
                getp("0/4/DemoBitmapDrawing_bitDraw1", DemoBitmapDrawing::bitDraw1),
                getp("0/4/DemoBitmapDrawing_bitDraw2", DemoBitmapDrawing::bitDraw2),
                getp("0/4/DemoFlick_flickTest", DemoFlick::flickTest),
                getp("0/4/DemoPathExpression_pathTest1", DemoPathExpression::pathTest1),
                getp("0/4/DemoPathExpression_pathTest2", DemoPathExpression::pathTest2),
                getp("0/4/DemoPathExpression_pathTest3", DemoPathExpression::pathTest3),
                getp("0/4/DemoWindingRule_pathWinding", DemoWindingRule::pathWinding),
                getp("0/4/FancyClocks_fancyClock1", FancyClocks::fancyClock1),
                getp("0/4/FancyClocks_fancyClock2", FancyClocks::fancyClock2),
                getp("0/4/FancyClocks_fancyClock3", FancyClocks::fancyClock3),
                getp("0/4/FlowControlChecks_flowControlChecks1",
                        FlowControlChecks::flowControlChecks1),
                getp("0/4/FlowControlChecks_flowControlChecks2",
                        FlowControlChecks::flowControlChecks2),
                getp("0/4/FlowControlChecks_testConditional",
                        FlowControlChecks::testConditional),
                getp("0/4/Graph_graph1", Graph::graph1),
                getp("0/4/Graph_graph2", Graph::graph2),
                getp("0/4/HapticDemo_demoHaptic1", HapticDemo::demoHaptic1),
                getp("0/4/ImpulseDemo_confettiDemo", ImpulseDemo::confettiDemo),
                getp("0/4/ImpulseDemo_heartsDemo", ImpulseDemo::heartsDemo),
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
                getp("DataViz/ActivityRings", DataVizDemosKt::demoActivityRings),
                getp("DataViz/HeartRateTimeline", DataVizDemosKt::demoHeartRateTimeline),
                getp("DataViz/StepProgressArc", DataVizDemosKt::demoStepProgressArc),
                getp("DataViz/WeatherForecastBars", DataVizDemosKt::demoWeatherForecastBars),
                getp("DataViz/SleepQualityRings", DataVizDemosKt::demoSleepQualityRings),
                getp("DataViz/BatteryRadialGauge", DataVizDemosKt::demoBatteryRadialGauge),
                getp("DataViz/CalendarHeatmapGrid", DataVizDemosKt::demoCalendarHeatmapGrid),
                getp("DataViz/StockSparkline", DataVizDemosKt::demoStockSparkline),
                getp("DataViz/MoonPhaseDial", DataVizDemosKt::demoMoonPhaseDial),
                getp("DataViz/HydrationWave", DataVizDemosKt::demoHydrationWave),

                getp("5/Server/serverClock", ServerSideKt::serverClock),
                getp("2/Example/spreadSheet", ExampleNumbersKt::spreadSheet),
                getp("1/Example/color", DemoColorKt::colorButtons),
                getp("0/Color/colorTable", ColorCheckKt::colorTable),
                getp("0/alt/clock", MClockKt::MClock),
                getp("0/Alt/ColorTheme", ColorThemeCheckKt::themeList),
                getpc("0/alt/stock", () -> {
                    return RcTicker(activity.getApplicationContext());
                }),

                getp("0/font/base", FontCheckKt::fontList),
                getp("1/font/colorTable", ColorCheckKt::colorTable),
                getp("1/font/colorList", ColorCheckKt::colorList),

                getp("1/Example/spreadSheet", ExampleNumbersKt::spreadSheet),
                getp("1/Example/demoUseOfGlobal",
                        DemoGlobalKt::demoUseOfGlobal),
                getp("1/Example/demoTextTransform",
                        DemoTextTransformKt::demoTextTransform),
                getp("1/Example/HostileActor1",
                        HostileActor::demoImage),
                getp("1/Example/HostileActor1",
                        HostileActor::demoImageColor),

                getp("1/ThemedPlot1", DemoKt::plot1),
                getp("1/plot2", DemoKt::plot2),
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

                getp("Procedural/plotWave", PlotWaveKt::basicPlot),
                getp("Procedural/plotWave", PlotWaveKt::plotWave),
                getpc("Procedural/CountDown", CountdownKt::countDown),
                getpc("Procedural/Cube3D", Cube3DKt::cube3d),
                getpc("Procedural/ShaderCalendar", ShaderCalendarKt::ShaderCalendar),
                getp("Procedural/countdown", ExampleTimerKt::basicTimer),
                getpc("Procedural/TextBaseline", TextKt::RcTextDemo),

                getp("Java/AttributeString", DemoAttributedString::demo),
                getp("Java/anchoredText", DemoAnchorText::anchoredText),
                getp("1/Java/pathsDemos", DemoPaths::pathTest),

                getp("fancyClock2", DemoMetalClockKt::fancyClock2),
                getp("Server/maze", DemoParticlesKt::pmaze),
                getp("Server/maze1", DemoParticlesKt::pmaze1),
                getp("Server/maze2", DemoParticlesKt::pmaze2)


        ));
    }

}
