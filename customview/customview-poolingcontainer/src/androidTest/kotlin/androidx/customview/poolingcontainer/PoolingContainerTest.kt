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

package androidx.customview.poolingcontainer

import android.view.View
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

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
@SmallTest
@RunWith(AndroidJUnit4::class)
class PoolingContainerTest {
    @Test
    fun listenerIsCalledExactlyOnce() {
        val view = View(InstrumentationRegistry.getInstrumentation().context)
        var callbacks = 0
        view.addPoolingContainerListener {
            callbacks++
        }
        view.callPoolingContainerOnRelease()
        assertThat(callbacks).isEqualTo(1)
    }

    @Test
    fun listenerRemoved_notCalled() {
        val view = View(InstrumentationRegistry.getInstrumentation().context)
        var callbacks = 0
        val listener = PoolingContainerListener {
            callbacks++
        }
        view.addPoolingContainerListener(listener)
        view.removePoolingContainerListener(listener)
        view.callPoolingContainerOnRelease()
        assertThat(callbacks).isEqualTo(0)
    }

    @Test
    fun complexHierarchy_listenersCalled_isWithinPoolingContainerWorks() {
        val callbacks = intArrayOf(0, 0, 0, 0)
        val topLevelParent =
            LinearLayout(InstrumentationRegistry.getInstrumentation().context).also {
                it.isPoolingContainer = true
            }
        val secondLevelParent1 =
            LinearLayout(InstrumentationRegistry.getInstrumentation().context).also {
                topLevelParent.addView(it)
            }
        val secondLevelParent2 =
            LinearLayout(InstrumentationRegistry.getInstrumentation().context).also {
                topLevelParent.addView(it)
                it.addPoolingContainerListener {
                    callbacks[0]++
                }
            }
        val secondLevelView = View(InstrumentationRegistry.getInstrumentation().context).also {
            topLevelParent.addView(it)
            it.addPoolingContainerListener {
                callbacks[1]++
            }
        }
        val thirdLevelView1 = View(InstrumentationRegistry.getInstrumentation().context).also {
            secondLevelParent1.addView(it)
            it.addPoolingContainerListener {
                callbacks[2]++
            }
        }
        val thirdLevelView2 = View(InstrumentationRegistry.getInstrumentation().context).also {
            secondLevelParent2.addView(it)
            it.addPoolingContainerListener {
                callbacks[3]++
            }
        }

        assertThat(topLevelParent.isWithinPoolingContainer).isFalse()
        listOf(
            secondLevelParent1, secondLevelParent2, secondLevelView, thirdLevelView1,
            thirdLevelView2
        ).forEach {
            assertThat(it.isWithinPoolingContainer).isTrue()
        }

        topLevelParent.callPoolingContainerOnRelease()
        // All listeners called exactly once
        assertThat(callbacks).isEqualTo(intArrayOf(1, 1, 1, 1))
    }

    @Test
    // While this test looks trivial, the implementation is more than just a boolean setter
    fun isPoolingContainerTest() {
        val view = LinearLayout(InstrumentationRegistry.getInstrumentation().context)
        val child = View(InstrumentationRegistry.getInstrumentation().context).also {
            view.addView(it)
        }

        assertThat(view.isPoolingContainer).isFalse()
        assertThat(child.isPoolingContainer).isFalse()

        view.isPoolingContainer = true
        assertThat(view.isPoolingContainer).isTrue()
        assertThat(child.isPoolingContainer).isFalse()
    }

    @Test
    fun isWithinPoolingContainerTest() {
        val parent = LinearLayout(InstrumentationRegistry.getInstrumentation().context)
        val child = View(InstrumentationRegistry.getInstrumentation().context).also {
            parent.addView(it)
        }

        assertThat(parent.isWithinPoolingContainer).isFalse()
        assertThat(child.isWithinPoolingContainer).isFalse()

        parent.isPoolingContainer = true
        assertThat(parent.isWithinPoolingContainer).isFalse()
        assertThat(child.isWithinPoolingContainer).isTrue()

        parent.isPoolingContainer = false
        assertThat(parent.isWithinPoolingContainer).isFalse()
        assertThat(child.isWithinPoolingContainer).isFalse()
    }

    @Test
    fun multipleListenersTest() {
        val callbacks = intArrayOf(0, 0)
        val view = View(InstrumentationRegistry.getInstrumentation().context)

        val listener1 = PoolingContainerListener {
            callbacks[0]++
        }
        view.addPoolingContainerListener(listener1)
        view.addPoolingContainerListener {
            callbacks[1]++
        }

        view.callPoolingContainerOnRelease()
        assertThat(callbacks).isEqualTo(intArrayOf(1, 1))

        view.removePoolingContainerListener(listener1)
        view.callPoolingContainerOnRelease()
        assertThat(callbacks).isEqualTo(intArrayOf(1, 2))
    }

    @Test
    fun listenerRemovedInCallback() {
        val callbacks = intArrayOf(0, 0, 0)

        val view = View(InstrumentationRegistry.getInstrumentation().context)
        view.addPoolingContainerListener(object : PoolingContainerListener {
            override fun onRelease() {
                callbacks[0]++
                view.removePoolingContainerListener(this)
            }
        })
        view.addPoolingContainerListener {
            callbacks[1]++
        }
        view.addPoolingContainerListener(object : PoolingContainerListener {
            override fun onRelease() {
                callbacks[2]++
                view.removePoolingContainerListener(this)
            }
        })

        view.callPoolingContainerOnRelease()
        assertThat(callbacks).isEqualTo(intArrayOf(1, 1, 1))

        view.callPoolingContainerOnRelease()
        assertThat(callbacks).isEqualTo(intArrayOf(1, 2, 1))
    }
}