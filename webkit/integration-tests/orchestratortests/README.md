# Webkit Orchestrator Tests

This folder contains test targets that run using the AndroidX Test Orchestrator.

## How this differs from standard targets

Standard instrumentation tests in AndroidX run all tests in a module within the same instrumentation instance (and thus the same process). This means that state can leak between tests. For example, if one test initializes `WebView`, subsequent tests in the same run will see `WebView` as already initialized.

Orchestrator tests run each test in its own isolated instrumentation instance. This ensures:
- **No shared state**: Each test starts with a clean state.
- **Isolated crashes**: A crash in one test doesn't prevent others from running.

## When to add tests here

You should add tests to this target if:
1. The test requires `WebView` to **not** be loaded yet when the test starts (e.g., testing startup behavior or process-global configurations).
2. The test modifies global state that cannot be easily reset, causing interference with other tests.
3. The test is flaky due to state leakage from other tests.

For most other tests, use the standard test targets (e.g., in `webkit/integration-tests/instrumentation/`) to avoid the overhead of starting a new instrumentation instance for every test.
