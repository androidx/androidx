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

package android.support.text.emoji.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.EmojiSpan;
import android.support.text.emoji.R;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;

/**
 * Layout that contains emoji compatibility enhanced ExtractEditText. Should be used by
 * {@link InputMethodService} implementations.
 * <p/>
 * Call {@link #onUpdateExtractingViews(InputMethodService, EditorInfo)} from
 * {@link InputMethodService#onUpdateExtractingViews(EditorInfo)
 * InputMethodService#onUpdateExtractingViews(EditorInfo)}.
 * <pre>
 * public class MyInputMethodService extends InputMethodService {
 *     // ..
 *     {@literal @}Override
 *     public View onCreateExtractTextView() {
 *         mExtractView = getLayoutInflater().inflate(R.layout.emoji_input_method_extract_layout,
 *                 null);
 *         return mExtractView;
 *     }
 *
 *     {@literal @}Override
 *     public void onUpdateExtractingViews(EditorInfo ei) {
 *         mExtractView.onUpdateExtractingViews(this, ei);
 *     }
 * }
 * </pre>
 *
 * @attr ref android.support.text.emoji.R.styleable#EmojiExtractTextLayout_emojiReplaceStrategy
 */
public class EmojiExtractTextLayout extends LinearLayout {

    private ExtractButtonCompat mExtractAction;
    private EmojiExtractEditText mExtractEditText;
    private ViewGroup mExtractAccessories;
    private View.OnClickListener mButtonOnClickListener;

    /**
     * Prevent calling {@link #init(Context, AttributeSet, int)}} multiple times in case super()
     * constructors call other constructors.
     */
    private boolean mInitialized;

    public EmojiExtractTextLayout(Context context) {
        super(context);
        init(context, null /*attrs*/, 0 /*defStyleAttr*/, 0 /*defStyleRes*/);
    }

    public EmojiExtractTextLayout(Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0 /*defStyleAttr*/, 0 /*defStyleRes*/);
    }

    public EmojiExtractTextLayout(Context context,
            @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0 /*defStyleRes*/);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EmojiExtractTextLayout(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        if (!mInitialized) {
            mInitialized = true;
            setOrientation(HORIZONTAL);
            final View view = LayoutInflater.from(context)
                    .inflate(R.layout.input_method_extract_view, this /*root*/,
                            true /*attachToRoot*/);
            mExtractAccessories = view.findViewById(R.id.inputExtractAccessories);
            mExtractAction = view.findViewById(R.id.inputExtractAction);
            mExtractEditText = view.findViewById(android.R.id.inputExtractEditText);

            if (attrs != null) {
                final TypedArray a = context.obtainStyledAttributes(attrs,
                        R.styleable.EmojiExtractTextLayout, defStyleAttr, defStyleRes);
                final int replaceStrategy = a.getInteger(
                        R.styleable.EmojiExtractTextLayout_emojiReplaceStrategy,
                        EmojiCompat.REPLACE_STRATEGY_DEFAULT);
                mExtractEditText.setEmojiReplaceStrategy(replaceStrategy);
                a.recycle();
            }
        }
    }

    /**
     * Sets whether to replace all emoji with {@link EmojiSpan}s. Default value is
     * {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT}.
     *
     * @param replaceStrategy should be one of {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_NON_EXISTENT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_ALL}
     *
     * @attr ref android.support.text.emoji.R.styleable#EmojiExtractTextLayout_emojiReplaceStrategy
     */
    public void setEmojiReplaceStrategy(@EmojiCompat.ReplaceStrategy int replaceStrategy) {
        mExtractEditText.setEmojiReplaceStrategy(replaceStrategy);
    }

    /**
     * Returns whether to replace all emoji with {@link EmojiSpan}s. Default value is
     * {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT}.
     *
     * @return one of {@link EmojiCompat#REPLACE_STRATEGY_DEFAULT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_NON_EXISTENT},
     *                        {@link EmojiCompat#REPLACE_STRATEGY_ALL}
     *
     * @attr ref android.support.text.emoji.R.styleable#EmojiExtractTextLayout_emojiReplaceStrategy
     */
    public int getEmojiReplaceStrategy() {
        return mExtractEditText.getEmojiReplaceStrategy();
    }

    /**
     * Initializes the layout. Call this function from
     * {@link InputMethodService#onUpdateExtractingViews(EditorInfo)
     * InputMethodService#onUpdateExtractingViews(EditorInfo)}.
     */
    public void onUpdateExtractingViews(InputMethodService inputMethodService, EditorInfo ei) {
        // the following code is ported as it is from InputMethodService.onUpdateExtractingViews
        if (!inputMethodService.isExtractViewShown()) {
            return;
        }

        if (mExtractAccessories == null) {
            return;
        }

        final boolean hasAction = ei.actionLabel != null
                || ((ei.imeOptions & EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE
                && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION) == 0
                && ei.inputType != InputType.TYPE_NULL);

        if (hasAction) {
            mExtractAccessories.setVisibility(View.VISIBLE);
            if (mExtractAction != null) {
                if (ei.actionLabel != null) {
                    mExtractAction.setText(ei.actionLabel);
                } else {
                    mExtractAction.setText(inputMethodService.getTextForImeAction(ei.imeOptions));
                }
                mExtractAction.setOnClickListener(getButtonClickListener(inputMethodService));
            }
        } else {
            mExtractAccessories.setVisibility(View.GONE);
            if (mExtractAction != null) {
                mExtractAction.setOnClickListener(null);
            }
        }
    }

    private View.OnClickListener getButtonClickListener(
            final InputMethodService inputMethodService) {
        if (mButtonOnClickListener == null) {
            mButtonOnClickListener = new ButtonOnclickListener(inputMethodService);
        }
        return mButtonOnClickListener;
    }

    private static final class ButtonOnclickListener implements View.OnClickListener {
        private final InputMethodService mInputMethodService;

        ButtonOnclickListener(InputMethodService inputMethodService) {
            mInputMethodService = inputMethodService;
        }

        /**
         * The following code is ported as it is from InputMethodService.mActionClickListener.
         */
        @Override
        public void onClick(View v) {
            final EditorInfo ei = mInputMethodService.getCurrentInputEditorInfo();
            final InputConnection ic = mInputMethodService.getCurrentInputConnection();
            if (ei != null && ic != null) {
                if (ei.actionId != 0) {
                    ic.performEditorAction(ei.actionId);
                } else if ((ei.imeOptions & EditorInfo.IME_MASK_ACTION)
                        != EditorInfo.IME_ACTION_NONE) {
                    ic.performEditorAction(ei.imeOptions & EditorInfo.IME_MASK_ACTION);
                }
            }
        }
    }
}
