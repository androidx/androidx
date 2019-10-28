/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.widget;

import android.graphics.Color;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// Note: This is forked from android.media.Cea708CCParser since P
/**
 * A class for parsing CEA-708, which is the standard for closed captioning for ATSC DTV.
 *
 * <p>ATSC DTV closed caption data are carried on picture user data of video streams.
 * This class starts to parse from picture user data payload, so extraction process of user_data
 * from video streams is up to outside of this code.
 *
 * <p>There are 4 steps to decode user_data to provide closed caption services. Step 1 and 2 are
 * done in NuPlayer and libstagefright.
 *
 * <h3>Step 1. user_data -&gt; CcPacket</h3>
 *
 * <p>First, user_data consists of cc_data packets, which are 3-byte segments. Here, CcPacket is a
 * collection of cc_data packets in a frame along with same presentation timestamp. Because cc_data
 * packets must be reassembled in the frame display order, CcPackets are reordered.
 *
 * <h3>Step 2. CcPacket -&gt; DTVCC packet</h3>
 *
 * <p>Each cc_data packet has a one byte for declaring a type of itself and data validity, and the
 * subsequent two bytes for input data of a DTVCC packet. There are 4 types for cc_data packet.
 * We're interested in DTVCC_PACKET_START(type 3) and DTVCC_PACKET_DATA(type 2). Each DTVCC packet
 * begins with DTVCC_PACKET_START(type 3) and the following cc_data packets which has
 * DTVCC_PACKET_DATA(type 2) are appended into the DTVCC packet being assembled.
 *
 * <h3>Step 3. DTVCC packet -&gt; Service Blocks</h3>
 *
 * <p>A DTVCC packet consists of multiple service blocks. Each service block represents a caption
 * track and has a service number, which ranges from 1 to 63, that denotes caption track identity.
 * In here, we listen at most one chosen caption track by service number. Otherwise, just skip the
 * other service blocks.
 *
 * <h3>Step 4. Interpreting Service Block Data ({@link #parseServiceBlockData}, {@code parseXX},
 * and {@link #parseExt1} methods)</h3>
 *
 * <p>Service block data is actual caption stream. it looks similar to telnet. It uses most parts of
 * ASCII table and consists of specially defined commands and some ASCII control codes which work
 * in a behavior slightly different from their original purpose. ASCII control codes and caption
 * commands are explicit instructions that control the state of a closed caption service and the
 * other ASCII and text codes are implicit instructions that send their characters to buffer.
 *
 * <p>There are 4 main code groups and 4 extended code groups. Both the range of code groups are the
 * same as the range of a byte.
 *
 * <p>4 main code groups: C0, C1, G0, G1
 * <br>4 extended code groups: C2, C3, G2, G3
 *
 * <p>Each code group has its own handle method. For example, {@link #parseC0} handles C0 code group
 * and so on. And {@link #parseServiceBlockData} method maps a stream on the main code groups while
 * {@link #parseExt1} method maps on the extended code groups.
 *
 * <p>The main code groups:
 * <ul>
 * <li>C0 - contains modified ASCII control codes. It is not intended by CEA-708 but Korea TTA
 *      standard for ATSC CC uses P16 character heavily, which is unclear entity in CEA-708 doc,
 *      even for the alphanumeric characters instead of ASCII characters.</li>
 * <li>C1 - contains the caption commands. There are 3 categories of a caption command.</li>
 * <ul>
 * <li>Window commands: The window commands control a caption window which is addressable area being
 *                  with in the Safe title area. (CWX, CLW, DSW, HDW, TGW, DLW, SWA, DFX)</li>
 * <li>Pen commands: Th pen commands control text style and location. (SPA, SPC, SPL)</li>
 * <li>Job commands: The job commands make a delay and recover from the delay. (DLY, DLC, RST)</li>
 * </ul>
 * <li>G0 - same as printable ASCII character set except music note character.</li>
 * <li>G1 - same as ISO 8859-1 Latin 1 character set.</li>
 * </ul>
 * <p>Most of the extended code groups are being skipped.
 *
 */
class Cea708CCParser {
    private static final String TAG = "Cea708CCParser";
    private static final boolean DEBUG = false;

