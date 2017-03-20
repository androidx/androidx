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

package com.android.sample.githubbrowser.databinding;

import android.databinding.BindingAdapter;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataBindingAdapters {
    private static SimpleDateFormat sJsonDateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
            Locale.ENGLISH);

    RequestManager mRequestManager;
    public DataBindingAdapters(RequestManager requestManager) {
        mRequestManager = requestManager;
    }

    @SuppressWarnings("WeakerAccess")
    @BindingAdapter({"imageUrl"})
    public void loadImage(ImageView imageView, String url) {
        if (!TextUtils.isEmpty(url)) {
            mRequestManager.load(url).fitCenter().crossFade().into(imageView);
        } else {
            imageView.setImageBitmap(null);
        }
    }

    /**
     * Displays formatted date given a JSON-originating date.
     */
    @BindingAdapter("jsonDate")
    public static void formatDate(TextView textView, String jsonDate) {
        if (TextUtils.isEmpty(jsonDate)) {
            return;
        }
        try {
            textView.setText(SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(
                    sJsonDateParser.parse(jsonDate)));
        } catch (ParseException pe) {
            // WTF
        }
    }

    /**
     * Displays formatted date given a JSON-originating date.
     */
    @BindingAdapter({"stringRes", "jsonDate"})
    public static void formatDateWithString(TextView textView, @StringRes int stringRes,
            String jsonDate) {
        if (TextUtils.isEmpty(jsonDate)) {
            return;
        }
        try {
            Date date = sJsonDateParser.parse(jsonDate);
            String formattedDate = SimpleDateFormat.getDateInstance(
                    SimpleDateFormat.SHORT).format(date);
            textView.setText(textView.getResources().getString(stringRes,
                    formattedDate));
        } catch (ParseException pe) {
            // WTF
        }
    }

    @BindingAdapter("visibleInvisible")
    public static void changeVisiblity(View view, boolean value) {
        view.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
    }

    @BindingAdapter("visibleGone")
    public static void showHide(View view, boolean value) {
        view.setVisibility(value ? View.VISIBLE : View.GONE);
    }
}
