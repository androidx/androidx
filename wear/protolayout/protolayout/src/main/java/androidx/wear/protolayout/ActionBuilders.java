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
import android.app.PendingIntent;
import android.content.ComponentName;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.StateBuilders.State;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.ActionProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Builders for actions that can be performed when a user interacts with layout elements. */
public final class ActionBuilders {
    private ActionBuilders() {}

    /** Shortcut for building an {@link AndroidStringExtra}. */
    public static @NonNull AndroidStringExtra stringExtra(@NonNull String value) {
        return new AndroidStringExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidIntExtra}. */
    public static @NonNull AndroidIntExtra intExtra(int value) {
        return new AndroidIntExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidLongExtra}. */
    public static @NonNull AndroidLongExtra longExtra(long value) {
        return new AndroidLongExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidDoubleExtra}. */
    public static @NonNull AndroidDoubleExtra doubleExtra(double value) {
        return new AndroidDoubleExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building an {@link AndroidBooleanExtra}. */
    public static @NonNull AndroidBooleanExtra booleanExtra(boolean value) {
        return new AndroidBooleanExtra.Builder().setValue(value).build();
    }

    /** Shortcut for building a {@link LaunchAction}. */
    public static @NonNull LaunchAction launchAction(@NonNull ComponentName activityComponentName) {
        return new LaunchAction.Builder()
                .setAndroidActivity(
                        new AndroidActivity.Builder()
                                .setClassName(activityComponentName.getClassName())
                                .setPackageName(activityComponentName.getPackageName())
                                .build())
                .build();
    }

    /** Shortcut for building a {@link LaunchAction} with extras in the launch intent. */
    public static @NonNull LaunchAction launchAction(
            @NonNull ComponentName activityComponentName,
            @NonNull Map<String, AndroidExtra> intentExtras) {
        AndroidActivity.Builder builder = new AndroidActivity.Builder();
        for (Map.Entry<String, AndroidExtra> intentExtra : intentExtras.entrySet()) {
            builder.addKeyToExtraMapping(intentExtra.getKey(), intentExtra.getValue());
        }
        return new LaunchAction.Builder()
                .setAndroidActivity(
                        builder.setClassName(activityComponentName.getClassName())
                                .setPackageName(activityComponentName.getPackageName())
                                .build())
                .build();
    }

    /** A string value that can be added to an Android intent's extras. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class AndroidStringExtra implements AndroidExtra {
        private final ActionProto.AndroidStringExtra mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AndroidStringExtra(ActionProto.AndroidStringExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public @NonNull String getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidStringExtra fromProto(
                ActionProto.@NonNull AndroidStringExtra proto, @Nullable Fingerprint fingerprint) {
            return new AndroidStringExtra(proto, fingerprint);
        }

        static @NonNull AndroidStringExtra fromProto(
                ActionProto.@NonNull AndroidStringExtra proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ActionProto.@NonNull AndroidStringExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setStringVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "AndroidStringExtra{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link AndroidStringExtra}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidStringExtra.Builder mImpl =
                    ActionProto.AndroidStringExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-973795259);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@NonNull String value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value.hashCode());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull AndroidStringExtra build() {
                return new AndroidStringExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An integer value that can be added to an Android intent's extras. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class AndroidIntExtra implements AndroidExtra {
        private final ActionProto.AndroidIntExtra mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AndroidIntExtra(ActionProto.AndroidIntExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public int getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidIntExtra fromProto(
                ActionProto.@NonNull AndroidIntExtra proto, @Nullable Fingerprint fingerprint) {
            return new AndroidIntExtra(proto, fingerprint);
        }

        static @NonNull AndroidIntExtra fromProto(ActionProto.@NonNull AndroidIntExtra proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ActionProto.@NonNull AndroidIntExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setIntVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "AndroidIntExtra{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link AndroidIntExtra}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidIntExtra.Builder mImpl =
                    ActionProto.AndroidIntExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1199435881);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(int value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull AndroidIntExtra build() {
                return new AndroidIntExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A long value that can be added to an Android intent's extras. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class AndroidLongExtra implements AndroidExtra {
        private final ActionProto.AndroidLongExtra mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AndroidLongExtra(ActionProto.AndroidLongExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public long getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidLongExtra fromProto(
                ActionProto.@NonNull AndroidLongExtra proto, @Nullable Fingerprint fingerprint) {
            return new AndroidLongExtra(proto, fingerprint);
        }

        static @NonNull AndroidLongExtra fromProto(ActionProto.@NonNull AndroidLongExtra proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ActionProto.@NonNull AndroidLongExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setLongVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "AndroidLongExtra{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link AndroidLongExtra}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidLongExtra.Builder mImpl =
                    ActionProto.AndroidLongExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-906933303);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(long value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Long.hashCode(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull AndroidLongExtra build() {
                return new AndroidLongExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A double value that can be added to an Android intent's extras. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class AndroidDoubleExtra implements AndroidExtra {
        private final ActionProto.AndroidDoubleExtra mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AndroidDoubleExtra(ActionProto.AndroidDoubleExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public double getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidDoubleExtra fromProto(
                ActionProto.@NonNull AndroidDoubleExtra proto, @Nullable Fingerprint fingerprint) {
            return new AndroidDoubleExtra(proto, fingerprint);
        }

        static @NonNull AndroidDoubleExtra fromProto(
                ActionProto.@NonNull AndroidDoubleExtra proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ActionProto.@NonNull AndroidDoubleExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setDoubleVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "AndroidDoubleExtra{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link AndroidDoubleExtra}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidDoubleExtra.Builder mImpl =
                    ActionProto.AndroidDoubleExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1104636989);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(double value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Double.hashCode(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull AndroidDoubleExtra build() {
                return new AndroidDoubleExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A boolean value that can be added to an Android intent's extras. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class AndroidBooleanExtra implements AndroidExtra {
        private final ActionProto.AndroidBooleanExtra mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AndroidBooleanExtra(
                ActionProto.AndroidBooleanExtra impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public boolean getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidBooleanExtra fromProto(
                ActionProto.@NonNull AndroidBooleanExtra proto, @Nullable Fingerprint fingerprint) {
            return new AndroidBooleanExtra(proto, fingerprint);
        }

        static @NonNull AndroidBooleanExtra fromProto(
                ActionProto.@NonNull AndroidBooleanExtra proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ActionProto.@NonNull AndroidBooleanExtra toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull AndroidExtra toAndroidExtraProto() {
            return ActionProto.AndroidExtra.newBuilder().setBooleanVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "AndroidBooleanExtra{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link AndroidBooleanExtra}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements AndroidExtra.Builder {
            private final ActionProto.AndroidBooleanExtra.Builder mImpl =
                    ActionProto.AndroidBooleanExtra.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1244694745);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setValue(boolean value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Boolean.hashCode(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull AndroidBooleanExtra build() {
                return new AndroidBooleanExtra(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining an item that can be included in the extras of an intent that will be sent
     * to an Android activity. Supports types in android.os.PersistableBundle, excluding arrays.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface AndroidExtra {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ActionProto.@NonNull AndroidExtra toAndroidExtraProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link AndroidExtra} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull AndroidExtra build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull AndroidExtra androidExtraFromProto(
            ActionProto.@NonNull AndroidExtra proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasStringVal()) {
            return AndroidStringExtra.fromProto(proto.getStringVal(), fingerprint);
        }
        if (proto.hasIntVal()) {
            return AndroidIntExtra.fromProto(proto.getIntVal(), fingerprint);
        }
        if (proto.hasLongVal()) {
            return AndroidLongExtra.fromProto(proto.getLongVal(), fingerprint);
        }
        if (proto.hasDoubleVal()) {
            return AndroidDoubleExtra.fromProto(proto.getDoubleVal(), fingerprint);
        }
        if (proto.hasBooleanVal()) {
            return AndroidBooleanExtra.fromProto(proto.getBooleanVal(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of AndroidExtra");
    }

    static @NonNull AndroidExtra androidExtraFromProto(ActionProto.@NonNull AndroidExtra proto) {
        return androidExtraFromProto(proto, null);
    }

    /** A launch action to send an intent to an Android activity. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class AndroidActivity {
        private final ActionProto.AndroidActivity mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AndroidActivity(ActionProto.AndroidActivity impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the package name to send the intent to, for example, "com.example.weather". */
        public @NonNull String getPackageName() {
            return mImpl.getPackageName();
        }

        /**
         * Gets the fully qualified class name (including the package) to send the intent to, for
         * example, "com.example.weather.WeatherOverviewActivity".
         */
        public @NonNull String getClassName() {
            return mImpl.getClassName();
        }

        /** Gets the extras to be included in the intent. */
        public @NonNull Map<String, AndroidExtra> getKeyToExtraMapping() {
            Map<String, AndroidExtra> map = new HashMap<>();
            for (Entry<String, ActionProto.AndroidExtra> entry :
                    mImpl.getKeyToExtraMap().entrySet()) {
                map.put(entry.getKey(), ActionBuilders.androidExtraFromProto(entry.getValue()));
            }
            return Collections.unmodifiableMap(map);
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidActivity fromProto(
                ActionProto.@NonNull AndroidActivity proto, @Nullable Fingerprint fingerprint) {
            return new AndroidActivity(proto, fingerprint);
        }

        static @NonNull AndroidActivity fromProto(ActionProto.@NonNull AndroidActivity proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull AndroidActivity toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "AndroidActivity{"
                    + "packageName="
                    + getPackageName()
                    + ", className="
                    + getClassName()
                    + ", keyToExtraMapping="
                    + getKeyToExtraMapping()
                    + "}";
        }

        /** Builder for {@link AndroidActivity} */
        public static final class Builder {
            private final ActionProto.AndroidActivity.Builder mImpl =
                    ActionProto.AndroidActivity.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1799520061);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the package name to send the intent to, for example, "com.example.weather". */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setPackageName(@NonNull String packageName) {
                mImpl.setPackageName(packageName);
                mFingerprint.recordPropertyUpdate(1, packageName.hashCode());
                return this;
            }

            /**
             * Sets the fully qualified class name (including the package) to send the intent to,
             * for example, "com.example.weather.WeatherOverviewActivity".
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setClassName(@NonNull String className) {
                mImpl.setClassName(className);
                mFingerprint.recordPropertyUpdate(2, className.hashCode());
                return this;
            }

            /** Adds an entry into the extras to be included in the intent. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder addKeyToExtraMapping(
                    @NonNull String key, @NonNull AndroidExtra extra) {
                mImpl.putKeyToExtra(key, extra.toAndroidExtraProto());
                mFingerprint.recordPropertyUpdate(
                        key.hashCode(), checkNotNull(extra.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull AndroidActivity build() {
                return new AndroidActivity(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * An action used to launch another activity on the system. This can hold multiple different
     * underlying action types, which will be picked based on what the underlying runtime believes
     * to be suitable.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class LaunchAction implements Action {
        private final ActionProto.LaunchAction mImpl;
        private final @Nullable Fingerprint mFingerprint;

        LaunchAction(ActionProto.LaunchAction impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets an action to launch an Android activity. */
        public @Nullable AndroidActivity getAndroidActivity() {
            if (mImpl.hasAndroidActivity()) {
                return AndroidActivity.fromProto(mImpl.getAndroidActivity());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull LaunchAction fromProto(
                ActionProto.@NonNull LaunchAction proto, @Nullable Fingerprint fingerprint) {
            return new LaunchAction(proto, fingerprint);
        }

        static @NonNull LaunchAction fromProto(ActionProto.@NonNull LaunchAction proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ActionProto.@NonNull LaunchAction toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull Action toActionProto() {
            return ActionProto.Action.newBuilder().setLaunchAction(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "LaunchAction{" + "androidActivity=" + getAndroidActivity() + "}";
        }

        /** Builder for {@link LaunchAction}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Action.Builder {
            private final ActionProto.LaunchAction.Builder mImpl =
                    ActionProto.LaunchAction.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(2004803940);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets an action to launch an Android activity. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAndroidActivity(@NonNull AndroidActivity androidActivity) {
                mImpl.setAndroidActivity(androidActivity.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(androidActivity.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull LaunchAction build() {
                return new LaunchAction(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An action used to load (or reload) the layout contents. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class LoadAction implements Action {
        private final ActionProto.LoadAction mImpl;
        private final @Nullable Fingerprint mFingerprint;

        LoadAction(ActionProto.LoadAction impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the state to load the next layout with. This will be included in the layout request
         * sent after this action is invoked by a {@link
         * androidx.wear.protolayout.ModifiersBuilders.Clickable}.
         */
        public @Nullable State getRequestState() {
            if (mImpl.hasRequestState()) {
                return State.fromProto(mImpl.getRequestState());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull LoadAction fromProto(
                ActionProto.@NonNull LoadAction proto, @Nullable Fingerprint fingerprint) {
            return new LoadAction(proto, fingerprint);
        }

        static @NonNull LoadAction fromProto(ActionProto.@NonNull LoadAction proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ActionProto.@NonNull LoadAction toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull Action toActionProto() {
            return ActionProto.Action.newBuilder().setLoadAction(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "LoadAction{" + "requestState=" + getRequestState() + "}";
        }

        /** Builder for {@link LoadAction}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Action.Builder {
            private final ActionProto.LoadAction.Builder mImpl =
                    ActionProto.LoadAction.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(674205536);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the state to load the next layout with. This will be included in the layout
             * request sent after this action is invoked by a {@link
             * androidx.wear.protolayout.ModifiersBuilders.Clickable}.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setRequestState(@NonNull State requestState) {
                mImpl.setRequestState(requestState.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(requestState.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull LoadAction build() {
                return new LoadAction(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * An action used to perform the operation associated with a {@link PendingIntent}, such as
     * launching an activity. The {@link PendingIntent} could be retrieved with the ID of the
     * clickable to which this action is attached.
     */
    @RequiresSchemaVersion(major = 1, minor = 600)
    static final class PendingIntentAction implements Action {
        private final ActionProto.PendingIntentAction mImpl;
        private final @Nullable Fingerprint mFingerprint;

        PendingIntentAction(
                ActionProto.PendingIntentAction impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull PendingIntentAction fromProto(
                ActionProto.@NonNull PendingIntentAction proto, @Nullable Fingerprint fingerprint) {
            return new PendingIntentAction(proto, fingerprint);
        }

        static @NonNull PendingIntentAction fromProto(
                ActionProto.@NonNull PendingIntentAction proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ActionProto.@NonNull PendingIntentAction toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ActionProto.@NonNull Action toActionProto() {
            return ActionProto.Action.newBuilder().setPendingIntentAction(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "PendingIntentAction";
        }

        /** Builder for {@link PendingIntentAction}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Action.Builder {
            private final ActionProto.PendingIntentAction.Builder mImpl =
                    ActionProto.PendingIntentAction.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1136890824);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 600)
            public Builder() {}

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull PendingIntentAction build() {
                return new PendingIntentAction(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining an action that can be used by a layout element. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface Action {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ActionProto.@NonNull Action toActionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link Action} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull Action build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull Action actionFromProto(
            ActionProto.@NonNull Action proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasLaunchAction()) {
            return LaunchAction.fromProto(proto.getLaunchAction(), fingerprint);
        }
        if (proto.hasLoadAction()) {
            return LoadAction.fromProto(proto.getLoadAction(), fingerprint);
        }
        if (proto.hasPendingIntentAction()) {
            return PendingIntentAction.fromProto(proto.getPendingIntentAction(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of Action");
    }

    static @NonNull Action actionFromProto(ActionProto.@NonNull Action proto) {
        return actionFromProto(proto, null);
    }
}
