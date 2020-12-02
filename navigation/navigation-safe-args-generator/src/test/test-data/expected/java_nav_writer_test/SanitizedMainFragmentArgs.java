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
import androidx.navigation.NavArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class SanitizedMainFragmentArgs implements NavArgs {
    private final HashMap arguments = new HashMap();

    private SanitizedMainFragmentArgs() {
    }

    private SanitizedMainFragmentArgs(HashMap argumentsMap) {
        this.arguments.putAll(argumentsMap);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static SanitizedMainFragmentArgs fromBundle(@NonNull Bundle bundle) {
        SanitizedMainFragmentArgs __result = new SanitizedMainFragmentArgs();
        bundle.setClassLoader(SanitizedMainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("name.with.dot")) {
            int nameWithDot;
            nameWithDot = bundle.getInt("name.with.dot");
            __result.arguments.put("name.with.dot", nameWithDot);
        } else {
            throw new IllegalArgumentException("Required argument \"name.with.dot\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name_with_underscore")) {
            int nameWithUnderscore;
            nameWithUnderscore = bundle.getInt("name_with_underscore");
            __result.arguments.put("name_with_underscore", nameWithUnderscore);
        } else {
            throw new IllegalArgumentException("Required argument \"name_with_underscore\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("name with spaces")) {
            int nameWithSpaces;
            nameWithSpaces = bundle.getInt("name with spaces");
            __result.arguments.put("name with spaces", nameWithSpaces);
        } else {
            throw new IllegalArgumentException("Required argument \"name with spaces\" is missing and does not have an android:defaultValue");
        }
        return __result;
    }

    @SuppressWarnings("unchecked")
    public int getNameWithDot() {
        return (int) arguments.get("name.with.dot");
    }

    @SuppressWarnings("unchecked")
    public int getNameWithUnderscore() {
        return (int) arguments.get("name_with_underscore");
    }

    @SuppressWarnings("unchecked")
    public int getNameWithSpaces() {
        return (int) arguments.get("name with spaces");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle toBundle() {
        Bundle __result = new Bundle();
        if (arguments.containsKey("name.with.dot")) {
            int nameWithDot = (int) arguments.get("name.with.dot");
            __result.putInt("name.with.dot", nameWithDot);
        }
        if (arguments.containsKey("name_with_underscore")) {
            int nameWithUnderscore = (int) arguments.get("name_with_underscore");
            __result.putInt("name_with_underscore", nameWithUnderscore);
        }
        if (arguments.containsKey("name with spaces")) {
            int nameWithSpaces = (int) arguments.get("name with spaces");
            __result.putInt("name with spaces", nameWithSpaces);
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
        SanitizedMainFragmentArgs that = (SanitizedMainFragmentArgs) object;
        if (arguments.containsKey("name.with.dot") != that.arguments.containsKey("name.with.dot")) {
            return false;
        }
        if (getNameWithDot() != that.getNameWithDot()) {
            return false;
        }
        if (arguments.containsKey("name_with_underscore") != that.arguments.containsKey("name_with_underscore")) {
            return false;
        }
        if (getNameWithUnderscore() != that.getNameWithUnderscore()) {
            return false;
        }
        if (arguments.containsKey("name with spaces") != that.arguments.containsKey("name with spaces")) {
            return false;
        }
        if (getNameWithSpaces() != that.getNameWithSpaces()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + getNameWithDot();
        result = 31 * result + getNameWithUnderscore();
        result = 31 * result + getNameWithSpaces();
        return result;
    }

    @Override
    public String toString() {
        return "SanitizedMainFragmentArgs{"
                + "nameWithDot=" + getNameWithDot()
                + ", nameWithUnderscore=" + getNameWithUnderscore()
                + ", nameWithSpaces=" + getNameWithSpaces()
                + "}";
    }

    public static class Builder {
        private final HashMap arguments = new HashMap();

        public Builder(SanitizedMainFragmentArgs original) {
            this.arguments.putAll(original.arguments);
        }

        public Builder(int nameWithDot, int nameWithUnderscore, int nameWithSpaces) {
            this.arguments.put("name.with.dot", nameWithDot);
            this.arguments.put("name_with_underscore", nameWithUnderscore);
            this.arguments.put("name with spaces", nameWithSpaces);
        }

        @NonNull
        public SanitizedMainFragmentArgs build() {
            SanitizedMainFragmentArgs result = new SanitizedMainFragmentArgs(arguments);
            return result;
        }

        @NonNull
        public Builder setNameWithDot(int nameWithDot) {
            this.arguments.put("name.with.dot", nameWithDot);
            return this;
        }

        @NonNull
        public Builder setNameWithUnderscore(int nameWithUnderscore) {
            this.arguments.put("name_with_underscore", nameWithUnderscore);
            return this;
        }

        @NonNull
        public Builder setNameWithSpaces(int nameWithSpaces) {
            this.arguments.put("name with spaces", nameWithSpaces);
            return this;
        }

        @SuppressWarnings("unchecked")
        public int getNameWithDot() {
            return (int) arguments.get("name.with.dot");
        }

        @SuppressWarnings("unchecked")
        public int getNameWithUnderscore() {
            return (int) arguments.get("name_with_underscore");
        }

        @SuppressWarnings("unchecked")
        public int getNameWithSpaces() {
            return (int) arguments.get("name with spaces");
        }
    }
}