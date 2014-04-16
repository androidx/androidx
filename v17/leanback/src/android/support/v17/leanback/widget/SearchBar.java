/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v17.leanback.widget;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.support.v17.leanback.R;
import android.widget.TextView;

/**
 * SearchBar is a search widget.
 */
public class SearchBar extends RelativeLayout {
    private static final String TAG = SearchBar.class.getSimpleName();
    private static final boolean DEBUG = false;

    /**
     * Listener for search query changes
     */
    public interface SearchBarListener {

        /**
         * Method invoked when the search bar detects a change in the query.
         *
         * @param query The current full query.
         */
        public void onSearchQueryChange(String query);

        /**
         * Method invoked when the search query is submitted.
         *
         * @param query The query being submitted.
         */
        public void onSearchQuerySubmit(String query);

        /**
         * Method invoked when the IME is being dismissed.
         *
         * @param query The query set in the search bar at the time the IME is being dismissed.
         */
        public void onKeyboardDismiss(String query);
    }

    private SearchBarListener mSearchBarListener;
    private SearchEditText mSearchTextEditor;
    private String mSearchQuery;
    private final Handler mHandler = new Handler();

    public SearchBar(Context context) {
        this(context, null);
    }

    public SearchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mSearchQuery = "";
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSearchTextEditor = (SearchEditText)findViewById(R.id.lb_search_text_editor);
        mSearchTextEditor.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (DEBUG) Log.v(TAG, "onFocusChange " + hasFocus);
                if (hasFocus) {
                    showNativeKeyboard();
                }
            }
        });
        mSearchTextEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                setSearchQuery(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mSearchTextEditor.setOnKeyboardDismissListener(
                new SearchEditText.OnKeyboardDismissListener() {
                    @Override
                    public void onKeyboardDismiss() {
                        if (null != mSearchBarListener) {
                            mSearchBarListener.onKeyboardDismiss(mSearchQuery);
                        }
                    }
                });
        mSearchTextEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
                if (EditorInfo.IME_ACTION_SEARCH == action && null != mSearchBarListener) {
                    mSearchBarListener.onSearchQuerySubmit(mSearchQuery);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSearchTextEditor.requestFocus();
                mSearchTextEditor.requestFocusFromTouch();
            }
        });
    }

    /**
     * Set a listener for when the term search changes
     * @param listener
     */
    public void setSearchBarListener(SearchBarListener listener) {
        mSearchBarListener = listener;
    }

    /**
     * Set the search query
     * @param query the search query to use
     */
    public void setSearchQuery(String query) {
        if (query.equals(mSearchQuery)) {
            return;
        }
        mSearchQuery = query;
        if (null != mSearchBarListener) {
            mSearchBarListener.onSearchQueryChange(mSearchQuery);
        }
    }

    /**
     * Set the hint text shown in the search bar.
     * @param hint The hint to use.
     */
    public void setHint(String hint) {
        mSearchTextEditor.setHint(hint);
    }

    protected void showNativeKeyboard() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSearchTextEditor.requestFocusFromTouch();
                mSearchTextEditor.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN,
                        mSearchTextEditor.getWidth(), mSearchTextEditor.getHeight(), 0));
                mSearchTextEditor.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_UP,
                        mSearchTextEditor.getWidth(), mSearchTextEditor.getHeight(), 0));
            }
        });
    }

}
