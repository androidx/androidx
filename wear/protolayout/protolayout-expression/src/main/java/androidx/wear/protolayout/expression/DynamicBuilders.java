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
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec;
import androidx.wear.protolayout.expression.ConditionScopes.ConditionScope;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedBool;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedColor;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedFloat;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedInt32;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedString;
import androidx.wear.protolayout.expression.StateEntryBuilders.StateEntryValue;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Builders for dynamic primitive types used by layout elements. */
public final class DynamicBuilders {
  private DynamicBuilders() {}

  /**
   * Rounding mode to use when converting a float to an int32.
   *
   * @since 1.2
   * @hide
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY)
  @IntDef({ROUND_MODE_UNDEFINED, ROUND_MODE_FLOOR, ROUND_MODE_ROUND, ROUND_MODE_CEILING})
  @Retention(RetentionPolicy.SOURCE)
  @interface FloatToInt32RoundMode {}

  /**
   * An undefined rounding mode.
   *
   * @since 1.2
   */
  static final int ROUND_MODE_UNDEFINED = 0;

  /**
   * Use floor(x) when rounding.
   *
   * @since 1.2
   */
  static final int ROUND_MODE_FLOOR = 1;

  /**
   * Use round(x) when rounding (i.e. rounds to the closest int).
   *
   * @since 1.2
   */
  static final int ROUND_MODE_ROUND = 2;

  /**
   * Use ceil(x) when rounding.
   *
   * @since 1.2
   */
  static final int ROUND_MODE_CEILING = 3;

  /**
   * The type of comparison used in {@link ComparisonInt32Op} and {@link ComparisonFloatOp}.
   *
   * @since 1.2
   * @hide
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY)
  @IntDef({
    COMPARISON_OP_TYPE_UNDEFINED,
    COMPARISON_OP_TYPE_EQUALS,
    COMPARISON_OP_TYPE_NOT_EQUALS,
    COMPARISON_OP_TYPE_LESS_THAN,
    COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO,
    COMPARISON_OP_TYPE_GREATER_THAN,
    COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface ComparisonOpType {}

  /**
   * Undefined operation type.
   *
   * @since 1.2
   */
  static final int COMPARISON_OP_TYPE_UNDEFINED = 0;

  /**
   * Equality check (result = LHS == RHS). For floats, for equality check, small epsilon is used,
   * i.e.: (result = abs(LHS - RHS) < epsilon).
   *
   * @since 1.2
   */
  static final int COMPARISON_OP_TYPE_EQUALS = 1;

  /**
   * Not equal check (result = LHS != RHS).
   *
   * @since 1.2
   */
  static final int COMPARISON_OP_TYPE_NOT_EQUALS = 2;

  /**
   * Strictly less than (result = LHS < RHS).
   *
   * @since 1.2
   */
  static final int COMPARISON_OP_TYPE_LESS_THAN = 3;

  /**
   * Less than or equal to (result = LHS <= RHS).
   *
   * @since 1.2
   */
  static final int COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO = 4;

  /**
   * Strictly greater than (result = LHS > RHS).
   *
   * @since 1.2
   */
  static final int COMPARISON_OP_TYPE_GREATER_THAN = 5;

  /**
   * Greater than or equal to (result = LHS >= RHS).
   *
   * @since 1.2
   */
  static final int COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO = 6;

  /**
   * The type of logical operation to carry out in a {@link LogicalBoolOp} operation.
   *
   * @since 1.2
   * @hide
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY)
  @IntDef({LOGICAL_OP_TYPE_UNDEFINED, LOGICAL_OP_TYPE_AND, LOGICAL_OP_TYPE_OR})
  @Retention(RetentionPolicy.SOURCE)
  @interface LogicalOpType {}

  /**
   * Undefined operation type.
   *
   * @since 1.2
   */
  static final int LOGICAL_OP_TYPE_UNDEFINED = 0;

  /**
   * Logical AND.
   *
   * @since 1.2
   */
  static final int LOGICAL_OP_TYPE_AND = 1;

  /**
   * Logical OR.
   *
   * @since 1.2
   */
  static final int LOGICAL_OP_TYPE_OR = 2;

  /**
   * A dynamic Int32 which sources its data from the tile's state.
   *
   * @since 1.2
   */
  static final class StateInt32Source implements DynamicInt32 {
    private final DynamicProto.StateInt32Source mImpl;
    @Nullable private final Fingerprint mFingerprint;

