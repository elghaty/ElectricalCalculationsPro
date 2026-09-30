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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.ui.components.EngineeringStatus

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

val context =
    LocalContext.current

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

                context
                    .contentResolver
                    .openOutputStream(uri)
                    ?.use { stream ->

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
                MaterialTheme.colorScheme.background
            )
) {

    // ========================================================
    // HEADER
    // ========================================================

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        if (onBack != null) {

            IconButton(
                onClick = onBack
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
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    if (arabic) {
                        "تصميم كهربائي تفاعلي مع الحساب الهندسي التلقائي"
                    } else {
                        "Interactive electrical design with automatic engineering calculation"
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
                    Icons.Outlined.Calculate,
                contentDescription = null
            )

            Spacer(
                modifier =
                    Modifier.width(5.dp)
            )

            Text(
                text =
                    if (arabic) {
                        "حساب"
                    } else {
                        "Calculate"
                    }
            )
        }
    }

    // ========================================================
    // STATUS
    // ========================================================

    val statusText =
        when {

            state.connectionStartId != null -> {
                if (arabic) {
                    "وضع التوصيل نشط — اختر العنصر الثاني"
                } else {
                    "Connection mode active — select destination"
                }
            }

            state.selectedNodeId != null -> {
                if (arabic) {
                    "تم تحديد عنصر"
                } else {
                    "Component selected"
                }
            }

            state.selectedConnectionId != null -> {
                if (arabic) {
                    "تم تحديد وصلة"
                } else {
                    "Connection selected"
                }
            }

            state.engineeringPackage != null -> {
                if (arabic) {
                    "الدراسة الهندسية محدثة"
                } else {
                    "Engineering study is up to date"
                }
            }

            else -> {
                if (arabic) {
                    "جاهز للتصميم"
                } else {
                    "Ready for design"
                }
            }
        }

    EngineeringStatus(
        text = statusText,
        success = state.engineeringError == null,
        modifier =
            Modifier.padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )
    )

    if (!state.engineeringError.isNullOrBlank()) {

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 3.dp
                    ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.errorContainer
                )
        ) {

            Text(
                text =
                    state.engineeringError!!,
                modifier =
                    Modifier.padding(12.dp),
                color =
                    MaterialTheme.colorScheme.onErrorContainer,
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }

    // ========================================================
    // CONNECTION MODE
    // ========================================================

    if (state.connectionStartId != null) {

        val startNode =
            state.nodes.firstOrNull {
                it.id == state.connectionStartId
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
                        MaterialTheme.colorScheme.secondaryContainer
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

                Icon(
                    imageVector =
                        Icons.Outlined.Link,
                    contentDescription = null
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
                                "وضع التوصيل"
                            } else {
                                "CONNECTION MODE"
                            },
                        style =
                            MaterialTheme.typography.labelLarge,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            if (arabic) {
                                "من: ${startNode?.name ?: "العنصر الأول"} — اختر العنصر الثاني"
                            } else {
                                "From: ${startNode?.name ?: "Start"} — select destination"
                            },
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedButton(
                    onClick = {
                        state.clearDialogs()
                        state.selectedNodeId = null
                        state.selectedConnectionId = null
                        state.engineeringError = null
                    }
                ) {

                    Text(
                        text =
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

    // ========================================================
    // TOOLBAR
    // ========================================================

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 5.dp
                ),
        horizontalArrangement =
            Arrangement.spacedBy(7.dp)
    ) {

        AddComponentMenu(
            arabic = arabic,
            onType = {
                actions.resetNodeEditor(it)
            }
        )

        SldToolButton(
            icon = Icons.Outlined.Link,
            text =
                if (state.connectionStartId == null) {
                    if (arabic) {
                        "توصيل"
                    } else {
                        "Connect"
                    }
                } else {
                    if (arabic) {
                        "إلغاء التوصيل"
                    } else {
                        "Cancel"
                    }
                },
            onClick = {

                if (
                    state.connectionStartId == null
                ) {

                    if (
                        state.selectedNodeId == null
                    ) {

                        state.engineeringError =
                            if (arabic) {
                                "حدد العنصر الأول ثم اضغط توصيل."
                            } else {
                                "Select the first component, then press Connect."
                            }

                    } else {

                        actions.startOrCompleteConnection()
                    }

                } else {

                    state.connectionStartId = null
                    state.engineeringError = null
                }
            }
        )

        SldToolButton(
            icon = Icons.Outlined.AutoFixHigh,
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

        SldToolButton(
            icon = Icons.Outlined.Delete,
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

        SldToolButton(
            icon = Icons.Outlined.Calculate,
            text =
                if (arabic) {
                    "قصر"
                } else {
                    "Short Circuit"
                },
            onClick = {
                actions.runShortCircuit()
            }
        )

        SldToolButton(
            icon = Icons.Outlined.TableView,
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

        SldToolButton(
            icon = Icons.Outlined.Settings,
            text =
                if (arabic) {
                    "الدراسة"
                } else {
                    "Complete Study"
                },
            onClick = {
                actions.generateCompleteSld()
            }
        )

        SldToolButton(
            icon = Icons.Outlined.PictureAsPdf,
            text = "PDF",
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
        )
    }

    // ========================================================
    // METRICS
    // ========================================================

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 2.dp
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            SldMetric(
                value =
                    state.nodes.size.toString(),
                label =
                    if (arabic) {
                        "عناصر"
                    } else {
                        "Nodes"
                    }
            )

            SldMetric(
                value =
                    state.connections.size.toString(),
                label =
                    if (arabic) {
                        "وصلات"
                    } else {
                        "Connections"
                    }
            )

            SldMetric(
                value =
                    if (
                        state.engineeringPackage != null
                    ) {
                        "OK"
                    } else {
                        "—"
                    },
                label =
                    if (arabic) {
                        "الحساب"
                    } else {
                        "Study"
                    }
            )

            Text(
                text = "SLD Engineering",
                style =
                    MaterialTheme.typography.labelMedium,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )
        }
    }

    // ========================================================
    // CANVAS
    // ========================================================

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
             * Normal click = SELECT ONLY.
             *
             * Click while connectionStartId is non-null =
             * COMPLETE CONNECTION.
             *
             * This prevents ordinary node taps and dragging
             * from unexpectedly entering connection mode.
             */
            onSelectNode = { id ->

                if (
                    state.connectionStartId != null
                ) {

                    actions.startOrCompleteConnection(id)

                } else {

                    state.selectedNodeId = id
                    state.selectedConnectionId = null
                    state.engineeringError = null
                }
            },

            onMoveNode = { id, dx, dy ->

                /*
                 * Never move nodes while connection mode is
                 * active.
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

                state.selectedConnectionId = id
                state.selectedNodeId = null
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

// ============================================================
// NODE DIALOG
// ============================================================

if (state.showNodeDialog) {

    SldNodeEditorDialog(
        arabic = arabic,
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

// ============================================================
// CONNECTION DIALOG
// ============================================================

if (state.showConnectionDialog) {

    SldConnectionEditorDialog(
        arabic = arabic,

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

        /*
         * IMPORTANT:
         *
         * Cancel must terminate the complete connection
         * operation, not only hide the dialog.
         */
        onCancel = {

            state.showConnectionDialog = false
            state.editingConnectionId = null
            state.connectionStartId = null
            state.selectedNodeId = null
            state.selectedConnectionId = null
            state.engineeringError = null
        }
    )
}

// ============================================================
// REPORT
// ============================================================

if (state.showReport) {

    AlertDialog(

        onDismissRequest = {
            state.showReport = false
        },

        title = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {

                Text(
                    text =
                        state.reportTitle.ifBlank {
                            if (arabic) {
                                "تقرير التصميم"
                            } else {
                                "Engineering Report"
                            }
                        },
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        if (arabic) {
                            "نتائج دراسة المخطط الأحادي"
                        } else {
                            "Single line diagram engineering study"
                        },
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },

        text = {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {

                Text(
                    text =
                        state.reportText,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(520.dp)
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .padding(14.dp),
                    style =
                        MaterialTheme.typography.bodySmall
                )
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
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    text =
                        if (arabic) {
                            "تصدير PDF"
                        } else {
                            "Export PDF"
                        }
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = {
                    state.showReport = false
                }
            ) {

                Text(
                    text =
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

// ================================================================
// TOOL BUTTON
// ================================================================

@Composable
private fun SldToolButton(
icon: ImageVector,
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

    Text(text)
}

}

// ================================================================
// ADD COMPONENT MENU
// ================================================================

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
            contentDescription = null
        )

        Spacer(
            modifier =
                Modifier.width(5.dp)
        )

        Text(
            text =
                if (arabic) {
                    "إضافة"
                } else {
                    "Add"
                }
        )
    }

    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {

        SldNodeType.values().forEach { type ->

            androidx.compose.material3.DropdownMenuItem(

                text = {

                    Text(
                        when (type) {

                            SldNodeType.SOURCE ->
                                if (arabic) {
                                    "مصدر"
                                } else {
                                    "Source"
                                }

                            SldNodeType.TRANSFORMER ->
                                if (arabic) {
                                    "محول"
                                } else {
                                    "Transformer"
                                }

                            SldNodeType.GENERATOR ->
                                if (arabic) {
                                    "مولد"
                                } else {
                                    "Generator"
                                }

                            SldNodeType.BUS ->
                                if (arabic) {
                                    "Bus"
                                } else {
                                    "Bus"
                                }

                            SldNodeType.PANEL ->
                                if (arabic) {
                                    "لوحة"
                                } else {
                                    "Panel"
                                }

                            SldNodeType.BREAKER ->
                                if (arabic) {
                                    "قاطع"
                                } else {
                                    "Breaker"
                                }

                            SldNodeType.LOAD ->
                                if (arabic) {
                                    "حمل"
                                } else {
                                    "Load"
                                }
                        }
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

// ================================================================
// METRIC
// ================================================================

@Composable
private fun SldMetric(
value: String,
label: String
) {

Column(
    horizontalAlignment =
        Alignment.CenterHorizontally
) {

    Text(
        text = value,
        style =
            MaterialTheme.typography.titleMedium,
        fontWeight =
            FontWeight.Bold
    )

    Text(
        text = label,
        style =
            MaterialTheme.typography.labelSmall,
        color =
            MaterialTheme.colorScheme.onSurfaceVariant
    )
}

}
