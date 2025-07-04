#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface OSLogger : NSObject

- (void)beginSection:(NSString *)name;
- (void)endSection;

@end

NS_ASSUME_NONNULL_END