    StateInt32Source(DynamicProto.StateInt32Source impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the key in the state to bind to. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @NonNull
    public String getSourceKey() {
      return mImpl.getSourceKey();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static StateInt32Source fromProto(@NonNull DynamicProto.StateInt32Source proto) {
      return new StateInt32Source(proto, null);
    }

    @NonNull
    DynamicProto.StateInt32Source toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
      return DynamicProto.DynamicInt32.newBuilder().setStateSource(mImpl).build();
    }

    /** Builder for {@link StateInt32Source}. */
    public static final class Builder implements DynamicInt32.Builder {
      private final DynamicProto.StateInt32Source.Builder mImpl =
          DynamicProto.StateInt32Source.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(58614749);

      public Builder() {}

      /**
       * Sets the key in the state to bind to.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSourceKey(@NonNull String sourceKey) {
        mImpl.setSourceKey(sourceKey);
        mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
        return this;
      }

      @Override
      @NonNull
      public StateInt32Source build() {
        return new StateInt32Source(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Converts a Float to an Int32, with a customizable rounding mode.
   *
   * @since 1.2
   */
  static final class FloatToInt32Op implements DynamicInt32 {
    private final DynamicProto.FloatToInt32Op mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FloatToInt32Op(DynamicProto.FloatToInt32Op impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the float to round. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicFloat getInput() {
      if (mImpl.hasInput()) {
        return DynamicBuilders.dynamicFloatFromProto(mImpl.getInput());
      } else {
        return null;
      }
    }

    /**
     * Gets the rounding mode to use. Defaults to ROUND_MODE_FLOOR if not specified. Intended for
     * testing purposes only.
     *
     * @since 1.2
     */
    @FloatToInt32RoundMode
    public int getRoundMode() {
      return mImpl.getRoundMode().getNumber();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static FloatToInt32Op fromProto(@NonNull DynamicProto.FloatToInt32Op proto) {
      return new FloatToInt32Op(proto, null);
    }

    @NonNull
    DynamicProto.FloatToInt32Op toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
      return DynamicProto.DynamicInt32.newBuilder().setFloatToInt(mImpl).build();
    }

    /** Builder for {@link FloatToInt32Op}. */
    public static final class Builder implements DynamicInt32.Builder {
      private final DynamicProto.FloatToInt32Op.Builder mImpl =
          DynamicProto.FloatToInt32Op.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-1272973414);

      public Builder() {}

      /**
       * Sets the float to round.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInput(@NonNull DynamicFloat input) {
        mImpl.setInput(input.toDynamicFloatProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the rounding mode to use. Defaults to ROUND_MODE_FLOOR if not specified.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setRoundMode(@FloatToInt32RoundMode int roundMode) {
        mImpl.setRoundMode(DynamicProto.FloatToInt32RoundMode.forNumber(roundMode));
        mFingerprint.recordPropertyUpdate(2, roundMode);
        return this;
      }

      @Override
      @NonNull
      public FloatToInt32Op build() {
        return new FloatToInt32Op(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining a dynamic int32 type.
   *
   * @since 1.2
   */
  public interface DynamicInt32 extends DynamicType {
    /**
     * Get the protocol buffer representation of this object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    DynamicProto.DynamicInt32 toDynamicInt32Proto();

    /** Creates a constant-valued {@link DynamicInt32}. */
    @NonNull
    static DynamicInt32 constant(int constant) {
      return new FixedInt32.Builder().setValue(constant).build();
    }

    /**
     * Creates a {@link DynamicInt32} that is bound to the value of an item of the State.
     *
     * @param stateKey The key to a {@link StateEntryValue} with an int value from the provider's
     *     state.
     */
    @NonNull
    static DynamicInt32 fromState(@NonNull String stateKey) {
      return new StateInt32Source.Builder().setSourceKey(stateKey).build();
    }

    /** Convert the value represented by this {@link DynamicInt32} into a {@link DynamicFloat}. */
    @NonNull
    default DynamicFloat asFloat() {
      return new Int32ToFloatOp.Builder().setInput(this).build();
    }

    /**
     * Returns a {@link DynamicString} that contains the formatted value of this {@link
     * DynamicInt32} (with default formatting parameters). As an example, in the English locale, the
     * following is equal to {@code DynamicString.constant("12")}
     *
     * <pre>
     *   DynamicInt32.constant(12).format()
     * </pre>
     */
    @NonNull
    default DynamicString format() {
      return IntFormatter.with().buildForInput(this);
    }

    /**
     * Returns a {@link DynamicString} that contains the formatted value of this {@link
     * DynamicInt32}. As an example, in the English locale, the following is equal to {@code
     * DynamicString.constant("0,012")}
     *
     * <pre>
     *   DynamicInt32.constant(12)
     *            .format(
     *                IntFormatter.with().minIntegerDigits(4).groupingUsed(true));
     * </pre>
     *
     * @param formatter The formatting parameter.
     */
    @NonNull
    default DynamicString format(@NonNull IntFormatter formatter) {
      return formatter.buildForInput(this);
    }

    /** Allows formatting {@link DynamicInt32} into a {@link DynamicString}. */
    class IntFormatter {
      private final Int32FormatOp.Builder builder;

      private IntFormatter() {
        builder = new Int32FormatOp.Builder();
      }

      /** Creates an instance of {@link IntFormatter} with default configuration. */
      @NonNull
      public static IntFormatter with() {
        return new IntFormatter();
      }

      /**
       * Sets minimum number of integer digits for the formatter. Defaults to one if not specified.
       */
      @NonNull
      public IntFormatter minIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
        builder.setMinIntegerDigits(minIntegerDigits);
        return this;
      }

      /** Sets whether grouping is used for the formatter. Defaults to false if not specified. */
      @NonNull
      public IntFormatter groupingUsed(boolean groupingUsed) {
        builder.setGroupingUsed(groupingUsed);
        return this;
      }

      @NonNull
      Int32FormatOp buildForInput(@NonNull DynamicInt32 dynamicInt32) {
        return builder.setInput(dynamicInt32).build();
      }
    }

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link DynamicInt32} objects.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      DynamicInt32 build();
    }
  }

  /**
   * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
   * created using this method can't be added to any other wrapper.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  @NonNull
  public static DynamicInt32 dynamicInt32FromProto(@NonNull DynamicProto.DynamicInt32 proto) {
    if (proto.hasFixed()) {
      return FixedInt32.fromProto(proto.getFixed());
    }
    if (proto.hasStateSource()) {
      return StateInt32Source.fromProto(proto.getStateSource());
    }
    if (proto.hasFloatToInt()) {
      return FloatToInt32Op.fromProto(proto.getFloatToInt());
    }
    throw new IllegalStateException("Proto was not a recognised instance of DynamicInt32");
  }

  /**
   * Simple formatting for dynamic int32.
   *
   * @since 1.2
   */
  static final class Int32FormatOp implements DynamicString {
    private final DynamicProto.Int32FormatOp mImpl;
    @Nullable private final Fingerprint mFingerprint;

    Int32FormatOp(DynamicProto.Int32FormatOp impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the source of Int32 data to convert to a string. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicInt32 getInput() {
      if (mImpl.hasInput()) {
        return DynamicBuilders.dynamicInt32FromProto(mImpl.getInput());
      } else {
        return null;
      }
    }

    /**
     * Gets minimum integer digits. Sign and grouping characters are not considered when applying
     * minIntegerDigits constraint. If not defined, defaults to one. For example,in the English
     * locale, applying minIntegerDigit=4 to 12 would yield "0012". Intended for testing purposes
     * only.
     *
     * @since 1.2
     */
    @IntRange(from = 0)
    public int getMinIntegerDigits() {
      return mImpl.getMinIntegerDigits();
    }

    /**
     * Gets digit grouping used. Grouping size and grouping character depend on the current locale.
     * If not defined, defaults to false. For example, in the English locale, using grouping with
     * 1234 would yield "1,234". Intended for testing purposes only.
     *
     * @since 1.2
     */
    public boolean getGroupingUsed() {
      return mImpl.getGroupingUsed();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static Int32FormatOp fromProto(@NonNull DynamicProto.Int32FormatOp proto) {
      return new Int32FormatOp(proto, null);
    }

    @NonNull
    DynamicProto.Int32FormatOp toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicString toDynamicStringProto() {
      return DynamicProto.DynamicString.newBuilder().setInt32FormatOp(mImpl).build();
    }

    /** Builder for {@link Int32FormatOp}. */
    public static final class Builder implements DynamicString.Builder {
      private final DynamicProto.Int32FormatOp.Builder mImpl =
          DynamicProto.Int32FormatOp.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(196209833);

      public Builder() {}

      /**
       * Sets the source of Int32 data to convert to a string.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInput(@NonNull DynamicInt32 input) {
        mImpl.setInput(input.toDynamicInt32Proto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets minimum integer digits. Sign and grouping characters are not considered when applying
       * minIntegerDigits constraint. If not defined, defaults to one. For example,in the English
       * locale, applying minIntegerDigit=4 to 12 would yield "0012".
       *
       * @since 1.2
       */
      @NonNull
      public Builder setMinIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
        mImpl.setMinIntegerDigits(minIntegerDigits);
        mFingerprint.recordPropertyUpdate(4, minIntegerDigits);
        return this;
      }

      /**
       * Sets digit grouping used. Grouping size and grouping character depend on the current
       * locale. If not defined, defaults to false. For example, in the English locale, using
       * grouping with 1234 would yield "1,234".
       *
       * @since 1.2
       */
      @SuppressLint("MissingGetterMatchingBuilder")
      @NonNull
      public Builder setGroupingUsed(boolean groupingUsed) {
        mImpl.setGroupingUsed(groupingUsed);
        mFingerprint.recordPropertyUpdate(5, Boolean.hashCode(groupingUsed));
        return this;
      }

      @Override
      @NonNull
      public Int32FormatOp build() {
        return new Int32FormatOp(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A dynamic String which sources its data from the tile's state.
   *
   * @since 1.2
   */
  static final class StateStringSource implements DynamicString {
    private final DynamicProto.StateStringSource mImpl;
    @Nullable private final Fingerprint mFingerprint;

    StateStringSource(DynamicProto.StateStringSource impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the key in the state to bind to. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @NonNull
    public String getSourceKey() {
      return mImpl.getSourceKey();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static StateStringSource fromProto(@NonNull DynamicProto.StateStringSource proto) {
      return new StateStringSource(proto, null);
    }

    @NonNull
    DynamicProto.StateStringSource toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicString toDynamicStringProto() {
      return DynamicProto.DynamicString.newBuilder().setStateSource(mImpl).build();
    }

    /** Builder for {@link StateStringSource}. */
    public static final class Builder implements DynamicString.Builder {
      private final DynamicProto.StateStringSource.Builder mImpl =
          DynamicProto.StateStringSource.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1261652090);

      public Builder() {}

      /**
       * Sets the key in the state to bind to.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSourceKey(@NonNull String sourceKey) {
        mImpl.setSourceKey(sourceKey);
        mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
        return this;
      }

      @Override
      @NonNull
      public StateStringSource build() {
        return new StateStringSource(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A conditional operator which yields an string depending on the boolean operand. This implements
   * "string result = condition ? value_if_true : value_if_false".
   *
   * @since 1.2
   */
  static final class ConditionalStringOp implements DynamicString {
    private final DynamicProto.ConditionalStringOp mImpl;
    @Nullable private final Fingerprint mFingerprint;

    ConditionalStringOp(DynamicProto.ConditionalStringOp impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the condition to use. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicBool getCondition() {
      if (mImpl.hasCondition()) {
        return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
      } else {
        return null;
      }
    }

    /**
     * Gets the string to yield if condition is true. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicString getValueIfTrue() {
      if (mImpl.hasValueIfTrue()) {
        return DynamicBuilders.dynamicStringFromProto(mImpl.getValueIfTrue());
      } else {
        return null;
      }
    }

    /**
     * Gets the string to yield if condition is false. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicString getValueIfFalse() {
      if (mImpl.hasValueIfFalse()) {
        return DynamicBuilders.dynamicStringFromProto(mImpl.getValueIfFalse());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static ConditionalStringOp fromProto(@NonNull DynamicProto.ConditionalStringOp proto) {
      return new ConditionalStringOp(proto, null);
    }

    @NonNull
    DynamicProto.ConditionalStringOp toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicString toDynamicStringProto() {
      return DynamicProto.DynamicString.newBuilder().setConditionalOp(mImpl).build();
    }

    /** Builder for {@link ConditionalStringOp}. */
    public static final class Builder implements DynamicString.Builder {
      private final DynamicProto.ConditionalStringOp.Builder mImpl =
          DynamicProto.ConditionalStringOp.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-1535849633);

      public Builder() {}

      /**
       * Sets the condition to use.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setCondition(@NonNull DynamicBool condition) {
        mImpl.setCondition(condition.toDynamicBoolProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the string to yield if condition is true.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setValueIfTrue(@NonNull DynamicString valueIfTrue) {
        mImpl.setValueIfTrue(valueIfTrue.toDynamicStringProto());
        mFingerprint.recordPropertyUpdate(
            2, checkNotNull(valueIfTrue.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the string to yield if condition is false.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setValueIfFalse(@NonNull DynamicString valueIfFalse) {
        mImpl.setValueIfFalse(valueIfFalse.toDynamicStringProto());
        mFingerprint.recordPropertyUpdate(
            3, checkNotNull(valueIfFalse.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public ConditionalStringOp build() {
        return new ConditionalStringOp(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * This implements simple string concatenation "result = LHS+RHS".
   *
   * @since 1.2
   */
  static final class ConcatStringOp implements DynamicString {
    private final DynamicProto.ConcatStringOp mImpl;
    @Nullable private final Fingerprint mFingerprint;

    ConcatStringOp(DynamicProto.ConcatStringOp impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets left hand side of the concatenation operation. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicString getInputLhs() {
      if (mImpl.hasInputLhs()) {
        return DynamicBuilders.dynamicStringFromProto(mImpl.getInputLhs());
      } else {
        return null;
      }
    }

    /**
     * Gets right hand side of the concatenation operation. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicString getInputRhs() {
      if (mImpl.hasInputRhs()) {
        return DynamicBuilders.dynamicStringFromProto(mImpl.getInputRhs());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static ConcatStringOp fromProto(@NonNull DynamicProto.ConcatStringOp proto) {
      return new ConcatStringOp(proto, null);
    }

    @NonNull
    DynamicProto.ConcatStringOp toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicString toDynamicStringProto() {
      return DynamicProto.DynamicString.newBuilder().setConcatOp(mImpl).build();
    }

    /** Builder for {@link ConcatStringOp}. */
    public static final class Builder implements DynamicString.Builder {
      private final DynamicProto.ConcatStringOp.Builder mImpl =
          DynamicProto.ConcatStringOp.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-1516620377);

      public Builder() {}

      /**
       * Sets left hand side of the concatenation operation.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInputLhs(@NonNull DynamicString inputLhs) {
        mImpl.setInputLhs(inputLhs.toDynamicStringProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets right hand side of the concatenation operation.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInputRhs(@NonNull DynamicString inputRhs) {
        mImpl.setInputRhs(inputRhs.toDynamicStringProto());
        mFingerprint.recordPropertyUpdate(
            2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public ConcatStringOp build() {
        return new ConcatStringOp(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Simple formatting for dynamic floats.
   *
   * @since 1.2
   */
  static final class FloatFormatOp implements DynamicString {
    private final DynamicProto.FloatFormatOp mImpl;
    @Nullable private final Fingerprint mFingerprint;

    FloatFormatOp(DynamicProto.FloatFormatOp impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the source of Float data to convert to a string. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicFloat getInput() {
      if (mImpl.hasInput()) {
        return DynamicBuilders.dynamicFloatFromProto(mImpl.getInput());
      } else {
        return null;
      }
    }

    /**
     * Gets maximum fraction digits. Rounding will be applied if maxFractionDigits is smaller than
     * number of fraction digits. If not defined, defaults to three. minimumFractionDigits must be
     * <= maximumFractionDigits. If the condition is not satisfied, then minimumFractionDigits will
     * be used for both fields. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @IntRange(from = 0)
    public int getMaxFractionDigits() {
      return mImpl.getMaxFractionDigits();
    }

    /**
     * Gets minimum fraction digits. Zeros will be appended to the end to satisfy this constraint.
     * If not defined, defaults to zero. minimumFractionDigits must be <= maximumFractionDigits. If
     * the condition is not satisfied, then minimumFractionDigits will be used for both fields.
     * Intended for testing purposes only.
     *
     * @since 1.2
     */
    @IntRange(from = 0)
    public int getMinFractionDigits() {
      return mImpl.getMinFractionDigits();
    }

    /**
     * Gets minimum integer digits. Sign and grouping characters are not considered when applying
     * minIntegerDigits constraint. If not defined, defaults to one. For example, in the English
     * locale, applying minIntegerDigit=4 to 12.34 would yield "0012.34". Intended for testing
     * purposes only.
     *
     * @since 1.2
     */
    @IntRange(from = 0)
    public int getMinIntegerDigits() {
      return mImpl.getMinIntegerDigits();
    }

    /**
     * Gets digit grouping used. Grouping size and grouping character depend on the current locale.
     * If not defined, defaults to false. For example, in the English locale, using grouping with
     * 1234.56 would yield "1,234.56". Intended for testing purposes only.
     *
     * @since 1.2
     */
    public boolean getGroupingUsed() {
      return mImpl.getGroupingUsed();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static FloatFormatOp fromProto(@NonNull DynamicProto.FloatFormatOp proto) {
      return new FloatFormatOp(proto, null);
    }

    @NonNull
    DynamicProto.FloatFormatOp toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicString toDynamicStringProto() {
      return DynamicProto.DynamicString.newBuilder().setFloatFormatOp(mImpl).build();
    }

    /** Builder for {@link FloatFormatOp}. */
    public static final class Builder implements DynamicString.Builder {
      private final DynamicProto.FloatFormatOp.Builder mImpl =
          DynamicProto.FloatFormatOp.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-5150153);

      public Builder() {}

      /**
       * Sets the source of Float data to convert to a string.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInput(@NonNull DynamicFloat input) {
        mImpl.setInput(input.toDynamicFloatProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets maximum fraction digits. Rounding will be applied if maxFractionDigits is smaller than
       * number of fraction digits. If not defined, defaults to three. minimumFractionDigits must be
       * <= maximumFractionDigits. If the condition is not satisfied, then minimumFractionDigits
       * will be used for both fields.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setMaxFractionDigits(@IntRange(from = 0) int maxFractionDigits) {
        mImpl.setMaxFractionDigits(maxFractionDigits);
        mFingerprint.recordPropertyUpdate(2, maxFractionDigits);
        return this;
      }

      /**
       * Sets minimum fraction digits. Zeros will be appended to the end to satisfy this constraint.
       * If not defined, defaults to zero. minimumFractionDigits must be <= maximumFractionDigits.
       * If the condition is not satisfied, then minimumFractionDigits will be used for both fields.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setMinFractionDigits(@IntRange(from = 0) int minFractionDigits) {
        mImpl.setMinFractionDigits(minFractionDigits);
        mFingerprint.recordPropertyUpdate(3, minFractionDigits);
        return this;
      }

      /**
       * Sets minimum integer digits. Sign and grouping characters are not considered when applying
       * minIntegerDigits constraint. If not defined, defaults to one. For example, in the English
       * locale, applying minIntegerDigit=4 to 12.34 would yield "0012.34".
       *
       * @since 1.2
       */
      @NonNull
      public Builder setMinIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
        mImpl.setMinIntegerDigits(minIntegerDigits);
        mFingerprint.recordPropertyUpdate(4, minIntegerDigits);
        return this;
      }

      /**
       * Sets digit grouping used. Grouping size and grouping character depend on the current
       * locale. If not defined, defaults to false. For example, in the English locale, using
       * grouping with 1234.56 would yield "1,234.56".
       *
       * @since 1.2
       */
      @SuppressLint("MissingGetterMatchingBuilder")
      @NonNull
      public Builder setGroupingUsed(boolean groupingUsed) {
        mImpl.setGroupingUsed(groupingUsed);
        mFingerprint.recordPropertyUpdate(5, Boolean.hashCode(groupingUsed));
        return this;
      }

      @Override
      @NonNull
      public FloatFormatOp build() {
        return new FloatFormatOp(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining a dynamic string type.
   *
   * @since 1.2
   */
  public interface DynamicString extends DynamicType {
    /**
     * Get the protocol buffer representation of this object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    DynamicProto.DynamicString toDynamicStringProto();

    /** Creates a constant-valued {@link DynamicString}. */
    @NonNull
    static DynamicString constant(@NonNull String constant) {
      return new FixedString.Builder().setValue(constant).build();
    }

    /**
     * Creates a {@link DynamicString} that is bound to the value of an item of the State.
     *
     * @param stateKey The key to a {@link StateEntryValue} with a string value from the provider's
     *     state.
     */
    @NonNull
    static DynamicString fromState(@NonNull String stateKey) {
      return new StateStringSource.Builder().setSourceKey(stateKey).build();
    }

    /**
     * Creates a {@link DynamicString} that is bound to the result of a conditional expression. It
     * will use the value given in either {@link ConditionScope#use} or {@link
     * ConditionScopes.IfTrueScope#elseUse} depending on the value yielded from {@code condition}.
     *
     * @param condition The value used for evaluting this condition.
     */
    @NonNull
    static ConditionScope<DynamicString, String> onCondition(@NonNull DynamicBool condition) {
      return new ConditionScopes.ConditionScope<>(
          (trueValue, falseValue) ->
              new ConditionalStringOp.Builder()
                  .setCondition(condition)
                  .setValueIfTrue(trueValue)
                  .setValueIfFalse(falseValue)
                  .build(),
          DynamicString::constant);
    }

    /**
     * Returns a new {@link DynamicString} that has the result of concatenating this {@link
     * DynamicString} with {@code other}. i.e. {@code result = this + other}
     *
     * @param other The right hand side operand of the concatenation.
     */
    @NonNull
    default DynamicString concat(@NonNull DynamicString other) {
      return new DynamicBuilders.ConcatStringOp.Builder()
          .setInputLhs(this)
          .setInputRhs(other)
          .build();
    }

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link DynamicString} objects.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      DynamicString build();
    }
  }

  /**
   * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
   * created using this method can't be added to any other wrapper.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  @NonNull
  public static DynamicString dynamicStringFromProto(@NonNull DynamicProto.DynamicString proto) {
    if (proto.hasFixed()) {
      return FixedString.fromProto(proto.getFixed());
    }
    if (proto.hasInt32FormatOp()) {
      return Int32FormatOp.fromProto(proto.getInt32FormatOp());
    }
    if (proto.hasStateSource()) {
      return StateStringSource.fromProto(proto.getStateSource());
    }
    if (proto.hasConditionalOp()) {
      return ConditionalStringOp.fromProto(proto.getConditionalOp());
    }
    if (proto.hasConcatOp()) {
      return ConcatStringOp.fromProto(proto.getConcatOp());
    }
    if (proto.hasFloatFormatOp()) {
      return FloatFormatOp.fromProto(proto.getFloatFormatOp());
    }
    throw new IllegalStateException("Proto was not a recognised instance of DynamicString");
  }

  /**
   * A dynamic Float which sources its data from the tile's state.
   *
   * @since 1.2
   */
  static final class StateFloatSource implements DynamicFloat {
    private final DynamicProto.StateFloatSource mImpl;
    @Nullable private final Fingerprint mFingerprint;

    StateFloatSource(DynamicProto.StateFloatSource impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the key in the state to bind to. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @NonNull
    public String getSourceKey() {
      return mImpl.getSourceKey();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static StateFloatSource fromProto(@NonNull DynamicProto.StateFloatSource proto) {
      return new StateFloatSource(proto, null);
    }

    @NonNull
    DynamicProto.StateFloatSource toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicFloat toDynamicFloatProto() {
      return DynamicProto.DynamicFloat.newBuilder().setStateSource(mImpl).build();
    }

    /** Builder for {@link StateFloatSource}. */
    public static final class Builder implements DynamicFloat.Builder {
      private final DynamicProto.StateFloatSource.Builder mImpl =
          DynamicProto.StateFloatSource.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(384370154);

      public Builder() {}

      /**
       * Sets the key in the state to bind to.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSourceKey(@NonNull String sourceKey) {
        mImpl.setSourceKey(sourceKey);
        mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
        return this;
      }

      @Override
      @NonNull
      public StateFloatSource build() {
        return new StateFloatSource(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * An operation to convert a Int32 value in the dynamic data pipeline to a Float value.
   *
   * @since 1.2
   */
  static final class Int32ToFloatOp implements DynamicFloat {
    private final DynamicProto.Int32ToFloatOp mImpl;
    @Nullable private final Fingerprint mFingerprint;

    Int32ToFloatOp(DynamicProto.Int32ToFloatOp impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the input Int32 to convert to a Float. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicInt32 getInput() {
      if (mImpl.hasInput()) {
        return DynamicBuilders.dynamicInt32FromProto(mImpl.getInput());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static Int32ToFloatOp fromProto(@NonNull DynamicProto.Int32ToFloatOp proto) {
      return new Int32ToFloatOp(proto, null);
    }

    @NonNull
    DynamicProto.Int32ToFloatOp toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicFloat toDynamicFloatProto() {
      return DynamicProto.DynamicFloat.newBuilder().setInt32ToFloatOperation(mImpl).build();
    }

    /** Builder for {@link Int32ToFloatOp}. */
    public static final class Builder implements DynamicFloat.Builder {
      private final DynamicProto.Int32ToFloatOp.Builder mImpl =
          DynamicProto.Int32ToFloatOp.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-619592745);

      public Builder() {}

      /**
       * Sets the input Int32 to convert to a Float.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInput(@NonNull DynamicInt32 input) {
        mImpl.setInput(input.toDynamicInt32Proto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public Int32ToFloatOp build() {
        return new Int32ToFloatOp(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A static interpolation, between two fixed floating point values.
   *
   * @since 1.2
   */
  static final class AnimatableFixedFloat implements DynamicFloat {
    private final DynamicProto.AnimatableFixedFloat mImpl;
    @Nullable private final Fingerprint mFingerprint;

    AnimatableFixedFloat(
        DynamicProto.AnimatableFixedFloat impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the number to start animating from. Intended for testing purposes only.
     *
     * @since 1.2
     */
    public float getFromValue() {
      return mImpl.getFromValue();
    }

    /**
     * Gets the number to animate to. Intended for testing purposes only.
     *
     * @since 1.2
     */
    public float getToValue() {
      return mImpl.getToValue();
    }

    /**
     * Gets the animation parameters for duration, delay, etc. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public AnimationSpec getSpec() {
      if (mImpl.hasSpec()) {
        return AnimationSpec.fromProto(mImpl.getSpec());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static AnimatableFixedFloat fromProto(@NonNull DynamicProto.AnimatableFixedFloat proto) {
      return new AnimatableFixedFloat(proto, null);
    }

    @NonNull
    DynamicProto.AnimatableFixedFloat toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicFloat toDynamicFloatProto() {
      return DynamicProto.DynamicFloat.newBuilder().setAnimatableFixed(mImpl).build();
    }

    /** Builder for {@link AnimatableFixedFloat}. */
    public static final class Builder implements DynamicFloat.Builder {
      private final DynamicProto.AnimatableFixedFloat.Builder mImpl =
          DynamicProto.AnimatableFixedFloat.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1964707538);

      public Builder() {}

      /**
       * Sets the number to start animating from.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setFromValue(float fromValue) {
        mImpl.setFromValue(fromValue);
        mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(fromValue));
        return this;
      }

      /**
       * Sets the number to animate to.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setToValue(float toValue) {
        mImpl.setToValue(toValue);
        mFingerprint.recordPropertyUpdate(2, Float.floatToIntBits(toValue));
        return this;
      }

      /**
       * Sets the animation parameters for duration, delay, etc.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSpec(@NonNull AnimationSpec spec) {
        mImpl.setSpec(spec.toProto());
        mFingerprint.recordPropertyUpdate(
            3, checkNotNull(spec.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public AnimatableFixedFloat build() {
        return new AnimatableFixedFloat(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A dynamic interpolation node. This will watch the value of its input and, when the first update
   * arrives, immediately emit that value. On subsequent updates, it will animate between the old
   * and new values.
   *
   * <p>If this node receives an invalid value (e.g. as a result of an upstream node having no
   * value), then it will emit a single invalid value, and forget its "stored" value. The next valid
   * value that arrives is then used as the "first" value again.
   *
   * @since 1.2
   */
  static final class AnimatableDynamicFloat implements DynamicFloat {
    private final DynamicProto.AnimatableDynamicFloat mImpl;
    @Nullable private final Fingerprint mFingerprint;

    AnimatableDynamicFloat(
        DynamicProto.AnimatableDynamicFloat impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the value to watch, and animate when it changes. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicFloat getInput() {
      if (mImpl.hasInput()) {
        return DynamicBuilders.dynamicFloatFromProto(mImpl.getInput());
      } else {
        return null;
      }
    }

    /**
     * Gets the animation parameters for duration, delay, etc. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public AnimationSpec getSpec() {
      if (mImpl.hasSpec()) {
        return AnimationSpec.fromProto(mImpl.getSpec());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static AnimatableDynamicFloat fromProto(@NonNull DynamicProto.AnimatableDynamicFloat proto) {
      return new AnimatableDynamicFloat(proto, null);
    }

    @NonNull
    DynamicProto.AnimatableDynamicFloat toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicFloat toDynamicFloatProto() {
      return DynamicProto.DynamicFloat.newBuilder().setAnimatableDynamic(mImpl).build();
    }

    /** Builder for {@link AnimatableDynamicFloat}. */
    public static final class Builder implements DynamicFloat.Builder {
      private final DynamicProto.AnimatableDynamicFloat.Builder mImpl =
          DynamicProto.AnimatableDynamicFloat.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1543182280);

      public Builder() {}

      /**
       * Sets the value to watch, and animate when it changes.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInput(@NonNull DynamicFloat input) {
        mImpl.setInput(input.toDynamicFloatProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the animation parameters for duration, delay, etc.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSpec(@NonNull AnimationSpec spec) {
        mImpl.setSpec(spec.toProto());
        mFingerprint.recordPropertyUpdate(
            3, checkNotNull(spec.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public AnimatableDynamicFloat build() {
        return new AnimatableDynamicFloat(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining a dynamic float type.
   *
   * @since 1.2
   */
  public interface DynamicFloat extends DynamicType {
    /**
     * Get the protocol buffer representation of this object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    DynamicProto.DynamicFloat toDynamicFloatProto();

    /** Creates a constant-valued {@link DynamicFloat}. */
    @NonNull
    static DynamicFloat constant(float constant) {
      return new FixedFloat.Builder().setValue(constant).build();
    }

    /**
     * Creates a {@link DynamicFloat} that is bound to the value of an item of the State.
     *
     * @param stateKey The key to a {@link StateEntryValue} with a float value from the provider's
     *     state.
     */
    @NonNull
    static DynamicFloat fromState(@NonNull String stateKey) {
      return new StateFloatSource.Builder().setSourceKey(stateKey).build();
    }

    /**
     * Creates a {@link DynamicFloat} which will animate over the range of floats from {@code start}
     * to {@code end}.
     *
     * @param start The start value of the range.
     * @param end The end value of the range.
     */
    @NonNull
    static DynamicFloat animate(float start, float end) {
      return new AnimatableFixedFloat.Builder().setFromValue(start).setToValue(end).build();
    }

    /**
     * Creates a {@link DynamicFloat} which will animate over the range of floats from {@code start}
     * to {@code end} with the given animation parameters.
     *
     * @param start The start value of the range.
     * @param end The end value of the range.
     * @param spec The animation parameters.
     */
    @NonNull
    static DynamicFloat animate(float start, float end, @NonNull AnimationSpec spec) {
      return new AnimatableFixedFloat.Builder()
          .setFromValue(start)
          .setToValue(end)
          .setSpec(spec)
          .build();
    }

    /**
     * Creates a {@link DynamicFloat} that is bound to the value of an item of the State. Every time
     * the state value changes, this {@link DynamicFloat} will animate from its current value to the
     * new value (from the state).
     *
     * @param stateKey The key to a {@link StateEntryValue} with a float value from the providers
     *     state.
     */
    @NonNull
    static DynamicFloat animate(@NonNull String stateKey) {
      return new AnimatableDynamicFloat.Builder().setInput(fromState(stateKey)).build();
    }

    /**
     * Creates a {@link DynamicFloat} that is bound to the value of an item of the State. Every time
     * the state value changes, this {@link DynamicFloat} will animate from its current value to the
     * new value (from the state).
     *
     * @param stateKey The key to a {@link StateEntryValue} with a float value from the providers
     *     state.
     * @param spec The animation parameters.
     */
    @NonNull
    static DynamicFloat animate(@NonNull String stateKey, @NonNull AnimationSpec spec) {
      return new AnimatableDynamicFloat.Builder()
          .setInput(fromState(stateKey))
          .setSpec(spec)
          .build();
    }

    /**
     * Returns a {@link DynamicFloat} that is bound to the value of this {@link DynamicFloat} and
     * every time its value is changing, it animates from its current value to the new value.
     *
     * @param spec The animation parameters.
     */
    @NonNull
    default DynamicFloat animate(@NonNull AnimationSpec spec) {
      return new AnimatableDynamicFloat.Builder().setInput(this).setSpec(spec).build();
    }

    /**
     * Returns a {@link DynamicFloat} that is bound to the value of this {@link DynamicFloat} and
     * every time its value is changing, it animates from its current value to the new value.
     */
    @NonNull
    default DynamicFloat animate() {
      return new AnimatableDynamicFloat.Builder().setInput(this).build();
    }

    /**
     * Returns a {@link DynamicInt32} which holds the largest integer value that is smaller than or
     * equal to this {@link DynamicFloat}, i.e. {@code int result = (int) Math.floor(this)}
     */
    @NonNull
    default DynamicInt32 asInt() {
      return new FloatToInt32Op.Builder()
          .setRoundMode(DynamicBuilders.ROUND_MODE_FLOOR)
          .setInput(this)
          .build();
    }

    /**
     * Returns a {@link DynamicString} that contains the formatted value of this {@link
     * DynamicFloat} (with default formatting parameters). As an example, in the English locale, the
     * following is equal to {@code DynamicString.constant("12.346")}
     *
     * <pre>
     *   DynamicFloat.constant(12.34567f).format();
     * </pre>
     */
    @NonNull
    default DynamicString format() {
      return FloatFormatter.with().buildForInput(this);
    }

    /**
     * Returns a {@link DynamicString} that contains the formatted value of this {@link
     * DynamicFloat}. As an example, in the English locale, the following is equal to {@code
     * DynamicString.constant("0,012.34")}
     *
     * <pre>
     *   DynamicFloat.constant(12.345f)
     *       .format(
     *           FloatFormatter.with().maxFractionDigits(2).minIntegerDigits(4).groupingUsed(true));
     * </pre>
     *
     * @param formatter The formatting parameter.
     */
    @NonNull
    default DynamicString format(@NonNull FloatFormatter formatter) {
      return formatter.buildForInput(this);
    }

    /** Allows formatting {@link DynamicFloat} into a {@link DynamicString}. */
    class FloatFormatter {
      private final FloatFormatOp.Builder builder;

      private FloatFormatter() {
        builder = new FloatFormatOp.Builder();
      }

      /** Creates an instance of {@link FloatFormatter} with default configuration. */
      @NonNull
      public static FloatFormatter with() {
        return new FloatFormatter();
      }

      /**
       * Sets minimum number of fraction digits for the formatter. Defaults to zero if not
       * specified. minimumFractionDigits must be <= maximumFractionDigits. If the condition is not
       * satisfied, then minimumFractionDigits will be used for both fields.
       */
      @NonNull
      public FloatFormatter minFractionDigits(@IntRange(from = 0) int minFractionDigits) {
        builder.setMinFractionDigits(minFractionDigits);
        return this;
      }

      /**
       * Sets maximum number of fraction digits for the formatter. Defaults to three if not
       * specified. minimumFractionDigits must be <= maximumFractionDigits. If the condition is not
       * satisfied, then minimumFractionDigits will be used for both fields.
       */
      @NonNull
      public FloatFormatter maxFractionDigits(@IntRange(from = 0) int maxFractionDigits) {
        builder.setMaxFractionDigits(maxFractionDigits);
        return this;
      }

      /**
       * Sets minimum number of integer digits for the formatter. Defaults to one if not specified.
       */
      @NonNull
      public FloatFormatter minIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
        builder.setMinIntegerDigits(minIntegerDigits);
        return this;
      }

      /** Sets whether grouping is used for the formatter. Defaults to false if not specified. */
      @NonNull
      public FloatFormatter groupingUsed(boolean groupingUsed) {
        builder.setGroupingUsed(groupingUsed);
        return this;
      }

      @NonNull
      FloatFormatOp buildForInput(@NonNull DynamicFloat dynamicFloat) {
        return builder.setInput(dynamicFloat).build();
      }
    }

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link DynamicFloat} objects.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      DynamicFloat build();
    }
  }

  /**
   * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
   * created using this method can't be added to any other wrapper.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  @NonNull
  public static DynamicFloat dynamicFloatFromProto(@NonNull DynamicProto.DynamicFloat proto) {
    if (proto.hasFixed()) {
      return FixedFloat.fromProto(proto.getFixed());
    }
    if (proto.hasInt32ToFloatOperation()) {
      return Int32ToFloatOp.fromProto(proto.getInt32ToFloatOperation());
    }
    if (proto.hasStateSource()) {
      return StateFloatSource.fromProto(proto.getStateSource());
    }
    if (proto.hasAnimatableFixed()) {
      return AnimatableFixedFloat.fromProto(proto.getAnimatableFixed());
    }
    if (proto.hasAnimatableDynamic()) {
      return AnimatableDynamicFloat.fromProto(proto.getAnimatableDynamic());
    }
    throw new IllegalStateException("Proto was not a recognised instance of DynamicFloat");
  }

  /**
   * A dynamic boolean type which sources its data from the tile's state.
   *
   * @since 1.2
   */
  static final class StateBoolSource implements DynamicBool {
    private final DynamicProto.StateBoolSource mImpl;
    @Nullable private final Fingerprint mFingerprint;

    StateBoolSource(DynamicProto.StateBoolSource impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the key in the state to bind to. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @NonNull
    public String getSourceKey() {
      return mImpl.getSourceKey();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static StateBoolSource fromProto(@NonNull DynamicProto.StateBoolSource proto) {
      return new StateBoolSource(proto, null);
    }

    @NonNull
    DynamicProto.StateBoolSource toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicBool toDynamicBoolProto() {
      return DynamicProto.DynamicBool.newBuilder().setStateSource(mImpl).build();
    }

    /** Builder for {@link StateBoolSource}. */
    public static final class Builder implements DynamicBool.Builder {
      private final DynamicProto.StateBoolSource.Builder mImpl =
          DynamicProto.StateBoolSource.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1818702779);

      public Builder() {}

      /**
       * Sets the key in the state to bind to.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSourceKey(@NonNull String sourceKey) {
        mImpl.setSourceKey(sourceKey);
        mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
        return this;
      }

      @Override
      @NonNull
      public StateBoolSource build() {
        return new StateBoolSource(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A boolean operation which implements a "NOT" operator, i.e. "boolean result = !input".
   *
   * @since 1.2
   */
  static final class NotBoolOp implements DynamicBool {
    private final DynamicProto.NotBoolOp mImpl;
    @Nullable private final Fingerprint mFingerprint;

    NotBoolOp(DynamicProto.NotBoolOp impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the input, whose value to negate. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicBool getInput() {
      if (mImpl.hasInput()) {
        return DynamicBuilders.dynamicBoolFromProto(mImpl.getInput());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static NotBoolOp fromProto(@NonNull DynamicProto.NotBoolOp proto) {
      return new NotBoolOp(proto, null);
    }

    @NonNull
    DynamicProto.NotBoolOp toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicBool toDynamicBoolProto() {
      return DynamicProto.DynamicBool.newBuilder().setNotOp(mImpl).build();
    }

    /** Builder for {@link NotBoolOp}. */
    public static final class Builder implements DynamicBool.Builder {
      private final DynamicProto.NotBoolOp.Builder mImpl = DynamicProto.NotBoolOp.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(91300638);

      public Builder() {}

      /**
       * Sets the input, whose value to negate.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInput(@NonNull DynamicBool input) {
        mImpl.setInput(input.toDynamicBoolProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public NotBoolOp build() {
        return new NotBoolOp(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A logical boolean operator, implementing "boolean result = LHS <op> RHS", for various boolean
   * operators (i.e. AND/OR).
   *
   * @since 1.2
   */
  static final class LogicalBoolOp implements DynamicBool {
    private final DynamicProto.LogicalBoolOp mImpl;
    @Nullable private final Fingerprint mFingerprint;

    LogicalBoolOp(DynamicProto.LogicalBoolOp impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the left hand side of the logical operation. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicBool getInputLhs() {
      if (mImpl.hasInputLhs()) {
        return DynamicBuilders.dynamicBoolFromProto(mImpl.getInputLhs());
      } else {
        return null;
      }
    }

    /**
     * Gets the right hand side of the logical operation. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicBool getInputRhs() {
      if (mImpl.hasInputRhs()) {
        return DynamicBuilders.dynamicBoolFromProto(mImpl.getInputRhs());
      } else {
        return null;
      }
    }

    /**
     * Gets the operation type to apply to LHS/RHS. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @LogicalOpType
    public int getOperationType() {
      return mImpl.getOperationType().getNumber();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static LogicalBoolOp fromProto(@NonNull DynamicProto.LogicalBoolOp proto) {
      return new LogicalBoolOp(proto, null);
    }

    @NonNull
    DynamicProto.LogicalBoolOp toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicBool toDynamicBoolProto() {
      return DynamicProto.DynamicBool.newBuilder().setLogicalOp(mImpl).build();
    }

    /** Builder for {@link LogicalBoolOp}. */
    public static final class Builder implements DynamicBool.Builder {
      private final DynamicProto.LogicalBoolOp.Builder mImpl =
          DynamicProto.LogicalBoolOp.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1067523409);

      public Builder() {}

      /**
       * Sets the left hand side of the logical operation.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInputLhs(@NonNull DynamicBool inputLhs) {
        mImpl.setInputLhs(inputLhs.toDynamicBoolProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the right hand side of the logical operation.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInputRhs(@NonNull DynamicBool inputRhs) {
        mImpl.setInputRhs(inputRhs.toDynamicBoolProto());
        mFingerprint.recordPropertyUpdate(
            2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the operation type to apply to LHS/RHS.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setOperationType(@LogicalOpType int operationType) {
        mImpl.setOperationType(DynamicProto.LogicalOpType.forNumber(operationType));
        mFingerprint.recordPropertyUpdate(3, operationType);
        return this;
      }

      @Override
      @NonNull
      public LogicalBoolOp build() {
        return new LogicalBoolOp(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining a dynamic boolean type.
   *
   * @since 1.2
   */
  public interface DynamicBool extends DynamicType {
    /**
     * Get the protocol buffer representation of this object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    DynamicProto.DynamicBool toDynamicBoolProto();

    /** Creates a constant-valued {@link DynamicBool}. */
    @NonNull
    static DynamicBool constant(boolean constant) {
      return new FixedBool.Builder().setValue(constant).build();
    }

    /**
     * Creates a {@link DynamicBool} that is bound to the value of an item of the State.
     *
     * @param stateKey The key to a {@link StateEntryValue} with a boolean value from the provider's
     *     state.
     */
    @NonNull
    static DynamicBool fromState(@NonNull String stateKey) {
      return new StateBoolSource.Builder().setSourceKey(stateKey).build();
    }

    /** Returns a {@link DynamicBool} that has the same value as this {@link DynamicBool}. */
    @NonNull
    default DynamicBool isTrue() {
      return this;
    }

    /**
     * Returns a {@link DynamicBool} that has the opposite value of this {@link DynamicBool}. i.e.
     * {code result = !this}
     */
    @NonNull
    default DynamicBool isFalse() {
      return new NotBoolOp.Builder().setInput(this).build();
    }

    /**
     * Returns a {@link DynamicBool} that is true if this {@link DynamicBool} and {@code input} are
     * both true, otherwise it is false. i.e. {@code boolean result = this && input}
     *
     * @param input The right hand operand of the "and" operation.
     */
    @NonNull
    default DynamicBool and(@NonNull DynamicBool input) {
      return new LogicalBoolOp.Builder()
          .setInputLhs(this)
          .setInputRhs(input)
          .setOperationType(DynamicBuilders.LOGICAL_OP_TYPE_AND)
          .build();
    }

    /**
     * Returns a {@link DynamicBool} that is true if this {@link DynamicBool} or {@code input} are
     * true, otherwise it is false. i.e. {@code boolean result = this || input}
     *
     * @param input The right hand operand of the "or" operation.
     */
    @NonNull
    default DynamicBool or(@NonNull DynamicBool input) {
      return new LogicalBoolOp.Builder()
          .setInputLhs(this)
          .setInputRhs(input)
          .setOperationType(DynamicBuilders.LOGICAL_OP_TYPE_OR)
          .build();
    }

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link DynamicBool} objects.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      DynamicBool build();
    }
  }

  /**
   * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
   * created using this method can't be added to any other wrapper.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  @NonNull
  public static DynamicBool dynamicBoolFromProto(@NonNull DynamicProto.DynamicBool proto) {
    if (proto.hasFixed()) {
      return FixedBool.fromProto(proto.getFixed());
    }
    if (proto.hasStateSource()) {
      return StateBoolSource.fromProto(proto.getStateSource());
    }
    if (proto.hasNotOp()) {
      return NotBoolOp.fromProto(proto.getNotOp());
    }
    if (proto.hasLogicalOp()) {
      return LogicalBoolOp.fromProto(proto.getLogicalOp());
    }
    throw new IllegalStateException("Proto was not a recognised instance of DynamicBool");
  }

  /**
   * A dynamic Color which sources its data from the tile's state.
   *
   * @since 1.2
   */
  static final class StateColorSource implements DynamicColor {
    private final DynamicProto.StateColorSource mImpl;
    @Nullable private final Fingerprint mFingerprint;

    StateColorSource(DynamicProto.StateColorSource impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the key in the state to bind to. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @NonNull
    public String getSourceKey() {
      return mImpl.getSourceKey();
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static StateColorSource fromProto(@NonNull DynamicProto.StateColorSource proto) {
      return new StateColorSource(proto, null);
    }

    @NonNull
    DynamicProto.StateColorSource toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicColor toDynamicColorProto() {
      return DynamicProto.DynamicColor.newBuilder().setStateSource(mImpl).build();
    }

    /** Builder for {@link StateColorSource}. */
    public static final class Builder implements DynamicColor.Builder {
      private final DynamicProto.StateColorSource.Builder mImpl =
          DynamicProto.StateColorSource.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1981221690);

      public Builder() {}

      /**
       * Sets the key in the state to bind to.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSourceKey(@NonNull String sourceKey) {
        mImpl.setSourceKey(sourceKey);
        mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
        return this;
      }

      @Override
      @NonNull
      public StateColorSource build() {
        return new StateColorSource(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A static interpolation, between two fixed color values.
   *
   * @since 1.2
   */
  static final class AnimatableFixedColor implements DynamicColor {
    private final DynamicProto.AnimatableFixedColor mImpl;
    @Nullable private final Fingerprint mFingerprint;

    AnimatableFixedColor(
        DynamicProto.AnimatableFixedColor impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the color value (in ARGB format) to start animating from. Intended for testing purposes
     * only.
     *
     * @since 1.2
     */
    @ColorInt
    public int getFromArgb() {
      return mImpl.getFromArgb();
    }

    /**
     * Gets the color value (in ARGB format) to animate to. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @ColorInt
    public int getToArgb() {
      return mImpl.getToArgb();
    }

    /**
     * Gets the animation parameters for duration, delay, etc. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public AnimationSpec getSpec() {
      if (mImpl.hasSpec()) {
        return AnimationSpec.fromProto(mImpl.getSpec());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static AnimatableFixedColor fromProto(@NonNull DynamicProto.AnimatableFixedColor proto) {
      return new AnimatableFixedColor(proto, null);
    }

    @NonNull
    DynamicProto.AnimatableFixedColor toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicColor toDynamicColorProto() {
      return DynamicProto.DynamicColor.newBuilder().setAnimatableFixed(mImpl).build();
    }

    /** Builder for {@link AnimatableFixedColor}. */
    public static final class Builder implements DynamicColor.Builder {
      private final DynamicProto.AnimatableFixedColor.Builder mImpl =
          DynamicProto.AnimatableFixedColor.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(2051778294);

      public Builder() {}

      /**
       * Sets the color value (in ARGB format) to start animating from.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setFromArgb(@ColorInt int fromArgb) {
        mImpl.setFromArgb(fromArgb);
        mFingerprint.recordPropertyUpdate(1, fromArgb);
        return this;
      }

      /**
       * Sets the color value (in ARGB format) to animate to.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setToArgb(@ColorInt int toArgb) {
        mImpl.setToArgb(toArgb);
        mFingerprint.recordPropertyUpdate(2, toArgb);
        return this;
      }

      /**
       * Sets the animation parameters for duration, delay, etc.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSpec(@NonNull AnimationSpec spec) {
        mImpl.setSpec(spec.toProto());
        mFingerprint.recordPropertyUpdate(
            3, checkNotNull(spec.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public AnimatableFixedColor build() {
        return new AnimatableFixedColor(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * A dynamic interpolation node. This will watch the value of its input and, when the first update
   * arrives, immediately emit that value. On subsequent updates, it will animate between the old
   * and new values.
   *
   * <p>If this node receives an invalid value (e.g. as a result of an upstream node having no
   * value), then it will emit a single invalid value, and forget its "stored" value. The next valid
   * value that arrives is then used as the "first" value again.
   *
   * @since 1.2
   */
  static final class AnimatableDynamicColor implements DynamicColor {
    private final DynamicProto.AnimatableDynamicColor mImpl;
    @Nullable private final Fingerprint mFingerprint;

    AnimatableDynamicColor(
        DynamicProto.AnimatableDynamicColor impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the value to watch, and animate when it changes. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public DynamicColor getInput() {
      if (mImpl.hasInput()) {
        return DynamicBuilders.dynamicColorFromProto(mImpl.getInput());
      } else {
        return null;
      }
    }

    /**
     * Gets the animation parameters for duration, delay, etc. Intended for testing purposes only.
     *
     * @since 1.2
     */
    @Nullable
    public AnimationSpec getSpec() {
      if (mImpl.hasSpec()) {
        return AnimationSpec.fromProto(mImpl.getSpec());
      } else {
        return null;
      }
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static AnimatableDynamicColor fromProto(@NonNull DynamicProto.AnimatableDynamicColor proto) {
      return new AnimatableDynamicColor(proto, null);
    }

    @NonNull
    DynamicProto.AnimatableDynamicColor toProto() {
      return mImpl;
    }

    /** @hide */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public DynamicProto.DynamicColor toDynamicColorProto() {
      return DynamicProto.DynamicColor.newBuilder().setAnimatableDynamic(mImpl).build();
    }

    /** Builder for {@link AnimatableDynamicColor}. */
    public static final class Builder implements DynamicColor.Builder {
      private final DynamicProto.AnimatableDynamicColor.Builder mImpl =
          DynamicProto.AnimatableDynamicColor.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-193597422);

      public Builder() {}

      /**
       * Sets the value to watch, and animate when it changes.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setInput(@NonNull DynamicColor input) {
        mImpl.setInput(input.toDynamicColorProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      /**
       * Sets the animation parameters for duration, delay, etc.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setSpec(@NonNull AnimationSpec spec) {
        mImpl.setSpec(spec.toProto());
        mFingerprint.recordPropertyUpdate(
            3, checkNotNull(spec.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public AnimatableDynamicColor build() {
        return new AnimatableDynamicColor(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining a dynamic color type.
   *
   * @since 1.2
   */
  public interface DynamicColor extends DynamicType {
    /**
     * Get the protocol buffer representation of this object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    DynamicProto.DynamicColor toDynamicColorProto();

    /** Creates a constant-valued {@link DynamicColor}. */
    @NonNull
    static DynamicColor constant(@ColorInt int constant) {
      return new FixedColor.Builder().setArgb(constant).build();
    }

    /**
     * Creates a {@link DynamicColor} that is bound to the value of an item of the State.
     *
     * @param stateKey The key to a {@link StateEntryValue} with a color value from the provider's
     *     state.
     */
    @NonNull
    static DynamicColor fromState(@NonNull String stateKey) {
      return new StateColorSource.Builder().setSourceKey(stateKey).build();
    }

    /**
     * Creates a {@link DynamicColor} which will animate over the range of colors from {@code start}
     * to {@code end}.
     *
     * @param start The start value of the range.
     * @param end The end value of the range.
     */
    @NonNull
    static DynamicColor animate(@ColorInt int start, @ColorInt int end) {
      return new AnimatableFixedColor.Builder().setFromArgb(start).setToArgb(end).build();
    }

    /**
     * Creates a {@link DynamicColor} which will animate over the range of colors from {@code start}
     * to {@code end} with the given animation parameters.
     *
     * @param start The start value of the range.
     * @param end The end value of the range.
     * @param spec The animation parameters.
     */
    @NonNull
    static DynamicColor animate(
        @ColorInt int start, @ColorInt int end, @NonNull AnimationSpec spec) {
      return new AnimatableFixedColor.Builder()
          .setFromArgb(start)
          .setToArgb(end)
          .setSpec(spec)
          .build();
    }

    /**
     * Creates a {@link DynamicColor} that is bound to the value of an item of the State. Every time
     * the state value changes, this {@link DynamicColor} will animate from its current value to the
     * new value (from the state).
     *
     * @param stateKey The key to a {@link StateEntryValue} with a color value from the providers
     *     state.
     */
    @NonNull
    static DynamicColor animate(@NonNull String stateKey) {
      return new AnimatableDynamicColor.Builder().setInput(fromState(stateKey)).build();
    }

    /**
     * Creates a {@link DynamicColor} that is bound to the value of an item of the State. Every time
     * the state value changes, this {@link DynamicColor} will animate from its current value to the
     * new value (from the state).
     *
     * @param stateKey The key to a {@link StateEntryValue} with a color value from the providers
     *     state.
     * @param spec The animation parameters.
     */
    @NonNull
    static DynamicColor animate(@NonNull String stateKey, @NonNull AnimationSpec spec) {
      return new AnimatableDynamicColor.Builder()
          .setInput(fromState(stateKey))
          .setSpec(spec)
          .build();
    }

    /**
     * Returns a {@link DynamicColor} that is bound to the value of this {@link DynamicColor} and
     * every time its value is changing, it animates from its current value to the new value.
     *
     * @param spec The animation parameters.
     */
    @NonNull
    default DynamicColor animate(@NonNull AnimationSpec spec) {
      return new AnimatableDynamicColor.Builder().setInput(this).setSpec(spec).build();
    }

    /**
     * Returns a {@link DynamicColor} that is bound to the value of this {@link DynamicColor} and
     * every time its value is changing, it animates from its current value to the new value.
     */
    @NonNull
    default DynamicColor animate() {
      return new AnimatableDynamicColor.Builder().setInput(this).build();
    }

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link DynamicColor} objects.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      DynamicColor build();
    }
  }

  /**
   * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
   * created using this method can't be added to any other wrapper.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  @NonNull
  public static DynamicColor dynamicColorFromProto(@NonNull DynamicProto.DynamicColor proto) {
    if (proto.hasFixed()) {
      return FixedColor.fromProto(proto.getFixed());
    }
    if (proto.hasStateSource()) {
      return StateColorSource.fromProto(proto.getStateSource());
    }
    if (proto.hasAnimatableFixed()) {
      return AnimatableFixedColor.fromProto(proto.getAnimatableFixed());
    }
    if (proto.hasAnimatableDynamic()) {
      return AnimatableDynamicColor.fromProto(proto.getAnimatableDynamic());
    }
    throw new IllegalStateException("Proto was not a recognised instance of DynamicColor");
  }

  /**
   * Interface to be used as a base type for all other dynamic types. This is not consumed by any
   * Tile elements, it exists just as a marker interface for use internally in the Tiles library.
   */
  public interface DynamicType {}
}
