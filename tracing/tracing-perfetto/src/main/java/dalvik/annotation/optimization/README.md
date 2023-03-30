# Rationale

`FastNative` and `CriticalNative` annotations are duplicated from the platform, so that the platform
detects their presence, and applies them, even though they're not part of platform public API.

# Next steps

Once the annotations become public in the platform, remove duplicate files and use the platform.
Tracking item: b/35664282
