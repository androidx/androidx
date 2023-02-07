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
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.window.WindowProperties
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitController.Api31Impl.isSplitPropertyEnabled
import androidx.window.layout.WindowMetrics
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
* A singleton controller class that gets information about the currently active activity
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
class SplitController private constructor(private val applicationContext: Context) {
    private val embeddingBackend: EmbeddingBackend = ExtensionEmbeddingBackend
        .getInstance(applicationContext)
    private var splitPropertyEnabled: Boolean = false

    // TODO(b/258356512): Make this method a flow API
    /**
     * Registers a listener for updates about the active split state(s) that this
     * activity is part of. An activity can be in zero, one or more active splits.
     * More than one active split is possible if an activity created multiple
     * containers to side, stacked on top of each other. Or it can be in two
     * different splits at the same time - in a secondary container for one (it was
     * launched to the side) and in the primary for another (it launched another
     * activity to the side). The reported splits in the list are ordered from
     * bottom to top by their z-order, more recent splits appearing later.
     * Guaranteed to be called at least once to report the most recent state.
     *
     * @param activity only split that this [Activity] is part of will be reported.
     * @param executor when there is an update to the active split state(s), the [consumer] will be
     * invoked on this [Executor].
     * @param consumer [Consumer] that will be invoked on the [executor] when there is an update to
     * the active split state(s).
     */
    fun addSplitListener(
        activity: Activity,
        executor: Executor,
        consumer: Consumer<List<SplitInfo>>
    ) {
        embeddingBackend.addSplitListenerForActivity(activity, executor, consumer)
    }

    /**
     * Unregisters a listener that was previously registered via [addSplitListener].
     *
     * @param consumer the previously registered [Consumer] to unregister.
     */
    fun removeSplitListener(
        consumer: Consumer<List<SplitInfo>>
    ) {
        embeddingBackend.removeSplitListenerForActivity(consumer)
    }

    /**
     * Indicates whether split functionality is supported on the device. Note
     * that devices might not enable splits in all states or conditions. For
     * example, a foldable device with multiple screens can choose to collapse
     * splits when apps run on the device's small display, but enable splits
     * when apps run on the device's large display. In cases like this,
     * `isSplitSupported` always returns `true`, and if the split is collapsed,
     * activities are launched on top, following the non-activity embedding
     * model.
     *
     * Also the [androidx.window.WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED]
     * must be enabled in AndroidManifest within <application> in order to get the correct
     * state or `false` will be returned by default.
     */
    fun isSplitSupported(): Boolean {
        if (!splitPropertyEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                splitPropertyEnabled = isSplitPropertyEnabled(applicationContext)
            } else {
                // The PackageManager#getProperty API is not supported before S, assuming
                // the property is enabled to keep the same behavior on earlier platforms.
                splitPropertyEnabled = true
            }
        }
        return splitPropertyEnabled && embeddingBackend.isSplitSupported()
    }

    /**
     * Sets or replaces the previously registered [SplitAttributes] calculator.
     *
     * **Note** that it's callers' responsibility to check if this API is supported by calling
     * [isSplitAttributesCalculatorSupported] before using the this API. It is suggested to always
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
     * @throws UnsupportedOperationException if [isSplitAttributesCalculatorSupported] reports
     * `false`
     */
    @ExperimentalWindowApi
    fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ) {
        embeddingBackend.setSplitAttributesCalculator(calculator)
    }

    /**
     * Clears the callback previously set by [setSplitAttributesCalculator].
     * The caller **must** make sure [isSplitAttributesCalculatorSupported] before invoking.
     *
     * @throws UnsupportedOperationException if [isSplitAttributesCalculatorSupported] reports
     * `false`
     */
    @ExperimentalWindowApi
    fun clearSplitAttributesCalculator() {
        embeddingBackend.clearSplitAttributesCalculator()
    }

    /** Returns whether [setSplitAttributesCalculator] is supported or not. */
    @ExperimentalWindowApi
    fun isSplitAttributesCalculatorSupported(): Boolean =
        embeddingBackend.isSplitAttributesCalculatorSupported()

    companion object {
        @Volatile
        private var globalInstance: SplitController? = null
        private val globalLock = ReentrantLock()
        private const val TAG = "SplitController"

        internal const val sDebug = false

        /**
         * Obtains the singleton instance of [SplitController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): SplitController {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        globalInstance = SplitController(context.applicationContext)
                    }
                }
            }
            return globalInstance!!
        }
    }

    @RequiresApi(31)
    private object Api31Impl {
        @DoNotInline
        fun isSplitPropertyEnabled(applicationContext: Context): Boolean {
            val property = try {
                applicationContext.packageManager.getProperty(
                    WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED,
                    applicationContext.packageName
                )
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(
                    TAG, WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED +
                    " must be set and enabled in AndroidManifest.xml to use splits APIs."
                )
                return false
            } catch (e: Exception) {
                // This can happen when it is a test environment that doesn't support getProperty.
                Log.e(TAG, "PackageManager.getProperty is not supported", e)
                return false
            }
            if (!property.isBoolean) {
                Log.e(
                    TAG, WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED +
                    " must have a boolean value"
                )
                return false
            }
            return property.boolean
        }
    }
}