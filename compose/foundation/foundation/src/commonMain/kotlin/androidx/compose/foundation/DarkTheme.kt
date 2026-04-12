/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Returns whether the operating system is in dark theme.
 *
 * This function should be used to help build responsive UIs that follow the system setting, to
 * avoid harsh contrast changes when switching between applications.
 *
 * It is recommended for this to be used at the top level of the application, to create theming
 * objects such as a set of colors that can be provided through the hierarchy. This way, screens and
 * components don't need to be aware of the system theme setting, they just consume properties from
 * the theming object. This also makes it easier to support user-defined overrides, such as forcing
 * light / dark theme regardless of the system setting.
 *
 * @sample androidx.compose.foundation.samples.DarkThemeSample
 * @return `true` if the system is considered to be in 'dark theme'.
 */
@Composable @ReadOnlyComposable fun isSystemInDarkTheme() = _isSystemInDarkTheme()

@Composable @ReadOnlyComposable internal expect fun _isSystemInDarkTheme(): Boolean
