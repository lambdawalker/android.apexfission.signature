# Validation — Development

Host tests compare UTF-8 length-prefix bytes with the shared JSON vector (`protocol/test-vectors/v1.json`), reject malformed statements and test fingerprint/base64 behavior. Android instrumentation tests inspect generated hardware security levels, biometric-per-use policy, enrollment invalidation request, duplicate aliases, chain export and explicit StrongBox rejection.

```sh
./gradlew :protocol:test :signature:testDebugUnitTest :signature:lintDebug :signature:assembleDebug :signature:assembleDebugAndroidTest :examples:integration:assembleDebug
# Physical API31+ device, enrolled strong biometric, secure lock screen:
./gradlew :signature:connectedDebugAndroidTest
python3 scripts/docs.py verify
```

Instrumentation can skip biometric-dependent tests when prerequisites are missing. A skip is not evidence of enforcement. Emulators cannot establish hardware/attestation trust. Do not automate successful biometric authentication in production code or add an auth bypass for tests.

| Physical device scenario | Required result |
| --- | --- |
| Fresh StrongBox registration, then TEE registration where supported | Exact selected level, server-trusted chain, server-verified proof of possession |
| Valid biometric for each of two signing requests | Two prompts, distinct signatures verify for exact statements |
| Wrong biometric, negative button, OS back, lockout | No signature on terminal failure; wrong nonterminal match may continue prompt |
| Explicit cancel, Home, rotation, destroy | Exactly one failure callback; no late success/network submission |
| Enroll a new biometric after key creation | Prior key unusable; fresh challenged registration required |
| Remove/reset secure lock screen | Prior key unusable; no credential/software fallback |
| Forged/mutated server statement or changed document | Host stops before prompt or server rejects |
| Unsupported StrongBox or exhausted resources | Explicit failure; no TEE key generated for selected StrongBox challenge |
| Process death and reopen | Reopen account-bound alias with expected suite; never auto-resume signing |

Record device model, OS/security patch, app signing identity, suite, server policy/root version, scenario, pass/fail and redacted error codes in a reviewed test report. No physical validation report is included until those scenarios are actually run. No screenshot suite exists because there is no library-owned UI; OS prompt behavior is part of this device matrix.
