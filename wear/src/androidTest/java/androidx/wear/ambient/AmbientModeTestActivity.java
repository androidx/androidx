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
package androidx.wear.ambient;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class AmbientModeTestActivity extends FragmentActivity
        implements AmbientMode.AmbientCallbackProvider {
    AmbientMode.AmbientController mAmbientController;

    boolean mEnterAmbientCalled;
    boolean mUpdateAmbientCalled;
    boolean mExitAmbientCalled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAmbientController = AmbientMode.attachAmbientSupport(this);
    }

    @Override
    public AmbientMode.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientMode.AmbientCallback {

        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            mEnterAmbientCalled = true;
        }

        @Override
        public void onUpdateAmbient() {
            mUpdateAmbientCalled = true;
        }

        @Override
        public void onExitAmbient() {
            mExitAmbientCalled = true;
        }
    }

    public AmbientMode.AmbientController getAmbientController() {
        return mAmbientController;
    }

}
