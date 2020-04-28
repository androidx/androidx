/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material.demos

import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.material.samples.BottomDrawerSample
import androidx.ui.material.samples.EmphasisSample
import androidx.ui.material.samples.ModalDrawerSample
import androidx.ui.material.samples.ScaffoldWithBottomBarAndCutout
import androidx.ui.material.samples.SideBySideAlertDialogSample

val MaterialDemos = DemoCategory("Material", listOf(
    ComposableDemo("AlertDialog") { SideBySideAlertDialogSample() },
    ComposableDemo("App Bars") { AppBarDemo() },
    ComposableDemo("Bottom Navigation") { BottomNavigationDemo() },
    ComposableDemo("Buttons & FABs") { ButtonDemo() },
    DemoCategory("Drawer", listOf(
        ComposableDemo("Modal") { ModalDrawerSample() },
        ComposableDemo("Bottom") { BottomDrawerSample() }
    )),
    ComposableDemo("Elevation") { ElevationDemo() },
    ComposableDemo("Emphasis") { EmphasisSample() },
    ComposableDemo("ListItems") { ListItemDemo() },
    DemoCategory("TextFields", listOf(
        ComposableDemo("FilledTextField") { FilledTextFieldDemo() },
        ComposableDemo("Multiple text fields") { TextFieldsDemo() }
    )),
    ComposableDemo("Material Theme") { MaterialThemeDemo() },
    ComposableDemo("Menus") { MenuDemo() },
    DemoCategory("Playground", listOf(
        ComposableDemo("Color Picker") { ColorPickerDemo() },
        ActivityDemo("Dynamic Theme", DynamicThemeActivity::class)
    )),
    ComposableDemo("Progress Indicators") { ProgressIndicatorDemo() },
    ComposableDemo("Scaffold") { ScaffoldWithBottomBarAndCutout() },
    ComposableDemo("Selection Controls") { SelectionControlsDemo() },
    ComposableDemo("Slider") { SliderDemo() },
    ComposableDemo("Snackbar") { SnackbarDemo() },
    ComposableDemo("Tabs") { TabDemo() }
))
