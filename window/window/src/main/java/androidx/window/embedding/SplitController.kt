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
import androidx.window.core.ExperimentalWindowApi
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Controller class that will be used to get information about the currently active activity splits,
 * as well as provide interaction points to customize them and form new splits. A split is a pair of
 * containers that host activities in the same or different tasks, combined under the same parent
 * window of the hosting task.
 * <p>A pair of activities can be put into split by providing a static or runtime split rule and
 * launching activity in the same task and process using [android.content.Context.startActivity].
 * <p>This class should be configured before [android.app.Application.onCreate] for upcoming
 * activity launches using the split rules statically defined in an XML using
 * [androidx.startup.Initializer] and [Companion.initialize]. See Jetpack App Startup reference
 * for more information.
 */
@ExperimentalWindowApi
class SplitController private constructor() {
    private val embeddingBackend: EmbeddingBackend = ExtensionEmbeddingBackend.getInstance()
    private var staticSplitRules: Set<EmbeddingRule> = emptySet()

    /**
     * Returns a copy of the currently applied split configurations.
     */
    fun getSplitRules(): Set<EmbeddingRule> {
        return embeddingBackend.getSplitRules().toSet()
    }

    /**
     * Registers a new runtime rule. Will be cleared automatically when the process is stopped.
     */
    fun registerRule(rule: EmbeddingRule) {
        embeddingBackend.registerRule(rule)
    }

    /**
     * Unregisters a runtime rule that was previously registered via [SplitController.registerRule].
     */
    fun unregisterRule(rule: EmbeddingRule) {
        embeddingBackend.unregisterRule(rule)
    }

    /**
     * Unregisters all runtime rules added with [registerRule].
     */
    fun clearRegisteredRules() {
        embeddingBackend.setSplitRules(staticSplitRules)
    }

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
     */
    fun addSplitListener(
        activity: Activity,
        executor: Executor,
        consumer: Consumer<List<SplitInfo>>
    ) {
        embeddingBackend.registerSplitListenerForActivity(activity, executor, consumer)
    }

    /**
     * Unregisters a listener for updates about the active split states.
     */
    fun removeSplitListener(
        consumer: Consumer<List<SplitInfo>>
    ) {
        embeddingBackend.unregisterSplitListenerForActivity(consumer)
    }

    /**
     * Indicates whether the split functionality is supported on the device. Note
     * that the device might enable splits in all conditions, but it should be
     * available in some states that the device supports. An example can be a
     * foldable device with multiple screens can choose to collapse all splits for
     * apps running on a small display, but enable when running on a larger
     * one - on such devices this method will always return "true".
     * If the split is not supported, activities will be launched on top, following
     * the regular model.
     */
    fun isSplitSupported(): Boolean {
        return embeddingBackend.isSplitSupported()
    }

    private fun setStaticSplitRules(staticRules: Set<EmbeddingRule>) {
        staticSplitRules = staticRules
        embeddingBackend.setSplitRules(staticRules)
    }

    companion object {
        @Volatile
        private var globalInstance: SplitController? = null
        private val globalLock = ReentrantLock()

        internal const val sDebug = false

        /**
         * Gets the shared instance of the class.
         */
        @JvmStatic
        fun getInstance(): SplitController {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        globalInstance = SplitController()
                    }
                }
            }
            return globalInstance!!
        }

        /**
         * Initializes the shared class instance with the split rules statically defined in an
         * app-provided XML. The rules will be kept for the lifetime of the application process.
         * <p>It's recommended to set the static rules via an [androidx.startup.Initializer], so
         * that they are applied early in the application startup before any activities appear.
         */
        @JvmStatic
        fun initialize(context: Context, staticRuleResourceId: Int) {
            val parser = SplitRuleParser()
            val configs = parser.parseSplitRules(context, staticRuleResourceId)
            val controllerInstance = getInstance()
            controllerInstance.setStaticSplitRules(configs ?: emptySet())
        }
    }
}