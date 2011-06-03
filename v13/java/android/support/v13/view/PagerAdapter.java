/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v13.view;

import android.os.Parcelable;
import android.view.View;

/**
 * Special kind of Adapter for supplying fragments to the FragmentPager.
 */
public interface PagerAdapter {
    /**
     * Return the number of fragments available.
     */
    int getCount();

    void startUpdate();

    Object instantiateItem(int viewId, int position);

    void destroyItem(int position, Object object);

    void finishUpdate();

    boolean isViewFromObject(View view, Object object);

    Parcelable saveState();

    void restoreState(Parcelable state);
}
