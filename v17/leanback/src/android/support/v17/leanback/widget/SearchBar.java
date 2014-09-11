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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.support.v17.leanback.R;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>SearchBar is a search widget.</p>
 *
 * <p>Note: Your application will need to request android.permission.RECORD_AUDIO</p>
 */
public class SearchBar extends RelativeLayout {
    private static final String TAG = SearchBar.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final float FULL_LEFT_VOLUME = 1.0f;
    private static final float FULL_RIGHT_VOLUME = 1.0f;
    private static final int DEFAULT_PRIORITY = 1;
    private static final int DO_NOT_LOOP = 0;
    private static final float DEFAULT_RATE = 1.0f;

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
         * <p>Method invoked when the search query is submitted.</p>
         *
         * <p>This method can be called without a preceeding onSearchQueryChange,
         * in particular in the case of a voice input.</p>
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
    private SpeechOrbView mSpeechOrbView;
    private ImageView mBadgeView;
    private String mSearchQuery;
    private String mTitle;
    private Drawable mBadgeDrawable;
    private final Handler mHandler = new Handler();
    private final InputMethodManager mInputMethodManager;
    private boolean mAutoStartRecognition = false;
    private Drawable mBarBackground;

    private final int mTextColor;
    private final int mTextColorSpeechMode;
    private final int mTextHintColor;
    private final int mTextHintColorSpeechMode;
    private int mBackgroundAlpha;
    private int mBackgroundSpeechAlpha;
    private int mBarHeight;
    private SpeechRecognizer mSpeechRecognizer;
    private boolean mListening;
    private SoundPool mSoundPool;
    private SparseIntArray mSoundMap = new SparseIntArray();
    private boolean mRecognizing = false;
    private final Context mContext;

    public SearchBar(Context context) {
        this(context, null);
    }

    public SearchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        enforceAudioRecordPermission();

