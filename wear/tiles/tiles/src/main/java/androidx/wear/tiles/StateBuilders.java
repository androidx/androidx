/*
 * Copyright 2021-2022 The Android Open Source Project
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

package androidx.wear.tiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.StateProto;

/**
 * Builders for state of a tile.
 *
 * @deprecated Use {@link androidx.wear.protolayout.StateBuilders} instead.
 */
@Deprecated
public final class StateBuilders {
    private StateBuilders() {}

    /** {@link State} information. */
    public static final class State {
        private final StateProto.State mImpl;
        @Nullable private final Fingerprint mFingerprint;

        State(StateProto.State impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the ID of the clickable that was last clicked. */
        @NonNull
        public String getLastClickableId() {
            return mImpl.getLastClickableId();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static State fromProto(@NonNull StateProto.State proto) {
            return new State(proto, null);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public StateProto.State toProto() {
            return mImpl;
        }

        /** Builder for {@link State} */
        public static final class Builder {
            private final StateProto.State.Builder mImpl = StateProto.State.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(616326811);

            public Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public State build() {
                return new State(mImpl.build(), mFingerprint);
            }
        }
    }
}
