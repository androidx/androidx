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

package androidx.xr.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import androidx.xr.runtime.math.Pose;

import kotlin.Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HeadMovementHelperJavaTest {

    @Test
    public void constants_accessibleFromJava() {
        assertEquals(50L, HeadMovementHelper.DEFAULT_HEAD_SETTLE_DELAY_MS);
        assertEquals(500L, HeadMovementHelper.DEFAULT_HEAD_MOVEMENT_DURATION_MS);
    }

    @Test
    public void fakeHeadTrackingRule_constructibleFromJava() {
        assertNotNull(new FakeHeadTrackingRule());
        assertNotNull(new FakeHeadTrackingRule(10L));
    }

    @Test
    public void fakeHeadTrackingRule_headMovementOverloadsCallableFromJava() {
        FakeHeadTrackingRule rule = new FakeHeadTrackingRule();

        // Outside a guarded test, every overload fails fast before running any shell command.
        assertThrows(
                IllegalStateException.class,
                () ->
                        rule.performHeadMovement(
                                scope -> {
                                    scope.poseHeadTo(Pose.Identity);
                                    return Unit.INSTANCE;
                                }));
        assertThrows(
                IllegalStateException.class,
                () -> rule.performHeadMovement(true, scope -> Unit.INSTANCE));
        assertThrows(IllegalStateException.class, rule::resetHeadMovement);
    }
}