        Resources r = getResources();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.lb_search_bar, this, true);

        mBarHeight = getResources().getDimensionPixelSize(R.dimen.lb_search_bar_height);
        RelativeLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                mBarHeight);
        params.addRule(ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        setLayoutParams(params);
        setBackgroundColor(Color.TRANSPARENT);
        setClipChildren(false);

        mSearchQuery = "";
        mInputMethodManager =
                (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

        mTextColorSpeechMode = r.getColor(R.color.lb_search_bar_text_speech_mode);
        mTextColor = r.getColor(R.color.lb_search_bar_text);

        mBackgroundSpeechAlpha = r.getInteger(R.integer.lb_search_bar_speech_mode_background_alpha);
        mBackgroundAlpha = r.getInteger(R.integer.lb_search_bar_text_mode_background_alpha);

        mTextHintColorSpeechMode = r.getColor(R.color.lb_search_bar_hint_speech_mode);
        mTextHintColor = r.getColor(R.color.lb_search_bar_hint);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        RelativeLayout items = (RelativeLayout)findViewById(R.id.lb_search_bar_items);
        mBarBackground = items.getBackground();

        mSearchTextEditor = (SearchEditText)findViewById(R.id.lb_search_text_editor);
        mBadgeView = (ImageView)findViewById(R.id.lb_search_bar_badge);
        if (null != mBadgeDrawable) {
            mBadgeView.setImageDrawable(mBadgeDrawable);
        }

        mSearchTextEditor.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (DEBUG) Log.v(TAG, "EditText.onFocusChange " + hasFocus);
                if (hasFocus) {
                    showNativeKeyboard();
                }
                updateUi();
            }
        });
        mSearchTextEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (mSearchTextEditor.hasFocus()) {
                    setSearchQuery(charSequence.toString());
                }
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
                if (DEBUG) Log.v(TAG, "onEditorAction: " + action + " event: " + keyEvent);
                boolean handled = true;
                if (EditorInfo.IME_ACTION_SEARCH == action && null != mSearchBarListener) {
                    if (DEBUG) Log.v(TAG, "Action Pressed");
                    hideNativeKeyboard();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) Log.v(TAG, "Delayed action handling (search)");
                            submitQuery();
                        }
                    }, 500);

                } else if (EditorInfo.IME_ACTION_NONE == action && null != mSearchBarListener) {
                    if (DEBUG) Log.v(TAG, "Escaped North");
                    hideNativeKeyboard();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) Log.v(TAG, "Delayed action handling (escape_north)");
                            mSearchBarListener.onKeyboardDismiss(mSearchQuery);
                        }
                    }, 500);
                } else if (EditorInfo.IME_ACTION_GO == action) {
                    if (DEBUG) Log.v(TAG, "Voice Clicked");
                        hideNativeKeyboard();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (DEBUG) Log.v(TAG, "Delayed action handling (voice_mode)");
                                mAutoStartRecognition = true;
                                mSpeechOrbView.requestFocus();
                            }
                        }, 500);
                } else {
                    handled = false;
                }

                return handled;
            }
        });

        mSearchTextEditor.setPrivateImeOptions("EscapeNorth=1;VoiceDismiss=1;");

        mSpeechOrbView = (SpeechOrbView)findViewById(R.id.lb_search_bar_speech_orb);
        mSpeechOrbView.setOnOrbClickedListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRecognition();
            }
        });
        mSpeechOrbView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (DEBUG) Log.v(TAG, "SpeechOrb.onFocusChange " + hasFocus);
                if (hasFocus) {
                    hideNativeKeyboard();
                    if (mAutoStartRecognition) {
                        startRecognition();
                        mAutoStartRecognition = false;
                    }
                } else {
                    stopRecognition();
                }
                updateUi();
            }
        });

        updateHint();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) Log.v(TAG, "Loading soundPool");
        mSoundPool = new SoundPool(2, AudioManager.STREAM_SYSTEM, 0);
        loadSounds(mContext);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startRecognition();
            }
        }, 300);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (DEBUG) Log.v(TAG, "Releasing SoundPool");
        mSoundPool.release();

        super.onDetachedFromWindow();
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
     * Set the title text used in the hint shown in the search bar.
     * @param title The hint to use.
     */
    public void setTitle(String title) {
        mTitle = title;
        updateHint();
    }

    /**
     * Returns the current title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Set the badge drawable showing inside the search bar.
     * @param drawable The drawable to be used in the search bar.
     */
    public void setBadgeDrawable(Drawable drawable) {
        mBadgeDrawable = drawable;
        if (null != mBadgeView) {
            mBadgeView.setImageDrawable(drawable);
            if (null != drawable) {
                mBadgeView.setVisibility(View.VISIBLE);
            } else {
                mBadgeView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Returns the badge drawable
     */
    public Drawable getBadgeDrawable() {
        return mBadgeDrawable;
    }

    /**
     * Update the completion list shown by the IME
     *
     * @param completions list of completions shown in the IME, can be null or empty to clear them
     */
    public void displayCompletions(List<String> completions) {
        List<CompletionInfo> infos = new ArrayList<CompletionInfo>();
        if (null != completions) {
            for (String completion : completions) {
                infos.add(new CompletionInfo(infos.size(), infos.size(), completion));
            }
        }

        mInputMethodManager.displayCompletions(mSearchTextEditor,
                infos.toArray(new CompletionInfo[] {}));
    }

    /**
     * Set the speech recognizer to be used when doing voice search. The Activity/Fragment is in
     * charge of creating and destroying the recognizer with its own lifecycle.
     *
     * @param recognizer a SpeechRecognizer
     */
    public void setSpeechRecognizer(SpeechRecognizer recognizer) {
        if (null != mSpeechRecognizer) {
            mSpeechRecognizer.setRecognitionListener(null);
            if (mListening) {
                mSpeechRecognizer.stopListening();
                mListening = false;
            }
        }
        mSpeechRecognizer = recognizer;
    }

    private void hideNativeKeyboard() {
        mInputMethodManager.hideSoftInputFromWindow(mSearchTextEditor.getWindowToken(),
                InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    private void showNativeKeyboard() {
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

    /**
     * This will update the hint for the search bar properly depending on state and provided title
     */
    private void updateHint() {
        if (null == mSearchTextEditor) return;

        String title = getResources().getString(R.string.lb_search_bar_hint);
        if (!TextUtils.isEmpty(mTitle)) {
            if (isVoiceMode()) {
                title = getResources().getString(R.string.lb_search_bar_hint_with_title_speech, mTitle);
            } else {
                title = getResources().getString(R.string.lb_search_bar_hint_with_title, mTitle);
            }
        } else if (isVoiceMode()) {
            title = getResources().getString(R.string.lb_search_bar_hint_speech);
        }
        mSearchTextEditor.setHint(title);
    }

    private void toggleRecognition() {
        if (mRecognizing) {
            stopRecognition();
        } else {
            startRecognition();
        }
    }

    private void stopRecognition() {
        if (null == mSpeechRecognizer) return;
        if (!mRecognizing) return;
        mRecognizing = false;

        if (DEBUG) Log.v(TAG, "stopRecognition " + mListening);
        mSpeechOrbView.showNotListening();

        if (mListening) {
            mSpeechRecognizer.cancel();
        }
    }

    private void startRecognition() {
        if (null == mSpeechRecognizer) return;
        if (mRecognizing) return;
        mRecognizing = true;

        if (DEBUG) Log.v(TAG, "startRecognition " + mListening);

        mSearchTextEditor.setText("");

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                if (DEBUG) Log.v(TAG, "onReadyForSpeech");
            }

            @Override
            public void onBeginningOfSpeech() {
                if (DEBUG) Log.v(TAG, "onBeginningOfSpeech");
                mListening = true;
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                if (DEBUG) Log.v(TAG, "onRmsChanged " + rmsdB);
                int level = rmsdB < 0 ? 0 : (int)(10 * rmsdB);
                mSpeechOrbView.setSoundLevel(level);
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                if (DEBUG) Log.v(TAG, "onBufferReceived " + bytes.length);
            }

            @Override
            public void onEndOfSpeech() {
                if (DEBUG) Log.v(TAG, "onEndOfSpeech");
                mListening = false;
            }

            @Override
            public void onError(int error) {
                if (DEBUG) Log.v(TAG, "onError " + error);
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        Log.d(TAG, "recognizer error no match");
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        Log.d(TAG, "recognizer error server error");
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        Log.d(TAG, "recognizer error speech timeout");
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        Log.d(TAG, "recognizer error client error");
                        break;
                    default:
                        Log.d(TAG, "recognizer other error");
                        break;
                }

                mSpeechRecognizer.stopListening();
                mListening = false;
                mSpeechRecognizer.setRecognitionListener(null);
                mSpeechOrbView.showNotListening();
                playSearchFailure();
            }

            @Override
            public void onResults(Bundle bundle) {
                if (DEBUG) Log.v(TAG, "onResults");
                final ArrayList<String> matches =
                        bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    Log.v(TAG, "Got results" + matches);

                    mSearchQuery = matches.get(0);
                    mSearchTextEditor.setText(mSearchQuery);
                    submitQuery();

                    if (mListening) {
                        mSpeechRecognizer.stopListening();
                    }
                }
                mSpeechRecognizer.setRecognitionListener(null);
                mSpeechOrbView.showNotListening();
                playSearchSuccess();
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        mSpeechOrbView.showListening();
        playSearchOpen();
        mSpeechRecognizer.startListening(recognizerIntent);
        mListening = true;
    }

    private void updateUi() {
        if (DEBUG) Log.v(TAG, String.format("Update UI %s %s",
                isVoiceMode() ? "Voice" : "Text",
                hasFocus() ? "Focused" : "Unfocused"));
        if (isVoiceMode()) {
            mBarBackground.setAlpha(mBackgroundSpeechAlpha);
            mSearchTextEditor.setTextColor(mTextColorSpeechMode);
            mSearchTextEditor.setHintTextColor(mTextHintColorSpeechMode);
        } else {
            mBarBackground.setAlpha(mBackgroundAlpha);
            mSearchTextEditor.setTextColor(mTextColor);
            mSearchTextEditor.setHintTextColor(mTextHintColor);
        }

        updateHint();
    }

    private boolean isVoiceMode() {
        return mSpeechOrbView.isFocused();
    }

    private void submitQuery() {
        if (!TextUtils.isEmpty(mSearchQuery) && null != mSearchBarListener) {
            mSearchBarListener.onSearchQuerySubmit(mSearchQuery);
        }
    }

    private void enforceAudioRecordPermission() {
        String permission = "android.permission.RECORD_AUDIO";
        int res = getContext().checkCallingOrSelfPermission(permission);
        if (PackageManager.PERMISSION_GRANTED != res) {
            throw new IllegalStateException("android.permission.RECORD_AUDIO required for search");
        }
    }

    private void loadSounds(Context context) {
        int[] sounds = {
                R.raw.lb_voice_failure,
                R.raw.lb_voice_open,
                R.raw.lb_voice_no_input,
                R.raw.lb_voice_success,
        };
        for (int sound : sounds) {
            mSoundMap.put(sound, mSoundPool.load(context, sound, 1));
        }
    }

    private void play(final int resId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int sound = mSoundMap.get(resId);
                mSoundPool.play(sound, FULL_LEFT_VOLUME, FULL_RIGHT_VOLUME, DEFAULT_PRIORITY,
                        DO_NOT_LOOP, DEFAULT_RATE);
            }
        });
    }

    private void playSearchOpen() {
        play(R.raw.lb_voice_open);
    }

    private void playSearchFailure() {
        play(R.raw.lb_voice_failure);
    }

    private void playSearchNoInput() {
        play(R.raw.lb_voice_no_input);
    }

    private void playSearchSuccess() {
        play(R.raw.lb_voice_success);
    }

    @Override
    public void setNextFocusDownId(int viewId) {
        mSpeechOrbView.setNextFocusDownId(viewId);
        mSearchTextEditor.setNextFocusDownId(viewId);
    }

}
