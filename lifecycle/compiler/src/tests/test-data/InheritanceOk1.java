/*
 * Copyright (C) 2016 The Android Open Source Project
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

package foo;

import static com.android.support.lifecycle.Lifecycle.STOPPED;

import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnState;

class Base1 {
    @OnState(STOPPED)
    public void onStop(LifecycleProvider provider, int prevstate){}
}

class Proxy extends Base1 { }

class Derived1 extends Proxy {
    @OnState(STOPPED)
    public void onStop2(LifecycleProvider provider, int prevstate){}
}

class Derived2 extends Proxy {
    @OnState(STOPPED)
    public void onStop2(LifecycleProvider provider, int prevstate){}
}

class Base2 {
    @OnState(STOPPED)
    public void onStop(LifecycleProvider provider, int prevstate){}
}

class Derived3 extends Base2 {
    @OnState(STOPPED)
    public void onStop2(LifecycleProvider provider, int prevstate){}
}
