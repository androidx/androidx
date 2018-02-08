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
import java.lang.IllegalArgumentException;
import java.lang.String;

public class MainFragmentArgs {
    private String main;

    private int optional;

    private MainFragmentArgs() {
    }

    public static MainFragmentArgs fromBundle(Bundle bundle) {
        MainFragmentArgs result = new MainFragmentArgs();
        if (bundle.containsKey("main")) {
            result.main = bundle.getString("main");
        } else {
            throw new IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("optional")) {
            result.optional = bundle.getInt("optional");
        } else {
            result.optional = -1;
        }
        return result;
    }

    public String getMain() {
        return main;
    }

    public int getOptional() {
        return optional;
    }
}
