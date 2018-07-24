/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.recyclerview.widget;


import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class that can collect list of view attach and detach events so that we can assert on them
 */
public class AttachDetachCollector implements RecyclerView.OnChildAttachStateChangeListener {
    private final List<View> mAttached = new ArrayList<>();
    private final List<View> mDetached = new ArrayList<>();

    public AttachDetachCollector(RecyclerView recyclerView) {
        recyclerView.addOnChildAttachStateChangeListener(this);
    }

    @Override
    public void onChildViewAttachedToWindow(@NonNull View view) {
        mAttached.add(view);
    }

    @Override
    public void onChildViewDetachedFromWindow(@NonNull View view) {
        mDetached.add(view);
    }

    public void reset() {
        mAttached.clear();
        mDetached.clear();
    }

    public List<View> getAttached() {
        return mAttached;
    }

    public List<View> getDetached() {
        return mDetached;
    }
}
