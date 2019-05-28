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

import android.os.Build;

import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CaptureBundleTest {
    @Test
    public void bundleRetainsOrder() {
        List<Integer> captureOrder = new ArrayList<>();
        captureOrder.add(0);
        captureOrder.add(2);
        captureOrder.add(1);

        CaptureBundle captureBundle = new CaptureBundle();
        for (Integer captureId : captureOrder) {
            captureBundle.addCaptureStage(new FakeCaptureStage(captureId, null));
        }

        List<CaptureStage> captureStages = captureBundle.getCaptureStages();

        assertThat(captureOrder.size()).isEqualTo(captureStages.size());

        Iterator<Integer> captureOrderIterator = captureOrder.iterator();
        Iterator<CaptureStage> captureStageIterator = captureStages.iterator();

        while (captureOrderIterator.hasNext() && captureStageIterator.hasNext()) {
            assertThat(captureOrderIterator.next()).isEqualTo(captureStageIterator.next().getId());
        }
    }
}
