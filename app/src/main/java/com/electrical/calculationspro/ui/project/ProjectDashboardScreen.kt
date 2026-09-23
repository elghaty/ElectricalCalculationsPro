package com.electrical.calculationspro.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.DesignStatus
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge

@Composable
fun ProjectDashboardScreen(
    language: AppLanguage,
    onOpenProject: (DesignProject) -> Unit
) {
    var projects by remember {
        mutableStateOf(DesignProjects.getAll())
    }

    var showNewProject by remember {
        mutableStateOf(false)
    }

    val arabic = language == AppLanguage.ARABIC

    if (showNewProject) {
        NewProjectScreen(
            language = language,
            onCancel = {
                showNewProject = false
            },
            onCreated = { project ->
                projects = DesignProjects.getAll()
                showNewProject = false
                onOpenProject(project)
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (arabic)
                            "المشروعات الهندسية"
                        else
                            "Engineering Projects",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text =
                        if (arabic)
                            "ابدأ التصميم من المشروع وليس من حاسبة منفصلة"
                        else
                            "Start from a project instead of an isolated calculator",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    showNewProject = true
                }
            ) {

                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )

                Text(
                    text =
                        if (arabic)
                            "مشروع جديد"
                        else
                            "New Project"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        if (projects.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            if (arabic)
                                "لا توجد مشروعات"
                            else
                                "No projects"
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Button(
                        onClick = {
                            showNewProject = true
                        }
                    ) {

                        Text(
                            text =
                                if (arabic)
                                    "إنشاء مشروع"
                                else
                                    "Create Project"
                        )
                    }
                }
            }

        } else {

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = projects,
                    key = {
                        it.id
                    }
                ) { project ->

                    ProjectCard(
                        project = project,
                        language = language,
                        onOpen = {
                            onOpenProject(project)
                        },
                        onDelete = {

                            DesignProjects.delete(
                                project.id
                            )

                            projects =
                                DesignProjects.getAll()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: DesignProject,
    language: AppLanguage,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Description,
                contentDescription = null
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
            ) {

                Text(
                    text =
                        project.projectName.ifBlank {
                            if (arabic)
                                "مشروع بدون اسم"
                            else
                                "Unnamed Project"
                        },
                    style =
                        MaterialTheme.typography.titleLarge
                )

                if (project.projectNumber.isNotBlank()) {

                    Text(
                        text =
                            if (arabic)
                                "رقم: ${project.projectNumber}"
                            else
                                "No: ${project.projectNumber}"
                    )
                }

                if (project.clientName.isNotBlank()) {

                    Text(
                        text =
                            if (arabic)
                                "العميل: ${project.clientName}"
                            else
                                "Client: ${project.clientName}"
                    )
                }

                Text(
                    text =
                        when (project.status) {

                            DesignStatus.DRAFT ->
                                if (arabic)
                                    "مسودة"
                                else
                                    "Draft"

                            DesignStatus.IN_PROGRESS ->
                                if (arabic)
                                    "قيد التنفيذ"
                                else
                                    "In Progress"

                            DesignStatus.COMPLETED ->
                                if (arabic)
                                    "مكتمل"
                                else
                                    "Completed"

                            DesignStatus.ARCHIVED ->
                                if (arabic)
                                    "مؤرشف"
                                else
                                    "Archived"
                        }
                )
            }

            Button(
                onClick = onOpen
            ) {

                Text(
                    text =
                        if (arabic)
                            "فتح"
                        else
                            "Open"
                )
            }

            IconButton(
                onClick = onDelete
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Delete,
                    contentDescription =
                        if (arabic)
                            "حذف"
                        else
                            "Delete"
                )
            }
        }
    }
}

@Composable
fun NewProjectScreen(
    language: AppLanguage,
    onCancel: () -> Unit,
    onCreated: (DesignProject) -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    var projectName by remember {
        mutableStateOf("")
    }

    var projectNumber by remember {
        mutableStateOf("")
    }

    var clientName by remember {
        mutableStateOf("")
    }

    var consultantName by remember {
        mutableStateOf("")
    }

    var location by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var standard by remember {
        mutableStateOf(Standard.IEC)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Text(
            text =
                if (arabic)
                    "إنشاء مشروع هندسي"
                else
                    "Create Engineering Project",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = {
                projectName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "اسم المشروع"
                    else
                        "Project Name"
                )
            },
            singleLine = true
        )

        OutlinedTextField(
            value = projectNumber,
            onValueChange = {
                projectNumber = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "رقم المشروع"
                    else
                        "Project Number"
                )
            },
            singleLine = true
        )

        OutlinedTextField(
            value = clientName,
            onValueChange = {
                clientName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "العميل"
                    else
                        "Client"
                )
            },
            singleLine = true
        )

        OutlinedTextField(
            value = consultantName,
            onValueChange = {
                consultantName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "الاستشاري"
                    else
                        "Consultant"
                )
            },
            singleLine = true
        )

        OutlinedTextField(
            value = location,
            onValueChange = {
                location = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "الموقع"
                    else
                        "Location"
                )
            },
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "وصف المشروع"
                    else
                        "Project Description"
                )
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text =
                if (arabic)
                    "الكود الكهربائي"
                else
                    "Electrical Standard"
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            listOf(
                Standard.EGYPTIAN,
                Standard.IEC,
                Standard.NEC
            ).forEach { item ->

                OutlinedButton(
                    onClick = {
                        standard = item
                    }
                ) {

                    Text(
                        text =
                            if (standard == item)
                                "✓ ${item.shortName}"
                            else
                                item.shortName
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            OutlinedButton(
                onClick = onCancel
            ) {

                Text(
                    if (arabic)
                        "إلغاء"
                    else
                        "Cancel"
                )
            }

            Button(
                enabled =
                    projectName.isNotBlank(),
                onClick = {

                    val project =
                        DesignProjectCoreBridge.createProject(
                            projectName =
                                projectName.trim(),
                            projectNumber =
                                projectNumber.trim(),
                            clientName =
                                clientName.trim(),
                            consultantName =
                                consultantName.trim(),
                            location =
                                location.trim(),
                            description =
                                description.trim(),
                            electricalStandard =
                                standard
                        )

                    onCreated(project)
                }
            ) {

                Text(
                    if (arabic)
                        "إنشاء المشروع"
                    else
                        "Create Project"
                )
            }
        }
    }
}
