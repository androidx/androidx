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

#import <QuartzCore/QuartzCore.h>
#import <Metal/Metal.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

@class CMPDrawable;

/**
 * CALayer managing a pool of up to 3 IOSurface-backed Metal drawables.
 * Provides explicit control over drawable lifecycle and presentation timing.
 *
 * Usage: nextDrawable → render → prepareDrawableForPresent → commit → presentDrawable
 */
@interface CMPMetalLayer : CALayer

@property (nonatomic, strong, nullable) id<MTLDevice> device;
@property (nonatomic, assign) CGSize drawableSize;

/**
 * This increases every time the metal layer clears the active set of drawables.
 * The new generation guarantees that none of the drawables previously used will be returned by the nextDrawable method.
 */
@property (nonatomic, readonly) NSInteger drawablesGeneration;

/**
 * Acquires next drawable, blocking up to 1 second if pool is exhausted.
 * Returns nil on timeout or if last presented drawable is still in use by GPU.
 */
- (nullable CMPDrawable *)nextDrawable;

/**
 * Sets up GPU completion tracking for the drawable.
 * Call before committing the command buffer.
 */
- (void)prepareDrawableForPresent:(CMPDrawable *)drawable
                    commandBuffer:(id<MTLCommandBuffer>)commandBuffer;

/**
 * Presents drawable on screen via CATransaction. Thread-safe; dispatches to main thread if needed.
 * The displayHandler is called on the main thread with the presentation CATransaction only when
 * content of the drawable is presented.
 */
- (void)presentDrawable:(CMPDrawable *)drawable
              onDisplay:(void (^)(void))displayHandler;

/**
 * Returns drawable to pool without presenting.
 * Use when cancelling or recovering from errors.
 */
- (void)releaseDrawable:(CMPDrawable *)drawable;

/**
 * Disposes and removes all drawables currently held in the pool.
 * Thread-safe; may be called from any thread.
 */
- (void)drainDrawables;

@end

NS_ASSUME_NONNULL_END
