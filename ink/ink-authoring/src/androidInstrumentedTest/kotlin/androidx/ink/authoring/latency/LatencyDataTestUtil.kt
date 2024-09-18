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

package androidx.ink.authoring.latency

import androidx.ink.authoring.ExperimentalLatencyDataApi
import com.google.common.truth.Correspondence

@ExperimentalLatencyDataApi
public val latencyDataEqual: Correspondence<LatencyData, LatencyData> =
    Correspondence.from(
        { actual: LatencyData?, expected: LatencyData? ->
            if (expected == null || actual == null) return@from actual == expected
            actual.eventAction == expected.eventAction &&
                actual.strokeAction == expected.strokeAction &&
                actual.strokeId == expected.strokeId &&
                actual.batchSize == expected.batchSize &&
                actual.batchIndex == expected.batchIndex &&
                actual.osDetectsEvent == expected.osDetectsEvent &&
                actual.strokesViewGetsAction == expected.strokesViewGetsAction &&
                actual.strokesViewFinishesDrawCalls == expected.strokesViewFinishesDrawCalls &&
                actual.estimatedPixelPresentationTime == expected.estimatedPixelPresentationTime &&
                actual.canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls ==
                    expected.canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls &&
                actual.hwuiInProgressStrokesRenderHelperData.finishesDrawCalls ==
                    expected.hwuiInProgressStrokesRenderHelperData.finishesDrawCalls
        },
        "equals",
    )
