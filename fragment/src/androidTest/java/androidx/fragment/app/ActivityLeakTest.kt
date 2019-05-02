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

package androidx.fragment.app

import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.ref.WeakReference

/**
 * Class representing the different configurations for testing leaks
 */
sealed class LeakConfiguration {
    abstract fun commit(fragmentManager: FragmentManager): Fragment?

    override fun toString(): String = this.javaClass.simpleName
}

object NotRetained : LeakConfiguration() {
    override fun commit(fragmentManager: FragmentManager) = StrictFragment().also {
        fragmentManager.beginTransaction()
            .add(it, "tag")
            .commitNow()
    }
}

object Retained : LeakConfiguration() {
    override fun commit(fragmentManager: FragmentManager) = StrictFragment().apply {
        retainInstance = true
    }.also {
        fragmentManager.beginTransaction()
            .add(it, "tag")
            .commitNow()
    }
}

object NoChild : LeakConfiguration() {
    override fun commit(fragmentManager: FragmentManager): Fragment? = null
}

@LargeTest
@RunWith(Parameterized::class)
class ActivityLeakTest(
    private val parentConfiguration: LeakConfiguration,
    private val childConfiguration: LeakConfiguration
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "parent={0}, child={1}")
        fun data() = mutableListOf<Array<Any>>().apply {
            arrayOf(
                NotRetained,
                Retained
            ).forEach { operation ->
                add(arrayOf(operation, NotRetained))
                add(arrayOf(operation, Retained))
                add(arrayOf(operation, NoChild))
            }
        }
    }

    @Test
    fun testActivityDoesNotLeak() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val weakRef = withActivity { WeakReference(this) }
            onActivity { activity ->
                val parent = parentConfiguration.commit(activity.supportFragmentManager)!!
                childConfiguration.commit(parent.childFragmentManager)
            }
            recreate()
            forceGC()
            assertWithMessage("Old activity should be garbage collected")
                .that(weakRef.get())
                .isNull()
        }
    }
}
