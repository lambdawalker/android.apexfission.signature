# Sworn Ink Android — Development

No confirmed stable release exists. This guide covers the source tree, not a published dependency. Android signs exact server-bound statements; the Go service owns verification, replay prevention, JWT validation, evidence and receipts. JWT acquisition, email verification and document approval UX belong to the host.

| Task | Read |
| --- | --- |
| Install and connect a host | [Quickstart](quickstart.md) |
| Understand binding and hardware policy | [Concepts](concepts.md) |
| Use public Kotlin declarations | [API](api.md) |
| Register, sign, cancel, recover | [Recipes](recipes.md) |
| Diagnose failures or upgrade | [Troubleshooting](troubleshooting.md), [Migration](migration.md) |
| Validate on real hardware | [Testing](testing.md) |
| Understand trust boundaries | [Limitations](limitations.md) |

Critical invariants: use only the server-selected suite; use a fresh 32-byte nonce for registration; sign `Statement.encode()` with SHA256withECDSA; standard padded base64 for binary HTTP fields; issuer and subject bind ownership, never email. Preserve statement bytes and DER ECDSA signature bytes. Do not sign a document digest as though it were a protocol statement.

[Installation facts](../../IMPORT.md) · [Authoritative Go protocol](https://github.com/lambdawalker/go.apexfission.aws.signature/blob/feat/sworn-ink-signatures/docs/agents/protocol.md)
