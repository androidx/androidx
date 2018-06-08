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

import androidx.lifecycle.GeneratedAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MethodCallsLogger;
import foo.DifferentPackagesBase1_LifecycleAdapter;
import java.lang.Override;
import javax.annotation.Generated;

@Generated("androidx.lifecycle.LifecycleProcessor")
public class DifferentPackagesDerived1_LifecycleAdapter implements GeneratedAdapter {
  final DifferentPackagesDerived1 mReceiver;

  DifferentPackagesDerived1_LifecycleAdapter(DifferentPackagesDerived1 receiver) {
    this.mReceiver = receiver;
  }

  @Override
  public void callMethods(LifecycleOwner owner, Lifecycle.Event event, boolean onAny,
      MethodCallsLogger logger) {
    boolean hasLogger = logger != null;
    if (onAny) {
      return;
    }
    if (event == Lifecycle.Event.ON_STOP) {
      if (!hasLogger || logger.approveCall("onStop", 2)) {
        DifferentPackagesBase1_LifecycleAdapter.__synthetic_onStop(mReceiver,owner);
      }
      if (!hasLogger || logger.approveCall("onStop2", 2)) {
        mReceiver.onStop2(owner);
      }
      return;
    }
  }
}
