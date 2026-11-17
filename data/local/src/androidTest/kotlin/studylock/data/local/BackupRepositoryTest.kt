package studylock.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class BackupRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: BackupRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("backup_repo_test_db")
        database = Room.databaseBuilder(context, AppDatabase::class.java, "backup_repo_test_db").build()
        repository = BackupRepository(context, database)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase("backup_repo_test_db")
    }

    @Test
    fun export_then_restore_preserves_academic_data() = runBlocking {
        val semesterId = UUID.randomUUID().toString()
        val moduleId = UUID.randomUUID().toString()
        val topicId = UUID.randomUUID().toString()
        database.semesterDao().insert(
            SemesterEntity(
                id = semesterId,
                academicYear = "2026",
                label = "Semester 1",
                startDate = null
            )
        )
        database.moduleDao().insert(
            ModuleEntity(id = moduleId, semesterId = semesterId, code = "CS101", name = "Computer Science")
        )
        database.topicDao().insert(
            TopicEntity(id = topicId, moduleId = moduleId, title = "Control Flow", lecturerSequence = 1)
        )

        val name = repository.exportBackup(1_000L)

        database.topicDao().delete(database.topicDao().getById(topicId)!!)
        database.moduleDao().delete(database.moduleDao().getById(moduleId)!!)
        database.semesterDao().delete(database.semesterDao().getById(semesterId)!!)
        assertEquals(0, database.semesterDao().getAll().size)

        repository.restoreBackup(name)

        assertEquals(semesterId, database.semesterDao().getAll().single().id)
        assertEquals(moduleId, database.moduleDao().getAll().single().id)
        assertEquals("Control Flow", database.topicDao().getById(topicId)?.title)
    }
}
