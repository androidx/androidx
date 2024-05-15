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

#ifndef ANDROIDX_PDF_EXTRACTORS_H
#define ANDROIDX_PDF_EXTRACTORS_H

#include <stdint.h>

namespace pdflib {

// Interface for extracting bytes from or into an underlying something.
    class Extractor {
    public:
        // Transfers {num_bytes} bytes between the underlying something and {buffer}.
        virtual ~Extractor();

        virtual bool extract(uint8_t *buffer, int num_bytes) = 0;
    };


// An Extractor that copies bytes on the given buffer.
    class BufferWriter : public pdflib::Extractor {
    public:
        explicit BufferWriter(uint8_t *buffer) : buffer_(buffer) {}

        ~BufferWriter() override;

        bool extract(uint8_t *source, int num_bytes) override;

    private:
        uint8_t *buffer_;
    };

// An Extractor that copies bytes from the given buffer.
    class BufferReader : public pdflib::Extractor {
    public:
        explicit BufferReader(uint8_t *buffer) : buffer_(buffer) {}

        ~BufferReader() override;

        bool extract(uint8_t *source, int num_bytes) override;

    private:
        uint8_t *buffer_;
    };

// An extractor that writes bytes on the given fd. It closes the fd thereafter.
    class FdWriter : public pdflib::Extractor {
    public:
        explicit FdWriter(int fd) : fd_(fd) {}

        ~FdWriter() override;

        bool extract(uint8_t *source, int num_bytes) override;

    private:
        int fd_;
    };

// An extractor that read bytes from the given fd. It closes the fd thereafter.
    class FdReader : public pdflib::Extractor {
    public:
        explicit FdReader(int fd) : fd_(fd) {}

        ~FdReader() override;

        bool extract(uint8_t *destination, int num_bytes) override;

    private:
        int fd_;
    };

}  // namespace pdfClient

#endif  // ANDROIDX_PDF_EXTRACTORS_H
