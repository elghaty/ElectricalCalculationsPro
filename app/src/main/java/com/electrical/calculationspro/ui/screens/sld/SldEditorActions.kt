package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldAutoLayoutEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldEngineeringFacade
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldNetwork
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class SldEditorActions(
private val state: SldEditorState,
private val language: AppLanguage? = null,
private val arabic: Boolean = language == AppLanguage.ARABIC,
private val onSave: ((SldNetwork) -> Unit)? = null
) {

private companion object {
    const val MIN_X = 40f
    const val MIN_Y = 40f
    const val GRID = 20f

    const val NODE_GAP_X = 70f
    const val NODE_GAP_Y = 50f

    const val DEFAULT_X = 700f
    const val DEFAULT_Y = 160f

    const val POSITION_STEP_X = 220f
    const val POSITION_STEP_Y = 180f

    const val DEFAULT_BUSBAR_CURRENT_A = 400.0
    const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA = 25.0
}

// ============================================================
// NETWORK
// ============================================================

private fun network(): SldNetwork {
    return SldNetwork(
        nodes = state.nodes,
        connections = state.connections
    )
}

// ============================================================
// ENGINEERING
// ============================================================

fun saveAndRecalculate() {
    recalculateEngineering()
    onSave?.invoke(network())
}

fun recalculateEngineering() {
    try {
        state.engineeringError = null

        val result: SldEngineeringPackage =
            SldEngineeringFacade.calculateComplete(
                network = network()
            )

        state.engineeringPackage = result

    } catch (e: Exception) {
        state.engineeringPackage = null

        state.engineeringError =
            e.message
                ?: if (arabic) {
                    "فشل تحديث الحسابات الهندسية."
                } else {
                    "Engineering calculation failed."
                }
    }
}

// ============================================================
// PROJECT
// ============================================================

fun loadProjectNetwork() {
    recalculateEngineering()
}

// ============================================================
// COMPLETE SLD
// ============================================================

fun generateCompleteSld() {
    try {
        val generated =
            SldCompleteGenerator.generate()

        state.nodes =
            generated.nodes

        state.connections =
            generated.connections

        state.clearSelection()

        saveAndRecalculate()

    } catch (e: Exception) {
        state.engineeringError =
            e.message
                ?: if (arabic) {
                    "فشل إنشاء المخطط الكهربائي الكامل."
                } else {
                    "Failed to generate the complete SLD."
                }
    }
}

// ============================================================
// NODE CREATION
// ============================================================

fun addNode(
    type: SldNodeType
) {

    val position =
        findFreeNodePosition()

    val node =
        SldNode(
            id = createNodeId(),
            name = defaultNodeName(type),
            type = type,
            x = position.first,
            y = position.second,
            voltage = 400.0,

            loadKw =
                if (type == SldNodeType.LOAD) {
                    100.0
                } else {
                    0.0
                },

            powerFactor = 0.90,

            demandFactor =
                if (type == SldNodeType.LOAD) {
                    0.80
                } else {
                    1.0
                },

            ratedKva =
                when (type) {
                    SldNodeType.TRANSFORMER -> 500.0
                    SldNodeType.GENERATOR -> 500.0
                    SldNodeType.PANEL -> 500.0
                    else -> 0.0
                },

            transformerPercentZ =
                if (type == SldNodeType.TRANSFORMER) {
                    6.0
                } else {
                    0.0
                },

            generatorXdSubtransient =
                if (type == SldNodeType.GENERATOR) {
                    15.0
                } else {
                    0.0
                },

            sourceShortCircuitMva =
                if (type == SldNodeType.SOURCE) {
                    500.0
                } else {
                    0.0
                }
        )

    state.nodes =
        state.nodes + node

    state.selectedNodeId =
        node.id

    state.selectedConnectionId =
        null

    state.connectionStartId =
        null

    saveAndRecalculate()
}

private fun defaultNodeName(
    type: SldNodeType
): String {

    val prefix =
        when (type) {
            SldNodeType.SOURCE -> "UTILITY"
            SldNodeType.TRANSFORMER -> "TR"
            SldNodeType.GENERATOR -> "GEN"
            SldNodeType.BUS -> "BUS"
            SldNodeType.PANEL -> "MDB"
            SldNodeType.BREAKER -> "CB"
            SldNodeType.LOAD -> "LOAD"
        }

    var index = 1

    while (
        state.nodes.any {
            it.name.equals(
                "$prefix-$index",
                ignoreCase = true
            )
        }
    ) {
        index++
    }

    return "$prefix-$index"
}

// ============================================================
// NODE EDITOR
// ============================================================

fun resetNodeEditor(
    type: SldNodeType
) {

    state.editingNodeId = null
    state.nodeType = type
    state.name = ""
    state.voltage = "400"

    state.loadKw =
        if (type == SldNodeType.LOAD) {
            "100"
        } else {
            "0"
        }

    state.pf = "0.90"

    state.demand =
        if (type == SldNodeType.LOAD) {
            "0.80"
        } else {
            "1.00"
        }

    state.kva =
        when (type) {
            SldNodeType.TRANSFORMER,
            SldNodeType.GENERATOR,
            SldNodeType.PANEL -> "500"

            else -> "0"
        }

    state.transformerZ =
        if (type == SldNodeType.TRANSFORMER) {
            "6"
        } else {
            "0"
        }

    state.generatorXd =
        if (type == SldNodeType.GENERATOR) {
            "15"
        } else {
            "0"
        }

    state.sourceMva =
        if (type == SldNodeType.SOURCE) {
            "500"
        } else {
            "0"
        }

    state.showNodeDialog = true
}

// ============================================================
// FREE POSITION
// ============================================================

private fun findFreeNodePosition():
    Pair<Float, Float> {

    if (state.nodes.isEmpty()) {
        return DEFAULT_X to DEFAULT_Y
    }

    val candidates =
        mutableListOf<Pair<Float, Float>>()

    for (row in 0..16) {
        for (column in 0..16) {

            candidates +=
                (
                    DEFAULT_X +
                        column * POSITION_STEP_X
                    ) to
                    (
                        DEFAULT_Y +
                            row * POSITION_STEP_Y
                        )
        }
    }

    state.nodes.forEach { node ->

        candidates +=
            (
                node.x +
                    POSITION_STEP_X
                ) to node.y

        candidates +=
            (
                node.x -
                    POSITION_STEP_X
                ) to node.y

        candidates +=
            node.x to
                (
                    node.y +
                        POSITION_STEP_Y
                    )

        candidates +=
            node.x to
                (
                    node.y -
                        POSITION_STEP_Y
                    )
    }

    return candidates
        .asSequence()
        .map {
            snapToGrid(
                it.first,
                it.second
            )
        }
        .filter {
            isFreePosition(
                it.first,
                it.second
            )
        }
        .sortedBy {
            distanceFromWorkingArea(
                it.first,
                it.second
            )
        }
        .firstOrNull()
        ?: fallbackFreePosition()
}

private fun isFreePosition(
    x: Float,
    y: Float
): Boolean {

    return state.nodes.none { existing ->

        val dx =
            abs(
                (
                    x +
                        NODE_WIDTH / 2f
                    ) -
                    (
                        existing.x +
                            NODE_WIDTH / 2f
                        )
            )

        val dy =
            abs(
                (
                    y +
                        NODE_HEIGHT / 2f
                    ) -
                    (
                        existing.y +
                            NODE_HEIGHT / 2f
                        )
            )

        dx >=
            NODE_WIDTH +
                NODE_GAP_X ||
            dy >=
            NODE_HEIGHT +
                NODE_GAP_Y
    }
}

private fun fallbackFreePosition():
    Pair<Float, Float> {

    var x = DEFAULT_X
    var y = DEFAULT_Y
    var attempts = 0

    while (
        attempts < 1000 &&
        !isFreePosition(
            x,
            y
        )
    ) {

        x += POSITION_STEP_X

        if (x > 4000f) {
            x = DEFAULT_X
            y += POSITION_STEP_Y
        }

        attempts++
    }

    return snapToGrid(
        x,
        y
    )
}

private fun distanceFromWorkingArea(
    x: Float,
    y: Float
): Float {

    val dx =
        x -
            DEFAULT_X

    val dy =
        y -
            DEFAULT_Y

    return sqrt(
        dx * dx +
            dy * dy
    )
}

private fun snapToGrid(
    x: Float,
    y: Float
): Pair<Float, Float> {

    fun snap(
        value: Float
    ): Float {
        return (
            value /
                GRID
            ).toInt() *
            GRID
    }

    return maxOf(
        MIN_X,
        snap(x)
    ) to
        maxOf(
            MIN_Y,
            snap(y)
        )
}

// ============================================================
// NODE EDIT
// ============================================================

fun editNode(
    node: SldNode
) {

    state.editingNodeId = node.id
    state.name = node.name
    state.nodeType = node.type
    state.voltage = node.voltage.toEngineeringString()
    state.loadKw = node.loadKw.toEngineeringString()
    state.pf = node.powerFactor.toEngineeringString()
    state.demand = node.demandFactor.toEngineeringString()
    state.kva = node.ratedKva.toEngineeringString()
    state.transformerZ =
        node.transformerPercentZ.toEngineeringString()

    state.generatorXd =
        node.generatorXdSubtransient.toEngineeringString()

    state.sourceMva =
        node.sourceShortCircuitMva.toEngineeringString()

    state.showNodeDialog = true
}

fun saveNode() {

    val editingId =
        state.editingNodeId

    val existing =
        editingId?.let { id ->
            state.nodes.firstOrNull {
                it.id == id
            }
        }

    val voltage =
        state.voltage
            .toDoubleOrNull()
            ?.takeIf {
                it > 0.0
            }
            ?: existing?.voltage
            ?: 400.0

    val loadKw =
        state.loadKw
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.loadKw
            ?: 0.0

    val pf =
        state.pf
            .toDoubleOrNull()
            ?.coerceIn(
                0.50,
                1.00
            )
            ?: existing?.powerFactor
            ?: 0.90

    val demand =
        state.demand
            .toDoubleOrNull()
            ?.coerceIn(
                0.0,
                1.0
            )
            ?: existing?.demandFactor
            ?: 1.0

    val kva =
        state.kva
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.ratedKva
            ?: 0.0

    val transformerZ =
        state.transformerZ
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.transformerPercentZ
            ?: 0.0

    val generatorXd =
        state.generatorXd
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.generatorXdSubtransient
            ?: 0.0

    val sourceMva =
        state.sourceMva
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.sourceShortCircuitMva
            ?: 0.0

    val cleanName =
        state.name
            .trim()
            .ifBlank {
                existing?.name
                    ?: defaultNodeName(
                        state.nodeType
                    )
            }

    if (existing == null) {

        val position =
            findFreeNodePosition()

        val node =
            SldNode(
                id =
                    editingId
                        ?: createNodeId(),

                name =
                    cleanName,

                type =
                    state.nodeType,

                x =
                    position.first,

                y =
                    position.second,

                voltage =
                    voltage,

                loadKw =
                    if (
                        state.nodeType ==
                        SldNodeType.LOAD
                    ) {
                        loadKw
                    } else {
                        0.0
                    },

                powerFactor =
                    pf,

                demandFactor =
                    demand,

                ratedKva =
                    kva,

                transformerPercentZ =
                    transformerZ,

                generatorXdSubtransient =
                    generatorXd,

                sourceShortCircuitMva =
                    sourceMva
            )

        state.nodes =
            state.nodes +
                node

        state.selectedNodeId =
            node.id

    } else {

        val updated =
            existing.copy(
                name = cleanName,
                type = state.nodeType,
                voltage = voltage,

                loadKw =
                    if (
                        state.nodeType ==
                        SldNodeType.LOAD
                    ) {
                        loadKw
                    } else {
                        0.0
                    },

                powerFactor = pf,
                demandFactor = demand,
                ratedKva = kva,
                transformerPercentZ = transformerZ,
                generatorXdSubtransient = generatorXd,
                sourceShortCircuitMva = sourceMva
            )

        state.nodes =
            state.nodes.map {
                if (
                    it.id ==
                    existing.id
                ) {
                    updated
                } else {
                    it
                }
            }

        state.connections =
            state.connections.map { connection ->

                if (
                    connection.connectionType ==
                    SldConnectionType.BUSBAR &&
                    (
                        connection.fromNodeId ==
                            existing.id ||
                            connection.toNodeId ==
                            existing.id
                        )
                ) {

                    connection.copy(
                        connectionType =
                            SldConnectionType.CABLE,

                        busbarRatedCurrentA =
                            0.0,

                        busbarShortCircuitKA =
                            0.0
                    )

                } else {
                    connection
                }
            }

        state.selectedNodeId =
            updated.id
    }

    state.showNodeDialog =
        false

    state.editingNodeId =
        null

    state.connectionStartId =
        null

    saveAndRecalculate()
}

// ============================================================
// NODE MOVEMENT
// ============================================================

fun moveNode(
    nodeId: String,
    deltaX: Float,
    deltaY: Float
) {

    val node =
        state.nodes.firstOrNull {
            it.id == nodeId
        }
            ?: return

    val x =
        maxOf(
            MIN_X,
            node.x + deltaX
        )

    val y =
        maxOf(
            MIN_Y,
            node.y + deltaY
        )

    state.nodes =
        state.nodes.map {

            if (
                it.id ==
                nodeId
            ) {

                it.copy(
                    x = x,
                    y = y
                )

            } else {
                it
            }
        }
}

fun moveNodeEnd() {
    saveAndRecalculate()
}

// ============================================================
// CONNECTION CREATION
// ============================================================

fun startOrCompleteConnection() {

    state.selectedNodeId?.let {
        startOrCompleteConnection(it)
    }
}

fun startOrCompleteConnection(
    nodeId: String
) {

    val target =
        state.nodes.firstOrNull {
            it.id == nodeId
        }
            ?: return

    val startId =
        state.connectionStartId

    if (startId == null) {

        state.selectedNodeId =
            target.id

        state.selectedConnectionId =
            null

        state.connectionStartId =
            target.id

        state.engineeringError =
            null

        return
    }

    if (
        startId ==
        target.id
    ) {
        return
    }

    val from =
        state.nodes.firstOrNull {
            it.id == startId
        }
            ?: run {
                state.connectionStartId = null
                return
            }

    val to =
        target

    if (
        state.connections.any {
            it.fromNodeId == from.id &&
                it.toNodeId == to.id
        }
    ) {

        state.engineeringError =
            if (arabic) {
                "هذا الاتصال موجود بالفعل."
            } else {
                "This connection already exists."
            }

        state.connectionStartId =
            null

        return
    }

    val type =
        automaticConnectionType(
            from,
            to
        )

    state.selectedNodeId =
        to.id

    state.selectedConnectionId =
        null

    if (
        type ==
        SldConnectionType.BUSBAR
    ) {

        createBusbarConnection(
            from,
            to
        )

        state.connectionStartId =
            null

        saveAndRecalculate()

        return
    }

    state.editingConnectionId =
        null

    state.connectionType =
        SldConnectionType.CABLE.name

    state.length =
        "50"

    state.resistance =
        "0.125"

    state.reactance =
        "0.080"

    state.cableSize =
        "240"

    state.parallelRuns =
        "1"

    state.capacity =
        "350"

    state.showConnectionDialog =
        true
}

private fun createBusbarConnection(
    from: SldNode,
    to: SldNode
) {

    val ratedCurrent =
        estimateBusbarCurrent(
            from
        )

    val connection =
        SldConnection(
            id = createConnectionId(),
            fromNodeId = from.id,
            toNodeId = to.id,
            connectionType = SldConnectionType.BUSBAR,
            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,
            currentCapacityA = ratedCurrent,
            conductorMaterial = state.conductorMaterial,
            insulationType = state.insulationType,
            installationMethodCode = state.installationMethodCode,
            busbarMaterial = state.busbarMaterial,
            busbarRatedCurrentA = ratedCurrent,
            busbarShortCircuitKA =
                DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
        )

    state.connections =
        state.connections +
            connection
}

// ============================================================
// CONNECTION EDIT
// ============================================================

fun editConnection(
    connection: SldConnection
) {

    state.editingConnectionId =
        connection.id

    state.connectionType =
        connection.connectionType.name

    state.length =
        connection.lengthMeters.toEngineeringString()

    state.resistance =
        connection.resistanceOhmPerKm.toEngineeringString()

    state.reactance =
        connection.reactanceOhmPerKm.toEngineeringString()

    state.cableSize =
        connection.cableSizeMm2.toEngineeringString()

    state.parallelRuns =
        connection.parallelRuns.toString()

    state.capacity =
        connection.currentCapacityA.toEngineeringString()

    state.conductorMaterial =
        connection.conductorMaterial

    state.insulationType =
        connection.insulationType

    state.installationMethodCode =
        connection.installationMethodCode

    state.busbarMaterial =
        connection.busbarMaterial

    state.busbarRatedCurrent =
        connection.busbarRatedCurrentA.toEngineeringString()

    state.busbarShortCircuit =
        connection.busbarShortCircuitKA.toEngineeringString()

    state.showConnectionDialog =
        true
}

fun saveConnection() {

    val editingId =
        state.editingConnectionId

    val existing =
        editingId?.let { id ->
            state.connections.firstOrNull {
                it.id == id
            }
        }

    val type =
        try {
            SldConnectionType.valueOf(
                state.connectionType.uppercase(
                    Locale.US
                )
            )
        } catch (_: Exception) {
            SldConnectionType.CABLE
        }

    val length =
        state.length
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val resistance =
        state.resistance
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val reactance =
        state.reactance
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val cableSize =
        state.cableSize
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val runs =
        state.parallelRuns
            .toIntOrNull()
            ?.coerceAtLeast(1)
            ?: 1

    val capacity =
        state.capacity
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val busbarRated =
        state.busbarRatedCurrent
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val busbarShortCircuit =
        state.busbarShortCircuit
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    if (existing != null) {

        val updated =
            existing.copy(

                connectionType =
                    type,

                lengthMeters =
                    if (
                        type ==
                        SldConnectionType.CABLE
                    ) {
                        length
                    } else {
                        0.0
                    },

                resistanceOhmPerKm =
                    if (
                        type ==
                        SldConnectionType.CABLE
                    ) {
                        resistance
                    } else {
                        0.0
                    },

                reactanceOhmPerKm =
                    if (
                        type ==
                        SldConnectionType.CABLE
                    ) {
                        reactance
                    } else {
                        0.0
                    },

                cableSizeMm2 =
                    if (
                        type ==
                        SldConnectionType.CABLE
                    ) {
                        cableSize
                    } else {
                        0.0
                    },

                parallelRuns =
                    if (
                        type ==
                        SldConnectionType.CABLE
                    ) {
                        runs
                    } else {
                        1
                    },

                currentCapacityA =
                    if (
                        type ==
                        SldConnectionType.CABLE
                    ) {
                        capacity
                    } else {
                        busbarRated
                    },

                conductorMaterial =
                    state.conductorMaterial,

                insulationType =
                    state.insulationType,

                installationMethodCode =
                    state.installationMethodCode,

                busbarMaterial =
                    state.busbarMaterial,

                busbarRatedCurrentA =
                    if (
                        type ==
                        SldConnectionType.BUSBAR
                    ) {
                        busbarRated
                    } else {
                        0.0
                    },

                busbarShortCircuitKA =
                    if (
                        type ==
                        SldConnectionType.BUSBAR
                    ) {
                        busbarShortCircuit
                    } else {
                        0.0
                    }
            )

        state.connections =
            state.connections.map {

                if (
                    it.id ==
                    existing.id
                ) {
                    updated
                } else {
                    it
                }
            }

        state.selectedConnectionId =
            existing.id

    } else {

        val startId =
            state.connectionStartId

        val endId =
            state.selectedNodeId

        val from =
            startId?.let { id ->
                state.nodes.firstOrNull {
                    it.id == id
                }
            }

        val to =
            endId?.let { id ->
                state.nodes.firstOrNull {
                    it.id == id
                }
            }

        if (
            from == null ||
            to == null ||
            from.id == to.id
        ) {

            state.engineeringError =
                if (arabic) {
                    "حدد عنصر البداية والنهاية للاتصال."
                } else {
                    "Select valid connection start and end nodes."
                }

            return
        }

        if (
            automaticConnectionType(
                from,
                to
            ) ==
            SldConnectionType.BUSBAR
        ) {

            state.engineeringError =
                if (arabic) {
                    "اتصال اللوحة بالقاطع الداخلي يجب أن يكون BUSBAR."
                } else {
                    "Panel-to-breaker internal connection must be BUSBAR."
                }

            state.showConnectionDialog =
                false

            state.connectionStartId =
                null

            return
        }

        val connection =
            SldConnection(
                id = createConnectionId(),
                fromNodeId = from.id,
                toNodeId = to.id,
                connectionType = SldConnectionType.CABLE,
                lengthMeters = length,
                resistanceOhmPerKm = resistance,
                reactanceOhmPerKm = reactance,
                cableSizeMm2 = cableSize,
                parallelRuns = runs,
                voltageDropPercent = 0.0,
                currentCapacityA = capacity,
                conductorMaterial = state.conductorMaterial,
                insulationType = state.insulationType,
                installationMethodCode =
                    state.installationMethodCode,
                busbarMaterial = state.busbarMaterial,
                busbarRatedCurrentA = 0.0,
                busbarShortCircuitKA = 0.0
            )

        state.connections =
            state.connections +
                connection

        state.selectedConnectionId =
            connection.id
    }

    state.clearDialogs()

    saveAndRecalculate()
}

fun cancelConnectionEdit() {

    state.showConnectionDialog =
        false

    state.editingConnectionId =
        null

    state.connectionStartId =
        null

    state.selectedConnectionId =
        null

    state.engineeringError =
        null
}

// ============================================================
// DELETE
// ============================================================

fun deleteSelected() {

    val selectedNode =
        state.selectedNodeId

    val selectedConnection =
        state.selectedConnectionId

    if (selectedNode != null) {

        state.nodes =
            state.nodes.filterNot {
                it.id == selectedNode
            }

        state.connections =
            state.connections.filter {
                it.fromNodeId != selectedNode &&
                    it.toNodeId != selectedNode
            }
    }

    if (selectedConnection != null) {

        state.connections =
            state.connections.filterNot {
                it.id == selectedConnection
            }
    }

    state.clearSelection()

    saveAndRecalculate()
}

// ============================================================
// AUTO LAYOUT
// ============================================================

fun autoLayout() {

    try {

        val result =
            SldAutoLayoutEngine.arrange(
                network()
            )

        state.nodes =
            result.network.nodes

        /*
         * The layout engine changes coordinates only.
         * Connections are therefore preserved from the
         * returned network.
         */
        state.connections =
            result.network.connections

        state.clearSelection()

        saveAndRecalculate()

    } catch (e: Exception) {

        state.engineeringError =
            e.message
                ?: if (arabic) {
                    "فشل ترتيب المخطط."
                } else {
                    "Auto layout failed."
                }
    }
}

// ============================================================
// SHORT CIRCUIT
// ============================================================

fun runShortCircuit() {

    try {

        val study =
            SldEngineeringFacade.calculateShortCircuit(
                network()
            )

        state.reportTitle =
            if (arabic) {
                "دراسة القصر الكهربائي"
            } else {
                "Short Circuit Study"
            }

        state.reportText =
            study.toString()

        state.showReport =
            true

        state.engineeringError =
            null

    } catch (e: Exception) {

        state.engineeringError =
            e.message
                ?: if (arabic) {
                    "فشل حساب القصر الكهربائي."
                } else {
                    "Short-circuit study failed."
                }
    }
}

// ============================================================
// PANEL SCHEDULE
// ============================================================

fun runPanelSchedule() {

    val panel =
        state.nodes.firstOrNull {
            it.type ==
                SldNodeType.PANEL
        }

    if (panel == null) {

        state.engineeringError =
            if (arabic) {
                "لا توجد لوحة PANEL في المخطط."
            } else {
                "No PANEL exists in the SLD."
            }

        return
    }

    try {

        val result =
            SldEngineeringFacade.calculateComplete(
                network = network(),
                panelNodeId = panel.id
            )

        state.engineeringPackage =
            result

        state.reportTitle =
            if (arabic) {
                "جدول اللوحة"
            } else {
                "Panel Schedule"
            }

        state.reportText =
            result.panelSchedule
                ?.toString()
                ?: if (arabic) {
                    "لم يتم إنشاء جدول اللوحة."
                } else {
                    "Panel schedule was not generated."
                }

        state.showReport =
            true

        state.engineeringError =
            null

    } catch (e: Exception) {

        state.engineeringError =
            e.message
                ?: if (arabic) {
                    "فشل إنشاء جدول اللوحة."
                } else {
                    "Panel schedule failed."
                }
    }
}

// ============================================================
// HELPERS
// ============================================================

private fun automaticConnectionType(
    from: SldNode,
    to: SldNode
): SldConnectionType {

    return if (
        from.type ==
            SldNodeType.PANEL &&
        to.type ==
            SldNodeType.BREAKER
    ) {
        SldConnectionType.BUSBAR
    } else {
        SldConnectionType.CABLE
    }
}

private fun estimateBusbarCurrent(
    panel: SldNode
): Double {

    if (
        panel.ratedKva > 0.0 &&
        panel.voltage > 0.0
    ) {

        val current =
            panel.ratedKva *
                1000.0 /
                (
                    sqrt(3.0) *
                        panel.voltage
                    )

        if (
            current.isFinite() &&
            current > 0.0
        ) {

            return current.coerceAtLeast(
                DEFAULT_BUSBAR_CURRENT_A
            )
        }
    }

    return DEFAULT_BUSBAR_CURRENT_A
}

private fun createNodeId(): String {

    var id =
        "node-${System.nanoTime()}"

    while (
        state.nodes.any {
            it.id == id
        }
    ) {
        id =
            "node-${System.nanoTime()}"
    }

    return id
}

private fun createConnectionId(): String {

    var id =
        "connection-${System.nanoTime()}"

    while (
        state.connections.any {
            it.id == id
        }
    ) {
        id =
            "connection-${System.nanoTime()}"
    }

    return id
}

private fun Double.toEngineeringString():
    String {

    return String.format(
        Locale.US,
        "%.2f",
        this
    )
}
