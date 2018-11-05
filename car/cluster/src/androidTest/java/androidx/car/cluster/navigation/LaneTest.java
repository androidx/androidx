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

import static androidx.car.cluster.navigation.utils.Assertions.assertImmutable;

import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Tests for {@link Lane} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class LaneTest {
    /**
     * Tests that lists returned by {@link Lane} are immutable.
     */
    @Test
    public void immutability() {
        assertImmutable(new Lane.Builder().build().getDirections());
        assertImmutable(new Lane().getDirections());
    }

    /**
     * Tests that even if we receive a null list of {@link LaneDirection}s, we return an empty list
     * to the consumers.
     */
    @Test
    public void nullability_directionsListIsNeverNull() {
        assertEquals(new ArrayList<>(), new Lane().getDirections());
    }

    /**
     * Returns a sample {@link Lane} for testing.
     */
    public static Lane createSampleLane() {
        return new Lane.Builder()
                .addDirection(new LaneDirection.Builder()
                        .setShape(LaneDirection.Shape.NORMAL_LEFT)
                        .setHighlighted(true)
                        .build())
                .addDirection(new LaneDirection.Builder()
                        .setShape(LaneDirection.Shape.STRAIGHT)
                        .setHighlighted(true)
                        .build())
                .build();
    }
}
