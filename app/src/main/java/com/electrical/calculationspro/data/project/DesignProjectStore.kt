package com.electrical.calculationspro.data.project

/**
 * ================================================================
 * PROFESSIONAL DESIGN
 * Project Store
 * ================================================================
 *
 * Application-level project state.
 *
 * This is intentionally independent from Compose.
 *
 * The store provides:
 * - project creation
 * - active project
 * - update
 * - delete
 * - project lookup
 *
 * Persistent database can replace the internal storage later
 * without changing the engineering models.
 * ================================================================
 */

class DesignProjectStore {

    private val projects =
        mutableListOf<DesignProject>()

    private var activeProjectId: String? = null

    /**
     * Create a new project.
     */
    fun create(
        projectName: String,
        projectNumber: String = "",
        clientName: String = "",
        consultantName: String = "",
        location: String = "",
        description: String = "",
        standard: com.electrical.calculationspro.data.Standard =
            com.electrical.calculationspro.data.Standard.IEC
    ): DesignProject {

        val project =
            DesignProject(
                projectName = projectName.trim(),
                projectNumber = projectNumber.trim(),
                clientName = clientName.trim(),
                consultantName = consultantName.trim(),
                location = location.trim(),
                description = description.trim(),
                standard = standard
            )

        projects.removeAll {
            it.id == project.id
        }

        projects.add(project)

        activeProjectId =
            project.id

        return project
    }

    /**
     * Save or update an existing project.
     */
    fun save(
        project: DesignProject
    ): DesignProject {

        val updated =
            project.updateTimestamp()

        val index =
            projects.indexOfFirst {
                it.id == updated.id
            }

        if (index >= 0) {
            projects[index] = updated
        } else {
            projects.add(updated)
        }

        activeProjectId =
            updated.id

        return updated
    }

    /**
     * Get active project.
     */
    fun getActive(): DesignProject? {

        val id =
            activeProjectId
                ?: return null

        return projects.firstOrNull {
            it.id == id
        }
    }

    /**
     * Set active project.
     */
    fun setActive(
        projectId: String
    ): Boolean {

        val exists =
            projects.any {
                it.id == projectId
            }

        if (!exists) {
            return false
        }

        activeProjectId =
            projectId

        return true
    }

    /**
     * Get project by ID.
     */
    fun getById(
        projectId: String
    ): DesignProject? =
        projects.firstOrNull {
            it.id == projectId
        }

    /**
     * Get all projects.
     */
    fun getAll(): List<DesignProject> =
        projects
            .sortedByDescending {
                it.updatedAtMillis
            }
            .toList()

    /**
     * Delete a project.
     */
    fun delete(
        projectId: String
    ): Boolean {

        val removed =
            projects.removeAll {
                it.id == projectId
            }

        if (activeProjectId == projectId) {
            activeProjectId = null
        }

        return removed
    }

    /**
     * Clear all projects.
     */
    fun clear() {

        projects.clear()

        activeProjectId = null
    }

    /**
     * Number of projects.
     */
    fun size(): Int =
        projects.size

    /**
     * Whether there is an active project.
     */
    fun hasActiveProject(): Boolean =
        getActive() != null
}

/**
 * Shared application-level project store.
 *
 * The UI can use this object without owning engineering logic.
 */
object DesignProjects {

    private val store =
        DesignProjectStore()

    fun create(
        projectName: String,
        projectNumber: String = "",
        clientName: String = "",
        consultantName: String = "",
        location: String = "",
        description: String = "",
        standard: com.electrical.calculationspro.data.Standard =
            com.electrical.calculationspro.data.Standard.IEC
    ): DesignProject =
        store.create(
            projectName = projectName,
            projectNumber = projectNumber,
            clientName = clientName,
            consultantName = consultantName,
            location = location,
            description = description,
            standard = standard
        )

    fun save(
        project: DesignProject
    ): DesignProject =
        store.save(project)

    fun getActive(): DesignProject? =
        store.getActive()

    fun setActive(
        projectId: String
    ): Boolean =
        store.setActive(projectId)

    fun getById(
        projectId: String
    ): DesignProject? =
        store.getById(projectId)

    fun getAll(): List<DesignProject> =
        store.getAll()

    fun delete(
        projectId: String
    ): Boolean =
        store.delete(projectId)

    fun clear() =
        store.clear()

    fun size(): Int =
        store.size()

    fun hasActiveProject(): Boolean =
        store.hasActiveProject()
}
