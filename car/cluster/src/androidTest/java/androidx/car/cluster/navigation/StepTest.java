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
import static androidx.car.cluster.navigation.utils.Assertions.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Tests for {@link Step} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class StepTest {
    /**
     * Test a few equality conditions
     */
    @Test
    public void equality() {
        Step expected = createSampleStep();

        assertEquals(expected, createSampleStep());
        assertNotEquals(expected, new Step.Builder()
                .setDistance(new Distance(10, "10", Distance.Unit.METERS))
                .setManeuver(new Maneuver.Builder().setType(Maneuver.Type.DEPART).build())
                .addLane(LaneTest.createSampleLane())
                .setLanesImage(ImageReferenceTest.createSampleImage())
                .build());
        assertNotEquals(expected, new Step.Builder()
                .setCue(RichTextTest.createSampleRichText())
                .setManeuver(new Maneuver.Builder().setType(Maneuver.Type.DEPART).build())
                .addLane(LaneTest.createSampleLane())
                .setLanesImage(ImageReferenceTest.createSampleImage())
                .build());
        assertNotEquals(expected, new Step.Builder()
                .setCue(RichTextTest.createSampleRichText())
                .setDistance(new Distance(10, "10", Distance.Unit.METERS))
                .addLane(LaneTest.createSampleLane())
                .setLanesImage(ImageReferenceTest.createSampleImage())
                .build());
        assertNotEquals(expected, new Step.Builder()
                .setCue(RichTextTest.createSampleRichText())
                .setDistance(new Distance(10, "10", Distance.Unit.METERS))
                .setManeuver(new Maneuver.Builder().setType(Maneuver.Type.DEPART).build())
                .build());
        assertNotEquals(expected, new Step.Builder()
                .setCue(RichTextTest.createSampleRichText())
                .setDistance(new Distance(10, "10", Distance.Unit.METERS))
                .setManeuver(new Maneuver.Builder().setType(Maneuver.Type.DEPART).build())
                .addLane(LaneTest.createSampleLane())
                .addLane(LaneTest.createSampleLane())
                .setLanesImage(ImageReferenceTest.createSampleImage())
                .build());

        assertEquals(expected.hashCode(), createSampleStep().hashCode());
    }

    /**
     * Lists returned by {@link Step} are immutable.
     */
    @Test
    public void immutability_lanesListIsNeverNull() {
        assertImmutable(new Step.Builder().build().getLanes());
        assertImmutable(new Step().getLanes());
    }

    /**
     * Builder doesn't accept null lanes
     */
    @Test(expected = NullPointerException.class)
    public void builder_lanesCantBeNull() {
        new Step.Builder().addLane(null);
    }

    /**
     * Lane configuration metadata and image must be both provided, or not provided at all.
     */
    @Test
    public void builder_lanesMetadataAndImageMustBeBothProvided() {
        assertThrows(IllegalArgumentException.class, () ->
                new Step.Builder().addLane(LaneTest.createSampleLane()).build());
        assertThrows(IllegalArgumentException.class, () ->
                new Step.Builder().setLanesImage(ImageReferenceTest.createSampleImage()).build());
    }

    /**
     * Tests that even if we receive a null list of {@link Lane}s, we return an empty list to the
     * consumers.
     */
    @Test
    public void nullability_lanesIsNeverNull() {
        assertEquals(new ArrayList<>(), new Step().getLanes());
    }

    /**
     * Returns a sample {@link Step} for testing
     */
    public static Step createSampleStep() {
        return new Step.Builder()
                .setCue(RichTextTest.createSampleRichText())
                .setDistance(new Distance(10, "10", Distance.Unit.METERS))
                .setManeuver(new Maneuver.Builder().setType(Maneuver.Type.DEPART).build())
                .addLane(LaneTest.createSampleLane())
                .setLanesImage(ImageReferenceTest.createSampleImage())
                .build();
    }
}
