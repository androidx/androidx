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

package android.support.v4.widget;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * Helper for accessing features in {@link SearchView}
 * introduced after API level 4 in a backwards compatible fashion.
 *
 * @deprecated Use {@link SearchView} directly.
 */
@Deprecated
public final class SearchViewCompat {
    private static void checkIfLegalArg(View searchView) {
        if (searchView == null) {
            throw new IllegalArgumentException("searchView must be non-null");
        }
        if (!(searchView instanceof SearchView)) {
            throw new IllegalArgumentException("searchView must be an instance of "
                    + "android.widget.SearchView");
        }
    }

    private SearchViewCompat(Context context) {
        /* Hide constructor */
    }

    /**
     * Creates a new SearchView.
     *
     * @param context The Context the view is running in.
     * @return A SearchView instance if the class is present on the current
     *         platform, null otherwise.
     *
     * @deprecated Use {@link SearchView} constructor directly.
     */
    @Deprecated
    public static View newSearchView(Context context) {
        return new SearchView(context);
    }

    /**
     * Sets the SearchableInfo for this SearchView. Properties in the SearchableInfo are used
     * to display labels, hints, suggestions, create intents for launching search results screens
     * and controlling other affordances such as a voice button.
     *
     * @param searchView The SearchView to operate on.
     * @param searchableComponent The application component whose
     * {@link android.app.SearchableInfo} should be loaded and applied to
     * the SearchView.
     *
     * @deprecated Use {@link SearchView#setSearchableInfo(SearchableInfo)} directly.
     */
    @Deprecated
    public static void setSearchableInfo(View searchView, ComponentName searchableComponent) {
        checkIfLegalArg(searchView);
        SearchManager searchManager = (SearchManager)
                searchView.getContext().getSystemService(Context.SEARCH_SERVICE);
        ((SearchView) searchView).setSearchableInfo(
                searchManager.getSearchableInfo(searchableComponent));
    }

