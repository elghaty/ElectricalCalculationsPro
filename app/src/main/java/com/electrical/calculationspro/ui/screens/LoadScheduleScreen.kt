
package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.electrical.ElectricalDesignModule
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.ElectricalLoad
import com.electrical.calculationspro.ui.components.EngineeringEmptyState
import com.electrical.calculationspro.ui.components.EngineeringInput
import com.electrical.calculationspro.ui.components.EngineeringPage
import com.electrical.calculationspro.ui.components.EngineeringPrimaryButton
import com.electrical.calculationspro.ui.components.EngineeringResult
import com.electrical.calculationspro.ui.components.EngineeringSection
import com.electrical.calculationspro.ui.components.EngineeringStatus
import com.electrical.calculationspro.ui.components.EngineeringValueRow
import com.electrical.calculationspro.ui.components.FourColumnFields
import java.util.Locale

@Composable
fun LoadScheduleScreen(
    language: AppLanguage,
    project: DesignProject?,
    onBack: () -> Unit,
    onProjectUpdated: (DesignProject) -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var connectedLoadKw by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var demandFactor by remember { mutableStateOf("1.0") }
    var diversityFactor by remember { mutableStateOf("1.0") }
    var powerFactor by remember { mutableStateOf("0.90") }
    var voltageV by remember { mutableStateOf("400") }
    var phases by remember { mutableStateOf("3") }

    var selectedPanelId by remember(project?.id) {
        mutableStateOf(project?.electrical?.panels?.firstOrNull()?.id)
    }
    var panelMenuExpanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    val panels = project?.electrical?.panels.orEmpty()
    val selectedPanel = panels.firstOrNull { it.id == selectedPanelId }
    val loads = project?.electrical?.loads.orEmpty()

    fun saveLoad() {
        val currentProject = project

        if (currentProject == null) {
            message = if (arabic) {
                "لا يوجد مشروع نشط. أنشئ مشروعًا أو اختر مشروعًا أولًا."
            } else {
                "No active project. Create or select a project first."
            }
            messageIsError = true
            return
        }

        val loadKw = connectedLoadKw.toDoubleOrNull()
        val count = quantity.toIntOrNull()
        val demand = demandFactor.toDoubleOrNull()
        val diversity = diversityFactor.toDoubleOrNull()
        val pf = powerFactor.toDoubleOrNull()
        val voltage = voltageV.toDoubleOrNull()
        val phaseCount = phases.toIntOrNull()

        val validationError = when {
            name.isBlank() ->
                if (arabic) "أدخل اسم الحمل." else "Enter a load name."

            loadKw == null || !loadKw.isFinite() || loadKw <= 0.0 ->
                if (arabic) "القدرة المتصلة يجب أن تكون أكبر من صفر." else "Connected load must be greater than zero."

            count == null || count < 1 ->
                if (arabic) "الكمية يجب أن تكون 1 على الأقل." else "Quantity must be at least 1."

            demand == null || !demand.isFinite() || demand !in 0.0..1.0 ->
                if (arabic) "معامل الطلب يجب أن يكون من صفر إلى 1." else "Demand factor must be between 0 and 1."

            diversity == null || !diversity.isFinite() || diversity !in 0.0..1.0 ->
                if (arabic) "معامل التنوع يجب أن يكون من صفر إلى 1." else "Diversity factor must be between 0 and 1."

            pf == null || !pf.isFinite() || pf <= 0.0 || pf > 1.0 ->
                if (arabic) "معامل القدرة يجب أن يكون أكبر من صفر وحتى 1." else "Power factor must be greater than 0 and no more than 1."

            voltage == null || !voltage.isFinite() || voltage <= 0.0 ->
                if (arabic) "الجهد يجب أن يكون أكبر من صفر." else "Voltage must be greater than zero."

            phaseCount == null || phaseCount !in 1..3 ->
                if (arabic) "عدد الأطوار يجب أن يكون 1 أو 2 أو 3." else "Phases must be 1, 2 or 3."

            else -> null
        }

        if (validationError != null) {
            message = validationError
            messageIsError = true
            return
        }

        val load = ElectricalLoad(
            name = name.trim(),
            description = description.trim(),
            quantity = count!!,
            connectedLoadKw = loadKw!!,
            demandFactor = demand!!,
            diversityFactor = diversity!!,
            powerFactor = pf!!,
            voltageV = voltage!!,
            phases = phaseCount!!,
            sourcePanelId = selectedPanelId
        )

        val updatedProject = try {
            val projectWithLoad = ElectricalDesignModule.addLoad(
                project = currentProject,
                load = load
            )
            ElectricalDesignModule.recalculate(projectWithLoad)
        } catch (exception: Exception) {
            message = (if (arabic) "فشل حفظ الحمل: " else "Failed to save load: ") +
                (exception.message ?: exception.javaClass.simpleName)
            messageIsError = true
            return
        }

        onProjectUpdated(updatedProject)

        name = ""
        description = ""
        connectedLoadKw = ""
        quantity = "1"
        demandFactor = "1.0"
        diversityFactor = "1.0"
        powerFactor = "0.90"
        voltageV = "400"
        phases = "3"

        message = if (arabic) {
            "تم حفظ الحمل وإعادة الحساب."
        } else {
            "Load saved and recalculated."
        }
        messageIsError = false
    }

    EngineeringPage(
        title = if (arabic) "جدول الأحمال الكهربائية" else "Electrical Load Schedule",
        subtitle = if (arabic) {
            "تسجيل أحمال المشروع وربطها بلوحة التغذية."
        } else {
            "Register project loads and assign their source panel."
        },
        onBack = onBack
    ) {
        if (project == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا يوجد مشروع نشط" else "No active project",
                message = if (arabic) {
                    "افتح إدارة المشروعات واختر مشروعًا قبل تسجيل الأحمال."
                } else {
                    "Open Project Management and select a project before adding loads."
                }
            )
            return@EngineeringPage
        }

        EngineeringSection(
            title = if (arabic) "بيانات الحمل الجديد" else "New Load Data"
        ) {
            FourColumnFields {
                item {
                    EngineeringInput(
                        value = name,
                        label = if (arabic) "اسم الحمل" else "Load name",
                        onValueChange = {
                            name = it
                            message = null
                        },
                        isNumeric = false
                    )
                }
                item {
                    EngineeringInput(
                        value = connectedLoadKw,
                        label = if (arabic) "القدرة المتصلة (kW)" else "Connected load (kW)",
                        onValueChange = {
                            connectedLoadKw = it
                            message = null
                        }
                    )
                }
                item {
                    EngineeringInput(
                        value = quantity,
                        label = if (arabic) "الكمية" else "Quantity",
                        onValueChange = {
                            quantity = it
                            message = null
                        }
                    )
                }
                item {
                    EngineeringInput(
                        value = demandFactor,
                        label = if (arabic) "معامل الطلب" else "Demand factor",
                        onValueChange = {
                            demandFactor = it
                            message = null
                        }
                    )
                }
                item {
                    EngineeringInput(
                        value = diversityFactor,
                        label = if (arabic) "معامل التنوع" else "Diversity factor",
                        onValueChange = {
                            diversityFactor = it
                            message = null
                        }
                    )
                }
                item {
                    EngineeringInput(
                        value = powerFactor,
                        label = if (arabic) "معامل القدرة" else "Power factor",
                        onValueChange = {
                            powerFactor = it
                            message = null
                        }
                    )
                }
                item {
                    EngineeringInput(
                        value = voltageV,
                        label = if (arabic) "الجهد (V)" else "Voltage (V)",
                        onValueChange = {
                            voltageV = it
                            message = null
                        }
                    )
                }
                item {
                    EngineeringInput(
                        value = phases,
                        label = if (arabic) "عدد الأطوار" else "Phases (1–3)",
                        onValueChange = {
                            phases = it
                            message = null
                        }
                    )
                }
            }

            EngineeringInput(
                value = description,
                label = if (arabic) "وصف الحمل (اختياري)" else "Description (optional)",
                onValueChange = {
                    description = it
                    message = null
                },
                isNumeric = false
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box {
                OutlinedButton(
                    onClick = { panelMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedPanel?.name
                            ?: if (arabic) "بدون لوحة تغذية" else "No source panel"
                    )
                }

                DropdownMenu(
                    expanded = panelMenuExpanded,
                    onDismissRequest = { panelMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (arabic) "بدون لوحة تغذية" else "No source panel")
                        },
                        onClick = {
                            selectedPanelId = null
                            panelMenuExpanded = false
                        }
                    )

                    panels.forEach { panel ->
                        DropdownMenuItem(
                            text = { Text(panel.name) },
                            onClick = {
                                selectedPanelId = panel.id
                                panelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (panels.isEmpty()) {
                EngineeringStatus(
                    text = if (arabic) {
                        "لا توجد لوحات بالمشروع. يمكنك تسجيل الحمل دون لوحة، ثم ربطه بلوحة بعد إنشائها."
                    } else {
                        "The project has no panels. You can register the load without a panel and assign it after creating one."
                    },
                    success = false
                )
            }

            EngineeringPrimaryButton(
                text = if (arabic) "حفظ الحمل وإعادة الحساب" else "Save Load & Recalculate",
                onClick = { saveLoad() }
            )

            message?.let {
                EngineeringStatus(
                    text = it,
                    success = !messageIsError
                )
            }
        }

        EngineeringSection(
            title = if (arabic) "الأحمال المسجلة" else "Registered Loads"
        ) {
            if (loads.isEmpty()) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد أحمال مسجلة" else "No registered loads",
                    message = if (arabic) {
                        "أضف أول حمل باستخدام النموذج أعلاه."
                    } else {
                        "Add the first load using the form above."
                    }
                )
            } else {
                EngineeringValueRow(
                    label = if (arabic) "عدد الأحمال" else "Load count",
                    value = loads.size.toString()
                )

                EngineeringValueRow(
                    label = if (arabic) "إجمالي القدرة المتصلة" else "Total connected load",
                    value = String.format(
                        Locale.US,
                        "%.2f kW",
                        loads.sumOf { it.connectedLoadKw * it.quantity.coerceAtLeast(1) }
                    )
                )

                loads.forEach { load ->
                    EngineeringResult(
                        title = load.name,
                        value = buildString {
                            append(
                                String.format(
                                    Locale.US,
                                    "%.2f kW × %d",
                                    load.connectedLoadKw,
                                    load.quantity
                                )
                            )
                            append("\n")
                            append(
                                if (arabic) "الحمل التصميمي: " else "Design load: "
                            )
                            append(
                                String.format(Locale.US, "%.2f kW", load.designLoadKw)
                            )
                            append("\n")
                            append(
                                if (arabic) "تيار التصميم: " else "Design current: "
                            )
                            append(
                                String.format(Locale.US, "%.2f A", load.designCurrentA)
                            )
                            append("\n")
                            append(
                                if (arabic) "الحالة: " else "Status: "
                            )
                            append(load.status.name)
                        }
                    )

                    val sourcePanel = panels.firstOrNull {
                        it.id == load.sourcePanelId
                    }

                    if (sourcePanel != null) {
                        Text(
                            text = (if (arabic) "لوحة التغذية: " else "Source panel: ") +
                                sourcePanel.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
