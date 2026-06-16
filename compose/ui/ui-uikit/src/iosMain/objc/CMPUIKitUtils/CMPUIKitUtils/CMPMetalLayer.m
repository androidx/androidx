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

#import "CMPMetalLayer.h"
#import "CMPDrawable.h"
#import <IOSurface/IOSurfaceRef.h>
#import <CoreVideo/CoreVideo.h>
#import <UIKit/UIKit.h>

static const NSUInteger kMaxDrawables = 3;
static const NSTimeInterval kDrawablePollingInterval = 0.001;
static const NSTimeInterval kDrawableAcquisitionTimeout = 1.0;
static const NSUInteger kBGRA32ColorFormatBytesPerPixel = 4;

@implementation CMPMetalLayer {
    NSLock *_drawablesLock;
    NSMutableSet<CMPDrawable *> *_availableDrawables;
    NSUInteger _totalDrawables;
    CMPDrawable *_lastPresentedDrawable;
    CGSize _drawableSize;
    CFTimeInterval _lastDrawablePresentedTime;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _device = MTLCreateSystemDefaultDevice();
        _drawablesLock = [[NSLock alloc] init];
        _availableDrawables = [NSMutableSet set];
        _totalDrawables = 0;
        _lastPresentedDrawable = nil;
        _drawableSize = CGSizeZero;
        _drawablesGeneration = 0;
        _lastDrawablePresentedTime = 0;
    }
    return self;
}

- (void)setDrawableSize:(CGSize)size {
    NSAssert([NSThread isMainThread], @"setDrawableSize - must be called on main thread");
    if (!CGSizeEqualToSize(_drawableSize, size)) {
        [_drawablesLock lock];
        _drawableSize = size;
        [self drainDrawablesUnsafe];
        [_drawablesLock unlock];
    }
}

- (CGSize)drawableSize {
    NSAssert([NSThread isMainThread], @"getDrawableSize - must be called on main thread");
    return _drawableSize;
}

- (IOSurfaceRef)IOSurface {
    NSInteger width = (NSInteger)_drawableSize.width;
    NSInteger height = (NSInteger)_drawableSize.height;

    if (width <= 0 || height <= 0) {
        return NULL;
    }

    NSUInteger bytesPerRow = IOSurfaceAlignProperty(kIOSurfaceBytesPerRow, width * kBGRA32ColorFormatBytesPerPixel);
    NSUInteger allocSize = IOSurfaceAlignProperty(kIOSurfaceAllocSize, height * bytesPerRow);

    NSDictionary *properties = @{
        (id)kIOSurfaceWidth: @(width),
        (id)kIOSurfaceHeight: @(height),
        (id)kIOSurfacePixelFormat: @(kCVPixelFormatType_32BGRA),
        (id)kIOSurfaceBytesPerRow: @(bytesPerRow),
        (id)kIOSurfaceAllocSize: @(allocSize),
        (id)kIOSurfaceBytesPerElement: @(kBGRA32ColorFormatBytesPerPixel)
    };

    return IOSurfaceCreate((__bridge CFDictionaryRef)properties);
}

- (nullable CMPDrawable *)findNextDrawable {
    [_drawablesLock lock];

    if (_lastPresentedDrawable != nil && _lastPresentedDrawable.isWaitingForCommandBufferCompletion) {
        [_drawablesLock unlock];
        return nil;
    }

    CMPDrawable *result = nil;

    if (_totalDrawables < kMaxDrawables) {
        IOSurfaceRef surface = [self IOSurface];
        if (surface != NULL) {
            MTLTextureDescriptor *descriptor = [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                                                                                  width:(NSUInteger)_drawableSize.width
                                                                                                 height:(NSUInteger)_drawableSize.height
                                                                                              mipmapped:NO];
            descriptor.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead | MTLTextureUsageShaderWrite;

            id<MTLTexture> texture = [_device newTextureWithDescriptor:descriptor iosurface:surface plane:0];

            if (texture != nil) {
                result = [[CMPDrawable alloc] initWithTexture:texture size:_drawableSize surface:surface];
                _totalDrawables++;
            }

            CFRelease(surface);
        }
    } else {
        // Prefer a drawable that is not in use, or the oldest presented drawable
        // Separate idle and in-use drawables for clarity
        CMPDrawable *oldestIdle = nil;
        CMPDrawable *oldestInUse = nil;
        CFTimeInterval oldestIdleTime = INFINITY;
        CFTimeInterval oldestInUseTime = INFINITY;

        for (CMPDrawable *drawable in _availableDrawables) {
            BOOL inUse = IOSurfaceIsInUse(drawable.surface);

            if (!inUse) {
                if (drawable.presentedTime < oldestIdleTime) {
                    oldestIdle = drawable;
                    oldestIdleTime = drawable.presentedTime;
                }
            } else {
                if (drawable.presentedTime < oldestInUseTime) {
                    oldestInUse = drawable;
                    oldestInUseTime = drawable.presentedTime;
                }
            }
        }

        // Prefer oldest idle, fall back to oldest in-use
        CMPDrawable *bestDrawable = oldestIdle ?: oldestInUse;

        if (bestDrawable != nil) {
            result = bestDrawable;
            [_availableDrawables removeObject:bestDrawable];
        }
    }

    [_drawablesLock unlock];
    return result;
}

