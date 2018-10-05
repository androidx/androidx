/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.cluster.navigation;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link Lane} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class LaneTest {
    /**
     * Tests that lists returned by {@link Lane} are immutable.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void immutableLists() {
        Lane lane = new Lane.Builder().build();
        lane.getDirections().add(new LaneDirection.Builder().build());
    }
}
