/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testapp.timingapp;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.Lifecycle.State;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

/**
 * A customized lifecycle owner which obeys the lifecycle transition rules.
 */
public final class CustomLifecycle implements LifecycleOwner {
  private final LifecycleRegistry lifecycleRegistry;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public CustomLifecycle() {
    lifecycleRegistry = new LifecycleRegistry(this);
    lifecycleRegistry.markState(Lifecycle.State.INITIALIZED);
    lifecycleRegistry.markState(Lifecycle.State.CREATED);
  }

  @NonNull
  @Override
  public Lifecycle getLifecycle() {
    return lifecycleRegistry;
  }

  public void doOnResume() {
    if (Looper.getMainLooper() != Looper.myLooper()) {
      mainHandler.post(() -> doOnResume());
      return;
    }
    lifecycleRegistry.markState(State.RESUMED);
  }

  public void doDestroyed() {
    if (Looper.getMainLooper() != Looper.myLooper()) {
      mainHandler.post(() -> doDestroyed());
      return;
    }
    lifecycleRegistry.markState(State.DESTROYED);
  }
}
