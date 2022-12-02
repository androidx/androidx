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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedBool;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedColor;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedFloat;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedInt32;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedString;
import androidx.wear.protolayout.expression.proto.StateEntryProto;

/** Builders for state of a tile. */
final class StateEntryBuilders {
  private StateEntryBuilders() {}

  /**
   * Interface defining a state entry value.
   *
   * @since 1.2
   */
  public interface StateEntryValue {
    /**
     * Get the protocol buffer representation of this object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    StateEntryProto.StateEntryValue toStateEntryValueProto();

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link StateEntryValue} objects. */
    @SuppressLint("StaticFinalBuilder")
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      StateEntryValue build();
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
  public static StateEntryValue stateEntryValueFromProto(
      @NonNull StateEntryProto.StateEntryValue proto) {
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
    throw new IllegalStateException("Proto was not a recognised instance of StateEntryValue");
  }
}
