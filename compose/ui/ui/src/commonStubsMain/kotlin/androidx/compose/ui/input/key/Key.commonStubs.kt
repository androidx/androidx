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
actual value class Key(val keyCode: Long) {
    actual companion object {
        actual val Unknown: Key
            get() = implementedInJetBrainsFork()

        @Deprecated(
            "`Key.Home` is never delivered to applications. For the keyboard \"Home\" key " +
                "use `Key.MoveHome`. For the system \"Home\" key (unlikely to be needed), use " +
                "`Key.SystemHome`",
            level = DeprecationLevel.ERROR,
        )
        actual val Home: Key
            get() = implementedInJetBrainsFork()

        actual val SystemHome: Key
            get() = implementedInJetBrainsFork()

        actual val Help: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionUp: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionDown: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionLeft: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionRight: Key
            get() = implementedInJetBrainsFork()

        actual val Zero: Key
            get() = implementedInJetBrainsFork()

        actual val One: Key
            get() = implementedInJetBrainsFork()

        actual val Two: Key
            get() = implementedInJetBrainsFork()

        actual val Three: Key
            get() = implementedInJetBrainsFork()

        actual val Four: Key
            get() = implementedInJetBrainsFork()

        actual val Five: Key
            get() = implementedInJetBrainsFork()

        actual val Six: Key
            get() = implementedInJetBrainsFork()

        actual val Seven: Key
            get() = implementedInJetBrainsFork()

        actual val Eight: Key
            get() = implementedInJetBrainsFork()

        actual val Nine: Key
            get() = implementedInJetBrainsFork()

        actual val Plus: Key
            get() = implementedInJetBrainsFork()

        actual val Minus: Key
            get() = implementedInJetBrainsFork()

        actual val Multiply: Key
            get() = implementedInJetBrainsFork()

        actual val Equals: Key
            get() = implementedInJetBrainsFork()

        actual val Pound: Key
            get() = implementedInJetBrainsFork()

        actual val A: Key
            get() = implementedInJetBrainsFork()

        actual val B: Key
            get() = implementedInJetBrainsFork()

        actual val C: Key
            get() = implementedInJetBrainsFork()

        actual val D: Key
            get() = implementedInJetBrainsFork()

        actual val E: Key
            get() = implementedInJetBrainsFork()

        actual val F: Key
            get() = implementedInJetBrainsFork()

        actual val G: Key
            get() = implementedInJetBrainsFork()

        actual val H: Key
            get() = implementedInJetBrainsFork()

        actual val I: Key
            get() = implementedInJetBrainsFork()

        actual val J: Key
            get() = implementedInJetBrainsFork()

        actual val K: Key
            get() = implementedInJetBrainsFork()

        actual val L: Key
            get() = implementedInJetBrainsFork()

        actual val M: Key
            get() = implementedInJetBrainsFork()

        actual val N: Key
            get() = implementedInJetBrainsFork()

        actual val O: Key
            get() = implementedInJetBrainsFork()

        actual val P: Key
            get() = implementedInJetBrainsFork()

        actual val Q: Key
            get() = implementedInJetBrainsFork()

        actual val R: Key
            get() = implementedInJetBrainsFork()

        actual val S: Key
            get() = implementedInJetBrainsFork()

        actual val T: Key
            get() = implementedInJetBrainsFork()

        actual val U: Key
            get() = implementedInJetBrainsFork()

        actual val V: Key
            get() = implementedInJetBrainsFork()

        actual val W: Key
            get() = implementedInJetBrainsFork()

        actual val X: Key
            get() = implementedInJetBrainsFork()

        actual val Y: Key
            get() = implementedInJetBrainsFork()

        actual val Z: Key
            get() = implementedInJetBrainsFork()

        actual val Comma: Key
            get() = implementedInJetBrainsFork()

        actual val Period: Key
            get() = implementedInJetBrainsFork()

        actual val AltLeft: Key
            get() = implementedInJetBrainsFork()

        actual val AltRight: Key
            get() = implementedInJetBrainsFork()

        actual val ShiftLeft: Key
            get() = implementedInJetBrainsFork()

        actual val ShiftRight: Key
            get() = implementedInJetBrainsFork()

        actual val Tab: Key
            get() = implementedInJetBrainsFork()

        actual val Spacebar: Key
            get() = implementedInJetBrainsFork()

        actual val Enter: Key
            get() = implementedInJetBrainsFork()

        actual val Backspace: Key
            get() = implementedInJetBrainsFork()

        actual val Delete: Key
            get() = implementedInJetBrainsFork()

        actual val Escape: Key
            get() = implementedInJetBrainsFork()

        actual val CtrlLeft: Key
            get() = implementedInJetBrainsFork()

        actual val CtrlRight: Key
            get() = implementedInJetBrainsFork()

        actual val CapsLock: Key
            get() = implementedInJetBrainsFork()

        actual val ScrollLock: Key
            get() = implementedInJetBrainsFork()

        actual val MetaLeft: Key
            get() = implementedInJetBrainsFork()

        actual val MetaRight: Key
            get() = implementedInJetBrainsFork()

        actual val PrintScreen: Key
            get() = implementedInJetBrainsFork()

        actual val Insert: Key
            get() = implementedInJetBrainsFork()

        actual val Cut: Key
            get() = implementedInJetBrainsFork()

        actual val Copy: Key
            get() = implementedInJetBrainsFork()

        actual val Paste: Key
            get() = implementedInJetBrainsFork()

        actual val Grave: Key
            get() = implementedInJetBrainsFork()

        actual val LeftBracket: Key
            get() = implementedInJetBrainsFork()

        actual val RightBracket: Key
            get() = implementedInJetBrainsFork()

        actual val Slash: Key
            get() = implementedInJetBrainsFork()

        actual val Backslash: Key
            get() = implementedInJetBrainsFork()

        actual val Semicolon: Key
            get() = implementedInJetBrainsFork()

        actual val Apostrophe: Key
            get() = implementedInJetBrainsFork()

        actual val At: Key
            get() = implementedInJetBrainsFork()

        actual val PageUp: Key
            get() = implementedInJetBrainsFork()

        actual val PageDown: Key
            get() = implementedInJetBrainsFork()

        actual val F1: Key
            get() = implementedInJetBrainsFork()

        actual val F2: Key
            get() = implementedInJetBrainsFork()

        actual val F3: Key
            get() = implementedInJetBrainsFork()

        actual val F4: Key
            get() = implementedInJetBrainsFork()

        actual val F5: Key
            get() = implementedInJetBrainsFork()

        actual val F6: Key
            get() = implementedInJetBrainsFork()

        actual val F7: Key
            get() = implementedInJetBrainsFork()

        actual val F8: Key
            get() = implementedInJetBrainsFork()

        actual val F9: Key
            get() = implementedInJetBrainsFork()

        actual val F10: Key
            get() = implementedInJetBrainsFork()

        actual val F11: Key
            get() = implementedInJetBrainsFork()

        actual val F12: Key
            get() = implementedInJetBrainsFork()

        actual val NumLock: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad0: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad1: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad2: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad3: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad4: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad5: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad6: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad7: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad8: Key
            get() = implementedInJetBrainsFork()

        actual val NumPad9: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadDivide: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadMultiply: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadSubtract: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadAdd: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadDot: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadComma: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadEnter: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadEquals: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadLeftParenthesis: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadRightParenthesis: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadDirectionUp: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadDirectionDown: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadDirectionLeft: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadDirectionRight: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadMoveHome: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadMoveEnd: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadPageUp: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadPageDown: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadInsert: Key
            get() = implementedInJetBrainsFork()

        actual val NumPadDelete: Key
            get() = implementedInJetBrainsFork()

        actual val MoveHome: Key
            get() = implementedInJetBrainsFork()

        actual val MoveEnd: Key
            get() = implementedInJetBrainsFork()

        actual val SoftLeft: Key
            get() = implementedInJetBrainsFork()

        actual val SoftRight: Key
            get() = implementedInJetBrainsFork()

        actual val Back: Key
            get() = implementedInJetBrainsFork()

        actual val NavigatePrevious: Key
            get() = implementedInJetBrainsFork()

        actual val NavigateNext: Key
            get() = implementedInJetBrainsFork()

        actual val NavigateIn: Key
            get() = implementedInJetBrainsFork()

        actual val NavigateOut: Key
            get() = implementedInJetBrainsFork()

        actual val SystemNavigationUp: Key
            get() = implementedInJetBrainsFork()

        actual val SystemNavigationDown: Key
            get() = implementedInJetBrainsFork()

        actual val SystemNavigationLeft: Key
            get() = implementedInJetBrainsFork()

        actual val SystemNavigationRight: Key
            get() = implementedInJetBrainsFork()

        actual val Call: Key
            get() = implementedInJetBrainsFork()

        actual val EndCall: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionCenter: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionUpLeft: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionDownLeft: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionUpRight: Key
            get() = implementedInJetBrainsFork()

        actual val DirectionDownRight: Key
            get() = implementedInJetBrainsFork()

        actual val VolumeUp: Key
            get() = implementedInJetBrainsFork()

        actual val VolumeDown: Key
            get() = implementedInJetBrainsFork()

        actual val Power: Key
            get() = implementedInJetBrainsFork()

        actual val Camera: Key
            get() = implementedInJetBrainsFork()

        actual val Clear: Key
            get() = implementedInJetBrainsFork()

        actual val Symbol: Key
            get() = implementedInJetBrainsFork()

        actual val Browser: Key
            get() = implementedInJetBrainsFork()

        actual val Envelope: Key
            get() = implementedInJetBrainsFork()

        actual val Function: Key
            get() = implementedInJetBrainsFork()

        actual val Break: Key
            get() = implementedInJetBrainsFork()

        actual val Number: Key
            get() = implementedInJetBrainsFork()

        actual val HeadsetHook: Key
            get() = implementedInJetBrainsFork()

        actual val Focus: Key
            get() = implementedInJetBrainsFork()

        actual val Menu: Key
            get() = implementedInJetBrainsFork()

        actual val Notification: Key
            get() = implementedInJetBrainsFork()

        actual val Search: Key
            get() = implementedInJetBrainsFork()

        actual val PictureSymbols: Key
            get() = implementedInJetBrainsFork()

        actual val SwitchCharset: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonA: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonB: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonC: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonX: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonY: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonZ: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonL1: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonR1: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonL2: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonR2: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonThumbLeft: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonThumbRight: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonStart: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonSelect: Key
            get() = implementedInJetBrainsFork()

        actual val ButtonMode: Key
            get() = implementedInJetBrainsFork()

        actual val Button1: Key
            get() = implementedInJetBrainsFork()

        actual val Button2: Key
            get() = implementedInJetBrainsFork()

        actual val Button3: Key
            get() = implementedInJetBrainsFork()

        actual val Button4: Key
            get() = implementedInJetBrainsFork()

        actual val Button5: Key
            get() = implementedInJetBrainsFork()

        actual val Button6: Key
            get() = implementedInJetBrainsFork()

        actual val Button7: Key
            get() = implementedInJetBrainsFork()

        actual val Button8: Key
            get() = implementedInJetBrainsFork()

        actual val Button9: Key
            get() = implementedInJetBrainsFork()

        actual val Button10: Key
            get() = implementedInJetBrainsFork()

        actual val Button11: Key
            get() = implementedInJetBrainsFork()

        actual val Button12: Key
            get() = implementedInJetBrainsFork()

        actual val Button13: Key
            get() = implementedInJetBrainsFork()

        actual val Button14: Key
            get() = implementedInJetBrainsFork()

        actual val Button15: Key
            get() = implementedInJetBrainsFork()

        actual val Button16: Key
            get() = implementedInJetBrainsFork()

        actual val Forward: Key
            get() = implementedInJetBrainsFork()

        actual val MediaPlay: Key
            get() = implementedInJetBrainsFork()

        actual val MediaPause: Key
            get() = implementedInJetBrainsFork()

        actual val MediaPlayPause: Key
            get() = implementedInJetBrainsFork()

        actual val MediaStop: Key
            get() = implementedInJetBrainsFork()

        actual val MediaRecord: Key
            get() = implementedInJetBrainsFork()

        actual val MediaNext: Key
            get() = implementedInJetBrainsFork()

        actual val MediaPrevious: Key
            get() = implementedInJetBrainsFork()

        actual val MediaRewind: Key
            get() = implementedInJetBrainsFork()

        actual val MediaFastForward: Key
            get() = implementedInJetBrainsFork()

        actual val MediaClose: Key
            get() = implementedInJetBrainsFork()

        actual val MediaAudioTrack: Key
            get() = implementedInJetBrainsFork()

        actual val MediaEject: Key
            get() = implementedInJetBrainsFork()

        actual val MediaTopMenu: Key
            get() = implementedInJetBrainsFork()

        actual val MediaSkipForward: Key
            get() = implementedInJetBrainsFork()

        actual val MediaSkipBackward: Key
            get() = implementedInJetBrainsFork()

        actual val MediaStepForward: Key
            get() = implementedInJetBrainsFork()

        actual val MediaStepBackward: Key
            get() = implementedInJetBrainsFork()

        actual val MicrophoneMute: Key
            get() = implementedInJetBrainsFork()

        actual val VolumeMute: Key
            get() = implementedInJetBrainsFork()

        actual val Info: Key
            get() = implementedInJetBrainsFork()

        actual val ChannelUp: Key
            get() = implementedInJetBrainsFork()

        actual val ChannelDown: Key
            get() = implementedInJetBrainsFork()

        actual val ZoomIn: Key
            get() = implementedInJetBrainsFork()

        actual val ZoomOut: Key
            get() = implementedInJetBrainsFork()

        actual val Tv: Key
            get() = implementedInJetBrainsFork()

        actual val Window: Key
            get() = implementedInJetBrainsFork()

        actual val Guide: Key
            get() = implementedInJetBrainsFork()

        actual val Dvr: Key
            get() = implementedInJetBrainsFork()

        actual val Bookmark: Key
            get() = implementedInJetBrainsFork()

        actual val Captions: Key
            get() = implementedInJetBrainsFork()

        actual val Settings: Key
            get() = implementedInJetBrainsFork()

        actual val TvPower: Key
            get() = implementedInJetBrainsFork()

        actual val TvInput: Key
            get() = implementedInJetBrainsFork()

        actual val SetTopBoxPower: Key
            get() = implementedInJetBrainsFork()

        actual val SetTopBoxInput: Key
            get() = implementedInJetBrainsFork()

        actual val AvReceiverPower: Key
            get() = implementedInJetBrainsFork()

        actual val AvReceiverInput: Key
            get() = implementedInJetBrainsFork()

        actual val ProgramRed: Key
            get() = implementedInJetBrainsFork()

        actual val ProgramGreen: Key
            get() = implementedInJetBrainsFork()

        actual val ProgramYellow: Key
            get() = implementedInJetBrainsFork()

        actual val ProgramBlue: Key
            get() = implementedInJetBrainsFork()

        actual val AppSwitch: Key
            get() = implementedInJetBrainsFork()

        actual val LanguageSwitch: Key
            get() = implementedInJetBrainsFork()

        actual val MannerMode: Key
            get() = implementedInJetBrainsFork()

        actual val Toggle2D3D: Key
            get() = implementedInJetBrainsFork()

        actual val Contacts: Key
            get() = implementedInJetBrainsFork()

        actual val Calendar: Key
            get() = implementedInJetBrainsFork()

        actual val Music: Key
            get() = implementedInJetBrainsFork()

        actual val Calculator: Key
            get() = implementedInJetBrainsFork()

        actual val ZenkakuHankaru: Key
            get() = implementedInJetBrainsFork()

        actual val Eisu: Key
            get() = implementedInJetBrainsFork()

        actual val Muhenkan: Key
            get() = implementedInJetBrainsFork()

        actual val Henkan: Key
            get() = implementedInJetBrainsFork()

        actual val KatakanaHiragana: Key
            get() = implementedInJetBrainsFork()

        actual val Yen: Key
            get() = implementedInJetBrainsFork()

        actual val Ro: Key
            get() = implementedInJetBrainsFork()

        actual val Kana: Key
            get() = implementedInJetBrainsFork()

        actual val Assist: Key
            get() = implementedInJetBrainsFork()

        actual val BrightnessDown: Key
            get() = implementedInJetBrainsFork()

        actual val BrightnessUp: Key
            get() = implementedInJetBrainsFork()

        actual val Sleep: Key
            get() = implementedInJetBrainsFork()

        actual val WakeUp: Key
            get() = implementedInJetBrainsFork()

        actual val SoftSleep: Key
            get() = implementedInJetBrainsFork()

        actual val Pairing: Key
            get() = implementedInJetBrainsFork()

        actual val LastChannel: Key
            get() = implementedInJetBrainsFork()

        actual val TvDataService: Key
            get() = implementedInJetBrainsFork()

        actual val VoiceAssist: Key
            get() = implementedInJetBrainsFork()

        actual val TvRadioService: Key
            get() = implementedInJetBrainsFork()

        actual val TvTeletext: Key
            get() = implementedInJetBrainsFork()

        actual val TvNumberEntry: Key
            get() = implementedInJetBrainsFork()

        actual val TvTerrestrialAnalog: Key
            get() = implementedInJetBrainsFork()

        actual val TvTerrestrialDigital: Key
            get() = implementedInJetBrainsFork()

        actual val TvSatellite: Key
            get() = implementedInJetBrainsFork()

        actual val TvSatelliteBs: Key
            get() = implementedInJetBrainsFork()

        actual val TvSatelliteCs: Key
            get() = implementedInJetBrainsFork()

        actual val TvSatelliteService: Key
            get() = implementedInJetBrainsFork()

        actual val TvNetwork: Key
            get() = implementedInJetBrainsFork()

        actual val TvAntennaCable: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputHdmi1: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputHdmi2: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputHdmi3: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputHdmi4: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputComposite1: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputComposite2: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputComponent1: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputComponent2: Key
            get() = implementedInJetBrainsFork()

        actual val TvInputVga1: Key
            get() = implementedInJetBrainsFork()

        actual val TvAudioDescription: Key
            get() = implementedInJetBrainsFork()

        actual val TvAudioDescriptionMixingVolumeUp: Key
            get() = implementedInJetBrainsFork()

        actual val TvAudioDescriptionMixingVolumeDown: Key
            get() = implementedInJetBrainsFork()

        actual val TvZoomMode: Key
            get() = implementedInJetBrainsFork()

        actual val TvContentsMenu: Key
            get() = implementedInJetBrainsFork()

        actual val TvMediaContextMenu: Key
            get() = implementedInJetBrainsFork()

        actual val TvTimerProgramming: Key
            get() = implementedInJetBrainsFork()

        actual val StemPrimary: Key
            get() = implementedInJetBrainsFork()

        actual val Stem1: Key
            get() = implementedInJetBrainsFork()

        actual val Stem2: Key
            get() = implementedInJetBrainsFork()

        actual val Stem3: Key
            get() = implementedInJetBrainsFork()

        actual val AllApps: Key
            get() = implementedInJetBrainsFork()

        actual val Refresh: Key
            get() = implementedInJetBrainsFork()

        actual val ThumbsUp: Key
            get() = implementedInJetBrainsFork()

        actual val ThumbsDown: Key
            get() = implementedInJetBrainsFork()

        actual val ProfileSwitch: Key
            get() = implementedInJetBrainsFork()
    }
}
