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

package bar;

import com.android.support.lifecycle.GenericLifecycleObserver;
import com.android.support.lifecycle.LifecycleProvider;
import foo.DifferentPackagesBase1_LifecycleAdapter;
import java.lang.Object;
import java.lang.Override;

public class DifferentPackagesDerived1_LifecycleAdapter implements GenericLifecycleObserver {
    final DifferentPackagesDerived1 mReceiver;

    DifferentPackagesDerived1_LifecycleAdapter(DifferentPackagesDerived1 receiver) {
        this.mReceiver = receiver;
    }

    @Override
    public void onStateChanged(LifecycleProvider provider, int event) {
        if ((event & 8192) != 0) {
            DifferentPackagesBase1_LifecycleAdapter.__synthetic_onStop(mReceiver, provider, event);
            mReceiver.onStop2(provider, event);
        }
    }

    public Object getReceiver() {
        return mReceiver;
    }
}