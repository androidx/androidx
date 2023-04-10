/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.androidx.widget.selection.fancy;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

abstract class DemoHolder extends RecyclerView.ViewHolder {
    DemoHolder(LinearLayout layout) {
        super(layout);
    }

    abstract void update(@NonNull Uri uri);

    @SuppressWarnings("TypeParameterUnusedInFormals")  // Convenience to avoid clumsy cast.
    static <V extends View> V inflateLayout(Context context, ViewGroup parent, int layout) {
        return (V) LayoutInflater.from(context).inflate(layout, parent, false);
    }
}
