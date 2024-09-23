/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring

import androidx.annotation.VisibleForTesting

/**
 * Identifier for a stroke that is being authored via [InProgressStrokesView], returned from
 * [InProgressStrokesView.startStroke] and an input to some of its other functions. This identifier
 * is unique within the app process lifetime.
 *
 * It can be used for equality checks and as a map key.
 */
@Suppress("ClassShouldBeObject") // Multiple instances of this class are required as IDs.
public class InProgressStrokeId @VisibleForTesting public constructor() {

    internal companion object {
        // If VisibleForTesting supported otherwise=INTERNAL (b/174783094), then the constructor
        // could
        // serve both testing use cases as well as being called from code in the same module but a
        // different package (e.g. LatencyData).
        internal fun create(): InProgressStrokeId = InProgressStrokeId()
    }
}
