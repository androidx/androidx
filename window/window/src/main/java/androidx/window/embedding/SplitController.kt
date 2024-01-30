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

package androidx.window.embedding

import android.app.Activity
import android.content.Context
import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowProperties
import androidx.window.WindowSdkExtensions
import androidx.window.core.ExperimentalWindowApi
import androidx.window.layout.WindowMetrics
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The controller class that gets information about the currently active activity
 * splits and provides interaction points to customize the splits and form new
 * splits.
 *
 * A split is a pair of containers that host activities in the same or different
 * processes, combined under the same parent window of the hosting task.
 *
 * A pair of activities can be put into a split by providing a static or runtime
 * split rule and then launching the activities in the same task using
 * [Activity.startActivity()][android.app.Activity.startActivity].
 */
class SplitController internal constructor(private val embeddingBackend: EmbeddingBackend) {

    /**
     * A [Flow] of [SplitInfo] list that contains the current split states that this [activity] is
     * part of.
     *
     * An activity can be in zero, one or more [active splits][SplitInfo].
     * More than one active split is possible if an activity created multiple
     * containers to side, stacked on top of each other. Or it can be in two
     * different splits at the same time - in a secondary container for one (it was
     * launched to the side) and in the primary for another (it launched another
     * activity to the side). The reported splits in the list are ordered from
     * bottom to top by their z-order, more recent splits appearing later.
     * Guaranteed to be called at least once to report the most recent state.
     *
     * @param activity The [Activity] that is interested in getting the split states
     * @return a [Flow] of [SplitInfo] list that includes this [activity]
     */
    fun splitInfoList(activity: Activity): Flow<List<SplitInfo>> = callbackFlow {
        val listener = Consumer { info: List<SplitInfo> -> trySend(info) }
        embeddingBackend.addSplitListenerForActivity(activity, Runnable::run, listener)
        awaitClose {
            embeddingBackend.removeSplitListenerForActivity(listener)
        }
    }

    /**
     * Indicates whether split functionality is supported on the device. Note
     * that devices might not enable splits in all states or conditions. For
     * example, a foldable device with multiple screens can choose to collapse
     * splits when apps run on the device's small display, but enable splits
     * when apps run on the device's large display. In cases like this,
     * [splitSupportStatus] always returns [SplitSupportStatus.SPLIT_AVAILABLE], and if the split is
     * collapsed, activities are launched on top, following the non-activity embedding model.
     *
     * Also the [androidx.window.WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED]
     * must be enabled in AndroidManifest within <application> in order to get the correct
     * state or [SplitSupportStatus.SPLIT_ERROR_PROPERTY_NOT_DECLARED] will be returned in some
     * cases.
     *
     * @see SplitSupportStatus
     */
    val splitSupportStatus: SplitSupportStatus
        get() = embeddingBackend.splitSupportStatus

