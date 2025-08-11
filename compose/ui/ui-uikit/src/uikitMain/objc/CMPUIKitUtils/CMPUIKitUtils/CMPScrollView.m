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

#import "CMPScrollView.h"

@implementation CMPScrollView

// Private API methods that enable scroll commands.
// The scroll command will be propagated as an accessibilityScroll
// function call for the corresponding accessibility element.

- (BOOL)accessibilityScrollUpPageSupported {
    return YES;
}

- (BOOL)accessibilityScrollDownPageSupported {
    return YES;
}

- (BOOL)accessibilityScrollLeftPageSupported {
    return YES;
}

- (BOOL)accessibilityScrollRightPageSupported {
    return YES;
}

- (BOOL)_drawsFocusRingWhenChildrenFocused {
    return [self drawsFocusRingWhenChildrenFocused];
}

- (BOOL)drawsFocusRingWhenChildrenFocused {
    return YES;
}

@end
