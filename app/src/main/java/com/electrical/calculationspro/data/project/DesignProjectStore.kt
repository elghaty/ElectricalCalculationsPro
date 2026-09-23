package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.Standard

class DesignProjectStore {

    private val projects =
        LinkedHashMap<String, DesignProject>()

    private var activeProjectId: String? = null

    @Synchronized
    fun create(
        projectName: String,
        projectNumber: String = "",
        clientName: String = "",
        consultantName: String = "",
        location: String = "",
        description: String = "",
        electricalStandard: Standard? = Standard.IEC
    ): DesignProject {

        val project =
            DesignProject(
                projectName = projectName.trim(),
                projectNumber = projectNumber.trim(),
                clientName = clientName.trim(),
                consultantName = consultantName.trim(),
                location = location.trim(),
                description = description.trim(),
                electricalStandard = electricalStandard
            )

        projects[project.id] = project
        activeProjectId = project.id

        return project
    }

    @Synchronized
    fun save(
        project: DesignProject
    ): DesignProject {

        val updated =
            project.updateTimestamp()

        projects[updated.id] = updated
        activeProjectId = updated.id

        return updated
    }

    @Synchronized
    fun getActive(): DesignProject? =
        activeProjectId?.let {
            projects[it]
        }

    @Synchronized
    fun getById(
        projectId: String
    ): DesignProject? =
        projects[projectId]

    @Synchronized
    fun getAll(): List<DesignProject> =
        projects.values
            .sortedByDescending {
                it.updatedAtMillis
            }
            .toList()

    @Synchronized
    fun setActive(
        projectId: String
    ): Boolean {

        if (!projects.containsKey(projectId)) {
            return false
        }

        activeProjectId = projectId
        return true
    }

    @Synchronized
    fun delete(
        projectId: String
    ): Boolean {

        val removed =
            projects.remove(projectId) != null

        if (activeProjectId == projectId) {
            activeProjectId =
                projects.values
                    .maxByOrNull {
                        it.updatedAtMillis
                    }
                    ?.id
        }

        return removed
    }

    @Synchronized
    fun clear() {
        projects.clear()
        activeProjectId = null
    }

    @Synchronized
    fun size(): Int =
        projects.size

    @Synchronized
    fun hasActiveProject(): Boolean =
        getActive() != null
}

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
        electricalStandard: Standard? = Standard.IEC
    ): DesignProject =
        store.create(
            projectName = projectName,
            projectNumber = projectNumber,
            clientName = clientName,
            consultantName = consultantName,
            location = location,
            description = description,
            electricalStandard = electricalStandard
        )

    fun save(
        project: DesignProject
    ): DesignProject =
        store.save(project)

    fun getActive(): DesignProject? =
        store.getActive()

    fun getById(
        projectId: String
    ): DesignProject? =
        store.getById(projectId)

    fun getAll(): List<DesignProject> =
        store.getAll()

    fun setActive(
        projectId: String
    ): Boolean =
        store.setActive(projectId)

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
