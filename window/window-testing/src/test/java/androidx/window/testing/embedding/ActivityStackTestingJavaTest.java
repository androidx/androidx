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

package androidx.window.testing.embedding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.app.Activity;

import androidx.annotation.OptIn;
import androidx.window.core.ExperimentalWindowApi;
import androidx.window.embedding.ActivityStack;

import org.junit.Test;

import java.util.Collections;

/** Test class to verify {@link TestActivityStack} */
@OptIn(markerClass = ExperimentalWindowApi.class)
public class ActivityStackTestingJavaTest {

    /** Verifies the default value of {@link TestActivityStack} */
    @Test
    public void testActivityStackDefaultValue() {
        final ActivityStack activityStack = TestActivityStack.createTestActivityStack();

        assertEquals(new ActivityStack(Collections.emptyList(), false /* isEmpty */,
                TestActivityStack.TEST_ACTIVITY_STACK_TOKEN), activityStack);
    }

    /** Verifies {@link TestActivityStack} */
    @Test
    public void testActivityStackWithNonEmptyActivityList() {
        final Activity activity = mock(Activity.class);
        final ActivityStack activityStack = TestActivityStack.createTestActivityStack(
                Collections.singletonList(activity), false /* isEmpty */);

        assertTrue(activityStack.contains(activity));
        assertFalse(activityStack.isEmpty());
    }
}
