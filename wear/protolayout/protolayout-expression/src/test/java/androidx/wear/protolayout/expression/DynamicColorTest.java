/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import static androidx.wear.protolayout.expression.AnimationParameterBuilders.REPEAT_MODE_REVERSE;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import androidx.annotation.ColorInt;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationParameters;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.Repeatable;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicColorTest {
  private static final String STATE_KEY = "state-key";
  @ColorInt private static final int CONSTANT_VALUE = 0xff00ff00;
  private static final AnimationSpec SPEC =
      new AnimationSpec.Builder()
          .setAnimationParameters(
                  new AnimationParameters.Builder()
                          .setDurationMillis(2)
                          .setDelayMillis(1)
                          .build())
          .setRepeatable(
              new Repeatable.Builder().setRepeatMode(REPEAT_MODE_REVERSE).setIterations(10).build())
          .build();

  @Test
  public void constantColor() {
    DynamicColor constantColor = DynamicColor.constant(CONSTANT_VALUE);

    assertThat(constantColor.toDynamicColorProto().getFixed().getArgb()).isEqualTo(CONSTANT_VALUE);
  }

  @Test
  public void constantToString() {
    assertThat(DynamicColor.constant(0x00000001).toString()).isEqualTo("FixedColor{argb=1}");
  }

  @Test
  public void stateEntryValueColor() {
    DynamicColor stateColor = DynamicColor.fromState(STATE_KEY);

    assertThat(stateColor.toDynamicColorProto().getStateSource().getSourceKey())
        .isEqualTo(STATE_KEY);
  }

  @Test
  public void stateToString() {
    assertThat(DynamicColor.fromState("key").toString())
        .isEqualTo("StateColorSource{sourceKey=key, sourceNamespace=}");
  }

  @Test
  public void rangeAnimatedColor() {
    int startColor = 0xff00ff00;
    int endColor = 0xff00ffff;

    DynamicColor animatedColor = DynamicColor.animate(startColor, endColor);
    DynamicColor animatedColorWithSpec = DynamicColor.animate(startColor, endColor, SPEC);

    assertThat(animatedColor.toDynamicColorProto().getAnimatableFixed().hasAnimationSpec())
         .isFalse();
    assertThat(animatedColorWithSpec.toDynamicColorProto().getAnimatableFixed().getFromArgb())
        .isEqualTo(startColor);
    assertThat(animatedColorWithSpec.toDynamicColorProto().getAnimatableFixed().getToArgb())
        .isEqualTo(endColor);
    assertThat(animatedColorWithSpec.toDynamicColorProto().getAnimatableFixed().getAnimationSpec())
        .isEqualTo(SPEC.toProto());
  }

  @Test
  public void rangeAnimatedToString() {
    assertThat(
            DynamicColor.animate(
                    /* start= */ 0x00000001,
                    /* end= */ 0x00000002,
                    new AnimationSpec.Builder().build())
                .toString())
        .isEqualTo(
            "AnimatableFixedColor{"
                + "fromArgb=1, toArgb=2, animationSpec=AnimationSpec{"
                + "animationParameters=null, repeatable=null}}");
  }

  @Test
  public void stateAnimatedColor() {
    DynamicColor stateColor = DynamicColor.fromState(STATE_KEY);

    DynamicColor animatedColor = DynamicColor.animate(STATE_KEY);
    DynamicColor animatedColorWithSpec = DynamicColor.animate(STATE_KEY, SPEC);

    assertThat(animatedColor.toDynamicColorProto().getAnimatableDynamic().hasAnimationSpec())
        .isFalse();
    assertThat(animatedColorWithSpec.toDynamicColorProto().getAnimatableDynamic().getInput())
        .isEqualTo(stateColor.toDynamicColorProto());
    assertThat(
            animatedColorWithSpec.toDynamicColorProto().getAnimatableDynamic().getAnimationSpec()
    ).isEqualTo(SPEC.toProto());
    assertThat(animatedColor.toDynamicColorProto())
        .isEqualTo(stateColor.animate().toDynamicColorProto());
  }

  @Test
  public void stateAnimatedToString() {
    assertThat(
            DynamicColor.animate(
                    /* stateKey= */ "key",
                    new AnimationSpec.Builder()
                            .setAnimationParameters(
                                    new AnimationParameters.Builder().setDelayMillis(1).build())
                            .build())
                .toString())
        .isEqualTo(
            "AnimatableDynamicColor{"
                + "input=StateColorSource{sourceKey=key, sourceNamespace=}, "
                + "animationSpec=AnimationSpec{"
                + "animationParameters=AnimationParameters{durationMillis=0, easing=null, "
                + "delayMillis=1}, repeatable=null}}");
  }

  @Test
  public void validProto() {
    DynamicColor from = DynamicColor.constant(CONSTANT_VALUE);
    DynamicColor to = DynamicColor.fromByteArray(from.toDynamicColorByteArray());

    assertThat(to.toDynamicColorProto().getFixed().getArgb()).isEqualTo(CONSTANT_VALUE);
  }

  @Test
  public void invalidProto() {
    assertThrows(IllegalArgumentException.class, () -> DynamicColor.fromByteArray(new byte[] {1}));
  }
}
