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

import androidx.compose.remote.integration.view.demos.examples.CountdownKt;
import androidx.compose.remote.integration.view.demos.examples.Cube3DKt;
import androidx.compose.remote.integration.view.demos.examples.DemoAnchorText;
import androidx.compose.remote.integration.view.demos.examples.DemoAttributedString;
import androidx.compose.remote.integration.view.demos.examples.DemoMetalClockKt;
import androidx.compose.remote.integration.view.demos.examples.DemoPaths;
import androidx.compose.remote.integration.view.demos.examples.ExampleTimerKt;
import androidx.compose.remote.integration.view.demos.examples.ShaderCalendarKt;
import androidx.compose.remote.integration.view.demos.examples.TextKt;
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
    public static @NonNull ArrayList<RCDoc> getDemos() {
        return new ArrayList<>(Arrays.asList(
                getp("Java/AttributeString", DemoAttributedString::demo),
                getp("Java/pathTest", ExampleTimerKt::basicTimer),
                getp("fancyClock2", DemoMetalClockKt::fancyClock2),
                getp("Java/pathTest", DemoAnchorText::anchoredText),
                getp("Java/pathTest", DemoPaths::pathTest),
                getpc("Procedural/Text baseline", TextKt::RcTextDemo),
                getp("Procedural/StartAnimation", DemoPaths::pathTest),
                getpc("Procedural/CountDown", CountdownKt::countDown),
                getpc("Procedural/Cube 3D", Cube3DKt::cube3d),
                getpc("Procedural/Shader Calendar", ShaderCalendarKt::ShaderCalendar)
        ));
    }

}
