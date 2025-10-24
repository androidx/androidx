/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.compose.remote.core.operations.paint.PaintPathEffects;

import org.junit.Test;

public class PaintPathEffectsEncodeTest {
    @Test
    public void dashTest1() {
        PaintPathEffects pe = new PaintPathEffects.Dash(0.0f, 1.0f, 2.0f);
        float[] data = PaintPathEffects.encode(pe);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.Dash);
        PaintPathEffects.Dash pe3 = (PaintPathEffects.Dash) pe2;
        assert (pe3.mPhase == 0.0f);
        assert (pe3.mIntervals[0] == 1.0f);
    }

    @Test
    public void pathDashTest1() {
        PaintPathEffects pe = new PaintPathEffects.PathDash(42, 1.0f, 2.0f, 3);
        float[] data = PaintPathEffects.encode(pe);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.PathDash);
        PaintPathEffects.PathDash pe3 = (PaintPathEffects.PathDash) pe2;
        assert (pe3.mPhase == 2.0f);
        assert (pe3.mAdvance == 1.0);
        assert (pe3.mShapeId == 42);
        assert (pe3.mStyle == 3);

    }

    @Test
    public void discreteTest1() {
        PaintPathEffects pe = new PaintPathEffects.Discrete(32.1f, 3.3f);
        float[] data = PaintPathEffects.encode(pe);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.Discrete);
        PaintPathEffects.Discrete pe3 = (PaintPathEffects.Discrete) pe2;
        assert (pe3.mSegmentLength == 32.1f);
        assert (pe3.mDeviation == 3.3f);
    }

    @Test
    public void composeTest1() {
        PaintPathEffects pe = new PaintPathEffects.Compose(
                new PaintPathEffects.Discrete(32.1f, 3.3f),
                new PaintPathEffects.PathDash(42, 1.0f, 2.0f, 3));
        float[] data = PaintPathEffects.encode(pe);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.Compose);
        PaintPathEffects.Compose pe3 = (PaintPathEffects.Compose) pe2;

    }

    @Test
    public void sumTest1() {
        PaintPathEffects pe = new PaintPathEffects.Sum(
                new PaintPathEffects.Discrete(32.1f, 3.3f),
                new PaintPathEffects.PathDash(42, 1.0f, 2.0f, 3));
        float[] data = PaintPathEffects.encode(pe);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);

        assert (pe2 instanceof PaintPathEffects.Sum);
        PaintPathEffects.Sum pe3 = (PaintPathEffects.Sum) pe2;

    }

    @Test
    public void dashTest2() {

        float[] data = PaintPathEffects.dash(0.0f, 1.0f, 2.0f);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.Dash);
        PaintPathEffects.Dash pe3 = (PaintPathEffects.Dash) pe2;
        assert (pe3.mPhase == 0.0f);
        assert (pe3.mIntervals[0] == 1.0f);
    }

    @Test
    public void pathDashTest2() {
        float[] data = PaintPathEffects.pathDash(42, 1.0f, 2.0f, 3);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.PathDash);
        PaintPathEffects.PathDash pe3 = (PaintPathEffects.PathDash) pe2;
        assert (pe3.mPhase == 2.0f);
        assert (pe3.mAdvance == 1.0);
        assert (pe3.mShapeId == 42);
        assert (pe3.mStyle == 3);

    }

    @Test
    public void discreteTest2() {
        float[] data = PaintPathEffects.discrete(32.1f, 3.3f);
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.Discrete);
        PaintPathEffects.Discrete pe3 = (PaintPathEffects.Discrete) pe2;
        assert (pe3.mSegmentLength == 32.1f);
        assert (pe3.mDeviation == 3.3f);
    }

    @Test
    public void composeTest2() {
        PaintPathEffects pe = new PaintPathEffects.Compose(
                new PaintPathEffects.Discrete(32.1f, 3.3f),
                new PaintPathEffects.PathDash(42, 1.0f, 2.0f, 3));
        float[] data = PaintPathEffects.compose(
                PaintPathEffects.discrete(32.1f, 3.3f),
                PaintPathEffects.pathDash(42, 1.0f, 2.0f, 3)
        );
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);
        assert (pe2 instanceof PaintPathEffects.Compose);
        PaintPathEffects.Compose pe3 = (PaintPathEffects.Compose) pe2;

    }

    @Test
    public void sumTest2() {

        float[] data = PaintPathEffects.sum(
                PaintPathEffects.discrete(32.1f, 3.3f),
                PaintPathEffects.pathDash(42, 1.0f, 2.0f, 3)
        );
        PaintPathEffects pe2 = PaintPathEffects.parse(data, 0);

        assert (pe2 instanceof PaintPathEffects.Sum);
        PaintPathEffects.Sum pe3 = (PaintPathEffects.Sum) pe2;

    }


}
