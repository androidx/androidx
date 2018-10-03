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

import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.runtimeType
import androidx.ui.scheduler.binding.SchedulerBinding
import androidx.ui.scheduler.ticker.Ticker
import androidx.ui.scheduler.ticker.TickerCallback
import androidx.ui.scheduler.ticker.TickerProvider
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget

/**
 * This class should really be called _DisposingTicker or some such, but this
 * class name leaks into stack traces and error messages and that name would be
 * confusing. Instead we use the less precise but more anodyne "WidgetTicker",
 * which attracts less attention.
 */
internal class WidgetTicker(
    onTick: TickerCallback,
    private val creator: TickerProviderStateMixin<*>,
    debugLabel: String? = null,
    schedulerBinding: SchedulerBinding
) : Ticker(onTick, debugLabel, schedulerBinding) {

    override fun dispose() {
        creator.removeTicker(this)
        super.dispose()
    }
}

/**
 * Provides [Ticker] objects that are configured to only tick while the current
 * tree is enabled, as defined by [TickerMode].
 *
 * To create an [AnimationController] in a class that uses this mixin, pass
 * `vsync: this` to the animation controller constructor whenever you
 * create a new animation controller.
 *
 * If you only have a single [Ticker] (for example only a single
 * [AnimationController]) for the lifetime of your [State], then using a
 * [SingleTickerProviderStateMixin] is more efficient. This is the common case.
 */
abstract class TickerProviderStateMixin<T : StatefulWidget>(
    widget: T
) : State<T>(widget), TickerProvider {

    private var tickers: MutableSet<Ticker>? = null

    override fun createTicker(onTick: TickerCallback): Ticker {
        if (tickers == null) {
            tickers = mutableSetOf()
        }
        val result = WidgetTicker(
            onTick, this,
            debugLabel = "created by $this",
            schedulerBinding = schedulerBinding
        )
        tickers!!.add(result)
        return result
    }

    internal fun removeTicker(ticker: WidgetTicker) {
        val finalTickers = tickers
        assert(finalTickers != null)
        assert(finalTickers!!.contains(ticker))
        finalTickers.remove(ticker)
    }

    override fun dispose() {
        assert {
            tickers?.forEach {
                if (it.isActive) {
                    throw FlutterError(
                        "$this was disposed with an active Ticker.\n${runtimeType()} created a " +
                                "Ticker via its TickerProviderStateMixin, but at the time " +
                                "dispose() was called on the mixin, that Ticker was still " +
                                "active. All Tickers must be disposed before calling " +
                                "super.dispose(). Tickers used by AnimationControllers should " +
                                "be disposed by calling dispose() on the AnimationController " +
                                "itself. Otherwise, the ticker will leak.\n The offending ticker " +
                                "was: ${it.toStringParametrized(debugIncludeStack = true)}"
                    )
                }
            }
            true
        }
        super.dispose()
    }

    override fun didChangeDependencies() {
        val muted = !TickerMode.of(context!!)
        if (tickers != null) {
            for (ticker in tickers!!)
                ticker.muted = muted
        }
        super.didChangeDependencies()
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        val finalTickers = tickers
        properties.add(
            DiagnosticsProperty.create(
                "tickers",
                finalTickers,
                description = if (finalTickers == null) null else
                    "tracking ${finalTickers.size} ticker${if (finalTickers.size == 1)
                        "" else "s"}",
                defaultValue = null
            )
        )
    }
}
