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

package androidx.ui.layout.test

import android.app.Instrumentation
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.Dp
import androidx.ui.core.OnPositioned
import androidx.ui.core.Position
import androidx.ui.core.Size
import androidx.ui.core.adapter.CraneWrapper
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.times
import androidx.ui.core.toDp
import androidx.ui.core.unaryMinus
import androidx.ui.layout.Center
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Padding
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composeInto
import com.google.r4a.composer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class PaddingTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var activity: TestActivity
    private lateinit var instrumentation: Instrumentation
    private lateinit var handler: Handler
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        instrumentation = InstrumentationRegistry.getInstrumentation()
        // Kotlin IR compiler doesn't seem too happy with auto-conversion from
        // lambda to Runnable, so separate it here
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    @Test
    fun testPaddingIsApplied() {
        val size = 50.dp
        val padding = 10.dp

        val drawLatch = CountDownLatch(10)
        var childSize = Size(-1.dp, -1.dp)
        var childPosition = Position(-1.dp, -1.dp)
        show @Composable {
            <Center>
                <ConstrainedBox additionalConstraints=Constraints.tightConstraints(size, size)>
                    <Padding padding=EdgeInsets(padding)>
                        <Container padding=null color=null alignment=null margin=null
                                            constraints=null width=null height=null>
                            <OnPositioned> coordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.localToGlobal(Position(0.dp, 0.dp))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    </Padding>
                </ConstrainedBox>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(Size(size - padding * 2, size - padding * 2), childSize)
        val width = root.width.toDp(density)
        val height = root.height.toDp(density)
        assertEquals(
            Position(width / 2 - size / 2 + padding, height / 2 - size / 2 + padding),
            childPosition
        )
    }

    @Test
    fun testPaddingIsApplied_withDifferentInsets() {
        val size = 50.dp
        val padding = EdgeInsets(10.dp, 15.dp, 20.dp, 30.dp)

        val drawLatch = CountDownLatch(10)
        var childSize = Size(-1.dp, -1.dp)
        var childPosition = Position(-1.dp, -1.dp)
        show @Composable {
            <Center>
                <ConstrainedBox additionalConstraints=Constraints.tightConstraints(size, size)>
                    <Padding padding>
                        <Container padding=null color=null alignment=null margin=null
                                            constraints=null width=null height=null>
                            <OnPositioned> coordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.localToGlobal(Position(0.dp, 0.dp))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    </Padding>
                </ConstrainedBox>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(
            Size(size - padding.left - padding.right, size - padding.top - padding.bottom),
            childSize
        )
        val width = root.width.toDp(density)
        val height = root.height.toDp(density)
        assertEquals(
            Position(
                width / 2 - size / 2 + padding.left,
                height / 2 - size / 2 + padding.top
            ),
            childPosition
        )
    }

    @Test
    fun testPadding_withInsufficientSpace() {
        val size = 50.dp
        val padding = 30.dp

        val drawLatch = CountDownLatch(10)
        var childSize = Size(-1.dp, -1.dp)
        var childPosition = Position(-1.dp, -1.dp)
        show @Composable {
            <Center>
                <ConstrainedBox additionalConstraints=Constraints.tightConstraints(size, size)>
                    <Padding padding=EdgeInsets(padding)>
                        <Container padding=null color=null alignment=null margin=null
                                            constraints=null width=null height=null>
                            <OnPositioned> coordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.localToGlobal(Position(0.dp, 0.dp))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    </Padding>
                </ConstrainedBox>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(Size(0.dp, 0.dp), childSize)
        val width = root.width.toDp(density)
        val height = root.height.toDp(density)
        assertEquals(
            Position(
                width / 2 - size / 2 + padding,
                height / 2 - size / 2 + padding
            ),
            childPosition
        )
    }

    private fun show(@Children composable: () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.composeInto {
                    <CraneWrapper>
                        <composable />
                    </CraneWrapper>
                }
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    private fun findAndroidCraneView(): AndroidCraneView {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findAndroidCraneView(contentViewGroup)!!
    }

    private fun findAndroidCraneView(parent: ViewGroup): AndroidCraneView? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is AndroidCraneView) {
                return child
            } else if (child is ViewGroup) {
                val craneView = findAndroidCraneView(child)
                if (craneView != null) {
                    return craneView
                }
            }
        }
        return null
    }

    private fun waitForDraw(view: View) {
        val viewDrawLatch = CountDownLatch(1)
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                viewDrawLatch.countDown()
            }
        }
        view.viewTreeObserver.addOnDrawListener(listener)
        view.invalidate()
        assertTrue(viewDrawLatch.await(1, TimeUnit.SECONDS))
    }
}
