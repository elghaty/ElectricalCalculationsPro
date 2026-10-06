package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldAutoLayoutEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldDesignValidator
import com.electrical.calculationspro.data.SldEngineeringFacade
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldNetwork
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
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

        const val NODE_WIDTH = 180f
        const val NODE_HEIGHT = 150f

        const val NODE_GAP_X = 70f
        const val NODE_GAP_Y = 70f

        const val DEFAULT_X = 700f
        const val DEFAULT_Y = 160f

        const val POSITION_STEP_X = 220f
        const val POSITION_STEP_Y = 180f

        /*
         * Physical panel assembly geometry.
         *
         * The PANEL node is the enclosure anchor.
         * BUS and BREAKER nodes are positioned inside it.
         */
        const val ENCLOSURE_PADDING_X = 28f
        const val ENCLOSURE_TOP = 20f
        const val BREAKER_SLOT = 100f
        const val BREAKER_Y = 70f
        const val BUS_Y = 30f

        const val DEFAULT_BUSBAR_CURRENT_A = 400.0
        const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA = 25.0
    }

    private fun isPanelBreakerPair(a: SldNode, b: SldNode): Boolean =
        (a.type == SldNodeType.PANEL && b.type == SldNodeType.BREAKER) ||
            (a.type == SldNodeType.BREAKER && b.type == SldNodeType.PANEL)

    private fun isPanelBusPair(a: SldNode, b: SldNode): Boolean =
        (a.type == SldNodeType.PANEL && b.type == SldNodeType.BUS) ||
            (a.type == SldNodeType.BUS && b.type == SldNodeType.PANEL)

    private fun isBusBreakerPair(a: SldNode, b: SldNode): Boolean =
        (a.type == SldNodeType.BUS && b.type == SldNodeType.BREAKER) ||
            (a.type == SldNodeType.BREAKER && b.type == SldNodeType.BUS)

    private fun isBusbarPair(a: SldNode, b: SldNode): Boolean =
        isPanelBreakerPair(a, b) ||
            isPanelBusPair(a, b) ||
            isBusBreakerPair(a, b)

    private fun canonicalBusbarEndpoints(
        a: SldNode,
        b: SldNode
    ): Pair<SldNode, SldNode>? =
        when {
            a.type == SldNodeType.PANEL && b.type == SldNodeType.BUS -> a to b
            b.type == SldNodeType.PANEL && a.type == SldNodeType.BUS -> b to a
            a.type == SldNodeType.PANEL && b.type == SldNodeType.BREAKER -> a to b
            b.type == SldNodeType.PANEL && a.type == SldNodeType.BREAKER -> b to a
            a.type == SldNodeType.BUS && b.type == SldNodeType.BREAKER -> a to b
            b.type == SldNodeType.BUS && a.type == SldNodeType.BREAKER -> b to a
            else -> null
        }

    private fun normalizeConnection(
        connection: SldConnection,
        nodes: List<SldNode>
    ): SldConnection {
        val from = nodes.firstOrNull { it.id == connection.fromNodeId } ?: return connection
        val to = nodes.firstOrNull { it.id == connection.toNodeId } ?: return connection

        if (
            connection.connectionType != SldConnectionType.BUSBAR &&
            !isPanelBreakerPair(from, to)
        ) {
            return connection
        }

        if (!isBusbarPair(from, to)) return connection

        val endpoints = canonicalBusbarEndpoints(from, to) ?: return connection
        val panel = listOf(endpoints.first, endpoints.second)
            .firstOrNull { it.type == SldNodeType.PANEL }

        val estimated = panel?.let(::estimateBusbarCurrent)
            ?: DEFAULT_BUSBAR_CURRENT_A

        val rated = connection.busbarRatedCurrentA
            .takeIf { it.isFinite() && it > 0.0 }
            ?: estimated

        val shortCircuit = connection.busbarShortCircuitKA
            .takeIf { it.isFinite() && it > 0.0 }
            ?: DEFAULT_BUSBAR_SHORT_CIRCUIT_KA

        return connection.copy(
            fromNodeId = endpoints.first.id,
            toNodeId = endpoints.second.id,
            connectionType = SldConnectionType.BUSBAR,
            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,
            currentCapacityA = rated,
            conductorMaterial = "",
            insulationType = "",
            installationMethodCode = "",
            loadedConductors = 0,
            cableDesignation = "",
            cableManufacturer = "",
            cableModel = "",
            busbarMaterial = connection.busbarMaterial.ifBlank { "Copper" },
            busbarRatedCurrentA = rated,
            busbarShortCircuitKA = shortCircuit
        )
    }

    private fun normalizeConnections(
        nodes: List<SldNode>,
        connections: List<SldConnection>
    ): List<SldConnection> {
        val result = mutableListOf<SldConnection>()
        val seen = mutableSetOf<String>()

        connections.forEach { raw ->
            val connection = normalizeConnection(raw, nodes)
            val key =
                if (connection.connectionType == SldConnectionType.BUSBAR) {
                    val a = minOf(connection.fromNodeId, connection.toNodeId)
                    val b = maxOf(connection.fromNodeId, connection.toNodeId)
                    "$a<->$b:BUSBAR"
                } else {
                    "${connection.fromNodeId}->${connection.toNodeId}:${connection.connectionType}"
                }

            if (seen.add(key)) result += connection
        }

        return result
    }

    private fun network(): SldNetwork {
        val nodes = state.nodes.toList()
        val connections = normalizeConnections(nodes, state.connections.toList())

        if (connections != state.connections) {
            state.connections = connections
        }

        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

    private fun safePersist() {
        val callback = onSave ?: return

        try {
            callback(network())
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "تعذر حفظ التغييرات."
                    else "Unable to save the changes."
        }
    }

    fun saveAndRecalculate() {
        safePersist()
        recalculateEngineering()
    }

    fun recalculateEngineering() {
        val current =
            try {
                network()
            } catch (error: Throwable) {
                state.engineeringPackage = null
                state.engineeringError = error.message ?: "Unable to read SLD."
                return
            }

        if (current.nodes.isEmpty()) {
            state.engineeringPackage = null
            state.engineeringError = null
            return
        }

        try {
            val validation = SldDesignValidator.validate(current)

            val hardErrors = validation.errors.filterNot {
                val message = it.message.orEmpty().lowercase(Locale.US)
                message.contains("incoming") ||
                    message.contains("outgoing") ||
                    message.contains("parent") ||
                    message.contains("connection") ||
                    message.contains("connected") ||
                    message.contains("reach")
            }

            if (hardErrors.isNotEmpty()) {
                state.engineeringPackage = null
                state.engineeringError =
                    hardErrors.firstOrNull()?.message?.takeIf { it.isNotBlank() }
                return
            }

            val topology = SldEngineeringFacade.checkEngineeringTopology(current)

            if (!topology.valid) {
                state.engineeringPackage = null
                state.engineeringError = null
                return
            }

            state.engineeringPackage =
                SldEngineeringFacade.calculateComplete(network = current)

            state.engineeringError = null
        } catch (error: Throwable) {
            state.engineeringPackage = null
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "تعذر تنفيذ الدراسة الهندسية."
                    else "The engineering study could not be completed."
        }
    }

    fun loadProjectNetwork() {
        recalculateEngineering()
    }

    fun resetNodeEditor(type: SldNodeType) {
        state.editingNodeId = null
        state.nodeType = type
        state.name = ""
        state.voltage = "400"
        state.loadKw = if (type == SldNodeType.LOAD) "100" else "0"
        state.pf = "0.90"
        state.demand = if (type == SldNodeType.LOAD) "0.80" else "1.00"
        state.kva =
            when (type) {
                SldNodeType.TRANSFORMER,
                SldNodeType.GENERATOR,
                SldNodeType.PANEL -> "500"
                else -> "0"
            }
        state.transformerZ = if (type == SldNodeType.TRANSFORMER) "6" else "0"
        state.generatorXd = if (type == SldNodeType.GENERATOR) "15" else "0"
        state.sourceMva = if (type == SldNodeType.SOURCE) "500" else "0"
        state.engineeringError = null
        state.showNodeDialog = true
    }

    fun editNode(node: SldNode) {
        state.editingNodeId = node.id
        state.name = node.name
        state.nodeType = node.type
        state.voltage = node.voltage.eng()
        state.loadKw = node.loadKw.eng()
        state.pf = node.powerFactor.eng()
        state.demand = node.demandFactor.eng()
        state.kva = node.ratedKva.eng()
        state.transformerZ = node.transformerPercentZ.eng()
        state.generatorXd = node.generatorXdSubtransient.eng()
        state.sourceMva = node.sourceShortCircuitMva.eng()
        state.showNodeDialog = true
    }

    fun saveNode() {
        try {
            val editingId = state.editingNodeId
            val existing = editingId?.let { id ->
                state.nodes.firstOrNull { it.id == id }
            }

            val type = state.nodeType
            val voltage = positive(state.voltage, existing?.voltage ?: 400.0)
            val load = nonNegative(state.loadKw, existing?.loadKw ?: 0.0)
            val pf = range(state.pf, existing?.powerFactor ?: 0.90, 0.50, 1.0)
            val demand = range(state.demand, existing?.demandFactor ?: 1.0, 0.0, 1.0)
            val kva = nonNegative(state.kva, existing?.ratedKva ?: 0.0)
            val transformerZ =
                nonNegative(state.transformerZ, existing?.transformerPercentZ ?: 0.0)
            val generatorXd =
                nonNegative(
                    state.generatorXd,
                    existing?.generatorXdSubtransient ?: 0.0
                )
            val sourceMva =
                nonNegative(
                    state.sourceMva,
                    existing?.sourceShortCircuitMva ?: 0.0
                )

            val name =
                state.name.trim().ifBlank {
                    existing?.name ?: defaultNodeName(type)
                }

            if (existing == null) {
                val support =
                    if (type == SldNodeType.BREAKER) {
                        findSelectedBreakerSupport()
                    } else {
                        null
                    }

                val position =
                    if (support != null) {
                        findBreakerPosition(support)
                    } else {
                        findFreeNodePosition(type)
                    }

                val node = SldNode(
                    id = createNodeId(),
                    name = name,
                    type = type,
                    x = position.first,
                    y = position.second,
                    voltage = voltage,
                    loadKw = if (type == SldNodeType.LOAD) load else 0.0,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

                state.nodes = state.nodes + node
                state.selectedNodeId = node.id
                state.selectedConnectionId = null
                state.connectionStartId = null

                if (type == SldNodeType.BREAKER && support != null) {
                    val busbar = createBusbarConnection(support, node)

                    if (busbar != null) {
                        state.connections =
                            normalizeConnections(
                                state.nodes,
                                state.connections + busbar
                            )
                        state.selectedConnectionId = busbar.id
                    }
                }
            } else {
                val updated = existing.copy(
                    name = name,
                    type = type,
                    voltage = voltage,
                    loadKw = if (type == SldNodeType.LOAD) load else 0.0,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

                state.nodes =
                    state.nodes.map {
                        if (it.id == existing.id) updated else it
                    }

                state.selectedNodeId = updated.id
            }

            state.showNodeDialog = false
            state.editingNodeId = null
            state.connectionStartId = null
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "تعذر حفظ العنصر."
                    else "Unable to save component."
        }
    }

    private fun defaultNodeName(type: SldNodeType): String {
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
                it.name.equals("$prefix-$index", ignoreCase = true)
            }
        ) {
            index++
            if (index > 100000) break
        }

        return "$prefix-$index"
    }

    fun addNode(type: SldNodeType) {
        resetNodeEditor(type)
        saveNode()
    }

    fun addBusbar() {
        try {
            val selected =
                state.selectedNodeId?.let { id ->
                    state.nodes.firstOrNull { it.id == id }
                }

            val selectedBus =
                selected?.takeIf { it.type == SldNodeType.BUS }

            if (selectedBus != null) {
                state.selectedNodeId = selectedBus.id
                state.selectedConnectionId = null
                state.connectionStartId = null
                state.engineeringError = null
                return
            }

            val panel =
                selected?.takeIf { it.type == SldNodeType.PANEL }
                    ?: state.nodes.firstOrNull {
                        it.type == SldNodeType.PANEL
                    }

            val position =
                if (panel != null) {
                    findBusPosition(panel)
                } else {
                    findFreeNodePosition(SldNodeType.BUS)
                }

            val bus = SldNode(
                id = createNodeId(),
                name = "BUS-" + (state.nodes.count {
                    it.type == SldNodeType.BUS
                } + 1),
                type = SldNodeType.BUS,
                x = position.first,
                y = position.second,
                voltage = panel?.voltage ?: 400.0,
                loadKw = 0.0,
                powerFactor = panel?.powerFactor ?: 0.90,
                demandFactor = 1.0,
                ratedKva = panel?.ratedKva ?: 0.0,
                transformerPercentZ = 0.0,
                generatorXdSubtransient = 0.0,
                sourceShortCircuitMva = 0.0
            )

            state.nodes = state.nodes + bus

            if (panel != null) {
                val connection = createBusbarConnection(panel, bus)

                if (connection != null) {
                    state.connections =
                        normalizeConnections(
                            state.nodes,
                            state.connections + connection
                        )
                    state.selectedConnectionId = connection.id
                }
            }

            state.selectedNodeId = bus.id
            state.connectionStartId = null
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "تعذر إنشاء الباسبار."
                    else "Unable to create busbar."
        }
    }

    /*
     * BUS is an internal physical busbar.
     * It must occupy the same physical assembly as the panel.
     */
    private fun findBusPosition(panel: SldNode): Pair<Float, Float> {
        return snap(
            panel.x,
            panel.y
        )
    }

    private fun findSelectedBreakerSupport(): SldNode? {
        val selected =
            state.selectedNodeId?.let { id ->
                state.nodes.firstOrNull { it.id == id }
            }

        return when (selected?.type) {
            SldNodeType.PANEL,
            SldNodeType.BUS -> selected
            else ->
                state.nodes.firstOrNull { it.type == SldNodeType.BUS }
                    ?: state.nodes.firstOrNull { it.type == SldNodeType.PANEL }
        }
    }

    /*
     * Breakers are placed INSIDE the physical panel assembly.
     * Generic collision testing is deliberately NOT used here,
     * because PANEL/BREAKER overlap is intentional.
     */
    private fun findBreakerPosition(
        support: SldNode
    ): Pair<Float, Float> {
        val panel =
            if (support.type == SldNodeType.PANEL) {
                support
            } else {
                state.connections
                    .map { normalizeConnection(it, state.nodes) }
                    .firstOrNull {
                        it.connectionType == SldConnectionType.BUSBAR &&
                            (it.fromNodeId == support.id ||
                                it.toNodeId == support.id)
                    }
                    ?.let { connection ->
                        val otherId =
                            if (connection.fromNodeId == support.id) {
                                connection.toNodeId
                            } else {
                                connection.fromNodeId
                            }

                        state.nodes.firstOrNull {
                            it.id == otherId &&
                                it.type == SldNodeType.PANEL
                        }
                    }
                    ?: state.nodes.firstOrNull {
                        it.type == SldNodeType.PANEL
                    }
            }

        val anchor = panel ?: support

        val breakerIds =
            state.nodes
                .filter { it.type == SldNodeType.BREAKER }
                .filter { breaker ->
                    state.connections.any { raw ->
                        val connection =
                            normalizeConnection(raw, state.nodes)

                        connection.connectionType ==
                            SldConnectionType.BUSBAR &&
                            (
                                connection.fromNodeId == breaker.id ||
                                    connection.toNodeId == breaker.id
                                )
                    }
                }
                .map { it.id }
                .toSet()

        val occupiedSlots =
            state.nodes
                .filter {
                    it.id in breakerIds
                }
                .map { breaker ->
                    kotlin.math.round(
                        (
                            breaker.x -
                                anchor.x -
                                NODE_WIDTH / 2f
                            ) / BREAKER_SLOT
                    ).toInt()
                }
                .toSet()

        for (slot in 0..60) {
            val candidates =
                if (slot == 0) {
                    listOf(0)
                } else {
                    listOf(-slot, slot)
                }

            candidates.forEach { index ->
                if (index in occupiedSlots) return@forEach

                val x =
                    anchor.x +
                        NODE_WIDTH / 2f -
                        NODE_WIDTH / 2f +
                        index * BREAKER_SLOT

                val y =
                    anchor.y + BREAKER_Y

                return snap(x, y)
            }
        }

        return snap(
            anchor.x,
            anchor.y + BREAKER_Y
        )
    }

    private fun findFreeNodePosition(type: SldNodeType): Pair<Float, Float> {
        if (type == SldNodeType.BREAKER) {
            findSelectedBreakerSupport()?.let {
                return findBreakerPosition(it)
            }
        }

        for (row in 0..40) {
            for (column in 0..40) {
                val position =
                    snap(
                        DEFAULT_X + column * POSITION_STEP_X,
                        DEFAULT_Y + row * POSITION_STEP_Y
                    )

                if (isFreeExternalPosition(position.first, position.second)) {
                    return position
                }
            }
        }

        return fallbackFreePosition()
    }

    private fun isFreeExternalPosition(
        x: Float,
        y: Float
    ): Boolean {
        if (!x.isFinite() || !y.isFinite()) return false

        return state.nodes.none { other ->
            if (
                other.type == SldNodeType.BREAKER ||
                other.type == SldNodeType.BUS
            ) {
                return@none false
            }

            val dx =
                abs(
                    x + NODE_WIDTH / 2f -
                        (other.x + NODE_WIDTH / 2f)
                )

            val dy =
                abs(
                    y + NODE_HEIGHT / 2f -
                        (other.y + NODE_HEIGHT / 2f)
                )

            dx < NODE_WIDTH + NODE_GAP_X &&
                dy < NODE_HEIGHT + NODE_GAP_Y
        }
    }

    private fun fallbackFreePosition(): Pair<Float, Float> {
        var x = DEFAULT_X
        var y = DEFAULT_Y

        repeat(5000) {
            if (isFreeExternalPosition(x, y)) {
                return snap(x, y)
            }

            x += POSITION_STEP_X

            if (x > 10000f) {
                x = DEFAULT_X
                y += POSITION_STEP_Y
            }
        }

        return snap(DEFAULT_X, y)
    }

    private fun snap(
        x: Float,
        y: Float
    ): Pair<Float, Float> {
        fun safe(value: Float): Float {
            if (!value.isFinite()) return MIN_X
            return kotlin.math.round(value / GRID) * GRID
        }

        return max(MIN_X, safe(x)) to
            max(MIN_Y, safe(y))
    }

    fun moveNode(
        nodeId: String,
        deltaX: Float,
        deltaY: Float
    ) {
        val dx = deltaX.takeIf { it.isFinite() } ?: 0f
        val dy = deltaY.takeIf { it.isFinite() } ?: 0f

        state.nodes =
            state.nodes.map {
                if (it.id == nodeId) {
                    it.copy(
                        x = max(MIN_X, it.x + dx),
                        y = max(MIN_Y, it.y + dy)
                    )
                } else {
                    it
                }
            }
    }

    fun moveNodeEnd() {
        safePersist()
        recalculateEngineering()
    }

    fun startOrCompleteConnection() {
        state.selectedNodeId?.let(::startOrCompleteConnection)
    }

    fun startOrCompleteConnection(nodeId: String) {
        try {
            val target =
                state.nodes.firstOrNull {
                    it.id == nodeId
                } ?: return

            val startId = state.connectionStartId

            if (startId == null) {
                state.selectedNodeId = target.id
                state.selectedConnectionId = null
                state.connectionStartId = target.id
                state.engineeringError = null
                return
            }

            if (startId == target.id) {
                state.engineeringError =
                    if (arabic) "اختر عنصرًا آخر."
                    else "Select another component."
                return
            }

            val start =
                state.nodes.firstOrNull {
                    it.id == startId
                }

            if (start == null) {
                state.connectionStartId = null
                return
            }

            if (isBusbarPair(start, target)) {
                val busbar =
                    createBusbarConnection(
                        start,
                        target
                    ) ?: return

                state.connections =
                    normalizeConnections(
                        state.nodes,
                        state.connections.filterNot {
                            sameUndirectedPair(it, busbar) &&
                                it.connectionType ==
                                SldConnectionType.BUSBAR
                        } + busbar
                    )

                state.selectedNodeId = target.id
                state.selectedConnectionId = busbar.id
                state.connectionStartId = null
                state.engineeringError = null

                safePersist()
                recalculateEngineering()
                return
            }

            val duplicate =
                state.connections.any {
                    samePair(
                        it,
                        start.id,
                        target.id
                    )
                }

            if (duplicate) {
                state.selectedNodeId = target.id
                state.connectionStartId = null
                state.engineeringError =
                    if (arabic) "هذا الاتصال موجود بالفعل."
                    else "This connection already exists."
                return
            }

            state.selectedNodeId = target.id
            state.connectionStartId = start.id
            state.editingConnectionId = null
            state.connectionType = SldConnectionType.CABLE.name
            state.length = "50"
            state.resistance = "0.125"
            state.reactance = "0.080"
            state.cableSize = "240"
            state.parallelRuns = "1"
            state.capacity = "350"
            state.showConnectionDialog = true
            state.engineeringError = null
        } catch (error: Throwable) {
            state.connectionStartId = null
            state.showConnectionDialog = false
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "تعذر إنشاء الاتصال."
                    else "Unable to create connection."
        }
    }

    private fun samePair(
        connection: SldConnection,
        a: String,
        b: String
    ): Boolean =
        (connection.fromNodeId == a && connection.toNodeId == b) ||
            (connection.fromNodeId == b && connection.toNodeId == a)

    private fun sameUndirectedPair(
        a: SldConnection,
        b: SldConnection
    ): Boolean =
        samePair(
            a,
            b.fromNodeId,
            b.toNodeId
        )

    private fun createBusbarConnection(
        from: SldNode,
        to: SldNode
    ): SldConnection? {
        val endpoints =
            canonicalBusbarEndpoints(
                from,
                to
            )

        if (endpoints == null) {
            state.engineeringError =
                if (arabic) {
                    "لا يمكن إنشاء باسبار بين هذين العنصرين."
                } else {
                    "These components cannot be connected by busbar."
                }
            return null
        }

        val panel =
            listOf(
                endpoints.first,
                endpoints.second
            ).firstOrNull {
                it.type == SldNodeType.PANEL
            }

        val existing =
            state.connections
                .map {
                    normalizeConnection(
                        it,
                        state.nodes
                    )
                }
                .firstOrNull {
                    it.connectionType ==
                        SldConnectionType.BUSBAR &&
                        samePair(
                            it,
                            endpoints.first.id,
                            endpoints.second.id
                        )
                }

        if (existing != null) return existing

        val estimated =
            panel?.let(::estimateBusbarCurrent)
                ?: DEFAULT_BUSBAR_CURRENT_A

        val rated =
            nonNegative(
                state.busbarRatedCurrent,
                estimated
            ).coerceAtLeast(
                DEFAULT_BUSBAR_CURRENT_A
            )

        val shortCircuit =
            nonNegative(
                state.busbarShortCircuit,
                DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
            )

        return SldConnection(
            id = createConnectionId(),
            fromNodeId = endpoints.first.id,
            toNodeId = endpoints.second.id,
            connectionType = SldConnectionType.BUSBAR,
            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,
            currentCapacityA = rated,
            conductorMaterial = "",
            insulationType = "",
            installationMethodCode = "",
            loadedConductors = 0,
            cableDesignation = "",
            cableManufacturer = "",
            cableModel = "",
            busbarMaterial =
                state.busbarMaterial.ifBlank {
                    "Copper"
                },
            busbarRatedCurrentA = rated,
            busbarShortCircuitKA = shortCircuit
        )
    }

    fun editConnection(connection: SldConnection) {
        val normalized =
            normalizeConnection(
                connection,
                state.nodes
            )

        state.editingConnectionId = normalized.id
        state.connectionType = normalized.connectionType.name
        state.length = normalized.lengthMeters.eng()
        state.resistance = normalized.resistanceOhmPerKm.eng()
        state.reactance = normalized.reactanceOhmPerKm.eng()
        state.cableSize = normalized.cableSizeMm2.eng()
        state.parallelRuns = normalized.parallelRuns.toString()
        state.capacity = normalized.currentCapacityA.eng()
        state.conductorMaterial =
            normalized.conductorMaterial.ifBlank {
                "Copper"
            }
        state.insulationType =
            normalized.insulationType.ifBlank {
                "PVC"
            }
        state.installationMethodCode =
            normalized.installationMethodCode.ifBlank {
                "B1"
            }
        state.busbarMaterial =
            normalized.busbarMaterial.ifBlank {
                "Copper"
            }
        state.busbarRatedCurrent =
            normalized.busbarRatedCurrentA.eng()
        state.busbarShortCircuit =
            normalized.busbarShortCircuitKA.eng()
        state.connectionStartId = null
        state.showConnectionDialog = true
    }

    fun saveConnection() {
        val editingId = state.editingConnectionId

        try {
            if (editingId != null) {
                val existing =
                    state.connections.firstOrNull {
                        it.id == editingId
                    } ?: return

                val from =
                    state.nodes.firstOrNull {
                        it.id == existing.fromNodeId
                    }

                val to =
                    state.nodes.firstOrNull {
                        it.id == existing.toNodeId
                    }

                if (from == null || to == null) return

                if (
                    state.connectionType.equals(
                        SldConnectionType.BUSBAR.name,
                        ignoreCase = true
                    ) ||
                    isBusbarPair(from, to)
                ) {
                    val busbar =
                        createBusbarConnection(
                            from,
                            to
                        )

                    if (busbar != null) {
                        val replacement =
                            busbar.copy(
                                id = existing.id
                            )

                        state.connections =
                            normalizeConnections(
                                state.nodes,
                                state.connections.map {
                                    if (it.id == existing.id) {
                                        replacement
                                    } else {
                                        it
                                    }
                                }
                            )
                    }
                } else {
                    val cable =
                        existing.copy(
                            connectionType =
                                SldConnectionType.CABLE,
                            lengthMeters =
                                nonNegative(
                                    state.length,
                                    existing.lengthMeters
                                ),
                            resistanceOhmPerKm =
                                nonNegative(
                                    state.resistance,
                                    existing.resistanceOhmPerKm
                                ),
                            reactanceOhmPerKm =
                                nonNegative(
                                    state.reactance,
                                    existing.reactanceOhmPerKm
                                ),
                            cableSizeMm2 =
                                nonNegative(
                                    state.cableSize,
                                    existing.cableSizeMm2
                                ),
                            parallelRuns =
                                state.parallelRuns
                                    .toIntOrNull()
                                    ?.coerceAtLeast(1)
                                    ?: existing.parallelRuns,
                            currentCapacityA =
                                nonNegative(
                                    state.capacity,
                                    existing.currentCapacityA
                                ),
                            conductorMaterial =
                                state.conductorMaterial.ifBlank {
                                    existing.conductorMaterial
                                },
                            insulationType =
                                state.insulationType.ifBlank {
                                    existing.insulationType
                                },
                            installationMethodCode =
                                state.installationMethodCode.ifBlank {
                                    existing.installationMethodCode
                                },
                            busbarMaterial = "",
                            busbarRatedCurrentA = 0.0,
                            busbarShortCircuitKA = 0.0
                        )

                    state.connections =
                        state.connections.map {
                            if (it.id == existing.id) cable else it
                        }
                }
            } else {
                val from =
                    state.connectionStartId?.let { id ->
                        state.nodes.firstOrNull {
                            it.id == id
                        }
                    }

                val to =
                    state.selectedNodeId?.let { id ->
                        state.nodes.firstOrNull {
                            it.id == id
                        }
                    }

                if (from == null || to == null) return

                val connection =
                    if (
                        state.connectionType.equals(
                            SldConnectionType.BUSBAR.name,
                            ignoreCase = true
                        ) ||
                        isBusbarPair(from, to)
                    ) {
                        createBusbarConnection(
                            from,
                            to
                        )
                    } else {
                        SldConnection(
                            id = createConnectionId(),
                            fromNodeId = from.id,
                            toNodeId = to.id,
                            connectionType =
                                SldConnectionType.CABLE,
                            lengthMeters =
                                nonNegative(
                                    state.length,
                                    50.0
                                ),
                            resistanceOhmPerKm =
                                nonNegative(
                                    state.resistance,
                                    0.125
                                ),
                            reactanceOhmPerKm =
                                nonNegative(
                                    state.reactance,
                                    0.080
                                ),
                            cableSizeMm2 =
                                nonNegative(
                                    state.cableSize,
                                    240.0
                                ),
                            parallelRuns =
                                state.parallelRuns
                                    .toIntOrNull()
                                    ?.coerceAtLeast(1)
                                    ?: 1,
                            voltageDropPercent = 0.0,
                            currentCapacityA =
                                nonNegative(
                                    state.capacity,
                                    350.0
                                ),
                            conductorMaterial =
                                state.conductorMaterial.ifBlank {
                                    "Copper"
                                },
                            insulationType =
                                state.insulationType.ifBlank {
                                    "XLPE"
                                },
                            installationMethodCode =
                                state.installationMethodCode.ifBlank {
                                    "C"
                                },
                            busbarMaterial = "",
                            busbarRatedCurrentA = 0.0,
                            busbarShortCircuitKA = 0.0
                        )
                    }

                if (connection != null) {
                    state.connections =
                        normalizeConnections(
                            state.nodes,
                            state.connections + connection
                        )

                    state.selectedConnectionId =
                        connection.id
                }
            }

            state.showConnectionDialog = false
            state.editingConnectionId = null
            state.connectionStartId = null
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "تعذر حفظ الاتصال."
                    else "Unable to save connection."
        }
    }

    fun cancelConnectionEdit() {
        state.showConnectionDialog = false
        state.editingConnectionId = null
        state.connectionStartId = null
    }

    fun deleteSelected() {
        val nodeId = state.selectedNodeId
        val connectionId = state.selectedConnectionId

        if (nodeId != null) {
            state.nodes =
                state.nodes.filterNot {
                    it.id == nodeId
                }

            state.connections =
                state.connections.filterNot {
                    it.fromNodeId == nodeId ||
                        it.toNodeId == nodeId
                }
        }

        if (connectionId != null) {
            state.connections =
                state.connections.filterNot {
                    it.id == connectionId
                }
        }

        state.clearSelection()
        state.engineeringPackage = null
        state.engineeringError = null

        safePersist()
        recalculateEngineering()
    }

    fun autoLayout() {
        try {
            val result =
                SldAutoLayoutEngine.arrange(
                    network()
                )

            state.nodes =
                result.network.nodes

            state.connections =
                normalizeConnections(
                    state.nodes,
                    result.network.connections
                )

            state.clearSelection()
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "فشل ترتيب المخطط."
                    else "Auto layout failed."
        }
    }

    fun generateCompleteSld() {
        try {
            val generated =
                SldCompleteGenerator.generate()

            state.nodes = generated.nodes

            state.connections =
                normalizeConnections(
                    state.nodes,
                    generated.connections
                )

            state.clearSelection()
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "فشل إنشاء المخطط."
                    else "Failed to generate SLD."
        }
    }

    fun runShortCircuit() {
        try {
            val current = network()

            val topology =
                SldEngineeringFacade
                    .checkEngineeringTopology(current)

            if (!topology.valid) {
                state.engineeringError =
                    if (arabic) {
                        "المخطط غير مكتمل لحساب القصر."
                    } else {
                        "The SLD is incomplete for short-circuit calculation."
                    }
                return
            }

            val result =
                SldEngineeringFacade
                    .calculateShortCircuit(current)

            state.reportTitle =
                if (arabic) {
                    "دراسة القصر الكهربائي"
                } else {
                    "Short Circuit Study"
                }

            state.reportText = result.toString()
            state.showReport = true
            state.engineeringError = null
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "فشل حساب القصر."
                    else "Short-circuit calculation failed."
        }
    }

    fun runPanelSchedule() {
        try {
            val panel =
                state.nodes.firstOrNull {
                    it.type == SldNodeType.PANEL
                }

            if (panel == null) {
                state.engineeringError =
                    if (arabic) "لا توجد لوحة."
                    else "No panel exists."
                return
            }

            val current = network()

            val topology =
                SldEngineeringFacade
                    .checkEngineeringTopology(current)

            if (!topology.valid) {
                state.engineeringError =
                    if (arabic) "المخطط غير مكتمل."
                    else "The SLD is incomplete."
                return
            }

            val result =
                SldEngineeringFacade.calculateComplete(
                    network = current,
                    panelNodeId = panel.id
                )

            state.engineeringPackage = result

            state.reportTitle =
                if (arabic) {
                    "جدول اللوحة"
                } else {
                    "Panel Schedule"
                }

            state.reportText =
                result.panelSchedule?.toString() ?: ""

            state.showReport = true
            state.engineeringError = null
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf { it.isNotBlank() }
                    ?: if (arabic) "فشل إنشاء جدول اللوحة."
                    else "Panel schedule failed."
        }
    }

    private fun positive(
        value: String,
        fallback: Double
    ): Double =
        value.toDoubleOrNull()
            ?.takeIf {
                it.isFinite() && it > 0.0
            }
            ?: fallback.coerceAtLeast(0.000001)

    private fun nonNegative(
        value: String,
        fallback: Double
    ): Double =
        value.toDoubleOrNull()
            ?.takeIf {
                it.isFinite() && it >= 0.0
            }
            ?: fallback.coerceAtLeast(0.0)

    private fun range(
        value: String,
        fallback: Double,
        minimum: Double,
        maximum: Double
    ): Double =
        value.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
            ?.coerceIn(minimum, maximum)
            ?: fallback.coerceIn(minimum, maximum)

    private fun estimateBusbarCurrent(
        panel: SldNode
    ): Double {
        if (
            panel.ratedKva <= 0.0 ||
            panel.voltage <= 0.0
        ) {
            return DEFAULT_BUSBAR_CURRENT_A
        }

        val current =
            panel.ratedKva * 1000.0 /
                (sqrt(3.0) * panel.voltage)

        return if (
            current.isFinite() &&
            current > 0.0
        ) {
            max(
                DEFAULT_BUSBAR_CURRENT_A,
                current
            )
        } else {
            DEFAULT_BUSBAR_CURRENT_A
        }
    }

    private fun createNodeId(): String {
        var id = "node-${System.nanoTime()}"
        var index = 0

        while (state.nodes.any { it.id == id }) {
            index++
            id = "node-${System.nanoTime()}-$index"
        }

        return id
    }

    private fun createConnectionId(): String {
        var id = "connection-${System.nanoTime()}"
        var index = 0

        while (state.connections.any { it.id == id }) {
            index++
            id = "connection-${System.nanoTime()}-$index"
        }

        return id
    }

    private fun Double.eng(): String =
        if (isFinite()) {
            String.format(
                Locale.US,
                "%.2f",
                this
            )
        } else {
            "0.00"
        }
}
