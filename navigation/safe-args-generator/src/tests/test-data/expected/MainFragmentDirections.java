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
            this.arg2 = arg2;
        }

        @NonNull
        public Previous setArg1(@NonNull String arg1) {
            this.arg1 = arg1;
            return this;
        }

        @NonNull
        public Previous setArg2(@NonNull String arg2) {
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
    }

    public static class Next implements NavDirections {
        @NonNull
        private String main;

        @NonNull
        private String optional = "bla";

        public Next(@NonNull String main) {
            this.main = main;
        }

        @NonNull
        public Next setMain(@NonNull String main) {
            this.main = main;
            return this;
        }

        @NonNull
        public Next setOptional(@NonNull String optional) {
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
    }
}