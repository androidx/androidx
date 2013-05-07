/*
 * Copyright (C) 2013 The Android Open Source Project
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


package android.support.v7.app;

import android.view.MotionEvent;
import android.view.Window;

class ActionBarActivityDelegateHCMR1 extends ActionBarActivityDelegateHC {

    ActionBarActivityDelegateHCMR1(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    Window.Callback createWindowCallbackWrapper(Window.Callback cb) {
        return new WindowCallbackWrapperHCMR1(cb);
    }

    class WindowCallbackWrapperHCMR1 extends WindowCallbackWrapper {

        public WindowCallbackWrapperHCMR1(Window.Callback wrapped) {
            super(wrapped);
        }

        /*
         * This method didn't exist before HC-MR1 so we have this new version here.
         */
        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return mWrapped.dispatchGenericMotionEvent(event);
        }
    }
}
