#import "include/OSLogger.h"

#import <os/signpost.h>

@implementation OSLogger {
    os_log_t _log;
    os_signpost_id_t _signpostId;
}

- (void)beginSection:(NSString *)name {
    _log = os_log_create("androidx.tracing", [name cStringUsingEncoding:NSUTF8StringEncoding]);
    _signpostId = os_signpost_id_generate(_log);
    os_signpost_interval_begin(_log, _signpostId++, "AndroidX");
}

- (void)endSection {
    os_signpost_interval_end(_log, _signpostId--, "AndroidX");
}

@end
