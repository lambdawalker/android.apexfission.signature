# Troubleshooting — Development

| Failure | Action |
| --- | --- |
| Empty capabilities | Enroll a supported strong biometric and configure secure lock screen; weak face unlock may not qualify |
| StrongBox unavailable/exhausted | Surface the error and request a new server policy decision; do not retry the selected challenge with TEE |
| Hardware suite/auth policy mismatch | Reject/delete the newly created key; inspect device implementation and server policy |
| `InvalidatedKeyException` | Request fresh registration and nonce after the host confirms recovery |
| User cancel or negative button | Keep key; stop request; let user choose a new signing attempt |
| Lockout | Surface preserved biometric error code; wait for OS recovery; never use device credential as fallback |
| Lifecycle `CancellationException` | Recreate/reuse current activity signer as appropriate, obtain valid challenge and explicitly retry |
| Statement mismatch | Stop before prompting; check exact issuer/subject, document bytes, nonce encoding, suite and keyId |
| Server rejects attestation | Check configured roots/revocation/app signer/package/boot state with service operators; do not weaken client policy |
| Expired challenge | Obtain new challenge; registration needs a new attested key for the new nonce |

Never log JWTs, document content, biometric information or private key objects. Certificate chains, account identifiers and key fingerprints can also be identifying; apply the host's retention policy. The library does not install a logger.
