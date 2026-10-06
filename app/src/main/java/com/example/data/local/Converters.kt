package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Department
import com.example.data.model.EmployeeRole
import com.example.data.model.EmployeeStatus
import com.example.data.model.ProjectPriority
import com.example.data.model.ProjectStatus
import org.json.JSONArray
import java.util.Date

/**
 * Room TypeConverters for mapping complex and custom types into SQLite-compatible types.
 */
class Converters {

    // --- Date Converters ---
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // --- List<String> Converters (JSON Array serialization) ---
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        if (data == null) return null
        val array = JSONArray(data)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    // --- List<Long> Converters (JSON Array serialization) ---
    @TypeConverter
    fun fromLongList(list: List<Long>?): String? {
        if (list == null) return null
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toLongList(data: String?): List<Long>? {
        if (data == null) return null
        val array = JSONArray(data)
        val list = mutableListOf<Long>()
        for (i in 0 until array.length()) {
            list.add(array.getLong(i))
        }
        return list
    }

    // --- Enum Converters ---
    @TypeConverter
    fun fromDepartment(department: Department?): String? = department?.name

    @TypeConverter
    fun toDepartment(value: String?): Department? =
        value?.let { runCatching { Department.valueOf(it) }.getOrDefault(Department.ENGINEERING) }

    @TypeConverter
    fun fromEmployeeRole(role: EmployeeRole?): String? = role?.name

    @TypeConverter
    fun toEmployeeRole(value: String?): EmployeeRole? =
        value?.let { runCatching { EmployeeRole.valueOf(it) }.getOrDefault(EmployeeRole.DEVELOPER) }

    @TypeConverter
    fun fromEmployeeStatus(status: EmployeeStatus?): String? = status?.name

    @TypeConverter
    fun toEmployeeStatus(value: String?): EmployeeStatus? =
        value?.let { runCatching { EmployeeStatus.valueOf(it) }.getOrDefault(EmployeeStatus.ACTIVE) }

    @TypeConverter
    fun fromPresenceStatus(status: com.example.data.model.PresenceStatus?): String? = status?.name

    @TypeConverter
    fun toPresenceStatus(value: String?): com.example.data.model.PresenceStatus? =
        value?.let { runCatching { com.example.data.model.PresenceStatus.valueOf(it) }.getOrDefault(com.example.data.model.PresenceStatus.ONLINE) }

    @TypeConverter
    fun fromProjectPriority(priority: ProjectPriority?): String? = priority?.name

    @TypeConverter
    fun toProjectPriority(value: String?): ProjectPriority? =
        value?.let { runCatching { ProjectPriority.valueOf(it) }.getOrDefault(ProjectPriority.MEDIUM) }

    @TypeConverter
    fun fromProjectStatus(status: ProjectStatus?): String? = status?.name

    @TypeConverter
    fun toProjectStatus(value: String?): ProjectStatus? =
        value?.let { runCatching { ProjectStatus.valueOf(it) }.getOrDefault(ProjectStatus.ACTIVE) }
}
