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
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

/**
 * Helper for accessing features in {@link android.widget.SearchView}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class SearchViewCompat {

    interface SearchViewCompatImpl {
        View newSearchView(Context context);
        void setSearchableInfo(View searchView, ComponentName searchableComponent);
        void setImeOptions(View searchView, int imeOptions);
        void setInputType(View searchView, int inputType);
        Object newOnQueryTextListener(OnQueryTextListenerCompat listener);
        void setOnQueryTextListener(Object searchView, Object listener);
        Object newOnCloseListener(OnCloseListenerCompat listener);
        void setOnCloseListener(Object searchView, Object listener);
        CharSequence getQuery(View searchView);
        void setQuery(View searchView, CharSequence query, boolean submit);
        void setQueryHint(View searchView, CharSequence hint);
        void setIconified(View searchView, boolean iconify);
        boolean isIconified(View searchView);
        void setSubmitButtonEnabled(View searchView, boolean enabled);
        boolean isSubmitButtonEnabled(View searchView);
        void setQueryRefinementEnabled(View searchView, boolean enable);
        boolean isQueryRefinementEnabled(View searchView);
        void setMaxWidth(View searchView, int maxpixels);
    }

    static class SearchViewCompatStubImpl implements SearchViewCompatImpl {

        @Override
        public View newSearchView(Context context) {
            return null;
        }

        @Override
        public void setSearchableInfo(View searchView, ComponentName searchableComponent) {
        }

        @Override
        public void setImeOptions(View searchView, int imeOptions) {
        }

        @Override
        public void setInputType(View searchView, int inputType) {
        }

        @Override
        public Object newOnQueryTextListener(OnQueryTextListenerCompat listener) {
            return null;
        }

        @Override
        public void setOnQueryTextListener(Object searchView, Object listener) {
        }

        @Override
        public Object newOnCloseListener(OnCloseListenerCompat listener) {
            return null;
        }

        @Override
        public void setOnCloseListener(Object searchView, Object listener) {
        }

        @Override
        public CharSequence getQuery(View searchView) {
            return null;
        }

        @Override
        public void setQuery(View searchView, CharSequence query, boolean submit) {
        }

        @Override
        public void setQueryHint(View searchView, CharSequence hint) {
        }

        @Override
        public void setIconified(View searchView, boolean iconify) {
        }

        @Override
        public boolean isIconified(View searchView) {
            return true;
        }

        @Override
        public void setSubmitButtonEnabled(View searchView, boolean enabled) {
        }

        @Override
        public boolean isSubmitButtonEnabled(View searchView) {
            return false;
        }

        @Override
        public void setQueryRefinementEnabled(View searchView, boolean enable) {
        }

        @Override
        public boolean isQueryRefinementEnabled(View searchView) {
            return false;
        }

        @Override
        public void setMaxWidth(View searchView, int maxpixels) {
        }
    }

    static class SearchViewCompatHoneycombImpl extends SearchViewCompatStubImpl {

        @Override
        public View newSearchView(Context context) {
            return SearchViewCompatHoneycomb.newSearchView(context);
        }

        @Override
        public void setSearchableInfo(View searchView, ComponentName searchableComponent) {
            SearchViewCompatHoneycomb.setSearchableInfo(searchView, searchableComponent);
        }

        @Override
        public Object newOnQueryTextListener(final OnQueryTextListenerCompat listener) {
            return SearchViewCompatHoneycomb.newOnQueryTextListener(
                    new SearchViewCompatHoneycomb.OnQueryTextListenerCompatBridge() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            return listener.onQueryTextSubmit(query);
                        }
                        @Override
                        public boolean onQueryTextChange(String newText) {
                            return listener.onQueryTextChange(newText);
                        }
                    });
        }

        @Override
        public void setOnQueryTextListener(Object searchView, Object listener) {
            SearchViewCompatHoneycomb.setOnQueryTextListener(searchView, listener);
        }

        @Override
        public Object newOnCloseListener(final OnCloseListenerCompat listener) {
            return SearchViewCompatHoneycomb.newOnCloseListener(
                    new SearchViewCompatHoneycomb.OnCloseListenerCompatBridge() {
                        @Override
                        public boolean onClose() {
                            return listener.onClose();
                        }
                    });
        }

        @Override
        public void setOnCloseListener(Object searchView, Object listener) {
            SearchViewCompatHoneycomb.setOnCloseListener(searchView, listener);
        }

        @Override
        public CharSequence getQuery(View searchView) {
            return SearchViewCompatHoneycomb.getQuery(searchView);
        }

        @Override
        public void setQuery(View searchView, CharSequence query, boolean submit) {
            SearchViewCompatHoneycomb.setQuery(searchView, query, submit);
        }

        @Override
        public void setQueryHint(View searchView, CharSequence hint) {
            SearchViewCompatHoneycomb.setQueryHint(searchView, hint);
        }

        @Override
        public void setIconified(View searchView, boolean iconify) {
            SearchViewCompatHoneycomb.setIconified(searchView, iconify);
        }

        @Override
        public boolean isIconified(View searchView) {
            return SearchViewCompatHoneycomb.isIconified(searchView);
        }

        @Override
        public void setSubmitButtonEnabled(View searchView, boolean enabled) {
            SearchViewCompatHoneycomb.setSubmitButtonEnabled(searchView, enabled);
        }

        @Override
        public boolean isSubmitButtonEnabled(View searchView) {
            return SearchViewCompatHoneycomb.isSubmitButtonEnabled(searchView);
        }

        @Override
        public void setQueryRefinementEnabled(View searchView, boolean enable) {
            SearchViewCompatHoneycomb.setQueryRefinementEnabled(searchView, enable);
        }

        @Override
        public boolean isQueryRefinementEnabled(View searchView) {
            return SearchViewCompatHoneycomb.isQueryRefinementEnabled(searchView);
        }

        @Override
        public void setMaxWidth(View searchView, int maxpixels) {
            SearchViewCompatHoneycomb.setMaxWidth(searchView, maxpixels);
        }
    }

    static class SearchViewCompatIcsImpl extends SearchViewCompatHoneycombImpl {

        @Override
        public View newSearchView(Context context) {
            return SearchViewCompatIcs.newSearchView(context);
        }

        @Override
        public void setImeOptions(View searchView, int imeOptions) {
            SearchViewCompatIcs.setImeOptions(searchView, imeOptions);
        }

        @Override
        public void setInputType(View searchView, int inputType) {
            SearchViewCompatIcs.setInputType(searchView, inputType);
        }
    }

    private static final SearchViewCompatImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 14) { // ICS
            IMPL = new SearchViewCompatIcsImpl();
        } else if (Build.VERSION.SDK_INT >= 11) { // Honeycomb
            IMPL = new SearchViewCompatHoneycombImpl();
        } else {
            IMPL = new SearchViewCompatStubImpl();
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
     */
    public static View newSearchView(Context context) {
        return IMPL.newSearchView(context);
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
     */
    public static void setSearchableInfo(View searchView, ComponentName searchableComponent) {
        IMPL.setSearchableInfo(searchView, searchableComponent);
    }

    /**
     * Sets the IME options on the query text field.  This is a no-op if
     * called on pre-{@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH}
     * platforms.
     *
     * @see TextView#setImeOptions(int)
     * @param searchView The SearchView to operate on.
     * @param imeOptions the options to set on the query text field
     */
    public static void setImeOptions(View searchView, int imeOptions) {
        IMPL.setImeOptions(searchView, imeOptions);
    }

    /**
     * Sets the input type on the query text field.  This is a no-op if
     * called on pre-{@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH}
     * platforms.
     *
     * @see TextView#setInputType(int)
     * @param searchView The SearchView to operate on.
     * @param inputType the input type to set on the query text field
     */
    public static void setInputType(View searchView, int inputType) {
        IMPL.setInputType(searchView, inputType);
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param searchView The SearchView in which to register the listener.
     * @param listener the listener object that receives callbacks when the user performs
     *     actions in the SearchView such as clicking on buttons or typing a query.
     */
    public static void setOnQueryTextListener(View searchView, OnQueryTextListenerCompat listener) {
        IMPL.setOnQueryTextListener(searchView, listener.mListener);
    }

    /**
     * Callbacks for changes to the query text.
     */
    public static abstract class OnQueryTextListenerCompat {
        final Object mListener;

        public OnQueryTextListenerCompat() {
            mListener = IMPL.newOnQueryTextListener(this);
        }

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
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         *
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }

    /**
     * Sets a listener to inform when the user closes the SearchView.
     *
     * @param searchView The SearchView in which to register the listener.
     * @param listener the listener to call when the user closes the SearchView.
     */
    public static void setOnCloseListener(View searchView, OnCloseListenerCompat listener) {
        IMPL.setOnCloseListener(searchView, listener.mListener);
    }

    /**
     * Callback for closing the query UI.
     */
    public static abstract class OnCloseListenerCompat {
        final Object mListener;

        public OnCloseListenerCompat() {
            mListener = IMPL.newOnCloseListener(this);
        }

        /**
         * The user is attempting to close the SearchView.
         *
         * @return true if the listener wants to override the default behavior of clearing the
         * text field and dismissing it, false otherwise.
         */
        public boolean onClose() {
            return false;
        }
    }

    /**
     * Returns the query string currently in the text field.
     *
     * @param searchView The SearchView to operate on.
     *
     * @return the query string
     */
    public static CharSequence getQuery(View searchView) {
        return IMPL.getQuery(searchView);
    }

    /**
     * Sets a query string in the text field and optionally submits the query as well.
     *
     * @param searchView The SearchView to operate on.
     * @param query the query string. This replaces any query text already present in the
     * text field.
     * @param submit whether to submit the query right now or only update the contents of
     * text field.
     */
    public static void setQuery(View searchView, CharSequence query, boolean submit) {
        IMPL.setQuery(searchView, query, submit);
    }

    /**
     * Sets the hint text to display in the query text field. This overrides any hint specified
     * in the SearchableInfo.
     *
     * @param searchView The SearchView to operate on.
     * @param hint the hint text to display
     */
    public static void setQueryHint(View searchView, CharSequence hint) {
        IMPL.setQueryHint(searchView, hint);
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
     */
    public static void setIconified(View searchView, boolean iconify) {
        IMPL.setIconified(searchView, iconify);
    }

    /**
     * Returns the current iconified state of the SearchView.
     *
     * @param searchView The SearchView to operate on.
     * @return true if the SearchView is currently iconified, false if the search field is
     * fully visible.
     */
    public static boolean isIconified(View searchView) {
        return IMPL.isIconified(searchView);
    }

    /**
     * Enables showing a submit button when the query is non-empty. In cases where the SearchView
     * is being used to filter the contents of the current activity and doesn't launch a separate
     * results activity, then the submit button should be disabled.
     *
     * @param searchView The SearchView to operate on.
     * @param enabled true to show a submit button for submitting queries, false if a submit
     * button is not required.
     */
    public static void setSubmitButtonEnabled(View searchView, boolean enabled) {
        IMPL.setSubmitButtonEnabled(searchView, enabled);
    }

    /**
     * Returns whether the submit button is enabled when necessary or never displayed.
     *
     * @param searchView The SearchView to operate on.
     * @return whether the submit button is enabled automatically when necessary
     */
    public static boolean isSubmitButtonEnabled(View searchView) {
        return IMPL.isSubmitButtonEnabled(searchView);
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
     */
    public static void setQueryRefinementEnabled(View searchView, boolean enable) {
        IMPL.setQueryRefinementEnabled(searchView, enable);
    }

    /**
     * Returns whether query refinement is enabled for all items or only specific ones.
     * @param searchView The SearchView to operate on.
     * @return true if enabled for all items, false otherwise.
     */
    public static boolean isQueryRefinementEnabled(View searchView) {
        return IMPL.isQueryRefinementEnabled(searchView);
    }

    /**
     * Makes the view at most this many pixels wide
     * @param searchView The SearchView to operate on.
     */
    public static void setMaxWidth(View searchView, int maxpixels) {
        IMPL.setMaxWidth(searchView, maxpixels);
    }
}
