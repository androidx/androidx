/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "extractors.h"

#include <stdint.h>
#include <unistd.h>

#include <cstring>

#include "logging.h"

#define LOG_TAG "extractor"

namespace pdflib {

    Extractor::~Extractor() {}

    BufferWriter::~BufferWriter() {}

    bool BufferWriter::extract(uint8_t *source, int num_bytes) {
        memcpy(buffer_, source, num_bytes);
        return true;
    }

    BufferReader::~BufferReader() {}

    bool BufferReader::extract(uint8_t *destination, int num_bytes) {
        memcpy(destination, buffer_, num_bytes);
        return true;
    }

    FdWriter::~FdWriter() {}

    bool FdWriter::extract(uint8_t *source, int num_bytes) {
        LOGV("FdWriter Extracting %d bytes on %d", num_bytes, fd_);
        bool ret = true;
        while (num_bytes > 0) {
            int len = write(fd_, source, num_bytes);
            if (len == -1 || len == 0) {
                ret = false;
                LOGD("FdWriter extract failed at %d on %d", num_bytes, fd_);
                break;
            }
            num_bytes -= len;
            source += len;
        }
        close(fd_);
        return ret;
    }

    FdReader::~FdReader() {}

    bool FdReader::extract(uint8_t *destination, int num_bytes) {
        LOGV("FdReader Extracting %d bytes from %d", num_bytes, fd_);
        bool ret = true;
        while (num_bytes > 0) {
            int len = read(fd_, destination, num_bytes);
            if (len == -1 || len == 0) {
                ret = false;
                LOGD("FdWriter extract failed at %d on %d", num_bytes, fd_);
                break;
            }
            num_bytes -= len;
            destination += len;
        }
        close(fd_);
        return ret;
    }

}  // namespace pdflib
