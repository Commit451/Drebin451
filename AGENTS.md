# AGENTS.md

## Local verification policy

- Verify application changes locally using Android-targeted tests, builds, and lint only.
- Do not compile or run the JavaScript, Wasm, browser-test, or web-distribution targets locally; these builds are prohibitively slow and memory-intensive on the development machine.
- Rely on CI to validate all web targets.
- Only run web validation locally when the user explicitly requests it or when diagnosing a web-specific failure.
