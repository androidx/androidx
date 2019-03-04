/*
 * Copyright 2017 The Android Open Source Project
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
import androidx.navigation.NavDirections;
import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public static class Next implements NavDirections {
    private final HashMap arguments = new HashMap();

    private Next(@NonNull String main, int mainInt, @NonNull ActivityInfo parcelable,
            @NonNull ActivityInfo.WindowLayout innerData) {
        if (main == null) {
            throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
        }
        this.arguments.put("main", main);
        this.arguments.put("mainInt", mainInt);
        if (parcelable == null) {
            throw new IllegalArgumentException("Argument \"parcelable\" is marked as non-null but was passed a null value.");
        }
        this.arguments.put("parcelable", parcelable);
        if (innerData == null) {
            throw new IllegalArgumentException("Argument \"innerData\" is marked as non-null but was passed a null value.");
        }
        this.arguments.put("innerData", innerData);
    }

    @NonNull
    public Next setMain(@NonNull String main) {
        if (main == null) {
            throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
        }
        this.arguments.put("main", main);
        return this;
    }

    @NonNull
    public Next setMainInt(int mainInt) {
        this.arguments.put("mainInt", mainInt);
        return this;
    }

    @NonNull
    public Next setOptional(@NonNull String optional) {
        if (optional == null) {
            throw new IllegalArgumentException("Argument \"optional\" is marked as non-null but was passed a null value.");
        }
        this.arguments.put("optional", optional);
        return this;
    }

    @NonNull
    public Next setOptionalInt(int optionalInt) {
        this.arguments.put("optionalInt", optionalInt);
        return this;
    }

    @NonNull
    public Next setOptionalParcelable(@Nullable ActivityInfo optionalParcelable) {
        this.arguments.put("optionalParcelable", optionalParcelable);
        return this;
    }

    @NonNull
    public Next setParcelable(@NonNull ActivityInfo parcelable) {
        if (parcelable == null) {
            throw new IllegalArgumentException("Argument \"parcelable\" is marked as non-null but was passed a null value.");
        }
        this.arguments.put("parcelable", parcelable);
        return this;
    }

    @NonNull
    public Next setInnerData(@NonNull ActivityInfo.WindowLayout innerData) {
        if (innerData == null) {
            throw new IllegalArgumentException("Argument \"innerData\" is marked as non-null but was passed a null value.");
        }
        this.arguments.put("innerData", innerData);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
        Bundle __result = new Bundle();
        if (arguments.containsKey("main")) {
            String main = (String) arguments.get("main");
            __result.putString("main", main);
        }
        if (arguments.containsKey("mainInt")) {
            int mainInt = (int) arguments.get("mainInt");
            __result.putInt("mainInt", mainInt);
        }
        if (arguments.containsKey("optional")) {
            String optional = (String) arguments.get("optional");
            __result.putString("optional", optional);
        }
        if (arguments.containsKey("optionalInt")) {
            int optionalInt = (int) arguments.get("optionalInt");
            __result.putInt("optionalInt", optionalInt);
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
        if (arguments.containsKey("parcelable")) {
            ActivityInfo parcelable = (ActivityInfo) arguments.get("parcelable");
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || parcelable == null) {
                __result.putParcelable("parcelable", Parcelable.class.cast(parcelable));
            } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                __result.putSerializable("parcelable", Serializable.class.cast(parcelable));
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        if (arguments.containsKey("innerData")) {
            ActivityInfo.WindowLayout innerData = (ActivityInfo.WindowLayout) arguments.get("innerData");
            if (Parcelable.class.isAssignableFrom(ActivityInfo.WindowLayout.class) || innerData == null) {
                __result.putParcelable("innerData", Parcelable.class.cast(innerData));
            } else if (Serializable.class.isAssignableFrom(ActivityInfo.WindowLayout.class)) {
                __result.putSerializable("innerData", Serializable.class.cast(innerData));
            } else {
                throw new UnsupportedOperationException(ActivityInfo.WindowLayout.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        return __result;
    }

    @Override
    public int getActionId() {
        return R.id.next;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public String getMain() {
        return (String) arguments.get("main");
    }

    @SuppressWarnings("unchecked")
    public int getMainInt() {
        return (int) arguments.get("mainInt");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public String getOptional() {
        return (String) arguments.get("optional");
    }

    @SuppressWarnings("unchecked")
    public int getOptionalInt() {
        return (int) arguments.get("optionalInt");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public ActivityInfo getOptionalParcelable() {
        return (ActivityInfo) arguments.get("optionalParcelable");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public ActivityInfo getParcelable() {
        return (ActivityInfo) arguments.get("parcelable");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public ActivityInfo.WindowLayout getInnerData() {
        return (ActivityInfo.WindowLayout) arguments.get("innerData");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Next that = (Next) object;
        if (arguments.containsKey("main") != that.arguments.containsKey("main")) {
            return false;
        }
        if (getMain() != null ? !getMain().equals(that.getMain()) : that.getMain() != null) {
            return false;
        }
        if (arguments.containsKey("mainInt") != that.arguments.containsKey("mainInt")) {
            return false;
        }
        if (getMainInt() != that.getMainInt()) {
            return false;
        }
        if (arguments.containsKey("optional") != that.arguments.containsKey("optional")) {
            return false;
        }
        if (getOptional() != null ? !getOptional().equals(that.getOptional()) : that.getOptional() != null) {
            return false;
        }
        if (arguments.containsKey("optionalInt") != that.arguments.containsKey("optionalInt")) {
            return false;
        }
        if (getOptionalInt() != that.getOptionalInt()) {
            return false;
        }
        if (arguments.containsKey("optionalParcelable") != that.arguments.containsKey("optionalParcelable")) {
            return false;
        }
        if (getOptionalParcelable() != null ? !getOptionalParcelable().equals(that.getOptionalParcelable()) : that.getOptionalParcelable() != null) {
            return false;
        }
        if (arguments.containsKey("parcelable") != that.arguments.containsKey("parcelable")) {
            return false;
        }
        if (getParcelable() != null ? !getParcelable().equals(that.getParcelable()) : that.getParcelable() != null) {
            return false;
        }
        if (arguments.containsKey("innerData") != that.arguments.containsKey("innerData")) {
            return false;
        }
        if (getInnerData() != null ? !getInnerData().equals(that.getInnerData()) : that.getInnerData() != null) {
            return false;
        }
        if (getActionId() != that.getActionId()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (getMain() != null ? getMain().hashCode() : 0);
        result = 31 * result + getMainInt();
        result = 31 * result + (getOptional() != null ? getOptional().hashCode() : 0);
        result = 31 * result + getOptionalInt();
        result = 31 * result + (getOptionalParcelable() != null ? getOptionalParcelable().hashCode() : 0);
        result = 31 * result + (getParcelable() != null ? getParcelable().hashCode() : 0);
        result = 31 * result + (getInnerData() != null ? getInnerData().hashCode() : 0);
        result = 31 * result + getActionId();
        return result;
    }

    @Override
    public String toString() {
        return "Next(actionId=" + getActionId() + "){"
                + "main=" + getMain()
                + ", mainInt=" + getMainInt()
                + ", optional=" + getOptional()
                + ", optionalInt=" + getOptionalInt()
                + ", optionalParcelable=" + getOptionalParcelable()
                + ", parcelable=" + getParcelable()
                + ", innerData=" + getInnerData()
                + "}";
    }
}