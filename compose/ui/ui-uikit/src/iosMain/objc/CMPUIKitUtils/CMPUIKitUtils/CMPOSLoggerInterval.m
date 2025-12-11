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

#import "CMPOSLoggerInterval.h"

#import <os/signpost.h>

@implementation CMPOSLoggerInterval {
    os_log_t _log;
    os_signpost_id_t _signpostId;
}

- (instancetype)initWithLog:(os_log_t)log {
    self = [super init];
    
    if (self) {
        _log = log;
        _signpostId = os_signpost_id_generate(_log);
    }
    
    return self;
}

- (void)beginWithName:(NSString *)name {    
    os_signpost_interval_begin(_log, _signpostId, "CMPInterval", "%{public}s", [name UTF8String]);
}

- (void)end {
    os_signpost_interval_end(_log, _signpostId, "CMPInterval");
}

@end
