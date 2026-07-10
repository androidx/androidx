# Compose Inspector

## Change Descriptions

### Provide a quick return from GetComposablesCommand if no changes.

#### Added:
- `allow_empty_if_unchanged` to `GetComposablesCommand`
- `unchanged` to `GetComposablesResponse`

#### version: compose.ui:ui:1.12.0-alpha02

#### Backwards compatibility:
An older client will always send `false` for `allow_empty_if_unchanged`.
This makes the inspector return the full `GetComposabledResponse`.

#### Forwards compatibility:
A client expecting this functionality can send `true` for `allow_empty_if_unchanged`,
but an older inspector will never read this and always return `false` (the default value)
for `unchanged` and a full `GetComposablesResponse`.

### Detect parameter changes for each recomposition

#### Added:
- `include_parameter_changes` to `StateReadSettings.All` and `StateReadSettings.ById`.
- `parameter_changes` to `StateReadGroup`.

#### version: compose.ui:ui:1.12.0-alpha02

#### Backwards compatibility:
`include_parameter_changes` will be false since an older client doesn't know about it.
The inspector will not generate `parameter_changes` for the `StateReadGroup`.

#### Forwards compatibility:
An older inspector will ignore the `include_parameter_changes` setting.
A client expecting this functionality will simply get an
empty list of `parameter_changes` from an older inspector.
