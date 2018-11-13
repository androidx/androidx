/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.fragment.app.testing;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;

/**
 * A minimum example fragment which doesn't have default constructor. You have to use
 * {@link FragmentFactory} to instantiate this fragment.
 */
public class NoDefaultConstructorFragment extends Fragment {

    @NonNull private final String mName;

    public NoDefaultConstructorFragment(@NonNull String name) {
        this.mName = name;
    }

    @NonNull
    public String getName() {
        return mName;
    }
}