    /**
     * Sets or replaces the previously registered [SplitAttributes] calculator.
     *
     * **Note** that it's callers' responsibility to check if this API is supported by checking
     * [WindowSdkExtensions.extensionVersion] before using the this API. It is suggested to always
     * set meaningful [SplitRule.defaultSplitAttributes] in case this API is not supported on some
     * devices.
     *
     * Also, replacing the calculator will only update existing split pairs after a change
     * in the window or device state, such as orientation changes or folding state changes.
     *
     * The [SplitAttributes] calculator is a function to compute the current [SplitAttributes] for
     * the given [SplitRule] with the current device and window state. Then The calculator will be
     * invoked if either:
     * - An activity is started and matches a registered [SplitRule].
     * - A parent configuration is updated and there's an existing split pair.
     *
     * By default, [SplitRule.defaultSplitAttributes] are applied if the parent container's
     * [WindowMetrics] satisfies the [SplitRule]'s dimensions requirements, which are
     * [SplitRule.minWidthDp], [SplitRule.minHeightDp] and [SplitRule.minSmallestWidthDp].
     * The [SplitRule.defaultSplitAttributes] can be set by
     * - [SplitRule] Builder APIs, which are
     *   [SplitPairRule.Builder.setDefaultSplitAttributes] and
     *   [SplitPlaceholderRule.Builder.setDefaultSplitAttributes].
     * - Specifying with `splitRatio` and `splitLayoutDirection` attributes in `<SplitPairRule>` or
     * `<SplitPlaceHolderRule>` tags in XML files.
     *
     * Developers may want to apply different [SplitAttributes] for different device or window
     * states. For example, on foldable devices, developers may want to split the screen vertically
     * if the device is in landscape, fill the screen if the device is in portrait and split
     * the screen horizontally if the device is in
     * [tabletop posture](https://developer.android.com/guide/topics/ui/foldables#postures).
     * In this case, the [SplitAttributes] can be customized by the [SplitAttributes] calculator,
     * which takes effects after calling this API. Developers can also clear the calculator
     * by [clearSplitAttributesCalculator].
     * Then, developers could implement the [SplitAttributes] calculator as the sample linked below
     * shows.
     *
     * @sample androidx.window.samples.embedding.splitAttributesCalculatorSample
     * @param calculator the function to calculate [SplitAttributes] based on the
     * [SplitAttributesCalculatorParams]. It will replace the previously set if it exists.
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion]
     *                                       is less than 2.
     */
    @RequiresWindowSdkExtension(2)
    fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ) {
        embeddingBackend.setSplitAttributesCalculator(calculator)
    }

    /**
     * Clears the callback previously set by [setSplitAttributesCalculator].
     * The caller **must** make sure if [WindowSdkExtensions.extensionVersion] is greater than
     * or equal to 2.
     *
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion]
     *                                       is less than 2.
     */
    @RequiresWindowSdkExtension(2)
    fun clearSplitAttributesCalculator() {
        embeddingBackend.clearSplitAttributesCalculator()
    }

    /**
     * Triggers a [SplitAttributes] update callback for the current topmost and visible split layout
     * if there is one. This method can be used when a change to the split presentation originates
     * from an application state change. Changes that are driven by parent window changes or new
     * activity starts invoke the callback provided in [setSplitAttributesCalculator] automatically
     * without the need to call this function.
     *
     * The top [SplitInfo] is usually the last element of [SplitInfo] list which was received from
     * the callback registered in [splitInfoList].
     *
     * The call will be ignored if there is no visible split.
     *
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion]
     *                                       is less than 3.
     */
    @ExperimentalWindowApi
    @RequiresWindowSdkExtension(3)
    fun invalidateTopVisibleSplitAttributes() {
        embeddingBackend.invalidateTopVisibleSplitAttributes()
    }

    /**
     * Updates the [SplitAttributes] of a split pair. This is an alternative to using
     * a split attributes calculator callback set in [setSplitAttributesCalculator], useful when
     * apps only need to update the splits in a few cases proactively but rely on the default split
     * attributes most of the time otherwise.
     *
     * The provided split attributes will be used instead of the associated
     * [SplitRule.defaultSplitAttributes].
     *
     * **Note** that the split attributes may be updated if split attributes calculator callback is
     * registered and invoked. If [setSplitAttributesCalculator] is used, the callback will still be
     * applied to each [SplitInfo] when there's either:
     * - A new Activity being launched.
     * - A window or device state updates (e,g. due to screen rotation or folding state update).
     *
     * In most cases it is suggested to use [invalidateTopVisibleSplitAttributes] if
     * [SplitAttributes] calculator callback is used.
     *
     * @param splitInfo the split pair to update
     * @param splitAttributes the [SplitAttributes] to be applied
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion]
     *                                       is less than 3.
     */
    @ExperimentalWindowApi
    @RequiresWindowSdkExtension(3)
    fun updateSplitAttributes(splitInfo: SplitInfo, splitAttributes: SplitAttributes) {
        embeddingBackend.updateSplitAttributes(splitInfo, splitAttributes)
    }

    /**
     * A class to determine if activity splits with Activity Embedding are currently available.
     * Depending on the split property declaration, device software version or user preferences
     * the feature might not be available.
     */
    class SplitSupportStatus private constructor(private val rawValue: Int) {
        override fun toString(): String {
            return when (rawValue) {
                0 -> "SplitSupportStatus: AVAILABLE"
                1 -> "SplitSupportStatus: UNAVAILABLE"
                2 -> "SplitSupportStatus: ERROR_SPLIT_PROPERTY_NOT_DECLARED"
                else -> "UNKNOWN"
            }
        }

        companion object {
            /**
             * The activity splits API is available and split rules can take effect depending on
             * the window state.
             */
            @JvmField
            val SPLIT_AVAILABLE = SplitSupportStatus(0)

            /**
             * The activity splits API is currently unavailable.
             */
            @JvmField
            val SPLIT_UNAVAILABLE = SplitSupportStatus(1)

            /**
             * Denotes that [WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED] has not
             * been set. This property must be set and enabled in AndroidManifest.xml to use splits
             * APIs.
             */
            @JvmField
            val SPLIT_ERROR_PROPERTY_NOT_DECLARED = SplitSupportStatus(2)
        }
    }

    companion object {

        internal const val sDebug = false

        /**
         * Obtains an instance of [SplitController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): SplitController {
            val backend = EmbeddingBackend.getInstance(context)
            return SplitController(backend)
        }
    }
}
