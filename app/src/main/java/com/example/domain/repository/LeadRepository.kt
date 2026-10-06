package com.example.domain.repository

import com.example.domain.model.Lead
import com.example.domain.model.LeadPriority
import com.example.domain.model.LeadSource
import com.example.domain.model.LeadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface defining CRUD and query operations for CRM Leads.
 */
interface LeadRepository {
    /**
     * Observe all leads as a reactive Flow.
     */
    fun getAllLeads(): Flow<List<Lead>>

    /**
     * Retrieve a specific lead by unique ID.
     */
    fun getLeadById(id: Long): Flow<Lead?>

    /**
     * Filter leads by status.
     */
    fun getLeadsByStatus(status: LeadStatus): Flow<List<Lead>>

    /**
     * Filter leads by priority.
     */
    fun getLeadsByPriority(priority: LeadPriority): Flow<List<Lead>>

    /**
     * Filter leads by acquisition source.
     */
    fun getLeadsBySource(source: LeadSource): Flow<List<Lead>>

    /**
     * Retrieve total count of active leads.
     */
    suspend fun getLeadCount(): Int

    /**
     * Insert a new lead into persistence.
     */
    suspend fun insertLead(lead: Lead): Long

    /**
     * Insert a batch of leads into persistence.
     */
    suspend fun insertLeads(leads: List<Lead>)

    /**
     * Update an existing lead.
     */
    suspend fun updateLead(lead: Lead)

    /**
     * Delete a lead.
     */
    suspend fun deleteLead(lead: Lead)

    /**
     * Delete a lead by its ID.
     */
    suspend fun deleteLeadById(id: Long)

    /**
     * Clear all lead records.
     */
    suspend fun clearAllLeads()
}
