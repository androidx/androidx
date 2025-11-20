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

package androidx.camera.testing.impl

import android.os.Build
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

public class IgnoreVideoRecordingProblematicDeviceRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        skipVideoRecordingTestIfNotSupportedByEmulator()
        return base
    }

    public companion object {

        /**
         * Skips the test if the current device is emulator that doesn't support video recording.
         */
        public fun skipVideoRecordingTestIfNotSupportedByEmulator() {
            // Skip test for b/168175357, b/233661493
            Assume.assumeFalse(
                "Skip tests for Cuttlefish MediaCodec issues",
                Build.MODEL.contains("Cuttlefish") &&
                    (Build.VERSION.SDK_INT == 29 || Build.VERSION.SDK_INT == 33),
            )
            // Skip test for b/441563673
            Assume.assumeFalse(
                "Emulator API 23 codec native crash.",
                Build.VERSION.SDK_INT == 23 && AndroidUtil.isEmulator(),
            )
            // Skip test for b/399669628, b/401097968
            Assume.assumeFalse(
                "Emulator API 26 MediaCodec doesn't send encoded data.",
                Build.VERSION.SDK_INT == 26 && AndroidUtil.isEmulator(),
            )
            // Skip test for b/331618729
            Assume.assumeFalse(
                "Emulator API 28 crashes running this test.",
                Build.VERSION.SDK_INT == 28 && AndroidUtil.isEmulator(),
            )
            // Skip test for b/264902324, b/331618729
            Assume.assumeFalse(
                "Emulator API 30 crashes running this test.",
                Build.VERSION.SDK_INT == 30 && AndroidUtil.isEmulator(),
            )
        }
    }
}
