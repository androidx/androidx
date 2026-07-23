/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.key

import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP
import androidx.compose.ui.input.key.Key.Companion.Number
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1

/**
 * Actual implementation of [Key] for Android.
 *
 * @param keyCode an integer code representing the key pressed.
 * @sample androidx.compose.ui.samples.KeyEventIsAltPressedSample
 */
@JvmInline
public actual value class Key(public val keyCode: Long) {
    public actual companion object {
        /** Unknown key. */
        public actual val Unknown: Key
            get() = Key(KeyEvent.KEYCODE_UNKNOWN)

        /**
         * Soft Left key.
         *
         * Usually situated below the display on phones and used as a multi-function feature key for
         * selecting a software defined function shown on the bottom left of the display.
         */
        public actual val SoftLeft: Key
            get() = Key(KeyEvent.KEYCODE_SOFT_LEFT)

        /**
         * Soft Right key.
         *
         * Usually situated below the display on phones and used as a multi-function feature key for
         * selecting a software defined function shown on the bottom right of the display.
         */
        public actual val SoftRight: Key
            get() = Key(KeyEvent.KEYCODE_SOFT_RIGHT)

        /**
         * System Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        @Deprecated(
            "`Key.Home` is never delivered to applications. For the keyboard \"Home\" key " +
                "use `Key.MoveHome`. For the system \"Home\" key (unlikely to be needed), use " +
                "`Key.SystemHome`",
            level = DeprecationLevel.ERROR,
        )
        public actual val Home: Key
            get() = Key(KeyEvent.KEYCODE_HOME)

        /**
         * System Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        public actual val SystemHome: Key
            get() = Key(KeyEvent.KEYCODE_HOME)

        /** Back key. */
        public actual val Back: Key
            get() = Key(KeyEvent.KEYCODE_BACK)

        /** Help key. */
        public actual val Help: Key
            get() = Key(KeyEvent.KEYCODE_HELP)

        /**
         * Navigate to previous key.
         *
         * Goes backward by one item in an ordered collection of items.
         */
        public actual val NavigatePrevious: Key
            get() = Key(KeyEvent.KEYCODE_NAVIGATE_PREVIOUS)

        /**
         * Navigate to next key.
         *
         * Advances to the next item in an ordered collection of items.
         */
        public actual val NavigateNext: Key
            get() = Key(KeyEvent.KEYCODE_NAVIGATE_NEXT)

        /**
         * Navigate in key.
         *
         * Activates the item that currently has focus or expands to the next level of a navigation
         * hierarchy.
         */
        public actual val NavigateIn: Key
            get() = Key(KeyEvent.KEYCODE_NAVIGATE_IN)

        /**
         * Navigate out key.
         *
         * Backs out one level of a navigation hierarchy or collapses the item that currently has
         * focus.
         */
        public actual val NavigateOut: Key
            get() = Key(KeyEvent.KEYCODE_NAVIGATE_OUT)

        /** Consumed by the system for navigation up. */
        public actual val SystemNavigationUp: Key
            get() = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP)

        /** Consumed by the system for navigation down. */
        public actual val SystemNavigationDown: Key
            get() = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN)

        /** Consumed by the system for navigation left. */
        public actual val SystemNavigationLeft: Key
            get() = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT)

        /** Consumed by the system for navigation right. */
        public actual val SystemNavigationRight: Key
            get() = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT)

        /** Call key. */
        public actual val Call: Key
            get() = Key(KeyEvent.KEYCODE_CALL)

        /** End Call key. */
        public actual val EndCall: Key
            get() = Key(KeyEvent.KEYCODE_ENDCALL)

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        public actual val DirectionUp: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_UP)

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        public actual val DirectionDown: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_DOWN)

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        public actual val DirectionLeft: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_LEFT)

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        public actual val DirectionRight: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_RIGHT)

        /**
         * Center Arrow Key / Directional Pad Center key.
         *
         * May also be synthesized from trackball motions.
         */
        public actual val DirectionCenter: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_CENTER)

        /** Directional Pad Up-Left. */
        public actual val DirectionUpLeft: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_UP_LEFT)

        /** Directional Pad Down-Left. */
        public actual val DirectionDownLeft: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_DOWN_LEFT)

        /** Directional Pad Up-Right. */
        public actual val DirectionUpRight: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_UP_RIGHT)

        /** Directional Pad Down-Right. */
        public actual val DirectionDownRight: Key
            get() = Key(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT)

        /**
         * Volume Up key.
         *
         * Adjusts the speaker volume up.
         */
        public actual val VolumeUp: Key
            get() = Key(KeyEvent.KEYCODE_VOLUME_UP)

        /**
         * Volume Down key.
         *
         * Adjusts the speaker volume down.
         */
        public actual val VolumeDown: Key
            get() = Key(KeyEvent.KEYCODE_VOLUME_DOWN)

        /** Power key. */
        public actual val Power: Key
            get() = Key(KeyEvent.KEYCODE_POWER)

        /**
         * Camera key.
         *
         * Used to launch a camera application or take pictures.
         */
        public actual val Camera: Key
            get() = Key(KeyEvent.KEYCODE_CAMERA)

        /** Clear key. */
        public actual val Clear: Key
            get() = Key(KeyEvent.KEYCODE_CLEAR)

        /** '0' key. */
        public actual val Zero: Key
            get() = Key(KeyEvent.KEYCODE_0)

        /** '1' key. */
        public actual val One: Key
            get() = Key(KeyEvent.KEYCODE_1)

        /** '2' key. */
        public actual val Two: Key
            get() = Key(KeyEvent.KEYCODE_2)

        /** '3' key. */
        public actual val Three: Key
            get() = Key(KeyEvent.KEYCODE_3)

        /** '4' key. */
        public actual val Four: Key
            get() = Key(KeyEvent.KEYCODE_4)

        /** '5' key. */
        public actual val Five: Key
            get() = Key(KeyEvent.KEYCODE_5)

        /** '6' key. */
        public actual val Six: Key
            get() = Key(KeyEvent.KEYCODE_6)

        /** '7' key. */
        public actual val Seven: Key
            get() = Key(KeyEvent.KEYCODE_7)

        /** '8' key. */
        public actual val Eight: Key
            get() = Key(KeyEvent.KEYCODE_8)

        /** '9' key. */
        public actual val Nine: Key
            get() = Key(KeyEvent.KEYCODE_9)

        /** '+' key. */
        public actual val Plus: Key
            get() = Key(KeyEvent.KEYCODE_PLUS)

        /** '-' key. */
        public actual val Minus: Key
            get() = Key(KeyEvent.KEYCODE_MINUS)

        /** '*' key. */
        public actual val Multiply: Key
            get() = Key(KeyEvent.KEYCODE_STAR)

        /** '=' key. */
        public actual val Equals: Key
            get() = Key(KeyEvent.KEYCODE_EQUALS)

        /** '#' key. */
        public actual val Pound: Key
            get() = Key(KeyEvent.KEYCODE_POUND)

        /** 'A' key. */
        public actual val A: Key
            get() = Key(KeyEvent.KEYCODE_A)

        /** 'B' key. */
        public actual val B: Key
            get() = Key(KeyEvent.KEYCODE_B)

        /** 'C' key. */
        public actual val C: Key
            get() = Key(KeyEvent.KEYCODE_C)

        /** 'D' key. */
        public actual val D: Key
            get() = Key(KeyEvent.KEYCODE_D)

        /** 'E' key. */
        public actual val E: Key
            get() = Key(KeyEvent.KEYCODE_E)

        /** 'F' key. */
        public actual val F: Key
            get() = Key(KeyEvent.KEYCODE_F)

        /** 'G' key. */
        public actual val G: Key
            get() = Key(KeyEvent.KEYCODE_G)

        /** 'H' key. */
        public actual val H: Key
            get() = Key(KeyEvent.KEYCODE_H)

        /** 'I' key. */
        public actual val I: Key
            get() = Key(KeyEvent.KEYCODE_I)

        /** 'J' key. */
        public actual val J: Key
            get() = Key(KeyEvent.KEYCODE_J)

        /** 'K' key. */
        public actual val K: Key
            get() = Key(KeyEvent.KEYCODE_K)

        /** 'L' key. */
        public actual val L: Key
            get() = Key(KeyEvent.KEYCODE_L)

        /** 'M' key. */
        public actual val M: Key
            get() = Key(KeyEvent.KEYCODE_M)

        /** 'N' key. */
        public actual val N: Key
            get() = Key(KeyEvent.KEYCODE_N)

        /** 'O' key. */
        public actual val O: Key
            get() = Key(KeyEvent.KEYCODE_O)

        /** 'P' key. */
        public actual val P: Key
            get() = Key(KeyEvent.KEYCODE_P)

        /** 'Q' key. */
        public actual val Q: Key
            get() = Key(KeyEvent.KEYCODE_Q)

        /** 'R' key. */
        public actual val R: Key
            get() = Key(KeyEvent.KEYCODE_R)

        /** 'S' key. */
        public actual val S: Key
            get() = Key(KeyEvent.KEYCODE_S)

        /** 'T' key. */
        public actual val T: Key
            get() = Key(KeyEvent.KEYCODE_T)

        /** 'U' key. */
        public actual val U: Key
            get() = Key(KeyEvent.KEYCODE_U)

        /** 'V' key. */
        public actual val V: Key
            get() = Key(KeyEvent.KEYCODE_V)

        /** 'W' key. */
        public actual val W: Key
            get() = Key(KeyEvent.KEYCODE_W)

        /** 'X' key. */
        public actual val X: Key
            get() = Key(KeyEvent.KEYCODE_X)

        /** 'Y' key. */
        public actual val Y: Key
            get() = Key(KeyEvent.KEYCODE_Y)

        /** 'Z' key. */
        public actual val Z: Key
            get() = Key(KeyEvent.KEYCODE_Z)

        /** ',' key. */
        public actual val Comma: Key
            get() = Key(KeyEvent.KEYCODE_COMMA)

        /** '.' key. */
        public actual val Period: Key
            get() = Key(KeyEvent.KEYCODE_PERIOD)

        /** Left Alt modifier key. */
        public actual val AltLeft: Key
            get() = Key(KeyEvent.KEYCODE_ALT_LEFT)

        /** Right Alt modifier key. */
        public actual val AltRight: Key
            get() = Key(KeyEvent.KEYCODE_ALT_RIGHT)

        /** Left Shift modifier key. */
        public actual val ShiftLeft: Key
            get() = Key(KeyEvent.KEYCODE_SHIFT_LEFT)

        /** Right Shift modifier key. */
        public actual val ShiftRight: Key
            get() = Key(KeyEvent.KEYCODE_SHIFT_RIGHT)

        /** Tab key. */
        public actual val Tab: Key
            get() = Key(KeyEvent.KEYCODE_TAB)

        /** Space key. */
        public actual val Spacebar: Key
            get() = Key(KeyEvent.KEYCODE_SPACE)

        /**
         * Symbol modifier key.
         *
         * Used to enter alternate symbols.
         */
        public actual val Symbol: Key
            get() = Key(KeyEvent.KEYCODE_SYM)

        /**
         * Browser special function key.
         *
         * Used to launch a browser application.
         */
        public actual val Browser: Key
            get() = Key(KeyEvent.KEYCODE_EXPLORER)

        /**
         * Envelope special function key.
         *
         * Used to launch a mail application.
         */
        public actual val Envelope: Key
            get() = Key(KeyEvent.KEYCODE_ENVELOPE)

        /** Enter key. */
        public actual val Enter: Key
            get() = Key(KeyEvent.KEYCODE_ENTER)

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        public actual val Backspace: Key
            get() = Key(KeyEvent.KEYCODE_DEL)

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        public actual val Delete: Key
            get() = Key(KeyEvent.KEYCODE_FORWARD_DEL)

        /** Escape key. */
        public actual val Escape: Key
            get() = Key(KeyEvent.KEYCODE_ESCAPE)

        /** Left Control modifier key. */
        public actual val CtrlLeft: Key
            get() = Key(KeyEvent.KEYCODE_CTRL_LEFT)

        /** Right Control modifier key. */
        public actual val CtrlRight: Key
            get() = Key(KeyEvent.KEYCODE_CTRL_RIGHT)

        /** Caps Lock key. */
        public actual val CapsLock: Key
            get() = Key(KeyEvent.KEYCODE_CAPS_LOCK)

        /** Scroll Lock key. */
        public actual val ScrollLock: Key
            get() = Key(KeyEvent.KEYCODE_SCROLL_LOCK)

        /** Left Meta modifier key. */
        public actual val MetaLeft: Key
            get() = Key(KeyEvent.KEYCODE_META_LEFT)

        /** Right Meta modifier key. */
        public actual val MetaRight: Key
            get() = Key(KeyEvent.KEYCODE_META_RIGHT)

        /** Function modifier key. */
        public actual val Function: Key
            get() = Key(KeyEvent.KEYCODE_FUNCTION)

        /** System Request / Print Screen key. */
        public actual val PrintScreen: Key
            get() = Key(KeyEvent.KEYCODE_SYSRQ)

        /** Break / Pause key. */
        public actual val Break: Key
            get() = Key(KeyEvent.KEYCODE_BREAK)

        /**
         * Home Movement key.
         *
         * Used for scrolling or moving the cursor around to the start of a line or to the top of a
         * list.
         */
        public actual val MoveHome: Key
            get() = Key(KeyEvent.KEYCODE_MOVE_HOME)

        /**
         * End Movement key.
         *
         * Used for scrolling or moving the cursor around to the end of a line or to the bottom of a
         * list.
         */
        public actual val MoveEnd: Key
            get() = Key(KeyEvent.KEYCODE_MOVE_END)

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        public actual val Insert: Key
            get() = Key(KeyEvent.KEYCODE_INSERT)

        /** Cut key. */
        public actual val Cut: Key
            get() = Key(KeyEvent.KEYCODE_CUT)

        /** Copy key. */
        public actual val Copy: Key
            get() = Key(KeyEvent.KEYCODE_COPY)

        /** Paste key. */
        public actual val Paste: Key
            get() = Key(KeyEvent.KEYCODE_PASTE)

        /** '`' (backtick) key. */
        public actual val Grave: Key
            get() = Key(KeyEvent.KEYCODE_GRAVE)

        /** '[' key. */
        public actual val LeftBracket: Key
            get() = Key(KeyEvent.KEYCODE_LEFT_BRACKET)

        /** ']' key. */
        public actual val RightBracket: Key
            get() = Key(KeyEvent.KEYCODE_RIGHT_BRACKET)

        /** '/' key. */
        public actual val Slash: Key
            get() = Key(KeyEvent.KEYCODE_SLASH)

        /** '\' key. */
        public actual val Backslash: Key
            get() = Key(KeyEvent.KEYCODE_BACKSLASH)

        /** ';' key. */
        public actual val Semicolon: Key
            get() = Key(KeyEvent.KEYCODE_SEMICOLON)

        /** ''' (apostrophe) key. */
        public actual val Apostrophe: Key
            get() = Key(KeyEvent.KEYCODE_APOSTROPHE)

        /** '@' key. */
        public actual val At: Key
            get() = Key(KeyEvent.KEYCODE_AT)

        /**
         * Number modifier key.
         *
         * Used to enter numeric symbols. This key is not Num Lock; it is more like [AltLeft].
         */
        public actual val Number: Key
            get() = Key(KeyEvent.KEYCODE_NUM)

        /**
         * Headset Hook key.
         *
         * Used to hang up calls and stop media.
         */
        public actual val HeadsetHook: Key
            get() = Key(KeyEvent.KEYCODE_HEADSETHOOK)

        /**
         * Camera Focus key.
         *
         * Used to focus the camera.
         */
        public actual val Focus: Key
            get() = Key(KeyEvent.KEYCODE_FOCUS)

        /** Menu key. */
        public actual val Menu: Key
            get() = Key(KeyEvent.KEYCODE_MENU)

        /** Notification key. */
        public actual val Notification: Key
            get() = Key(KeyEvent.KEYCODE_NOTIFICATION)

        /** Search key. */
        public actual val Search: Key
            get() = Key(KeyEvent.KEYCODE_SEARCH)

        /** Page Up key. */
        public actual val PageUp: Key
            get() = Key(KeyEvent.KEYCODE_PAGE_UP)

        /** Page Down key. */
        public actual val PageDown: Key
            get() = Key(KeyEvent.KEYCODE_PAGE_DOWN)

        /**
         * Picture Symbols modifier key.
         *
         * Used to switch symbol sets (Emoji, Kao-moji).
         */
        public actual val PictureSymbols: Key
            get() = Key(KeyEvent.KEYCODE_PICTSYMBOLS)

        /**
         * Switch Charset modifier key.
         *
         * Used to switch character sets (Kanji, Katakana).
         */
        public actual val SwitchCharset: Key
            get() = Key(KeyEvent.KEYCODE_SWITCH_CHARSET)

        /**
         * A Button key.
         *
         * On a game controller, the A button should be either the button labeled A or the first
         * button on the bottom row of controller buttons.
         */
        public actual val ButtonA: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_A)

        /**
         * B Button key.
         *
         * On a game controller, the B button should be either the button labeled B or the second
         * button on the bottom row of controller buttons.
         */
        public actual val ButtonB: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_B)

        /**
         * C Button key.
         *
         * On a game controller, the C button should be either the button labeled C or the third
         * button on the bottom row of controller buttons.
         */
        public actual val ButtonC: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_C)

        /**
         * X Button key.
         *
         * On a game controller, the X button should be either the button labeled X or the first
         * button on the upper row of controller buttons.
         */
        public actual val ButtonX: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_X)

        /**
         * Y Button key.
         *
         * On a game controller, the Y button should be either the button labeled Y or the second
         * button on the upper row of controller buttons.
         */
        public actual val ButtonY: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_Y)

        /**
         * Z Button key.
         *
         * On a game controller, the Z button should be either the button labeled Z or the third
         * button on the upper row of controller buttons.
         */
        public actual val ButtonZ: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_Z)

        /**
         * L1 Button key.
         *
         * On a game controller, the L1 button should be either the button labeled L1 (or L) or the
         * top left trigger button.
         */
        public actual val ButtonL1: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_L1)

        /**
         * R1 Button key.
         *
         * On a game controller, the R1 button should be either the button labeled R1 (or R) or the
         * top right trigger button.
         */
        public actual val ButtonR1: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_R1)

        /**
         * L2 Button key.
         *
         * On a game controller, the L2 button should be either the button labeled L2 or the bottom
         * left trigger button.
         */
        public actual val ButtonL2: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_L2)

        /**
         * R2 Button key.
         *
         * On a game controller, the R2 button should be either the button labeled R2 or the bottom
         * right trigger button.
         */
        public actual val ButtonR2: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_R2)

        /**
         * Left Thumb Button key.
         *
         * On a game controller, the left thumb button indicates that the left (or only) joystick is
         * pressed.
         */
        public actual val ButtonThumbLeft: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_THUMBL)

        /**
         * Right Thumb Button key.
         *
         * On a game controller, the right thumb button indicates that the right joystick is
         * pressed.
         */
        public actual val ButtonThumbRight: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_THUMBR)

        /**
         * Start Button key.
         *
         * On a game controller, the button labeled Start.
         */
        public actual val ButtonStart: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_START)

        /**
         * Select Button key.
         *
         * On a game controller, the button labeled Select.
         */
        public actual val ButtonSelect: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_SELECT)

        /**
         * Mode Button key.
         *
         * On a game controller, the button labeled Mode.
         */
        public actual val ButtonMode: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_MODE)

        /** Generic Game Pad Button #1. */
        public actual val Button1: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_1)

        /** Generic Game Pad Button #2. */
        public actual val Button2: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_2)

        /** Generic Game Pad Button #3. */
        public actual val Button3: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_3)

        /** Generic Game Pad Button #4. */
        public actual val Button4: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_4)

        /** Generic Game Pad Button #5. */
        public actual val Button5: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_5)

        /** Generic Game Pad Button #6. */
        public actual val Button6: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_6)

        /** Generic Game Pad Button #7. */
        public actual val Button7: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_7)

        /** Generic Game Pad Button #8. */
        public actual val Button8: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_8)

        /** Generic Game Pad Button #9. */
        public actual val Button9: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_9)

        /** Generic Game Pad Button #10. */
        public actual val Button10: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_10)

        /** Generic Game Pad Button #11. */
        public actual val Button11: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_11)

        /** Generic Game Pad Button #12. */
        public actual val Button12: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_12)

        /** Generic Game Pad Button #13. */
        public actual val Button13: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_13)

        /** Generic Game Pad Button #14. */
        public actual val Button14: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_14)

        /** Generic Game Pad Button #15. */
        public actual val Button15: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_15)

        /** Generic Game Pad Button #16. */
        public actual val Button16: Key
            get() = Key(KeyEvent.KEYCODE_BUTTON_16)

        /**
         * Forward key.
         *
         * Navigates forward in the history stack. Complement of [Back].
         */
        public actual val Forward: Key
            get() = Key(KeyEvent.KEYCODE_FORWARD)

        /** F1 key. */
        public actual val F1: Key
            get() = Key(KeyEvent.KEYCODE_F1)

        /** F2 key. */
        public actual val F2: Key
            get() = Key(KeyEvent.KEYCODE_F2)

        /** F3 key. */
        public actual val F3: Key
            get() = Key(KeyEvent.KEYCODE_F3)

        /** F4 key. */
        public actual val F4: Key
            get() = Key(KeyEvent.KEYCODE_F4)

        /** F5 key. */
        public actual val F5: Key
            get() = Key(KeyEvent.KEYCODE_F5)

        /** F6 key. */
        public actual val F6: Key
            get() = Key(KeyEvent.KEYCODE_F6)

        /** F7 key. */
        public actual val F7: Key
            get() = Key(KeyEvent.KEYCODE_F7)

        /** F8 key. */
        public actual val F8: Key
            get() = Key(KeyEvent.KEYCODE_F8)

        /** F9 key. */
        public actual val F9: Key
            get() = Key(KeyEvent.KEYCODE_F9)

        /** F10 key. */
        public actual val F10: Key
            get() = Key(KeyEvent.KEYCODE_F10)

        /** F11 key. */
        public actual val F11: Key
            get() = Key(KeyEvent.KEYCODE_F11)

        /** F12 key. */
        public actual val F12: Key
            get() = Key(KeyEvent.KEYCODE_F12)

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number]. This key alters the behavior of
         * other keys on the numeric keypad.
         */
        public actual val NumLock: Key
            get() = Key(KeyEvent.KEYCODE_NUM_LOCK)

        /** Numeric keypad '0' key. */
        public actual val NumPad0: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_0)

        /** Numeric keypad '1' key. */
        public actual val NumPad1: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_1)

        /** Numeric keypad '2' key. */
        public actual val NumPad2: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_2)

        /** Numeric keypad '3' key. */
        public actual val NumPad3: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_3)

        /** Numeric keypad '4' key. */
        public actual val NumPad4: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_4)

        /** Numeric keypad '5' key. */
        public actual val NumPad5: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_5)

        /** Numeric keypad '6' key. */
        public actual val NumPad6: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_6)

        /** Numeric keypad '7' key. */
        public actual val NumPad7: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_7)

        /** Numeric keypad '8' key. */
        public actual val NumPad8: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_8)

        /** Numeric keypad '9' key. */
        public actual val NumPad9: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_9)

        /** Numeric keypad '/' key (for division). */
        public actual val NumPadDivide: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_DIVIDE)

        /** Numeric keypad '*' key (for multiplication). */
        public actual val NumPadMultiply: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_MULTIPLY)

        /** Numeric keypad '-' key (for subtraction). */
        public actual val NumPadSubtract: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_SUBTRACT)

        /** Numeric keypad '+' key (for addition). */
        public actual val NumPadAdd: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_ADD)

        /** Numeric keypad '.' key (for decimals or digit grouping). */
        public actual val NumPadDot: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_DOT)

        /** Numeric keypad ',' key (for decimals or digit grouping). */
        public actual val NumPadComma: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_COMMA)

        /** Numeric keypad Enter key. */
        public actual val NumPadEnter: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_ENTER)

        /** Numeric keypad '=' key. */
        public actual val NumPadEquals: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_EQUALS)

        /** Numeric keypad '(' key. */
        public actual val NumPadLeftParenthesis: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN)

        /** Numeric keypad ')' key. */
        public actual val NumPadRightParenthesis: Key
            get() = Key(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN)

        /** Play media key. */
        public actual val MediaPlay: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_PLAY)

        /** Pause media key. */
        public actual val MediaPause: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_PAUSE)

        /** Play/Pause media key. */
        public actual val MediaPlayPause: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

        /** Stop media key. */
        public actual val MediaStop: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_STOP)

        /** Record media key. */
        public actual val MediaRecord: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_RECORD)

        /** Play Next media key. */
        public actual val MediaNext: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_NEXT)

        /** Play Previous media key. */
        public actual val MediaPrevious: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        /** Rewind media key. */
        public actual val MediaRewind: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_REWIND)

        /** Fast Forward media key. */
        public actual val MediaFastForward: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

        /**
         * Close media key.
         *
         * May be used to close a CD tray, for example.
         */
        public actual val MediaClose: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_CLOSE)

        /**
         * Audio Track key.
         *
         * Switches the audio tracks.
         */
        public actual val MediaAudioTrack: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK)

        /**
         * Eject media key.
         *
         * May be used to eject a CD tray, for example.
         */
        public actual val MediaEject: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_EJECT)

        /**
         * Media Top Menu key.
         *
         * Goes to the top of media menu.
         */
        public actual val MediaTopMenu: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_TOP_MENU)

        /** Skip forward media key. */
        public actual val MediaSkipForward: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD)

        /** Skip backward media key. */
        public actual val MediaSkipBackward: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)

        /**
         * Step forward media key.
         *
         * Steps media forward, one frame at a time.
         */
        public actual val MediaStepForward: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_STEP_FORWARD)

        /**
         * Step backward media key.
         *
         * Steps media backward, one frame at a time.
         */
        public actual val MediaStepBackward: Key
            get() = Key(KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)

        /**
         * Mute key.
         *
         * Mutes the microphone, unlike [VolumeMute].
         */
        public actual val MicrophoneMute: Key
            get() = Key(KeyEvent.KEYCODE_MUTE)

        /**
         * Volume Mute key.
         *
         * Mutes the speaker, unlike [MicrophoneMute].
         *
         * This key should normally be implemented as a toggle such that the first press mutes the
         * speaker and the second press restores the original volume.
         */
        public actual val VolumeMute: Key
            get() = Key(KeyEvent.KEYCODE_VOLUME_MUTE)

        /**
         * Info key.
         *
         * Common on TV remotes to show additional information related to what is currently being
         * viewed.
         */
        public actual val Info: Key
            get() = Key(KeyEvent.KEYCODE_INFO)

        /**
         * Channel up key.
         *
         * On TV remotes, increments the television channel.
         */
        public actual val ChannelUp: Key
            get() = Key(KeyEvent.KEYCODE_CHANNEL_UP)

        /**
         * Channel down key.
         *
         * On TV remotes, decrements the television channel.
         */
        public actual val ChannelDown: Key
            get() = Key(KeyEvent.KEYCODE_CHANNEL_DOWN)

        /** Zoom in key. */
        public actual val ZoomIn: Key
            get() = Key(KeyEvent.KEYCODE_ZOOM_IN)

        /** Zoom out key. */
        public actual val ZoomOut: Key
            get() = Key(KeyEvent.KEYCODE_ZOOM_OUT)

        /**
         * TV key.
         *
         * On TV remotes, switches to viewing live TV.
         */
        public actual val Tv: Key
            get() = Key(KeyEvent.KEYCODE_TV)

        /**
         * Window key.
         *
         * On TV remotes, toggles picture-in-picture mode or other windowing functions. On Android
         * Wear devices, triggers a display offset.
         */
        public actual val Window: Key
            get() = Key(KeyEvent.KEYCODE_WINDOW)

        /**
         * Guide key.
         *
         * On TV remotes, shows a programming guide.
         */
        public actual val Guide: Key
            get() = Key(KeyEvent.KEYCODE_GUIDE)

        /**
         * DVR key.
         *
         * On some TV remotes, switches to a DVR mode for recorded shows.
         */
        public actual val Dvr: Key
            get() = Key(KeyEvent.KEYCODE_DVR)

        /**
         * Bookmark key.
         *
         * On some TV remotes, bookmarks content or web pages.
         */
        public actual val Bookmark: Key
            get() = Key(KeyEvent.KEYCODE_BOOKMARK)

        /**
         * Toggle captions key.
         *
         * Switches the mode for closed-captioning text, for example during television shows.
         */
        public actual val Captions: Key
            get() = Key(KeyEvent.KEYCODE_CAPTIONS)

        /**
         * Settings key.
         *
         * Starts the system settings activity.
         */
        public actual val Settings: Key
            get() = Key(KeyEvent.KEYCODE_SETTINGS)

        /**
         * TV power key.
         *
         * On TV remotes, toggles the power on a television screen.
         */
        public actual val TvPower: Key
            get() = Key(KeyEvent.KEYCODE_TV_POWER)

        /**
         * TV input key.
         *
         * On TV remotes, switches the input on a television screen.
         */
        public actual val TvInput: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT)

        /**
         * Set-top-box power key.
         *
         * On TV remotes, toggles the power on an external Set-top-box.
         */
        public actual val SetTopBoxPower: Key
            get() = Key(KeyEvent.KEYCODE_STB_POWER)

        /**
         * Set-top-box input key.
         *
         * On TV remotes, switches the input mode on an external Set-top-box.
         */
        public actual val SetTopBoxInput: Key
            get() = Key(KeyEvent.KEYCODE_STB_INPUT)

        /**
         * A/V Receiver power key.
         *
         * On TV remotes, toggles the power on an external A/V Receiver.
         */
        public actual val AvReceiverPower: Key
            get() = Key(KeyEvent.KEYCODE_AVR_POWER)

        /**
         * A/V Receiver input key.
         *
         * On TV remotes, switches the input mode on an external A/V Receiver.
         */
        public actual val AvReceiverInput: Key
            get() = Key(KeyEvent.KEYCODE_AVR_INPUT)

        /**
         * Red "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public actual val ProgramRed: Key
            get() = Key(KeyEvent.KEYCODE_PROG_RED)

        /**
         * Green "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public actual val ProgramGreen: Key
            get() = Key(KeyEvent.KEYCODE_PROG_GREEN)

        /**
         * Yellow "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public actual val ProgramYellow: Key
            get() = Key(KeyEvent.KEYCODE_PROG_YELLOW)

        /**
         * Blue "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public actual val ProgramBlue: Key
            get() = Key(KeyEvent.KEYCODE_PROG_BLUE)

        /**
         * App switch key.
         *
         * Should bring up the application switcher dialog.
         */
        public actual val AppSwitch: Key
            get() = Key(KeyEvent.KEYCODE_APP_SWITCH)

        /**
         * Language Switch key.
         *
         * Toggles the current input language such as switching between English and Japanese on a
         * QWERTY keyboard. On some devices, the same function may be performed by pressing
         * Shift+Space.
         */
        public actual val LanguageSwitch: Key
            get() = Key(KeyEvent.KEYCODE_LANGUAGE_SWITCH)

        /**
         * Manner Mode key.
         *
         * Toggles silent or vibrate mode on and off to make the device behave more politely in
         * certain settings such as on a crowded train. On some devices, the key may only operate
         * when long-pressed.
         */
        public actual val MannerMode: Key
            get() = Key(KeyEvent.KEYCODE_MANNER_MODE)

        /**
         * 3D Mode key.
         *
         * Toggles the display between 2D and 3D mode.
         */
        public actual val Toggle2D3D: Key
            get() = Key(KeyEvent.KEYCODE_3D_MODE)

        /**
         * Contacts special function key.
         *
         * Used to launch an address book application.
         */
        public actual val Contacts: Key
            get() = Key(KeyEvent.KEYCODE_CONTACTS)

        /**
         * Calendar special function key.
         *
         * Used to launch a calendar application.
         */
        public actual val Calendar: Key
            get() = Key(KeyEvent.KEYCODE_CALENDAR)

        /**
         * Music special function key.
         *
         * Used to launch a music player application.
         */
        public actual val Music: Key
            get() = Key(KeyEvent.KEYCODE_MUSIC)

        /**
         * Calculator special function key.
         *
         * Used to launch a calculator application.
         */
        public actual val Calculator: Key
            get() = Key(KeyEvent.KEYCODE_CALCULATOR)

        /** Japanese full-width / half-width key. */
        public actual val ZenkakuHankaru: Key
            get() = Key(KeyEvent.KEYCODE_ZENKAKU_HANKAKU)

        /** Japanese alphanumeric key. */
        public actual val Eisu: Key
            get() = Key(KeyEvent.KEYCODE_EISU)

        /** Japanese non-conversion key. */
        public actual val Muhenkan: Key
            get() = Key(KeyEvent.KEYCODE_MUHENKAN)

        /** Japanese conversion key. */
        public actual val Henkan: Key
            get() = Key(KeyEvent.KEYCODE_HENKAN)

        /** Japanese katakana / hiragana key. */
        public actual val KatakanaHiragana: Key
            get() = Key(KeyEvent.KEYCODE_KATAKANA_HIRAGANA)

        /** Japanese Yen key. */
        public actual val Yen: Key
            get() = Key(KeyEvent.KEYCODE_YEN)

        /** Japanese Ro key. */
        public actual val Ro: Key
            get() = Key(KeyEvent.KEYCODE_RO)

        /** Japanese kana key. */
        public actual val Kana: Key
            get() = Key(KeyEvent.KEYCODE_KANA)

        /**
         * Assist key.
         *
         * Launches the global assist activity. Not delivered to applications.
         */
        public actual val Assist: Key
            get() = Key(KeyEvent.KEYCODE_ASSIST)

        /**
         * Brightness Down key.
         *
         * Adjusts the screen brightness down.
         */
        public actual val BrightnessDown: Key
            get() = Key(KeyEvent.KEYCODE_BRIGHTNESS_DOWN)

        /**
         * Brightness Up key.
         *
         * Adjusts the screen brightness up.
         */
        public actual val BrightnessUp: Key
            get() = Key(KeyEvent.KEYCODE_BRIGHTNESS_UP)

        /**
         * Sleep key.
         *
         * Puts the device to sleep. Behaves somewhat like [Power] but it has no effect if the
         * device is already asleep.
         */
        public actual val Sleep: Key
            get() = Key(KeyEvent.KEYCODE_SLEEP)

        /**
         * Wakeup key.
         *
         * Wakes up the device. Behaves somewhat like [Power] but it has no effect if the device is
         * already awake.
         */
        public actual val WakeUp: Key
            get() = Key(KeyEvent.KEYCODE_WAKEUP)

        /** Put device to sleep unless a wakelock is held. */
        public actual val SoftSleep: Key
            get() = Key(KeyEvent.KEYCODE_SOFT_SLEEP)

        /**
         * Pairing key.
         *
         * Initiates peripheral pairing mode. Useful for pairing remote control devices or game
         * controllers, especially if no other input mode is available.
         */
        public actual val Pairing: Key
            get() = Key(KeyEvent.KEYCODE_PAIRING)

        /**
         * Last Channel key.
         *
         * Goes to the last viewed channel.
         */
        public actual val LastChannel: Key
            get() = Key(KeyEvent.KEYCODE_LAST_CHANNEL)

        /**
         * TV data service key.
         *
         * Displays data services like weather, sports.
         */
        public actual val TvDataService: Key
            get() = Key(KeyEvent.KEYCODE_TV_DATA_SERVICE)

        /**
         * Voice Assist key.
         *
         * Launches the global voice assist activity. Not delivered to applications.
         */
        public actual val VoiceAssist: Key
            get() = Key(KeyEvent.KEYCODE_VOICE_ASSIST)

        /**
         * Radio key.
         *
         * Toggles TV service / Radio service.
         */
        public actual val TvRadioService: Key
            get() = Key(KeyEvent.KEYCODE_TV_RADIO_SERVICE)

        /**
         * Teletext key.
         *
         * Displays Teletext service.
         */
        public actual val TvTeletext: Key
            get() = Key(KeyEvent.KEYCODE_TV_TELETEXT)

        /**
         * Number entry key.
         *
         * Initiates to enter multi-digit channel number when each digit key is assigned for
         * selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC User Control
         * Code.
         */
        public actual val TvNumberEntry: Key
            get() = Key(KeyEvent.KEYCODE_TV_NUMBER_ENTRY)

        /**
         * Analog Terrestrial key.
         *
         * Switches to analog terrestrial broadcast service.
         */
        public actual val TvTerrestrialAnalog: Key
            get() = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG)

        /**
         * Digital Terrestrial key.
         *
         * Switches to digital terrestrial broadcast service.
         */
        public actual val TvTerrestrialDigital: Key
            get() = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL)

        /**
         * Satellite key.
         *
         * Switches to digital satellite broadcast service.
         */
        public actual val TvSatellite: Key
            get() = Key(KeyEvent.KEYCODE_TV_SATELLITE)

        /**
         * BS key.
         *
         * Switches to BS digital satellite broadcasting service available in Japan.
         */
        public actual val TvSatelliteBs: Key
            get() = Key(KeyEvent.KEYCODE_TV_SATELLITE_BS)

        /**
         * CS key.
         *
         * Switches to CS digital satellite broadcasting service available in Japan.
         */
        public actual val TvSatelliteCs: Key
            get() = Key(KeyEvent.KEYCODE_TV_SATELLITE_CS)

        /**
         * BS/CS key.
         *
         * Toggles between BS and CS digital satellite services.
         */
        public actual val TvSatelliteService: Key
            get() = Key(KeyEvent.KEYCODE_TV_SATELLITE_SERVICE)

        /**
         * Toggle Network key.
         *
         * Toggles selecting broadcast services.
         */
        public actual val TvNetwork: Key
            get() = Key(KeyEvent.KEYCODE_TV_NETWORK)

        /**
         * Antenna/Cable key.
         *
         * Toggles broadcast input source between antenna and cable.
         */
        public actual val TvAntennaCable: Key
            get() = Key(KeyEvent.KEYCODE_TV_ANTENNA_CABLE)

        /**
         * HDMI #1 key.
         *
         * Switches to HDMI input #1.
         */
        public actual val TvInputHdmi1: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_1)

        /**
         * HDMI #2 key.
         *
         * Switches to HDMI input #2.
         */
        public actual val TvInputHdmi2: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_2)

        /**
         * HDMI #3 key.
         *
         * Switches to HDMI input #3.
         */
        public actual val TvInputHdmi3: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_3)

        /**
         * HDMI #4 key.
         *
         * Switches to HDMI input #4.
         */
        public actual val TvInputHdmi4: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_4)

        /**
         * Composite #1 key.
         *
         * Switches to composite video input #1.
         */
        public actual val TvInputComposite1: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1)

        /**
         * Composite #2 key.
         *
         * Switches to composite video input #2.
         */
        public actual val TvInputComposite2: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2)

        /**
         * Component #1 key.
         *
         * Switches to component video input #1.
         */
        public actual val TvInputComponent1: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1)

        /**
         * Component #2 key.
         *
         * Switches to component video input #2.
         */
        public actual val TvInputComponent2: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2)

        /**
         * VGA #1 key.
         *
         * Switches to VGA (analog RGB) input #1.
         */
        public actual val TvInputVga1: Key
            get() = Key(KeyEvent.KEYCODE_TV_INPUT_VGA_1)

        /**
         * Audio description key.
         *
         * Toggles audio description off / on.
         */
        public actual val TvAudioDescription: Key
            get() = Key(KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION)

        /**
         * Audio description mixing volume up key.
         *
         * Increase the audio description volume as compared with normal audio volume.
         */
        public actual val TvAudioDescriptionMixingVolumeUp: Key
            get() = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP)

        /**
         * Audio description mixing volume down key.
         *
         * Lessen audio description volume as compared with normal audio volume.
         */
        public actual val TvAudioDescriptionMixingVolumeDown: Key
            get() = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN)

        /**
         * Zoom mode key.
         *
         * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.)
         */
        public actual val TvZoomMode: Key
            get() = Key(KeyEvent.KEYCODE_TV_ZOOM_MODE)

        /**
         * Contents menu key.
         *
         * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control Code
         */
        public actual val TvContentsMenu: Key
            get() = Key(KeyEvent.KEYCODE_TV_CONTENTS_MENU)

        /**
         * Media context menu key.
         *
         * Goes to the context menu of media contents. Corresponds to Media Context-sensitive Menu
         * (0x11) of CEC User Control Code.
         */
        public actual val TvMediaContextMenu: Key
            get() = Key(KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU)

        /**
         * Timer programming key.
         *
         * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of CEC User
         * Control Code.
         */
        public actual val TvTimerProgramming: Key
            get() = Key(KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING)

        /**
         * Primary stem key for Wearables.
         *
         * Main power/reset button.
         */
        public actual val StemPrimary: Key
            get() = Key(KeyEvent.KEYCODE_STEM_PRIMARY)

        /** Generic stem key 1 for Wearables. */
        public actual val Stem1: Key
            get() = Key(KeyEvent.KEYCODE_STEM_1)

        /** Generic stem key 2 for Wearables. */
        public actual val Stem2: Key
            get() = Key(KeyEvent.KEYCODE_STEM_2)

        /** Generic stem key 3 for Wearables. */
        public actual val Stem3: Key
            get() = Key(KeyEvent.KEYCODE_STEM_3)

        /** Show all apps. */
        public actual val AllApps: Key
            get() = Key(KeyEvent.KEYCODE_ALL_APPS)

        /** Refresh key. */
        public actual val Refresh: Key
            get() = Key(KeyEvent.KEYCODE_REFRESH)

        /** Thumbs up key. Apps can use this to let user up-vote content. */
        public actual val ThumbsUp: Key
            get() = Key(KeyEvent.KEYCODE_THUMBS_UP)

        /** Thumbs down key. Apps can use this to let user down-vote content. */
        public actual val ThumbsDown: Key
            get() = Key(KeyEvent.KEYCODE_THUMBS_DOWN)

        /**
         * Used to switch current [account][android.accounts.Account] that is consuming content. May
         * be consumed by system to set account globally.
         */
        public actual val ProfileSwitch: Key
            get() = Key(KeyEvent.KEYCODE_PROFILE_SWITCH)

        // Keys that don't exist on Android.
        // The values are just consecutive negative numbers which hopefully don't correspond to any
        // real keycodes.

        /** Numeric keypad Up Arrow Key. Unsupported on Android. */
        public actual val NumPadDirectionUp: Key
            get() = Key(-1000000001)

        /** Numeric keypad Down Arrow Key. Unsupported on Android. */
        public actual val NumPadDirectionDown: Key
            get() = Key(-1000000002)

        /** Numeric keypad Left Arrow Key. Unsupported on Android. */
        public actual val NumPadDirectionLeft: Key
            get() = Key(-1000000003)

        /** Numeric keypad Right Arrow Key. Unsupported on Android. */
        public actual val NumPadDirectionRight: Key
            get() = Key(-1000000004)

        /** Numeric keypad Home Key. Unsupported on Android. */
        public actual val NumPadMoveHome: Key
            get() = Key(-1000000005)

        /** Numeric keypad End Key. Unsupported on Android. */
        public actual val NumPadMoveEnd: Key
            get() = Key(-1000000006)

        /** Numeric keypad Page Up Key. Unsupported on Android. */
        public actual val NumPadPageUp: Key
            get() = Key(-1000000007)

        /** Numeric keypad Page Down Key. Unsupported on Android. */
        public actual val NumPadPageDown: Key
            get() = Key(-1000000008)

        /** Numeric keypad Insert Key. Unsupported on Android. */
        public actual val NumPadInsert: Key
            get() = Key(-1000000009)

        /** Numeric keypad Delete key. Unsupported on Android. */
        public actual val NumPadDelete: Key
            get() = Key(-1000000010)
    }

    public actual override fun toString(): String = "Key code: $keyCode"
}

/** The native keycode corresponding to this [Key]. */
public val Key.nativeKeyCode: Int
    get() = unpackInt1(keyCode)

public fun Key(nativeKeyCode: Int): Key = Key(packInts(nativeKeyCode, 0))
