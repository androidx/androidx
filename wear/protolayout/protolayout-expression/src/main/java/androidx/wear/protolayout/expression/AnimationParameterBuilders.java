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

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Builders for parameters that can be used to customize an animation. */
final class AnimationParameterBuilders {
  private AnimationParameterBuilders() {}

  /**
   * The repeat mode to specify how animation will behave when repeated.
   *
   * @since 1.2
   * @hide
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY)
  @IntDef({REPEAT_MODE_UNKNOWN, REPEAT_MODE_RESTART, REPEAT_MODE_REVERSE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface RepeatMode {}

  /**
   * The unknown repeat mode.
   *
   * @since 1.2
   */
  public static final int REPEAT_MODE_UNKNOWN = 0;

  /**
   * The repeat mode where animation restarts from the beginning when repeated.
   *
   * @since 1.2
   */
  public static final int REPEAT_MODE_RESTART = 1;

  /**
   * The repeat mode where animation is played in reverse when repeated.
   *
   * @since 1.2
   */
  public static final int REPEAT_MODE_REVERSE = 2;

  /**
   * Animation parameters that can be added to any animatable node.
   *
   * @since 1.2
   */
  @ProtoLayoutExperimental
  public static final class AnimationSpec {
    private final AnimationParameterProto.AnimationSpec mImpl;
    @Nullable private final Fingerprint mFingerprint;

