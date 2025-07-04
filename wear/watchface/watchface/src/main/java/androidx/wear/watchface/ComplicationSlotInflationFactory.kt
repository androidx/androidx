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

package androidx.wear.watchface

/**
 * A ComplicationSlotInflationFactory provides the [CanvasComplicationFactory] and where necessary
 * edge complication [ComplicationTapFilter]s needed for inflating [ComplicationSlot]s.
 *
 * If a watch face doesn't define it's [ComplicationSlot]s in XML then this isn't used.
 *
 * @deprecated use Watch Face Format instead
 */
@Deprecated(
    message =
        "AndroidX watchface libraries are deprecated, use Watch Face Format instead. For more info see: https://developer.android.com/training/wearables/wff"
)
public abstract class ComplicationSlotInflationFactory {
    /** Returns the [CanvasComplicationFactory] to be used for the given [slotId]. */
    abstract fun getCanvasComplicationFactory(slotId: Int): CanvasComplicationFactory

    /**
     * Returns the [ComplicationTapFilter] to be used for the given edge [slotId]. Note not all
     * watch faces have edge complications.
     */
    @SuppressWarnings("DocumentExceptions")
    open fun getEdgeComplicationTapFilter(slotId: Int): ComplicationTapFilter {
        throw UnsupportedOperationException("You need to override getEdgeComplicationTapFilter")
    }
}
