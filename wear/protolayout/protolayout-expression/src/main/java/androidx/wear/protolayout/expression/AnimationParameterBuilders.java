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

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto;
import androidx.wear.protolayout.protobuf.ExtensionRegistryLite;
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Builders for parameters that can be used to customize an animation. */
public final class AnimationParameterBuilders {
  private AnimationParameterBuilders() {}

  /** Prebuilt easing functions with cubic polynomial easing. */
  public static class EasingFunctions {
    private static CubicBezierEasing buildCubicBezierEasing(
        float x1, float y1, float x2, float y2) {
      return new CubicBezierEasing.Builder().setX1(x1).setY1(y1).setX2(x2).setY2(y2).build();
    }

    private EasingFunctions() {}

    /**
     * Elements that begin and end at rest use this standard easing. They speed up quickly and slow
     * down gradually, in order to emphasize the end of the transition.
     *
     * <p>Standard easing puts subtle attention at the end of an animation, by giving more time to
     * deceleration than acceleration. It is the most common form of easing.
     *
     * <p>This is equivalent to the Compose {@code FastOutSlowInEasing}.
     */
    @NonNull
    public static final Easing FAST_OUT_SLOW_IN_EASING =
        buildCubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f);

    /**
     * Incoming elements are animated using deceleration easing, which starts a transition at peak
     * velocity (the fastest point of an element’s movement) and ends at rest.
     *
     * <p>This is equivalent to the Compose {@code LinearOutSlowInEasing}.
     */
    @NonNull
    public static final Easing LINEAR_OUT_SLOW_IN_EASING =
        buildCubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f);

    /**
     * Elements exiting a screen use acceleration easing, where they start at rest and end at peak
     * velocity.
     *
     * <p>This is equivalent to the Compose {@code FastOutLinearInEasing}.
     */
    @NonNull
    public static final Easing FAST_OUT_LINEAR_IN_EASING =
        buildCubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f);
  }

  /**
   * The repeat mode to specify how animation will behave when repeated.
   *
   * @since 1.2
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
  public static final class AnimationSpec {
    private final AnimationParameterProto.AnimationSpec mImpl;
    @Nullable private final Fingerprint mFingerprint;

    AnimationSpec(AnimationParameterProto.AnimationSpec impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets animation parameters including duration, easing and repeat delay.
     *
     * @since 1.2
     */
    @Nullable
    public AnimationParameters getAnimationParameters() {
      if (mImpl.hasAnimationParameters()) {
        return AnimationParameters.fromProto(mImpl.getAnimationParameters());
      } else {
        return null;
      }
    }

    /**
     * Gets the repeatable mode to be used for specifying repetition parameters for the animation.
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
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /**
     * Creates a new wrapper instance from the proto.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static AnimationSpec fromProto(
        @NonNull AnimationParameterProto.AnimationSpec proto, @Nullable Fingerprint fingerprint) {
      return new AnimationSpec(proto, fingerprint);
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static AnimationSpec fromProto(@NonNull AnimationParameterProto.AnimationSpec proto) {
      return fromProto(proto, null);
    }

    /**
     * Returns the internal proto instance.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public AnimationParameterProto.AnimationSpec toProto() {
      return mImpl;
    }

    @Override
    @NonNull
    public String toString() {
      return "AnimationSpec{"
          + "animationParameters="
          + getAnimationParameters()
          + ", repeatable="
          + getRepeatable()
          + "}";
    }

    /** Builder for {@link AnimationSpec} */
    public static final class Builder {
      private final AnimationParameterProto.AnimationSpec.Builder mImpl =
          AnimationParameterProto.AnimationSpec.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-2136602843);

      public Builder() {}

      /**
       * Sets animation parameters including duration, easing and repeat delay.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setAnimationParameters(@NonNull AnimationParameters animationParameters) {
        mImpl.setAnimationParameters(animationParameters.toProto());
        mFingerprint.recordPropertyUpdate(
            4, checkNotNull(animationParameters.getFingerprint()).aggregateValueAsInt());
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

      /** Sets the animation to repeat indefinitely with the given repeat mode. */
      @NonNull
      @SuppressWarnings("MissingGetterMatchingBuilder")
      public Builder setInfiniteRepeatable(@RepeatMode int mode) {
        Repeatable repeatable =
            new Repeatable.Builder().setRepeatMode(mode).build();
        return this.setRepeatable(repeatable);
      }

      /** Builds an instance from accumulated values. */
      @NonNull
      public AnimationSpec build() {
        return new AnimationSpec(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Animation specs of duration, easing and repeat delay.
   *
   * @since 1.2
   */
  public static final class AnimationParameters {
    private final AnimationParameterProto.AnimationParameters mImpl;
    @Nullable private final Fingerprint mFingerprint;

    AnimationParameters(
        AnimationParameterProto.AnimationParameters impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the duration of the animation in milliseconds.
     *
     * @since 1.2
     */
    @IntRange(from = 0)
    public long getDurationMillis() {
      return mImpl.getDurationMillis();
    }

    /**
     * Gets the easing to be used for adjusting an animation's fraction.
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
     * Gets animation delay in millis. When used outside repeatable, this is the delay to start the
     * animation in milliseconds. When set inside repeatable, this is the delay before repeating
     * animation in milliseconds.
     *
     * @since 1.2
     */
    @IntRange(from = 0)
    public long getDelayMillis() {
      return mImpl.getDelayMillis();
    }

    /** Get the fingerprint for this object, or null if unknown. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static AnimationParameters fromProto(
        @NonNull AnimationParameterProto.AnimationParameters proto,
        @Nullable Fingerprint fingerprint) {
      return new AnimationParameters(proto, fingerprint);
    }

    @NonNull
    static AnimationParameters fromProto(
        @NonNull AnimationParameterProto.AnimationParameters proto) {
      return fromProto(proto, null);
    }

    /** Returns the internal proto instance. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public AnimationParameterProto.AnimationParameters toProto() {
      return mImpl;
    }

    @Override
    @NonNull
    public String toString() {
      return "AnimationParameters{"
          + "durationMillis="
          + getDurationMillis()
          + ", easing="
          + getEasing()
          + ", delayMillis="
          + getDelayMillis()
          + "}";
    }

    /** Builder for {@link AnimationParameters} */
    public static final class Builder {
      private final AnimationParameterProto.AnimationParameters.Builder mImpl =
          AnimationParameterProto.AnimationParameters.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-1301308590);

      public Builder() {}

      /**
       * Sets the duration of the animation in milliseconds. If not set, defaults to 300ms.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setDurationMillis(@IntRange(from = 0) long durationMillis) {
        mImpl.setDurationMillis(durationMillis);
        mFingerprint.recordPropertyUpdate(1, Long.hashCode(durationMillis));
        return this;
      }

      /**
       * Sets the easing to be used for adjusting an animation's fraction. If not set, defaults to
       * Linear Interpolator.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setEasing(@NonNull Easing easing) {
        mImpl.setEasing(easing.toEasingProto());
        mFingerprint.recordPropertyUpdate(
            2, checkNotNull(easing.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets animation delay in millis. When used outside repeatable, this is the delay to start
       * the animation in milliseconds. When set inside repeatable, this is the delay before
       * repeating animation in milliseconds. If not set, no delay will be applied.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setDelayMillis(@IntRange(from = 0) long delayMillis) {
        mImpl.setDelayMillis(delayMillis);
        mFingerprint.recordPropertyUpdate(3, Long.hashCode(delayMillis));
        return this;
      }

      /** Builds an instance from accumulated values. */
      @NonNull
      public AnimationParameters build() {
        return new AnimationParameters(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining the easing to be used for adjusting an animation's fraction. This allows
   * animation to speed up and slow down, rather than moving at a constant rate. If not set,
   * defaults to Linear Interpolator.
   *
   * @since 1.2
   */
  public interface Easing {
    /**
     * Get the protocol buffer representation of this object.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    AnimationParameterProto.Easing toEasingProto();

    /** Creates a {@link Easing} from a byte array generated by {@link #toEasingByteArray()}. */
    @NonNull
    static Easing fromByteArray(@NonNull byte[] byteArray) {
      try {
        return easingFromProto(
                AnimationParameterProto.Easing.parseFrom(
                        byteArray, ExtensionRegistryLite.getEmptyRegistry()));
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException("Byte array could not be parsed into Easing", e);
      }
    }

    /** Creates a byte array that can later be used with {@link #fromByteArray(byte[])}. */
    @NonNull
    default byte[] toEasingByteArray() {
      return toEasingProto().toByteArray();
    }

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /**
     * Builder to create {@link Easing} objects.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      Easing build();
    }
  }

  /**
   * Creates a new wrapper instance from the proto.
   *
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  @NonNull
  public static Easing easingFromProto(
      @NonNull AnimationParameterProto.Easing proto, @Nullable Fingerprint fingerprint) {
    if (proto.hasCubicBezier()) {
      return CubicBezierEasing.fromProto(proto.getCubicBezier(), fingerprint);
    }
    throw new IllegalStateException("Proto was not a recognised instance of Easing");
  }

  @NonNull
  static Easing easingFromProto(@NonNull AnimationParameterProto.Easing proto) {
    return easingFromProto(proto, null);
  }

  /**
   * The cubic polynomial easing that implements third-order Bezier curves. This is equivalent to
   * the Android PathInterpolator.
   *
   * @since 1.2
   */
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
     * first control point is tangent to the easing at the point (0, 0).
     *
     * @since 1.2
     */
    public float getX1() {
      return mImpl.getX1();
    }

    /**
     * Gets the y coordinate of the first control point. The line through the point (0, 0) and the
     * first control point is tangent to the easing at the point (0, 0).
     *
     * @since 1.2
     */
    public float getY1() {
      return mImpl.getY1();
    }

    /**
     * Gets the x coordinate of the second control point. The line through the point (1, 1) and the
     * second control point is tangent to the easing at the point (1, 1).
     *
     * @since 1.2
     */
    public float getX2() {
      return mImpl.getX2();
    }

    /**
     * Gets the y coordinate of the second control point. The line through the point (1, 1) and the
     * second control point is tangent to the easing at the point (1, 1).
     *
     * @since 1.2
     */
    public float getY2() {
      return mImpl.getY2();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /**
     * Creates a new wrapper instance from the proto.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static CubicBezierEasing fromProto(
        @NonNull AnimationParameterProto.CubicBezierEasing proto,
        @Nullable Fingerprint fingerprint) {
      return new CubicBezierEasing(proto, fingerprint);
    }

    @NonNull
    static CubicBezierEasing fromProto(@NonNull AnimationParameterProto.CubicBezierEasing proto) {
      return fromProto(proto, null);
    }

    /**
     * Returns the internal proto instance.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    AnimationParameterProto.CubicBezierEasing toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public AnimationParameterProto.Easing toEasingProto() {
      return AnimationParameterProto.Easing.newBuilder().setCubicBezier(mImpl).build();
    }

    @Override
    @NonNull
    public String toString() {
      return "CubicBezierEasing{"
          + "x1="
          + getX1()
          + ", y1="
          + getY1()
          + ", x2="
          + getX2()
          + ", y2="
          + getY2()
          + "}";
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
  public static final class Repeatable {
    private final AnimationParameterProto.Repeatable mImpl;
    @Nullable private final Fingerprint mFingerprint;

    Repeatable(AnimationParameterProto.Repeatable impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the number specifying how many times animation will be repeated. this method can only be
     * called if {@link #hasInfiniteIteration()} is false.
     *
     * @throws IllegalStateException if {@link #hasInfiniteIteration()} is true.
     * @since 1.2
     */
    public int getIterations() {
      if (hasInfiniteIteration()) {
        throw new IllegalStateException("Repeatable has infinite iteration.");
      }
      return mImpl.getIterations();
    }

    /** Returns true if the animation has indefinite repeat. */
    public boolean hasInfiniteIteration() { return isInfiniteIteration(mImpl.getIterations()); }

    /**
     * Gets the repeat mode to specify how animation will behave when repeated.
     *
     * @since 1.2
     */
    @RepeatMode
    public int getRepeatMode() {
      return mImpl.getRepeatMode().getNumber();
    }

    /**
     * Gets optional custom parameters for the forward passes of animation.
     *
     * @since 1.2
     */
    @Nullable
    public AnimationParameters getForwardRepeatOverride() {
      if (mImpl.hasForwardRepeatOverride()) {
        return AnimationParameters.fromProto(mImpl.getForwardRepeatOverride());
      } else {
        return null;
      }
    }

    /**
     * Gets optional custom parameters for the reverse passes of animation.
     *
     * @since 1.2
     */
    @Nullable
    public AnimationParameters getReverseRepeatOverride() {
      if (mImpl.hasReverseRepeatOverride()) {
        return AnimationParameters.fromProto(mImpl.getReverseRepeatOverride());
      } else {
        return null;
      }
    }

    /** Get the fingerprint for this object, or null if unknown. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /**
     * Creates a new wrapper instance from the proto.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static Repeatable fromProto(
        @NonNull AnimationParameterProto.Repeatable proto, @Nullable Fingerprint fingerprint) {
      return new Repeatable(proto, fingerprint);
    }

    @NonNull
    static Repeatable fromProto(@NonNull AnimationParameterProto.Repeatable proto) {
      return fromProto(proto, null);
    }

    /**
     * Returns the internal proto instance.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public AnimationParameterProto.Repeatable toProto() {
      return mImpl;
    }

    static boolean isInfiniteIteration(int iteration){
      return iteration < 1;
    }

    @Override
    @NonNull
    public String toString() {
      return "Repeatable{"
          + "iterations="
          + getIterations()
          + ", repeatMode="
          + getRepeatMode()
          + ", forwardRepeatOverride="
          + getForwardRepeatOverride()
          + ", reverseRepeatOverride="
          + getReverseRepeatOverride()
          + "}";
    }

    /** Builder for {@link Repeatable} */
    public static final class Builder {
      private final AnimationParameterProto.Repeatable.Builder mImpl =
          AnimationParameterProto.Repeatable.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(2110475048);

      public Builder() {}

      /**
       * Sets the number specifying how many times animation will be repeated. If not set, defaults
       * to repeating infinitely.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setIterations(@IntRange(from = 1) int iterations) {
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

      /**
       * Sets optional custom parameters for the forward passes of animation. If not set, use the
       * main animation parameters set outside of {@link Repeatable}.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setForwardRepeatOverride(
          @NonNull AnimationParameters forwardRepeatOverride) {
        mImpl.setForwardRepeatOverride(forwardRepeatOverride.toProto());
        mFingerprint.recordPropertyUpdate(
            6, checkNotNull(forwardRepeatOverride.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets optional custom parameters for the reverse passes of animation. If not set, use the
       * main animation parameters set outside of {@link Repeatable}.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setReverseRepeatOverride(
          @NonNull AnimationParameters reverseRepeatOverride) {
        mImpl.setReverseRepeatOverride(reverseRepeatOverride.toProto());
        mFingerprint.recordPropertyUpdate(
            7, checkNotNull(reverseRepeatOverride.getFingerprint()).aggregateValueAsInt());
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
