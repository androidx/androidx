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

package androidx.ui.scheduler.ticker

import androidx.ui.core.Duration

/**
 * Signature for the callback passed to the [Ticker] class's constructor.
 *
 * The argument is the time that the object had spent enabled so far
 * at the time of the callback being called.
 */
typealias TickerCallback = (elapsed: Duration) -> Unit

/**
 * An interface implemented by classes that can vend [Ticker] objects.
 *
 * Tickers can be used by any object that wants to be notified whenever a frame
 * triggers, but are most commonly used indirectly via an
 * [AnimationController]. [AnimationController]s need a [TickerProvider] to
 * obtain their [Ticker]. If you are creating an [AnimationController] from a
 * [State], then you can use the [TickerProviderStateMixin] and
 * [SingleTickerProviderStateMixin] classes to obtain a suitable
 * [TickerProvider]. The widget test framework [WidgetTester] object can be
 * used as a ticker provider in the context of tests. In other contexts, you
 * will have to either pass a [TickerProvider] from a higher level (e.g.
 * indirectly from a [State] that mixes in [TickerProviderStateMixin]), or
 * create a custom [TickerProvider] subclass.
 */
interface TickerProvider {
    /**
     * Creates a ticker with the given callback.
     *
     * The kind of ticker provided depends on the kind of ticker provider.
     */
    fun createTicker(onTick: TickerCallback): Ticker
}