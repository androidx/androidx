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

#import <Foundation/Foundation.h>

/// Marker for indicating that function is abstract and is expected to be overrided by subclasses
#define CMP_ABSTRACT_FUNCTION

/// Macro for assertion failure due to function marked as `CMP_ABSTRACT_FUNCTION`  wasn't overrided or called `super` implementation
#define CMP_ABSTRACT_FUNCTION_CALLED assert(false && "This function must be overrided, and not call super implementation");

/// Marker for functions which can be override, but have default behavior
#define CMP_CAN_OVERRIDE

/// Marker for indicating that raw pointer returned from a function is owned by the caller. It's responsible for releasing it or passing it to
/// API marked with `CMP_CONSUMED`
#define CMP_OWNED

/// Marker for indicating that raw pointer is consumed when passed as an argument, owner is not allowed to use it anymore
#define CMP_CONSUMED

/// Marker for indicating that raw pointer is implied as borrowed when returned from a function or passed as an argument
/// No actions implying ownership are required
#define CMP_BORROWED
