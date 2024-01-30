/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.activity

/**
 * A class that has a [FullyDrawnReporter] that allows you to have separate parts of the
 * UI independently register when they have been fully loaded.
 */
interface FullyDrawnReporterOwner {
    /**
     * Retrieve the [FullyDrawnReporter] that should handle the independent parts of the UI
     * that separately report that they are fully drawn.
     */
    val fullyDrawnReporter: FullyDrawnReporter
}
