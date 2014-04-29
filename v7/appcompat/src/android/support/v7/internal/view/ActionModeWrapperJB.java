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

package android.support.v7.internal.view;

import android.content.Context;
import android.view.ActionMode;

/**
 * @hide
 */
public class ActionModeWrapperJB extends ActionModeWrapper {

    public ActionModeWrapperJB(Context context, android.view.ActionMode frameworkActionMode) {
        super(context, frameworkActionMode);
    }

    @Override
    public boolean getTitleOptionalHint() {
        return mWrappedObject.getTitleOptionalHint();
    }

    @Override
    public void setTitleOptionalHint(boolean titleOptional) {
        mWrappedObject.setTitleOptionalHint(titleOptional);
    }

    @Override
    public boolean isTitleOptional() {
        return mWrappedObject.isTitleOptional();
    }

    /**
     * @hide
     */
    public static class CallbackWrapper extends ActionModeWrapper.CallbackWrapper {

        public CallbackWrapper(Context context, Callback supportCallback) {
            super(context, supportCallback);
        }

        @Override
        protected ActionModeWrapper createActionModeWrapper(Context context, ActionMode mode) {
            return new ActionModeWrapperJB(context, mode);
        }
    }

}