- (nullable CMPDrawable *)nextDrawable {
    NSTimeInterval startTime = CACurrentMediaTime();

    while (true) {
        CMPDrawable *drawable = [self findNextDrawable];
        if (drawable != nil) {
            return drawable;
        }

        if (CACurrentMediaTime() - startTime >= kDrawableAcquisitionTimeout) {
            return nil;
        }

        [NSThread sleepForTimeInterval:kDrawablePollingInterval];
    }
}

- (void)prepareDrawableForPresent:(CMPDrawable *)drawable
                    commandBuffer:(id<MTLCommandBuffer>)commandBuffer {
    drawable.isWaitingForCommandBufferCompletion = YES;

    __weak CMPDrawable *weakDrawable = drawable;
    [commandBuffer addCompletedHandler:^(id<MTLCommandBuffer> _Nonnull buffer) {
        CMPDrawable *strongDrawable = weakDrawable;
        if (strongDrawable != nil) {
            strongDrawable.isWaitingForCommandBufferCompletion = NO;
        }
    }];
}

- (void)presentDrawable:(CMPDrawable *)drawable
              onDisplay:(void (^)(void))displayHandler {
    drawable.presentedTime = CACurrentMediaTime();

    if ([NSThread isMainThread]) {
        [self presentOnMainThread:drawable onDisplay: displayHandler];
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self presentOnMainThread:drawable onDisplay: displayHandler];
        });
    }
}

- (void)presentOnMainThread:(CMPDrawable *)drawable
                  onDisplay:(void (^)(void))displayHandler {
    NSAssert([NSThread isMainThread], @"presentOnMainThread - must be called on main thread");

    if (!CGSizeEqualToSize(drawable.textureSize, _drawableSize)) {
        [drawable dispose];
        // Invalid drawable size. Disposing.
        return;
    }

    if (_lastDrawablePresentedTime > drawable.presentedTime) {
        [self releaseDrawable:drawable];
        // Drop drawable that was scheduled before the already presented one
        return;
    }

    _lastDrawablePresentedTime = drawable.presentedTime;

    [_drawablesLock lock];
    if (_lastPresentedDrawable != nil) {
        [_availableDrawables addObject:_lastPresentedDrawable];
    }
    _lastPresentedDrawable = drawable;
    [_drawablesLock unlock];

    [self setNeedsDisplay]; // Prevents frame drops during touch events

    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    self.contents = (__bridge id)drawable.surface;
    if (displayHandler != nil) {
        displayHandler();
    }
    [CATransaction commit];
}

- (void)releaseDrawable:(CMPDrawable *)drawable {
    [_drawablesLock lock];
    [_availableDrawables addObject:drawable];
    [_drawablesLock unlock];
}

- (void)drainDrawablesUnsafe {
    for (CMPDrawable *drawable in _availableDrawables) {
        [drawable dispose];
    }
    [_availableDrawables removeAllObjects];
    [_lastPresentedDrawable dispose];
    _lastPresentedDrawable = nil;
    _totalDrawables = 0;
    _drawablesGeneration++;
}

- (void)drainDrawables {
    [_drawablesLock lock];
    [self drainDrawablesUnsafe];
    [_drawablesLock unlock];
}

@end
