package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    val arabic =
        language == AppLanguage.ARABIC

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

    LaunchedEffect(Unit) {
        actions.loadProjectNetwork()
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    )
                    .horizontalScroll(
                        rememberScrollState()
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    state.clearSelection()
                    state.showNodeDialog = false
                    state.showConnectionDialog = false
                    state.editingNodeId = null
                    state.editingConnectionId = null
                }
            ) {
                Text(
                    if (arabic) {
                        "مخطط كهربائي"
                    } else {
                        "Electrical SLD"
                    }
                )
            }

            Button(
                onClick = {
                    state.showNodeDialog = false
                    state.showConnectionDialog = false
                }
            ) {
                Text(
                    if (arabic) {
                        "عرض"
                    } else {
                        "View"
                    }
                )
            }

            Button(
                onClick = {
                    state.showNodeDialog = false
                    state.showConnectionDialog = false
                }
            ) {
                Text(
                    if (arabic) {
                        "إضافة"
                    } else {
                        "Add"
                    }
                )
            }

            Button(
                enabled =
                    state.selectedNodeId != null,
                onClick = {
                    actions.startOrCompleteConnection()
                }
            ) {
                Text(
                    if (arabic) {
                        "توصيل"
                    } else {
                        "Connect"
                    }
                )
            }

            Button(
                enabled =
                    state.selectedNodeId != null ||
                        state.selectedConnectionId != null,
                onClick = {
                    actions.deleteSelected()
                }
            ) {
                Text(
                    if (arabic) {
                        "حذف"
                    } else {
                        "Delete"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    actions.recalculateEngineering()
                }
            ) {
                Text(
                    if (arabic) {
                        "حساب"
                    } else {
                        "Calculate"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    actions.validateDesign()
                }
            ) {
                Text(
                    if (arabic) {
                        "فحص"
                    } else {
                        "Validate"
                    }
                )
            }
        }

        state.engineeringError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    )
            )
        }

        Text(
            text =
                if (arabic) {
                    "Single Line Diagram — اسحب العناصر لتحريكها • اضغط مرتين للتعديل"
                } else {
                    "Single Line Diagram — Drag equipment to move • Double tap to edit"
                },
            style =
                MaterialTheme.typography.labelMedium,
            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                )
        )

        SldCanvas(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            nodes = state.nodes,
            connections = state.connections,
            selectedNodeId = state.selectedNodeId,
            selectedConnectionId = state.selectedConnectionId,
            connectionStartId = state.connectionStartId,
            engineering = state.engineeringPackage,

            onSelectNode = { id ->
                state.selectedNodeId = id
                state.selectedConnectionId = null
            },

            onMoveNode = { id, dx, dy ->
                state.nodes =
                    state.nodes.map { node ->
                        if (node.id == id) {
                            node.copy(
                                x = node.x + dx,
                                y = node.y + dy
                            )
                        } else {
                            node
                        }
                    }
            },

            onMoveNodeEnd = {
                actions.saveAndRecalculate()
            },

            onSelectConnection = { id ->
                state.selectedConnectionId = id
                state.selectedNodeId = null
            },

            onEditNode = { node ->
                actions.editNode(node)
            },

            onEditConnection = { connection ->
                actions.editConnection(connection)
            }
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    )
                    .horizontalScroll(
                        rememberScrollState()
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    actions.runShortCircuit()
                }
            ) {
                Text(
                    if (arabic) {
                        "تيار القصر"
                    } else {
                        "Short Circuit"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    actions.runPanelSchedule()
                }
            ) {
                Text(
                    if (arabic) {
                        "جدول اللوحة"
                    } else {
                        "Panel Schedule"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    actions.autoLayout()
                }
            ) {
                Text(
                    if (arabic) {
                        "ترتيب تلقائي"
                    } else {
                        "Auto Layout"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    actions.generateCompleteSld()
                }
            ) {
                Text(
                    if (arabic) {
                        "دراسة كاملة"
                    } else {
                        "Full Study"
                    }
                )
            }

            onBack?.let { back ->
                OutlinedButton(
                    onClick = back
                ) {
                    Text(
                        if (arabic) {
                            "رجوع"
                        } else {
                            "Back"
                        }
                    )
                }
            }
        }
    }

    if (state.showNodeDialog) {
        SldNodeEditorDialog(
            arabic = arabic,
            editing = state.editingNodeId != null,
            type = state.nodeType,
            name = state.name,
            voltage = state.voltage,
            loadKw = state.loadKw,
            pf = state.pf,
            demand = state.demand,
            kva = state.kva,
            transformerZ = state.transformerZ,
            generatorXd = state.generatorXd,
            sourceMva = state.sourceMva,

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
                state.clearDialogs()
            }
        )
    }

    if (state.showConnectionDialog) {
        SldConnectionEditorDialog(
            arabic = arabic,
            connectionType = state.connectionType,
            conductorMaterial = state.conductorMaterial,
            insulationType = state.insulationType,
            installationMethodCode = state.installationMethodCode,
            busbarMaterial = state.busbarMaterial,
            busbarRatedCurrent = state.busbarRatedCurrent,
            busbarShortCircuit = state.busbarShortCircuit,
            length = state.length,
            resistance = state.resistance,
            reactance = state.reactance,
            cableSize = state.cableSize,
            parallelRuns = state.parallelRuns,
            capacity = state.capacity,

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
                state.clearDialogs()
            }
        )
    }

    if (state.showReport) {
        SldReportDialog(
            title = state.reportTitle,
            text = state.reportText,
            onClose = {
                state.showReport = false
            }
        )
    }
}
