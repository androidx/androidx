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
package androidx.fragment.app

import android.os.SystemClock
import android.transition.Transition
import androidx.annotation.LayoutRes
import androidx.fragment.test.R
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify

/**
 * A fragment that has transitions that can be tracked.
 */
open class TransitionFragment(
    @LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment
) : StrictViewFragment(contentLayoutId) {
    val enterTransition = TrackingVisibility()
    val reenterTransition = TrackingVisibility()
    val exitTransition = TrackingVisibility()
    val returnTransition = TrackingVisibility()
    val sharedElementEnter = TrackingTransition()
    val sharedElementReturn = TrackingTransition()

    private val listener = mock(Transition.TransitionListener::class.java)

    init {
        @Suppress("LeakingThis")
        setEnterTransition(enterTransition)
        @Suppress("LeakingThis")
        setReenterTransition(reenterTransition)
        @Suppress("LeakingThis")
        setExitTransition(exitTransition)
        @Suppress("LeakingThis")
        setReturnTransition(returnTransition)
        sharedElementEnterTransition = sharedElementEnter
        sharedElementReturnTransition = sharedElementReturn
        enterTransition.addListener(listener)
        sharedElementEnter.addListener(listener)
        reenterTransition.addListener(listener)
        exitTransition.addListener(listener)
        returnTransition.addListener(listener)
        sharedElementReturn.addListener(listener)
    }

    internal fun waitForTransition() {
        verify(
            listener,
            CtsMockitoUtils.within(1000)
        ).onTransitionEnd(ArgumentMatchers.any())
        reset(listener)
    }

    internal fun waitForNoTransition() {
        SystemClock.sleep(250)
        verify(
            listener,
            never()
        ).onTransitionStart(ArgumentMatchers.any())
    }
}
