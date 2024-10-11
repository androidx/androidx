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

package androidx.benchmark.vmtrace;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

class VmTraceParser {

    static final Charset CHARSET_US_ASCII = Charset.forName("US-ASCII");
    static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
    private static final int TRACE_MAGIC = 0x574f4c53; // 'SLOW'
    private static final String HEADER_SECTION_VERSION = "*version";
    private static final String HEADER_SECTION_THREADS = "*threads";
    private static final String HEADER_SECTION_METHODS = "*methods";
    private static final String HEADER_END = "*end";
    private static final String KEY_CLOCK = "clock";

    private final File mTraceFile;
    final VmTraceHandler mTraceDataHandler;
    int mVersion;
    private VmClockType mVmClockType;

    VmTraceParser(@NonNull File traceFile, @NonNull VmTraceHandler traceHandler) {
        if (!traceFile.exists()) {
            throw new IllegalArgumentException(
                    "Trace file " + traceFile.getAbsolutePath() + " does not exist.");
        }
        mTraceFile = traceFile;
        mTraceDataHandler = traceHandler;
    }

    public void parse() throws IOException {
        ByteBuffer buffer;
        if (isStreamingTrace(mTraceFile)) {
            throw new UnsupportedOperationException("Streaming traces are not supported.");
        } else {
            long headerLength = parseHeader(mTraceFile);
            buffer = ByteBufferUtil.mapFile(mTraceFile, headerLength, ByteOrder.LITTLE_ENDIAN);
        }
        parseData(buffer);
    }

