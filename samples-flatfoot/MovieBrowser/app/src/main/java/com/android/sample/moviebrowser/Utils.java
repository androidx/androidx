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
package com.android.sample.moviebrowser;

import static com.android.sample.moviebrowser.DetailsFragment.CODE_EDIT;

import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * Helper methods.
 */
public class Utils {
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
     * Shows full details of the specific movie.
     */
    public static void showDetails(Fragment fragment, MovieData data) {
        DetailsFragment detailsFragment = new DetailsFragment();
        Bundle detailsFragmentArgs = new Bundle();
        detailsFragmentArgs.putParcelable(DetailsFragment.INITIAL, data);
        detailsFragment.setArguments(detailsFragmentArgs);

        FragmentManager fragmentManager = fragment.getActivity()
                .getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, detailsFragment, "details");
        transaction.addToBackStack("details");
        transaction.commit();
    }

    /**
     * Shows UI for editing movie details.
     */
    public static void editDetails(Fragment fragment, String imdbID) {
        EditDetailsFragment editDetailsFragment = new EditDetailsFragment();
        Bundle editDetailsFragmentArgs = new Bundle();
        editDetailsFragmentArgs.putString(EditDetailsFragment.IMDB_ID, imdbID);
        editDetailsFragment.setArguments(editDetailsFragmentArgs);
        editDetailsFragment.setTargetFragment(fragment, CODE_EDIT);

        FragmentManager fragmentManager = fragment.getActivity()
                .getSupportFragmentManager();
        editDetailsFragment.show(fragmentManager, "tag");
    }
}
