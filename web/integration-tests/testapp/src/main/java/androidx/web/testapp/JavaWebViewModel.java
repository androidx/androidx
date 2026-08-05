/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.web.testapp;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.web.WebContent;
import androidx.web.WebFeature;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A ViewModel that manages the lifecycle of {@link WebContent} for {@link JavaWebActivity}.
 * It ensures the WebContent instance is retained across configuration changes and
 * properly closed when the ViewModel is cleared.
 */
public class JavaWebViewModel extends AndroidViewModel {

    private WebContent mWebContent;
    private boolean mInitialUrlLoaded;

    @SuppressWarnings("RestrictedApiAndroidX")
    public JavaWebViewModel(@NonNull Application application) {
        super(application);
        if (WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT)) {
            mWebContent = new WebContent.Builder(application).build();
        }
    }

    @Nullable
    public WebContent getWebContent() {
        return mWebContent;
    }

    public boolean isInitialUrlLoaded() {
        return mInitialUrlLoaded;
    }

    public void setInitialUrlLoaded(boolean loaded) {
        mInitialUrlLoaded = loaded;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mWebContent != null) {
            mWebContent.close();
            mWebContent = null;
        }
    }
}
