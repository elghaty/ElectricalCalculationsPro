package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldNodeType

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

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF070C10)
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .padding(8.dp),

            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            onBack?.let {

                Button(
                    onClick = it
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

            val types =
                listOf(
                    SldNodeType.SOURCE,
                    SldNodeType.TRANSFORMER,
                    SldNodeType.GENERATOR,
                    SldNodeType.BUS,
                    SldNodeType.BREAKER,
                    SldNodeType.PANEL,
                    SldNodeType.LOAD
                )

            types.forEach { type ->

                Button(
                    onClick = {
                        actions.resetNodeEditor(type)
                    }
                ) {
                    Text(
                        type.name
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 8.dp
                    ),

            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Button(
                onClick = {
                    actions.startOrCompleteConnection()
                }
            ) {

                Text(
                    if (state.connectionStartId == null) {
                        if (arabic) {
                            "بدء التوصيل"
                        } else {
                            "Connect"
                        }
                    } else {
                        if (arabic) {
                            "اختيار الطرف الآخر"
                        } else {
                            "Select End"
                        }
                    }
                )
            }

            Button(
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

            Button(
                onClick = {
                    actions.runShortCircuit()
                }
            ) {
                Text(
                    if (arabic) {
                        "تيارات القصر"
                    } else {
                        "Short Circuit"
                    }
                )
            }

            Button(
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

            Button(
                onClick = {
                    actions.generateCompleteSld()
                }
            ) {
                Text(
                    if (arabic) {
                        "إنشاء SLD كامل"
                    } else {
                        "Generate Complete SLD"
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        SldCanvas(

            nodes = state.nodes,

            connections =
                state.connections,

            selectedNodeId =
                state.selectedNodeId,

            selectedConnectionId =
                state.selectedConnectionId,

            connectionStartId =
                state.connectionStartId,

            onSelectNode = { id ->

                state.selectedNodeId = id
                state.selectedConnectionId = null
            },

            onMoveNode = { id, dx, dy ->

                state.nodes =
                    state.nodes.map { node ->

                        if (node.id == id) {

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
            },

            onSelectConnection = { id ->

                state.selectedConnectionId = id
                state.selectedNodeId = null
                state.connectionStartId = null
            },

            onEditNode = {
                actions.editNode(it)
            },

            onEditConnection = {
                actions.editConnection(it)
            }
        )
    }

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

    if (state.showConnectionDialog) {

        SldConnectionEditorDialog(

            arabic = arabic,

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

    if (state.showReport) {

        SldReportDialog(

            title =
                state.reportTitle,

            text =
                state.reportText,

            onClose = {
                state.showReport = false
            }
        )
    }
}
