package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceDao
import com.example.data.local.Converters
import com.example.data.local.EmployeeDao
import com.example.data.local.ProjectDao
import com.example.data.local.UserProfileDao
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseSchemaTest {

    private lateinit var db: AppDatabase
    private lateinit var employeeDao: EmployeeDao
    private lateinit var projectDao: ProjectDao
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var attendanceDao: AttendanceDao
    private val converters = Converters()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        employeeDao = db.employeeDao()
        projectDao = db.projectDao()
        userProfileDao = db.userProfileDao()
        attendanceDao = db.attendanceDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testTypeConverters_Date() {
        val now = Date(1726588800000L)
        val timestamp = converters.dateToTimestamp(now)
        assertEquals(1726588800000L, timestamp)

        val reconstructed = converters.fromTimestamp(timestamp)
        assertEquals(now, reconstructed)

        assertNull(converters.dateToTimestamp(null))
        assertNull(converters.fromTimestamp(null))
    }

    @Test
    fun testTypeConverters_StringList() {
        val original = listOf("Kotlin", "Jetpack Compose", "Room Database")
        val json = converters.fromStringList(original)
        assertNotNull(json)

        val deserialized = converters.toStringList(json)
        assertEquals(original, deserialized)

        assertNull(converters.fromStringList(null))
        assertNull(converters.toStringList(null))
    }

    @Test
    fun testTypeConverters_LongList() {
        val original = listOf(101L, 202L, 303L)
        val json = converters.fromLongList(original)
        assertNotNull(json)

        val deserialized = converters.toLongList(json)
        assertEquals(original, deserialized)

        assertNull(converters.fromLongList(null))
        assertNull(converters.toLongList(null))
    }

    @Test
    fun testTypeConverters_Enums() {
        // Department
        assertEquals("ENGINEERING", converters.fromDepartment(Department.ENGINEERING))
        assertEquals(Department.ENGINEERING, converters.toDepartment("ENGINEERING"))
        assertEquals(Department.ENGINEERING, converters.toDepartment("UNKNOWN_VAL")) // fallback

        // EmployeeRole
        assertEquals("MANAGER", converters.fromEmployeeRole(EmployeeRole.MANAGER))
        assertEquals(EmployeeRole.MANAGER, converters.toEmployeeRole("MANAGER"))

        // EmployeeStatus
        assertEquals("ON_LEAVE", converters.fromEmployeeStatus(EmployeeStatus.ON_LEAVE))
        assertEquals(EmployeeStatus.ON_LEAVE, converters.toEmployeeStatus("ON_LEAVE"))

        // ProjectStatus
        assertEquals("COMPLETED", converters.fromProjectStatus(ProjectStatus.COMPLETED))
        assertEquals(ProjectStatus.COMPLETED, converters.toProjectStatus("COMPLETED"))

        // ProjectPriority
        assertEquals("CRITICAL", converters.fromProjectPriority(ProjectPriority.CRITICAL))
        assertEquals(ProjectPriority.CRITICAL, converters.toProjectPriority("CRITICAL"))
    }

    @Test
    fun testEmployeeDao_InsertAndRetrieve() = runBlocking {
        val employee = EmployeeEntity(
            name = "Amit Verma",
            email = "amit.verma@example.com",
            phone = "+91 91234 56789",
            designation = "Lead Mobile Architect",
            department = Department.ENGINEERING,
            role = EmployeeRole.TEAM_LEAD,
            status = EmployeeStatus.ACTIVE,
            joiningDate = Date(1672531200000L),
            skills = listOf("Android", "Kotlin", "KSP", "Coroutines"),
            assignedProjectIds = listOf(10L, 20L),
            salary = 120000.0,
            emergencyContact = "+91 90000 11111"
        )

        val id = employeeDao.insert(employee)
        assertTrue(id > 0)

        val fetched = employeeDao.getEmployeeById(id).first()
        assertNotNull(fetched)
        assertEquals("Amit Verma", fetched?.name)
        assertEquals(Department.ENGINEERING, fetched?.department)
        assertEquals(EmployeeRole.TEAM_LEAD, fetched?.role)
        assertEquals(EmployeeStatus.ACTIVE, fetched?.status)
        assertEquals(listOf("Android", "Kotlin", "KSP", "Coroutines"), fetched?.skills)
        assertEquals(listOf(10L, 20L), fetched?.assignedProjectIds)
        assertEquals(120000.0, fetched?.salary ?: 0.0, 0.001)

        val count = employeeDao.getEmployeeCount().first()
        assertEquals(1, count)
    }

    @Test
    fun testEmployeeDao_FilteringAndDeletion() = runBlocking {
        val emp1 = EmployeeEntity(
            name = "Dev One",
            email = "dev1@example.com",
            phone = "1111",
            designation = "Android Dev",
            department = Department.ENGINEERING,
            status = EmployeeStatus.ACTIVE
        )
        val emp2 = EmployeeEntity(
            name = "Designer One",
            email = "des1@example.com",
            phone = "2222",
            designation = "UI Designer",
            department = Department.DESIGN,
            status = EmployeeStatus.ON_LEAVE
        )
        employeeDao.insertAll(listOf(emp1, emp2))

        val engList = employeeDao.getEmployeesByDepartment(Department.ENGINEERING).first()
        assertEquals(1, engList.size)
        assertEquals("Dev One", engList[0].name)

        val leaveList = employeeDao.getEmployeesByStatus(EmployeeStatus.ON_LEAVE).first()
        assertEquals(1, leaveList.size)
        assertEquals("Designer One", leaveList[0].name)

        // Delete test
        employeeDao.delete(engList[0])
        val afterDeleteCount = employeeDao.getEmployeeCount().first()
        assertEquals(1, afterDeleteCount)
    }

    @Test
    fun testProjectDao_InsertAndRetrieveWithTags() = runBlocking {
        val project = ProjectEntity(
            name = "Enterprise Cloud Portal",
            clientName = "Acme Corp",
            totalTasks = 20,
            completedTasks = 15,
            progressPercent = 75,
            status = "Active",
            priority = "High",
            tags = listOf("Cloud", "Kotlin", "Room", "Compose"),
            assignedEmployeeIds = listOf(1L, 2L, 5L),
            budget = 450000.0
        )

        val id = projectDao.insert(project)
        assertTrue(id > 0)

        val fetched = projectDao.getProjectById(id).first()
        assertNotNull(fetched)
        assertEquals("Enterprise Cloud Portal", fetched?.name)
        assertEquals("Acme Corp", fetched?.clientName)
        assertEquals(listOf("Cloud", "Kotlin", "Room", "Compose"), fetched?.tags)
        assertEquals(listOf(1L, 2L, 5L), fetched?.assignedEmployeeIds)
        assertEquals(450000.0, fetched?.budget ?: 0.0, 0.001)

        val activeProjects = projectDao.getProjectsByStatus("Active").first()
        assertEquals(1, activeProjects.size)

        val count = projectDao.getProjectCount().first()
        assertEquals(1, count)

        // Delete test
        projectDao.deleteById(id)
        val afterCount = projectDao.getProjectCount().first()
        assertEquals(0, afterCount)
    }

    @Test
    fun testUserProfileDao_InsertAndRetrieve() = runBlocking {
        // Initially empty
        val initial = userProfileDao.getUserProfile().first()
        assertNull(initial)

        val profile = UserProfileEntity(
            id = 1L,
            name = "Amit Verma",
            role = "Lead Mobile Architect",
            isOnboarded = true
        )
        userProfileDao.insertOrUpdateProfile(profile)

        val retrieved = userProfileDao.getUserProfile().first()
        assertNotNull(retrieved)
        assertEquals("Amit Verma", retrieved?.name)
        assertEquals("Lead Mobile Architect", retrieved?.role)
        assertTrue(retrieved?.isOnboarded == true)

        val onboardedCount = userProfileDao.isOnboarded()
        assertEquals(1, onboardedCount)
    }

    @Test
    fun testUserProfileDao_UpdateProfile() = runBlocking {
        val initialProfile = UserProfileEntity(
            id = 1L,
            name = "Rahul Sharma",
            role = "Senior Developer",
            isOnboarded = true
        )
        userProfileDao.insertOrUpdateProfile(initialProfile)

        // Update name and role
        userProfileDao.updateNameAndRole("Vikram Malhotra", "Product Manager")

        val updated = userProfileDao.getUserProfile().first()
        assertNotNull(updated)
        assertEquals("Vikram Malhotra", updated?.name)
        assertEquals("Product Manager", updated?.role)
    }

    @Test
    fun testAttendanceDao_CheckInRecordsTimestamp() = runBlocking {
        val beforeTimestamp = System.currentTimeMillis()
        val attendanceRecord = AttendanceRecord(
            date = "2026-09-18",
            checkInTime = "09:30:15 AM",
            checkOutTime = null,
            durationMinutes = 0,
            isWorking = true,
            status = "Present",
            overtimeMinutes = 0,
            timestamp = beforeTimestamp,
            employeeName = "Amit Verma"
        )
        val id = attendanceDao.insert(attendanceRecord)
        assertTrue(id > 0)

        val latest = attendanceDao.getLatestAttendance().first()
        assertNotNull(latest)
        assertEquals("2026-09-18", latest?.date)
        assertEquals("09:30:15 AM", latest?.checkInTime)
        assertEquals(beforeTimestamp, latest?.timestamp)
        assertEquals("Amit Verma", latest?.employeeName)
        assertTrue(latest?.isWorking == true)

        val allList = attendanceDao.getAllAttendance().first()
        assertEquals(1, allList.size)
        assertEquals(beforeTimestamp, allList[0].timestamp)
    }
}

