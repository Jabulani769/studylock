package studylock.focus

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnforcementTest {

    private fun snapshot(
        usage: Boolean = true,
        dnd: Boolean = true,
        quiet: Boolean = true,
        session: Boolean = true,
        interrupted: Boolean = false
    ) = EnforcementSnapshot(usage, dnd, quiet, session, interrupted)

    @Test
    fun `full supported requires both permissions and active quiet mode`() {
        assertEquals(EnforcementState.FULL_SUPPORTED, EnforcementResolver.resolve(snapshot()))
    }

    @Test
    fun `granted permissions without active quiet mode is limited`() {
        assertEquals(EnforcementState.LIMITED, EnforcementResolver.resolve(snapshot(quiet = false)))
    }

    @Test
    fun `monitoring without notification control is monitoring only`() {
        assertEquals(EnforcementState.MONITORING_ONLY, EnforcementResolver.resolve(snapshot(dnd = false, quiet = false)))
    }

    @Test
    fun `missing permissions during a session requires setup`() {
        assertEquals(
            EnforcementState.PERMISSION_REQUIRED,
            EnforcementResolver.resolve(snapshot(usage = false, dnd = false, quiet = false))
        )
    }

    @Test
    fun `revoked control overrides every other signal`() {
        assertEquals(
            EnforcementState.ENFORCEMENT_INTERRUPTED,
            EnforcementResolver.resolve(snapshot(interrupted = true))
        )
    }

    @Test
    fun `descriptions never claim universal blocking`() {
        EnforcementState.entries.forEach { state ->
            val text = EnforcementResolver.describe(state)
            assertTrue(text.isNotBlank())
            assertTrue(!text.contains("100%", ignoreCase = true))
            assertTrue(!text.contains("unbreakable", ignoreCase = true))
        }
    }
}
