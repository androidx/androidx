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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.TriggerProto;

/** Builders for triggers that can be used to start an animation. */
public final class TriggerBuilders {
  private TriggerBuilders() {}

  /** Creates a {@link Trigger} that fires immediately when the layout is loaded / reloaded.. */
  @NonNull
  public static Trigger createOnLoadTrigger() {
    return new OnLoadTrigger.Builder().build();
  }

  /**
   * Creates a {@link Trigger} that fires *every time* the condition switches from false to true.
   * If the condition is true initially, that will fire the trigger on load.
   */
  @NonNull
  public static Trigger createOnConditionMetTrigger(
          @NonNull DynamicBool dynamicBool) {
    return new OnConditionMetTrigger.Builder().setCondition(dynamicBool).build();
  }

  /**
   * Triggers immediately when the layout is loaded / reloaded.
   *
   * @since 1.2
   */
  static final class OnLoadTrigger implements Trigger {
    private final TriggerProto.OnLoadTrigger mImpl;
    @Nullable private final Fingerprint mFingerprint;

    OnLoadTrigger(TriggerProto.OnLoadTrigger impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static OnLoadTrigger fromProto(@NonNull TriggerProto.OnLoadTrigger proto) {
      return new OnLoadTrigger(proto, null);
    }

    @NonNull
    TriggerProto.OnLoadTrigger toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
      public TriggerProto.Trigger toTriggerProto() {
      return TriggerProto.Trigger.newBuilder().setOnLoadTrigger(mImpl).build();
    }

    /** Builder for {@link OnLoadTrigger}. */
    public static final class Builder implements Trigger.Builder {
      private final TriggerProto.OnLoadTrigger.Builder mImpl =
          TriggerProto.OnLoadTrigger.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(-1262805599);

      public Builder() {}

      @Override
      @NonNull
      public OnLoadTrigger build() {
        return new OnLoadTrigger(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Triggers *every time* the condition switches from false to true. If the condition is true
   * initially, that will fire the trigger on load.
   *
   * @since 1.2
   */
  static final class OnConditionMetTrigger implements Trigger {
    private final TriggerProto.OnConditionMetTrigger mImpl;
    @Nullable private final Fingerprint mFingerprint;

    OnConditionMetTrigger(TriggerProto.OnConditionMetTrigger impl, @Nullable Fingerprint fingerprint) {
      this.mImpl = impl;
      this.mFingerprint = fingerprint;
    }

    /**
     * Gets dynamic boolean used as trigger. Intended for testing purposes only.
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

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Fingerprint getFingerprint() {
      return mFingerprint;
    }

    @NonNull
    static OnConditionMetTrigger fromProto(@NonNull TriggerProto.OnConditionMetTrigger proto) {
      return new OnConditionMetTrigger(proto, null);
    }

    @NonNull
    TriggerProto.OnConditionMetTrigger toProto() {
      return mImpl;
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
      public TriggerProto.Trigger toTriggerProto() {
      return TriggerProto.Trigger.newBuilder().setOnConditionMetTrigger(mImpl).build();
    }

    /** Builder for {@link OnConditionMetTrigger}. */
    public static final class Builder implements Trigger.Builder {
      private final TriggerProto.OnConditionMetTrigger.Builder mImpl =
          TriggerProto.OnConditionMetTrigger.newBuilder();
      private final Fingerprint mFingerprint = new Fingerprint(1952746052);

      public Builder() {}

      /**
       * Sets dynamic boolean used as trigger.
       *
       * @since 1.2
       */
      @NonNull
      public Builder setCondition(@NonNull DynamicBool dynamicBool) {
        mImpl.setCondition(dynamicBool.toDynamicBoolProto());
        mFingerprint.recordPropertyUpdate(
            1, checkNotNull(dynamicBool.getFingerprint()).aggregateValueAsInt());
        return this;
      }

      @Override
      @NonNull
      public OnConditionMetTrigger build() {
        return new OnConditionMetTrigger(mImpl.build(), mFingerprint);
      }
    }
  }

  /**
   * Interface defining the triggers that can be fired. These triggers can be used to allow
   * acting on events. For example some animations can be set to start based on a trigger.
   *
   * @since 1.2
   */
  public interface Trigger {
    /**
     * Get the protocol buffer representation of this object.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    TriggerProto.Trigger toTriggerProto();

    /**
     * Get the fingerprint for this object or null if unknown.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Fingerprint getFingerprint();

    /** Builder to create {@link Trigger} objects.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder {

      /** Builds an instance with values accumulated in this Builder. */
      @NonNull
      Trigger build();
    }
  }

  @NonNull
  static Trigger triggerFromProto(@NonNull TriggerProto.Trigger proto) {
    if (proto.hasOnLoadTrigger()) {
      return OnLoadTrigger.fromProto(proto.getOnLoadTrigger());
    }
    if (proto.hasOnConditionMetTrigger()) {
      return OnConditionMetTrigger.fromProto(proto.getOnConditionMetTrigger());
    }
    throw new IllegalStateException("Proto was not a recognised instance of Trigger");
  }
}