    AnimationSpec(AnimationParameterProto.AnimationSpec impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the duration of the animation in milliseconds. If not set, defaults to 300ms. Intended
     * for testing purposes only.
     *
     * @since 1.2
     */
    public int getDurationMillis() {
      return mImpl.getDurationMillis();
    }

    /**
     * Gets the delay to start the animation in milliseconds. If not set, defaults to 0. Intended
     * for testing purposes only.
     *
     * @since 1.2
     */
    public int getDelayMillis() {
      return mImpl.getDelayMillis();
    }

    /**
     * Gets the easing to be used for adjusting an animation’s fraction. If not set, defaults to
     * Linear Interpolator. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public Easing getEasing() {
      if (mImpl.hasEasing()) {
        return AnimationParameterBuilders.easingFromProto(mImpl.getEasing());
      } else {
        return null;
      }
    }

    /**
     * Gets the repeatable mode to be used for specifying repetition parameters for the animation.
     * If not set, animation won't be repeated. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public Repeatable getRepeatable() {
      if (mImpl.hasRepeatable()) {
        return Repeatable.fromProto(mImpl.getRepeatable());
      } else {
        return null;
      }
    }

    /**
     * Get the fingerprint for this object, or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static AnimationSpec fromProto(@NonNull AnimationParameterProto.AnimationSpec proto) {
      return new AnimationSpec(proto, null);
    }

    /**
     * Returns the internal proto instance.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public AnimationParameterProto.AnimationSpec toProto() {
      return mImpl;
    }

    /** Builder for {@link AnimationSpec} */
    public static final class Builder {
      private final AnimationParameterProto.AnimationSpec.Builder mImpl =
          AnimationParameterProto.AnimationSpec.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-2136602843);

      public Builder() {}

      /**
       * Sets the duration of the animation in milliseconds. If not set, defaults to 300ms.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setDurationMillis(int durationMillis) {
        mImpl.setDurationMillis(durationMillis);
        mFingerprint.recordPropertyUpdate(1, durationMillis);
        return this;
      }

      /**
       * Sets the delay to start the animation in milliseconds. If not set, defaults to 0.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setDelayMillis(int delayMillis) {
        mImpl.setDelayMillis(delayMillis);
        mFingerprint.recordPropertyUpdate(2, delayMillis);
        return this;
      }

      /**
       * Sets the easing to be used for adjusting an animation’s fraction. If not set, defaults to
       * Linear Interpolator.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setEasing(@NonNull Easing easing) {
        mImpl.setEasing(easing.toEasingProto());
        mFingerprint.recordPropertyUpdate(
            3, checkNotNull(easing.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the repeatable mode to be used for specifying repetition parameters for the animation.
       * If not set, animation won't be repeated.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setRepeatable(@NonNull Repeatable repeatable) {
        mImpl.setRepeatable(repeatable.toProto());
        mFingerprint.recordPropertyUpdate(
            5, checkNotNull(repeatable.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /** Builds an instance from accumulated values. */
      @NonNull
      public AnimationSpec build() {
        return new AnimationSpec(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining the easing to be used for adjusting an animation’s fraction. This allows
   * animation to speed up and slow down, rather than moving at a constant rate. If not set,
   * defaults to Linear Interpolator.
   *
   * @since 1.2
   */
  @ProtoLayoutExperimental
  public interface Easing {
    /**
     * Get the protocol buffer representation of this object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    AnimationParameterProto.Easing toEasingProto();

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link Easing} objects. */
    @SuppressLint("StaticFinalBuilder")
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      Easing build();
    }
  }

  @NonNull
  @ProtoLayoutExperimental
  static Easing easingFromProto(@NonNull AnimationParameterProto.Easing proto) {
    if (proto.hasCubicBezier()) {
      return CubicBezierEasing.fromProto(proto.getCubicBezier());
    }
    throw new IllegalStateException("Proto was not a recognised instance of Easing");
  }

  /**
   * The cubic polynomial easing that implements third-order Bézier curves. This is equivalent to
   * the Android PathInterpolator.
   *
   * @since 1.2
   */
  @ProtoLayoutExperimental
  public static final class CubicBezierEasing implements Easing {
    private final AnimationParameterProto.CubicBezierEasing mImpl;
    @Nullable private final Fingerprint mFingerprint;

    CubicBezierEasing(
        AnimationParameterProto.CubicBezierEasing impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the x coordinate of the first control point. The line through the point (0, 0) and the
     * first control point is tangent to the easing at the point (0, 0). Intended for testing
     * purposes only.
     *
     * @since 1.2
     */
    public float getX1() {
      return mImpl.getX1();
    }

    /**
     * Gets the y coordinate of the first control point. The line through the point (0, 0) and the
     * first control point is tangent to the easing at the point (0, 0). Intended for testing
     * purposes only.
     *
     * @since 1.2
     */
    public float getY1() {
      return mImpl.getY1();
    }

    /**
     * Gets the x coordinate of the second control point. The line through the point (1, 1) and the
     * second control point is tangent to the easing at the point (1, 1). Intended for testing
     * purposes only.
     *
     * @since 1.2
     */
    public float getX2() {
      return mImpl.getX2();
    }

    /**
     * Gets the y coordinate of the second control point. The line through the point (1, 1) and the
     * second control point is tangent to the easing at the point (1, 1). Intended for testing
     * purposes only.
     *
     * @since 1.2
     */
    public float getY2() {
      return mImpl.getY2();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static CubicBezierEasing fromProto(@NonNull AnimationParameterProto.CubicBezierEasing proto) {
      return new CubicBezierEasing(proto, null);
    }

    @NonNull
    AnimationParameterProto.CubicBezierEasing toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @ProtoLayoutExperimental
    public AnimationParameterProto.Easing toEasingProto() {
      return AnimationParameterProto.Easing.newBuilder().setCubicBezier(mImpl).build();
    }

    /** Builder for {@link CubicBezierEasing}. */
    public static final class Builder implements Easing.Builder {
      private final AnimationParameterProto.CubicBezierEasing.Builder mImpl =
          AnimationParameterProto.CubicBezierEasing.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(856403705);

      public Builder() {}

      /**
       * Sets the x coordinate of the first control point. The line through the point (0, 0) and the
       * first control point is tangent to the easing at the point (0, 0).
       *
       * @since 1.2
       */
      @NonNull
      public Builder setX1(float x1) {
        mImpl.setX1(x1);
        mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(x1));
        return this;
      }

      /**
       * Sets the y coordinate of the first control point. The line through the point (0, 0) and the
       * first control point is tangent to the easing at the point (0, 0).
       *
       * @since 1.2
       */
      @NonNull
      public Builder setY1(float y1) {
        mImpl.setY1(y1);
        mFingerprint.recordPropertyUpdate(2, Float.floatToIntBits(y1));
        return this;
      }

      /**
       * Sets the x coordinate of the second control point. The line through the point (1, 1) and
       * the second control point is tangent to the easing at the point (1, 1).
       *
       * @since 1.2
       */
      @NonNull
      public Builder setX2(float x2) {
        mImpl.setX2(x2);
        mFingerprint.recordPropertyUpdate(3, Float.floatToIntBits(x2));
        return this;
      }

      /**
       * Sets the y coordinate of the second control point. The line through the point (1, 1) and
       * the second control point is tangent to the easing at the point (1, 1).
       *
       * @since 1.2
       */
      @NonNull
      public Builder setY2(float y2) {
        mImpl.setY2(y2);
        mFingerprint.recordPropertyUpdate(4, Float.floatToIntBits(y2));
        return this;
      }

      @Override
      @NonNull
      public CubicBezierEasing build() {
        return new CubicBezierEasing(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * The repeatable mode to be used for specifying how many times animation will be repeated.
   *
   * @since 1.2
   */
  @ProtoLayoutExperimental
  public static final class Repeatable {
    private final AnimationParameterProto.Repeatable mImpl;
    @Nullable private final Fingerprint mFingerprint;

    Repeatable(AnimationParameterProto.Repeatable impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the number specifying how many times animation will be repeated. If not set, defaults to
     * 0, i.e. repeat infinitely. Intended for testing purposes only.
     *
     * @since 1.2
     */
    public int getIterations() {
      return mImpl.getIterations();
    }

    /**
     * Gets the repeat mode to specify how animation will behave when repeated. If not set, defaults
     * to restart. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @RepeatMode
    public int getRepeatMode() {
      return mImpl.getRepeatMode().getNumber();
    }

    /**
     * Get the fingerprint for this object, or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static Repeatable fromProto(@NonNull AnimationParameterProto.Repeatable proto) {
      return new Repeatable(proto, null);
    }

    @NonNull
    AnimationParameterProto.Repeatable toProto() {
      return mImpl;
    }

    /** Builder for {@link Repeatable} */
    public static final class Builder {
      private final AnimationParameterProto.Repeatable.Builder mImpl =
          AnimationParameterProto.Repeatable.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(2110475048);

      public Builder() {}

      /**
       * Sets the number specifying how many times animation will be repeated. If not set, defaults
       * to 0, i.e. repeat infinitely.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setIterations(int iterations) {
        mImpl.setIterations(iterations);
        mFingerprint.recordPropertyUpdate(1, iterations);
        return this;
      }

      /**
       * Sets the repeat mode to specify how animation will behave when repeated. If not set,
       * defaults to restart.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setRepeatMode(@RepeatMode int repeatMode) {
        mImpl.setRepeatMode(AnimationParameterProto.RepeatMode.forNumber(repeatMode));
        mFingerprint.recordPropertyUpdate(2, repeatMode);
        return this;
      }

      /** Builds an instance from accumulated values. */
      @NonNull
      public Repeatable build() {
        return new Repeatable(mImpl.build(), mFingerprint);
      }
    }
  }
}
