/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.proto.StateProto;

/** Builders for state of a tile. */
public final class StateBuilders {
    private StateBuilders() {}

    /** {@link State} information. */
    public static final class State {
        private final StateProto.State mImpl;

        private State(StateProto.State impl) {
            this.mImpl = impl;
        }

        /** Gets the ID of the clickable that was last clicked. */
        @NonNull
        public String getLastClickableId() {
            return mImpl.getLastClickableId();
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static State fromProto(@NonNull StateProto.State proto) {
            return new State(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public StateProto.State toProto() {
            return mImpl;
        }

        /** Builder for {@link State} */
        public static final class Builder {
            private final StateProto.State.Builder mImpl = StateProto.State.newBuilder();

            Builder() {}

            /** Builds an instance from accumulated values. */
            @NonNull
            public State build() {
                return State.fromProto(mImpl.build());
            }
        }
    }
}
