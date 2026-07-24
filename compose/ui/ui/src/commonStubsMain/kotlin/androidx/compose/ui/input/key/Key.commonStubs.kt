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

import androidx.compose.ui.implementedInJetBrainsFork
import kotlin.jvm.JvmInline

@JvmInline
public actual value class Key(public val keyCode: Long) {
    public actual companion object {
        public actual val Unknown: Key
            get() = implementedInJetBrainsFork()

        @Deprecated(
            "`Key.Home` is never delivered to applications. For the keyboard \"Home\" key " +
                "use `Key.MoveHome`. For the system \"Home\" key (unlikely to be needed), use " +
                "`Key.SystemHome`",
            level = DeprecationLevel.ERROR,
        )
        public actual val Home: Key
            get() = implementedInJetBrainsFork()

        public actual val SystemHome: Key
            get() = implementedInJetBrainsFork()

        public actual val Help: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionUp: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionDown: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionRight: Key
            get() = implementedInJetBrainsFork()

        public actual val Zero: Key
            get() = implementedInJetBrainsFork()

        public actual val One: Key
            get() = implementedInJetBrainsFork()

        public actual val Two: Key
            get() = implementedInJetBrainsFork()

        public actual val Three: Key
            get() = implementedInJetBrainsFork()

        public actual val Four: Key
            get() = implementedInJetBrainsFork()

        public actual val Five: Key
            get() = implementedInJetBrainsFork()

        public actual val Six: Key
            get() = implementedInJetBrainsFork()

        public actual val Seven: Key
            get() = implementedInJetBrainsFork()

        public actual val Eight: Key
            get() = implementedInJetBrainsFork()

        public actual val Nine: Key
            get() = implementedInJetBrainsFork()

        public actual val Plus: Key
            get() = implementedInJetBrainsFork()

        public actual val Minus: Key
            get() = implementedInJetBrainsFork()

        public actual val Multiply: Key
            get() = implementedInJetBrainsFork()

        public actual val Equals: Key
            get() = implementedInJetBrainsFork()

        public actual val Pound: Key
            get() = implementedInJetBrainsFork()

        public actual val A: Key
            get() = implementedInJetBrainsFork()

        public actual val B: Key
            get() = implementedInJetBrainsFork()

        public actual val C: Key
            get() = implementedInJetBrainsFork()

        public actual val D: Key
            get() = implementedInJetBrainsFork()

        public actual val E: Key
            get() = implementedInJetBrainsFork()

        public actual val F: Key
            get() = implementedInJetBrainsFork()

        public actual val G: Key
            get() = implementedInJetBrainsFork()

        public actual val H: Key
            get() = implementedInJetBrainsFork()

        public actual val I: Key
            get() = implementedInJetBrainsFork()

        public actual val J: Key
            get() = implementedInJetBrainsFork()

        public actual val K: Key
            get() = implementedInJetBrainsFork()

        public actual val L: Key
            get() = implementedInJetBrainsFork()

        public actual val M: Key
            get() = implementedInJetBrainsFork()

        public actual val N: Key
            get() = implementedInJetBrainsFork()

        public actual val O: Key
            get() = implementedInJetBrainsFork()

        public actual val P: Key
            get() = implementedInJetBrainsFork()

        public actual val Q: Key
            get() = implementedInJetBrainsFork()

        public actual val R: Key
            get() = implementedInJetBrainsFork()

        public actual val S: Key
            get() = implementedInJetBrainsFork()

        public actual val T: Key
            get() = implementedInJetBrainsFork()

        public actual val U: Key
            get() = implementedInJetBrainsFork()

        public actual val V: Key
            get() = implementedInJetBrainsFork()

        public actual val W: Key
            get() = implementedInJetBrainsFork()

        public actual val X: Key
            get() = implementedInJetBrainsFork()

        public actual val Y: Key
            get() = implementedInJetBrainsFork()

        public actual val Z: Key
            get() = implementedInJetBrainsFork()

        public actual val Comma: Key
            get() = implementedInJetBrainsFork()

        public actual val Period: Key
            get() = implementedInJetBrainsFork()

        public actual val AltLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val AltRight: Key
            get() = implementedInJetBrainsFork()

        public actual val ShiftLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val ShiftRight: Key
            get() = implementedInJetBrainsFork()

        public actual val Tab: Key
            get() = implementedInJetBrainsFork()

        public actual val Spacebar: Key
            get() = implementedInJetBrainsFork()

        public actual val Enter: Key
            get() = implementedInJetBrainsFork()

        public actual val Backspace: Key
            get() = implementedInJetBrainsFork()

        public actual val Delete: Key
            get() = implementedInJetBrainsFork()

        public actual val Escape: Key
            get() = implementedInJetBrainsFork()

        public actual val CtrlLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val CtrlRight: Key
            get() = implementedInJetBrainsFork()

        public actual val CapsLock: Key
            get() = implementedInJetBrainsFork()

        public actual val ScrollLock: Key
            get() = implementedInJetBrainsFork()

        public actual val MetaLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val MetaRight: Key
            get() = implementedInJetBrainsFork()

        public actual val PrintScreen: Key
            get() = implementedInJetBrainsFork()

        public actual val Insert: Key
            get() = implementedInJetBrainsFork()

        public actual val Cut: Key
            get() = implementedInJetBrainsFork()

        public actual val Copy: Key
            get() = implementedInJetBrainsFork()

        public actual val Paste: Key
            get() = implementedInJetBrainsFork()

        public actual val Grave: Key
            get() = implementedInJetBrainsFork()

        public actual val LeftBracket: Key
            get() = implementedInJetBrainsFork()

        public actual val RightBracket: Key
            get() = implementedInJetBrainsFork()

        public actual val Slash: Key
            get() = implementedInJetBrainsFork()

        public actual val Backslash: Key
            get() = implementedInJetBrainsFork()

        public actual val Semicolon: Key
            get() = implementedInJetBrainsFork()

        public actual val Apostrophe: Key
            get() = implementedInJetBrainsFork()

        public actual val At: Key
            get() = implementedInJetBrainsFork()

        public actual val PageUp: Key
            get() = implementedInJetBrainsFork()

        public actual val PageDown: Key
            get() = implementedInJetBrainsFork()

        public actual val F1: Key
            get() = implementedInJetBrainsFork()

        public actual val F2: Key
            get() = implementedInJetBrainsFork()

        public actual val F3: Key
            get() = implementedInJetBrainsFork()

        public actual val F4: Key
            get() = implementedInJetBrainsFork()

        public actual val F5: Key
            get() = implementedInJetBrainsFork()

        public actual val F6: Key
            get() = implementedInJetBrainsFork()

        public actual val F7: Key
            get() = implementedInJetBrainsFork()

        public actual val F8: Key
            get() = implementedInJetBrainsFork()

        public actual val F9: Key
            get() = implementedInJetBrainsFork()

        public actual val F10: Key
            get() = implementedInJetBrainsFork()

        public actual val F11: Key
            get() = implementedInJetBrainsFork()

        public actual val F12: Key
            get() = implementedInJetBrainsFork()

        public actual val NumLock: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad0: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad1: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad2: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad3: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad4: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad5: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad6: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad7: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad8: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPad9: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadDivide: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadMultiply: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadSubtract: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadAdd: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadDot: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadComma: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadEnter: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadEquals: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadLeftParenthesis: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadRightParenthesis: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadDirectionUp: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadDirectionDown: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadDirectionLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadDirectionRight: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadMoveHome: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadMoveEnd: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadPageUp: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadPageDown: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadInsert: Key
            get() = implementedInJetBrainsFork()

        public actual val NumPadDelete: Key
            get() = implementedInJetBrainsFork()

        public actual val MoveHome: Key
            get() = implementedInJetBrainsFork()

        public actual val MoveEnd: Key
            get() = implementedInJetBrainsFork()

        public actual val SoftLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val SoftRight: Key
            get() = implementedInJetBrainsFork()

        public actual val Back: Key
            get() = implementedInJetBrainsFork()

        public actual val NavigatePrevious: Key
            get() = implementedInJetBrainsFork()

        public actual val NavigateNext: Key
            get() = implementedInJetBrainsFork()

        public actual val NavigateIn: Key
            get() = implementedInJetBrainsFork()

        public actual val NavigateOut: Key
            get() = implementedInJetBrainsFork()

        public actual val SystemNavigationUp: Key
            get() = implementedInJetBrainsFork()

        public actual val SystemNavigationDown: Key
            get() = implementedInJetBrainsFork()

        public actual val SystemNavigationLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val SystemNavigationRight: Key
            get() = implementedInJetBrainsFork()

        public actual val Call: Key
            get() = implementedInJetBrainsFork()

        public actual val EndCall: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionCenter: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionUpLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionDownLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionUpRight: Key
            get() = implementedInJetBrainsFork()

        public actual val DirectionDownRight: Key
            get() = implementedInJetBrainsFork()

        public actual val VolumeUp: Key
            get() = implementedInJetBrainsFork()

        public actual val VolumeDown: Key
            get() = implementedInJetBrainsFork()

        public actual val Power: Key
            get() = implementedInJetBrainsFork()

        public actual val Camera: Key
            get() = implementedInJetBrainsFork()

        public actual val Clear: Key
            get() = implementedInJetBrainsFork()

        public actual val Symbol: Key
            get() = implementedInJetBrainsFork()

        public actual val Browser: Key
            get() = implementedInJetBrainsFork()

        public actual val Envelope: Key
            get() = implementedInJetBrainsFork()

        public actual val Function: Key
            get() = implementedInJetBrainsFork()

        public actual val Break: Key
            get() = implementedInJetBrainsFork()

        public actual val Number: Key
            get() = implementedInJetBrainsFork()

        public actual val HeadsetHook: Key
            get() = implementedInJetBrainsFork()

        public actual val Focus: Key
            get() = implementedInJetBrainsFork()

        public actual val Menu: Key
            get() = implementedInJetBrainsFork()

        public actual val Notification: Key
            get() = implementedInJetBrainsFork()

        public actual val Search: Key
            get() = implementedInJetBrainsFork()

        public actual val PictureSymbols: Key
            get() = implementedInJetBrainsFork()

        public actual val SwitchCharset: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonA: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonB: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonC: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonX: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonY: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonZ: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonL1: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonR1: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonL2: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonR2: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonThumbLeft: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonThumbRight: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonStart: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonSelect: Key
            get() = implementedInJetBrainsFork()

        public actual val ButtonMode: Key
            get() = implementedInJetBrainsFork()

        public actual val Button1: Key
            get() = implementedInJetBrainsFork()

        public actual val Button2: Key
            get() = implementedInJetBrainsFork()

        public actual val Button3: Key
            get() = implementedInJetBrainsFork()

        public actual val Button4: Key
            get() = implementedInJetBrainsFork()

        public actual val Button5: Key
            get() = implementedInJetBrainsFork()

        public actual val Button6: Key
            get() = implementedInJetBrainsFork()

        public actual val Button7: Key
            get() = implementedInJetBrainsFork()

        public actual val Button8: Key
            get() = implementedInJetBrainsFork()

        public actual val Button9: Key
            get() = implementedInJetBrainsFork()

        public actual val Button10: Key
            get() = implementedInJetBrainsFork()

        public actual val Button11: Key
            get() = implementedInJetBrainsFork()

        public actual val Button12: Key
            get() = implementedInJetBrainsFork()

        public actual val Button13: Key
            get() = implementedInJetBrainsFork()

        public actual val Button14: Key
            get() = implementedInJetBrainsFork()

        public actual val Button15: Key
            get() = implementedInJetBrainsFork()

        public actual val Button16: Key
            get() = implementedInJetBrainsFork()

        public actual val Forward: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaPlay: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaPause: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaPlayPause: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaStop: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaRecord: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaNext: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaPrevious: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaRewind: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaFastForward: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaClose: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaAudioTrack: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaEject: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaTopMenu: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaSkipForward: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaSkipBackward: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaStepForward: Key
            get() = implementedInJetBrainsFork()

        public actual val MediaStepBackward: Key
            get() = implementedInJetBrainsFork()

        public actual val MicrophoneMute: Key
            get() = implementedInJetBrainsFork()

        public actual val VolumeMute: Key
            get() = implementedInJetBrainsFork()

        public actual val Info: Key
            get() = implementedInJetBrainsFork()

        public actual val ChannelUp: Key
            get() = implementedInJetBrainsFork()

        public actual val ChannelDown: Key
            get() = implementedInJetBrainsFork()

        public actual val ZoomIn: Key
            get() = implementedInJetBrainsFork()

        public actual val ZoomOut: Key
            get() = implementedInJetBrainsFork()

        public actual val Tv: Key
            get() = implementedInJetBrainsFork()

        public actual val Window: Key
            get() = implementedInJetBrainsFork()

        public actual val Guide: Key
            get() = implementedInJetBrainsFork()

        public actual val Dvr: Key
            get() = implementedInJetBrainsFork()

        public actual val Bookmark: Key
            get() = implementedInJetBrainsFork()

        public actual val Captions: Key
            get() = implementedInJetBrainsFork()

        public actual val Settings: Key
            get() = implementedInJetBrainsFork()

        public actual val TvPower: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInput: Key
            get() = implementedInJetBrainsFork()

        public actual val SetTopBoxPower: Key
            get() = implementedInJetBrainsFork()

        public actual val SetTopBoxInput: Key
            get() = implementedInJetBrainsFork()

        public actual val AvReceiverPower: Key
            get() = implementedInJetBrainsFork()

        public actual val AvReceiverInput: Key
            get() = implementedInJetBrainsFork()

        public actual val ProgramRed: Key
            get() = implementedInJetBrainsFork()

        public actual val ProgramGreen: Key
            get() = implementedInJetBrainsFork()

        public actual val ProgramYellow: Key
            get() = implementedInJetBrainsFork()

        public actual val ProgramBlue: Key
            get() = implementedInJetBrainsFork()

        public actual val AppSwitch: Key
            get() = implementedInJetBrainsFork()

        public actual val LanguageSwitch: Key
            get() = implementedInJetBrainsFork()

        public actual val MannerMode: Key
            get() = implementedInJetBrainsFork()

        public actual val Toggle2D3D: Key
            get() = implementedInJetBrainsFork()

        public actual val Contacts: Key
            get() = implementedInJetBrainsFork()

        public actual val Calendar: Key
            get() = implementedInJetBrainsFork()

        public actual val Music: Key
            get() = implementedInJetBrainsFork()

        public actual val Calculator: Key
            get() = implementedInJetBrainsFork()

        public actual val ZenkakuHankaru: Key
            get() = implementedInJetBrainsFork()

        public actual val Eisu: Key
            get() = implementedInJetBrainsFork()

        public actual val Muhenkan: Key
            get() = implementedInJetBrainsFork()

        public actual val Henkan: Key
            get() = implementedInJetBrainsFork()

        public actual val KatakanaHiragana: Key
            get() = implementedInJetBrainsFork()

        public actual val Yen: Key
            get() = implementedInJetBrainsFork()

        public actual val Ro: Key
            get() = implementedInJetBrainsFork()

        public actual val Kana: Key
            get() = implementedInJetBrainsFork()

        public actual val Assist: Key
            get() = implementedInJetBrainsFork()

        public actual val BrightnessDown: Key
            get() = implementedInJetBrainsFork()

        public actual val BrightnessUp: Key
            get() = implementedInJetBrainsFork()

        public actual val Sleep: Key
            get() = implementedInJetBrainsFork()

        public actual val WakeUp: Key
            get() = implementedInJetBrainsFork()

        public actual val SoftSleep: Key
            get() = implementedInJetBrainsFork()

        public actual val Pairing: Key
            get() = implementedInJetBrainsFork()

        public actual val LastChannel: Key
            get() = implementedInJetBrainsFork()

        public actual val TvDataService: Key
            get() = implementedInJetBrainsFork()

        public actual val VoiceAssist: Key
            get() = implementedInJetBrainsFork()

        public actual val TvRadioService: Key
            get() = implementedInJetBrainsFork()

        public actual val TvTeletext: Key
            get() = implementedInJetBrainsFork()

        public actual val TvNumberEntry: Key
            get() = implementedInJetBrainsFork()

        public actual val TvTerrestrialAnalog: Key
            get() = implementedInJetBrainsFork()

        public actual val TvTerrestrialDigital: Key
            get() = implementedInJetBrainsFork()

        public actual val TvSatellite: Key
            get() = implementedInJetBrainsFork()

        public actual val TvSatelliteBs: Key
            get() = implementedInJetBrainsFork()

        public actual val TvSatelliteCs: Key
            get() = implementedInJetBrainsFork()

        public actual val TvSatelliteService: Key
            get() = implementedInJetBrainsFork()

        public actual val TvNetwork: Key
            get() = implementedInJetBrainsFork()

        public actual val TvAntennaCable: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputHdmi1: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputHdmi2: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputHdmi3: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputHdmi4: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputComposite1: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputComposite2: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputComponent1: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputComponent2: Key
            get() = implementedInJetBrainsFork()

        public actual val TvInputVga1: Key
            get() = implementedInJetBrainsFork()

        public actual val TvAudioDescription: Key
            get() = implementedInJetBrainsFork()

        public actual val TvAudioDescriptionMixingVolumeUp: Key
            get() = implementedInJetBrainsFork()

        public actual val TvAudioDescriptionMixingVolumeDown: Key
            get() = implementedInJetBrainsFork()

        public actual val TvZoomMode: Key
            get() = implementedInJetBrainsFork()

        public actual val TvContentsMenu: Key
            get() = implementedInJetBrainsFork()

        public actual val TvMediaContextMenu: Key
            get() = implementedInJetBrainsFork()

        public actual val TvTimerProgramming: Key
            get() = implementedInJetBrainsFork()

        public actual val StemPrimary: Key
            get() = implementedInJetBrainsFork()

        public actual val Stem1: Key
            get() = implementedInJetBrainsFork()

        public actual val Stem2: Key
            get() = implementedInJetBrainsFork()

        public actual val Stem3: Key
            get() = implementedInJetBrainsFork()

        public actual val AllApps: Key
            get() = implementedInJetBrainsFork()

        public actual val Refresh: Key
            get() = implementedInJetBrainsFork()

        public actual val ThumbsUp: Key
            get() = implementedInJetBrainsFork()

        public actual val ThumbsDown: Key
            get() = implementedInJetBrainsFork()

        public actual val ProfileSwitch: Key
            get() = implementedInJetBrainsFork()
    }
}
