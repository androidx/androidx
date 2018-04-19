/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation.testing;

import android.content.Context;

import androidx.navigation.NavGraphNavigator;
import androidx.navigation.SimpleNavigatorProvider;

/**
 * Simple NavigatorProvider that only supports &lt;navigation&gt; and &lt;test&gt; navigation
 * elements.
 */
public class TestNavigatorProvider extends SimpleNavigatorProvider {

    public TestNavigatorProvider(Context context) {
        addNavigator(new NavGraphNavigator(context));
        addNavigator(new TestNavigator());
    }
}
