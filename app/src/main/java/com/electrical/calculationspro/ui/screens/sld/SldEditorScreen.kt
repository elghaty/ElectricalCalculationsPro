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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070C10))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151E26))
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            onBack?.let {

                IconButton(
                    onClick = it
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (arabic)
                            "مصمم الشبكة الكهربائية"
                        else
                            "Electrical Network Designer",
                    color = Color.White,
                    fontSize = 18.sp
                )

                Text(
                    text =
                        if (arabic)
                            "Single Line Diagram • Engineering Network"
                        else
                            "Single Line Diagram • Engineering Network",
                    color = Color(0xFF8D9DA7),
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = {
                    actions.generateCompleteSld()
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF101820))
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
                icon = Icons.Outlined.Link,
                text =
                    if (state.connectionStartId == null)
                        if (arabic)
                            "توصيل"
                        else
                            "Connect"
                    else
                        if (arabic)
                            "اختر الطرف الآخر"
                        else
                            "Select End",
                onClick = {
                    actions.startOrCompleteConnection()
                }
            )

            ToolButton(
                icon = Icons.Outlined.Delete,
                text =
                    if (arabic)
                        "حذف"
                    else
                        "Delete",
                onClick = {
                    actions.deleteSelected()
                }
            )

            ToolButton(
                icon = Icons.Outlined.Calculate,
                text =
                    if (arabic)
                        "القصر"
                    else
                        "Short Circuit",
                onClick = {
                    actions.runShortCircuit()
                }
            )

            ToolButton(
                icon = Icons.Outlined.TableView,
                text =
                    if (arabic)
                        "جدول اللوحة"
                    else
                        "Panel Schedule",
                onClick = {
                    actions.runPanelSchedule()
                }
            )

            ToolButton(
                icon = Icons.Outlined.Settings,
                text =
                    if (arabic)
                        "SLD كامل"
                    else
                        "Complete SLD",
                onClick = {
                    actions.generateCompleteSld()
                }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111A21)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            if (state.selectedNodeId != null)
                                if (arabic)
                                    "عنصر محدد"
                                else
                                    "Selected Component"
                            else if (
                                state.selectedConnectionId != null
                            )
                                if (arabic)
                                    "وصلة محددة"
                                else
                                    "Selected Connection"
                            else
                                if (arabic)
                                    "جاهز للتصميم"
                                else
                                    "Ready for Design",
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Text(
                        text =
                            when {
                                state.connectionStartId != null ->
                                    if (arabic)
                                        "اختر العنصر الذي سيتم توصيله"
                                    else
                                        "Select the destination component"

                                state.selectedNodeId != null ->
                                    if (arabic)
                                        "اضغط مرتين على العنصر لتحرير بياناته"
                                    else
                                        "Double tap the component to edit"

                                else ->
                                    if (arabic)
                                        "أضف العناصر ثم استخدم Connect لربط الشبكة"
                                    else
                                        "Add components, then use Connect to build the network"
                            },
                        color = Color(0xFF8FA0AA),
                        fontSize = 10.sp
                    )
                }

                if (state.selectedNodeId != null) {

                    OutlinedButton(
                        onClick = {

                            state.selectedNodeId?.let {
                                actions.editNode(it)
                            }
                        }
                    ) {

                        Text(
                            if (arabic)
                                "Properties"
                            else
                                "Properties"
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {

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
                if (arabic)
                    "إضافة عنصر"
                else
                    "Add Component"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

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

                DropdownMenuItem(
                    text = {
                        Text(
                            typeLabel(
                                type,
                                arabic
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
            modifier = Modifier.width(5.dp)
        )

        Text(text)
    }
}

private fun typeLabel(
    type: SldNodeType,
    arabic: Boolean
): String {

    return when (type) {

        SldNodeType.SOURCE ->
            if (arabic) "مصدر تغذية" else "Utility Source"

        SldNodeType.TRANSFORMER ->
            if (arabic) "محول" else "Transformer"

        SldNodeType.GENERATOR ->
            if (arabic) "مولد" else "Generator"

        SldNodeType.BUS ->
            if (arabic) "قضبان Busbar" else "Busbar"

        SldNodeType.BREAKER ->
            if (arabic) "قاطع" else "Breaker"

        SldNodeType.PANEL ->
            if (arabic) "لوحة" else "Panel"

        SldNodeType.LOAD ->
            if (arabic) "حمل" else "Load"
    }
}
