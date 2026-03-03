/*
 * Copyright 2025 The Android Open Source Project
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

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, CMPLayoutRegionAdaptivityAxis) {
    CMPLayoutRegionAdaptivityAxisNone = 0,
    CMPLayoutRegionAdaptivityAxisHorizontal = 1,
    CMPLayoutRegionAdaptivityAxisVertical = 2,
};

@interface CMPLayoutRegion : NSObject

- (instancetype)init NS_UNAVAILABLE;
- (instancetype)new NS_UNAVAILABLE;

+ (instancetype)marginsWithCornerAdaptation:(CMPLayoutRegionAdaptivityAxis)axis;
+ (instancetype)readableContentWithCornerAdaptation:(CMPLayoutRegionAdaptivityAxis)axis;
+ (instancetype)safeAreaWithCornerAdaptation:(CMPLayoutRegionAdaptivityAxis)axis;

- (UIEdgeInsets)edgeInsetsInView:(UIView *)view;

@end

NS_ASSUME_NONNULL_END
