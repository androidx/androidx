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
 * limitations under the License
 */

package android.support.v4.app;

import android.support.annotation.CallSuper;
import android.support.v4.os.BuildCompat;

/**
 * Base class for {@code FragmentActivity} to be able to use v24 APIs.
 */
abstract class BaseFragmentActivityApi24 extends BaseFragmentActivityHoneycomb {

    @Override
    @CallSuper
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (BuildCompat.isAtLeastN()) {
            super.onMultiWindowModeChanged(isInMultiWindowMode);
        }
        dispatchFragmentsOnMultiWindowModeChanged(isInMultiWindowMode);
    }

    @Override
    @CallSuper
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (BuildCompat.isAtLeastN()) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        }
        dispatchFragmentsOnPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    abstract void dispatchFragmentsOnMultiWindowModeChanged(boolean isInMultiWindowMode);

    abstract void dispatchFragmentsOnPictureInPictureModeChanged(boolean isInPictureInPictureMode);

}
