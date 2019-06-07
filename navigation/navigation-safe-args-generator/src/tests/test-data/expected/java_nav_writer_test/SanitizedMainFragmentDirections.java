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
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class SanitizedMainFragmentDirections {
    private SanitizedMainFragmentDirections() {
    }

    @NonNull
    public static PreviousAction previousAction(@NonNull String arg1, @NonNull String arg2) {
        return new PreviousAction(arg1, arg2);
    }

    @NonNull
    public static NextAction nextAction(@NonNull String mainArg) {
        return new NextAction(mainArg);
    }

    public static class PreviousAction implements NavDirections {
        private final HashMap arguments = new HashMap();

        private PreviousAction(@NonNull String arg1, @NonNull String arg2) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg_1\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg_1", arg1);
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg.2\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg.2", arg2);
        }

        @NonNull
        public PreviousAction setArg1(@NonNull String arg1) {
            if (arg1 == null) {
                throw new IllegalArgumentException("Argument \"arg_1\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg_1", arg1);
            return this;
        }

        @NonNull
        public PreviousAction setArg2(@NonNull String arg2) {
            if (arg2 == null) {
                throw new IllegalArgumentException("Argument \"arg.2\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("arg.2", arg2);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        @NonNull
        public Bundle getArguments() {
            Bundle __result = new Bundle();
            if (arguments.containsKey("arg_1")) {
                String arg1 = (String) arguments.get("arg_1");
                __result.putString("arg_1", arg1);
            }
            if (arguments.containsKey("arg.2")) {
                String arg2 = (String) arguments.get("arg.2");
                __result.putString("arg.2", arg2);
            }
            return __result;
        }

        @Override
        public int getActionId() {
            return R.id.previous_action;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getArg1() {
            return (String) arguments.get("arg_1");
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getArg2() {
            return (String) arguments.get("arg.2");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            PreviousAction that = (PreviousAction) object;
            if (arguments.containsKey("arg_1") != that.arguments.containsKey("arg_1")) {
                return false;
            }
            if (getArg1() != null ? !getArg1().equals(that.getArg1()) : that.getArg1() != null) {
                return false;
            }
            if (arguments.containsKey("arg.2") != that.arguments.containsKey("arg.2")) {
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
            return "PreviousAction(actionId=" + getActionId() + "){"
                    + "arg1=" + getArg1()
                    + ", arg2=" + getArg2()
                    + "}";
        }
    }

    public static class NextAction implements NavDirections {
        private final HashMap arguments = new HashMap();

        private NextAction(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"main_arg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("main_arg", mainArg);
        }

        @NonNull
        public NextAction setMainArg(@NonNull String mainArg) {
            if (mainArg == null) {
                throw new IllegalArgumentException("Argument \"main_arg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("main_arg", mainArg);
            return this;
        }

        @NonNull
        public NextAction setOptionalArg(@NonNull String optionalArg) {
            if (optionalArg == null) {
                throw new IllegalArgumentException("Argument \"optional.arg\" is marked as non-null but was passed a null value.");
            }
            this.arguments.put("optional.arg", optionalArg);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        @NonNull
        public Bundle getArguments() {
            Bundle __result = new Bundle();
            if (arguments.containsKey("main_arg")) {
                String mainArg = (String) arguments.get("main_arg");
                __result.putString("main_arg", mainArg);
            }
            if (arguments.containsKey("optional.arg")) {
                String optionalArg = (String) arguments.get("optional.arg");
                __result.putString("optional.arg", optionalArg);
            }
            return __result;
        }

        @Override
        public int getActionId() {
            return R.id.next_action;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getMainArg() {
            return (String) arguments.get("main_arg");
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public String getOptionalArg() {
            return (String) arguments.get("optional.arg");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            NextAction that = (NextAction) object;
            if (arguments.containsKey("main_arg") != that.arguments.containsKey("main_arg")) {
                return false;
            }
            if (getMainArg() != null ? !getMainArg().equals(that.getMainArg()) : that.getMainArg() != null) {
                return false;
            }
            if (arguments.containsKey("optional.arg") != that.arguments.containsKey("optional.arg")) {
                return false;
            }
            if (getOptionalArg() != null ? !getOptionalArg().equals(that.getOptionalArg()) : that.getOptionalArg() != null) {
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
            result = 31 * result + (getMainArg() != null ? getMainArg().hashCode() : 0);
            result = 31 * result + (getOptionalArg() != null ? getOptionalArg().hashCode() : 0);
            result = 31 * result + getActionId();
            return result;
        }

        @Override
        public String toString() {
            return "NextAction(actionId=" + getActionId() + "){"
                    + "mainArg=" + getMainArg()
                    + ", optionalArg=" + getOptionalArg()
                    + "}";
        }
    }
}