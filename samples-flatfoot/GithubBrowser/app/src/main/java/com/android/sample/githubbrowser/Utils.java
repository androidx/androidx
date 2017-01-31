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
package com.android.sample.githubbrowser;

import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.data.RepositoryData;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper methods.
 */
public class Utils {
    private static SimpleDateFormat sJsonDateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
            Locale.ENGLISH);

    private Utils() {
        // This is here to prevent object initialization
    }

    /**
     * Loads the image bitmap using Glide. This method is marked to be a binding adapter
     * so that it can be used from our XML layouts.
     */
    @BindingAdapter({"url", "fragment"})
    public static void loadImage(ImageView imageView, String url, Fragment fragment) {
        Glide.with(fragment).load(url).fitCenter().crossFade().into(imageView);
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

    /**
     * Shows full details of the specific repository.
     */
    public static void showRepoDetails(Fragment fragment, RepositoryData data) {
        RepositoryDetailsFragment repoDetailsFragment = new RepositoryDetailsFragment();
        Bundle detailsFragmentArgs = new Bundle();
        detailsFragmentArgs.putString(RepositoryDetailsFragment.REPO_ID, data.id);
        detailsFragmentArgs.putString(RepositoryDetailsFragment.REPO_FULL_NAME, data.full_name);
        repoDetailsFragment.setArguments(detailsFragmentArgs);

        FragmentManager fragmentManager = fragment.getActivity()
                .getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, repoDetailsFragment, "repoDetails:" + data.id);
        transaction.addToBackStack("repoDetails:" + data.id);
        transaction.commit();
    }

    /**
     * Shows full details of the specific user.
     */
    public static void showUserDetails(Fragment fragment, ContributorData data) {
        UserDetailsFragment userDetailsFragment = new UserDetailsFragment();
        Bundle detailsFragmentArgs = new Bundle();
        detailsFragmentArgs.putParcelable(UserDetailsFragment.INITIAL, data);
        userDetailsFragment.setArguments(detailsFragmentArgs);

        FragmentManager fragmentManager = fragment.getActivity()
                .getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, userDetailsFragment, "userDetails:" + data.login);
        transaction.addToBackStack("userDetails:" + data.login);
        transaction.commit();
    }

    /**
     * Shows UI for editing user details.
     */
    public static void editUserDetails(Fragment fragment, String login) {
        EditUserDetailsFragment editUserDetailsFragment = new EditUserDetailsFragment();
        Bundle editUserDetailsFragmentArgs = new Bundle();
        editUserDetailsFragmentArgs.putString(EditUserDetailsFragment.LOGIN, login);
        editUserDetailsFragment.setArguments(editUserDetailsFragmentArgs);
        editUserDetailsFragment.setTargetFragment(fragment, UserDetailsFragment.CODE_EDIT);

        FragmentManager fragmentManager = fragment.getActivity()
                .getSupportFragmentManager();
        editUserDetailsFragment.show(fragmentManager, "editUser:" + login);
    }
}
