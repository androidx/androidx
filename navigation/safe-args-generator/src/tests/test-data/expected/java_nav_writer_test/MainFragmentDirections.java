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
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class MainFragmentDirections {
    private MainFragmentDirections() {
    }

    @NonNull
    public static Previous previous(@NonNull String arg1, @NonNull String arg2) {
        return new Previous(arg1, arg2);
    }

    @NonNull
    public static Next next(@NonNull String main) {
        return new Next(main);
    }

    public static class Previous implements NavDirections {
        private final HashMap arguments = new HashMap();

        private Previous(@NonNull String arg1, @NonNull String arg2) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg1\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg1", arg1);
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg2\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg2", arg2);
        }

        @NonNull
        public Previous setArg1(@NonNull String arg1) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg1\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg1", arg1);
            return this;
        }

        @NonNull
        public Previous setArg2(@NonNull String arg2) {
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg2\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg2", arg2);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        @NonNull
        public Bundle getArguments() {
            Bundle __result = new Bundle();
            if (arguments.containsKey("arg1")) {
                String arg1 = (String) arguments.get("arg1");
                __result.putString("arg1", arg1);
            }
            if (arguments.containsKey("arg2")) {
                String arg2 = (String) arguments.get("arg2");
                __result.putString("arg2", arg2);
            }
            return __result;
        }

        @Override
        public int getActionId() {
            return R.id.previous;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getArg1() {
            return (String) arguments.get("arg1");
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getArg2() {
            return (String) arguments.get("arg2");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            Previous that = (Previous) object;
            if (arguments.containsKey("arg1") != that.arguments.containsKey("arg1")) {
                return false;
            }
            if (getArg1() != null ? !getArg1().equals(that.getArg1()) : that.getArg1() != null) {
                return false;
            }
            if (arguments.containsKey("arg2") != that.arguments.containsKey("arg2")) {
                return false;
            }
            if (getArg2() != null ? !getArg2().equals(that.getArg2()) : that.getArg2() != null) {
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
            result = 31 * result + (getArg1() != null ? getArg1().hashCode() : 0);
            result = 31 * result + (getArg2() != null ? getArg2().hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "Previous(actionId=" + getActionId() + "){"
                    + "arg1=" + getArg1()
                    + ", arg2=" + getArg2()
                    + "}";
        }
    }

    public static class Next implements NavDirections {
        private final HashMap arguments = new HashMap();

        private Next(@NonNull String main) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("main", main);
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
        public Next setOptional(@NonNull String optional) {
            if (optional == null) {
                throw new IllegalArgumentException("Argument \"optional\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("optional", optional);
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
            if (arguments.containsKey("optional")) {
                String optional = (String) arguments.get("optional");
                __result.putString("optional", optional);
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
        @NonNull
        public String getOptional() {
            return (String) arguments.get("optional");
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
            if (arguments.containsKey("optional") != that.arguments.containsKey("optional")) {
                return false;
            }
            if (getOptional() != null ? !getOptional().equals(that.getOptional()) : that.getOptional() != null) {
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
            result = 31 * result + (getOptional() != null ? getOptional().hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "Next(actionId=" + getActionId() + "){"
                    + "main=" + getMain()
                    + ", optional=" + getOptional()
                    + "}";
        }
    }
}