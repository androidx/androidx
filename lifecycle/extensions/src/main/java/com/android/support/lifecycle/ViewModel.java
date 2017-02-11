/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

import android.app.Activity;

/**
 * Marks a class as a ViewModel that can be retrieved from ViewModelStore.
 *
 * A ViewModel is always created in association with a LifecycleProvider and will be retained
 * as long as the scope (LifecycleProvider) is alive. E.g. if it is an activity, until it is
 * finished or the process is killed.
 * <p>
 * This <b>doesn't</b> mean that ViewModel will be destroyed after {@link Activity#onDestroy()} is
 * called. If the activity is recreated due to a configuration change (e.g. rotation), ViewModel
 * <b>won't</b> be recreated. The replacement activity will be given the same ViewHolder instance.
 * <p>
 * The purpose of the ViewModel is to acquire and keep the information that is necessary for an
 * Activity or a Fragment. The Activity or the Fragment should be able to observe changes in the
 * ViewModel. ViewModels usually expose this information via {@link LiveData} or Android Data
 * Binding. You can also use any observability construct from you favorite framework.
 * <p>
 * ViewModel's only responsibility is to manage the data for the UI. It <b>should never</b> access
 * your view hierarchy or hold a reference back to the Activity or the Fragment.
 * <p>
 * Typical usage from an Activity standpoint would be:
 * <pre>
 * class UserActivity extends Activity {
 *
 *     {@literal @}Override
 *     protected void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.user_activity_layout);
 *         final UserModel viewModel = ViewModelStore.get(this, "userModel", UserModel.class);
 *         viewModel.userLiveData.observer(this, new Observer<User>() {
 *            {@literal @}Override
 *             public void onChanged(@Nullable User data) {
 *                 // update ui.
 *             }
 *         });
 *         findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
 *             {@literal @}Override
 *             public void onClick(View v) {
 *                  // calculate changes
 *                  // Changes changes = ...;
 *                  viewModel.reactOnChanges(changes);
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * ViewModel would be:
 * <pre>
 * class UserModel extends ViewModel {
 *     LiveData<User> userLiveData = new LiveData<>();
 *
 *     UserModel() {
 *         // trigger user load.
 *     }
 *
 *     void reactOnChanges(Changes changes) {
 *         // trigger updates, that should later result in
 *         // userLiveData.setValue(updatedUser) call;
 *     }
 * }
 * </pre>
 *
 * <p>
 * ViewModels can also be used as a communication layer between different Fragments of an Activity.
 * Each Fragment can acquire the ViewModel using the same key via their Activity. This allows
 * communication between Fragments in a de-coupled fashion such that they never need to talk to
 * the other Fragment directly.
 * <pre>
 * class MyFragment extends Fragment {
 *     void onStart() {
 *         UserModel userModel = ViewModelStore.get(getActivity(), "sharedModel", UserModel.class);
 *     }
 * }
 * </pre>
 * </>
 */
public abstract class ViewModel {

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     * <p>
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this object.
     */
    protected void onCleared() {
    }
}
