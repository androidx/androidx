/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.customviews;

import static android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.compose.remote.player.core.platform.AndroidComponentSupport;
import androidx.compose.remote.player.view.RemoteComposePlayer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Component support delegate for Android's TextView.
 */
@SuppressLint("RestrictedApiAndroidX")
public class SupportEditText implements AndroidComponentSupport {

    public static final short PROP_TEXT = 1;
    public static final short PROP_TEXT_COLOR = 2;
    public static final short PROP_TEXT_SIZE = 3;
    public static final short PROP_BACKGROUND_COLOR = 4;
    public static final short PROP_HINT = 5;
    public static final short RET_TEXT = 6;

    private final @NonNull AndroidCustomSupport mSupport;

    @Nullable
    private RemoteComposePlayer mPlayer;

    /**
     * Anchor the view to the player.
     *
     * @param player The player to anchor the view to
     */
    public void anchorView(@NonNull RemoteComposePlayer player) {
        mPlayer = player;
    }

    public SupportEditText(@NonNull AndroidCustomSupport support) {
        mSupport = support;
    }

    @Override
    public @NonNull View createView(@NonNull Context context) {
        EditText editText = new EditText(context);

        editText.setLayoutParams(new FrameLayout.LayoutParams(120, 220));
        editText.setClickable(true);
        editText.setFocusable(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setVisibility(View.VISIBLE);
        if (mPlayer != null) {
            mPlayer.setFocusable(true);
            editText.setGravity(Gravity.TOP | Gravity.START);
            mPlayer.addView(editText);
            // editText.requestFocus();

            editText.setOnEditorActionListener(
                    (TextView.OnEditorActionListener) (v, actionId, event) -> {
                        // IME_ACTION_DONE is the default 'checkmark' or 'enter' action on most
                        // soft keyboards
                        if (actionId == EditorInfo.IME_ACTION_DONE
                                || (event != null
                                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            editText.clearFocus();
                            String text = editText.getText().toString();
                            keyboardFocus(editText, false);
                            Integer id = (Integer) editText.getTag();
                            if (id != null && id != -1 && mSupport.getRemoteContext() != null) {
                                mSupport.getRemoteContext().loadText(id, text);
                            }
                            return true;
                        }
                        return false;
                    });

            /*  enable if you want update as you type
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    Integer id = (Integer) editText.getTag();
                    if (id == null) return;
                    if (id != -1 && mSupport.getRemoteContext() != null) {
                        mSupport.getRemoteContext().loadText(id, s.toString());
                    }
                }
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });
             */
        }
        return editText;
    }


    @Override
    public void configure(@NonNull View view, int type, @NonNull String value) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (type == PROP_TEXT) {
                textView.setText(value);
            } else if (type == PROP_HINT) {
                textView.setHint(value);
            }
        }
    }

    @Override
    public void configure(@NonNull View view, int type, int value) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (type == PROP_TEXT_COLOR) {
                textView.setTextColor(value);
            } else if (type == PROP_TEXT_SIZE) {
                textView.setTextSize(value);
            } else if (type == PROP_BACKGROUND_COLOR) {
                textView.setBackgroundColor(value);
            } else if (type == RET_TEXT) {
                view.setTag(value);
            }
        }
    }

    @Override
    public void configure(@NonNull View view, int type, float value) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (type == PROP_TEXT_SIZE) {
                textView.setTextSize(value);
            }
        }
    }

    /**
     * Show or hide the keyboard.
     *
     * @param view  The view to show the keyboard on
     * @param focus Whether to show or hide the keyboard
     */
    public void keyboardFocus(@NonNull View view, boolean focus) {
        if (focus) {
            view.post(() -> {
                EditText editText = (EditText) view;
                editText.requestFocus();

                InputMethodManager imm =
                        (InputMethodManager) mPlayer.getContext().getSystemService(
                                Context.INPUT_METHOD_SERVICE);

                // 3. Show the software keyboard attached to this focused view
                imm.showSoftInput(editText, SHOW_IMPLICIT);
            });
        } else {
            InputMethodManager imm =
                    (InputMethodManager) mPlayer.getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
