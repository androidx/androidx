/**
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * <p>Support classes providing high level Leanback user interface building blocks:
 * fragments and helpers.</p>
 * <p>
 * Leanback fragments are available both as platform fragments (subclassed from
 * {@link android.app.Fragment android.app.Fragment}) and as support fragments (subclassed from
 * {@link android.support.v4.app.Fragment android.support.v4.app.Fragment}).  A few of the most
 * commonly used leanback fragments are described here.
 * </p>
 * <p>
 * A {@link android.support.v17.leanback.app.BrowseFragment} includes an optional “fastlane”
 * navigation side panel and a list of rows, with one-to-one correspondance between each header
 * in the fastlane and a row.  The application supplies the
 * {@link android.support.v17.leanback.widget.ObjectAdapter} containing the list of
 * rows and a {@link android.support.v17.leanback.widget.PresenterSelector} of row presenters.
 * </p>
 * <p>
 * A {@link android.support.v17.leanback.app.DetailsFragment} will typically consist of a large
 * overview of an item at the top,
 * some actions that a user can perform, and possibly rows of additional or related items.
 * The content for this fragment is specified in the same way as for the BrowseFragment, with the
 * convention that the first element in the ObjectAdapter corresponds to the overview row.
 * The {@link android.support.v17.leanback.widget.DetailsOverviewRow} and
 * {@link android.support.v17.leanback.widget.DetailsOverviewRowPresenter} provide a default template
 * for this row.
 * </p>
 * <p>
 * A {@link android.support.v17.leanback.app.PlaybackOverlayFragment} implements standard playback
 * transport controls with a Leanback
 * look and feel.  It is recommended to use an instance of the
 * {@link android.support.v17.leanback.app.PlaybackControlGlue} with the
 * PlaybackOverlayFragment.  This helper implements a standard behavior for user interaction with
 * the most commonly used controls such as fast forward and rewind.
 * </p>
 * <p>
 * A {@link android.support.v17.leanback.app.SearchFragment} allows the developer to accept a query
 * from a user and display the results
 * using the familiar list rows.
 * </p>
 * <p>
 * A {@link android.support.v17.leanback.app.GuidedStepFragment} is used to guide the user through a
 * decision or series of decisions.
 * </p>
 **/

package android.support.v17.leanback.app;
