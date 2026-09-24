package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldNodeType

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    val state = remember {
        SldEditorState()
    }

    val actions = remember(language) {
        SldEditorActions(
            state = state,
            language = language
        )
    }

    val arabic = language == AppLanguage.ARABIC

    LaunchedEffect(actions) {
        actions.loadProjectNetwork()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7F9))
    ) {

        /* =========================================================
         * HEADER
         * ========================================================= */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let { back ->
                IconButton(
                    onClick = back
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = if (arabic) {
                            "رجوع"
                        } else {
                            "Back"
                        },
                        tint = Color(0xFF263238)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (arabic) {
                        "مصمم الشبكة الكهربائية"
                    } else {
                        "Electrical Network Designer"
                    },
                    color = Color(0xFF172027),
                    fontSize = 18.sp
                )

                Text(
                    text = if (arabic) {
                        "المخطط الأحادي • التصميم الهندسي"
                    } else {
                        "Single Line Diagram • Engineering Design"
                    },
                    color = Color(0xFF687780),
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = {
                    actions.generateCompleteSld()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = if (arabic) {
                        "تشغيل الدراسة الهندسية"
                    } else {
                        "Run Engineering Study"
                    },
                    tint = Color(0xFF1565C0)
                )
            }
        }

        /* =========================================================
         * TOOLBAR
         * ========================================================= */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEAF0F4))
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {

            AddComponentMenu(
                arabic = arabic,
                onType = {
                    actions.resetNodeEditor(it)
                }
            )

            ToolButton(
                icon = Icons.Outlined.AutoFixHigh,
                text = if (arabic) {
                    "ترتيب تلقائي"
                } else {
                    "Auto Arrange"
                },
                onClick = {
                    actions.autoLayout()
                }
            )

            ToolButton(
                icon = Icons.Outlined.Link,
                text = if (state.connectionStartId == null) {
                    if (arabic) "توصيل" else "Connect"
                } else {
                    if (arabic) {
                        "اختر الطرف الآخر"
                    } else {
                        "Select End"
                    }
                },
                onClick = {
                    actions.startOrCompleteConnection()
                }
            )

            ToolButton(
                icon = Icons.Outlined.Delete,
                text = if (arabic) {
                    "حذف"
                } else {
                    "Delete"
                },
                onClick = {
                    actions.deleteSelected()
                }
            )

            ToolButton(
                icon = Icons.Outlined.Calculate,
                text = if (arabic) {
                    "تيارات القصر"
                } else {
                    "Short Circuit"
                },
                onClick = {
                    actions.runShortCircuit()
                }
            )

            ToolButton(
                icon = Icons.Outlined.TableView,
                text = if (arabic) {
                    "جدول اللوحة"
                } else {
                    "Panel Schedule"
                },
                onClick = {
                    actions.runPanelSchedule()
                }
            )

            ToolButton(
                icon = Icons.Outlined.Settings,
                text = if (arabic) {
                    "الدراسة الكاملة"
                } else {
                    "Complete Study"
                },
                onClick = {
                    actions.generateCompleteSld()
                }
            )
        }

        /* =========================================================
         * STATUS BAR
         * ========================================================= */

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when {
                            state.connectionStartId != null ->
                                if (arabic) {
                                    "بدء توصيل"
                                } else {
                                    "Connection Started"
                                }

                            state.selectedNodeId != null ->
                                if (arabic) {
                                    "عنصر محدد"
                                } else {
                                    "Component Selected"
                                }

                            state.selectedConnectionId != null ->
                                if (arabic) {
                                    "وصلة محددة"
                                } else {
                                    "Connection Selected"
                                }

                            else ->
                                if (arabic) {
                                    "جاهز للتصميم"
                                } else {
                                    "Ready for Design"
                                }
                        },
                        color = Color(0xFF172027),
                        fontSize = 13.sp
                    )

                    Text(
                        text = when {
                            state.connectionStartId != null ->
                                if (arabic) {
                                    "اضغط على العنصر الآخر لإكمال المسار"
                                } else {
                                    "Select the destination component to complete the feeder"
                                }

                            state.selectedNodeId != null ->
                                if (arabic) {
                                    "اسحب للتحريك • اضغط مرتين للتعديل"
                                } else {
                                    "Drag to move • Double tap to edit"
                                }

                            state.selectedConnectionId != null ->
                                if (arabic) {
                                    "اضغط مرتين على الوصلة لتعديل بيانات الكابل"
                                } else {
                                    "Double tap the connection to edit cable data"
                                }

                            else ->
                                if (arabic) {
                                    "أضف المصدر والمحول والقواطع واللوحات والأحمال ثم قم بالتوصيل"
                                } else {
                                    "Add sources, transformers, breakers, panels and loads, then connect them"
                                }
                        },
                        color = Color(0xFF687780),
                        fontSize = 10.sp
                    )
                }

                if (state.selectedNodeId != null) {
                    OutlinedButton(
                        onClick = {
                            state.selectedNodeId?.let { id ->
                                state.nodes
                                    .firstOrNull { it.id == id }
                                    ?.let { node ->
                                        actions.editNode(node)
                                    }
                            }
                        }
                    ) {
                        Text(
                            text = if (arabic) {
                                "خصائص"
                            } else {
                                "Properties"
                            }
                        )
                    }
                }

                if (state.selectedConnectionId != null) {
                    OutlinedButton(
                        onClick = {
                            state.selectedConnectionId?.let { id ->
                                state.connections
                                    .firstOrNull { it.id == id }
                                    ?.let { connection ->
                                        actions.editConnection(connection)
                                    }
                            }
                        }
                    ) {
                        Text(
                            text = if (arabic) {
                                "الكابل"
                            } else {
                                "Cable"
                            }
                        )
                    }
                }
            }
        }

        /* =========================================================
         * SLD CANVAS
         * ========================================================= */

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            SldCanvas(
                nodes = state.nodes,
                connections = state.connections,
                selectedNodeId = state.selectedNodeId,
                selectedConnectionId = state.selectedConnectionId,
                connectionStartId = state.connectionStartId,

                onSelectNode = { id ->
                    state.selectedNodeId = id
                    state.selectedConnectionId = null
                },

                onMoveNode = { id, dx, dy ->
                    state.nodes = state.nodes.map { node ->
                        if (node.id == id) {
                            node.copy(
                                x = maxOf(
                                    20f,
                                    node.x + dx
                                ),
                                y = maxOf(
                                    20f,
                                    node.y + dy
                                )
                            )
                        } else {
                            node
                        }
                    }

                    actions.saveProjectNetwork()
                },

                onSelectConnection = { id ->
                    state.selectedConnectionId = id
                    state.selectedNodeId = null
                    state.connectionStartId = null
                },

                onEditNode = { node ->
                    actions.editNode(node)
                },

                onEditConnection = { connection ->
                    actions.editConnection(connection)
                }
            )
        }
    }

    /* =============================================================
     * NODE DIALOG
     * ============================================================= */

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

    /* =============================================================
     * CONNECTION DIALOG
     * ============================================================= */

    if (state.showConnectionDialog) {
        SldConnectionEditorDialog(
            arabic = arabic,
            length = state.length,
            resistance = state.resistance,
            reactance = state.reactance,
            cableSize = state.cableSize,
            parallelRuns = state.parallelRuns,
            capacity = state.capacity,

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

    /* =============================================================
     * ENGINEERING REPORT
     * ============================================================= */

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

/* =================================================================
 * ADD COMPONENT MENU
 * ================================================================= */

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
                imageVector = Icons.Outlined.Add,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(5.dp)
            )

            Text(
                text = if (arabic) {
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
            listOf(
                SldNodeType.SOURCE,
                SldNodeType.TRANSFORMER,
                SldNodeType.GENERATOR,
                SldNodeType.BUS,
                SldNodeType.BREAKER,
                SldNodeType.PANEL,
                SldNodeType.LOAD
            ).forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            typeLabel(
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

/* =================================================================
 * TOOL BUTTON
 * ================================================================= */

@Composable
private fun ToolButton(
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
            modifier = Modifier.width(5.dp)
        )

        Text(text)
    }
}

/* =================================================================
 * COMPONENT LABELS
 * ================================================================= */

private fun typeLabel(
    type: SldNodeType,
    arabic: Boolean
): String {
    return when (type) {
        SldNodeType.SOURCE ->
            if (arabic) "مصدر الشبكة / Utility" else "Utility Source"

        SldNodeType.TRANSFORMER ->
            if (arabic) "محول قدرة" else "Transformer"

        SldNodeType.GENERATOR ->
            if (arabic) "مولد" else "Generator"

        SldNodeType.BUS ->
            if (arabic) "قضبان Busbar" else "Busbar"

        SldNodeType.BREAKER ->
            if (arabic) "قاطع ACB / MCCB / MCB" else "Breaker"

        SldNodeType.PANEL ->
            if (arabic) "لوحة MDB / MCC / DB" else "Panel"

        SldNodeType.LOAD ->
            if (arabic) "حمل / موتور / مضخة" else "Load / Motor / Pump"
    }
}
