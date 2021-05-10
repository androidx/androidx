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

package androidx.window.test

import androidx.window.WindowInfoRepo
import androidx.window.WindowLayoutInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to help test consuming a stream of [WindowLayoutInfo] values.
 * [WindowLayoutInfoPublisherRule] allows you to push through different [WindowLayoutInfo] values
 * on demand.
 *
 * Here are some recommended testing scenarios.
 *
 * To test the scenario where no [WindowLayoutInfo] is ever emitted.  Just set the rule as a
 * standard rule.
 *
 * To test sending a generic feature build your own [WindowLayoutInfo] and publish it through the
 * method [WindowLayoutInfoPublisherRule.overrideWindowLayoutInfo].
 *
 * Some helper methods are provided to test the following scenarios.
 * <ul>
 *     <li>A fold in the middle and the dimension matches the shortest window dimension.</li>
 *     <li>A fold in the middle and the dimension matches the longest window dimension.</li>
 *     <li>A fold in the middle and has vertical orientation.</li>
 *     <li>A fold in the middle and has horizontal orientation.</li>
 * </ul>
 */
public class WindowLayoutInfoPublisherRule() : TestRule {

    private val flow = MutableSharedFlow<WindowLayoutInfo>(
        extraBufferCapacity = 1,
        onBufferOverflow = DROP_OLDEST
    )
    private val overrideServices = PublishWindowInfoRepoDecorator(flow)

    @ExperimentalCoroutinesApi
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                WindowInfoRepo.overrideDecorator(overrideServices)
                base.evaluate()
                WindowInfoRepo.reset()
            }
        }
    }

    /**
     * Send an arbitrary [WindowLayoutInfo] through
     * [androidx.window.WindowInfoRepo.windowLayoutInfo]. Each event is sent only once.
     */
    public fun overrideWindowLayoutInfo(info: WindowLayoutInfo) {
        flow.tryEmit(info)
    }
}