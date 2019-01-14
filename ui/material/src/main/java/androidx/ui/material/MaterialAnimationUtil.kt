/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.ui.animation.Tween
import androidx.ui.animation.TweenEvaluator
import androidx.ui.core.Dimension
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.times
import androidx.ui.engine.window.Window
import androidx.ui.scheduler.ticker.Ticker
import androidx.ui.scheduler.ticker.TickerCallback
import androidx.ui.scheduler.ticker.TickerProvider
import androidx.ui.widgets.binding.WidgetsFlutterBinding
import com.google.r4a.Children
import com.google.r4a.Composable


// TODO("Migration|Andrey: Utils for using AnimationController in material package")
// TODO("Migration|Andrey: We will replace it with our Swan animation framework later")

/**
 * Singleton ticker provider.
 */
private val tickerProvider = object : TickerProvider {

    val schedulerBinding = WidgetsFlutterBinding.create(Window())

    override fun createTicker(onTick: TickerCallback): Ticker {
        return Ticker(onTick, schedulerBinding = schedulerBinding)
    }
}

/**
 * Composable to gather the ticker provider and start animation.
 */
@Composable
fun UseTickerProvider(@Children children: (ticker: TickerProvider) -> Unit) {
    <children ticker=tickerProvider />
}

/**
 * A Tween implementation for Dimension class.
 */
fun Tween(begin: Dimension? = null, end: Dimension? = null) =
    Tween(begin, end, DimensionTweenEvaluator)

private object DimensionTweenEvaluator : TweenEvaluator<Dimension> {
    override fun invoke(begin: Dimension, end: Dimension, t: Float): Dimension {
        return begin + ((end - begin) * t)
    }
}
