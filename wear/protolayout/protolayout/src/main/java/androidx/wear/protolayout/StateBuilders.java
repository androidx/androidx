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

package androidx.wear.protolayout;

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.StateEntryBuilders;
import androidx.wear.protolayout.expression.StateEntryBuilders.StateEntryValue;
import androidx.wear.protolayout.expression.proto.StateEntryProto;
import androidx.wear.protolayout.proto.StateProto;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Builders for state of a layout. */
public final class StateBuilders {
  private StateBuilders() {}

  /**
   * {@link State} information.
   *
   * @since 1.0
   */
  public static final class State {
    private final StateProto.State mImpl;
    @Nullable private final Fingerprint mFingerprint;

    State(StateProto.State impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets the ID of the clickable that was last clicked.
     *
     * @since 1.0
     */
    @NonNull
    public String getLastClickableId() {
      return mImpl.getLastClickableId();
    }

    /**
     * Gets any shared state between the provider and renderer.
     *
     * @since 1.2
     */
    @NonNull
    public Map<String, StateEntryValue> getIdToValueMapping() {
      Map<String, StateEntryValue> map = new HashMap<>();
      for (Entry<String, StateEntryProto.StateEntryValue> entry :
          mImpl.getIdToValueMap().entrySet()) {
        map.put(entry.getKey(), StateEntryBuilders.stateEntryValueFromProto(entry.getValue()));
      }
      return Collections.unmodifiableMap(map);
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
    public static State fromProto(
        @NonNull StateProto.State proto, @Nullable Fingerprint fingerprint) {
      return new State(proto, fingerprint);
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static State fromProto(@NonNull StateProto.State proto) {
      return fromProto(proto, null);
    }

    /**
     * Returns the internal proto instance.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public StateProto.State toProto() {
      return mImpl;
    }

    @Override
    @NonNull
    public String toString() {
      return "State{"
          + "lastClickableId="
          + getLastClickableId()
          + ", idToValueMapping="
          + getIdToValueMapping()
          + "}";
    }

    /** Builder for {@link State} */
    public static final class Builder {
      private final StateProto.State.Builder mImpl = StateProto.State.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-688813584);

      public Builder() {}

      /**
       * Adds an entry into any shared state between the provider and renderer.
       *
       * @since 1.2
       */
      @SuppressLint("MissingGetterMatchingBuilder")
      @NonNull
      public Builder addIdToValueMapping(@NonNull String id, @NonNull StateEntryValue value) {
        mImpl.putIdToValue(id, value.toStateEntryValueProto());
        mFingerprint.recordPropertyUpdate(
            id.hashCode(), checkNotNull(value.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      private static final int MAX_STATE_SIZE = 30;

      /** Builds an instance from accumulated values. */
      @NonNull
      public State build() {
        if (mImpl.getIdToValueMap().size() > MAX_STATE_SIZE) {
          throw new IllegalStateException(
                  String.format(
                          "State size is too large: %d. Maximum " + "allowed state size is %d.",
                          mImpl.getIdToValueMap().size(), MAX_STATE_SIZE));
        }
        return new State(mImpl.build(), mFingerprint);
      }
    }
  }
}