    private static boolean isStreamingTrace(File file) throws IOException {
        BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), CHARSET_US_ASCII));
        try {
            String firstLine = in.readLine();
            if (firstLine != null && firstLine.startsWith(HEADER_SECTION_VERSION)) {
                // Trace file not obtained by using streaming mode
                return false;
            }
        } finally {
            in.close();
        }
        return true;
    }

    // The values of PARSE_METHODS, PARSE_THREADS and PARSE_SUMMARY match the corresponding value
    // in trace files,
    // which are written by Android Runtime (ART) code. Please do not change their values without
    // a matching change to ART.
    private static final int PARSE_VERSION = 0;
    private static final int PARSE_METHODS = 1;
    private static final int PARSE_THREADS = 2;
    private static final int PARSE_OPTIONS = 4;

    /** Parses the trace file header and returns the offset in the file where the header ends. */
    long parseHeader(File f) throws IOException {
        long offset = 0;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f), CHARSET_UTF_8));
            int mode = PARSE_VERSION;
            String line;
            while (true) {
                line = in.readLine();
                if (line == null) {
                    throw new IOException("Key section does not have an *end marker");
                }

                // Calculate how much we have read from the file so far. The extra byte is for
                // the line ending not included by readLine().
                // We can't use line.length() as unicode characters can be represented by more
                // than 1 byte.
                offset += line.getBytes(CHARSET_UTF_8).length + 1;

                if (line.startsWith("*")) {
                    if (line.equals(HEADER_SECTION_VERSION)) {
                        mode = PARSE_VERSION;
                        continue;
                    }
                    if (line.equals(HEADER_SECTION_THREADS)) {
                        mode = PARSE_THREADS;
                        continue;
                    }
                    if (line.equals(HEADER_SECTION_METHODS)) {
                        mode = PARSE_METHODS;
                        continue;
                    }
                    if (line.equals(HEADER_END)) {
                        break;
                    }
                }

                switch (mode) {
                    case PARSE_VERSION:
                        mVersion = Integer.decode(line);
                        mTraceDataHandler.setVersion(mVersion);
                        mode = PARSE_OPTIONS;
                        break;
                    case PARSE_THREADS:
                        parseThread(line);
                        break;
                    case PARSE_METHODS:
                        parseMethod(line);
                        break;
                    case PARSE_OPTIONS:
                        parseOption(line);
                        break;
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // cannot happen
                }
            }
        }

        return offset;
    }

    /** Parses trace option formatted as a key value pair. */
    void parseOption(String line) {
        String[] tokens = line.split("=");
        if (tokens.length == 2) {
            String key = tokens[0];
            String value = tokens[1];
            mTraceDataHandler.setProperty(key, value);

            if (key.equals(KEY_CLOCK)) {
                if (value.equals("thread-cpu")) {
                    mVmClockType = VmClockType.THREAD_CPU;
                } else if (value.equals("wall")) {
                    mVmClockType = VmClockType.WALL;
                } else if (value.equals("dual")) {
                    mVmClockType = VmClockType.DUAL;
                }
            }

        }
    }

    /** Parses thread information comprising an integer id and the thread name */
    private void parseThread(String line) {
        int index = line.indexOf('\t');
        if (index < 0) {
            return;
        }

        try {
            int id = Integer.decode(line.substring(0, index));
            String name = line.substring(index).trim();
            mTraceDataHandler.addThread(id, name);
        } catch (NumberFormatException ignored) {
        }
    }

    void parseMethod(String line) {
        String[] tokens = line.split("\t");
        long id;
        try {
            id = Long.decode(tokens[0]);
        } catch (NumberFormatException e) {
            return;
        }

        String className = tokens[1];
        String methodName = null;
        String signature = null;
        String pathname = null;
        int lineNumber = -1;
        if (tokens.length == 6) {
            methodName = tokens[2];
            signature = tokens[3];
            pathname = tokens[4];
            lineNumber = Integer.decode(tokens[5]);
            pathname = constructPathname(className, pathname);
        } else if (tokens.length > 2) {
            if (tokens[3].startsWith("(")) {
                methodName = tokens[2];
                signature = tokens[3];
                if (tokens.length >= 5) {
                    pathname = tokens[4];
                }
            } else {
                pathname = tokens[2];
                lineNumber = Integer.decode(tokens[3]);
            }
        }

        mTraceDataHandler.addMethod(
                id, new MethodInfo(id, className, methodName, signature, pathname, lineNumber));
    }

    private String constructPathname(String className, String pathname) {
        int index = className.lastIndexOf('/');
        if (index > 0 && index < className.length() - 1 && pathname.endsWith(".java")) {
            pathname = className.substring(0, index + 1) + pathname;
        }
        return pathname;
    }

    /**
     * Parses the data section of the trace. The data section comprises of a header followed
     * by a list of records.
     *
     * All values are stored in little-endian order.
     */
    private void parseData(ByteBuffer buffer) {
        int recordSize = readDataFileHeader(buffer);
        parseMethodTraceData(buffer, recordSize);
    }

    /**
     * Parses the list of records corresponding to each trace event (method entry, exit, ...)
     * Record format v1:
     * u1  thread ID
     * u4  method ID | method action
     * u4  time delta since start, in usec
     *
     * Record format v2:
     * u2  thread ID
     * u4  method ID | method action
     * u4  time delta since start, in usec
     *
     * Record format v3:
     * u2  thread ID
     * u4  method ID | method action
     * u4  time delta since start, in usec
     * u4  wall time since start, in usec (when clock == "dual" only)
     *
     * 32 bits of microseconds is 70 minutes.
     */
    private void parseMethodTraceData(ByteBuffer buffer, int recordSize) {
        int methodId;
        int threadId;
        int version = mVersion;
        while (buffer.hasRemaining()) {
            int threadTime;
            int globalTime;

            int positionStart = buffer.position();

            threadId = version == 1 ? buffer.get() : buffer.getShort();
            methodId = buffer.getInt();

            switch (mVmClockType) {
                case WALL:
                    globalTime = buffer.getInt();
                    threadTime = globalTime;
                    break;
                case DUAL:
                    threadTime = buffer.getInt();
                    globalTime = buffer.getInt();
                    break;
                case THREAD_CPU:
                default:
                    threadTime = buffer.getInt();
                    globalTime = threadTime;
                    break;
            }

            int positionEnd = buffer.position();
            int bytesRead = positionEnd - positionStart;
            if (bytesRead < recordSize) {
                buffer.position(positionEnd + (recordSize - bytesRead));
            }

            int action = methodId & 0x03;
            TraceAction methodAction;
            switch (action) {
                case 0:
                    methodAction = TraceAction.METHOD_ENTER;
                    break;
                case 1:
                    methodAction = TraceAction.METHOD_EXIT;
                    break;
                case 2:
                    methodAction = TraceAction.METHOD_EXIT_UNROLL;
                    break;
                default:
                    throw new RuntimeException(
                            "Invalid trace action, expected one of method entry, exit or unroll.");
            }
            methodId &= ~0x03;

            mTraceDataHandler.addMethodAction(
                    threadId, unsignedIntToLong(methodId), methodAction, threadTime, globalTime);
        }
    }

    private static long unsignedIntToLong(int value) {
        return value & 0xffffffffL;
    }

    /**
     * Parses the data header with the following format:
     * u4  magic ('SLOW')
     * u2  version
     * u2  offset to data
     * u8  start date/time in usec
     * u2  record size in bytes (version >= 2 only)
     * ... padding to 32 bytes
     *
     * @param buffer byte buffer pointing to the header
     * @return record size for each data entry following the header
     */
    private int readDataFileHeader(ByteBuffer buffer) {
        validateMagic(buffer.getInt());
        // read version
        int version = buffer.getShort();
        if (version != mVersion) {
            String msg =
                    String.format(
                            "Error: version number mismatch; got %d in data header but %d in "
                                    + "options\n",
                            version, mVersion);
            throw new RuntimeException(msg);
        }
        validateTraceVersion(version);

        // read offset
        int offsetToData = buffer.getShort() - 16;

        // read startWhen
        mTraceDataHandler.setStartTimeUs(buffer.getLong());

        // read record size
        int recordSize;
        switch (version) {
            case 1:
                recordSize = 9;
                break;
            case 2:
                recordSize = 10;
                break;
            default:
                recordSize = buffer.getShort();
                offsetToData -= 2;
                break;
        }

        // Skip over offsetToData bytes
        while (offsetToData-- > 0) {
            buffer.get();
        }

        return recordSize;
    }

    static void validateTraceVersion(int version) {
        if (version < 1 || version > 3) {
            String msg =
                    String.format(
                            "Error: unsupported trace version number %d.  "
                                    + "Please use a newer version of TraceView to read this file.",
                            version);
            throw new RuntimeException(msg);
        }
    }

    static void validateMagic(int magic) {
        if (magic != TRACE_MAGIC) {
            String msg =
                    String.format(
                            "Error: magic number mismatch; got 0x%x, expected 0x%x\n",
                            magic, TRACE_MAGIC);
            throw new RuntimeException(msg);
        }
    }
}
