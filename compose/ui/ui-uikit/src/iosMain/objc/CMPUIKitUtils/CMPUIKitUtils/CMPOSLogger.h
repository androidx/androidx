/*
 * Copyright 2024 The Android Open Source Project
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

#import "CMPOSLoggerInterval.h"

NS_ASSUME_NONNULL_BEGIN

@interface CMPOSLogger : NSObject

- (instancetype)initWithCategoryName:(NSString *)name;
- (CMPOSLoggerInterval *)beginIntervalNamed:(NSString *)name;
- (void)endInterval:(CMPOSLoggerInterval *)interval;

@end

NS_ASSUME_NONNULL_END

void CMPOSInitializeAppTraceLogger(NSString * _Nonnull name);
CMPOSLogger * _Nullable CMPOSAppTraceLogger(void);
