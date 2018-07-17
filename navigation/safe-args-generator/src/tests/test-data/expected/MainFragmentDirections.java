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

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class MainFragmentDirections {
    @NonNull
    public static Previous previous(@NonNull String arg1, @NonNull String arg2) {
        return new Previous(arg1, arg2);
    }

    @NonNull
    public static Next next(@NonNull String main) {
        return new Next(main);
    }

    public static class Previous implements NavDirections {
        @NonNull
        private String arg1;

        @NonNull
        private String arg2;

        public Previous(@NonNull String arg1, @NonNull String arg2) {
            this.arg1 = arg1;
            if (this.arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg1\" is marked as non-null but was passed a null value.");
            }
            this.arg2 = arg2;
            if (this.arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg2\" is marked as non-null but was passed a null value.");
            }
        }

        @NonNull
        public Previous setArg1(@NonNull String arg1) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg1\" is marked as non-null but was passed a null value.");
            }
            this.arg1 = arg1;
            return this;
        }

        @NonNull
        public Previous setArg2(@NonNull String arg2) {
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg2\" is marked as non-null but was passed a null value.");
            }
            this.arg2 = arg2;
            return this;
        }

        @NonNull
        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("arg1", this.arg1);
            __outBundle.putString("arg2", this.arg2);
            return __outBundle;
        }

        public int getActionId() {
            return a.b.R.id.previous;
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
            Previous that = (Previous) object;
            if (arg1 != null ? !arg1.equals(that.arg1) : that.arg1 != null) {
                return false;
            }
            if (arg2 != null ? !arg2.equals(that.arg2) : that.arg2 != null) {
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
            result = 31 * result + (arg1 != null ? arg1.hashCode() : 0);
            result = 31 * result + (arg2 != null ? arg2.hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }
    }

    public static class Next implements NavDirections {
        @NonNull
        private String main;

        @NonNull
        private String optional = "bla";

        public Next(@NonNull String main) {
            this.main = main;
            if (this.main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
        }

        @NonNull
        public Next setMain(@NonNull String main) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            this.main = main;
            return this;
        }

        @NonNull
        public Next setOptional(@NonNull String optional) {
            if (optional == null) {
                throw new IllegalArgumentException("Argument \"optional\" is marked as non-null but was passed a null value.");
            }
            this.optional = optional;
            return this;
        }

        @NonNull
        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("main", this.main);
            __outBundle.putString("optional", this.optional);
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
            if (optional != null ? !optional.equals(that.optional) : that.optional != null) {
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
            result = 31 * result + (optional != null ? optional.hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }
    }
}