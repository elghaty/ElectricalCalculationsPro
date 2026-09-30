package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldNetwork
import kotlin.math.sqrt
import java.util.Locale

class SldEditorActions(
private val state: SldEditorState,
private val arabic: Boolean = false,
private val onSave: ((SldNetwork) -> Unit)? = null
) {

private companion object {
    const val MIN_X = 40f
    const val MIN_Y = 40f
    const val GRID = 20f
    const val NODE_GAP = 70f
    const val DEFAULT_X = 700f
    const val DEFAULT_Y = 160f

    const val DEFAULT_BUSBAR_CURRENT_A = 400.0
    const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA = 25.0
}

// ============================================================
// NETWORK
// ============================================================

private fun network(): SldNetwork =
    SldNetwork(
        nodes = state.nodes,
        connections = state.connections
    )

private fun saveAndRecalculate() {
    recalculateEngineering()
    onSave?.invoke(network())
}

fun recalculateEngineering() {
    try {
        state.engineeringError = null

        val result =
            SldUpstreamEngineering.calculate(
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
// NODE CREATION
// ============================================================

fun addNode(type: SldNodeType) {

    val position =
        findFreeNodePosition()

    val node =
        SldNode(
            id = createNodeId(),
            name = defaultNodeName(type),
            type = type,
            x = position.first,
            y = position.second,
            voltage =
                if (
                    type == SldNodeType.SOURCE
                ) {
                    400.0
                } else {
                    400.0
                },
            loadKw =
                if (
                    type == SldNodeType.LOAD
                ) {
                    100.0
                } else {
                    0.0
                },
            powerFactor =
                0.90,
            demandFactor =
                if (
                    type == SldNodeType.LOAD
                ) {
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
                if (
                    type ==
                        SldNodeType.TRANSFORMER
                ) {
                    6.0
                } else {
                    0.0
                },
            generatorXdSubtransient =
                if (
                    type ==
                        SldNodeType.GENERATOR
                ) {
                    15.0
                } else {
                    0.0
                },
            sourceShortCircuitMva =
                if (
                    type ==
                        SldNodeType.SOURCE
                ) {
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

/*
 * Find a genuinely free position instead of using one
 * hard-coded position for every newly created element.
 */
private fun findFreeNodePosition(): Pair<Float, Float> {

    if (state.nodes.isEmpty()) {
        return DEFAULT_X to DEFAULT_Y
    }

    val candidates =
        mutableListOf<Pair<Float, Float>>()

    /*
     * First try a grid around the visible working area.
     */
    for (row in 0..14) {
        for (column in 0..14) {

            candidates +=
                (
                    DEFAULT_X +
                        column * 220f
                    ) to
                    (
                        DEFAULT_Y +
                            row * 180f
                        )
        }
    }

    /*
     * Then add positions around existing nodes.
     * This makes the algorithm useful even when the user
     * has moved the SLD far away from the initial area.
     */
    state.nodes.forEach { node ->

        candidates +=
            (node.x + 220f) to node.y

        candidates +=
            (node.x - 220f) to node.y

        candidates +=
            node.x to
                (node.y + 180f)

        candidates +=
            node.x to
                (node.y - 180f)
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
            absDistance(
                x + NODE_WIDTH / 2f,
                existing.x +
                    NODE_WIDTH / 2f
            )

        val dy =
            absDistance(
                y + NODE_HEIGHT / 2f,
                existing.y +
                    NODE_HEIGHT / 2f
            )

        dx <
            NODE_WIDTH +
                NODE_GAP &&
            dy <
                NODE_HEIGHT +
                NODE_GAP
    }
}

private fun fallbackFreePosition():
    Pair<Float, Float> {

    var x =
        DEFAULT_X

    var y =
        DEFAULT_Y

    var attempts = 0

    while (
        attempts < 500 &&
        !isFreePosition(
            x,
            y
        )
    ) {

        x += 240f

        if (x > 2600f) {
            x = DEFAULT_X
            y += 200f
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
        x - DEFAULT_X

    val dy =
        y - DEFAULT_Y

    return sqrt(
        dx * dx +
            dy * dy
    )
}

private fun snapToGrid(
    x: Float,
    y: Float
): Pair<Float, Float> {

    fun snap(value: Float): Float =
        (
            value /
                GRID
            ).toInt() *
            GRID

    return maxOf(
        MIN_X,
        snap(x)
    ) to
        maxOf(
            MIN_Y,
            snap(y)
        )
}

private fun absDistance(
    a: Float,
    b: Float
): Float =
    kotlin.math.abs(
        a - b
    )

// ============================================================
// NODE EDIT
// ============================================================

fun editNode(
    node: SldNode
) {

    state.editingNodeId =
        node.id

    state.name =
        node.name

    state.nodeType =
        node.type

    state.voltage =
        node.voltage.toEngineeringString()

    state.loadKw =
        node.loadKw.toEngineeringString()

    state.pf =
        node.powerFactor.toEngineeringString()

    state.demand =
        node.demandFactor.toEngineeringString()

    state.kva =
        node.ratedKva.toEngineeringString()

    state.transformerZ =
        node.transformerPercentZ.toEngineeringString()

    state.generatorXd =
        node.generatorXdSubtransient
            .toEngineeringString()

    state.sourceMva =
        node.sourceShortCircuitMva
            .toEngineeringString()

    state.showNodeDialog =
        true
}

fun saveNode() {

    val id =
        state.editingNodeId

    val existing =
        id?.let {
            state.nodes.firstOrNull {
                it.id == id
            }
        }

    val parsedVoltage =
        state.voltage.toDoubleOrNull()
            ?.takeIf {
                it > 0.0
            }
            ?: existing?.voltage
            ?: 400.0

    val parsedLoad =
        state.loadKw.toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.loadKw
            ?: 0.0

    val parsedPf =
        state.pf.toDoubleOrNull()
            ?.coerceIn(
                0.50,
                1.00
            )
            ?: existing?.powerFactor
            ?: 0.90

    val parsedDemand =
        state.demand.toDoubleOrNull()
            ?.coerceIn(
                0.0,
                1.0
            )
            ?: existing?.demandFactor
            ?: 1.0

    val parsedKva =
        state.kva.toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.ratedKva
            ?: 0.0

    val parsedZ =
        state.transformerZ
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.transformerPercentZ
            ?: 0.0

    val parsedXd =
        state.generatorXd
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: existing?.generatorXdSubtransient
            ?: 0.0

    val parsedMva =
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
                    id
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
                    parsedVoltage,
                loadKw =
                    if (
                        state.nodeType ==
                            SldNodeType.LOAD
                    ) {
                        parsedLoad
                    } else {
                        0.0
                    },
                powerFactor =
                    parsedPf,
                demandFactor =
                    parsedDemand,
                ratedKva =
                    parsedKva,
                transformerPercentZ =
                    parsedZ,
                generatorXdSubtransient =
                    parsedXd,
                sourceShortCircuitMva =
                    parsedMva
            )

        state.nodes =
            state.nodes + node

        state.selectedNodeId =
            node.id

    } else {

        val updated =
            existing.copy(
                name = cleanName,
                type = state.nodeType,
                voltage = parsedVoltage,
                loadKw =
                    if (
                        state.nodeType ==
                            SldNodeType.LOAD
                    ) {
                        parsedLoad
                    } else {
                        0.0
                    },
                powerFactor = parsedPf,
                demandFactor = parsedDemand,
                ratedKva = parsedKva,
                transformerPercentZ = parsedZ,
                generatorXdSubtransient = parsedXd,
                sourceShortCircuitMva = parsedMva
            )

        state.nodes =
            state.nodes.map {
                if (it.id == existing.id) {
                    updated
                } else {
                    it
                }
            }

        /*
         * If a node changes from PANEL to another type,
         * previously generated BUSBAR links are converted
         * to normal cable links. This prevents an invalid
         * internal busbar from surviving a type edit.
         */
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

    val newX =
        maxOf(
            MIN_X,
            node.x +
                deltaX
        )

    val newY =
        maxOf(
            MIN_Y,
            node.y +
                deltaY
        )

    /*
     * During normal dragging we do NOT automatically move
     * other nodes. The user's manual position is authoritative.
     */
    state.nodes =
        state.nodes.map {
            if (it.id == nodeId) {
                it.copy(
                    x = newX,
                    y = newY
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

fun startOrCompleteConnection(
    nodeId: String
) {

    val node =
        state.nodes.firstOrNull {
            it.id == nodeId
        }
        ?: return

    val start =
        state.connectionStartId

    if (start == null) {

        state.selectedNodeId =
            node.id

        state.selectedConnectionId =
            null

        state.connectionStartId =
            node.id

        state.engineeringError =
            null

        return
    }

    if (start == node.id) {
        return
    }

    val from =
        state.nodes.firstOrNull {
            it.id == start
        }
        ?: run {
            state.connectionStartId =
                null
            return
        }

    val to =
        node

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

    /*
     * Never silently reverse the user's selected direction.
     * The stored from -> to direction is the engineering flow.
     */
    val finalFrom =
        from

    val finalTo =
        to

    val type =
        automaticConnectionType(
            finalFrom,
            finalTo
        )

    state.selectedNodeId =
        finalTo.id

    state.selectedConnectionId =
        null

    if (
        type ==
            SldConnectionType.BUSBAR
    ) {

        createBusbarConnection(
            finalFrom,
            finalTo
        )

        state.connectionStartId =
            null

        saveAndRecalculate()

        return
    }

    /*
     * Cable properties are collected before the connection
     * is committed to state.
     */
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
            id =
                createConnectionId(),
            fromNodeId =
                from.id,
            toNodeId =
                to.id,
            connectionType =
                SldConnectionType.BUSBAR,
            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,
            currentCapacityA =
                ratedCurrent,
            conductorMaterial =
                state.conductorMaterial,
            insulationType =
                state.insulationType,
            installationMethodCode =
                state.installationMethodCode,
            busbarMaterial =
                state.busbarMaterial,
            busbarRatedCurrentA =
                ratedCurrent,
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
        connection.lengthMeters
            .toEngineeringString()

    state.resistance =
        connection.resistanceOhmPerKm
            .toEngineeringString()

    state.reactance =
        connection.reactanceOhmPerKm
            .toEngineeringString()

    state.cableSize =
        connection.cableSizeMm2
            .toEngineeringString()

    state.parallelRuns =
        connection.parallelRuns
            .toString()

    state.capacity =
        connection.currentCapacityA
            .toEngineeringString()

    state.conductorMaterial =
        connection.conductorMaterial

    state.insulationType =
        connection.insulationType

    state.installationMethodCode =
        connection.installationMethodCode

    state.busbarMaterial =
        connection.busbarMaterial

    state.busbarRatedCurrent =
        connection.busbarRatedCurrentA
            .toEngineeringString()

    state.busbarShortCircuit =
        connection.busbarShortCircuitKA
            .toEngineeringString()

    state.showConnectionDialog =
        true
}

fun saveConnection() {

    val editingId =
        state.editingConnectionId

    val existing =
        editingId?.let {
            state.connections.firstOrNull {
                it.id == editingId
            }
        }

    val type =
        try {
            SldConnectionType.valueOf(
                state.connectionType
                    .uppercase(
                        Locale.US
                    )
            )
        } catch (_: Exception) {
            SldConnectionType.CABLE
        }

    val from =
        existing?.let {
            state.nodes.firstOrNull {
                it.id ==
                    existing.fromNodeId
            }
        }

    val to =
        existing?.let {
            state.nodes.firstOrNull {
                it.id ==
                    existing.toNodeId
            }
        }

    if (
        existing != null &&
        (
            from == null ||
                to == null
            )
    ) {

        state.engineeringError =
            if (arabic) {
                "العناصر المرتبطة بالاتصال غير موجودة."
            } else {
                "Connection nodes no longer exist."
            }

        return
    }

    val parsedLength =
        state.length
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val parsedResistance =
        state.resistance
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val parsedReactance =
        state.reactance
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val parsedSize =
        state.cableSize
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val parsedRuns =
        state.parallelRuns
            .toIntOrNull()
            ?.coerceAtLeast(1)
            ?: 1

    val parsedCapacity =
        state.capacity
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val parsedBusbarRated =
        state.busbarRatedCurrent
            .toDoubleOrNull()
            ?.coerceAtLeast(0.0)
            ?: 0.0

    val parsedBusbarFault =
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
                        parsedLength
                    } else {
                        0.0
                    },
                resistanceOhmPerKm =
                    if (
                        type ==
                            SldConnectionType.CABLE
                    ) {
                        parsedResistance
                    } else {
                        0.0
                    },
                reactanceOhmPerKm =
                    if (
                        type ==
                            SldConnectionType.CABLE
                    ) {
                        parsedReactance
                    } else {
                        0.0
                    },
                cableSizeMm2 =
                    if (
                        type ==
                            SldConnectionType.CABLE
                    ) {
                        parsedSize
                    } else {
                        0.0
                    },
                parallelRuns =
                    if (
                        type ==
                            SldConnectionType.CABLE
                    ) {
                        parsedRuns
                    } else {
                        1
                    },
                currentCapacityA =
                    if (
                        type ==
                            SldConnectionType.CABLE
                    ) {
                        parsedCapacity
                    } else {
                        parsedBusbarRated
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
                        parsedBusbarRated
                    } else {
                        0.0
                    },
                busbarShortCircuitKA =
                    if (
                        type ==
                            SldConnectionType.BUSBAR
                    ) {
                        parsedBusbarFault
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

    } else {

        /*
         * A new CABLE must actually be appended.
         * This was previously the critical missing branch.
         */
        val startId =
            state.connectionStartId

        val targetId =
            state.selectedNodeId

        val finalFrom =
            startId?.let {
                state.nodes.firstOrNull {
                    node -> node.id == it
                }
            }

        val finalTo =
            targetId?.let {
                state.nodes.firstOrNull {
                    node -> node.id == it
                }
            }

        if (
            finalFrom == null ||
                finalTo == null ||
                finalFrom.id ==
                finalTo.id
        ) {

            state.engineeringError =
                if (arabic) {
                    "حدد عنصر البداية والنهاية للاتصال."
                } else {
                    "Select valid connection start and end nodes."
                }

            return
        }

        val connection =
            SldConnection(
                id =
                    createConnectionId(),
                fromNodeId =
                    finalFrom.id,
                toNodeId =
                    finalTo.id,
                connectionType =
                    SldConnectionType.CABLE,
                lengthMeters =
                    parsedLength,
                resistanceOhmPerKm =
                    parsedResistance,
                reactanceOhmPerKm =
                    parsedReactance,
                cableSizeMm2 =
                    parsedSize,
                parallelRuns =
                    parsedRuns,
                voltageDropPercent =
                    0.0,
                currentCapacityA =
                    parsedCapacity,
                conductorMaterial =
                    state.conductorMaterial,
                insulationType =
                    state.insulationType,
                installationMethodCode =
                    state.installationMethodCode,
                busbarMaterial =
                    state.busbarMaterial,
                busbarRatedCurrentA =
                    0.0,
                busbarShortCircuitKA =
                    0.0
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

    val nodeId =
        state.selectedNodeId

    val connectionId =
        state.selectedConnectionId

    if (nodeId != null) {

        state.nodes =
            state.nodes.filterNot {
                it.id == nodeId
            }

        state.connections =
            state.connections.filter {
                it.fromNodeId != nodeId &&
                    it.toNodeId != nodeId
            }
    }

    if (connectionId != null) {

        state.connections =
            state.connections.filterNot {
                it.id == connectionId
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
            SldAutoLayoutEngine.layout(
                network()
            )

        state.nodes =
            result.nodes

        state.connections =
            result.connections

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
// HELPERS
// ============================================================

private fun automaticConnectionType(
    from: SldNode,
    to: SldNode
): SldConnectionType {

    /*
     * Panel internal busbar:
     * PANEL -> BREAKER only.
     *
     * Everything else remains an external CABLE.
     */
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
    String =
    String.format(
        Locale.US,
        "%.2f",
        this
    )

}
