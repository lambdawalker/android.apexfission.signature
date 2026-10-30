package io.apexfission.signature

import kotlin.test.*

class KeyPolicyTest {
    @Test fun acceptsBothPlatformPerUseConventionsAndRejectsTimedWindows() {
        assertTrue(isPerUseAuthenticationDuration(-1))
        assertTrue(isPerUseAuthenticationDuration(0))
        assertFalse(isPerUseAuthenticationDuration(1))
        assertFalse(isPerUseAuthenticationDuration(300))
        assertFalse(isPerUseAuthenticationDuration(-2))
    }
    @Test fun aliasesCannotEscapeNamespaceOrOverwriteUnrelatedKeys() {
        assertEquals("sworn-ink.v1.account-1", HardwareKeys.storageAlias("account-1"))
        for (alias in listOf("", "../account", "a/b", "a".repeat(81), "ñ")) {
            assertFailsWith<IllegalArgumentException> { HardwareKeys.storageAlias(alias) }
        }
    }
    @Test fun promptRequiresVisibleTitleAndCancel() {
        assertFailsWith<IllegalArgumentException> { PromptText(" ") }
        assertFailsWith<IllegalArgumentException> { PromptText("Sign", cancel="") }
        assertEquals("Cancel", PromptText("Sign").cancel)
    }
}
