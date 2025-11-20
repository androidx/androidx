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

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.ResourceBuilders.ANIMATED_IMAGE_FORMAT_AVD;
import static androidx.wear.protolayout.ResourceBuilders.AndroidLottieResourceByResId.Builder.LOTTIE_PROPERTIES_LIMIT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ResourceBuilders.AndroidAnimatedImageResourceByResId;
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId;
import androidx.wear.protolayout.ResourceBuilders.AndroidLottieResourceByResId;
import androidx.wear.protolayout.ResourceBuilders.AndroidSeekableAnimatedImageResourceByResId;
import androidx.wear.protolayout.ResourceBuilders.ImageResource;
import androidx.wear.protolayout.ResourceBuilders.InlineImageResource;
import androidx.wear.protolayout.ResourceBuilders.LottieProperty;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.ResourceProto;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResourceBuildersTest {
    private static final int RESOURCE_ID = 10;
    private static final int FORMAT = ANIMATED_IMAGE_FORMAT_AVD;
    private static final TriggerBuilders.@NonNull Trigger ON_CONDITION_MET_TRIGGER =
            TriggerBuilders.createOnConditionMetTrigger(DynamicBuilders.DynamicBool.constant(true));
    private static final DynamicBuilders.@NonNull DynamicFloat PROGRESS =
            DynamicBuilders.DynamicFloat.constant(1).div(2).animate();

    @Test
    public void avd() {
        AndroidAnimatedImageResourceByResId avd =
                new AndroidAnimatedImageResourceByResId.Builder()
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
        AndroidSeekableAnimatedImageResourceByResId avd =
                new AndroidSeekableAnimatedImageResourceByResId.Builder()
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
        AndroidLottieResourceByResId lottieResource =
                new AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                        .setProgress(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(stateKey)))
                        .build();

        ResourceProto.AndroidLottieResourceByResId lottieProto = lottieResource.toProto();

        assertThat(lottieProto.getRawResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(lottieProto.getProgress().getStateSource().getSourceKey()).isEqualTo(stateKey);
    }

    @Test
    public void lottieAnimation_hasTrigger() {
        AndroidLottieResourceByResId lottieResource =
                new AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                        .setStartTrigger(TriggerBuilders.createOnVisibleTrigger())
                        .build();

        ResourceProto.AndroidLottieResourceByResId lottieProto = lottieResource.toProto();

        assertThat(lottieProto.getRawResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(lottieProto.hasStartTrigger()).isTrue();
        assertThat(lottieProto.getStartTrigger().hasOnVisibleTrigger()).isTrue();
        assertThat(lottieProto.getStartTrigger().hasOnVisibleOnceTrigger()).isFalse();
        assertThat(lottieProto.getStartTrigger().hasOnLoadTrigger()).isFalse();
    }

    @Test
    public void lottieAnimation_hasProperty() {
        AndroidLottieResourceByResId lottieResource =
                new AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                        .setProperties(LottieProperty.colorForSlot("sid", argb(Color.YELLOW)))
                        .build();

        ResourceProto.AndroidLottieResourceByResId lottieProto = lottieResource.toProto();

        assertThat(lottieProto.getRawResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(lottieProto.getPropertiesList().size()).isEqualTo(1);
        assertThat(lottieProto.getPropertiesList().get(0).getSlotColor().getColor().getArgb())
                .isEqualTo(Color.YELLOW);
        assertThat(lottieProto.getPropertiesList().get(0).getSlotColor().getSid()).isEqualTo("sid");
    }

    @Test
    public void lottieAnimation_exceedsPropertiesLimit_throws() {
        LottieProperty[] properties = new LottieProperty[LOTTIE_PROPERTIES_LIMIT + 1];

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                                .setProperties(properties)
                                .build());
    }

    @Test
    public void imageResource_withSameData_hashCodeIsTheSame() {
        ImageResource image =
                new ImageResource.Builder()
                        .setAndroidLottieResourceByResId(
                                new AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                                        .setStartTrigger(ON_CONDITION_MET_TRIGGER)
                                        .build())
                        .setInlineResource(
                                new InlineImageResource.Builder()
                                        .setData(new byte[] {1, 2, 3, 4})
                                        .build())
                        .build();
        ImageResource sameImage =
                new ImageResource.Builder()
                        .setInlineResource(
                                new InlineImageResource.Builder()
                                        .setData(new byte[] {1, 2, 3, 4})
                                        .build())
                        .setAndroidLottieResourceByResId(
                                new AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                                        .setStartTrigger(ON_CONDITION_MET_TRIGGER)
                                        .build())
                        .build();

        assertThat(image).isEqualTo(sameImage);
        assertThat(image.hashCode()).isEqualTo(sameImage.hashCode());
    }

    @Test
    public void imageResource_withDifferentData_hashCodeDifferent() {
        ImageResource image1 =
                new ImageResource.Builder()
                        .setAndroidLottieResourceByResId(
                                new AndroidLottieResourceByResId.Builder(RESOURCE_ID)
                                        .setStartTrigger(TriggerBuilders.createOnVisibleTrigger())
                                        .build())
                        .setInlineResource(
                                new InlineImageResource.Builder()
                                        .setData(
                                                new byte[] {
                                                    1, 2, 3,
                                                })
                                        .build())
                        .build();
        ImageResource image2 =
                new ImageResource.Builder()
                        .setInlineResource(
                                new InlineImageResource.Builder()
                                        .setData(new byte[] {1, 2, 3, 4})
                                        .build())
                        .setAndroidAnimatedResourceByResId(
                                new AndroidAnimatedImageResourceByResId.Builder()
                                        .setResourceId(RESOURCE_ID)
                                        .setStartTrigger(TriggerBuilders.createOnVisibleTrigger())
                                        .build())
                        .build();

        assertThat(image1).isNotEqualTo(image2);
        assertThat(image1.hashCode()).isNotEqualTo(image2.hashCode());
    }

    @Test
    public void androidImage_withSameData_hashCodeIsSame() {
        int id = 1234;
        AndroidImageResourceByResId image1 =
                new AndroidImageResourceByResId.Builder().setResourceId(id).build();
        AndroidImageResourceByResId image2 =
                new AndroidImageResourceByResId.Builder().setResourceId(id).build();

        assertThat(image1).isEqualTo(image2);
        assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
    }

    @Test
    public void avd_withSameData_hashCodeIsSame() {
        int id = 1234;
        AndroidAnimatedImageResourceByResId image1 =
                new AndroidAnimatedImageResourceByResId.Builder()
                        .setResourceId(id)
                        .setAnimatedImageFormat(ANIMATED_IMAGE_FORMAT_AVD)
                        .setStartTrigger(TriggerBuilders.createOnVisibleTrigger())
                        .build();
        AndroidAnimatedImageResourceByResId image2 =
                new AndroidAnimatedImageResourceByResId.Builder()
                        .setResourceId(id)
                        .setAnimatedImageFormat(ANIMATED_IMAGE_FORMAT_AVD)
                        .setStartTrigger(TriggerBuilders.createOnVisibleTrigger())
                        .build();

        assertThat(image1).isEqualTo(image2);
        assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
    }

    @Test
    public void seekableAvd_withSameData_hashCodeIsSame() {
        int id = 1234;
        AndroidSeekableAnimatedImageResourceByResId image1 =
                new AndroidSeekableAnimatedImageResourceByResId.Builder()
                        .setResourceId(id)
                        .setAnimatedImageFormat(ANIMATED_IMAGE_FORMAT_AVD)
                        .setProgress(PROGRESS)
                        .build();
        AndroidSeekableAnimatedImageResourceByResId image2 =
                new AndroidSeekableAnimatedImageResourceByResId.Builder()
                        .setResourceId(id)
                        .setAnimatedImageFormat(ANIMATED_IMAGE_FORMAT_AVD)
                        .setProgress(PROGRESS)
                        .build();

        assertThat(image1).isEqualTo(image2);
        assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
    }

    @Test
    public void lottie_withSameData_hashCodeIsSame() {
        int id = 1234;
        AndroidLottieResourceByResId image1 =
                new AndroidLottieResourceByResId.Builder(id)
                        .setStartTrigger(ON_CONDITION_MET_TRIGGER)
                        .setProgress(PROGRESS)
                        .build();
        AndroidLottieResourceByResId image2 =
                new AndroidLottieResourceByResId.Builder(id)
                        .setStartTrigger(ON_CONDITION_MET_TRIGGER)
                        .setProgress(PROGRESS)
                        .build();

        assertThat(image1).isEqualTo(image2);
        assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
    }

    @Test
    public void inlineImage_withSameData_hashCodeIsSame() {
        InlineImageResource image1 =
                new InlineImageResource.Builder()
                        .setFormat(1)
                        .setWidthPx(10)
                        .setHeightPx(10)
                        .setData(new byte[] {1, 2, 3, 4})
                        .build();
        InlineImageResource image2 =
                new InlineImageResource.Builder()
                        .setFormat(1)
                        .setWidthPx(10)
                        .setHeightPx(10)
                        .setData(new byte[] {1, 2, 3, 4})
                        .build();

        assertThat(image1).isEqualTo(image2);
        assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
    }

    @Test
    public void lottieProperties_limitReturned() {
        assertThat(AndroidLottieResourceByResId.getMaxPropertiesCount())
                .isEqualTo(LOTTIE_PROPERTIES_LIMIT);
    }
}
