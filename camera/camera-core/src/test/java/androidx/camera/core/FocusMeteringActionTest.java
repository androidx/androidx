/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class FocusMeteringActionTest {
    private SurfaceOrientedMeteringPointFactory mPointFactory =
            new SurfaceOrientedMeteringPointFactory(1.0f, 1.0f);

    MeteringPoint mPoint1 = mPointFactory.createPoint(0, 0);
    MeteringPoint mPoint2 = mPointFactory.createPoint(1, 1);
    MeteringPoint mPoint3 = mPointFactory.createPoint(1, 0);

    @Test
    public void defaultBuilder_valueIsDefault() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1).build();

        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1);
        assertThat(action.getAutoCancelDurationInMillis()).isEqualTo(
                FocusMeteringAction.DEFAULT_AUTOCANCEL_DURATION);
        assertThat(action.isAutoCancelEnabled()).isTrue();
    }

    @Test
    public void fromPointWithAFAEAWB() {
        FocusMeteringAction action =
                new FocusMeteringAction.Builder(mPoint1,
                        FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                                | FocusMeteringAction.FLAG_AWB).build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1);
    }

    @Test
    public void fromPointWithAFAE() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE).build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAwb()).isEmpty();
    }

    @Test
    public void fromPointWithAFAWB() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AWB).build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAe()).isEmpty();
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1);
    }

    @Test
    public void fromPointWithAEAWB() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB).build();
        assertThat(action.getMeteringPointsAf()).isEmpty();
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1);
    }

    @Test
    public void fromPointWithAF() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF).build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAe()).isEmpty();
        assertThat(action.getMeteringPointsAwb()).isEmpty();
    }

    @Test
    public void fromPointWithAE() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AE).build();
        assertThat(action.getMeteringPointsAf()).isEmpty();
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAwb()).isEmpty();
    }

    @Test
    public void fromPointWithAWB() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AWB).build();
        assertThat(action.getMeteringPointsAf()).isEmpty();
        assertThat(action.getMeteringPointsAe()).isEmpty();
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1);
    }

    @Test
    public void multiplePointsWithDefaultMeteringMode() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .addPoint(mPoint2)
                .addPoint(mPoint3)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1, mPoint2, mPoint3);
    }

    @Test
    public void multiplePointsWithSameAF_AE_AWB() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                        | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint2,
                        FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                                | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint3,
                        FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                                | FocusMeteringAction.FLAG_AWB)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1, mPoint2, mPoint3);
    }

    @Test
    public void multiplePointsWithSameAF_AE() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAwb()).isEmpty();
    }

    @Test
    public void multiplePointsWithSameAE_AWB() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                .build();
        assertThat(action.getMeteringPointsAf()).isEmpty();
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1, mPoint2, mPoint3);
    }

    @Test
    public void multiplePointsWithSameAF_AWB() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AWB)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAe()).isEmpty();
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1, mPoint2, mPoint3);
    }

    @Test
    public void multiplePointsWithSameAWBOnly() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AWB)
                .build();
        assertThat(action.getMeteringPointsAf()).isEmpty();
        assertThat(action.getMeteringPointsAe()).isEmpty();
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1, mPoint2, mPoint3);
    }

    @Test
    public void multiplePointsWithSameAEOnly() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AE)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AE)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AE)
                .build();
        assertThat(action.getMeteringPointsAf()).isEmpty();
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAwb()).isEmpty();
    }

    @Test
    public void multiplePointsWithSameAFOnly() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AF)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AF)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAe()).isEmpty();
        assertThat(action.getMeteringPointsAwb()).isEmpty();
    }

    @Test
    public void multiplePointsWithAFOnly_AEOnly_AWBOnly() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AE)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AWB)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint2);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint3);
    }

    @Test
    public void multiplePointsWithAFAE_AEAWB_AFAWB() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AWB)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1, mPoint3);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint2, mPoint1);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint3, mPoint2);
    }

    @Test
    public void multiplePointsWithAFAEAWB_AEAWB_AFOnly() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                        | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AF)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint1, mPoint3);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1, mPoint2);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint1, mPoint2);
    }

    @Test
    public void multiplePointsWithAEOnly_AFAWAEB_AEOnly() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1,
                FocusMeteringAction.FLAG_AE)
                .addPoint(mPoint2,
                        FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                                | FocusMeteringAction.FLAG_AWB)
                .addPoint(mPoint3, FocusMeteringAction.FLAG_AE)
                .build();
        assertThat(action.getMeteringPointsAf()).containsExactly(mPoint2);
        assertThat(action.getMeteringPointsAe()).containsExactly(mPoint1, mPoint2, mPoint3);
        assertThat(action.getMeteringPointsAwb()).containsExactly(mPoint2);
    }

    @Test
    public void setAutoCancelDurationBySeconds() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build();
        assertThat(action.getAutoCancelDurationInMillis()).isEqualTo(3000);
    }

    @Test
    public void setAutoCancelDurationByMinutes() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(2, TimeUnit.MINUTES)
                .build();
        assertThat(action.getAutoCancelDurationInMillis()).isEqualTo(120000);
    }

    @Test
    public void setAutoCancelDurationByMilliseconds() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(1500, TimeUnit.MILLISECONDS)
                .build();
        assertThat(action.getAutoCancelDurationInMillis()).isEqualTo(1500);
    }

    @Test
    public void setAutoCancelDurationLargerThan0_shouldEnableAutoCancel() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(1, TimeUnit.MILLISECONDS)
                .build();

        assertThat(action.isAutoCancelEnabled()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAutoCancelDuration0_shouldThrowException() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(0, TimeUnit.MILLISECONDS)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAutoCancelDurationSmallerThan0_shouldThrowException() {
        FocusMeteringAction action = new FocusMeteringAction.Builder(mPoint1)
                .setAutoCancelDuration(-1, TimeUnit.MILLISECONDS)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderWithNullPoint2() {
        new FocusMeteringAction.Builder(null, FocusMeteringAction.FLAG_AF).build();
    }

    @Test
    public void copyBuilder() {
        // 1. Arrange
        FocusMeteringAction action1 = new FocusMeteringAction.Builder(mPoint1)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(8000, TimeUnit.MILLISECONDS)
                .build();

        // 2. Act
        FocusMeteringAction action2 = new FocusMeteringAction.Builder(action1).build();

        // 3. Assert
        assertThat(action1.getMeteringPointsAf()).containsExactlyElementsIn(
                action2.getMeteringPointsAf()
        );
        assertThat(action1.getMeteringPointsAe()).containsExactlyElementsIn(
                action2.getMeteringPointsAe()
        );
        assertThat(action1.getMeteringPointsAwb()).containsExactlyElementsIn(
                action2.getMeteringPointsAwb()
        );
        assertThat(action1.getAutoCancelDurationInMillis())
                .isEqualTo(action2.getAutoCancelDurationInMillis());
        assertThat(action1.isAutoCancelEnabled()).isEqualTo(action2.isAutoCancelEnabled());
    }

    @Test
    public void removePoints() {
        // 1. Arrange
        FocusMeteringAction.Builder builder = new FocusMeteringAction.Builder(mPoint1)
                .addPoint(mPoint2, FocusMeteringAction.FLAG_AE);

        // 2. Act
        FocusMeteringAction action = builder.removePoints(FocusMeteringAction.FLAG_AE).build();

        // 3. Assert
        assertThat(action.getMeteringPointsAe()).isEmpty();
        assertThat(action.getMeteringPointsAf()).containsExactlyElementsIn(
                Arrays.asList(mPoint1)
        );
        assertThat(action.getMeteringPointsAwb()).containsExactlyElementsIn(
                Arrays.asList(mPoint1)
        );
    }

}
