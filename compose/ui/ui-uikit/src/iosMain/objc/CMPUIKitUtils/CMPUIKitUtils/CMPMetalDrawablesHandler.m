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

#import "CMPMetalDrawablesHandler.h"

@implementation CMPMetalDrawablesHandler {
    CAMetalLayer *_metalLayer;
}

- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer {
    self = [super init];
    if (self) {
        _metalLayer = metalLayer;
    }
    return self;
}

- (void * CMP_OWNED)nextDrawable {
    id <CAMetalDrawable> drawable = [_metalLayer nextDrawable];
    
    if (drawable) {
        void *ptr = (__bridge_retained void *)drawable;
        return ptr;
    } else {
        return NULL;
    }
}

- (void)releaseDrawable:(void * CMP_CONSUMED)drawablePtr {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable __unused = (__bridge_transfer id <CAMetalDrawable>)drawablePtr;
    // drawable will be released by ARC
}

- (void * CMP_BORROWED)drawableTexture:(void * CMP_BORROWED)drawablePtr {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable = (__bridge id <CAMetalDrawable>)drawablePtr;
    id <MTLTexture> texture = drawable.texture;    
    void *texturePtr = (__bridge void *)texture;
    return texturePtr;
}

- (void)presentDrawable:(void * CMP_CONSUMED)drawablePtr {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable = (__bridge_transfer id <CAMetalDrawable>)drawablePtr;
    [drawable present];
}

- (void)scheduleDrawablePresentation:(void * CMP_CONSUMED)drawablePtr onCommandBuffer:(id <MTLCommandBuffer>)commandBuffer {
    assert(drawablePtr != NULL);
    
    id <CAMetalDrawable> drawable = (__bridge_transfer id <CAMetalDrawable>)drawablePtr;
    [commandBuffer presentDrawable:drawable];
}

@end
