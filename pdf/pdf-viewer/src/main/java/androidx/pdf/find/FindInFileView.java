/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.pdf.find;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.R;
import androidx.pdf.util.Accessibility;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.ObservableValue.ValueObserver;

import javax.annotation.Nullable;

/**
 * A View that has a search query box, find-next and find-previous button, useful for finding
 * matches in a file. This does not use the real platform SearchView because it cannot be styled to
 * remove the search icon and underline.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FindInFileView extends LinearLayout {

    private TextView mQueryBox;
    private ImageView mPrevButton;
    private ImageView mNextButton;
    private TextView mMatchStatus;
    private View mCloseButton;
    private FindInFileListener mFindInFileListener;
    private ObservableValue<MatchCount> mMatchCount;
    private static final char MATCH_STATUS_COUNTING = '\u2026';

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mPrevButton || v == mNextButton) {
                mQueryBox.clearFocus();
                if (mFindInFileListener != null) {
                    boolean mBackwards = (v == mPrevButton);
                    mFindInFileListener.onFindNextMatch(mQueryBox.getText().toString(), mBackwards);
                }
            }
        }
    };

    private final ValueObserver<MatchCount> mMatchCountObserver = new ValueObserver<MatchCount>() {
        @Override
        public void onChange(MatchCount oldMatchCount, MatchCount newMatchCount) {
            if (newMatchCount == null) {
                mMatchStatus.setFocusableInTouchMode(false);
                mMatchStatus.setText("");
            } else {
                String matchStatusText = getContext().getString(R.string.message_match_status,
                        newMatchCount.mSelectedIndex + 1,
                        // Zero-based - change to one-based for user.
                        newMatchCount.mTotalMatches);
                if (newMatchCount.mIsAllPagesCounted) {
                    if (newMatchCount.mSelectedIndex >= 0) {
                        Accessibility.get().announce(getContext(), FindInFileView.this,
                                matchStatusText);
                    }
                } else {
                    matchStatusText += MATCH_STATUS_COUNTING;  // Not yet all counted, use ellipses.
                }
                mMatchStatus.setText(matchStatusText);
                mMatchStatus.setFocusableInTouchMode(true);
            }
        }
    };

    private final TextWatcher mOnQueryTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mFindInFileListener != null) {
                mFindInFileListener.onQueryTextChange(s.toString());
            }

            // Enable next/prev button
            if (!TextUtils.isGraphic(s)) {
                mMatchStatus.setVisibility(GONE);
            } else {
                mMatchStatus.setVisibility(VISIBLE);
            }
        }
    };

    private final OnEditorActionListener mOnActionListener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mQueryBox.clearFocus();
                if (mFindInFileListener != null) {
                    return mFindInFileListener.onFindNextMatch(textView.getText().toString(),
                            false);
                }
            }
            return false;
        }
    };

    public FindInFileView(@NonNull Context context) {
        this(context, null);
    }

    public FindInFileView(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.find_in_file, this, true);

        mQueryBox = (TextView) findViewById(R.id.find_query_box);
        mPrevButton = findViewById(R.id.find_prev_btn);
        mNextButton = findViewById(R.id.find_next_btn);
        mMatchStatus = (TextView) findViewById(R.id.match_status_textview);
        mCloseButton = findViewById(R.id.close_btn);
        mQueryBox.addTextChangedListener(mOnQueryTextListener);
        mQueryBox.setOnEditorActionListener(mOnActionListener);
        mPrevButton.setOnClickListener(mOnClickListener);
        mNextButton.setOnClickListener(mOnClickListener);
        mCloseButton.setOnClickListener(mOnClickListener);
        this.setFocusableInTouchMode(true);
    }

    /**
     * registers the {@link FindInFileListener}
     * @param findInFileListener
     */
    public void setFindInFileListener(@Nullable FindInFileListener findInFileListener) {
        this.mFindInFileListener = findInFileListener;
        setObservableMatchCount(
                (findInFileListener != null) ? findInFileListener.matchCount() : null);
    }

    private void setObservableMatchCount(@Nullable ObservableValue<MatchCount> matchCount) {
        if (this.mMatchCount != null) {
            this.mMatchCount.removeObserver(mMatchCountObserver);
        }
        this.mMatchCount = matchCount;
        if (this.mMatchCount != null) {
            this.mMatchCount.addObserver(mMatchCountObserver);
        }
    }

    /**
     * Shows the keyboard when find in file view is inflated.
     */
    public void queryBoxRequestFocus() {
        mQueryBox.requestFocus();
    }
}
