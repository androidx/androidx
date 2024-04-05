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

#import "CMPAccessibilityContainer.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPAccessibilityContainer

- (NSInteger)accessibilityElementCount {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (NSInteger)indexOfAccessibilityElement:(id)element {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (__nullable id)accessibilityElementAtIndex:(NSInteger)index {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)setIsAccessibilityElement:(BOOL)isAccessibilityElement {
    // NoOp
}

- (BOOL)isAccessibilityElement {
    return NO;
}
 
- (void)setAccessibilityContainer:(__nullable id)accessibilityContainer {
    // NoOp
}

- (__nullable id)accessibilityContainer {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

- (void)setAccessibilityFrame:(CGRect)accessibilityFrame {
    // NoOp
}

- (CGRect)accessibilityFrame {
    CMP_MUST_BE_OVERRIDED_INVARIANT_VIOLATION
}

@end

NS_ASSUME_NONNULL_END
