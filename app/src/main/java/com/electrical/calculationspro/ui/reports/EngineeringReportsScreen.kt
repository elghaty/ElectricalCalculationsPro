
package com.electrical.calculationspro.ui.reports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldEngineeringReportEngine
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import java.util.Locale

@Composable
fun EngineeringReportsScreen(
    language: AppLanguage,
    project: DesignProject?
) {
    val arabic = language == AppLanguage.ARABIC
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    val reportLines = remember(
        project?.id,
        project?.updatedAtMillis,
        arabic
    ) {
        project?.let { buildReportLines(it, arabic) }.orEmpty()
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri == null) {
            message = if (arabic) {
                "تم إلغاء حفظ التقرير."
            } else {
                "Report saving was cancelled."
            }
        } else {
            val result = runCatching {
                val currentProject = project
                    ?: error(
                        if (arabic) {
                            "لا يوجد مشروع نشط."
                        } else {
                            "There is no active project."
                        }
                    )

                val currentLines = buildReportLines(
                    currentProject,
                    arabic
                )

                EngineeringReportPdfExporter.write(
                    context = context,
                    uri = uri,
                    title = if (arabic) {
                        "التقرير الهندسي للمشروع"
                    } else {
                        "Engineering Project Report"
                    },
                    lines = currentLines
                )
            }

            message = result.fold(
                onSuccess = {
                    if (arabic) {
                        "تم حفظ تقرير PDF بنجاح."
                    } else {
                        "PDF report saved successfully."
                    }
                },
                onFailure = { error ->
                    (if (arabic) {
                        "تعذر إنشاء التقرير: "
                    } else {
                        "Could not create report: "
                    }) + (error.message ?: "Unknown error")
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (arabic) {
                "التقارير الهندسية"
            } else {
                "Engineering Reports"
            },
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.width(1.dp))

        if (project == null) {
            Text(
                text = if (arabic) {
                    "لا يوجد مشروع نشط. افتح إدارة المشروعات واختر مشروعًا أولًا."
                } else {
                    "No active project. Open Project Management and select a project first."
                },
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = project.projectName.ifBlank {
                    if (arabic) "مشروع بدون اسم" else "Unnamed project"
                },
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = if (arabic) {
                    "رقم المشروع: ${project.projectNumber.ifBlank { "—" }}\n" +
                        "العميل: ${project.clientName.ifBlank { "—" }}\n" +
                        "الاستشاري: ${project.consultantName.ifBlank { "—" }}\n" +
                        "الموقع: ${project.location.ifBlank { "—" }}\n" +
                        "الكود الكهربائي: ${project.electricalStandard ?: Standard.IEC}"
                } else {
                    "Project number: ${project.projectNumber.ifBlank { "—" }}\n" +
                        "Client: ${project.clientName.ifBlank { "—" }}\n" +
                        "Consultant: ${project.consultantName.ifBlank { "—" }}\n" +
                        "Location: ${project.location.ifBlank { "—" }}\n" +
                        "Electrical standard: ${project.electricalStandard ?: Standard.IEC}"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportCountCard(
                    modifier = Modifier.weight(1f),
                    label = if (arabic) "الأحمال" else "Loads",
                    count = project.electrical.loads.size
                )
                ReportCountCard(
                    modifier = Modifier.weight(1f),
                    label = if (arabic) "اللوحات" else "Panels",
                    count = project.electrical.panels.size
                )
                ReportCountCard(
                    modifier = Modifier.weight(1f),
                    label = if (arabic) "الكابلات" else "Cables",
                    count = project.electrical.cables.size
                )
            }

            Button(
                onClick = {
                    val fileName = project.projectName
                        .ifBlank { "Engineering_Report" }
                        .replace(Regex("[^A-Za-z0-9_-]"), "_")
                        .take(48) + "_Report.pdf"

                    createPdfLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (arabic) {
                        "إنشاء وحفظ تقرير PDF"
                    } else {
                        "Create and save PDF report"
                    }
                )
            }

            Text(
                text = if (arabic) {
                    "معاينة محتويات التقرير"
                } else {
                    "Report preview"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                reportLines.take(100).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (reportLines.size > 100) {
                    Text("…")
                }
            }
        }

        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ReportCountCard(
    modifier: Modifier,
    label: String,
    count: Int
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun buildReportLines(
    project: DesignProject,
    arabic: Boolean
): List<String> {
    val lines = mutableListOf<String>()

    fun section(title: String) {
        lines += ""
        lines += "========== $title =========="
    }

    fun field(label: String, value: Any?) {
        lines += "$label: ${value?.toString()?.ifBlank { "—" } ?: "—"}"
    }

    fun number(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    lines += if (arabic) {
        "التقرير الهندسي للمشروع"
    } else {
        "ENGINEERING PROJECT REPORT"
    }

    section(if (arabic) "بيانات المشروع" else "PROJECT INFORMATION")
    field("Project", project.projectName)
    field("Project number", project.projectNumber)
    field("Client", project.clientName)
    field("Consultant", project.consultantName)
    field("Location", project.location)
    field("Description", project.description)
    field("Status", project.status)
    field("Electrical standard", project.electricalStandard ?: Standard.IEC)

    section(if (arabic) "ملخص الكهرباء" else "ELECTRICAL SUMMARY")
    field("Load count", project.electrical.loads.size)
    field("Panel count", project.electrical.panels.size)
    field("Cable count", project.electrical.cables.size)
    field("Breaker count", project.electrical.breakers.size)
    field("Transformer count", project.electrical.transformers.size)
    field("Generator count", project.electrical.generators.size)

    section(if (arabic) "جدول الأحمال" else "LOAD SCHEDULE")
    if (project.electrical.loads.isEmpty()) {
        lines += if (arabic) "لا توجد أحمال مسجلة." else "No loads recorded."
    } else {
        project.electrical.loads.forEachIndexed { index, load ->
            lines += "${index + 1}. ${load.name}"
            lines += "Quantity: ${load.quantity}"
            lines += "Connected load per unit kW: ${number(load.connectedLoadKw)}"
            lines += "Total connected load kW: ${number(load.connectedLoadKw * load.quantity)}"
            lines += "Demand factor: ${load.demandFactor}"
            lines += "Diversity factor: ${load.diversityFactor}"
            lines += "Power factor: ${load.powerFactor}"
            lines += "Voltage V: ${load.voltageV}; Phases: ${load.phases}"
            lines += "Calculated design load kW: ${number(load.designLoadKw)}"
            lines += "Design current A: ${number(load.designCurrentA)}"
            lines += "Panel ID: ${load.sourcePanelId ?: "—"}"
            lines += "Status: ${load.status}"
            lines += ""
        }
    }

    section(if (arabic) "اللوحات" else "PANELS")
    if (project.electrical.panels.isEmpty()) {
        lines += "No panels recorded."
    }
    project.electrical.panels.forEach { panel ->
        lines += "Panel: ${panel.name}"
        lines += "Voltage V: ${panel.voltageV}; Phases: ${panel.phases}"
        lines += "Design load kW: ${number(panel.designLoadKw)}"
        lines += "Design current A: ${number(panel.designCurrentA)}"
        lines += "Short circuit kA: ${number(panel.shortCircuitKA)}"
        lines += "Status: ${panel.status}"
    }

    section(if (arabic) "القواطع" else "BREAKERS")
    if (project.electrical.breakers.isEmpty()) {
        lines += "No breakers recorded."
    }
    project.electrical.breakers.forEach { breaker ->
        lines += "Breaker: ${breaker.name}"
        lines += "Rating A: ${number(breaker.ratingA)}"
        lines += "Poles: ${breaker.poles}"
        lines += "Breaking capacity kA: ${number(breaker.breakingCapacityKA)}"
        lines += "Status: ${breaker.status}"
    }

    section(if (arabic) "الكابلات" else "CABLES")
    if (project.electrical.cables.isEmpty()) {
        lines += "No cables recorded."
    }
    project.electrical.cables.forEach { cable ->
        lines += "Cable: ${cable.name}; From: ${cable.from}; To: ${cable.to}"
        lines += "Length m: ${number(cable.lengthM)}; Section mm2: ${number(cable.sectionMm2)}"
        lines += "Design current A: ${number(cable.designCurrentA)}; Ampacity A: ${number(cable.ampacityA)}"
        lines += "Voltage drop %: ${number(cable.voltageDropPercent)}; Status: ${cable.status}"
    }

    section(if (arabic) "المحولات والمولدات" else "TRANSFORMERS AND GENERATORS")
    project.electrical.transformers.forEach { transformer ->
        lines += "Transformer: ${transformer.name}; Rating kVA: ${number(transformer.ratingKva)}"
        lines += "Primary V: ${transformer.primaryVoltageV}; Secondary V: ${transformer.secondaryVoltageV}"
        lines += "Impedance %: ${number(transformer.impedancePercent)}; Status: ${transformer.status}"
    }
    project.electrical.generators.forEach { generator ->
        lines += "Generator: ${generator.name}; Rating kVA: ${number(generator.ratingKva)}"
        lines += "Voltage V: ${generator.voltageV}; Power factor: ${generator.powerFactor}"
        lines += "Status: ${generator.status}"
    }

    section(if (arabic) "المياه" else "WATER DESIGN")
    field("Water design status", project.water.status)
    field("Required flow m3/h", number(project.water.requiredFlowM3PerHour))
    field("Static head m", number(project.water.staticHeadM))
    field("Friction head m", number(project.water.frictionHeadM))
    field("Minor loss head m", number(project.water.minorLossHeadM))
    field("Required pressure head m", number(project.water.requiredPressureHeadM))
    field("Total dynamic head m", number(project.water.tdhM))

    project.water.pipes.forEach { pipe ->
        lines += "Pipe: ${pipe.name}; Diameter mm: ${number(pipe.diameterMm)}"
        lines += "Length m: ${number(pipe.lengthM)}; Flow m3/h: ${number(pipe.flowM3PerHour)}"
        lines += "Velocity m/s: ${number(pipe.velocityMPerS)}"
        lines += "Friction loss m: ${number(pipe.frictionLossM)}; Status: ${pipe.status}"
    }

    project.water.pumps.forEach { pump ->
        lines += "Water pump: ${pump.name}; Flow m3/h: ${number(pump.flowM3PerHour)}"
        lines += "Head m: ${number(pump.headM)}; Motor power kW: ${number(pump.motorPowerKw)}"
        lines += "Pump efficiency: ${pump.pumpEfficiency}; Motor efficiency: ${pump.motorEfficiency}"
        lines += "Yearly energy kWh: ${number(pump.yearlyEnergyKwh)}"
        lines += "Manufacturer: ${pump.manufacturer}; Model: ${pump.model}; Status: ${pump.status}"
    }

    section(if (arabic) "الصرف الصحي" else "SEWAGE DESIGN")
    field("Sewage design status", project.sewage.status)
    field("Average flow m3/day", number(project.sewage.averageFlowM3PerDay))
    field("Peak flow m3/day", number(project.sewage.peakFlowM3PerDay))
    field("Minimum flow m3/day", number(project.sewage.minimumFlowM3PerDay))
    field("Static head m", number(project.sewage.staticHeadM))
    field("Total dynamic head m", number(project.sewage.tdhM))

    project.sewage.wetWell?.let { wetWell ->
        lines += "Wet well: ${wetWell.name}"
        lines += "Diameter m: ${number(wetWell.diameterM)}"
        lines += "Effective depth m: ${number(wetWell.effectiveDepthM)}"
        lines += "Operating volume m3: ${number(wetWell.operatingVolumeM3)}"
        lines += "Start level m: ${number(wetWell.startLevelM)}; Stop level m: ${number(wetWell.stopLevelM)}"
        lines += "High level m: ${number(wetWell.highLevelM)}; Emergency level m: ${number(wetWell.emergencyLevelM)}"
    }

    project.sewage.risingMain?.let { main ->
        lines += "Rising main: ${main.name}; Diameter mm: ${number(main.diameterMm)}"
        lines += "Length m: ${number(main.lengthM)}; Flow m3/h: ${number(main.flowM3PerHour)}"
        lines += "Velocity m/s: ${number(main.velocityMPerS)}"
        lines += "Friction loss m: ${number(main.frictionLossM)}; Minor loss m: ${number(main.minorLossHeadM)}"
    }

    project.sewage.pumps.forEach { pump ->
        lines += "Sewage pump: ${pump.name}; Duty: ${pump.duty}; Standby: ${pump.standby}"
        lines += "Flow m3/h: ${number(pump.flowM3PerHour)}; Head m: ${number(pump.headM)}"
        lines += "Motor power kW: ${number(pump.motorPowerKw)}"
        lines += "Pump efficiency: ${pump.pumpEfficiency}; Motor efficiency: ${pump.motorEfficiency}"
        lines += "Yearly energy kWh: ${number(pump.yearlyEnergyKwh)}"
        lines += "Manufacturer: ${pump.manufacturer}; Model: ${pump.model}; Status: ${pump.status}"
    }

    section(if (arabic) "دراسة SLD" else "SLD ENGINEERING STUDY")
    val networkResult = runCatching {
        DesignProjectCoreBridge.getProjectSld(project)
    }

    val network = networkResult.getOrNull()
    if (network == null || network.nodes.isEmpty()) {
        lines += if (arabic) {
            "لا توجد شبكة SLD متاحة للتقرير."
        } else {
            "No SLD network is available for reporting."
        }
    } else {
        lines += "Node count: ${network.nodes.size}"
        lines += "Connection count: ${network.connections.size}"

        val studyResult = runCatching {
            DesignProjectCoreBridge.calculateProjectSld(project)
        }

        val study = studyResult.getOrNull()
        if (study == null) {
            lines += if (arabic) {
                "لم تُدرج نتائج SLD لأن الشبكة لم تجتز التحقق الهندسي أو تعذر إكمال الحساب."
            } else {
                "SLD results were omitted because topology validation failed or the engineering study could not be completed."
            }
            lines += "Reason: ${studyResult.exceptionOrNull()?.message ?: "Not available"}"
        } else {
            lines += SldEngineeringReportEngine
                .build(network, study)
                .asText()
                .lineSequence()
                .toList()
        }
    }

    section(if (arabic) "ملاحظات التقرير" else "REPORT NOTES")
    lines += if (arabic) {
        "يعرض التقرير البيانات المسجلة ونتائج SLD عند نجاح التحقق والحساب. القيم غير المتاحة ليست اعتمادًا نهائيًا للتصميم."
    } else {
        "This report presents stored project data and SLD results only when validation and calculation succeed. Missing values do not represent final design approval."
    }

    return lines
}
