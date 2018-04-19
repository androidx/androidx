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
import androidx.navigation.NavDirections;
import java.lang.String;

public class MainFragmentDirections {
    public static Previous previous(String arg1, String arg2) {
        return new Previous(arg1, arg2);
    }

    public static Next next(String main) {
        return new Next(main);
    }

    public static class Previous implements NavDirections {
        private String arg1;

        private String arg2;

        public Previous(String arg1, String arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public Previous setArg1(String arg1) {
            this.arg1 = arg1;
            return this;
        }

        public Previous setArg2(String arg2) {
            this.arg2 = arg2;
            return this;
        }

        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("arg1", this.arg1);
            __outBundle.putString("arg2", this.arg2);
            return __outBundle;
        }

        public int getActionId() {
            return a.b.R.id.previous;
        }
    }

    public static class Next implements NavDirections {
        private String main;

        private String optional = "bla";

        public Next(String main) {
            this.main = main;
        }

        public Next setMain(String main) {
            this.main = main;
            return this;
        }

        public Next setOptional(String optional) {
            this.optional = optional;
            return this;
        }

        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("main", this.main);
            __outBundle.putString("optional", this.optional);
            return __outBundle;
        }

        public int getActionId() {
            return a.b.R.id.next;
        }
    }
}