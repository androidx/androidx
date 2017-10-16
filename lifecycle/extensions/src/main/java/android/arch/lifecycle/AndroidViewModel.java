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

package android.arch.lifecycle;

import android.app.Application;

/**
 * Application context aware {@link ViewModel}.
 * <p>
 * Subclasses must have a constructor which accepts {@link Application} as the only parameter.
 * <p>
 */
public class AndroidViewModel extends ViewModel {
    private Application mApplication;

    public AndroidViewModel(Application application) {
        mApplication = application;
    }

    /**
     * Return the application.
     */
    public <T extends Application> T getApplication() {
        return (T) mApplication;
    }
}
