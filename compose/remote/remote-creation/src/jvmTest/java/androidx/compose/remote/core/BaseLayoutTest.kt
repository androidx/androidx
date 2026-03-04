/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.LayoutTestPlayer
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.layout.TestParameters
import androidx.compose.remote.creation.RemoteComposeContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.rules.TestName

open class BaseLayoutTest : LayoutTestPlayer() {
    var GENERATE_GOLD_FILES: Boolean = false
    var platform: RcPlatformServices = RcPlatformServices.None

    @Rule @JvmField var name = TestName()

    fun checkLayout(
        w: Int,
        h: Int,
        apiLevel: Int,
        profile: Int,
        description: String,
        ops: ArrayList<TestOperation>,
        testClock: RemoteClock = TestClock(1234),
    ) {
        checkLayout(w, h, apiLevel, profile, description, ops, testClock, false)
    }

    fun checkLayout(
        w: Int,
        h: Int,
        apiLevel: Int,
        profile: Int,
        description: String,
        ops: ArrayList<TestOperation>,
        testClock: RemoteClock = TestClock(1234),
        overridePlayerSize: Boolean = false,
    ) {
        if (ops.size == 0) {
            return
        }
        if (ops[0] !is TestLayout) {
            return
        }
        val function = (ops[0] as TestLayout).layout
        val testParameters = TestParameters(name.getMethodName(), GENERATE_GOLD_FILES, testClock)
        val writer =
            RemoteComposeContext(
                    w,
                    h,
                    description,
                    apiLevel,
                    profile,
                    platform,
                    { root { function.invoke(this) } },
                )
                .writer
        if (overridePlayerSize) {
            play(writer, ops as ArrayList<TestOperation>, testParameters, w, h, true)
        } else {
            play(writer, ops as ArrayList<TestOperation>, testParameters)
        }
    }

    data class TestLayout(var layout: RemoteComposeContext.() -> Unit) : TestOperation() {
        override fun apply(
            context: RemoteContext,
            document: CoreDocument,
            testParameters: TestParameters,
            commands: MutableList<Map<String, Any>>?,
        ): Boolean {
            // Nothing here
            return false
        }
    }

    class ValidateX(val expectedX: Float) : TestOperation() {
        override fun apply(
            context: RemoteContext,
            document: CoreDocument,
            testParameters: TestParameters,
            commands: MutableList<Map<String, Any>>?,
        ): Boolean {
            val root = document.rootLayoutComponent
            if (root!!.getX() != expectedX) {
                throw AssertionError("Expected X: $expectedX, actual X: ${root.getX()}")
            }
            return false
        }
    }

    class ValidateY(val expectedY: Float) : TestOperation() {
        override fun apply(
            context: RemoteContext,
            document: CoreDocument,
            testParameters: TestParameters,
            commands: MutableList<Map<String, Any>>?,
        ): Boolean {
            val root = document.rootLayoutComponent
            if (root!!.getY() != expectedY) {
                throw AssertionError("Expected Y: $expectedY, actual Y: ${root.getY()}")
            }
            return false
        }
    }

    class ValidatePosition(val expectedX: Float, val expectedY: Float) : TestOperation() {
        override fun apply(
            context: RemoteContext,
            document: CoreDocument,
            testParameters: TestParameters,
            commands: MutableList<Map<String, Any>>?,
        ): Boolean {
            val root = document.rootLayoutComponent
            if (root!!.getX() != expectedX || root.getY() != expectedY) {
                throw AssertionError(
                    "Expected ($expectedX, $expectedY), actual (${root.getX()}, ${root.getY()})"
                )
            }
            return false
        }
    }

    fun validateX(expectedX: Float): TestOperation = ValidateX(expectedX)

    fun validateY(expectedY: Float): TestOperation = ValidateY(expectedY)

    fun validatePosition(expectedX: Float, expectedY: Float): TestOperation =
        ValidatePosition(expectedX, expectedY)

    class ValidateNoAnimation() : TestOperation() {
        override fun apply(
            context: RemoteContext,
            document: CoreDocument,
            testParameters: TestParameters,
            commands: MutableList<Map<String, Any>>?,
        ): Boolean {
            val root = document.rootLayoutComponent
            if (root!!.mAnimateMeasure != null) {
                throw AssertionError("Expected no animation, but mAnimateMeasure is not null")
            }
            return false
        }
    }

    fun validateNoAnimation(): TestOperation = ValidateNoAnimation()

    internal fun TestClock(time: Int): RemoteClock {
        return SystemClock(Clock.fixed(Instant.ofEpochMilli(time.toLong()), ZoneId.of("UTC")))
    }
}
