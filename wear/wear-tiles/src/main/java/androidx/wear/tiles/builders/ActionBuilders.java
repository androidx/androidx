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

package androidx.wear.tiles.builders;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.proto.ActionProto;

/** Builders for actions that can be performed when a user interacts with layout elements. */
public final class ActionBuilders {
    private ActionBuilders() {}

    /** A launch action to send an intent to an Android activity. */
    public static final class AndroidActivity {
        private final ActionProto.AndroidActivity mImpl;

        AndroidActivity(ActionProto.AndroidActivity impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public ActionProto.AndroidActivity toProto() {
            return mImpl;
        }

        /** Builder for {@link AndroidActivity} */
        public static final class Builder {
            private final ActionProto.AndroidActivity.Builder mImpl =
                    ActionProto.AndroidActivity.newBuilder();

            Builder() {}

            /** Sets the package name to send the intent to, for example, "com.google.weather". */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setPackageName(@NonNull String packageName) {
                mImpl.setPackageName(packageName);
                return this;
            }

            /**
             * Sets the fully qualified class name (including the package) to send the intent to,
             * for example, "com.google.weather.WeatherOverviewActivity".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setClassName(@NonNull String className) {
                mImpl.setClassName(className);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public AndroidActivity build() {
                return new AndroidActivity(mImpl.build());
            }
        }
    }

    /**
     * An action used to launch another activity on the system. This can hold multiple different
     * underlying action types, which will be picked based on what the underlying runtime believes
     * to be suitable.
     */
    public static final class LaunchAction implements Action {
        private final ActionProto.LaunchAction mImpl;

        LaunchAction(ActionProto.LaunchAction impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        ActionProto.LaunchAction toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public ActionProto.Action toActionProto() {
            return ActionProto.Action.newBuilder().setLaunchAction(mImpl).build();
        }

        /** Builder for {@link LaunchAction}. */
        public static final class Builder implements Action.Builder {
            private final ActionProto.LaunchAction.Builder mImpl =
                    ActionProto.LaunchAction.newBuilder();

            Builder() {}

            /** Sets an action to launch an Android activity. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setAndroidActivity(@NonNull AndroidActivity androidActivity) {
                mImpl.setAndroidActivity(androidActivity.toProto());
                return this;
            }

            /** Sets an action to launch an Android activity. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setAndroidActivity(
                    @NonNull AndroidActivity.Builder androidActivityBuilder) {
                mImpl.setAndroidActivity(androidActivityBuilder.build().toProto());
                return this;
            }

            @Override
            @NonNull
            public LaunchAction build() {
                return new LaunchAction(mImpl.build());
            }
        }
    }

    /** An action used to load (or reload) the tile contents. */
    public static final class LoadAction implements Action {
        private final ActionProto.LoadAction mImpl;

        LoadAction(ActionProto.LoadAction impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        ActionProto.LoadAction toProto() {
            return mImpl;
        }

        /** @hide */
        @Override
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public ActionProto.Action toActionProto() {
            return ActionProto.Action.newBuilder().setLoadAction(mImpl).build();
        }

        /** Builder for {@link LoadAction}. */
        public static final class Builder implements Action.Builder {
            private final ActionProto.LoadAction.Builder mImpl =
                    ActionProto.LoadAction.newBuilder();

            Builder() {}

            @Override
            @NonNull
            public LoadAction build() {
                return new LoadAction(mImpl.build());
            }
        }
    }

    /** Interface defining an action that can be used by a layout element. */
    public interface Action {
        /**
         * Get the protocol buffer representation of this object.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        ActionProto.Action toActionProto();

        /** Builder to create {@link Action} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            Action build();
        }
    }
}
