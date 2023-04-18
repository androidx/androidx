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

import android.annotation.SuppressLint;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.FixedProto;
import androidx.wear.protolayout.expression.proto.StateEntryProto;

/**
 * Builders for fixed value primitive types that can be used in dynamic expressions and in for state
 * state values.
 */
final class FixedValueBuilders {
  private FixedValueBuilders() {}

  /**
   * A fixed int32 type.
   *
   * @since 1.2
   */
  static final class FixedInt32
      implements DynamicBuilders.DynamicInt32, StateEntryBuilders.StateEntryValue {
    private final FixedProto.FixedInt32 mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FixedInt32(FixedProto.FixedInt32 impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the value.
     *
     * @since 1.2
     */
    public int getValue() {
      return mImpl.getValue();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static FixedInt32 fromProto(
            @NonNull FixedProto.FixedInt32 proto, @Nullable Fingerprint fingerprint) {
      return new FixedInt32(proto, fingerprint);
    }

    @NonNull
    static FixedInt32 fromProto(@NonNull FixedProto.FixedInt32 proto) {
      return fromProto(proto, null);
    }

    @NonNull
    FixedProto.FixedInt32 toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
      return DynamicProto.DynamicInt32.newBuilder().setFixed(mImpl).build();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public StateEntryProto.StateEntryValue toStateEntryValueProto() {
      return StateEntryProto.StateEntryValue.newBuilder().setInt32Val(mImpl).build();
    }

    @Override
    @NonNull
    public String toString() {
      return "FixedInt32{" + "value=" + getValue() + "}";
    }

    /** Builder for {@link FixedInt32}. */
    public static final class Builder
        implements DynamicBuilders.DynamicInt32.Builder,
            StateEntryBuilders.StateEntryValue.Builder {
      private final FixedProto.FixedInt32.Builder mImpl = FixedProto.FixedInt32.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(974881783);

      public Builder() {}

      /**
       * Sets the value.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setValue(int value) {
        mImpl.setValue(value);
        mFingerprint.recordPropertyUpdate(1, value);
        return this;
      }

      @Override
      @NonNull
      public FixedInt32 build() {
        return new FixedInt32(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A fixed string type.
   *
   * @since 1.2
   */
  static final class FixedString
      implements DynamicBuilders.DynamicString, StateEntryBuilders.StateEntryValue {
    private final FixedProto.FixedString mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FixedString(FixedProto.FixedString impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the value.
     *
     * @since 1.2
     */
    @NonNull
    public String getValue() {
      return mImpl.getValue();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static FixedString fromProto(
            @NonNull FixedProto.FixedString proto, @Nullable Fingerprint fingerprint) {
      return new FixedString(proto, fingerprint);
    }

    @NonNull
    static FixedString fromProto(@NonNull FixedProto.FixedString proto) {
      return fromProto(proto, null);
    }

    @NonNull
    FixedProto.FixedString toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicString toDynamicStringProto() {
      return DynamicProto.DynamicString.newBuilder().setFixed(mImpl).build();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public StateEntryProto.StateEntryValue toStateEntryValueProto() {
      return StateEntryProto.StateEntryValue.newBuilder().setStringVal(mImpl).build();
    }

    @Override
    @NonNull
    public String toString() {
      return "FixedString{" + "value=" + getValue() + "}";
    }

    /** Builder for {@link FixedString}. */
    public static final class Builder
        implements DynamicBuilders.DynamicString.Builder,
            StateEntryBuilders.StateEntryValue.Builder {
      private final FixedProto.FixedString.Builder mImpl = FixedProto.FixedString.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1963352072);

      public Builder() {}

      /**
       * Sets the value.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setValue(@NonNull String value) {
        mImpl.setValue(value);
        mFingerprint.recordPropertyUpdate(1, value.hashCode());
        return this;
      }

      @Override
      @NonNull
      public FixedString build() {
        return new FixedString(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A fixed float type.
   *
   * @since 1.2
   */
  static final class FixedFloat
      implements DynamicBuilders.DynamicFloat, StateEntryBuilders.StateEntryValue {
    private final FixedProto.FixedFloat mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FixedFloat(FixedProto.FixedFloat impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the value. Note that a NaN value is considered invalid and any expression with this node
     * will have an invalid value delivered via {@link DynamicTypeValueReceiver<T>#onInvalidate()}.
     *
     * @since 1.2
     */
    public float getValue() {
      return mImpl.getValue();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static FixedFloat fromProto(
            @NonNull FixedProto.FixedFloat proto, @Nullable Fingerprint fingerprint) {
      return new FixedFloat(proto, fingerprint);
    }

    @NonNull
    static FixedFloat fromProto(@NonNull FixedProto.FixedFloat proto) {
      return fromProto(proto, null);
    }

    @NonNull
    FixedProto.FixedFloat toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicFloat toDynamicFloatProto() {
      return DynamicProto.DynamicFloat.newBuilder().setFixed(mImpl).build();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public StateEntryProto.StateEntryValue toStateEntryValueProto() {
      return StateEntryProto.StateEntryValue.newBuilder().setFloatVal(mImpl).build();
    }

    @Override
    @NonNull
    public String toString() {
      return "FixedFloat{" + "value=" + getValue() + "}";
    }

    /** Builder for {@link FixedFloat}. */
    public static final class Builder
        implements DynamicBuilders.DynamicFloat.Builder,
            StateEntryBuilders.StateEntryValue.Builder {
      private final FixedProto.FixedFloat.Builder mImpl = FixedProto.FixedFloat.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-144724541);

      public Builder() {}


      /**
       * Sets the value. Note that a NaN value is considered invalid and any expression with this
       * node will have an invalid value delivered via
       * {@link DynamicTypeValueReceiver<T>#onInvalidate()}.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setValue(float value) {
        mImpl.setValue(value);
        mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
        return this;
      }

      @Override
      @NonNull
      public FixedFloat build() {
        return new FixedFloat(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A fixed boolean type.
   *
   * @since 1.2
   */
  static final class FixedBool
      implements DynamicBuilders.DynamicBool, StateEntryBuilders.StateEntryValue {
    private final FixedProto.FixedBool mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FixedBool(FixedProto.FixedBool impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the value.
     *
     * @since 1.2
     */
    public boolean getValue() {
      return mImpl.getValue();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static FixedBool fromProto(
            @NonNull FixedProto.FixedBool proto, @Nullable Fingerprint fingerprint) {
      return new FixedBool(proto, fingerprint);
    }

    @NonNull
    static FixedBool fromProto(@NonNull FixedProto.FixedBool proto) {
      return fromProto(proto, null);
    }

    @NonNull
    FixedProto.FixedBool toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicBool toDynamicBoolProto() {
      return DynamicProto.DynamicBool.newBuilder().setFixed(mImpl).build();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public StateEntryProto.StateEntryValue toStateEntryValueProto() {
      return StateEntryProto.StateEntryValue.newBuilder().setBoolVal(mImpl).build();
    }

    @Override
    @NonNull
    public String toString() {
      return "FixedBool{" + "value=" + getValue() + "}";
    }

    /** Builder for {@link FixedBool}. */
    public static final class Builder
        implements DynamicBuilders.DynamicBool.Builder, StateEntryBuilders.StateEntryValue.Builder {
      private final FixedProto.FixedBool.Builder mImpl = FixedProto.FixedBool.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-665116398);

      public Builder() {}

      /**
       * Sets the value.
       *
       * @since 1.2
       */
      @SuppressLint("MissingGetterMatchingBuilder")
      @NonNull
      public Builder setValue(boolean value) {
        mImpl.setValue(value);
        mFingerprint.recordPropertyUpdate(1, Boolean.hashCode(value));
        return this;
      }

      @Override
      @NonNull
      public FixedBool build() {
        return new FixedBool(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A fixed color type.
   *
   * @since 1.2
   */
  static final class FixedColor
      implements DynamicBuilders.DynamicColor, StateEntryBuilders.StateEntryValue {
    private final FixedProto.FixedColor mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FixedColor(FixedProto.FixedColor impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the color value, in ARGB format.
     *
     * @since 1.2
     */
    @ColorInt
    public int getArgb() {
      return mImpl.getArgb();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static FixedColor fromProto(
            @NonNull FixedProto.FixedColor proto, @Nullable Fingerprint fingerprint) {
      return new FixedColor(proto, fingerprint);
    }

    @NonNull
    static FixedColor fromProto(@NonNull FixedProto.FixedColor proto) {
      return fromProto(proto, null);
    }

    @NonNull
    FixedProto.FixedColor toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicColor toDynamicColorProto() {
      return DynamicProto.DynamicColor.newBuilder().setFixed(mImpl).build();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public StateEntryProto.StateEntryValue toStateEntryValueProto() {
      return StateEntryProto.StateEntryValue.newBuilder().setColorVal(mImpl).build();
    }

    @Override
    @NonNull
    public String toString() {
      return "FixedColor{" + "argb=" + getArgb() + "}";
    }

    /** Builder for {@link FixedColor}. */
    public static final class Builder
        implements DynamicBuilders.DynamicColor.Builder,
            StateEntryBuilders.StateEntryValue.Builder {
      private final FixedProto.FixedColor.Builder mImpl = FixedProto.FixedColor.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-1895809356);

      public Builder() {}

      /**
       * Sets the color value, in ARGB format.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setArgb(@ColorInt int argb) {
        mImpl.setArgb(argb);
        mFingerprint.recordPropertyUpdate(1, argb);
        return this;
      }

      @Override
      @NonNull
      public FixedColor build() {
        return new FixedColor(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A fixed time instant type.
   *
   * @since 1.2
   */
  static final class FixedInstant implements DynamicBuilders.DynamicInstant {
    private final FixedProto.FixedInstant mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FixedInstant(FixedProto.FixedInstant impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the number of seconds that have elapsed since 00:00:00 UTC on 1 January 1970.
     *
     * @since 1.2
     */
    public long getEpochSeconds() {
      return mImpl.getEpochSeconds();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static FixedInstant fromProto(
            @NonNull FixedProto.FixedInstant proto, @Nullable Fingerprint fingerprint) {
      return new FixedInstant(proto, fingerprint);
    }

    @NonNull
    static FixedInstant fromProto(@NonNull FixedProto.FixedInstant proto) {
      return fromProto(proto, null);
    }

    @NonNull
    FixedProto.FixedInstant toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicInstant toDynamicInstantProto() {
      return DynamicProto.DynamicInstant.newBuilder().setFixed(mImpl).build();
    }

    @Override
    @NonNull
    public String toString() {
      return "FixedInstant{" + "epochSeconds=" + getEpochSeconds() + "}";
    }

    /** Builder for {@link FixedInstant}. */
    public static final class Builder implements DynamicBuilders.DynamicInstant.Builder {
      private final FixedProto.FixedInstant.Builder mImpl = FixedProto.FixedInstant.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-1986552556);

      public Builder() {}

      /**
       * Sets the number of seconds that have elapsed since 00:00:00 UTC on 1 January 1970.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setEpochSeconds(long epochSeconds) {
        mImpl.setEpochSeconds(epochSeconds);
        mFingerprint.recordPropertyUpdate(1, Long.hashCode(epochSeconds));
        return this;
      }

      @Override
      @NonNull
      public FixedInstant build() {
        return new FixedInstant(mImpl.build(), mFingerprint);
      }
    }
  }
}
