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

#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#import <IOSurface/IOSurfaceRef.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * IOSurface-backed Metal drawable from a managed pool.
 * Managed by CMPMetalLayer with explicit lifecycle control.
 */
@interface CMPDrawable : NSObject

/// Size of the drawable texture
@property (readonly) CGSize textureSize;

/// Backing IOSurface displayed by the layer.
@property (nonatomic, readonly) IOSurfaceRef surface;

/// Tracks GPU command buffer completion.
@property (nonatomic, assign) BOOL isWaitingForCommandBufferCompletion;

/// Last presentation timestamp for pool management.
@property (nonatomic, assign) CFTimeInterval presentedTime;

/// Reference to the reusable associated skia surface.
@property (nonatomic, weak, nullable) id associatedSkiaSurface;

- (instancetype)initWithTexture:(id<MTLTexture>)texture size:(CGSize)textureSize surface:(IOSurfaceRef)surface;

/// Get metal texture for rendering.
- (void * CMP_BORROWED)drawableTexture;

/**
 * Eagerly releases the Metal texture and IOSurface backing this drawable.
 * Safe to call multiple times (idempotent). Called automatically from -dealloc.
 */
- (void)dispose;

@end

NS_ASSUME_NONNULL_END
