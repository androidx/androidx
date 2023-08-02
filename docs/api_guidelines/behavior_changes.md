## Behavior changes

### Changes that affect API documentation

Do not make behavior changes that require altering API documentation in a way
that would break existing clients, even if such changes are technically binary
compatible. For example, changing the meaning of a method's return value to
return true rather than false in a given state would be considered a breaking
change. Because this change is binary-compatible, it will not be caught by
tooling and is effectively invisible to clients.

Instead, add new methods and deprecate the existing ones if necessary, noting
behavior changes in the deprecation message.

### High-risk behavior changes

Behavior changes that conform to documented API contracts but are highly complex
and difficult to comprehensively test are considered high-risk and should be
implemented using behavior flags. These changes may be flagged on initially, but
the original behaviors must be preserved until the library enters release
candidate stage and the behavior changes have been appropriately verified by
integration testing against public pre-release
revisions.

It may be necessary to soft-revert a high-risk behavior change with only 24-hour
notice, which should be achievable by flipping the behavior flag to off.

```java
// Flag for whether to throw exceptions when the state is known to be bad. This
// is expected to be a high-risk change since apps may be working fine even with
// a bad state, so we may need to disable this as a hotfix.
private static final boolean FLAG_EXCEPTION_ON_BAD_STATE = false;
```

```java
/**
 * Allows a developer to toggle throwing exceptions when the state is known to
 * be bad. This method is intended to give developers time to update their code.
 * It is temporary and will be removed in a future release.
 */
@TemporaryFeatureFlag
public void setExceptionOnBadStateEnabled(boolean enabled);
```

Avoid adding multiple high-risk changes during a feature cycle, as verifying the
interaction of multiple feature flags leads to unnecessary complexity and
exposes clients to high risk even when a single change is flagged off. Instead,
wait until one high-risk change has landed in RC before moving on to the next.

#### Testing

Relevant tests should be run for the behavior change in both the on and off
flagged states to prevent regressions.
