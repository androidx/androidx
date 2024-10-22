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

package androidx.wear.protolayout;

import static androidx.wear.protolayout.ResourceBuilders.ANIMATED_IMAGE_FORMAT_AVD;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.ResourceProto;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResourceBuildersTest {
    private static final int RESOURCE_ID = 10;
    private static final int FORMAT = ANIMATED_IMAGE_FORMAT_AVD;

    @Test
    public void avd() {
        ResourceBuilders.AndroidAnimatedImageResourceByResId avd =
                new ResourceBuilders.AndroidAnimatedImageResourceByResId.Builder()
                        .setResourceId(RESOURCE_ID)
                        .setAnimatedImageFormat(FORMAT)
                        .setStartTrigger(TriggerBuilders.createOnLoadTrigger())
                        .build();

        ResourceProto.AndroidAnimatedImageResourceByResId avdProto = avd.toProto();

        assertThat(avdProto.getResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(avdProto.getAnimatedImageFormat().getNumber()).isEqualTo(FORMAT);
        assertThat(avdProto.getStartTrigger().hasOnLoadTrigger()).isTrue();
    }

    @Test
    public void seekableAvd() {
        String stateKey = "state-key";
        ResourceBuilders.AndroidSeekableAnimatedImageResourceByResId avd =
                new ResourceBuilders.AndroidSeekableAnimatedImageResourceByResId.Builder()
                        .setResourceId(RESOURCE_ID)
                        .setAnimatedImageFormat(FORMAT)
                        .setProgress(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(stateKey)))
                        .build();

        ResourceProto.AndroidSeekableAnimatedImageResourceByResId avdProto = avd.toProto();

        assertThat(avdProto.getResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(avdProto.getAnimatedImageFormat().getNumber()).isEqualTo(FORMAT);
        assertThat(avdProto.getProgress().getStateSource().getSourceKey()).isEqualTo(stateKey);
    }

    @Test
    public void lottieAnimation() {
        String stateKey = "state-key";
        ResourceBuilders.AndroidLottieResourceByResId lottieResource =
                new ResourceBuilders.AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                        .setProgress(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(stateKey)))
                        .build();

        ResourceProto.AndroidLottieResourceByResId avdProto = lottieResource.toProto();

        assertThat(avdProto.getRawResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(avdProto.getProgress().getStateSource().getSourceKey()).isEqualTo(stateKey);
    }
}
