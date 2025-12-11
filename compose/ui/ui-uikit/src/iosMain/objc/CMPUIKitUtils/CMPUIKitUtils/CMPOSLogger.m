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

#import "CMPOSLogger.h"

@implementation CMPOSLogger {
    os_log_t _log;
    NSMutableArray<CMPOSLoggerInterval *> *_intervalsPool;
    NSLock *_poolLock;
}

- (instancetype)initWithCategoryName:(NSString *)name {
    self = [super init];
    
    if (self) {
        _log = os_log_create("androidx.compose", [name cStringUsingEncoding:NSUTF8StringEncoding]);
        _intervalsPool = [NSMutableArray new];
        _poolLock = [NSLock new];
    }
    
    return self;
}

- (CMPOSLoggerInterval *)beginIntervalNamed:(NSString *)name {
    CMPOSLoggerInterval *interval;
    
    [_poolLock lock];
    
    if (_intervalsPool.count > 0) {
        interval = _intervalsPool.lastObject;
        [_intervalsPool removeLastObject];
    } else {
        interval = nil;
    }
    
    [_poolLock unlock];
    
    if (interval) {
        [interval beginWithName:name];
        return interval;
    } else {
        interval = [[CMPOSLoggerInterval alloc] initWithLog:_log];
        [interval beginWithName:name];
        return interval;
    }
}

- (void)endInterval:(CMPOSLoggerInterval *)interval {
    [interval end];
    [_poolLock lock];
    
    [_intervalsPool addObject:interval];
    
    [_poolLock unlock];
}


@end

static CMPOSLogger *_globalAppTraceLogger = nil;

void CMPOSInitializeAppTraceLogger(NSString *name) {
    _globalAppTraceLogger = [[CMPOSLogger alloc] initWithCategoryName:name];
}

CMPOSLogger * _Nullable CMPOSAppTraceLogger(void) {
    return _globalAppTraceLogger;
}
