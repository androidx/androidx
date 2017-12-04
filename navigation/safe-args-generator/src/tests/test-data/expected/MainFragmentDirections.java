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

import android.arch.navigation.NavDirections;
import android.arch.navigation.NavOptions;
import android.os.Bundle;
import java.lang.String;

public class MainFragmentDirections {
    public static Previous previous(String arg1, String arg2) {
        return new Previous(arg1, arg2);
    }

    public static Next next(String main) {
        return new Next(main);
    }

    public static class Previous implements NavDirections {
        private final String arg1;

        private final String arg2;

        public Previous(String arg1, String arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("arg1", arg1);
            __outBundle.putString("arg2", arg2);
            return __outBundle;
        }

        public int getDestinationId() {
            return a.b.R.id.destB;
        }

        public NavOptions getOptions() {
            return null;
        }
    }

    public static class Next implements NavDirections {
        private final String main;

        private String optional;

        public Next(String main) {
            this.main = main;
        }

        public Next setOptional(String optional) {
            this.optional = optional;
            return this;
        }

        public Bundle getArguments() {
            Bundle __outBundle = new Bundle();
            __outBundle.putString("main", main);
            __outBundle.putString("optional", optional);
            return __outBundle;
        }

        public int getDestinationId() {
            return a.b.R.id.destA;
        }

        public NavOptions getOptions() {
            return null;
        }
    }
}