    private static final String MUSIC_NOTE_CHAR = new String(
            "\u266B".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

    private final StringBuilder mBuilder = new StringBuilder();
    private int mCommand = 0;

    // Assign a dummy listener in order to avoid null checks.
    private DisplayListener mListener = new DisplayListener() {
        @Override
        public void emitEvent(CaptionEvent event) {
            // do nothing
        }
    };

    /**
     * {@link Cea708Parser} emits caption event of three different types.
     * {@link DisplayListener#emitEvent} is invoked with the parameter
     * {@link CaptionEvent} to pass all the results to an observer of the decoding process .
     *
     * <p>{@link CaptionEvent#type} determines the type of the result and
     * {@link CaptionEvent#obj} contains the output value of a caption event.
     * The observer must do the casting to the corresponding type.
     *
     * <ul><li>{@code CAPTION_EMIT_TYPE_BUFFER}: Passes a caption text buffer to a observer.
     * {@code obj} must be of {@link String}.</li>
     *
     * <li>{@code CAPTION_EMIT_TYPE_CONTROL}: Passes a caption character control code to a observer.
     * {@code obj} must be of {@link Character}.</li>
     *
     * <li>{@code CAPTION_EMIT_TYPE_CLEAR_COMMAND}: Passes a clear command to a observer.
     * {@code obj} must be {@code NULL}.</li></ul>
     */
    public static final int CAPTION_EMIT_TYPE_BUFFER = 1;
    public static final int CAPTION_EMIT_TYPE_CONTROL = 2;
    public static final int CAPTION_EMIT_TYPE_COMMAND_CWX = 3;
    public static final int CAPTION_EMIT_TYPE_COMMAND_CLW = 4;
    public static final int CAPTION_EMIT_TYPE_COMMAND_DSW = 5;
    public static final int CAPTION_EMIT_TYPE_COMMAND_HDW = 6;
    public static final int CAPTION_EMIT_TYPE_COMMAND_TGW = 7;
    public static final int CAPTION_EMIT_TYPE_COMMAND_DLW = 8;
    public static final int CAPTION_EMIT_TYPE_COMMAND_DLY = 9;
    public static final int CAPTION_EMIT_TYPE_COMMAND_DLC = 10;
    public static final int CAPTION_EMIT_TYPE_COMMAND_RST = 11;
    public static final int CAPTION_EMIT_TYPE_COMMAND_SPA = 12;
    public static final int CAPTION_EMIT_TYPE_COMMAND_SPC = 13;
    public static final int CAPTION_EMIT_TYPE_COMMAND_SPL = 14;
    public static final int CAPTION_EMIT_TYPE_COMMAND_SWA = 15;
    public static final int CAPTION_EMIT_TYPE_COMMAND_DFX = 16;

    Cea708CCParser(DisplayListener listener) {
        if (listener != null) {
            mListener = listener;
        }
    }

    interface DisplayListener {
        void emitEvent(CaptionEvent event);
    }

    private void emitCaptionEvent(CaptionEvent captionEvent) {
        // Emit the existing string buffer before a new event is arrived.
        emitCaptionBuffer();
        mListener.emitEvent(captionEvent);
    }

    private void emitCaptionBuffer() {
        if (mBuilder.length() > 0) {
            mListener.emitEvent(new CaptionEvent(CAPTION_EMIT_TYPE_BUFFER, mBuilder.toString()));
            mBuilder.setLength(0);
        }
    }

    // Step 3. DTVCC packet -> Service Blocks (parseDtvCcPacket method)
    public void parse(byte[] data) {
        // From this point, starts to read DTVCC coding layer.
        // First, identify code groups, which is defined in CEA-708B Section 7.1.
        int pos = 0;
        while (pos < data.length) {
            pos = parseServiceBlockData(data, pos);
        }

        // Emit the buffer after reading codes.
        emitCaptionBuffer();
    }

    // Step 4. Main code groups
    private int parseServiceBlockData(byte[] data, int pos) {
        // For the details of the ranges of DTVCC code groups, see CEA-708B Table 6.
        mCommand = data[pos] & 0xff;
        ++pos;
        if (mCommand == Const.CODE_C0_EXT1) {
            if (DEBUG) {
                Log.d(TAG, String.format("parseServiceBlockData EXT1 %x", mCommand));
            }
            pos = parseExt1(data, pos);
        } else if (mCommand >= Const.CODE_C0_RANGE_START
                && mCommand <= Const.CODE_C0_RANGE_END) {
            if (DEBUG) {
                Log.d(TAG, String.format("parseServiceBlockData C0 %x", mCommand));
            }
            pos = parseC0(data, pos);
        } else if (mCommand >= Const.CODE_C1_RANGE_START
                && mCommand <= Const.CODE_C1_RANGE_END) {
            if (DEBUG) {
                Log.d(TAG, String.format("parseServiceBlockData C1 %x", mCommand));
            }
            pos = parseC1(data, pos);
        } else if (mCommand >= Const.CODE_G0_RANGE_START
                && mCommand <= Const.CODE_G0_RANGE_END) {
            if (DEBUG) {
                Log.d(TAG, String.format("parseServiceBlockData G0 %x", mCommand));
            }
            pos = parseG0(data, pos);
        } else if (mCommand >= Const.CODE_G1_RANGE_START
                && mCommand <= Const.CODE_G1_RANGE_END) {
            if (DEBUG) {
                Log.d(TAG, String.format("parseServiceBlockData G1 %x", mCommand));
            }
            pos = parseG1(data, pos);
        }
        return pos;
    }

    private int parseC0(byte[] data, int pos) {
        // For the details of C0 code group, see CEA-708B Section 7.4.1.
        // CL Group: C0 Subset of ASCII Control codes
        if (mCommand >= Const.CODE_C0_SKIP2_RANGE_START
                && mCommand <= Const.CODE_C0_SKIP2_RANGE_END) {
            if (mCommand == Const.CODE_C0_P16) {
                // P16 escapes next two bytes for the large character maps.(no standard rule)
                // For Korea broadcasting, express whole letters by using this.
                try {
                    if (data[pos] == 0) {
                        mBuilder.append((char) data[pos + 1]);
                    } else {
                        String value = new String(Arrays.copyOfRange(data, pos, pos + 2), "EUC-KR");
                        mBuilder.append(value);
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "P16 Code - Could not find supported encoding", e);
                }
            }
            pos += 2;
        } else if (mCommand >= Const.CODE_C0_SKIP1_RANGE_START
                && mCommand <= Const.CODE_C0_SKIP1_RANGE_END) {
            ++pos;
        } else {
            // NUL, BS, FF, CR interpreted as they are in ASCII control codes.
            // HCR moves the pen location to th beginning of the current line and deletes contents.
            // FF clears the screen and moves the pen location to (0,0).
            // ETX is the NULL command which is used to flush text to the current window when no
            // other command is pending.
            switch (mCommand) {
                case Const.CODE_C0_NUL:
                    break;
                case Const.CODE_C0_ETX:
                    emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_CONTROL, (char) mCommand));
                    break;
                case Const.CODE_C0_BS:
                    emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_CONTROL, (char) mCommand));
                    break;
                case Const.CODE_C0_FF:
                    emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_CONTROL, (char) mCommand));
                    break;
                case Const.CODE_C0_CR:
                    mBuilder.append('\n');
                    break;
                case Const.CODE_C0_HCR:
                    emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_CONTROL, (char) mCommand));
                    break;
                default:
                    break;
            }
        }
        return pos;
    }

    private int parseC1(byte[] data, int pos) {
        // For the details of C1 code group, see CEA-708B Section 8.10.
        // CR Group: C1 Caption Control Codes
        switch (mCommand) {
            case Const.CODE_C1_CW0:
            case Const.CODE_C1_CW1:
            case Const.CODE_C1_CW2:
            case Const.CODE_C1_CW3:
            case Const.CODE_C1_CW4:
            case Const.CODE_C1_CW5:
            case Const.CODE_C1_CW6:
            case Const.CODE_C1_CW7: {
                // SetCurrentWindow0-7
                int windowId = mCommand - Const.CODE_C1_CW0;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_CWX, windowId));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand CWX windowId: %d", windowId));
                }
                break;
            }

            case Const.CODE_C1_CLW: {
                // ClearWindows
                int windowBitmap = data[pos] & 0xff;
                ++pos;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_CLW, windowBitmap));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand CLW windowBitmap: %d", windowBitmap));
                }
                break;
            }

            case Const.CODE_C1_DSW: {
                // DisplayWindows
                int windowBitmap = data[pos] & 0xff;
                ++pos;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_DSW, windowBitmap));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand DSW windowBitmap: %d", windowBitmap));
                }
                break;
            }

            case Const.CODE_C1_HDW: {
                // HideWindows
                int windowBitmap = data[pos] & 0xff;
                ++pos;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_HDW, windowBitmap));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand HDW windowBitmap: %d", windowBitmap));
                }
                break;
            }

            case Const.CODE_C1_TGW: {
                // ToggleWindows
                int windowBitmap = data[pos] & 0xff;
                ++pos;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_TGW, windowBitmap));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand TGW windowBitmap: %d", windowBitmap));
                }
                break;
            }

            case Const.CODE_C1_DLW: {
                // DeleteWindows
                int windowBitmap = data[pos] & 0xff;
                ++pos;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_DLW, windowBitmap));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand DLW windowBitmap: %d", windowBitmap));
                }
                break;
            }

            case Const.CODE_C1_DLY: {
                // Delay
                int tenthsOfSeconds = data[pos] & 0xff;
                ++pos;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_DLY, tenthsOfSeconds));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand DLY %d tenths of seconds",
                            tenthsOfSeconds));
                }
                break;
            }
            case Const.CODE_C1_DLC: {
                // DelayCancel
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_DLC, null));
                if (DEBUG) {
                    Log.d(TAG, "CaptionCommand DLC");
                }
                break;
            }

            case Const.CODE_C1_RST: {
                // Reset
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_RST, null));
                if (DEBUG) {
                    Log.d(TAG, "CaptionCommand RST");
                }
                break;
            }

            case Const.CODE_C1_SPA: {
                // SetPenAttributes
                int textTag = (data[pos] & 0xf0) >> 4;
                int penSize = data[pos] & 0x03;
                int penOffset = (data[pos] & 0x0c) >> 2;
                boolean italic = (data[pos + 1] & 0x80) != 0;
                boolean underline = (data[pos + 1] & 0x40) != 0;
                int edgeType = (data[pos + 1] & 0x38) >> 3;
                int fontTag = data[pos + 1] & 0x7;
                pos += 2;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_SPA,
                        new CaptionPenAttr(penSize, penOffset, textTag, fontTag, edgeType,
                                underline, italic)));
                if (DEBUG) {
                    Log.d(TAG, String.format(
                            "CaptionCommand SPA penSize: %d, penOffset: %d, textTag: %d, "
                                    + "fontTag: %d, edgeType: %d, underline: %s, italic: %s",
                            penSize, penOffset, textTag, fontTag, edgeType, underline, italic));
                }
                break;
            }

            case Const.CODE_C1_SPC: {
                // SetPenColor
                int opacity = (data[pos] & 0xc0) >> 6;
                int red = (data[pos] & 0x30) >> 4;
                int green = (data[pos] & 0x0c) >> 2;
                int blue = data[pos] & 0x03;
                CaptionColor foregroundColor = new CaptionColor(opacity, red, green, blue);
                ++pos;
                opacity = (data[pos] & 0xc0) >> 6;
                red = (data[pos] & 0x30) >> 4;
                green = (data[pos] & 0x0c) >> 2;
                blue = data[pos] & 0x03;
                CaptionColor backgroundColor = new CaptionColor(opacity, red, green, blue);
                ++pos;
                red = (data[pos] & 0x30) >> 4;
                green = (data[pos] & 0x0c) >> 2;
                blue = data[pos] & 0x03;
                CaptionColor edgeColor = new CaptionColor(
                        CaptionColor.OPACITY_SOLID, red, green, blue);
                ++pos;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_SPC,
                        new CaptionPenColor(foregroundColor, backgroundColor, edgeColor)));
                if (DEBUG) {
                    Log.d(TAG, String.format(
                            "CaptionCommand SPC foregroundColor %s backgroundColor %s edgeColor %s",
                            foregroundColor, backgroundColor, edgeColor));
                }
                break;
            }

            case Const.CODE_C1_SPL: {
                // SetPenLocation
                // column is normally 0-31 for 4:3 formats, and 0-41 for 16:9 formats
                int row = data[pos] & 0x0f;
                int column = data[pos + 1] & 0x3f;
                pos += 2;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_SPL,
                        new CaptionPenLocation(row, column)));
                if (DEBUG) {
                    Log.d(TAG, String.format("CaptionCommand SPL row: %d, column: %d",
                            row, column));
                }
                break;
            }

            case Const.CODE_C1_SWA: {
                // SetWindowAttributes
                int opacity = (data[pos] & 0xc0) >> 6;
                int red = (data[pos] & 0x30) >> 4;
                int green = (data[pos] & 0x0c) >> 2;
                int blue = data[pos] & 0x03;
                CaptionColor fillColor = new CaptionColor(opacity, red, green, blue);
                int borderType = (data[pos + 1] & 0xc0) >> 6 | (data[pos + 2] & 0x80) >> 5;
                red = (data[pos + 1] & 0x30) >> 4;
                green = (data[pos + 1] & 0x0c) >> 2;
                blue = data[pos + 1] & 0x03;
                CaptionColor borderColor = new CaptionColor(
                        CaptionColor.OPACITY_SOLID, red, green, blue);
                boolean wordWrap = (data[pos + 2] & 0x40) != 0;
                int printDirection = (data[pos + 2] & 0x30) >> 4;
                int scrollDirection = (data[pos + 2] & 0x0c) >> 2;
                int justify = (data[pos + 2] & 0x03);
                int effectSpeed = (data[pos + 3] & 0xf0) >> 4;
                int effectDirection = (data[pos + 3] & 0x0c) >> 2;
                int displayEffect = data[pos + 3] & 0x3;
                pos += 4;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_SWA,
                        new CaptionWindowAttr(fillColor, borderColor, borderType, wordWrap,
                                printDirection, scrollDirection, justify,
                                effectDirection, effectSpeed, displayEffect)));
                if (DEBUG) {
                    Log.d(TAG, String.format(
                            "CaptionCommand SWA fillColor: %s, borderColor: %s, borderType: %d"
                                    + "wordWrap: %s, printDirection: %d, scrollDirection: %d, "
                                    + "justify: %s, effectDirection: %d, effectSpeed: %d, "
                                    + "displayEffect: %d",
                            fillColor, borderColor, borderType, wordWrap, printDirection,
                            scrollDirection, justify, effectDirection, effectSpeed, displayEffect));
                }
                break;
            }

            case Const.CODE_C1_DF0:
            case Const.CODE_C1_DF1:
            case Const.CODE_C1_DF2:
            case Const.CODE_C1_DF3:
            case Const.CODE_C1_DF4:
            case Const.CODE_C1_DF5:
            case Const.CODE_C1_DF6:
            case Const.CODE_C1_DF7: {
                // DefineWindow0-7
                int windowId = mCommand - Const.CODE_C1_DF0;
                boolean visible = (data[pos] & 0x20) != 0;
                boolean rowLock = (data[pos] & 0x10) != 0;
                boolean columnLock = (data[pos] & 0x08) != 0;
                int priority = data[pos] & 0x07;
                boolean relativePositioning = (data[pos + 1] & 0x80) != 0;
                int anchorVertical = data[pos + 1] & 0x7f;
                int anchorHorizontal = data[pos + 2] & 0xff;
                int anchorId = (data[pos + 3] & 0xf0) >> 4;
                int rowCount = data[pos + 3] & 0x0f;
                int columnCount = data[pos + 4] & 0x3f;
                int windowStyle = (data[pos + 5] & 0x38) >> 3;
                int penStyle = data[pos + 5] & 0x07;
                pos += 6;
                emitCaptionEvent(new CaptionEvent(CAPTION_EMIT_TYPE_COMMAND_DFX,
                        new CaptionWindow(windowId, visible, rowLock, columnLock, priority,
                                relativePositioning, anchorVertical, anchorHorizontal, anchorId,
                                rowCount, columnCount, penStyle, windowStyle)));
                if (DEBUG) {
                    Log.d(TAG, String.format(
                            "CaptionCommand DFx windowId: %d, priority: %d, columnLock: %s, "
                                    + "rowLock: %s, visible: %s, anchorVertical: %d, "
                                    + "relativePositioning: %s, anchorHorizontal: %d, "
                                    + "rowCount: %d, anchorId: %d, columnCount: %d, penStyle: %d, "
                                    + "windowStyle: %d",
                            windowId, priority, columnLock, rowLock, visible, anchorVertical,
                            relativePositioning, anchorHorizontal, rowCount, anchorId, columnCount,
                            penStyle, windowStyle));
                }
                break;
            }

            default:
                break;
        }
        return pos;
    }

    private int parseG0(byte[] data, int pos) {
        // For the details of G0 code group, see CEA-708B Section 7.4.3.
        // GL Group: G0 Modified version of ANSI X3.4 Printable Character Set (ASCII)
        if (mCommand == Const.CODE_G0_MUSICNOTE) {
            // Music note.
            mBuilder.append(MUSIC_NOTE_CHAR);
        } else {
            // Put ASCII code into buffer.
            mBuilder.append((char) mCommand);
        }
        return pos;
    }

    private int parseG1(byte[] data, int pos) {
        // For the details of G0 code group, see CEA-708B Section 7.4.4.
        // GR Group: G1 ISO 8859-1 Latin 1 Characters
        // Put ASCII Extended character set into buffer.
        mBuilder.append((char) mCommand);
        return pos;
    }

    // Step 4. Extended code groups
    private int parseExt1(byte[] data, int pos) {
        // For the details of EXT1 code group, see CEA-708B Section 7.2.
        mCommand = data[pos] & 0xff;
        ++pos;
        if (mCommand >= Const.CODE_C2_RANGE_START
                && mCommand <= Const.CODE_C2_RANGE_END) {
            pos = parseC2(data, pos);
        } else if (mCommand >= Const.CODE_C3_RANGE_START
                && mCommand <= Const.CODE_C3_RANGE_END) {
            pos = parseC3(data, pos);
        } else if (mCommand >= Const.CODE_G2_RANGE_START
                && mCommand <= Const.CODE_G2_RANGE_END) {
            pos = parseG2(data, pos);
        } else if (mCommand >= Const.CODE_G3_RANGE_START
                && mCommand <= Const.CODE_G3_RANGE_END) {
            pos = parseG3(data, pos);
        }
        return pos;
    }

    private int parseC2(byte[] data, int pos) {
        // For the details of C2 code group, see CEA-708B Section 7.4.7.
        // Extended Miscellaneous Control Codes
        // C2 Table : No commands as of CEA-708B. A decoder must skip.
        if (mCommand >= Const.CODE_C2_SKIP0_RANGE_START
                && mCommand <= Const.CODE_C2_SKIP0_RANGE_END) {
            // Do nothing.
        } else if (mCommand >= Const.CODE_C2_SKIP1_RANGE_START
                && mCommand <= Const.CODE_C2_SKIP1_RANGE_END) {
            ++pos;
        } else if (mCommand >= Const.CODE_C2_SKIP2_RANGE_START
                && mCommand <= Const.CODE_C2_SKIP2_RANGE_END) {
            pos += 2;
        } else if (mCommand >= Const.CODE_C2_SKIP3_RANGE_START
                && mCommand <= Const.CODE_C2_SKIP3_RANGE_END) {
            pos += 3;
        }
        return pos;
    }

    private int parseC3(byte[] data, int pos) {
        // For the details of C3 code group, see CEA-708B Section 7.4.8.
        // Extended Control Code Set 2
        // C3 Table : No commands as of CEA-708B. A decoder must skip.
        if (mCommand >= Const.CODE_C3_SKIP4_RANGE_START
                && mCommand <= Const.CODE_C3_SKIP4_RANGE_END) {
            pos += 4;
        } else if (mCommand >= Const.CODE_C3_SKIP5_RANGE_START
                && mCommand <= Const.CODE_C3_SKIP5_RANGE_END) {
            pos += 5;
        }
        return pos;
    }

    private int parseG2(byte[] data, int pos) {
        // For the details of C3 code group, see CEA-708B Section 7.4.5.
        // Extended Control Code Set 1(G2 Table)
        switch (mCommand) {
            case Const.CODE_G2_TSP:
                // TODO : TSP is the Transparent space
                break;
            case Const.CODE_G2_NBTSP:
                // TODO : NBTSP is Non-Breaking Transparent Space.
                break;
            case Const.CODE_G2_BLK:
                // TODO : BLK indicates a solid block which fills the entire character block
                // TODO : with a solid foreground color.
                break;
            default:
                break;
        }
        return pos;
    }

    private int parseG3(byte[] data, int pos) {
        // For the details of C3 code group, see CEA-708B Section 7.4.6.
        // Future characters and icons(G3 Table)
        if (mCommand == Const.CODE_G3_CC) {
            // TODO : [CC] icon with square corners
        }

        // Do nothing
        return pos;
    }

    /**
     * Collection of CEA-708 structures.
     */
    private static class Const {

        private Const() {
        }

        // For the details of the ranges of DTVCC code groups, see CEA-708B Table 6.
        public static final int CODE_C0_RANGE_START = 0x00;
        public static final int CODE_C0_RANGE_END = 0x1f;
        public static final int CODE_C1_RANGE_START = 0x80;
        public static final int CODE_C1_RANGE_END = 0x9f;
        public static final int CODE_G0_RANGE_START = 0x20;
        public static final int CODE_G0_RANGE_END = 0x7f;
        public static final int CODE_G1_RANGE_START = 0xa0;
        public static final int CODE_G1_RANGE_END = 0xff;
        public static final int CODE_C2_RANGE_START = 0x00;
        public static final int CODE_C2_RANGE_END = 0x1f;
        public static final int CODE_C3_RANGE_START = 0x80;
        public static final int CODE_C3_RANGE_END = 0x9f;
        public static final int CODE_G2_RANGE_START = 0x20;
        public static final int CODE_G2_RANGE_END = 0x7f;
        public static final int CODE_G3_RANGE_START = 0xa0;
        public static final int CODE_G3_RANGE_END = 0xff;

        // The following ranges are defined in CEA-708B Section 7.4.1.
        public static final int CODE_C0_SKIP2_RANGE_START = 0x18;
        public static final int CODE_C0_SKIP2_RANGE_END = 0x1f;
        public static final int CODE_C0_SKIP1_RANGE_START = 0x10;
        public static final int CODE_C0_SKIP1_RANGE_END = 0x17;

        // The following ranges are defined in CEA-708B Section 7.4.7.
        public static final int CODE_C2_SKIP0_RANGE_START = 0x00;
        public static final int CODE_C2_SKIP0_RANGE_END = 0x07;
        public static final int CODE_C2_SKIP1_RANGE_START = 0x08;
        public static final int CODE_C2_SKIP1_RANGE_END = 0x0f;
        public static final int CODE_C2_SKIP2_RANGE_START = 0x10;
        public static final int CODE_C2_SKIP2_RANGE_END = 0x17;
        public static final int CODE_C2_SKIP3_RANGE_START = 0x18;
        public static final int CODE_C2_SKIP3_RANGE_END = 0x1f;

        // The following ranges are defined in CEA-708B Section 7.4.8.
        public static final int CODE_C3_SKIP4_RANGE_START = 0x80;
        public static final int CODE_C3_SKIP4_RANGE_END = 0x87;
        public static final int CODE_C3_SKIP5_RANGE_START = 0x88;
        public static final int CODE_C3_SKIP5_RANGE_END = 0x8f;

        // The following values are the special characters of CEA-708 spec.
        public static final int CODE_C0_NUL = 0x00;
        public static final int CODE_C0_ETX = 0x03;
        public static final int CODE_C0_BS = 0x08;
        public static final int CODE_C0_FF = 0x0c;
        public static final int CODE_C0_CR = 0x0d;
        public static final int CODE_C0_HCR = 0x0e;
        public static final int CODE_C0_EXT1 = 0x10;
        public static final int CODE_C0_P16 = 0x18;
        public static final int CODE_G0_MUSICNOTE = 0x7f;
        public static final int CODE_G2_TSP = 0x20;
        public static final int CODE_G2_NBTSP = 0x21;
        public static final int CODE_G2_BLK = 0x30;
        public static final int CODE_G3_CC = 0xa0;

        // The following values are the command bits of CEA-708 spec.
        public static final int CODE_C1_CW0 = 0x80;
        public static final int CODE_C1_CW1 = 0x81;
        public static final int CODE_C1_CW2 = 0x82;
        public static final int CODE_C1_CW3 = 0x83;
        public static final int CODE_C1_CW4 = 0x84;
        public static final int CODE_C1_CW5 = 0x85;
        public static final int CODE_C1_CW6 = 0x86;
        public static final int CODE_C1_CW7 = 0x87;
        public static final int CODE_C1_CLW = 0x88;
        public static final int CODE_C1_DSW = 0x89;
        public static final int CODE_C1_HDW = 0x8a;
        public static final int CODE_C1_TGW = 0x8b;
        public static final int CODE_C1_DLW = 0x8c;
        public static final int CODE_C1_DLY = 0x8d;
        public static final int CODE_C1_DLC = 0x8e;
        public static final int CODE_C1_RST = 0x8f;
        public static final int CODE_C1_SPA = 0x90;
        public static final int CODE_C1_SPC = 0x91;
        public static final int CODE_C1_SPL = 0x92;
        public static final int CODE_C1_SWA = 0x97;
        public static final int CODE_C1_DF0 = 0x98;
        public static final int CODE_C1_DF1 = 0x99;
        public static final int CODE_C1_DF2 = 0x9a;
        public static final int CODE_C1_DF3 = 0x9b;
        public static final int CODE_C1_DF4 = 0x9c;
        public static final int CODE_C1_DF5 = 0x9d;
        public static final int CODE_C1_DF6 = 0x9e;
        public static final int CODE_C1_DF7 = 0x9f;
    }

    /**
     * CEA-708B-specific color.
     */
    public static class CaptionColor {
        public static final int OPACITY_SOLID = 0;
        public static final int OPACITY_FLASH = 1;
        public static final int OPACITY_TRANSLUCENT = 2;
        public static final int OPACITY_TRANSPARENT = 3;

        private static final int[] COLOR_MAP = new int[] { 0x00, 0x0f, 0xf0, 0xff };
        private static final int[] OPACITY_MAP = new int[] { 0xff, 0xfe, 0x80, 0x00 };

        public final int opacity;
        public final int red;
        public final int green;
        public final int blue;

        CaptionColor(int opacity, int red, int green, int blue) {
            this.opacity = opacity;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public int getArgbValue() {
            return Color.argb(
                    OPACITY_MAP[opacity], COLOR_MAP[red], COLOR_MAP[green], COLOR_MAP[blue]);
        }
    }

    /**
     * Caption event generated by {@link Cea708CCParser}.
     */
    public static class CaptionEvent {
        public final int type;
        public final Object obj;

        CaptionEvent(int type, Object obj) {
            this.type = type;
            this.obj = obj;
        }
    }

    /**
     * Pen style information.
     */
    public static class CaptionPenAttr {
        // Pen sizes
        public static final int PEN_SIZE_SMALL = 0;
        public static final int PEN_SIZE_STANDARD = 1;
        public static final int PEN_SIZE_LARGE = 2;

        // Offsets
        public static final int OFFSET_SUBSCRIPT = 0;
        public static final int OFFSET_NORMAL = 1;
        public static final int OFFSET_SUPERSCRIPT = 2;

        public final int penSize;
        public final int penOffset;
        public final int textTag;
        public final int fontTag;
        public final int edgeType;
        public final boolean underline;
        public final boolean italic;

        CaptionPenAttr(int penSize, int penOffset, int textTag, int fontTag, int edgeType,
                boolean underline, boolean italic) {
            this.penSize = penSize;
            this.penOffset = penOffset;
            this.textTag = textTag;
            this.fontTag = fontTag;
            this.edgeType = edgeType;
            this.underline = underline;
            this.italic = italic;
        }
    }

    /**
     * {@link CaptionColor} objects that indicate the foreground, background, and edge color of a
     * pen.
     */
    public static class CaptionPenColor {
        public final CaptionColor foregroundColor;
        public final CaptionColor backgroundColor;
        public final CaptionColor edgeColor;

        CaptionPenColor(CaptionColor foregroundColor, CaptionColor backgroundColor,
                CaptionColor edgeColor) {
            this.foregroundColor = foregroundColor;
            this.backgroundColor = backgroundColor;
            this.edgeColor = edgeColor;
        }
    }

    /**
     * Location information of a pen.
     */
    public static class CaptionPenLocation {
        public final int row;
        public final int column;

        CaptionPenLocation(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    /**
     * Attributes of a caption window, which is defined in CEA-708B.
     */
    public static class CaptionWindowAttr {
        public final CaptionColor fillColor;
        public final CaptionColor borderColor;
        public final int borderType;
        public final boolean wordWrap;
        public final int printDirection;
        public final int scrollDirection;
        public final int justify;
        public final int effectDirection;
        public final int effectSpeed;
        public final int displayEffect;

        CaptionWindowAttr(CaptionColor fillColor, CaptionColor borderColor, int borderType,
                boolean wordWrap, int printDirection, int scrollDirection, int justify,
                int effectDirection,
                int effectSpeed, int displayEffect) {
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.borderType = borderType;
            this.wordWrap = wordWrap;
            this.printDirection = printDirection;
            this.scrollDirection = scrollDirection;
            this.justify = justify;
            this.effectDirection = effectDirection;
            this.effectSpeed = effectSpeed;
            this.displayEffect = displayEffect;
        }
    }

    /**
     * Construction information of the caption window of CEA-708B.
     */
    public static class CaptionWindow {
        public final int id;
        public final boolean visible;
        public final boolean rowLock;
        public final boolean columnLock;
        public final int priority;
        public final boolean relativePositioning;
        public final int anchorVertical;
        public final int anchorHorizontal;
        public final int anchorId;
        public final int rowCount;
        public final int columnCount;
        public final int penStyle;
        public final int windowStyle;

        CaptionWindow(int id, boolean visible,
                boolean rowLock, boolean columnLock, int priority, boolean relativePositioning,
                int anchorVertical, int anchorHorizontal, int anchorId,
                int rowCount, int columnCount, int penStyle, int windowStyle) {
            this.id = id;
            this.visible = visible;
            this.rowLock = rowLock;
            this.columnLock = columnLock;
            this.priority = priority;
            this.relativePositioning = relativePositioning;
            this.anchorVertical = anchorVertical;
            this.anchorHorizontal = anchorHorizontal;
            this.anchorId = anchorId;
            this.rowCount = rowCount;
            this.columnCount = columnCount;
            this.penStyle = penStyle;
            this.windowStyle = windowStyle;
        }
    }
}
