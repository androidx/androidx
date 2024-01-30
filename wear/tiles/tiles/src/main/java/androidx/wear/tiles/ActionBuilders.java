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

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import static java.util.stream.Collectors.toMap;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.ActionProto;

import java.util.Collections;
import java.util.Map;

/**
 * Builders for actions that can be performed when a user interacts with layout elements.
 *
 * @deprecated Use {@link androidx.wear.protolayout.ActionBuilders} instead.
 */
@Deprecated
public final class ActionBuilders {
    private ActionBuilders() {}

    /** Shortcut for building an {@link AndroidStringExtra}. */
    @NonNull
    public static AndroidStringExtra stringExtra(@NonNull String value) {
        return new AndroidStringExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidIntExtra}. */
    @NonNull
    public static AndroidIntExtra intExtra(int value) {
        return new AndroidIntExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidLongExtra}. */
    @NonNull
    public static AndroidLongExtra longExtra(long value) {
        return new AndroidLongExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidDoubleExtra}. */
    @NonNull
    public static AndroidDoubleExtra doubleExtra(double value) {
        return new AndroidDoubleExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidBooleanExtra}. */
    @NonNull
    public static AndroidBooleanExtra booleanExtra(boolean value) {
        return new AndroidBooleanExtra.Builder().setValue(value).build();
    }

    /** A string value that can be added to an Android intent's extras. */
    public static final class AndroidStringExtra implements AndroidExtra {
        private final ActionProto.AndroidStringExtra mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AndroidStringExtra(ActionProto.AndroidStringExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
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

        @NonNull
        static AndroidStringExtra fromProto(@NonNull ActionProto.AndroidStringExtra proto) {
            return new AndroidStringExtra(proto, null);
        }

        @NonNull
        ActionProto.AndroidStringExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ActionProto.AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setStringVal(mImpl).build();
        }

