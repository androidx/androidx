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

#include "bitmap_parcel.h"

#include <android/bitmap.h>
#include <jni.h>
#include <stdint.h>

#include "extractors.h"
#include "logging.h"

#define LOG_TAG "bitmap_parcel"

using pdflib::Extractor;
using pdflib::BufferReader;
using pdflib::FdReader;

namespace {

    static const int kBytesPerPixel = 4;

    bool FeedBitmap(JNIEnv *env, jobject jbitmap, Extractor *source);

}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_androidx_pdf_util_BitmapParcel_readIntoBitmap
        (JNIEnv *env, jclass, jobject jbitmap, int fd) {
    FdReader source(fd);
    return FeedBitmap(env, jbitmap, &source);
}

namespace {

    bool FeedBitmap(JNIEnv *env, jobject jbitmap, Extractor *source) {
        void *bitmap_pixels;
        int ret;
        if ((ret = AndroidBitmap_lockPixels(env, jbitmap, &bitmap_pixels)) < 0) {
            LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
            return false;
        }


        AndroidBitmapInfo info;
        AndroidBitmap_getInfo(env, jbitmap, &info);

        int num_bytes = info.width * info.height * kBytesPerPixel;
        uint8_t *bitmap_bytes = reinterpret_cast<uint8_t *>(bitmap_pixels);
        source->extract(bitmap_bytes, num_bytes);

        AndroidBitmap_unlockPixels(env, jbitmap);
        LOGV("Copied %d bytes into bitmap", num_bytes);
        return true;
    }

}  // namespace
