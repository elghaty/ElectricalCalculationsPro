package com.electrical.calculationspro.ui.screens.sld

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldNodeType
import androidx.compose.material3.MaterialTheme

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {

    val state =
        remember {
            SldEditorState()
        }

    val actions =
        remember(language) {
            SldEditorActions(
                state = state,
                language = language
            )
        }

    val arabic =
        language == AppLanguage.ARABIC

    /*
     * ============================================================
     * PDF EXPORT
     * ============================================================
     */

    val pdfLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "application/pdf"
                )
        ) { uri ->

            if (
                uri != null &&
                    state.reportText.isNotBlank()
            ) {

                runCatching {

                    val output =
                        androidx.compose.ui.platform.LocalContext
                            .current
                            .contentResolver
                            .openOutputStream(uri)

                    output?.use { stream ->

                        SldPdfReportExporter.export(
                            outputStream = stream,
                            title =
                                state.reportTitle.ifBlank {
                                    if (arabic) {
                                        "تقرير التصميم الكهربائي"
                                    } else {
                                        "Electrical Engineering Design Report"
                                    }
                                },
                            reportText =
                                state.reportText
                        )
                    }

                }.onFailure { error ->

                    state.engineeringError =
                        error.message
                            ?: if (arabic) {
                                "فشل تصدير التقرير PDF."
                            } else {
                                "PDF export failed."
                            }
                }
            }
        }

    LaunchedEffect(Unit) {
        actions.loadProjectNetwork()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF3F6F8)
                )
    ) {

        /*
         * ========================================================
         * PROFESSIONAL HEADER
         * ========================================================
         */

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 7.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            onBack?.let { back ->

                IconButton(
                    onClick = back
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.ArrowBack,

                        contentDescription =
                            if (arabic) {
                                "رجوع"
                            } else {
                                "Back"
                            }
                    )
                }
            }

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (arabic) {
                            "مصمم المخطط الأحادي SLD"
                        } else {
                            "Single Line Diagram Designer"
                        },

                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text =
                        if (arabic) {
                            "تصميم وتوصيل وحساب هندسي تلقائي"
                        } else {
                            "Interactive electrical design and automatic engineering calculation"
                        },

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    actions.generateCompleteSld()
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.PlayArrow,

                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    if (arabic) {
                        "حساب التصميم"
                    } else {
                        "Calculate"
                    }
                )
            }
        }

        /*
         * ========================================================
         * CONNECTION MODE BANNER
         * ========================================================
         */

        if (
            state.connectionStartId != null
        ) {

            val startNode =
                state.nodes.firstOrNull {
                    it.id ==
                        state.connectionStartId
                }

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFFFF3CD)
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(9.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Link,

                        contentDescription =
                            null,

                        tint =
                            Color(0xFF8A5A00)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                if (arabic) {
                                    "وضع التوصيل نشط"
                                } else {
                                    "CONNECTION MODE"
                                },

                            fontSize = 13.sp,

                            color =
                                Color(0xFF6D4700)
                        )

                        Text(
                            text =
                                if (arabic) {
                                    "تم اختيار: ${startNode?.name ?: "العنصر الأول"} — اختر العنصر الثاني"
                                } else {
                                    "From: ${startNode?.name ?: "Start"} — select the destination component"
                                },

                            fontSize = 11.sp,

                            color =
                                Color(0xFF795548)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            state.connectionStartId = null
                        }
                    ) {

                        Text(
                            if (arabic) {
                                "إلغاء"
                            } else {
                                "Cancel"
                            }
                        )
                    }
                }
            }
        }

        /*
         * ========================================================
         * TOOLBAR
         * ========================================================
         */

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFE7EDF1)
                    )
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .padding(8.dp),

            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {

            AddComponentMenu(
                arabic = arabic,
                onType = {
                    actions.resetNodeEditor(it)
                }
            )

            ToolButton(
                icon =
                    Icons.Outlined.AutoFixHigh,

                text =
                    if (arabic) {
                        "ترتيب تلقائي"
                    } else {
                        "Auto Layout"
                    },

                onClick = {
                    actions.autoLayout()
                }
            )

            ToolButton(
                icon =
                    Icons.Outlined.Link,

                text =
                    if (
                        state.connectionStartId == null
                    ) {
                        if (arabic) {
                            "بدء التوصيل"
                        } else {
                            "Connect"
                        }
                    } else {
                        if (arabic) {
                            "اختر العنصر الثاني"
                        } else {
                            "Select End"
                        }
                    },

                onClick = {

                    /*
                     * If a node is already selected, pressing Connect
                     * immediately starts the connection from it.
                     *
                     * Otherwise the user is asked to select the first
                     * component.
                     */
                    actions.startOrCompleteConnection()
                }
            )

            ToolButton(
                icon =
                    Icons.Outlined.Delete,

                text =
                    if (arabic) {
                        "حذف"
                    } else {
                        "Delete"
                    },

                onClick = {
                    actions.deleteSelected()
                }
            )

            ToolButton(
                icon =
                    Icons.Outlined.Calculate,

                text =
                    if (arabic) {
                        "تيار القصر"
                    } else {
                        "Short Circuit"
                    },

                onClick = {
                    actions.runShortCircuit()
                }
            )

            ToolButton(
                icon =
                    Icons.Outlined.TableView,

                text =
                    if (arabic) {
                        "جدول اللوحة"
                    } else {
                        "Panel Schedule"
                    },

                onClick = {
                    actions.runPanelSchedule()
                }
            )

            ToolButton(
                icon =
                    Icons.Outlined.Settings,

                text =
                    if (arabic) {
                        "الدراسة الكاملة"
                    } else {
                        "Complete Study"
                    },

                onClick = {
                    actions.generateCompleteSld()
                }
            )

            Button(
                onClick = {

                    if (
                        state.reportText.isBlank()
                    ) {

                        actions.generateCompleteSld()

                    } else {

                        pdfLauncher.launch(
                            "SLD_Engineering_Report.pdf"
                        )
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.PictureAsPdf,

                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    if (arabic) {
                        "تقرير PDF"
                    } else {
                        "PDF Report"
                    }
                )
            }
        }

        /*
         * ========================================================
         * ENGINEERING STATUS
         * ========================================================
         */

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 5.dp
                    ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    val status =
                        when {

                            state.connectionStartId != null ->
                                if (arabic) {
                                    "في انتظار الطرف الثاني"
                                } else {
                                    "Waiting for destination"
                                }

                            state.selectedNodeId != null ->
                                if (arabic) {
                                    "عنصر محدد"
                                } else {
                                    "Component selected"
                                }

                            state.selectedConnectionId != null ->
                                if (arabic) {
                                    "وصلة محددة"
                                } else {
                                    "Connection selected"
                                }

                            else ->
                                if (arabic) {
                                    "جاهز للتصميم"
                                } else {
                                    "Ready for design"
                                }
                        }

                    Text(
                        text = status,
                        fontSize = 13.sp
                    )

                    if (
                        state.engineeringError != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        Text(
                            text =
                                state.engineeringError!!,

                            fontSize = 11.sp,

                            color =
                                Color(0xFFC62828)
                        )
                    }
                }

                Text(
                    text =
                        "${state.nodes.size} Nodes  •  ${state.connections.size} Connections",

                    fontSize = 11.sp,

                    color =
                        Color(0xFF607D8B)
                )
            }
        }

        /*
         * ========================================================
         * CANVAS
         * ========================================================
         */

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(6.dp)
        ) {

            SldCanvas(
                modifier =
                    Modifier.fillMaxSize(),

                nodes =
                    state.nodes,

                connections =
                    state.connections,

                selectedNodeId =
                    state.selectedNodeId,

                selectedConnectionId =
                    state.selectedConnectionId,

                connectionStartId =
                    state.connectionStartId,

                engineering =
                    state.engineeringPackage,

                /*
                 * IMPORTANT:
                 *
                 * In normal mode this only selects.
                 *
                 * In connection mode the second node immediately
                 * completes the connection through Actions.
                 */
                onSelectNode = { id ->

                    state.selectedNodeId =
                        id

                    state.selectedConnectionId =
                        null

                    if (
                        state.connectionStartId != null
                    ) {

                        actions.startOrCompleteConnection()
                    }
                },

                onMoveNode = { id, dx, dy ->

                    /*
                     * Never move a node while a connection is being
                     * created.
                     */
                    if (
                        state.connectionStartId == null
                    ) {

                        state.nodes =
                            state.nodes.map { node ->

                                if (
                                    node.id == id
                                ) {

                                    node.copy(
                                        x =
                                            kotlin.math.max(
                                                20f,
                                                node.x + dx
                                            ),

                                        y =
                                            kotlin.math.max(
                                                20f,
                                                node.y + dy
                                            )
                                    )

                                } else {
                                    node
                                }
                            }
                    }
                },

                onMoveNodeEnd = {

                    if (
                        state.connectionStartId == null
                    ) {
                        actions.saveAndRecalculate()
                    }
                },

                onSelectConnection = { id ->

                    state.selectedConnectionId =
                        id

                    state.selectedNodeId =
                        null
                },

                onEditNode = {
                    actions.editNode(it)
                },

                onEditConnection = {
                    actions.editConnection(it)
                }
            )
        }
    }

    /*
     * ============================================================
     * NODE DIALOG
     * ============================================================
     */

    if (
        state.showNodeDialog
    ) {

        SldNodeEditorDialog(
            arabic =
                arabic,

            editing =
                state.editingNodeId != null,

            type =
                state.nodeType,

            name =
                state.name,

            voltage =
                state.voltage,

            loadKw =
                state.loadKw,

            pf =
                state.pf,

            demand =
                state.demand,

            kva =
                state.kva,

            transformerZ =
                state.transformerZ,

            generatorXd =
                state.generatorXd,

            sourceMva =
                state.sourceMva,

            onNameChange = {
                state.name = it
            },

            onVoltageChange = {
                state.voltage = it
            },

            onLoadKwChange = {
                state.loadKw = it
            },

            onPfChange = {
                state.pf = it
            },

            onDemandChange = {
                state.demand = it
            },

            onKvaChange = {
                state.kva = it
            },

            onTransformerZChange = {
                state.transformerZ = it
            },

            onGeneratorXdChange = {
                state.generatorXd = it
            },

            onSourceMvaChange = {
                state.sourceMva = it
            },

            onSave = {
                actions.saveNode()
            },

            onCancel = {
                state.showNodeDialog = false
                state.editingNodeId = null
            }
        )
    }

    /*
     * ============================================================
     * CONNECTION DIALOG
     * ============================================================
     */

    if (
        state.showConnectionDialog
    ) {

        SldConnectionEditorDialog(
            arabic =
                arabic,

            connectionType =
                state.connectionType,

            conductorMaterial =
                state.conductorMaterial,

            insulationType =
                state.insulationType,

            installationMethodCode =
                state.installationMethodCode,

            busbarMaterial =
                state.busbarMaterial,

            busbarRatedCurrent =
                state.busbarRatedCurrent,

            busbarShortCircuit =
                state.busbarShortCircuit,

            length =
                state.length,

            resistance =
                state.resistance,

            reactance =
                state.reactance,

            cableSize =
                state.cableSize,

            parallelRuns =
                state.parallelRuns,

            capacity =
                state.capacity,

            onConnectionTypeChange = {
                state.connectionType = it
            },

            onConductorMaterialChange = {
                state.conductorMaterial = it
            },

            onInsulationTypeChange = {
                state.insulationType = it
            },

            onInstallationMethodChange = {
                state.installationMethodCode = it
            },

            onBusbarMaterialChange = {
                state.busbarMaterial = it
            },

            onBusbarRatedCurrentChange = {
                state.busbarRatedCurrent = it
            },

            onBusbarShortCircuitChange = {
                state.busbarShortCircuit = it
            },

            onLengthChange = {
                state.length = it
            },

            onResistanceChange = {
                state.resistance = it
            },

            onReactanceChange = {
                state.reactance = it
            },

            onCableSizeChange = {
                state.cableSize = it
            },

            onParallelRunsChange = {
                state.parallelRuns = it
            },

            onCapacityChange = {
                state.capacity = it
            },

            onSave = {
                actions.saveConnection()
            },

            onCancel = {
                state.showConnectionDialog = false
                state.editingConnectionId = null
                state.connectionStartId = null
            }
        )
    }

    /*
     * ============================================================
     * REPORT
     * ============================================================
     */

    if (
        state.showReport
    ) {

        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                state.showReport = false
            },

            title = {
                Text(
                    state.reportTitle
                )
            },

            text = {

                androidx.compose.foundation.layout.Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(520.dp)
                ) {

                    androidx.compose.foundation.verticalScroll(
                        rememberScrollState()
                    ).let { scrollState ->

                        Text(
                            text =
                                state.reportText,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),

                            fontSize = 11.sp
                        )
                    }
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        pdfLauncher.launch(
                            "SLD_Engineering_Report.pdf"
                        )
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.PictureAsPdf,

                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(5.dp)
                    )

                    Text(
                        if (arabic) {
                            "تصدير PDF"
                        } else {
                            "Export PDF"
                        }
                    )
                }
            },

            dismissButton = {

                androidx.compose.material3.TextButton(
                    onClick = {
                        state.showReport = false
                    }
                ) {

                    Text(
                        if (arabic) {
                            "إغلاق"
                        } else {
                            "Close"
                        }
                    )
                }
            }
        )
    }
}

