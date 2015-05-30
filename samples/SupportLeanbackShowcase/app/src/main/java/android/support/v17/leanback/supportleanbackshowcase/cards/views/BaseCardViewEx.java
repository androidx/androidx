/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package android.support.v17.leanback.supportleanbackshowcase.cards.views;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.ResourceCache;
import android.support.v17.leanback.widget.BaseCardView;
import android.view.View;

/**
 * This class is an extension for the BaseCardView which is focusable by default. This behavior has
 * to be merged into the original BaseCardView at some point in the development. After merging those
 * two classes, this one, the BaseCardViewEx, can be removed.
 */
public class BaseCardViewEx extends BaseCardView {

    protected Context mContext;
    private ResourceCache mResourceCache = new ResourceCache();
    private OnActivateStateChangeHandler mActivationCallback;


    public BaseCardViewEx(Context context) {
        super(context);
        mContext = context;
        setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);

        // TODO: @hahnr BaseCardView should be focusable by default. Merge!
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (mActivationCallback != null)
            mActivationCallback.onActivateStateChanged(this, activated);
    }

    public void setOnActivateStateChangeHandler(OnActivateStateChangeHandler handler) {
        mActivationCallback = handler;
    }

    public <ViewType extends View> ViewType getViewById(int resId) {
        return mResourceCache.getViewById(this, resId);
    }

}