        /** Builder for {@link AndroidStringExtra}. */
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidStringExtra.Builder mImpl =
                    ActionProto.AndroidStringExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1281351679);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@NonNull String value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value.hashCode());
                return this;
            }

            @Override
            @NonNull
            public AndroidStringExtra build() {
                return new AndroidStringExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An integer value that can be added to an Android intent's extras. */
    public static final class AndroidIntExtra implements AndroidExtra {
        private final ActionProto.AndroidIntExtra mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AndroidIntExtra(ActionProto.AndroidIntExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        public int getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static AndroidIntExtra fromProto(@NonNull ActionProto.AndroidIntExtra proto) {
            return new AndroidIntExtra(proto, null);
        }

        @NonNull
        ActionProto.AndroidIntExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ActionProto.AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setIntVal(mImpl).build();
        }

        /** Builder for {@link AndroidIntExtra}. */
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidIntExtra.Builder mImpl =
                    ActionProto.AndroidIntExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1929293734);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(int value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            @Override
            @NonNull
            public AndroidIntExtra build() {
                return new AndroidIntExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A long value that can be added to an Android intent's extras. */
    public static final class AndroidLongExtra implements AndroidExtra {
        private final ActionProto.AndroidLongExtra mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AndroidLongExtra(ActionProto.AndroidLongExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        public long getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static AndroidLongExtra fromProto(@NonNull ActionProto.AndroidLongExtra proto) {
            return new AndroidLongExtra(proto, null);
        }

        @NonNull
        ActionProto.AndroidLongExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ActionProto.AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setLongVal(mImpl).build();
        }

        /** Builder for {@link AndroidLongExtra}. */
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidLongExtra.Builder mImpl =
                    ActionProto.AndroidLongExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-874743180);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(long value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Long.hashCode(value));
                return this;
            }

            @Override
            @NonNull
            public AndroidLongExtra build() {
                return new AndroidLongExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A double value that can be added to an Android intent's extras. */
    public static final class AndroidDoubleExtra implements AndroidExtra {
        private final ActionProto.AndroidDoubleExtra mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AndroidDoubleExtra(ActionProto.AndroidDoubleExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        public double getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static AndroidDoubleExtra fromProto(@NonNull ActionProto.AndroidDoubleExtra proto) {
            return new AndroidDoubleExtra(proto, null);
        }

        @NonNull
        ActionProto.AndroidDoubleExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ActionProto.AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setDoubleVal(mImpl).build();
        }

        /** Builder for {@link AndroidDoubleExtra}. */
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidDoubleExtra.Builder mImpl =
                    ActionProto.AndroidDoubleExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-278689892);

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(double value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Double.hashCode(value));
                return this;
            }

            @Override
            @NonNull
            public AndroidDoubleExtra build() {
                return new AndroidDoubleExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A boolean value that can be added to an Android intent's extras. */
    public static final class AndroidBooleanExtra implements AndroidExtra {
        private final ActionProto.AndroidBooleanExtra mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AndroidBooleanExtra(
                ActionProto.AndroidBooleanExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. Intended for testing purposes only. */
        public boolean getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static AndroidBooleanExtra fromProto(@NonNull ActionProto.AndroidBooleanExtra proto) {
            return new AndroidBooleanExtra(proto, null);
        }

        @NonNull
        ActionProto.AndroidBooleanExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ActionProto.AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setBooleanVal(mImpl).build();
        }

        /** Builder for {@link AndroidBooleanExtra}. */
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidBooleanExtra.Builder mImpl =
                    ActionProto.AndroidBooleanExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1238672683);

            public Builder() {}

            /** Sets the value. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setValue(boolean value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Boolean.hashCode(value));
                return this;
            }

            @Override
            @NonNull
            public AndroidBooleanExtra build() {
                return new AndroidBooleanExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining an item that can be included in the extras of an intent that will be sent
     * to an Android activity. Supports types in android.os.PersistableBundle, excluding arrays.
     */
    public interface AndroidExtra {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        ActionProto.AndroidExtra toAndroidExtraProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link AndroidExtra} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            AndroidExtra build();
        }
    }

    /**
     * Return an instance of one of this object's subtypes, from the protocol buffer representation.
     */
    @NonNull
    static AndroidExtra androidExtraFromProto(@NonNull ActionProto.AndroidExtra proto) {
        if (proto.hasStringVal()) {
            return AndroidStringExtra.fromProto(proto.getStringVal());
        }
        if (proto.hasIntVal()) {
            return AndroidIntExtra.fromProto(proto.getIntVal());
        }
        if (proto.hasLongVal()) {
            return AndroidLongExtra.fromProto(proto.getLongVal());
        }
        if (proto.hasDoubleVal()) {
            return AndroidDoubleExtra.fromProto(proto.getDoubleVal());
        }
        if (proto.hasBooleanVal()) {
            return AndroidBooleanExtra.fromProto(proto.getBooleanVal());
        }
        throw new IllegalStateException("Proto was not a recognised instance of AndroidExtra");
    }

    /** A launch action to send an intent to an Android activity. */
    public static final class AndroidActivity {
        private final ActionProto.AndroidActivity mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AndroidActivity(ActionProto.AndroidActivity impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the package name to send the intent to, for example, "com.google.weather". Intended
         * for testing purposes only.
         */
        @NonNull
        public String getPackageName() {
            return mImpl.getPackageName();
        }

        /**
         * Gets the fully qualified class name (including the package) to send the intent to, for
         * example, "com.google.weather.WeatherOverviewActivity". Intended for testing purposes
         * only.
         */
        @NonNull
        public String getClassName() {
            return mImpl.getClassName();
        }

        /** Gets the extras to be included in the intent. Intended for testing purposes only. */
        @NonNull
        public Map<String, AndroidExtra> getKeyToExtraMapping() {
            return Collections.unmodifiableMap(
                    mImpl.getKeyToExtraMap().entrySet().stream()
                            .collect(
                                    toMap(
                                            Map.Entry::getKey,
                                            f -> androidExtraFromProto(f.getValue()))));
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static AndroidActivity fromProto(@NonNull ActionProto.AndroidActivity proto) {
            return new AndroidActivity(proto, null);
        }

        @NonNull
        ActionProto.AndroidActivity toProto() {
            return mImpl;
        }

        /** Builder for {@link AndroidActivity} */
        public static final class Builder {
            private final ActionProto.AndroidActivity.Builder mImpl =
                    ActionProto.AndroidActivity.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1939606345);

            public Builder() {}

            /** Sets the package name to send the intent to, for example, "com.google.weather". */
            @NonNull
            public Builder setPackageName(@NonNull String packageName) {
                mImpl.setPackageName(packageName);
                mFingerprint.recordPropertyUpdate(1, packageName.hashCode());
                return this;
            }

            /**
             * Sets the fully qualified class name (including the package) to send the intent to,
             * for example, "com.google.weather.WeatherOverviewActivity".
             */
            @NonNull
            public Builder setClassName(@NonNull String className) {
                mImpl.setClassName(className);
                mFingerprint.recordPropertyUpdate(2, className.hashCode());
                return this;
            }

            /** Adds an entry into the extras to be included in the intent. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addKeyToExtraMapping(@NonNull String key, @NonNull AndroidExtra extra) {
                mImpl.putKeyToExtra(key, extra.toAndroidExtraProto());
                mFingerprint.recordPropertyUpdate(
                        key.hashCode(), checkNotNull(extra.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public AndroidActivity build() {
                return new AndroidActivity(mImpl.build(), mFingerprint);
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
        @Nullable private final Fingerprint mFingerprint;

        LaunchAction(ActionProto.LaunchAction impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets an action to launch an Android activity. Intended for testing purposes only. */
        @Nullable
        public AndroidActivity getAndroidActivity() {
            if (mImpl.hasAndroidActivity()) {
                return AndroidActivity.fromProto(mImpl.getAndroidActivity());
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
        static LaunchAction fromProto(@NonNull ActionProto.LaunchAction proto) {
            return new LaunchAction(proto, null);
        }

        @NonNull
        ActionProto.LaunchAction toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ActionProto.Action toActionProto() {
            return ActionProto.Action.newBuilder().setLaunchAction(mImpl).build();
        }

        /** Builder for {@link LaunchAction}. */
        public static final class Builder implements Action.Builder {
            private final ActionProto.LaunchAction.Builder mImpl =
                    ActionProto.LaunchAction.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(175064445);

            public Builder() {}

            /** Sets an action to launch an Android activity. */
            @NonNull
            public Builder setAndroidActivity(@NonNull AndroidActivity androidActivity) {
                mImpl.setAndroidActivity(androidActivity.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(androidActivity.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public LaunchAction build() {
                return new LaunchAction(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An action used to load (or reload) the tile contents. */
    public static final class LoadAction implements Action {
        private final ActionProto.LoadAction mImpl;
        @Nullable private final Fingerprint mFingerprint;

        LoadAction(ActionProto.LoadAction impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the state to load the next tile with. This will be included in the {@link
         * androidx.wear.tiles.RequestBuilders.TileRequest} sent after this action is invoked by a
         * {@link androidx.wear.tiles.ModifiersBuilders.Clickable}. Intended for testing purposes
         * only.
         */
        @Nullable
        public StateBuilders.State getRequestState() {
            if (mImpl.hasRequestState()) {
                return StateBuilders.State.fromProto(mImpl.getRequestState());
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
        static LoadAction fromProto(@NonNull ActionProto.LoadAction proto) {
            return new LoadAction(proto, null);
        }

        @NonNull
        ActionProto.LoadAction toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ActionProto.Action toActionProto() {
            return ActionProto.Action.newBuilder().setLoadAction(mImpl).build();
        }

        /** Builder for {@link LoadAction}. */
        public static final class Builder implements Action.Builder {
            private final ActionProto.LoadAction.Builder mImpl =
                    ActionProto.LoadAction.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1728161517);

            public Builder() {}

            /**
             * Sets the state to load the next tile with. This will be included in the {@link
             * androidx.wear.tiles.RequestBuilders.TileRequest} sent after this action is invoked by
             * a {@link androidx.wear.tiles.ModifiersBuilders.Clickable}.
             */
            @NonNull
            public Builder setRequestState(@NonNull StateBuilders.State requestState) {
                mImpl.setRequestState(requestState.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(requestState.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public LoadAction build() {
                return new LoadAction(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining an action that can be used by a layout element. */
    public interface Action {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        ActionProto.Action toActionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link Action} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            Action build();
        }
    }

    /**
     * Return an instance of one of this object's subtypes, from the protocol buffer representation.
     */
    @NonNull
    static Action actionFromProto(@NonNull ActionProto.Action proto) {
        if (proto.hasLaunchAction()) {
            return LaunchAction.fromProto(proto.getLaunchAction());
        }
        if (proto.hasLoadAction()) {
            return LoadAction.fromProto(proto.getLoadAction());
        }
        throw new IllegalStateException("Proto was not a recognised instance of Action");
    }
}
