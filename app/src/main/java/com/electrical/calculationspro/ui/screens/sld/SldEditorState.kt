package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType

class SldEditorState {

    var nodes by mutableStateOf(
        listOf(
            SldNode(
                id = "source-1",
                name = "UTILITY SOURCE",
                type = SldNodeType.SOURCE,
                x = 900f,
                y = 60f,
                voltage = 400.0,
                sourceShortCircuitMva = 500.0
            )
        )
    )

    var connections by mutableStateOf(
        emptyList<SldConnection>()
    )

    var selectedNodeId by mutableStateOf<String?>(null)
    var selectedConnectionId by mutableStateOf<String?>(null)
    var connectionStartId by mutableStateOf<String?>(null)

    var editingNodeId by mutableStateOf<String?>(null)
    var editingConnectionId by mutableStateOf<String?>(null)

    var showNodeDialog by mutableStateOf(false)
    var showConnectionDialog by mutableStateOf(false)
    var showReport by mutableStateOf(false)

    var reportTitle by mutableStateOf("")
    var reportText by mutableStateOf("")

    var nodeType by mutableStateOf(SldNodeType.BUS)

    var name by mutableStateOf("")
    var voltage by mutableStateOf("400")
    var loadKw by mutableStateOf("100")
    var pf by mutableStateOf("0.90")
    var demand by mutableStateOf("0.80")
    var kva by mutableStateOf("500")
    var transformerZ by mutableStateOf("6")
    var generatorXd by mutableStateOf("15")
    var sourceMva by mutableStateOf("500")

    var length by mutableStateOf("50")
    var resistance by mutableStateOf("0.125")
    var reactance by mutableStateOf("0.080")
    var cableSize by mutableStateOf("240")
    var parallelRuns by mutableStateOf("1")
    var capacity by mutableStateOf("350")

    fun clearSelection() {
        selectedNodeId = null
        selectedConnectionId = null
        connectionStartId = null
    }

    fun clearDialogs() {
        showNodeDialog = false
        showConnectionDialog = false
        editingNodeId = null
        editingConnectionId = null
        connectionStartId = null
    }
}
