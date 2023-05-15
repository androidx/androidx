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

package androidx.window.testing.layout

import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowMetricsCalculatorDecorator

/**
 * A decorator to return [StubWindowMetricsCalculator] instead of the actual implementation.
 */
internal object StubMetricDecorator : WindowMetricsCalculatorDecorator {
    override fun decorate(calculator: WindowMetricsCalculator): WindowMetricsCalculator {
        return StubWindowMetricsCalculator()
    }
}