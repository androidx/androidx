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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import androidx.navigation.NavDirections;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public static class Next implements NavDirections {
    @NonNull
    private String main;

    private int mainInt;

    @NonNull
    private String optional = "bla";

    private int optionalInt = 239;

    @Nullable
    private ActivityInfo optionalParcelable = null;

    @NonNull
    private ActivityInfo parcelable;

    public Next(@NonNull String main, int mainInt, @NonNull ActivityInfo parcelable) {
        this.main = main;
        this.mainInt = mainInt;
        this.parcelable = parcelable;
    }

    @NonNull
    public Next setMain(@NonNull String main) {
        this.main = main;
        return this;
    }

    @NonNull
    public Next setMainInt(int mainInt) {
        this.mainInt = mainInt;
        return this;
    }

    @NonNull
    public Next setOptional(@NonNull String optional) {
        this.optional = optional;
        return this;
    }

    @NonNull
    public Next setOptionalInt(int optionalInt) {
        this.optionalInt = optionalInt;
        return this;
    }

    @NonNull
    public Next setOptionalParcelable(@Nullable ActivityInfo optionalParcelable) {
        this.optionalParcelable = optionalParcelable;
        return this;
    }

    @NonNull
    public Next setParcelable(@NonNull ActivityInfo parcelable) {
        this.parcelable = parcelable;
        return this;
    }

    @NonNull
    public Bundle getArguments() {
        Bundle __outBundle = new Bundle();
        __outBundle.putString("main", this.main);
        __outBundle.putInt("mainInt", this.mainInt);
        __outBundle.putString("optional", this.optional);
        __outBundle.putInt("optionalInt", this.optionalInt);
        __outBundle.putParcelable("optionalParcelable", this.optionalParcelable);
        __outBundle.putParcelable("parcelable", this.parcelable);
        return __outBundle;
    }

    public int getActionId() {
        return a.b.R.id.next;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        if (!super.equals(object)) {
            return false;
        }
        Next that = (Next) object;
        if (main != null ? !main.equals(that.main) : that.main != null) {
            return false;
        }
        if (mainInt != that.mainInt) {
            return false;
        }
        if (optional != null ? !optional.equals(that.optional) : that.optional != null) {
            return false;
        }
        if (optionalInt != that.optionalInt) {
            return false;
        }
        if (optionalParcelable != null ? !optionalParcelable.equals(that.optionalParcelable) : that.optionalParcelable != null) {
            return false;
        }
        if (parcelable != null ? !parcelable.equals(that.parcelable) : that.parcelable != null) {
            return false;
        }
        if (getActionId() != that.getActionId()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (main != null ? main.hashCode() : 0);
        result = 31 * result + mainInt;
        result = 31 * result + (optional != null ? optional.hashCode() : 0);
        result = 31 * result + optionalInt;
        result = 31 * result + (optionalParcelable != null ? optionalParcelable.hashCode() : 0);
        result = 31 * result + (parcelable != null ? parcelable.hashCode() : 0);
        result = 31 * result + getActionId();
        return result;
    }
}