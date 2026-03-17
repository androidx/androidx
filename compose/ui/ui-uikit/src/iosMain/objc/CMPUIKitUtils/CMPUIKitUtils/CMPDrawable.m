/*
 * Copyright 2026 The Android Open Source Project
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

#import "CMPDrawable.h"
#import <UIKit/UIKit.h>

@interface CMPDrawable() {
    /// Metal texture for rendering.
    id<MTLTexture> _texture;
}

@end


@implementation CMPDrawable

- (instancetype)initWithTexture:(id<MTLTexture>)texture size:(CGSize)textureSize surface:(IOSurfaceRef)surface {
    self = [super init];
    if (self) {
        _texture = texture;
        _surface = surface;
        CFRetain(_surface);
        _presentedTime = 0;
        _isWaitingForCommandBufferCompletion = NO;
        _textureSize = textureSize;
    }
    return self;
}

- (void)dispose {
    _texture = nil;
    if (_surface != NULL) {
        CFRelease(_surface);
        _surface = NULL;
    }
}

- (void * CMP_BORROWED)drawableTexture {
    assert(_texture != NULL);
    void *texturePtr = (__bridge void *)_texture;
    return texturePtr;
}

- (void)dealloc {
    [self dispose];
}

@end
