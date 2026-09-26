package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldNodeType

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

    /*
     * Load the project once when the SLD editor is opened.
     *
     * SldEditorActions.loadProjectNetwork()
     * also performs the first engineering calculation so the
     * diagram opens with current engineering values.
     */
    LaunchedEffect(Unit) {
        actions.loadProjectNetwork()
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        var addMenuExpanded by remember {
            mutableStateOf(false)
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Button(
                onClick = {
                    addMenuExpanded = true
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

            DropdownMenu(
                expanded =
                    addMenuExpanded,
                onDismissRequest = {
                    addMenuExpanded = false
                }
            ) {

                SldNodeType.values()
                    .forEach { type ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    nodeTypeTitle(
                                        type = type,
                                        arabic = arabic
                                    )
                                )
                            },
                            onClick = {

                                addMenuExpanded =
                                    false

                                actions.resetNodeEditor(
                                    type
                                )
                            }
                        )
                    }
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

        /*
         * Engineering error is intentionally displayed directly
         * above the canvas so an invalid topology cannot silently
         * look like a valid engineering result.
         */
        state.engineeringError?.let { error ->

            Text(
                text = error,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                modifier =
                    Modifier.padding(8.dp)
            )
        }

        /*
         * The canvas receives state.engineeringPackage directly.
         *
         * SldEditorState.engineeringPackage is Compose state,
         * therefore every successful recalculation causes the
         * engineering overlays to be redrawn automatically.
         */
        SldCanvas(
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

            onSelectNode = { id ->

                state.selectedNodeId =
                    id

                state.selectedConnectionId =
                    null
            },

            /*
             * Dragging changes only geometry while the finger is
             * moving. Engineering calculation is intentionally
             * performed once at the end of the drag, not on every
             * pointer event.
             */
            onMoveNode = { id, dx, dy ->

                state.nodes =
                    state.nodes.map { node ->

                        if (node.id == id) {

                            node.copy(
                                x =
                                    node.x + dx,

                                y =
                                    node.y + dy
                            )

                        } else {
                            node
                        }
                    }
            },

            /*
             * IMPORTANT:
             *
             * Previously this only called saveProjectNetwork().
             *
             * Now a completed move immediately refreshes the
             * engineering package as well.
             */
            onMoveNodeEnd = {
                actions.saveAndRecalculate()
            },

            onSelectConnection = { id ->

                state.selectedConnectionId =
                    id

                state.selectedNodeId =
                    null
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
                    .padding(8.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
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

    /*
     * ============================================================
     * NODE EDITOR
     * ============================================================
     */

    if (state.showNodeDialog) {

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

            /*
             * saveNode()
             *      ↓
             * saveAndRecalculate()
             *      ↓
             * current SldNetwork
             *      ↓
             * Core Bridge
             *      ↓
             * Complete SLD Engineering
             */
            onSave = {
                actions.saveNode()
            },

            onCancel = {
                state.clearDialogs()
            }
        )
    }

    /*
     * ============================================================
     * CONNECTION EDITOR
     * ============================================================
     */

    if (state.showConnectionDialog) {

        SldConnectionEditorDialog(

            arabic =
                arabic,

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

            /*
             * saveConnection()
             *      ↓
             * saveAndRecalculate()
             *      ↓
             * feeder engineering
             *      ↓
             * upstream propagation
             *      ↓
             * voltage drop / cable / protection
             */
            onSave = {
                actions.saveConnection()
            },

            onCancel = {
                state.clearDialogs()
            }
        )
    }

    /*
     * ============================================================
     * REPORT
     * ============================================================
     */

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

private fun nodeTypeTitle(
    type: SldNodeType,
    arabic: Boolean
): String =
    when (type) {

        SldNodeType.SOURCE ->
            if (arabic) {
                "مصدر تغذية"
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
                "قضبان Bus"
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
