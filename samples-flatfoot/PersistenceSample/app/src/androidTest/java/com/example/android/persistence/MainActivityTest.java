/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.persistence;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.Nullable;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;

import com.example.android.persistence.db.entity.ProductEntity;
import com.example.android.persistence.viewmodel.ProductListViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    private SimpleIdlingResource idlingRes = new SimpleIdlingResource();

    @Before
    public void idlingResourceSetup() {

        Espresso.registerIdlingResources(idlingRes);
        // There's always
        idlingRes.setIdleNow(false);

        ProductListViewModel productListViewModel = getProductListViewModel();

        // Subscribe to ProductListViewModel's products list observable to figure out when the
        // app is idle.
        productListViewModel.getProducts().observeForever(new Observer<List<ProductEntity>>() {
            @Override
            public void onChanged(@Nullable List<ProductEntity> productEntities) {
                if (productEntities != null) {
                    idlingRes.setIdleNow(true);
                }
            }
        });
    }

    @Test
    public void clickOnFirstItem_opensComments() {
        // When clicking on the first product
        onView(withContentDescription(R.string.cd_products_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Then the second screen with the comments should appear.
        onView(withContentDescription(R.string.cd_comments_list))
                .check(matches(isDisplayed()));

        // Then the second screen with the comments should appear.
        onView(withContentDescription(R.string.cd_product_name))
                .check(matches(not(withText(""))));

    }

    /** Gets the ViewModel for the current fragment */
    private ProductListViewModel getProductListViewModel() {
        MainActivity activity = mActivityRule.getActivity();

        Fragment productListFragment = activity.getSupportFragmentManager()
                .findFragmentByTag(ProductListFragment.TAG);

        return ViewModelProviders.of(productListFragment)
                .get(ProductListViewModel.class);
    }

    private static class SimpleIdlingResource implements IdlingResource {

        // written from main thread, read from any thread.
        private volatile ResourceCallback mResourceCallback;

        private AtomicBoolean mIsIdleNow = new AtomicBoolean(true);

        public void setIdleNow(boolean idleNow) {
            mIsIdleNow.set(idleNow);
            if (idleNow) {
                mResourceCallback.onTransitionToIdle();
            }
        }

        @Override
        public String getName() {
            return "Simple idling resource";
        }

        @Override
        public boolean isIdleNow() {
            return mIsIdleNow.get();
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            mResourceCallback = callback;
        }
    }
}