/*
 * ================================================================
 * TOOL BUTTON
 * ================================================================
 */

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null
        )

        Spacer(
            modifier =
                Modifier.width(5.dp)
        )

        Text(
            text = text
        )
    }
}

/*
 * ================================================================
 * ADD COMPONENT MENU
 * ================================================================
 */

@Composable
private fun AddComponentMenu(
    arabic: Boolean,
    onType: (SldNodeType) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box {

        Button(
            onClick = {
                expanded = true
            }
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Add,

                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.width(5.dp)
            )

            Text(
                if (arabic) {
                    "إضافة عنصر"
                } else {
                    "Add Component"
                }
            )
        }

        DropdownMenu(
            expanded = expanded,

            onDismissRequest = {
                expanded = false
            }
        ) {

            SldNodeType.values().forEach { type ->

                DropdownMenuItem(
                    text = {
                        Text(
                            sldNodeTypeLabel(
                                type = type,
                                arabic = arabic
                            )
                        )
                    },

                    onClick = {

                        expanded = false

                        onType(type)
                    }
                )
            }
        }
    }
}

private fun sldNodeTypeLabel(
    type: SldNodeType,
    arabic: Boolean
): String {

    return when (type) {

        SldNodeType.SOURCE ->
            if (arabic) "مصدر كهرباء" else "Utility Source"

        SldNodeType.TRANSFORMER ->
            if (arabic) "محول" else "Transformer"

        SldNodeType.GENERATOR ->
            if (arabic) "مولد" else "Generator"

        SldNodeType.BUS ->
            if (arabic) "باسبار" else "Busbar"

        SldNodeType.PANEL ->
            if (arabic) "لوحة" else "Panel"

        SldNodeType.BREAKER ->
            if (arabic) "قاطع" else "Breaker"

        SldNodeType.LOAD ->
            if (arabic) "حمل" else "Load"
    }
}
