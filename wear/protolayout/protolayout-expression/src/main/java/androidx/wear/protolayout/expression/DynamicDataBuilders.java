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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedBool;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedColor;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedFloat;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedInt32;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedString;
import androidx.wear.protolayout.expression.proto.DynamicDataProto;

/** Builders for dynamic data value of a provider. */
public final class DynamicDataBuilders {
  private DynamicDataBuilders() {}

  /**
   * Interface defining a dynamic data value.
   *
   * @since 1.2
   */
  public interface DynamicDataValue {
    /**
     * Get the protocol buffer representation of this object.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    DynamicDataProto.DynamicDataValue toDynamicDataValueProto();

    /** Creates a boolean {@link DynamicDataValue}. */
    @NonNull
    static DynamicDataValue fromBool(boolean constant) {
      return new FixedBool.Builder().setValue(constant).build();
    }

    /** Creates a int {@link DynamicDataValue}. */
    @NonNull
    static DynamicDataValue fromInt(int constant) {
      return new FixedInt32.Builder().setValue(constant).build();
    }

    /** Creates a float {@link DynamicDataValue}. */
    @NonNull
    static DynamicDataValue fromFloat(float constant) {
      return new FixedFloat.Builder().setValue(constant).build();
    }

    /** Creates a color {@link DynamicDataValue}. */
    @NonNull
    static DynamicDataValue fromColor(@ColorInt int constant) {
      return new FixedColor.Builder().setArgb(constant).build();
    }

    /** Creates a string {@link DynamicDataValue}. */
    @NonNull
    static DynamicDataValue fromString(@NonNull String constant) {
      return new FixedString.Builder().setValue(constant).build();
    }

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link DynamicDataValue} objects.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      DynamicDataValue build();
    }
  }

  /**
   * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
   * created using this method can't be added to any other wrapper.
   *
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  @NonNull
  public static DynamicDataValue dynamicDataValueFromProto(
      @NonNull DynamicDataProto.DynamicDataValue proto) {
    if (proto.hasStringVal()) {
      return FixedString.fromProto(proto.getStringVal());
    }
    if (proto.hasInt32Val()) {
      return FixedInt32.fromProto(proto.getInt32Val());
    }
    if (proto.hasFloatVal()) {
      return FixedFloat.fromProto(proto.getFloatVal());
    }
    if (proto.hasBoolVal()) {
      return FixedBool.fromProto(proto.getBoolVal());
    }
    if (proto.hasColorVal()) {
      return FixedColor.fromProto(proto.getColorVal());
    }
    throw new IllegalStateException("Proto was not a recognised instance of DynamicDataValue");
  }
}
