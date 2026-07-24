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

/**
 * Represents keys on a keyboard.
 *
 * @param keyCode a Long value representing the key pressed. Note: This keycode can be used to
 *   uniquely identify a hardware key. It is different from the native keycode.
 * @sample androidx.compose.ui.samples.KeyEventIsAltPressedSample
 */
@kotlin.jvm.JvmInline
public expect value class Key(public val keyCode: Long) {
    public companion object {
        /** Unknown key. */
        public val Unknown: Key

        /**
         * Soft Left key.
         *
         * Usually situated below the display on phones and used as a multi-function feature key for
         * selecting a software defined function shown on the bottom left of the display.
         */
        public val SoftLeft: Key

        /**
         * Soft Right key.
         *
         * Usually situated below the display on phones and used as a multi-function feature key for
         * selecting a software defined function shown on the bottom right of the display.
         */
        public val SoftRight: Key

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
        public val Home: Key

        /**
         * System Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        public val SystemHome: Key

        /** Back key. */
        public val Back: Key

        /** Help key. */
        public val Help: Key

        /**
         * Navigate to previous key.
         *
         * Goes backward by one item in an ordered collection of items.
         */
        public val NavigatePrevious: Key

        /**
         * Navigate to next key.
         *
         * Advances to the next item in an ordered collection of items.
         */
        public val NavigateNext: Key

        /**
         * Navigate in key.
         *
         * Activates the item that currently has focus or expands to the next level of a navigation
         * hierarchy.
         */
        public val NavigateIn: Key

        /**
         * Navigate out key.
         *
         * Backs out one level of a navigation hierarchy or collapses the item that currently has
         * focus.
         */
        public val NavigateOut: Key

        /** Consumed by the system for navigation up. */
        public val SystemNavigationUp: Key

        /** Consumed by the system for navigation down. */
        public val SystemNavigationDown: Key

        /** Consumed by the system for navigation left. */
        public val SystemNavigationLeft: Key

        /** Consumed by the system for navigation right. */
        public val SystemNavigationRight: Key

        /** Call key. */
        public val Call: Key

        /** End Call key. */
        public val EndCall: Key

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        public val DirectionUp: Key

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        public val DirectionDown: Key

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        public val DirectionLeft: Key

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        public val DirectionRight: Key

        /**
         * Center Arrow Key / Directional Pad Center key.
         *
         * May also be synthesized from trackball motions.
         */
        public val DirectionCenter: Key

        /** Directional Pad Up-Left. */
        public val DirectionUpLeft: Key

        /** Directional Pad Down-Left. */
        public val DirectionDownLeft: Key

        /** Directional Pad Up-Right. */
        public val DirectionUpRight: Key

        /** Directional Pad Down-Right. */
        public val DirectionDownRight: Key

        /**
         * Volume Up key.
         *
         * Adjusts the speaker volume up.
         */
        public val VolumeUp: Key

        /**
         * Volume Down key.
         *
         * Adjusts the speaker volume down.
         */
        public val VolumeDown: Key

        /** Power key. */
        public val Power: Key

        /**
         * Camera key.
         *
         * Used to launch a camera application or take pictures.
         */
        public val Camera: Key

        /** Clear key. */
        public val Clear: Key

        /** '0' key. */
        public val Zero: Key

        /** '1' key. */
        public val One: Key

        /** '2' key. */
        public val Two: Key

        /** '3' key. */
        public val Three: Key

        /** '4' key. */
        public val Four: Key

        /** '5' key. */
        public val Five: Key

        /** '6' key. */
        public val Six: Key

        /** '7' key. */
        public val Seven: Key

        /** '8' key. */
        public val Eight: Key

        /** '9' key. */
        public val Nine: Key

        /** '+' key. */
        public val Plus: Key

        /** '-' key. */
        public val Minus: Key

        /** '*' key. */
        public val Multiply: Key

        /** '=' key. */
        public val Equals: Key

        /** '#' key. */
        public val Pound: Key

        /** 'A' key. */
        public val A: Key

        /** 'B' key. */
        public val B: Key

        /** 'C' key. */
        public val C: Key

        /** 'D' key. */
        public val D: Key

        /** 'E' key. */
        public val E: Key

        /** 'F' key. */
        public val F: Key

        /** 'G' key. */
        public val G: Key

        /** 'H' key. */
        public val H: Key

        /** 'I' key. */
        public val I: Key

        /** 'J' key. */
        public val J: Key

        /** 'K' key. */
        public val K: Key

        /** 'L' key. */
        public val L: Key

        /** 'M' key. */
        public val M: Key

        /** 'N' key. */
        public val N: Key

        /** 'O' key. */
        public val O: Key

        /** 'P' key. */
        public val P: Key

        /** 'Q' key. */
        public val Q: Key

        /** 'R' key. */
        public val R: Key

        /** 'S' key. */
        public val S: Key

        /** 'T' key. */
        public val T: Key

        /** 'U' key. */
        public val U: Key

        /** 'V' key. */
        public val V: Key

        /** 'W' key. */
        public val W: Key

        /** 'X' key. */
        public val X: Key

        /** 'Y' key. */
        public val Y: Key

        /** 'Z' key. */
        public val Z: Key

        /** ',' key. */
        public val Comma: Key

        /** '.' key. */
        public val Period: Key

        /** Left Alt modifier key. */
        public val AltLeft: Key

        /** Right Alt modifier key. */
        public val AltRight: Key

        /** Left Shift modifier key. */
        public val ShiftLeft: Key

        /** Right Shift modifier key. */
        public val ShiftRight: Key

        /** Tab key. */
        public val Tab: Key

        /** Space key. */
        public val Spacebar: Key

        /**
         * Symbol modifier key.
         *
         * Used to enter alternate symbols.
         */
        public val Symbol: Key

        /**
         * Browser special function key.
         *
         * Used to launch a browser application.
         */
        public val Browser: Key

        /**
         * Envelope special function key.
         *
         * Used to launch a mail application.
         */
        public val Envelope: Key

        /** Enter key. */
        public val Enter: Key

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        public val Backspace: Key

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        public val Delete: Key

        /** Escape key. */
        public val Escape: Key

        /** Left Control modifier key. */
        public val CtrlLeft: Key

        /** Right Control modifier key. */
        public val CtrlRight: Key

        /** Caps Lock key. */
        public val CapsLock: Key

        /** Scroll Lock key. */
        public val ScrollLock: Key

        /** Left Meta modifier key. */
        public val MetaLeft: Key

        /** Right Meta modifier key. */
        public val MetaRight: Key

        /** Function modifier key. */
        public val Function: Key

        /** System Request / Print Screen key. */
        public val PrintScreen: Key

        /** Break / Pause key. */
        public val Break: Key

        /**
         * Home Movement key.
         *
         * Used for scrolling or moving the cursor around to the start of a line or to the top of a
         * list.
         */
        public val MoveHome: Key

        /**
         * End Movement key.
         *
         * Used for scrolling or moving the cursor around to the end of a line or to the bottom of a
         * list.
         */
        public val MoveEnd: Key

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        public val Insert: Key

        /** Cut key. */
        public val Cut: Key

        /** Copy key. */
        public val Copy: Key

        /** Paste key. */
        public val Paste: Key

        /** '`' (backtick) key. */
        public val Grave: Key

        /** '[' key. */
        public val LeftBracket: Key

        /** ']' key. */
        public val RightBracket: Key

        /** '/' key. */
        public val Slash: Key

        /** '\' key. */
        public val Backslash: Key

        /** ';' key. */
        public val Semicolon: Key

        /** ''' (apostrophe) key. */
        public val Apostrophe: Key

        /** '@' key. */
        public val At: Key

        /**
         * Number modifier key.
         *
         * Used to enter numeric symbols. This key is not Num Lock; it is more like [AltLeft].
         */
        public val Number: Key

        /**
         * Headset Hook key.
         *
         * Used to hang up calls and stop media.
         */
        public val HeadsetHook: Key

        /**
         * Camera Focus key.
         *
         * Used to focus the camera.
         */
        public val Focus: Key

        /** Menu key. */
        public val Menu: Key

        /** Notification key. */
        public val Notification: Key

        /** Search key. */
        public val Search: Key

        /** Page Up key. */
        public val PageUp: Key

        /** Page Down key. */
        public val PageDown: Key

        /**
         * Picture Symbols modifier key.
         *
         * Used to switch symbol sets (Emoji, Kao-moji).
         */
        public val PictureSymbols: Key

        /**
         * Switch Charset modifier key.
         *
         * Used to switch character sets (Kanji, Katakana).
         */
        public val SwitchCharset: Key

        /**
         * A Button key.
         *
         * On a game controller, the A button should be either the button labeled A or the first
         * button on the bottom row of controller buttons.
         */
        public val ButtonA: Key

        /**
         * B Button key.
         *
         * On a game controller, the B button should be either the button labeled B or the second
         * button on the bottom row of controller buttons.
         */
        public val ButtonB: Key

        /**
         * C Button key.
         *
         * On a game controller, the C button should be either the button labeled C or the third
         * button on the bottom row of controller buttons.
         */
        public val ButtonC: Key

        /**
         * X Button key.
         *
         * On a game controller, the X button should be either the button labeled X or the first
         * button on the upper row of controller buttons.
         */
        public val ButtonX: Key

        /**
         * Y Button key.
         *
         * On a game controller, the Y button should be either the button labeled Y or the second
         * button on the upper row of controller buttons.
         */
        public val ButtonY: Key

        /**
         * Z Button key.
         *
         * On a game controller, the Z button should be either the button labeled Z or the third
         * button on the upper row of controller buttons.
         */
        public val ButtonZ: Key

        /**
         * L1 Button key.
         *
         * On a game controller, the L1 button should be either the button labeled L1 (or L) or the
         * top left trigger button.
         */
        public val ButtonL1: Key

        /**
         * R1 Button key.
         *
         * On a game controller, the R1 button should be either the button labeled R1 (or R) or the
         * top right trigger button.
         */
        public val ButtonR1: Key

        /**
         * L2 Button key.
         *
         * On a game controller, the L2 button should be either the button labeled L2 or the bottom
         * left trigger button.
         */
        public val ButtonL2: Key

        /**
         * R2 Button key.
         *
         * On a game controller, the R2 button should be either the button labeled R2 or the bottom
         * right trigger button.
         */
        public val ButtonR2: Key

        /**
         * Left Thumb Button key.
         *
         * On a game controller, the left thumb button indicates that the left (or only) joystick is
         * pressed.
         */
        public val ButtonThumbLeft: Key

        /**
         * Right Thumb Button key.
         *
         * On a game controller, the right thumb button indicates that the right joystick is
         * pressed.
         */
        public val ButtonThumbRight: Key

        /**
         * Start Button key.
         *
         * On a game controller, the button labeled Start.
         */
        public val ButtonStart: Key

        /**
         * Select Button key.
         *
         * On a game controller, the button labeled Select.
         */
        public val ButtonSelect: Key

        /**
         * Mode Button key.
         *
         * On a game controller, the button labeled Mode.
         */
        public val ButtonMode: Key

        /** Generic Game Pad Button #1. */
        public val Button1: Key

        /** Generic Game Pad Button #2. */
        public val Button2: Key

        /** Generic Game Pad Button #3. */
        public val Button3: Key

        /** Generic Game Pad Button #4. */
        public val Button4: Key

        /** Generic Game Pad Button #5. */
        public val Button5: Key

        /** Generic Game Pad Button #6. */
        public val Button6: Key

        /** Generic Game Pad Button #7. */
        public val Button7: Key

        /** Generic Game Pad Button #8. */
        public val Button8: Key

        /** Generic Game Pad Button #9. */
        public val Button9: Key

        /** Generic Game Pad Button #10. */
        public val Button10: Key

        /** Generic Game Pad Button #11. */
        public val Button11: Key

        /** Generic Game Pad Button #12. */
        public val Button12: Key

        /** Generic Game Pad Button #13. */
        public val Button13: Key

        /** Generic Game Pad Button #14. */
        public val Button14: Key

        /** Generic Game Pad Button #15. */
        public val Button15: Key

        /** Generic Game Pad Button #16. */
        public val Button16: Key

        /**
         * Forward key.
         *
         * Navigates forward in the history stack. Complement of [Back].
         */
        public val Forward: Key

        /** F1 key. */
        public val F1: Key

        /** F2 key. */
        public val F2: Key

        /** F3 key. */
        public val F3: Key

        /** F4 key. */
        public val F4: Key

        /** F5 key. */
        public val F5: Key

        /** F6 key. */
        public val F6: Key

        /** F7 key. */
        public val F7: Key

        /** F8 key. */
        public val F8: Key

        /** F9 key. */
        public val F9: Key

        /** F10 key. */
        public val F10: Key

        /** F11 key. */
        public val F11: Key

        /** F12 key. */
        public val F12: Key

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number]. This key alters the behavior of
         * other keys on the numeric keypad.
         */
        public val NumLock: Key

        /** Numeric keypad '0' key. */
        public val NumPad0: Key

        /** Numeric keypad '1' key. */
        public val NumPad1: Key

        /** Numeric keypad '2' key. */
        public val NumPad2: Key

        /** Numeric keypad '3' key. */
        public val NumPad3: Key

        /** Numeric keypad '4' key. */
        public val NumPad4: Key

        /** Numeric keypad '5' key. */
        public val NumPad5: Key

        /** Numeric keypad '6' key. */
        public val NumPad6: Key

        /** Numeric keypad '7' key. */
        public val NumPad7: Key

        /** Numeric keypad '8' key. */
        public val NumPad8: Key

        /** Numeric keypad '9' key. */
        public val NumPad9: Key

        /** Numeric keypad '/' key (for division). */
        public val NumPadDivide: Key

        /** Numeric keypad '*' key (for multiplication). */
        public val NumPadMultiply: Key

        /** Numeric keypad '-' key (for subtraction). */
        public val NumPadSubtract: Key

        /** Numeric keypad '+' key (for addition). */
        public val NumPadAdd: Key

        /** Numeric keypad '.' key (for decimals or digit grouping). */
        public val NumPadDot: Key

        /** Numeric keypad ',' key (for decimals or digit grouping). */
        public val NumPadComma: Key

        /** Numeric keypad Enter key. */
        public val NumPadEnter: Key

        /** Numeric keypad '=' key. */
        public val NumPadEquals: Key

        /** Numeric keypad '(' key. */
        public val NumPadLeftParenthesis: Key

        /** Numeric keypad ')' key. */
        public val NumPadRightParenthesis: Key

        /** Numeric keypad Up Arrow Key. */
        public val NumPadDirectionUp: Key

        /** Numeric keypad Down Arrow Key. */
        public val NumPadDirectionDown: Key

        /** Numeric keypad Left Arrow Key. */
        public val NumPadDirectionLeft: Key

        /** Numeric keypad Right Arrow Key. */
        public val NumPadDirectionRight: Key

        /** Numeric keypad Home Key. */
        public val NumPadMoveHome: Key

        /** Numeric keypad End Key. */
        public val NumPadMoveEnd: Key

        /** Numeric keypad Page Up Key. */
        public val NumPadPageUp: Key

        /** Numeric keypad Page Down Key. */
        public val NumPadPageDown: Key

        /** Numeric keypad Insert Key. */
        public val NumPadInsert: Key

        /** Numeric keypad Delete Key. */
        public val NumPadDelete: Key

        /** Play media key. */
        public val MediaPlay: Key

        /** Pause media key. */
        public val MediaPause: Key

        /** Play/Pause media key. */
        public val MediaPlayPause: Key

        /** Stop media key. */
        public val MediaStop: Key

        /** Record media key. */
        public val MediaRecord: Key

        /** Play Next media key. */
        public val MediaNext: Key

        /** Play Previous media key. */
        public val MediaPrevious: Key

        /** Rewind media key. */
        public val MediaRewind: Key

        /** Fast Forward media key. */
        public val MediaFastForward: Key

        /**
         * Close media key.
         *
         * May be used to close a CD tray, for example.
         */
        public val MediaClose: Key

        /**
         * Audio Track key.
         *
         * Switches the audio tracks.
         */
        public val MediaAudioTrack: Key

        /**
         * Eject media key.
         *
         * May be used to eject a CD tray, for example.
         */
        public val MediaEject: Key

        /**
         * Media Top Menu key.
         *
         * Goes to the top of media menu.
         */
        public val MediaTopMenu: Key

        /** Skip forward media key. */
        public val MediaSkipForward: Key

        /** Skip backward media key. */
        public val MediaSkipBackward: Key

        /**
         * Step forward media key.
         *
         * Steps media forward, one frame at a time.
         */
        public val MediaStepForward: Key

        /**
         * Step backward media key.
         *
         * Steps media backward, one frame at a time.
         */
        public val MediaStepBackward: Key

        /**
         * Mute key.
         *
         * Mutes the microphone, unlike [VolumeMute].
         */
        public val MicrophoneMute: Key

        /**
         * Volume Mute key.
         *
         * Mutes the speaker, unlike [MicrophoneMute].
         *
         * This key should normally be implemented as a toggle such that the first press mutes the
         * speaker and the second press restores the original volume.
         */
        public val VolumeMute: Key

        /**
         * Info key.
         *
         * Common on TV remotes to show additional information related to what is currently being
         * viewed.
         */
        public val Info: Key

        /**
         * Channel up key.
         *
         * On TV remotes, increments the television channel.
         */
        public val ChannelUp: Key

        /**
         * Channel down key.
         *
         * On TV remotes, decrements the television channel.
         */
        public val ChannelDown: Key

        /** Zoom in key. */
        public val ZoomIn: Key

        /** Zoom out key. */
        public val ZoomOut: Key

        /**
         * TV key.
         *
         * On TV remotes, switches to viewing live TV.
         */
        public val Tv: Key

        /**
         * Window key.
         *
         * On TV remotes, toggles picture-in-picture mode or other windowing functions. On Android
         * Wear devices, triggers a display offset.
         */
        public val Window: Key

        /**
         * Guide key.
         *
         * On TV remotes, shows a programming guide.
         */
        public val Guide: Key

        /**
         * DVR key.
         *
         * On some TV remotes, switches to a DVR mode for recorded shows.
         */
        public val Dvr: Key

        /**
         * Bookmark key.
         *
         * On some TV remotes, bookmarks content or web pages.
         */
        public val Bookmark: Key

        /**
         * Toggle captions key.
         *
         * Switches the mode for closed-captioning text, for example during television shows.
         */
        public val Captions: Key

        /**
         * Settings key.
         *
         * Starts the system settings activity.
         */
        public val Settings: Key

        /**
         * TV power key.
         *
         * On TV remotes, toggles the power on a television screen.
         */
        public val TvPower: Key

        /**
         * TV input key.
         *
         * On TV remotes, switches the input on a television screen.
         */
        public val TvInput: Key

        /**
         * Set-top-box power key.
         *
         * On TV remotes, toggles the power on an external Set-top-box.
         */
        public val SetTopBoxPower: Key

        /**
         * Set-top-box input key.
         *
         * On TV remotes, switches the input mode on an external Set-top-box.
         */
        public val SetTopBoxInput: Key

        /**
         * A/V Receiver power key.
         *
         * On TV remotes, toggles the power on an external A/V Receiver.
         */
        public val AvReceiverPower: Key

        /**
         * A/V Receiver input key.
         *
         * On TV remotes, switches the input mode on an external A/V Receiver.
         */
        public val AvReceiverInput: Key

        /**
         * Red "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public val ProgramRed: Key

        /**
         * Green "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public val ProgramGreen: Key

        /**
         * Yellow "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public val ProgramYellow: Key

        /**
         * Blue "programmable" key.
         *
         * On TV remotes, acts as a contextual/programmable key.
         */
        public val ProgramBlue: Key

        /**
         * App switch key.
         *
         * Should bring up the application switcher dialog.
         */
        public val AppSwitch: Key

        /**
         * Language Switch key.
         *
         * Toggles the current input language such as switching between English and Japanese on a
         * QWERTY keyboard. On some devices, the same function may be performed by pressing
         * Shift+Space.
         */
        public val LanguageSwitch: Key

        /**
         * Manner Mode key.
         *
         * Toggles silent or vibrate mode on and off to make the device behave more politely in
         * certain settings such as on a crowded train. On some devices, the key may only operate
         * when long-pressed.
         */
        public val MannerMode: Key

        /**
         * 3D Mode key.
         *
         * Toggles the display between 2D and 3D mode.
         */
        public val Toggle2D3D: Key

        /**
         * Contacts special function key.
         *
         * Used to launch an address book application.
         */
        public val Contacts: Key

        /**
         * Calendar special function key.
         *
         * Used to launch a calendar application.
         */
        public val Calendar: Key

        /**
         * Music special function key.
         *
         * Used to launch a music player application.
         */
        public val Music: Key

        /**
         * Calculator special function key.
         *
         * Used to launch a calculator application.
         */
        public val Calculator: Key

        /** Japanese full-width / half-width key. */
        public val ZenkakuHankaru: Key

        /** Japanese alphanumeric key. */
        public val Eisu: Key

        /** Japanese non-conversion key. */
        public val Muhenkan: Key

        /** Japanese conversion key. */
        public val Henkan: Key

        /** Japanese katakana / hiragana key. */
        public val KatakanaHiragana: Key

        /** Japanese Yen key. */
        public val Yen: Key

        /** Japanese Ro key. */
        public val Ro: Key

        /** Japanese kana key. */
        public val Kana: Key

        /**
         * Assist key.
         *
         * Launches the global assist activity. Not delivered to applications.
         */
        public val Assist: Key

        /**
         * Brightness Down key.
         *
         * Adjusts the screen brightness down.
         */
        public val BrightnessDown: Key

        /**
         * Brightness Up key.
         *
         * Adjusts the screen brightness up.
         */
        public val BrightnessUp: Key

        /**
         * Sleep key.
         *
         * Puts the device to sleep. Behaves somewhat like [Power] but it has no effect if the
         * device is already asleep.
         */
        public val Sleep: Key

        /**
         * Wakeup key.
         *
         * Wakes up the device. Behaves somewhat like [Power] but it has no effect if the device is
         * already awake.
         */
        public val WakeUp: Key

        /** Put device to sleep unless a wakelock is held. */
        public val SoftSleep: Key

        /**
         * Pairing key.
         *
         * Initiates peripheral pairing mode. Useful for pairing remote control devices or game
         * controllers, especially if no other input mode is available.
         */
        public val Pairing: Key

        /**
         * Last Channel key.
         *
         * Goes to the last viewed channel.
         */
        public val LastChannel: Key

        /**
         * TV data service key.
         *
         * Displays data services like weather, sports.
         */
        public val TvDataService: Key

        /**
         * Voice Assist key.
         *
         * Launches the global voice assist activity. Not delivered to applications.
         */
        public val VoiceAssist: Key

        /**
         * Radio key.
         *
         * Toggles TV service / Radio service.
         */
        public val TvRadioService: Key

        /**
         * Teletext key.
         *
         * Displays Teletext service.
         */
        public val TvTeletext: Key

        /**
         * Number entry key.
         *
         * Initiates to enter multi-digit channel number when each digit key is assigned for
         * selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC User Control
         * Code.
         */
        public val TvNumberEntry: Key

        /**
         * Analog Terrestrial key.
         *
         * Switches to analog terrestrial broadcast service.
         */
        public val TvTerrestrialAnalog: Key

        /**
         * Digital Terrestrial key.
         *
         * Switches to digital terrestrial broadcast service.
         */
        public val TvTerrestrialDigital: Key

        /**
         * Satellite key.
         *
         * Switches to digital satellite broadcast service.
         */
        public val TvSatellite: Key

        /**
         * BS key.
         *
         * Switches to BS digital satellite broadcasting service available in Japan.
         */
        public val TvSatelliteBs: Key

        /**
         * CS key.
         *
         * Switches to CS digital satellite broadcasting service available in Japan.
         */
        public val TvSatelliteCs: Key

        /**
         * BS/CS key.
         *
         * Toggles between BS and CS digital satellite services.
         */
        public val TvSatelliteService: Key

        /**
         * Toggle Network key.
         *
         * Toggles selecting broadcast services.
         */
        public val TvNetwork: Key

        /**
         * Antenna/Cable key.
         *
         * Toggles broadcast input source between antenna and cable.
         */
        public val TvAntennaCable: Key

        /**
         * HDMI #1 key.
         *
         * Switches to HDMI input #1.
         */
        public val TvInputHdmi1: Key

        /**
         * HDMI #2 key.
         *
         * Switches to HDMI input #2.
         */
        public val TvInputHdmi2: Key

        /**
         * HDMI #3 key.
         *
         * Switches to HDMI input #3.
         */
        public val TvInputHdmi3: Key

        /**
         * HDMI #4 key.
         *
         * Switches to HDMI input #4.
         */
        public val TvInputHdmi4: Key

        /**
         * Composite #1 key.
         *
         * Switches to composite video input #1.
         */
        public val TvInputComposite1: Key

        /**
         * Composite #2 key.
         *
         * Switches to composite video input #2.
         */
        public val TvInputComposite2: Key

        /**
         * Component #1 key.
         *
         * Switches to component video input #1.
         */
        public val TvInputComponent1: Key

        /**
         * Component #2 key.
         *
         * Switches to component video input #2.
         */
        public val TvInputComponent2: Key

        /**
         * VGA #1 key.
         *
         * Switches to VGA (analog RGB) input #1.
         */
        public val TvInputVga1: Key

        /**
         * Audio description key.
         *
         * Toggles audio description off / on.
         */
        public val TvAudioDescription: Key

        /**
         * Audio description mixing volume up key.
         *
         * Increase the audio description volume as compared with normal audio volume.
         */
        public val TvAudioDescriptionMixingVolumeUp: Key

        /**
         * Audio description mixing volume down key.
         *
         * Lessen audio description volume as compared with normal audio volume.
         */
        public val TvAudioDescriptionMixingVolumeDown: Key

        /**
         * Zoom mode key.
         *
         * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.)
         */
        public val TvZoomMode: Key

        /**
         * Contents menu key.
         *
         * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control Code
         */
        public val TvContentsMenu: Key

        /**
         * Media context menu key.
         *
         * Goes to the context menu of media contents. Corresponds to Media Context-sensitive Menu
         * (0x11) of CEC User Control Code.
         */
        public val TvMediaContextMenu: Key

        /**
         * Timer programming key.
         *
         * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of CEC User
         * Control Code.
         */
        public val TvTimerProgramming: Key

        /**
         * Primary stem key for Wearables.
         *
         * Main power/reset button.
         */
        public val StemPrimary: Key

        /** Generic stem key 1 for Wearables. */
        public val Stem1: Key

        /** Generic stem key 2 for Wearables. */
        public val Stem2: Key

        /** Generic stem key 3 for Wearables. */
        public val Stem3: Key

        /** Show all apps. */
        public val AllApps: Key

        /** Refresh key. */
        public val Refresh: Key

        /** Thumbs up key. Apps can use this to let user up-vote content. */
        public val ThumbsUp: Key

        /** Thumbs down key. Apps can use this to let user down-vote content. */
        public val ThumbsDown: Key

        /**
         * Used to switch current [account][android.accounts.Account] that is consuming content. May
         * be consumed by system to set account globally.
         */
        public val ProfileSwitch: Key
    }

    public override fun toString(): String
}
