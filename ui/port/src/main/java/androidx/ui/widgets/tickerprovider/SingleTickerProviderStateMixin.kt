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
import androidx.ui.scheduler.ticker.Ticker
import androidx.ui.scheduler.ticker.TickerCallback
import androidx.ui.scheduler.ticker.TickerProvider
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget

/**
 * Provides a single [Ticker] that is configured to only tick while the current
 * tree is enabled, as defined by [TickerMode].
 *
 * To create the [AnimationController] in a [State] that only uses a single
 * [AnimationController], mix in this class, then pass `vsync: this`
 * to the animation controller constructor.
 *
 * This mixin only supports vending a single ticker. If you might have multiple
 * [AnimationController] objects over the lifetime of the [State], use a full
 * [TickerProviderStateMixin] instead.
 */
abstract class SingleTickerProviderStateMixin<T : StatefulWidget>(
    widget: T
) : State<T>(widget), TickerProvider {

    private var ticker: Ticker? = null

    override fun createTicker(onTick: TickerCallback): Ticker {
        assert {
            if (ticker == null) {
                true
            } else {
                throw FlutterError(
                    "${runtimeType()} is a SingleTickerProviderStateMixin but multiple tickers " +
                            "were created.\nA SingleTickerProviderStateMixin can only be used as " +
                            "a TickerProvider once. If a State is used for multiple " +
                            "AnimationController objects, or if it is passed to other objects " +
                            "and those objects might use it more than one time in total, then " +
                            "instead of mixing in a SingleTickerProviderStateMixin, use a " +
                            "regular TickerProviderStateMixin."
                )
            }
        }
        // We assume that this is called from initState, build, or some sort of
        // event handler, and that thus TickerMode.of(context) would return true. We
        // cant actually check that here because if were in initState then were
        // not allowed to do inheritance checks yet.
        return Ticker(onTick, "created by $this", schedulerBinding).also {
            ticker = it
        }
    }

    override fun dispose() {
        assert {
            if (ticker == null || !ticker!!.isActive) {
                true
            } else {
                throw FlutterError(
                    "$this was disposed with an active Ticker.\n${runtimeType()} created a " +
                            "Ticker via its SingleTickerProviderStateMixin, but at the time " +
                            "dispose() was called on the mixin, that Ticker was still active. " +
                            "The Ticker must be disposed before calling super.dispose(). " +
                            "Tickers used by AnimationControllers should be disposed by calling " +
                            "dispose() on the AnimationController itself. Otherwise, the ticker " +
                            "will leak.\nThe offending ticker was: " +
                            ticker!!.toStringParametrized(debugIncludeStack = true)
                )
            }
        }
        super.dispose()
    }

    override fun didChangeDependencies() {
        ticker?.muted = !TickerMode.of(context!!)
        super.didChangeDependencies()
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        var tickerDescription = ""
        val finalTicker = ticker
        if (finalTicker != null) {
            if (finalTicker.isActive && finalTicker.muted) {
                tickerDescription = "active but muted"
            } else if (finalTicker.isActive) {
                tickerDescription = "active"
            } else if (finalTicker.muted) {
                tickerDescription = "inactive and muted"
            } else {
                tickerDescription = "inactive"
            }
        }
        properties.add(
            DiagnosticsProperty.create(
                "ticker", finalTicker,
                description = tickerDescription, showSeparator = false, defaultValue = null
            )
        )
    }
}