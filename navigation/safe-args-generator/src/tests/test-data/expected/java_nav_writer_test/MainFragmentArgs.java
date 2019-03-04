/*
 * Copyright 2018 The Android Open Source Project
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

package a.b;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavArgs;
import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.lang.System;
import java.nio.file.AccessMode;
import java.util.HashMap;

public class MainFragmentArgs implements NavArgs {
    private final HashMap arguments = new HashMap();

    private MainFragmentArgs() {
    }

    private MainFragmentArgs(HashMap argumentsMap) {
        this.arguments.putAll(argumentsMap);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static MainFragmentArgs fromBundle(@NonNull Bundle bundle) {
        MainFragmentArgs __result = new MainFragmentArgs();
        bundle.setClassLoader(MainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("main")) {
            String main;
            main = bundle.getString("main");
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            __result.arguments.put("main", main);
        } else {
            throw new IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("optional")) {
            int optional;
            optional = bundle.getInt("optional");
            __result.arguments.put("optional", optional);
        }
        if (bundle.containsKey("reference")) {
            int reference;
            reference = bundle.getInt("reference");
            __result.arguments.put("reference", reference);
        }
        if (bundle.containsKey("referenceZeroDefaultValue")) {
            int referenceZeroDefaultValue;
            referenceZeroDefaultValue = bundle.getInt("referenceZeroDefaultValue");
            __result.arguments.put("referenceZeroDefaultValue", referenceZeroDefaultValue);
        }
        if (bundle.containsKey("floatArg")) {
            float floatArg;
            floatArg = bundle.getFloat("floatArg");
            __result.arguments.put("floatArg", floatArg);
        }
        if (bundle.containsKey("floatArrayArg")) {
            float[] floatArrayArg;
            floatArrayArg = bundle.getFloatArray("floatArrayArg");
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            __result.arguments.put("floatArrayArg", floatArrayArg);
        } else {
            throw new IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("objectArrayArg")) {
            ActivityInfo[] objectArrayArg;
            Parcelable[] __array = bundle.getParcelableArray("objectArrayArg");
            if (__array != null) {
                objectArrayArg = new ActivityInfo[__array.length];
                System.arraycopy(__array, 0, objectArrayArg, 0, __array.length);
            } else {
                objectArrayArg = null;
            }
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            __result.arguments.put("objectArrayArg", objectArrayArg);
        } else {
            throw new IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("boolArg")) {
            boolean boolArg;
            boolArg = bundle.getBoolean("boolArg");
            __result.arguments.put("boolArg", boolArg);
        }
        if (bundle.containsKey("optionalParcelable")) {
            ActivityInfo optionalParcelable;
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                optionalParcelable = (ActivityInfo) bundle.get("optionalParcelable");
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            __result.arguments.put("optionalParcelable", optionalParcelable);
        }
        if (bundle.containsKey("enumArg")) {
            AccessMode enumArg;
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
                enumArg = (AccessMode) bundle.get("enumArg");
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            if (enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
            __result.arguments.put("enumArg", enumArg);
        }
        return __result;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public String getMain() {
        return (String) arguments.get("main");
    }

    @SuppressWarnings("unchecked")
    public int getOptional() {
        return (int) arguments.get("optional");
    }

    @SuppressWarnings("unchecked")
    public int getReference() {
        return (int) arguments.get("reference");
    }

    @SuppressWarnings("unchecked")
    public int getReferenceZeroDefaultValue() {
        return (int) arguments.get("referenceZeroDefaultValue");
    }

    @SuppressWarnings("unchecked")
    public float getFloatArg() {
        return (float) arguments.get("floatArg");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public float[] getFloatArrayArg() {
        return (float[]) arguments.get("floatArrayArg");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public ActivityInfo[] getObjectArrayArg() {
        return (ActivityInfo[]) arguments.get("objectArrayArg");
    }

    @SuppressWarnings("unchecked")
    public boolean getBoolArg() {
        return (boolean) arguments.get("boolArg");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public ActivityInfo getOptionalParcelable() {
        return (ActivityInfo) arguments.get("optionalParcelable");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public AccessMode getEnumArg() {
        return (AccessMode) arguments.get("enumArg");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle toBundle() {
        Bundle __result = new Bundle();
        if (arguments.containsKey("main")) {
            String main = (String) arguments.get("main");
            __result.putString("main", main);
        }
        if (arguments.containsKey("optional")) {
            int optional = (int) arguments.get("optional");
            __result.putInt("optional", optional);
        }
        if (arguments.containsKey("reference")) {
            int reference = (int) arguments.get("reference");
            __result.putInt("reference", reference);
        }
        if (arguments.containsKey("referenceZeroDefaultValue")) {
            int referenceZeroDefaultValue = (int) arguments.get("referenceZeroDefaultValue");
            __result.putInt("referenceZeroDefaultValue", referenceZeroDefaultValue);
        }
        if (arguments.containsKey("floatArg")) {
            float floatArg = (float) arguments.get("floatArg");
            __result.putFloat("floatArg", floatArg);
        }
        if (arguments.containsKey("floatArrayArg")) {
            float[] floatArrayArg = (float[]) arguments.get("floatArrayArg");
            __result.putFloatArray("floatArrayArg", floatArrayArg);
        }
        if (arguments.containsKey("objectArrayArg")) {
            ActivityInfo[] objectArrayArg = (ActivityInfo[]) arguments.get("objectArrayArg");
            __result.putParcelableArray("objectArrayArg", objectArrayArg);
        }
        if (arguments.containsKey("boolArg")) {
            boolean boolArg = (boolean) arguments.get("boolArg");
            __result.putBoolean("boolArg", boolArg);
        }
        if (arguments.containsKey("optionalParcelable")) {
            ActivityInfo optionalParcelable = (ActivityInfo) arguments.get("optionalParcelable");
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || optionalParcelable == null) {
                __result.putParcelable("optionalParcelable", Parcelable.class.cast(optionalParcelable));
            } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                __result.putSerializable("optionalParcelable", Serializable.class.cast(optionalParcelable));
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        if (arguments.containsKey("enumArg")) {
            AccessMode enumArg = (AccessMode) arguments.get("enumArg");
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || enumArg == null) {
                __result.putParcelable("enumArg", Parcelable.class.cast(enumArg));
            } else if (Serializable.class.isAssignableFrom(AccessMode.class)) {
                __result.putSerializable("enumArg", Serializable.class.cast(enumArg));
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        return __result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        MainFragmentArgs that = (MainFragmentArgs) object;
        if (arguments.containsKey("main") != that.arguments.containsKey("main")) {
            return false;
        }
        if (getMain() != null ? !getMain().equals(that.getMain()) : that.getMain() != null) {
            return false;
        }
        if (arguments.containsKey("optional") != that.arguments.containsKey("optional")) {
            return false;
        }
        if (getOptional() != that.getOptional()) {
            return false;
        }
        if (arguments.containsKey("reference") != that.arguments.containsKey("reference")) {
            return false;
        }
        if (getReference() != that.getReference()) {
            return false;
        }
        if (arguments.containsKey("referenceZeroDefaultValue") != that.arguments.containsKey("referenceZeroDefaultValue")) {
            return false;
        }
        if (getReferenceZeroDefaultValue() != that.getReferenceZeroDefaultValue()) {
            return false;
        }
        if (arguments.containsKey("floatArg") != that.arguments.containsKey("floatArg")) {
            return false;
        }
        if (Float.compare(that.getFloatArg(), getFloatArg()) != 0) {
            return false;
        }
        if (arguments.containsKey("floatArrayArg") != that.arguments.containsKey("floatArrayArg")) {
            return false;
        }
        if (getFloatArrayArg() != null ? !getFloatArrayArg().equals(that.getFloatArrayArg()) : that.getFloatArrayArg() != null) {
            return false;
        }
        if (arguments.containsKey("objectArrayArg") != that.arguments.containsKey("objectArrayArg")) {
            return false;
        }
        if (getObjectArrayArg() != null ? !getObjectArrayArg().equals(that.getObjectArrayArg()) : that.getObjectArrayArg() != null) {
            return false;
        }
        if (arguments.containsKey("boolArg") != that.arguments.containsKey("boolArg")) {
            return false;
        }
        if (getBoolArg() != that.getBoolArg()) {
            return false;
        }
        if (arguments.containsKey("optionalParcelable") != that.arguments.containsKey("optionalParcelable")) {
            return false;
        }
        if (getOptionalParcelable() != null ? !getOptionalParcelable().equals(that.getOptionalParcelable()) : that.getOptionalParcelable() != null) {
            return false;
        }
        if (arguments.containsKey("enumArg") != that.arguments.containsKey("enumArg")) {
            return false;
        }
        if (getEnumArg() != null ? !getEnumArg().equals(that.getEnumArg()) : that.getEnumArg() != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (getMain() != null ? getMain().hashCode() : 0);
        result = 31 * result + getOptional();
        result = 31 * result + getReference();
        result = 31 * result + getReferenceZeroDefaultValue();
        result = 31 * result + Float.floatToIntBits(getFloatArg());
        result = 31 * result + java.util.Arrays.hashCode(getFloatArrayArg());
        result = 31 * result + java.util.Arrays.hashCode(getObjectArrayArg());
        result = 31 * result + (getBoolArg() ? 1 : 0);
        result = 31 * result + (getOptionalParcelable() != null ? getOptionalParcelable().hashCode() : 0);
        result = 31 * result + (getEnumArg() != null ? getEnumArg().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MainFragmentArgs{"
                + "main=" + getMain()
                + ", optional=" + getOptional()
                + ", reference=" + getReference()
                + ", referenceZeroDefaultValue=" + getReferenceZeroDefaultValue()
                + ", floatArg=" + getFloatArg()
                + ", floatArrayArg=" + getFloatArrayArg()
                + ", objectArrayArg=" + getObjectArrayArg()
                + ", boolArg=" + getBoolArg()
                + ", optionalParcelable=" + getOptionalParcelable()
                + ", enumArg=" + getEnumArg()
                + "}";
    }

    public static class Builder {
        private final HashMap arguments = new HashMap();

        public Builder(MainFragmentArgs original) {
            this.arguments.putAll(original.arguments);
        }

        public Builder(@NonNull String main, @NonNull float[] floatArrayArg,
                @NonNull ActivityInfo[] objectArrayArg) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("main", main);
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("floatArrayArg", floatArrayArg);
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("objectArrayArg", objectArrayArg);
        }

        @NonNull
        public MainFragmentArgs build() {
            MainFragmentArgs result = new MainFragmentArgs(arguments);
            return result;
        }

        @NonNull
        public Builder setMain(@NonNull String main) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("main", main);
            return this;
        }

        @NonNull
        public Builder setOptional(int optional) {
            this.arguments.put("optional", optional);
            return this;
        }

        @NonNull
        public Builder setReference(int reference) {
            this.arguments.put("reference", reference);
            return this;
        }

        @NonNull
        public Builder setReferenceZeroDefaultValue(int referenceZeroDefaultValue) {
            this.arguments.put("referenceZeroDefaultValue", referenceZeroDefaultValue);
            return this;
        }

        @NonNull
        public Builder setFloatArg(float floatArg) {
            this.arguments.put("floatArg", floatArg);
            return this;
        }

        @NonNull
        public Builder setFloatArrayArg(@NonNull float[] floatArrayArg) {
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("floatArrayArg", floatArrayArg);
            return this;
        }

        @NonNull
        public Builder setObjectArrayArg(@NonNull ActivityInfo[] objectArrayArg) {
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("objectArrayArg", objectArrayArg);
            return this;
        }

        @NonNull
        public Builder setBoolArg(boolean boolArg) {
            this.arguments.put("boolArg", boolArg);
            return this;
        }

        @NonNull
        public Builder setOptionalParcelable(@Nullable ActivityInfo optionalParcelable) {
            this.arguments.put("optionalParcelable", optionalParcelable);
            return this;
        }

        @NonNull
        public Builder setEnumArg(@NonNull AccessMode enumArg) {
            if (enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("enumArg", enumArg);
            return this;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getMain() {
            return (String) arguments.get("main");
        }

        @SuppressWarnings("unchecked")
        public int getOptional() {
            return (int) arguments.get("optional");
        }

        @SuppressWarnings("unchecked")
        public int getReference() {
            return (int) arguments.get("reference");
        }

        @SuppressWarnings("unchecked")
        public int getReferenceZeroDefaultValue() {
            return (int) arguments.get("referenceZeroDefaultValue");
        }

        @SuppressWarnings("unchecked")
        public float getFloatArg() {
            return (float) arguments.get("floatArg");
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public float[] getFloatArrayArg() {
            return (float[]) arguments.get("floatArrayArg");
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public ActivityInfo[] getObjectArrayArg() {
            return (ActivityInfo[]) arguments.get("objectArrayArg");
        }

        @SuppressWarnings("unchecked")
        public boolean getBoolArg() {
            return (boolean) arguments.get("boolArg");
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public ActivityInfo getOptionalParcelable() {
            return (ActivityInfo) arguments.get("optionalParcelable");
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public AccessMode getEnumArg() {
            return (AccessMode) arguments.get("enumArg");
        }
    }
}