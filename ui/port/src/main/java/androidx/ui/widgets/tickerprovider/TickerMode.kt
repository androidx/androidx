/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.widgets.tickerprovider

import androidx.ui.Type
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.InheritedWidget
import androidx.ui.widgets.framework.Widget

/**
 * Enables or disables tickers (and thus animation controllers) in the widget
 * subtree.
 *
 * This only works if [AnimationController] objects are created using
 * widget-aware ticker providers. For example, using a
 * [TickerProviderStateMixin] or a [SingleTickerProviderStateMixin].
 */
class TickerMode(
    key: Key,
    /**
     * The current ticker mode of this subtree.
     *
     * If true, then tickers in this subtree will tick.
     *
     * If false, then tickers in this subtree will not tick. Animations driven by
     * such tickers are not paused, they just don't call their callbacks. Time
     * still elapses.
     */
    private val enabled: Boolean,
    child: Widget

) : InheritedWidget(key, child) {

    companion object {

        /**
         * Whether tickers in the given subtree should be enabled or disabled.
         *
         * This is used automatically by [TickerProviderStateMixin] and
         * [SingleTickerProviderStateMixin] to decide if their tickers should be
         * enabled or disabled.
         *
         * In the absence of a [TickerMode] widget, this function defaults to true.
         *
         * Typical usage is as follows:
         *
         * ```dart
         * bool tickingEnabled = TickerMode.of(context);
         * ```
         */
        fun of(context: BuildContext): Boolean {
            val widget: TickerMode? = context.inheritFromWidgetOfExactType(
                Type(TickerMode::class.java)) as TickerMode?
            return widget?.enabled ?: true
        }
    }

    override fun updateShouldNotify(oldWidget: InheritedWidget) =
        enabled != (oldWidget as TickerMode).enabled

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(FlagProperty("mode", value = enabled, ifTrue = "enabled",
            ifFalse = "disabled", showName = true))
    }
}