    /**
     * Sets the IME options on the query text field.  This is a no-op if
     * called on pre-{@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH}
     * platforms.
     *
     * @see TextView#setImeOptions(int)
     * @param searchView The SearchView to operate on.
     * @param imeOptions the options to set on the query text field
     *
     * @deprecated Use {@link SearchView#setImeOptions(int)} directly.
     */
    @Deprecated
    public static void setImeOptions(View searchView, int imeOptions) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setImeOptions(imeOptions);
    }

    /**
     * Sets the input type on the query text field.  This is a no-op if
     * called on pre-{@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH}
     * platforms.
     *
     * @see TextView#setInputType(int)
     * @param searchView The SearchView to operate on.
     * @param inputType the input type to set on the query text field
     *
     * @deprecated Use {@link SearchView#setInputType(int)} directly.
     */
    @Deprecated
    public static void setInputType(View searchView, int inputType) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setInputType(inputType);
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param searchView The SearchView in which to register the listener.
     * @param listener the listener object that receives callbacks when the user performs
     *     actions in the SearchView such as clicking on buttons or typing a query.
     *
     * @deprecated Use {@link SearchView#setOnQueryTextListener(SearchView.OnQueryTextListener)}
     * directly.
     */
    @Deprecated
    public static void setOnQueryTextListener(View searchView, OnQueryTextListener listener) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setOnQueryTextListener(newOnQueryTextListener(listener));
    }

    private static SearchView.OnQueryTextListener newOnQueryTextListener(
            final OnQueryTextListener listener) {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return listener.onQueryTextSubmit(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return listener.onQueryTextChange(newText);
            }
        };
    }

    /**
     * @deprecated Use {@link SearchView.OnQueryTextListener} instead.
     */
    @Deprecated
    public static abstract class OnQueryTextListenerCompat implements OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }

    /**
     * @deprecated Use {@link SearchView.OnQueryTextListener} instead.
     */
    @Deprecated
    public interface OnQueryTextListener {
        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         *
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         *
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        boolean onQueryTextChange(String newText);
    }

    /**
     * Sets a listener to inform when the user closes the SearchView.
     *
     * @param searchView The SearchView in which to register the listener.
     * @param listener the listener to call when the user closes the SearchView.
     *
     * @deprecated Use {@link SearchView#setOnCloseListener(SearchView.OnCloseListener)} directly.
     */
    @Deprecated
    public static void setOnCloseListener(View searchView, OnCloseListener listener) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setOnCloseListener(newOnCloseListener(listener));
    }

    private static SearchView.OnCloseListener newOnCloseListener(final OnCloseListener listener) {
        return new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return listener.onClose();
            }
        };
    }

    /**
     * @deprecated Use {@link SearchView.OnCloseListener} instead.
     */
    @Deprecated
    public static abstract class OnCloseListenerCompat implements OnCloseListener {
        @Override
        public boolean onClose() {
            return false;
        }
    }

    /**
     * Callback for closing the query UI.
     *
     * @deprecated Use {@link SearchView.OnCloseListener} instead.
     */
    @Deprecated
    public interface OnCloseListener {
        /**
         * The user is attempting to close the SearchView.
         *
         * @return true if the listener wants to override the default behavior of clearing the
         * text field and dismissing it, false otherwise.
         */
        boolean onClose();
    }

    /**
     * Returns the query string currently in the text field.
     *
     * @param searchView The SearchView to operate on.
     *
     * @return the query string
     *
     * @deprecated Use {@link SearchView#getQuery()} directly.
     */
    @Deprecated
    public static CharSequence getQuery(View searchView) {
        checkIfLegalArg(searchView);
        return ((SearchView) searchView).getQuery();
    }

    /**
     * Sets a query string in the text field and optionally submits the query as well.
     *
     * @param searchView The SearchView to operate on.
     * @param query the query string. This replaces any query text already present in the
     * text field.
     * @param submit whether to submit the query right now or only update the contents of
     * text field.
     *
     * @deprecated Use {@link SearchView#setQuery(CharSequence, boolean)} directly.
     */
    @Deprecated
    public static void setQuery(View searchView, CharSequence query, boolean submit) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setQuery(query, submit);
    }

    /**
     * Sets the hint text to display in the query text field. This overrides any hint specified
     * in the SearchableInfo.
     *
     * @param searchView The SearchView to operate on.
     * @param hint the hint text to display
     *
     * @deprecated Use {@link SearchView#setQueryHint(CharSequence)} directly.
     */
    @Deprecated
    public static void setQueryHint(View searchView, CharSequence hint) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setQueryHint(hint);
    }

    /**
     * Iconifies or expands the SearchView. Any query text is cleared when iconified. This is
     * a temporary state and does not override the default iconified state set by
     * setIconifiedByDefault(boolean). If the default state is iconified, then
     * a false here will only be valid until the user closes the field. And if the default
     * state is expanded, then a true here will only clear the text field and not close it.
     *
     * @param searchView The SearchView to operate on.
     * @param iconify a true value will collapse the SearchView to an icon, while a false will
     * expand it.
     *
     * @deprecated Use {@link SearchView#setIconified(boolean)} directly.
     */
    @Deprecated
    public static void setIconified(View searchView, boolean iconify) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setIconified(iconify);
    }

    /**
     * Returns the current iconified state of the SearchView.
     *
     * @param searchView The SearchView to operate on.
     * @return true if the SearchView is currently iconified, false if the search field is
     * fully visible.
     *
     * @deprecated Use {@link SearchView#isIconified()} directly.
     */
    @Deprecated
    public static boolean isIconified(View searchView) {
        checkIfLegalArg(searchView);
        return ((SearchView) searchView).isIconified();
    }

    /**
     * Enables showing a submit button when the query is non-empty. In cases where the SearchView
     * is being used to filter the contents of the current activity and doesn't launch a separate
     * results activity, then the submit button should be disabled.
     *
     * @param searchView The SearchView to operate on.
     * @param enabled true to show a submit button for submitting queries, false if a submit
     * button is not required.
     *
     * @deprecated Use {@link SearchView#setSubmitButtonEnabled(boolean)} directly.
     */
    @Deprecated
    public static void setSubmitButtonEnabled(View searchView, boolean enabled) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setSubmitButtonEnabled(enabled);
    }

    /**
     * Returns whether the submit button is enabled when necessary or never displayed.
     *
     * @param searchView The SearchView to operate on.
     * @return whether the submit button is enabled automatically when necessary
     *
     * @deprecated Use {@link SearchView#isSubmitButtonEnabled()} directly.
     */
    @Deprecated
    public static boolean isSubmitButtonEnabled(View searchView) {
        checkIfLegalArg(searchView);
        return ((SearchView) searchView).isSubmitButtonEnabled();
    }

    /**
     * Specifies if a query refinement button should be displayed alongside each suggestion
     * or if it should depend on the flags set in the individual items retrieved from the
     * suggestions provider. Clicking on the query refinement button will replace the text
     * in the query text field with the text from the suggestion. This flag only takes effect
     * if a SearchableInfo has been specified with {@link #setSearchableInfo(View, ComponentName)}
     * and not when using a custom adapter.
     *
     * @param searchView The SearchView to operate on.
     * @param enable true if all items should have a query refinement button, false if only
     * those items that have a query refinement flag set should have the button.
     *
     * @see SearchManager#SUGGEST_COLUMN_FLAGS
     * @see SearchManager#FLAG_QUERY_REFINEMENT
     *
     * @deprecated Use {@link SearchView#setQueryRefinementEnabled(boolean)} directly.
     */
    @Deprecated
    public static void setQueryRefinementEnabled(View searchView, boolean enable) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setQueryRefinementEnabled(enable);
    }

    /**
     * Returns whether query refinement is enabled for all items or only specific ones.
     * @param searchView The SearchView to operate on.
     * @return true if enabled for all items, false otherwise.
     *
     * @deprecated Use {@link SearchView#isQueryRefinementEnabled()} directly.
     */
    @Deprecated
    public static boolean isQueryRefinementEnabled(View searchView) {
        checkIfLegalArg(searchView);
        return ((SearchView) searchView).isQueryRefinementEnabled();
    }

    /**
     * Makes the view at most this many pixels wide
     * @param searchView The SearchView to operate on.
     *
     * @deprecated Use {@link SearchView#setMaxWidth(int)} directly.
     */
    @Deprecated
    public static void setMaxWidth(View searchView, int maxpixels) {
        checkIfLegalArg(searchView);
        ((SearchView) searchView).setMaxWidth(maxpixels);
    }
}
