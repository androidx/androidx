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

package androidx.camera.core;

import android.util.Size;
import androidx.camera.core.CameraX.LensFacing;
import java.util.Map;

/**
 * A second fake {@link BaseUseCase}.
 *
 * <p>This is used to complement the {@link FakeUseCase} for testing instances where a use case of
 * different type is created.
 */
class FakeOtherUseCase extends BaseUseCase {
  private volatile boolean isCleared = false;

  /** Creates a new instance of a {@link FakeOtherUseCase} with a given configuration. */
  FakeOtherUseCase(FakeOtherUseCaseConfiguration configuration) {
    super(configuration);
  }

  /** Creates a new instance of a {@link FakeOtherUseCase} with a default configuration. */
  FakeOtherUseCase() {
    this(new FakeOtherUseCaseConfiguration.Builder().build());
  }

  @Override
  public void clear() {
    super.clear();
    isCleared = true;
  }

  @Override
  protected UseCaseConfiguration.Builder<?, ?, ?> getDefaultBuilder() {
    return new FakeOtherUseCaseConfiguration.Builder().setLensFacing(LensFacing.BACK);
  }

  @Override
  protected Map<String, Size> onSuggestedResolutionUpdated(
      Map<String, Size> suggestedResolutionMap) {
    return suggestedResolutionMap;
  }

  /** Returns true if {@link #clear()} has been called previously. */
  public boolean isCleared() {
    return isCleared;
  }
}
