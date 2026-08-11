/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import android.os.Build
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen

val BaselineProfileScreens =
    listOf(
        AlertDialogScreen,
        *(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(AnimatedTextScreen)
        } else {
            emptyArray<MacrobenchmarkScreen>()
        }),
        ButtonGroupScreen,
        ButtonScreen,
        CardScreen,
        CheckboxButtonScreen,
        ColorSchemeScreen,
        ConfirmationScreen,
        CurvedTextScreen,
        DatePickerScreen,
        EdgeButtonScreen,
        IconButtonScreen,
        IconToggleButtonScreen,
        LevelIndicatorScreen,
        ListHeaderScreen,
        Navigation3Screen,
        OneHandedGestureScreen,
        OpenOnPhoneDialogScreen,
        PageIndicatorScreen,
        PickerGroupScreen,
        PickerScreen,
        PlaceHolderScreen,
        ProgressIndicatorScreen,
        RadioButtonScreen,
        ScaffoldScreen,
        ScrollIndicatorScreen,
        SliderScreen,
        StepperScreen,
        SwipeToDismissScreen,
        SwipeToRevealScreen,
        SwitchButtonScreen,
        TextButtonScreen,
        TextToggleButtonScreen,
        TimePickerScreen,
        TimeTextScreen,
        TransformingLazyColumnScreen,
    )
