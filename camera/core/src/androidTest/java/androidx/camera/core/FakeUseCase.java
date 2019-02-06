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

import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.util.Size;
import androidx.camera.core.CameraX.LensFacing;
import java.util.Map;

/** A fake {@link BaseUseCase}. */
@RestrictTo(Scope.LIBRARY_GROUP)
public class FakeUseCase extends BaseUseCase {
  private volatile boolean isCleared = false;

  /** Creates a new instance of a {@link FakeUseCase} with a given configuration. */
  protected FakeUseCase(FakeUseCaseConfiguration configuration) {
    super(configuration);
  }

  /** Creates a new instance of a {@link FakeUseCase} with a default configuration. */
  protected FakeUseCase() {
    this(new FakeUseCaseConfiguration.Builder().build());
  }

  @Override
  protected UseCaseConfiguration.Builder<?, ?, ?> getDefaultBuilder() {
    return new FakeUseCaseConfiguration.Builder()
        .setLensFacing(LensFacing.BACK)
        .setOptionUnpacker((useCaseConfig, sessionConfigBuilder) -> {});
  }

  @Override
  public void clear() {
    super.clear();
    isCleared = true;
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
