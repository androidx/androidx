/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.input.key.Key.Companion.Number

/**
 * Actual implementation of [Key] for JS and Native.
 *
 * @param keyCode an integer code representing the key pressed. Note: This keycode can be used to
 * uniquely identify a hardware key.
 */
actual value class Key(val keyCode: Long) {
    actual companion object {
        /** Unknown key. */
        actual val Unknown = Key(-1)

        /**
         * Home key.
         *
         * This key is handled by the framework and is never delivered to applications.
         */
        actual val Home = Key(115)

        /**
         * Up Arrow Key / Directional Pad Up key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionUp = Key(126)

        /**
         * Down Arrow Key / Directional Pad Down key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionDown = Key(125)

        /**
         * Left Arrow Key / Directional Pad Left key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionLeft = Key(123)

        /**
         * Right Arrow Key / Directional Pad Right key.
         *
         * May also be synthesized from trackball motions.
         */
        actual val DirectionRight = Key(124)

        /** '0' key. */
        actual val Zero = Key(29)

        /** '1' key. */
        actual val One = Key(18)

        /** '2' key. */
        actual val Two = Key(19)

        /** '3' key. */
        actual val Three = Key(20)

        /** '4' key. */
        actual val Four = Key(21)

        /** '5' key. */
        actual val Five = Key(23)

        /** '6' key. */
        actual val Six = Key(22)

        /** '7' key. */
        actual val Seven = Key(26)

        /** '8' key. */
        actual val Eight = Key(28)

        /** '9' key. */
        actual val Nine = Key(25)

        /** '-' key. */
        actual val Minus = Key(27)

        /** '=' key. */
        actual val Equals = Key(24)

        /** 'A' key. */
        actual val A = Key(0)

        /** 'B' key. */
        actual val B = Key(11)

        /** 'C' key. */
        actual val C = Key(8)

        /** 'D' key. */
        actual val D = Key(2)

        /** 'E' key. */
        actual val E = Key(14)

        /** 'F' key. */
        actual val F = Key(3)

        /** 'G' key. */
        actual val G = Key(5)

        /** 'H' key. */
        actual val H = Key(4)

        /** 'I' key. */
        actual val I = Key(34)

        /** 'J' key. */
        actual val J = Key(38)

        /** 'K' key. */
        actual val K = Key(40)

        /** 'L' key. */
        actual val L = Key(37)

        /** 'M' key. */
        actual val M = Key(46)

        /** 'N' key. */
        actual val N = Key(45)

        /** 'O' key. */
        actual val O = Key(31)

        /** 'P' key. */
        actual val P = Key(35)

        /** 'Q' key. */
        actual val Q = Key(12)

        /** 'R' key. */
        actual val R = Key(15)

        /** 'S' key. */
        actual val S = Key(1)

        /** 'T' key. */
        actual val T = Key(17)

        /** 'U' key. */
        actual val U = Key(32)

        /** 'V' key. */
        actual val V = Key(9)

        /** 'W' key. */
        actual val W = Key(13)

        /** 'X' key. */
        actual val X = Key(7)

        /** 'Y' key. */
        actual val Y = Key(16)

        /** 'Z' key. */
        actual val Z = Key(6)

        /** ',' key. */
        actual val Comma = Key(43)

        /** '.' key. */
        actual val Period = Key(47)

        /** Left Alt modifier key. */
        actual val AltLeft = Key(58)

        /** Right Alt modifier key. */
        actual val AltRight = Key(61)

        /** Left Shift modifier key. */
        actual val ShiftLeft = Key(56)

        /** Right Shift modifier key. */
        actual val ShiftRight = Key(60)

        /** Tab key. */
        actual val Tab = Key(48)

        /** Space key. */
        actual val Spacebar = Key(49)

        /** Enter key. */
        actual val Enter = Key(36)

        /**
         * Backspace key.
         *
         * Deletes characters before the insertion point, unlike [Delete].
         */
        actual val Backspace = Key(51)

        /**
         * Delete key.
         *
         * Deletes characters ahead of the insertion point, unlike [Backspace].
         */
        actual val Delete = Key(117)

        /** Escape key. */
        actual val Escape = Key(53)

        /** Left Control modifier key. */
        actual val CtrlLeft = Key(59)

        /** Right Control modifier key. */
        actual val CtrlRight = Key(62)

        /** Caps Lock key. */
        actual val CapsLock = Key(57)

        /** Scroll Lock key. */
        actual val ScrollLock = Key(107)

        /** Left Meta modifier key. */
        actual val MetaLeft = Key(55)

        /** Right Meta modifier key. */
        actual val MetaRight = Key(54)

        /** System Request / Print Screen key. */
        actual val PrintScreen = Key(105)

        /**
         * Insert key.
         *
         * Toggles insert / overwrite edit mode.
         */
        actual val Insert = Key(114)

        /** '`' (backtick) key. */
        actual val Grave = Key(50)

        /** '[' key. */
        actual val LeftBracket = Key(33)

        /** ']' key. */
        actual val RightBracket = Key(30)

        /** '/' key. */
        actual val Slash = Key(42)

        /** '\' key. */
        actual val Backslash = Key(44)

        /** ';' key. */
        actual val Semicolon = Key(41)

        /** Page Up key. */
        actual val PageUp = Key(116)

        /** Page Down key. */
        actual val PageDown = Key(121)

        /** F1 key. */
        actual val F1 = Key(122)

        /** F2 key. */
        actual val F2 = Key(120)

        /** F3 key. */
        actual val F3 = Key(99)

        /** F4 key. */
        actual val F4 = Key(118)

        /** F5 key. */
        actual val F5 = Key(96)

        /** F6 key. */
        actual val F6 = Key(97)

        /** F7 key. */
        actual val F7 = Key(98)

        /** F8 key. */
        actual val F8 = Key(100)

        /** F9 key. */
        actual val F9 = Key(101)

        /** F10 key. */
        actual val F10 = Key(109)

        /** F11 key. */
        actual val F11 = Key(103)

        /** F12 key. */
        actual val F12 = Key(111)

        /**
         * Num Lock key.
         *
         * This is the Num Lock key; it is different from [Number].
         * This key alters the behavior of other keys on the numeric keypad.
         */
        actual val NumLock = Key(71)

        /** Numeric keypad '0' key. */
        actual val NumPad0 = Key(82)

        /** Numeric keypad '1' key. */
        actual val NumPad1 = Key(83)

        /** Numeric keypad '2' key. */
        actual val NumPad2 = Key(84)

        /** Numeric keypad '3' key. */
        actual val NumPad3 = Key(85)

        /** Numeric keypad '4' key. */
        actual val NumPad4 = Key(86)

        /** Numeric keypad '5' key. */
        actual val NumPad5 = Key(87)

        /** Numeric keypad '6' key. */
        actual val NumPad6 = Key(88)

        /** Numeric keypad '7' key. */
        actual val NumPad7 = Key(89)

        /** Numeric keypad '8' key. */
        actual val NumPad8 = Key(91)

        /** Numeric keypad '9' key. */
        actual val NumPad9 = Key(92)

        /** Numeric keypad '/' key (for division). */
        actual val NumPadDivide = Key(75)

        /** Numeric keypad '*' key (for multiplication). */
        actual val NumPadMultiply = Key(67)

        /** Numeric keypad '-' key (for subtraction). */
        actual val NumPadSubtract = Key(78)

        /** Numeric keypad '+' key (for addition). */
        actual val NumPadAdd = Key(69)

        /** Numeric keypad Enter key. */
        actual val NumPadEnter = Key(76)

        actual val MoveHome = Key(115)

        actual val MoveEnd = Key(119)

        // Unsupported Keys
        actual val SoftLeft = Key(-1000000001)
        actual val SoftRight = Key(-1000000002)
        actual val Back = Key(-1000000003)
        actual val NavigatePrevious = Key(-1000000004)
        actual val NavigateNext = Key(-1000000005)
        actual val NavigateIn = Key(-1000000006)
        actual val NavigateOut = Key(-1000000007)
        actual val SystemNavigationUp = Key(-1000000008)
        actual val SystemNavigationDown = Key(-1000000009)
        actual val SystemNavigationLeft = Key(-1000000010)
        actual val SystemNavigationRight = Key(-1000000011)
        actual val Call = Key(-1000000012)
        actual val EndCall = Key(-1000000013)
        actual val DirectionCenter = Key(-1000000014)
        actual val DirectionUpLeft = Key(-1000000015)
        actual val DirectionDownLeft = Key(-1000000016)
        actual val DirectionUpRight = Key(-1000000017)
        actual val DirectionDownRight = Key(-1000000018)
        actual val VolumeUp = Key(-1000000019)
        actual val VolumeDown = Key(-1000000020)
        actual val Power = Key(-1000000021)
        actual val Camera = Key(-1000000022)
        actual val Clear = Key(-1000000023)
        actual val Symbol = Key(-1000000024)
        actual val Browser = Key(-1000000025)
        actual val Envelope = Key(-1000000026)
        actual val Function = Key(-1000000027)
        actual val Break = Key(-1000000028)
        actual val Number = Key(-1000000031)
        actual val HeadsetHook = Key(-1000000032)
        actual val Focus = Key(-1000000033)
        actual val Menu = Key(-1000000034)
        actual val Notification = Key(-1000000035)
        actual val Search = Key(-1000000036)
        actual val PictureSymbols = Key(-1000000037)
        actual val SwitchCharset = Key(-1000000038)
        actual val ButtonA = Key(-1000000039)
        actual val ButtonB = Key(-1000000040)
        actual val ButtonC = Key(-1000000041)
        actual val ButtonX = Key(-1000000042)
        actual val ButtonY = Key(-1000000043)
        actual val ButtonZ = Key(-1000000044)
        actual val ButtonL1 = Key(-1000000045)
        actual val ButtonR1 = Key(-1000000046)
        actual val ButtonL2 = Key(-1000000047)
        actual val ButtonR2 = Key(-1000000048)
        actual val ButtonThumbLeft = Key(-1000000049)
        actual val ButtonThumbRight = Key(-1000000050)
        actual val ButtonStart = Key(-1000000051)
        actual val ButtonSelect = Key(-1000000052)
        actual val ButtonMode = Key(-1000000053)
        actual val Button1 = Key(-1000000054)
        actual val Button2 = Key(-1000000055)
        actual val Button3 = Key(-1000000056)
        actual val Button4 = Key(-1000000057)
        actual val Button5 = Key(-1000000058)
        actual val Button6 = Key(-1000000059)
        actual val Button7 = Key(-1000000060)
        actual val Button8 = Key(-1000000061)
        actual val Button9 = Key(-1000000062)
        actual val Button10 = Key(-1000000063)
        actual val Button11 = Key(-1000000064)
        actual val Button12 = Key(-1000000065)
        actual val Button13 = Key(-1000000066)
        actual val Button14 = Key(-1000000067)
        actual val Button15 = Key(-1000000068)
        actual val Button16 = Key(-1000000069)
        actual val Forward = Key(-1000000070)
        actual val MediaPlay = Key(-1000000071)
        actual val MediaPause = Key(-1000000072)
        actual val MediaPlayPause = Key(-1000000073)
        actual val MediaStop = Key(-1000000074)
        actual val MediaRecord = Key(-1000000075)
        actual val MediaNext = Key(-1000000076)
        actual val MediaPrevious = Key(-1000000077)
        actual val MediaRewind = Key(-1000000078)
        actual val MediaFastForward = Key(-1000000079)
        actual val MediaClose = Key(-1000000080)
        actual val MediaAudioTrack = Key(-1000000081)
        actual val MediaEject = Key(-1000000082)
        actual val MediaTopMenu = Key(-1000000083)
        actual val MediaSkipForward = Key(-1000000084)
        actual val MediaSkipBackward = Key(-1000000085)
        actual val MediaStepForward = Key(-1000000086)
        actual val MediaStepBackward = Key(-1000000087)
        actual val MicrophoneMute = Key(-1000000088)
        actual val VolumeMute = Key(-1000000089)
        actual val Info = Key(-1000000090)
        actual val ChannelUp = Key(-1000000091)
        actual val ChannelDown = Key(-1000000092)
        actual val ZoomIn = Key(-1000000093)
        actual val ZoomOut = Key(-1000000094)
        actual val Tv = Key(-1000000095)
        actual val Window = Key(-1000000096)
        actual val Guide = Key(-1000000097)
        actual val Dvr = Key(-1000000098)
        actual val Bookmark = Key(-1000000099)
        actual val Captions = Key(-1000000100)
        actual val Settings = Key(-1000000101)
        actual val TvPower = Key(-1000000102)
        actual val TvInput = Key(-1000000103)
        actual val SetTopBoxPower = Key(-1000000104)
        actual val SetTopBoxInput = Key(-1000000105)
        actual val AvReceiverPower = Key(-1000000106)
        actual val AvReceiverInput = Key(-1000000107)
        actual val ProgramRed = Key(-1000000108)
        actual val ProgramGreen = Key(-1000000109)
        actual val ProgramYellow = Key(-1000000110)
        actual val ProgramBlue = Key(-1000000111)
        actual val AppSwitch = Key(-1000000112)
        actual val LanguageSwitch = Key(-1000000113)
        actual val MannerMode = Key(-1000000114)
        actual val Toggle2D3D = Key(-1000000125)
        actual val Contacts = Key(-1000000126)
        actual val Calendar = Key(-1000000127)
        actual val Music = Key(-1000000128)
        actual val Calculator = Key(-1000000129)
        actual val ZenkakuHankaru = Key(-1000000130)
        actual val Eisu = Key(-1000000131)
        actual val Muhenkan = Key(-1000000132)
        actual val Henkan = Key(-1000000133)
        actual val KatakanaHiragana = Key(-1000000134)
        actual val Yen = Key(-1000000135)
        actual val Ro = Key(-1000000136)
        actual val Kana = Key(-1000000137)
        actual val Assist = Key(-1000000138)
        actual val BrightnessDown = Key(-1000000139)
        actual val BrightnessUp = Key(-1000000140)
        actual val Sleep = Key(-1000000141)
        actual val WakeUp = Key(-1000000142)
        actual val SoftSleep = Key(-1000000143)
        actual val Pairing = Key(-1000000144)
        actual val LastChannel = Key(-1000000145)
        actual val TvDataService = Key(-1000000146)
        actual val VoiceAssist = Key(-1000000147)
        actual val TvRadioService = Key(-1000000148)
        actual val TvTeletext = Key(-1000000149)
        actual val TvNumberEntry = Key(-1000000150)
        actual val TvTerrestrialAnalog = Key(-1000000151)
        actual val TvTerrestrialDigital = Key(-1000000152)
        actual val TvSatellite = Key(-1000000153)
        actual val TvSatelliteBs = Key(-1000000154)
        actual val TvSatelliteCs = Key(-1000000155)
        actual val TvSatelliteService = Key(-1000000156)
        actual val TvNetwork = Key(-1000000157)
        actual val TvAntennaCable = Key(-1000000158)
        actual val TvInputHdmi1 = Key(-1000000159)
        actual val TvInputHdmi2 = Key(-1000000160)
        actual val TvInputHdmi3 = Key(-1000000161)
        actual val TvInputHdmi4 = Key(-1000000162)
        actual val TvInputComposite1 = Key(-1000000163)
        actual val TvInputComposite2 = Key(-1000000164)
        actual val TvInputComponent1 = Key(-1000000165)
        actual val TvInputComponent2 = Key(-1000000166)
        actual val TvInputVga1 = Key(-1000000167)
        actual val TvAudioDescription = Key(-1000000168)
        actual val TvAudioDescriptionMixingVolumeUp = Key(-1000000169)
        actual val TvAudioDescriptionMixingVolumeDown = Key(-1000000170)
        actual val TvZoomMode = Key(-1000000171)
        actual val TvContentsMenu = Key(-1000000172)
        actual val TvMediaContextMenu = Key(-1000000173)
        actual val TvTimerProgramming = Key(-1000000174)
        actual val StemPrimary = Key(-1000000175)
        actual val Stem1 = Key(-1000000176)
        actual val Stem2 = Key(-1000000177)
        actual val Stem3 = Key(-1000000178)
        actual val AllApps = Key(-1000000179)
        actual val Refresh = Key(-1000000180)
        actual val ThumbsUp = Key(-1000000181)
        actual val ThumbsDown = Key(-1000000182)
        actual val ProfileSwitch = Key(-1000000183)
        actual val Help = Key(-1000000184)
        actual val Plus = Key(-1000000185)
        actual val Multiply = Key(-1000000186)
        actual val Pound = Key(-1000000187)
        actual val Cut = Key(-1000000188)
        actual val Copy = Key(-1000000189)
        actual val Paste = Key(-1000000190)
        actual val Apostrophe = Key(-1000000191)
        actual val At = Key(-10000001902)
        actual val NumPadDot = Key(-1000000193)
        actual val NumPadComma = Key(-1000000194)
        actual val NumPadEquals = Key(-1000000195)
        actual val NumPadLeftParenthesis = Key(-1000000196)
        actual val NumPadRightParenthesis = Key(-1000000197)
    }

    actual override fun toString() = "Key keyCode: $keyCode"
}