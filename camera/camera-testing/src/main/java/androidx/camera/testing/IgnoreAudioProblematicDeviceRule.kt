/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing

import androidx.annotation.RequiresApi
import androidx.camera.testing.IgnoreProblematicDeviceRule.Companion.isPixel2Api26Emulator
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Test class to set the TestRule that should not be run on the audio problematically devices.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class IgnoreAudioProblematicDeviceRule : TestRule {
    private val isProblematicDevices = isPixel2Api26Emulator

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (isProblematicDevices) {
                    throw AssumptionViolatedException(
                        "AudioRecord of the emulator may not be well prepared for related" +
                            " tests. Ignore the test: " + description.displayName +
                            ". To test on emulator devices, please remove the" +
                            " IgnoreAudioProblematicallyDeviceRule from the test class."
                    )
                } else {
                    base.evaluate()
                }
            }
        }
    }
}