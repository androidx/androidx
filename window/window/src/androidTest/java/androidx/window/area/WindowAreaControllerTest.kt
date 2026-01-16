/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.window.area

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.window.WindowTestUtils.Companion.assumeAtLeastWindowExtensionVersion
import androidx.window.core.ExperimentalWindowApi
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalWindowApi::class)
class WindowAreaControllerTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val minVendorApiLevel = 3

    /** Verifies that [WindowAreaController.getOrCreate] returns the cached object */
    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testGetOrCreateReturnsSameInstance(): Unit =
        testScope.runTest {
            assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            val controller1 = WindowAreaController.getOrCreate()

            val controller2 = WindowAreaController.getOrCreate()

            assertTrue(controller1 === controller2, "Objects returned are not the same object")
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun testOverrideDecoratorUpdatesInstance(): Unit =
        testScope.runTest {
            assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            assumeAtLeastWindowExtensionVersion(minVendorApiLevel)
            val controller1 = WindowAreaController.getOrCreate()

            WindowAreaController.overrideDecorator(
                object : WindowAreaControllerDecorator {
                    override fun decorate(controller: WindowAreaController): WindowAreaController {
                        return testController
                    }
                }
            )
            val controller2 = WindowAreaController.getOrCreate()

            assertFalse(controller1 === controller2, "Objects returned are the same object")
        }

    companion object {
        val testController =
            object : WindowAreaController() {
                override fun addWindowAreasListener(
                    executor: Executor,
                    listener: Consumer<List<WindowArea>>,
                ) {
                    listener.accept(listOf())
                }

                override fun removeWindowAreasListener(listener: Consumer<List<WindowArea>>) {
                    return
                }

                override fun transferToWindowArea(
                    windowAreaToken: WindowAreaToken?,
                    activity: Activity,
                ) {
                    throw IllegalStateException("There are no WindowAreas")
                }

                override fun presentContentOnWindowArea(
                    windowAreaToken: WindowAreaToken,
                    activity: Activity,
                    executor: Executor,
                    windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
                ) {
                    windowAreaPresentationSessionCallback.onSessionEnded(
                        IllegalStateException("There are no WindowAreas")
                    )
                }

                override fun getActivePresentationSession(
                    windowAreaToken: WindowAreaToken
                ): WindowAreaSessionPresenter {
                    throw IllegalArgumentException("There is no active session")
                }
            }
    }
}
