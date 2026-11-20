package studylock.academic.engine

import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class RedesignPlannerTest {

    private val semesterId = UUID.randomUUID()
    private val moduleId = UUID.randomUUID()
    private val start = LocalDate.of(2026, 1, 12) // Monday
    private val end = LocalDate.of(2026, 2, 1)   // three weeks
    private val availability = (1..5).map {
        StudyAvailability(
            id = UUID.randomUUID(),
            semesterId = semesterId,
            dayOfWeek = it,
            available = true,
            commitmentMinutes = 120
        )
    }

    private fun topics(count: Int) = (1..count).map {
        Topic(
            id = UUID.randomUUID(),
            moduleId = moduleId,
            title = "Topic $it",
            lecturerSequence = it,
            durationEstimateMinutes = 60
        )
    }

    private fun propose(count: Int) = RedesignPlanner.propose(
        startDate = start,
        endDate = end,
        availability = availability,
        remainingTopics = topics(count)
    )

    @Test
    fun `feasible workload needs no redesign`() {
        val proposal = propose(8)
        assertFalse(proposal.needed)
        assertEquals(RedesignOption.NONE, proposal.recommendedOption)
        assertTrue(proposal.goalAchievable)
        assertEquals(0, proposal.shortfallSessions)
    }

    @Test
    fun `small shortfall recommends compression first`() {
        val proposal = propose(12)
        assertTrue(proposal.needed)
        assertEquals(RedesignOption.COMPRESS, proposal.recommendedOption)
        assertTrue(proposal.goalAchievable)
    }

    @Test
    fun `heavy shortfall recommends extra weekday block`() {
        val proposal = propose(30)
        assertEquals(RedesignOption.EXTRA_WEEKDAY, proposal.recommendedOption)
        assertTrue(proposal.goalAchievable)
    }

    @Test
    fun `infeasible workload is reported honestly`() {
        val proposal = propose(50)
        assertTrue(proposal.needed)
        assertFalse(proposal.goalAchievable)
        assertEquals(RedesignOption.NONE, proposal.recommendedOption)
        assertTrue(proposal.shortfallSessions > 0)
        assertTrue(proposal.summary.contains("cannot be claimed as met"))
    }

    @Test
    fun `weekends are never proposed as a normal option`() {
        val proposal = propose(12)
        assertFalse(proposal.summary.contains("weekend", ignoreCase = true))
    }
}
