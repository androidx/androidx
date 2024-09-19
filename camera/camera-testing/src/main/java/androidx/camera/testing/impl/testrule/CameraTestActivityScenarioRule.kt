/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.testing.impl.testrule

import android.app.Activity
import android.content.Intent
import androidx.camera.testing.impl.InternalTestConvenience.useInCameraTest
import androidx.test.core.app.ActivityScenario
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to use [ActivityScenario] in a safer way for internal camera tests.
 *
 * See [useInCameraTest] for details.
 */
public class CameraTestActivityScenarioRule<A : Activity>
private constructor(private val activityScenarioLazy: Lazy<ActivityScenario<A>>) : TestRule {
    public constructor(
        activityClass: Class<A>
    ) : this(lazy { ActivityScenario.launch(activityClass) })

    public constructor(intent: Intent) : this(lazy { ActivityScenario.launch(intent) })

    public val scenario: ActivityScenario<A>
        get() = activityScenarioLazy.value

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                scenario.useInCameraTest { base.evaluate() }
            }
        }
}
