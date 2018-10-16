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

import android.os.Bundle;
import android.support.annotation.NonNull;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class MainFragment$InnerFragmentArgs {
    @NonNull
    private String mainArg;

    private MainFragment$InnerFragmentArgs() {
    }

    @NonNull
    public static MainFragment$InnerFragmentArgs fromBundle(Bundle bundle) {
        MainFragment$InnerFragmentArgs result = new MainFragment$InnerFragmentArgs();
        bundle.setClassLoader(MainFragment$InnerFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("mainArg")) {
            result.mainArg = bundle.getString("mainArg");
            if (result.mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
        } else {
            throw new IllegalArgumentException("Required argument \"mainArg\" is missing and does not have an android:defaultValue");
        }
        return result;
    }

    @NonNull
    public String getMainArg() {
        return mainArg;
    }

    @NonNull
    public Bundle toBundle() {
        Bundle __outBundle = new Bundle();
        __outBundle.putString("mainArg", this.mainArg);
        return __outBundle;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        MainFragment$InnerFragmentArgs that = (MainFragment$InnerFragmentArgs) object;
        if (mainArg != null ? !mainArg.equals(that.mainArg) : that.mainArg != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mainArg != null ? mainArg.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MainFragment$InnerFragmentArgs{"
                + "mainArg=" + mainArg
                + "}";
    }

    public static class Builder {
        @NonNull
        private String mainArg;

        public Builder(MainFragment$InnerFragmentArgs original) {
            this.mainArg = original.mainArg;
        }

        public Builder(@NonNull String mainArg) {
            this.mainArg = mainArg;
            if (this.mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
        }

        @NonNull
        public MainFragment$InnerFragmentArgs build() {
            MainFragment$InnerFragmentArgs result = new MainFragment$InnerFragmentArgs();
            result.mainArg = this.mainArg;
            return result;
        }

        @NonNull
        public Builder setMainArg(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"mainArg\" is marked as non-null but was passed a null value.");
            }
            this.mainArg = mainArg;
            return this;
        }

        @NonNull
        public String getMainArg() {
            return mainArg;
        }
    }
}