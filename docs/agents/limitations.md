# Limitations — Development

Android API 31+ is required so exact KeyInfo security levels and biometric authentication policy can be checked. Capabilities cannot guarantee hardware resources, remote provisioning, trusted roots or server approval. Some devices expose StrongBox but cannot attest or create the selected key. Software keys and device credentials are rejected.

The server must validate the complete attestation, trusted operator-selected roots and fresh revocation status. Local chain export proves neither chain trust nor app identity. The enrollment invalidation flag is requested and checked locally; attestation does not explicitly prove that flag. The library does not claim biometric identity, legal enforceability, independent timestamps, JWT issuance, email verification, network replay defense or evidence retention.

No raw hash signing API is exposed. Sending a 32-byte document hash to SHA256withECDSA would hash that hash again and would not sign the Sworn Ink protocol statement. The production API accepts only typed protocol statements.

This headless library owns no composable or application screen. Screenshots/previews would not prove biometric key enforcement. Physical-device tests and the manual validation matrix are required. No live device, live server or AWS deployment result is implied by JVM tests or successful compilation.
