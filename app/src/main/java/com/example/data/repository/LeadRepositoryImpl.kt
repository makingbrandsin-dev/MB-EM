package com.example.data.repository

import com.example.data.local.LeadDao
import com.example.data.model.LeadEntity
import com.example.domain.model.Lead
import com.example.domain.model.LeadPriority
import com.example.domain.model.LeadSource
import com.example.domain.model.LeadStatus
import com.example.domain.repository.LeadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Data layer implementation of [LeadRepository] backed by Room [LeadDao].
 */
class LeadRepositoryImpl(
    private val leadDao: LeadDao
) : LeadRepository {

    override fun getAllLeads(): Flow<List<Lead>> =
        leadDao.getAllLeads().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getLeadById(id: Long): Flow<Lead?> =
        leadDao.getLeadById(id).map { it?.toDomain() }

    override fun getLeadsByStatus(status: LeadStatus): Flow<List<Lead>> =
        leadDao.getAllLeads().map { entities ->
            entities.filter { it.stage.equals(status.displayName, ignoreCase = true) || it.stage.equals(status.name, ignoreCase = true) }
                .map { it.toDomain() }
        }

    override fun getLeadsByPriority(priority: LeadPriority): Flow<List<Lead>> =
        leadDao.getAllLeads().map { entities ->
            entities.map { it.toDomain() }
                .filter { it.priority == priority }
        }

    override fun getLeadsBySource(source: LeadSource): Flow<List<Lead>> =
        leadDao.getLeadsBySource(source.displayName).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getLeadCount(): Int =
        leadDao.getLeadCount()

    override suspend fun insertLead(lead: Lead): Long =
        leadDao.insert(lead.toEntity())

    override suspend fun insertLeads(leads: List<Lead>) =
        leadDao.insertAll(leads.map { it.toEntity() })

    override suspend fun updateLead(lead: Lead) =
        leadDao.update(lead.toEntity())

    override suspend fun deleteLead(lead: Lead) =
        leadDao.delete(lead.toEntity())

    override suspend fun deleteLeadById(id: Long) {
        leadDao.deleteById(id)
    }

    override suspend fun clearAllLeads() =
        leadDao.clearAll()

    // --- Data Mapper Extensions ---

    private fun LeadEntity.toDomain(): Lead {
        val parsedStatus = LeadStatus.fromString(stage)
        val parsedSource = LeadSource.fromString(source)
        val parsedPriority = when {
            leadScore >= 85 -> LeadPriority.HIGH
            leadScore >= 60 -> LeadPriority.MEDIUM
            else -> LeadPriority.LOW
        }
        val cleanValue = potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 50000.0

        return Lead(
            id = id,
            name = name,
            company = company,
            phone = phone,
            email = email,
            status = parsedStatus,
            priority = parsedPriority,
            source = parsedSource,
            leadScore = leadScore,
            requirement = requirement,
            potentialValue = cleanValue,
            potentialValueFormatted = potentialValue.ifBlank { "₹ ${String.format("%,.0f", cleanValue)}" },
            assignedTo = assignedTo,
            nextFollowUp = nextFollowUp,
            notes = notes,
            createdAt = createdAt
        )
    }

    private fun Lead.toEntity(): LeadEntity =
        LeadEntity(
            id = id,
            name = name,
            company = company,
            phone = phone,
            email = email,
            leadScore = leadScore,
            requirement = requirement,
            potentialValue = potentialValueFormatted.ifBlank { "₹ ${String.format("%,.0f", potentialValue)}" },
            stage = status.displayName,
            assignedTo = assignedTo,
            nextFollowUp = nextFollowUp,
            notes = notes,
            source = source.displayName,
            createdAt = createdAt
        )
}
