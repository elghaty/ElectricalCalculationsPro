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
import kotlin.math.round
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
        const val NODE_GAP_Y = 70f

        const val DEFAULT_X = 700f
        const val DEFAULT_Y = 160f

        const val POSITION_STEP_X = 220f
        const val POSITION_STEP_Y = 180f

        const val BREAKER_Y_GAP = 230f
        const val BREAKER_SLOT = 150f

        const val DEFAULT_BUSBAR_CURRENT_A = 400.0
        const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA = 25.0

        const val NODE_WIDTH = 180f
        const val NODE_HEIGHT = 150f

        const val DEFAULT_CABLE_LENGTH = 50.0
        const val DEFAULT_CABLE_RESISTANCE = 0.125
        const val DEFAULT_CABLE_REACTANCE = 0.080
        const val DEFAULT_CABLE_SIZE = 240.0
        const val DEFAULT_CABLE_CAPACITY = 350.0
    }

    // ---------------------------------------------------------------------
    // Topology
    // ---------------------------------------------------------------------

    private fun isPanelBreakerPair(
        first: SldNode,
        second: SldNode
    ): Boolean {
        return (
            first.type == SldNodeType.PANEL &&
                second.type == SldNodeType.BREAKER
            ) || (
            first.type == SldNodeType.BREAKER &&
                second.type == SldNodeType.PANEL
            )
    }

    private fun isBusbarPair(
        first: SldNode,
        second: SldNode
    ): Boolean {
        val a = first.type
        val b = second.type

        return when {
            a == SldNodeType.PANEL &&
                b == SldNodeType.BUS -> true

            a == SldNodeType.BUS &&
                b == SldNodeType.PANEL -> true

            a == SldNodeType.PANEL &&
                b == SldNodeType.BREAKER -> true

            a == SldNodeType.BREAKER &&
                b == SldNodeType.PANEL -> true

            a == SldNodeType.BUS &&
                b == SldNodeType.BREAKER -> true

            a == SldNodeType.BREAKER &&
                b == SldNodeType.BUS -> true

            else -> false
        }
    }

    private fun canonicalBusbarEndpoints(
        first: SldNode,
        second: SldNode
    ): Pair<SldNode, SldNode>? {
        return when {
            first.type == SldNodeType.PANEL &&
                second.type == SldNodeType.BUS ->
                first to second

            first.type == SldNodeType.BUS &&
                second.type == SldNodeType.PANEL ->
                second to first

            first.type == SldNodeType.PANEL &&
                second.type == SldNodeType.BREAKER ->
                first to second

            first.type == SldNodeType.BREAKER &&
                second.type == SldNodeType.PANEL ->
                second to first

            first.type == SldNodeType.BUS &&
                second.type == SldNodeType.BREAKER ->
                first to second

            first.type == SldNodeType.BREAKER &&
                second.type == SldNodeType.BUS ->
                second to first

            else -> null
        }
    }

    private fun normalizeConnection(
        connection: SldConnection,
        nodes: List<SldNode>
    ): SldConnection {
        val from = nodes.firstOrNull { it.id == connection.fromNodeId }
            ?: return connection

        val to = nodes.firstOrNull { it.id == connection.toNodeId }
            ?: return connection

        val legacyPanelBreaker =
            isPanelBreakerPair(from, to) &&
                connection.connectionType == SldConnectionType.CABLE

        val requestedBusbar =
            connection.connectionType == SldConnectionType.BUSBAR

        if (!legacyPanelBreaker && !requestedBusbar) {
            return connection
        }

        if (!isBusbarPair(from, to)) {
            return connection
        }

        val endpoints = canonicalBusbarEndpoints(from, to)
            ?: return connection

        val canonicalFrom = endpoints.first
        val canonicalTo = endpoints.second

        val panel = listOf(canonicalFrom, canonicalTo)
            .firstOrNull { it.type == SldNodeType.PANEL }

        val estimatedCurrent =
            panel?.let(::estimateBusbarCurrent)
                ?: DEFAULT_BUSBAR_CURRENT_A

        val ratedCurrent =
            connection.busbarRatedCurrentA
                .takeIf { it.isFinite() && it > 0.0 }
                ?: estimatedCurrent

        val shortCircuit =
            connection.busbarShortCircuitKA
                .takeIf { it.isFinite() && it > 0.0 }
                ?: DEFAULT_BUSBAR_SHORT_CIRCUIT_KA

        return connection.copy(
            fromNodeId = canonicalFrom.id,
            toNodeId = canonicalTo.id,
            connectionType = SldConnectionType.BUSBAR,
            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,
            currentCapacityA = ratedCurrent,
            conductorMaterial = "",
            insulationType = "",
            installationMethodCode = "",
            loadedConductors = 0,
            cableDesignation = "",
            cableManufacturer = "",
            cableModel = "",
            busbarMaterial = connection.busbarMaterial.ifBlank { "Copper" },
            busbarRatedCurrentA = ratedCurrent,
            busbarShortCircuitKA = shortCircuit
        )
    }

    private fun normalizeConnections(
        nodes: List<SldNode>,
        connections: List<SldConnection>
    ): List<SldConnection> {
        val result = ArrayList<SldConnection>(connections.size)
        val seen = HashSet<String>()

        connections
            .map { normalizeConnection(it, nodes) }
            .forEach { connection ->
                val key = if (
                    connection.connectionType == SldConnectionType.BUSBAR
                ) {
                    val a = minOf(
                        connection.fromNodeId,
                        connection.toNodeId
                    )
                    val b = maxOf(
                        connection.fromNodeId,
                        connection.toNodeId
                    )
                    "$a<->$b:BUSBAR"
                } else {
                    "${connection.fromNodeId}->${connection.toNodeId}:${connection.connectionType}"
                }

                if (seen.add(key)) {
                    result += connection
                }
            }

        return result
    }

    private fun network(): SldNetwork {
        val nodes = state.nodes.toList()
        val connections = normalizeConnections(
            nodes = nodes,
            connections = state.connections.toList()
        )

        if (connections != state.connections) {
            state.connections = connections
        }

        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

    // ---------------------------------------------------------------------
    // Persistence / engineering
    // ---------------------------------------------------------------------

    private fun safePersist() {
        val callback = onSave ?: return

        try {
            callback(network())
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر حفظ التغييرات، لكن المخطط الحالي لم يتم حذفه.",
                        "The changes could not be persisted, but the current SLD was kept."
                    )
        }
    }

    fun saveAndRecalculate() {
        safePersist()
        recalculateEngineering()
    }

    fun recalculateEngineering() {
        val currentNetwork = try {
            network()
        } catch (error: Throwable) {
            state.engineeringPackage = null
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر قراءة بيانات المخطط.",
                        "Unable to read the SLD data."
                    )
            return
        }

        if (currentNetwork.nodes.isEmpty()) {
            state.engineeringPackage = null
            state.engineeringError = null
            return
        }

        try {
            val validation = SldDesignValidator.validate(currentNetwork)

            val realError = validation.errors.firstOrNull { error ->
                !isEditorTopologyWarning(error.message.orEmpty())
            }

            if (realError != null) {
                state.engineeringPackage = null
                state.engineeringError =
                    realError.message.takeIf(String::isNotBlank)
                        ?: message(
                            "بيانات المخطط تحتوي على خطأ هندسي.",
                            "The SLD contains an engineering data error."
                        )
                return
            }

            val topology =
                SldEngineeringFacade.checkEngineeringTopology(currentNetwork)

            if (!topology.valid) {
                state.engineeringPackage = null
                state.engineeringError = null
                return
            }

            state.engineeringPackage =
                SldEngineeringFacade.calculateComplete(
                    network = currentNetwork
                )

            state.engineeringError = null
        } catch (error: Throwable) {
            state.engineeringPackage = null
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر تنفيذ الدراسة الهندسية الحالية.",
                        "The current engineering study could not be completed."
                    )
        }
    }

    fun loadProjectNetwork() {
        recalculateEngineering()
    }

    private fun isEditorTopologyWarning(
        rawMessage: String
    ): Boolean {
        val message = rawMessage.lowercase(Locale.US)

        return message.contains("incoming") ||
            message.contains("outgoing") ||
            message.contains("parent") ||
            message.contains("connection") ||
            message.contains("connected") ||
            message.contains("reach")
    }

    private fun message(
        ar: String,
        en: String
    ): String = if (arabic) ar else en

    // ---------------------------------------------------------------------
    // Nodes
    // ---------------------------------------------------------------------

    fun addNode(type: SldNodeType) {
        try {
            state.nodeType = type

            val panel =
                if (type == SldNodeType.BREAKER) {
                    findPanelForNewBreaker()
                } else {
                    null
                }

            val position =
                if (panel != null && type == SldNodeType.BREAKER) {
                    findBreakerPosition(panel)
                } else {
                    findFreeNodePosition(type)
                }

            val node = createDefaultNode(type, position)

            state.nodes += node
            state.selectedNodeId = node.id
            state.selectedConnectionId = null
            state.connectionStartId = null
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر إضافة العنصر إلى المخطط.",
                        "Unable to add the component to the SLD."
                    )
        }
    }

    private fun createDefaultNode(
        type: SldNodeType,
        position: Pair<Float, Float>
    ): SldNode {
        val x = if (position.first.isFinite()) {
            max(MIN_X, position.first)
        } else {
            DEFAULT_X
        }

        val y = if (position.second.isFinite()) {
            max(MIN_Y, position.second)
        } else {
            DEFAULT_Y
        }

        return SldNode(
            id = createNodeId(),
            name = defaultNodeName(type),
            type = type,
            x = x,
            y = y,
            voltage = 400.0,
            loadKw = if (type == SldNodeType.LOAD) 100.0 else 0.0,
            powerFactor = 0.90,
            demandFactor = if (type == SldNodeType.LOAD) 0.80 else 1.0,
            ratedKva = when (type) {
                SldNodeType.TRANSFORMER,
                SldNodeType.GENERATOR,
                SldNodeType.PANEL -> 500.0

                else -> 0.0
            },
            transformerPercentZ =
                if (type == SldNodeType.TRANSFORMER) 6.0 else 0.0,
            generatorXdSubtransient =
                if (type == SldNodeType.GENERATOR) 15.0 else 0.0,
            sourceShortCircuitMva =
                if (type == SldNodeType.SOURCE) 500.0 else 0.0
        )
    }

    private fun defaultNodeName(type: SldNodeType): String {
        val prefix = when (type) {
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

    // ---------------------------------------------------------------------
    // Node editor
    // ---------------------------------------------------------------------

    fun resetNodeEditor(type: SldNodeType) {
        state.editingNodeId = null
        state.nodeType = type
        state.name = ""
        state.voltage = "400"
        state.loadKw = if (type == SldNodeType.LOAD) "100" else "0"
        state.pf = "0.90"
        state.demand = if (type == SldNodeType.LOAD) "0.80" else "1.00"

        state.kva = when (type) {
            SldNodeType.TRANSFORMER,
            SldNodeType.GENERATOR,
            SldNodeType.PANEL -> "500"

            else -> "0"
        }

        state.transformerZ =
            if (type == SldNodeType.TRANSFORMER) "6" else "0"

        state.generatorXd =
            if (type == SldNodeType.GENERATOR) "15" else "0"

        state.sourceMva =
            if (type == SldNodeType.SOURCE) "500" else "0"

        state.engineeringError = null
        state.showNodeDialog = true
    }

    fun editNode(node: SldNode) {
        state.editingNodeId = node.id
        state.name = node.name
        state.nodeType = node.type
        state.voltage = node.voltage.toEngineeringString()
        state.loadKw = node.loadKw.toEngineeringString()
        state.pf = node.powerFactor.toEngineeringString()
        state.demand = node.demandFactor.toEngineeringString()
        state.kva = node.ratedKva.toEngineeringString()
        state.transformerZ = node.transformerPercentZ.toEngineeringString()
        state.generatorXd = node.generatorXdSubtransient.toEngineeringString()
        state.sourceMva = node.sourceShortCircuitMva.toEngineeringString()
        state.showNodeDialog = true
    }

    fun saveNode() {
        try {
            val editingId = state.editingNodeId
            val existing = editingId?.let { id ->
                state.nodes.firstOrNull { it.id == id }
            }

            val type = state.nodeType

            val voltage = parsePositive(
                state.voltage,
                existing?.voltage ?: 400.0
            )

            val loadKw = parseNonNegative(
                state.loadKw,
                existing?.loadKw ?: 0.0
            )

            val pf = parseRange(
                state.pf,
                existing?.powerFactor ?: 0.90,
                0.50,
                1.00
            )

            val demand = parseRange(
                state.demand,
                existing?.demandFactor ?: 1.0,
                0.0,
                1.0
            )

            val kva = parseNonNegative(
                state.kva,
                existing?.ratedKva ?: 0.0
            )

            val transformerZ = parseNonNegative(
                state.transformerZ,
                existing?.transformerPercentZ ?: 0.0
            )

            val generatorXd = parseNonNegative(
                state.generatorXd,
                existing?.generatorXdSubtransient ?: 0.0
            )

            val sourceMva = parseNonNegative(
                state.sourceMva,
                existing?.sourceShortCircuitMva ?: 0.0
            )

            val name = state.name.trim().ifBlank {
                existing?.name ?: defaultNodeName(type)
            }

            if (existing == null) {
                val panel =
                    if (type == SldNodeType.BREAKER) {
                        findPanelForNewBreaker()
                    } else {
                        null
                    }

                val position =
                    if (panel != null && type == SldNodeType.BREAKER) {
                        findBreakerPosition(panel)
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
                    loadKw = if (type == SldNodeType.LOAD) loadKw else 0.0,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

                state.nodes += node
                state.selectedNodeId = node.id
                state.selectedConnectionId = null
                state.connectionStartId = null
            } else {
                val updated = existing.copy(
                    name = name,
                    type = type,
                    voltage = voltage,
                    loadKw = if (type == SldNodeType.LOAD) loadKw else 0.0,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

                state.nodes = state.nodes.map {
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
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر حفظ العنصر، وتم الاحتفاظ بالمخطط.",
                        "Unable to save the component. The drawing was kept intact."
                    )
        }
    }

    // ---------------------------------------------------------------------
    // Node positions
    // ---------------------------------------------------------------------

    private fun findPanelForNewBreaker(): SldNode? {
        val selected = state.selectedNodeId?.let { id ->
            state.nodes.firstOrNull {
                it.id == id && it.type == SldNodeType.PANEL
            }
        }

        return selected ?: state.nodes.firstOrNull {
            it.type == SldNodeType.PANEL
        }
    }

    private fun findBreakerPosition(
        panel: SldNode
    ): Pair<Float, Float> {
        val existingBreakerIds = state.connections
            .asSequence()
            .map { normalizeConnection(it, state.nodes) }
            .filter {
                it.connectionType == SldConnectionType.BUSBAR &&
                    it.fromNodeId == panel.id
            }
            .map { it.toNodeId }
            .toSet()

        val occupiedCenters = state.nodes
            .filter {
                it.type == SldNodeType.BREAKER &&
                    it.id in existingBreakerIds
            }
            .map { it.x + NODE_WIDTH / 2f }

        val panelCenter = panel.x + NODE_WIDTH / 2f
        val y = max(MIN_Y, panel.y + BREAKER_Y_GAP)

        val candidates = buildList {
            add(panelCenter)

            for (i in 1..50) {
                val offset = i * BREAKER_SLOT
                add(panelCenter - offset)
                add(panelCenter + offset)
            }
        }

        for (center in candidates) {
            if (
                occupiedCenters.any {
                    abs(it - center) < BREAKER_SLOT * 0.75f
                }
            ) {
                continue
            }

            val candidate = snapToGrid(
                center - NODE_WIDTH / 2f,
                y
            )

            if (isFreePosition(candidate.first, candidate.second)) {
                return candidate
            }
        }

        return fallbackFreePosition()
    }

    private fun findFreeNodePosition(
        type: SldNodeType
    ): Pair<Float, Float> {
        if (type == SldNodeType.BREAKER) {
            val panels = state.nodes.filter {
                it.type == SldNodeType.PANEL
            }

            for (panel in panels) {
                val center = panel.x + NODE_WIDTH / 2f
                val y = panel.y + BREAKER_Y_GAP

                for (i in 0..30) {
                    val direction =
                        if (i % 2 == 0) 1f else -1f

                    val index = (i + 1) / 2

                    val candidate = snapToGrid(
                        center +
                            direction *
                            index *
                            BREAKER_SLOT -
                            NODE_WIDTH / 2f,
                        y
                    )

                    if (isFreePosition(candidate.first, candidate.second)) {
                        return candidate
                    }
                }
            }
        }

        for (row in 0..30) {
            for (column in 0..30) {
                val candidate = snapToGrid(
                    DEFAULT_X + column * POSITION_STEP_X,
                    DEFAULT_Y + row * POSITION_STEP_Y
                )

                if (isFreePosition(candidate.first, candidate.second)) {
                    return candidate
                }
            }
        }

        return fallbackFreePosition()
    }

    private fun isFreePosition(
        x: Float,
        y: Float
    ): Boolean {
        if (!x.isFinite() || !y.isFinite()) return false

        return state.nodes.none { other ->
            val dx = abs(
                x + NODE_WIDTH / 2f -
                    (other.x + NODE_WIDTH / 2f)
            )

            val dy = abs(
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
            if (isFreePosition(x, y)) {
                return snapToGrid(x, y)
            }

            x += POSITION_STEP_X

            if (x > 10000f) {
                x = DEFAULT_X
                y += POSITION_STEP_Y
            }
        }

        return snapToGrid(
            DEFAULT_X,
            y + POSITION_STEP_Y
        )
    }

    private fun snapToGrid(
        x: Float,
        y: Float
    ): Pair<Float, Float> {
        fun snap(value: Float): Float {
            if (!value.isFinite()) return 0f
            return round(value / GRID) * GRID
        }

        return max(MIN_X, snap(x)) to
            max(MIN_Y, snap(y))
    }

    // ---------------------------------------------------------------------
    // Node movement
    // ---------------------------------------------------------------------

    fun moveNode(
        nodeId: String,
        deltaX: Float,
        deltaY: Float
    ) {
        val node = state.nodes.firstOrNull {
            it.id == nodeId
        } ?: return

        val dx = deltaX.takeIf(Float::isFinite) ?: 0f
        val dy = deltaY.takeIf(Float::isFinite) ?: 0f

        state.nodes = state.nodes.map {
            if (it.id == node.id) {
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

    // ---------------------------------------------------------------------
    // Connections
    // ---------------------------------------------------------------------

    fun startOrCompleteConnection() {
        state.selectedNodeId?.let(::startOrCompleteConnection)
    }

    fun startOrCompleteConnection(nodeId: String) {
        try {
            val target = state.nodes.firstOrNull {
                it.id == nodeId
            }

            if (target == null) {
                state.engineeringError =
                    message(
                        "العنصر المحدد غير موجود.",
                        "The selected component does not exist."
                    )
                return
            }

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
                    message(
                        "اختر عنصرًا آخر كطرف ثانٍ.",
                        "Select another component as the destination."
                    )
                return
            }

            val start = state.nodes.firstOrNull {
                it.id == startId
            }

            if (start == null) {
                state.connectionStartId = null
                state.engineeringError =
                    message(
                        "تعذر العثور على العنصر الأول.",
                        "The first component could not be found."
                    )
                return
            }

            if (isPanelBreakerPair(start, target)) {
                val endpoints = canonicalBusbarEndpoints(start, target)
                    ?: return

                val panel = endpoints.first
                val breaker = endpoints.second

                val existing = state.connections
                    .map { normalizeConnection(it, state.nodes) }
                    .firstOrNull {
                        it.fromNodeId == panel.id &&
                            it.toNodeId == breaker.id &&
                            it.connectionType == SldConnectionType.BUSBAR
                    }

                if (existing != null) {
                    state.selectedNodeId = breaker.id
                    state.selectedConnectionId = existing.id
                    state.connectionStartId = null
                    state.showConnectionDialog = false
                    state.engineeringError =
                        message(
                            "هذا القاطع متصل بالفعل بالباسبار.",
                            "This breaker is already connected to the panel busbar."
                        )
                    return
                }

                val created = createBusbarConnection(panel, breaker)
                    ?: return

                state.selectedNodeId = breaker.id
                state.selectedConnectionId = created.id
                state.connectionStartId = null
                state.showConnectionDialog = false
                state.engineeringPackage = null
                state.engineeringError = null

                state.connections =
                    normalizeConnections(
                        state.nodes,
                        state.connections + created
                    )

                safePersist()
                recalculateEngineering()
                return
            }

            val duplicate = state.connections.any { connection ->
                (
                    connection.fromNodeId == start.id &&
                        connection.toNodeId == target.id
                    ) || (
                    connection.fromNodeId == target.id &&
                        connection.toNodeId == start.id
                    )
            }

            if (duplicate) {
                state.selectedNodeId = target.id
                state.connectionStartId = start.id
                state.engineeringError =
                    message(
                        "هذا الاتصال موجود بالفعل.",
                        "This connection already exists."
                    )
                return
            }

            prepareNewConnectionDialog(start, target)
        } catch (error: Throwable) {
            state.showConnectionDialog = false
            state.editingConnectionId = null
            state.connectionStartId = null
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر إنشاء الاتصال.",
                        "Unable to create the connection."
                    )
        }
    }

    private fun prepareNewConnectionDialog(
        start: SldNode,
        target: SldNode
    ) {
        state.selectedNodeId = target.id
        state.selectedConnectionId = null
        state.editingConnectionId = null
        state.connectionStartId = start.id

        state.connectionType = SldConnectionType.CABLE.name
        state.length = DEFAULT_CABLE_LENGTH.toEngineeringString()
        state.resistance = DEFAULT_CABLE_RESISTANCE.toEngineeringString()
        state.reactance = DEFAULT_CABLE_REACTANCE.toEngineeringString()
        state.cableSize = DEFAULT_CABLE_SIZE.toEngineeringString()
        state.parallelRuns = "1"
        state.capacity = DEFAULT_CABLE_CAPACITY.toEngineeringString()

        state.conductorMaterial =
            state.conductorMaterial.ifBlank { "Copper" }

        state.insulationType =
            state.insulationType.ifBlank { "XLPE" }

        state.installationMethodCode =
            state.installationMethodCode.ifBlank { "C" }

        state.busbarMaterial =
            state.busbarMaterial.ifBlank { "Copper" }

        state.busbarRatedCurrent =
            state.busbarRatedCurrent.ifBlank {
                DEFAULT_BUSBAR_CURRENT_A.toString()
            }

        state.busbarShortCircuit =
            state.busbarShortCircuit.ifBlank {
                DEFAULT_BUSBAR_SHORT_CIRCUIT_KA.toString()
            }

        state.showConnectionDialog = true
        state.engineeringError = null
    }

    // ---------------------------------------------------------------------
    // BUSBAR creation
    // ---------------------------------------------------------------------

    private fun createBusbarConnection(
        from: SldNode,
        to: SldNode
    ): SldConnection? {
        if (!isBusbarPair(from, to)) {
            state.engineeringError =
                message(
                    "لا يمكن إنشاء BUSBAR بين هذين العنصرين.",
                    "A BUSBAR cannot be created between these components."
                )
            return null
        }

        val endpoints = canonicalBusbarEndpoints(from, to)
            ?: run {
                state.engineeringError =
                    message(
                        "تركيب BUSBAR غير صالح.",
                        "Invalid BUSBAR topology."
                    )
                return null
            }

        val canonicalFrom = endpoints.first
        val canonicalTo = endpoints.second

        val existing = state.connections
            .map { normalizeConnection(it, state.nodes) }
            .firstOrNull {
                it.connectionType == SldConnectionType.BUSBAR &&
                    sameUnorderedPair(
                        it.fromNodeId,
                        it.toNodeId,
                        canonicalFrom.id,
                        canonicalTo.id
                    )
            }

        if (existing != null) {
            return existing
        }

        val panel = listOf(canonicalFrom, canonicalTo)
            .firstOrNull { it.type == SldNodeType.PANEL }

        val estimatedCurrent =
            panel?.let(::estimateBusbarCurrent)
                ?: DEFAULT_BUSBAR_CURRENT_A

        val ratedCurrent = parseNonNegative(
            state.busbarRatedCurrent,
            estimatedCurrent
        )

        val shortCircuit = parseNonNegative(
            state.busbarShortCircuit,
            DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
        )

        return SldConnection(
            id = createConnectionId(),
            fromNodeId = canonicalFrom.id,
            toNodeId = canonicalTo.id,
            connectionType = SldConnectionType.BUSBAR,
            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,
            currentCapacityA = ratedCurrent,
            conductorMaterial = "",
            insulationType = "",
            installationMethodCode = "",
            busbarMaterial = state.busbarMaterial.ifBlank { "Copper" },
            busbarRatedCurrentA = ratedCurrent,
            busbarShortCircuitKA = shortCircuit
        )
    }

    /**
     * Adds a real internal busbar node to the selected/first panel.
     * The physical enclosure is still rendered by SldPanelEnclosureDrawing.
     */
    fun addBusbar() {
        try {
            val panel = state.selectedNodeId?.let { id ->
                state.nodes.firstOrNull {
                    it.id == id && it.type == SldNodeType.PANEL
                }
            } ?: state.nodes.firstOrNull {
                it.type == SldNodeType.PANEL
            }

            if (panel == null) {
                state.engineeringError =
                    message(
                        "أضف لوحة أولًا قبل إنشاء الباسبار.",
                        "Add a panel before creating a busbar."
                    )
                return
            }

            val existingBus = state.nodes.firstOrNull { bus ->
                bus.type == SldNodeType.BUS &&
                    state.connections.any { connection ->
                        val normalized =
                            normalizeConnection(
                                connection,
                                state.nodes
                            )

                        normalized.connectionType ==
                            SldConnectionType.BUSBAR &&
                            normalized.fromNodeId == panel.id &&
                            normalized.toNodeId == bus.id
                    }
            }

            if (existingBus != null) {
                state.selectedNodeId = existingBus.id
                state.engineeringError = null
                return
            }

            val busPosition = findBusbarPosition(panel)

            val bus = createDefaultNode(
                SldNodeType.BUS,
                busPosition
            ).copy(
                name = defaultNodeName(SldNodeType.BUS)
            )

            val busbar = createBusbarConnection(
                panel,
                bus
            ) ?: return

            state.nodes += bus
            state.connections = normalizeConnections(
                state.nodes,
                state.connections + busbar
            )

            state.selectedNodeId = bus.id
            state.selectedConnectionId = busbar.id
            state.connectionStartId = null
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر إنشاء الباسبار.",
                        "Unable to create the busbar."
                    )
        }
    }

    private fun findBusbarPosition(
        panel: SldNode
    ): Pair<Float, Float> {
        val candidates = listOf(
            panel.x + NODE_WIDTH + 120f to panel.y,
            panel.x - NODE_WIDTH - 120f to panel.y,
            panel.x to panel.y - NODE_HEIGHT - 100f,
            panel.x to panel.y + NODE_HEIGHT + 100f
        )

        candidates
            .map(::snapToGrid)
            .firstOrNull {
                isFreePosition(it.first, it.second)
            }
            ?.let { return it }

        return fallbackFreePosition()
    }

    private fun sameUnorderedPair(
        aFrom: String,
        aTo: String,
        bFrom: String,
        bTo: String
    ): Boolean {
        return (
            aFrom == bFrom && aTo == bTo
            ) || (
            aFrom == bTo && aTo == bFrom
        )
    }

    // ---------------------------------------------------------------------
    // Connection editor
    // ---------------------------------------------------------------------

    fun editConnection(connection: SldConnection) {
        val normalized = normalizeConnection(
            connection,
            state.nodes
        )

        state.editingConnectionId = normalized.id
        state.connectionType = normalized.connectionType.name
        state.length = normalized.lengthMeters.toEngineeringString()
        state.resistance =
            normalized.resistanceOhmPerKm.toEngineeringString()
        state.reactance =
            normalized.reactanceOhmPerKm.toEngineeringString()
        state.cableSize =
            normalized.cableSizeMm2.toEngineeringString()
        state.parallelRuns = normalized.parallelRuns.toString()
        state.capacity =
            normalized.currentCapacityA.toEngineeringString()

        state.conductorMaterial =
            normalized.conductorMaterial.ifBlank { "Copper" }

        state.insulationType =
            normalized.insulationType.ifBlank { "PVC" }

        state.installationMethodCode =
            normalized.installationMethodCode.ifBlank { "B1" }

        state.busbarMaterial =
            normalized.busbarMaterial.ifBlank { "Copper" }

        state.busbarRatedCurrent =
            normalized.busbarRatedCurrentA.toEngineeringString()

        state.busbarShortCircuit =
            normalized.busbarShortCircuitKA.toEngineeringString()

        state.connectionStartId = null
        state.showConnectionDialog = true
    }

    fun saveConnection() {
        val editingId = state.editingConnectionId
        val newFromId = state.connectionStartId
        val newToId = state.selectedNodeId

        state.showConnectionDialog = false
        state.editingConnectionId = null
        state.connectionStartId = null

        try {
            if (editingId != null) {
                saveExistingConnection(editingId)
            } else {
                saveNewConnection(newFromId, newToId)
            }

            state.connections = normalizeConnections(
                state.nodes,
                state.connections
            )

            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.showConnectionDialog = false
            state.editingConnectionId = null
            state.connectionStartId = null
            state.engineeringPackage = null
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "حدث خطأ أثناء حفظ التوصيل، وتم الاحتفاظ بالمخطط.",
                        "An error occurred while saving the connection. The drawing was preserved."
                    )
        }
    }

    private fun saveExistingConnection(
        editingId: String
    ) {
        val existing = state.connections.firstOrNull {
            it.id == editingId
        } ?: run {
            state.engineeringError =
                message(
                    "تعذر العثور على الوصلة المطلوب تعديلها.",
                    "The connection to edit could not be found."
                )
            return
        }

        val from = state.nodes.firstOrNull {
            it.id == existing.fromNodeId
        }

        val to = state.nodes.firstOrNull {
            it.id == existing.toNodeId
        }

        if (from == null || to == null) {
            state.engineeringError =
                message(
                    "العناصر المرتبطة بالوصلة غير موجودة.",
                    "The components associated with this connection do not exist."
                )
            return
        }

        val requestedBusbar =
            state.connectionType.equals(
                SldConnectionType.BUSBAR.name,
                ignoreCase = true
            )

        if (
            requestedBusbar ||
            isPanelBreakerPair(from, to)
        ) {
            saveExistingBusbar(
                existing,
                from,
                to
            )
        } else {
            saveExistingCable(existing)
        }
    }

    private fun saveExistingBusbar(
        existing: SldConnection,
        from: SldNode,
        to: SldNode
    ) {
        if (!isBusbarPair(from, to)) {
            state.engineeringError =
                message(
                    "لا يمكن تحويل هذه الوصلة إلى BUSBAR بين هذين العنصرين.",
                    "This connection cannot be converted to BUSBAR between these components."
                )
            state.showConnectionDialog = true
            state.editingConnectionId = existing.id
            return
        }

        val endpoints = canonicalBusbarEndpoints(from, to)
            ?: run {
                state.engineeringError =
                    message(
                        "تركيب BUSBAR غير صالح.",
                        "Invalid BUSBAR topology."
                    )
                return
            }

        val panel = listOf(
            endpoints.first,
            endpoints.second
        ).firstOrNull {
            it.type == SldNodeType.PANEL
        }

        val estimatedCurrent =
            panel?.let(::estimateBusbarCurrent)
                ?: DEFAULT_BUSBAR_CURRENT_A

        val currentFallback =
            existing.busbarRatedCurrentA
                .takeIf { it > 0.0 }
                ?: estimatedCurrent

        val shortCircuitFallback =
            existing.busbarShortCircuitKA
                .takeIf { it > 0.0 }
                ?: DEFAULT_BUSBAR_SHORT_CIRCUIT_KA

        val updated = existing.copy(
            fromNodeId = endpoints.first.id,
            toNodeId = endpoints.second.id,
            connectionType = SldConnectionType.BUSBAR,
            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,
            currentCapacityA = parseNonNegative(
                state.busbarRatedCurrent,
                currentFallback
            ),
            conductorMaterial = "",
            insulationType = "",
            installationMethodCode = "",
            busbarMaterial =
                state.busbarMaterial.ifBlank {
                    existing.busbarMaterial.ifBlank { "Copper" }
                },
            busbarRatedCurrentA = parseNonNegative(
                state.busbarRatedCurrent,
                currentFallback
            ),
            busbarShortCircuitKA = parseNonNegative(
                state.busbarShortCircuit,
                shortCircuitFallback
            )
        )

        state.connections = state.connections.map {
            if (it.id == existing.id) updated else it
        }

        state.selectedConnectionId = existing.id
        state.selectedNodeId = endpoints.second.id
    }

    private fun saveExistingCable(
        existing: SldConnection
    ) {
        val updated = existing.copy(
            connectionType = SldConnectionType.CABLE,
            lengthMeters = parseNonNegative(
                state.length,
                existing.lengthMeters
            ),
            resistanceOhmPerKm = parseNonNegative(
                state.resistance,
                existing.resistanceOhmPerKm
            ),
            reactanceOhmPerKm = parseNonNegative(
                state.reactance,
                existing.reactanceOhmPerKm
            ),
            cableSizeMm2 = parseNonNegative(
                state.cableSize,
                existing.cableSizeMm2
            ),
            parallelRuns =
                state.parallelRuns.toIntOrNull()
                    ?.coerceAtLeast(1)
                    ?: existing.parallelRuns,
            currentCapacityA = parseNonNegative(
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

        state.connections = state.connections.map {
            if (it.id == existing.id) updated else it
        }

        state.selectedConnectionId = existing.id
        state.selectedNodeId = existing.toNodeId
    }

    private fun saveNewConnection(
        fromId: String?,
        toId: String?
    ) {
        val first = fromId?.let { id ->
            state.nodes.firstOrNull { it.id == id }
        }

        val second = toId?.let { id ->
            state.nodes.firstOrNull { it.id == id }
        }

        if (first == null || second == null) {
            state.engineeringError =
                message(
                    "حدد عنصري البداية والنهاية قبل الحفظ.",
                    "Select valid start and destination components before saving."
                )
            return
        }

        if (first.id == second.id) {
            state.engineeringError =
                message(
                    "لا يمكن توصيل العنصر بنفسه.",
                    "A component cannot be connected to itself."
                )
            return
        }

        val requestedBusbar =
            state.connectionType.equals(
                SldConnectionType.BUSBAR.name,
                ignoreCase = true
            )

        if (requestedBusbar) {
            val busbar = createBusbarConnection(
                first,
                second
            ) ?: run {
                state.showConnectionDialog = true
                return
            }

            state.connections =
                state.connections.filterNot {
                    it.connectionType == SldConnectionType.BUSBAR &&
                        sameUnorderedPair(
                            it.fromNodeId,
                            it.toNodeId,
                            busbar.fromNodeId,
                            busbar.toNodeId
                        )
                } + busbar

            state.selectedNodeId = busbar.toNodeId
            state.selectedConnectionId = busbar.id
            return
        }

        val duplicate = state.connections.any {
            sameUnorderedPair(
                it.fromNodeId,
                it.toNodeId,
                first.id,
                second.id
            )
        }

        if (duplicate) {
            state.selectedNodeId = second.id
            state.engineeringError =
                message(
                    "هذا الاتصال موجود بالفعل.",
                    "This connection already exists."
                )
            return
        }

        val cable = SldConnection(
            id = createConnectionId(),
            fromNodeId = first.id,
            toNodeId = second.id,
            connectionType = SldConnectionType.CABLE,
            lengthMeters = parseNonNegative(
                state.length,
                DEFAULT_CABLE_LENGTH
            ),
            resistanceOhmPerKm = parseNonNegative(
                state.resistance,
                DEFAULT_CABLE_RESISTANCE
            ),
            reactanceOhmPerKm = parseNonNegative(
                state.reactance,
                DEFAULT_CABLE_REACTANCE
            ),
            cableSizeMm2 = parseNonNegative(
                state.cableSize,
                DEFAULT_CABLE_SIZE
            ),
            parallelRuns =
                state.parallelRuns.toIntOrNull()
                    ?.coerceAtLeast(1)
                    ?: 1,
            voltageDropPercent = 0.0,
            currentCapacityA = parseNonNegative(
                state.capacity,
                DEFAULT_CABLE_CAPACITY
            ),
            conductorMaterial =
                state.conductorMaterial.ifBlank { "Copper" },
            insulationType =
                state.insulationType.ifBlank { "XLPE" },
            installationMethodCode =
                state.installationMethodCode.ifBlank { "C" },
            busbarMaterial = "",
            busbarRatedCurrentA = 0.0,
            busbarShortCircuitKA = 0.0
        )

        state.connections += cable
        state.selectedNodeId = second.id
        state.selectedConnectionId = cable.id
    }

    fun cancelConnectionEdit() {
        state.showConnectionDialog = false
        state.editingConnectionId = null
        state.connectionStartId = null
        state.selectedConnectionId = null
    }

    // ---------------------------------------------------------------------
    // Delete / layout / generation
    // ---------------------------------------------------------------------

    fun deleteSelected() {
        try {
            val nodeId = state.selectedNodeId
            val connectionId = state.selectedConnectionId

            if (nodeId != null) {
                state.nodes = state.nodes.filterNot {
                    it.id == nodeId
                }

                state.connections = state.connections.filter {
                    it.fromNodeId != nodeId &&
                        it.toNodeId != nodeId
                }
            }

            if (connectionId != null) {
                state.connections = state.connections.filterNot {
                    it.id == connectionId
                }
            }

            state.clearSelection()
            state.engineeringPackage = null
            state.engineeringError = null

            safePersist()
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "تعذر حذف العنصر.",
                        "Unable to delete the component."
                    )
        }
    }

    fun autoLayout() {
        try {
            val result = SldAutoLayoutEngine.arrange(network())

            state.nodes = result.network.nodes
            state.connections = normalizeConnections(
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
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "فشل ترتيب المخطط.",
                        "Auto layout failed."
                    )
        }
    }

    fun generateCompleteSld() {
        try {
            val generated = SldCompleteGenerator.generate()

            state.nodes = generated.nodes
            state.connections = normalizeConnections(
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
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "فشل إنشاء المخطط الكهربائي.",
                        "Failed to generate the SLD."
                    )
        }
    }

    // ---------------------------------------------------------------------
    // Reports
    // ---------------------------------------------------------------------

    fun runShortCircuit() {
        try {
            val currentNetwork = network()
            val topology =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            if (!topology.valid) {
                state.engineeringError =
                    message(
                        "المخطط غير مكتمل لحساب القصر.",
                        "The SLD is incomplete for short-circuit calculation."
                    )
                return
            }

            val result =
                SldEngineeringFacade.calculateShortCircuit(
                    currentNetwork
                )

            state.reportTitle =
                message(
                    "دراسة القصر الكهربائي",
                    "Short Circuit Study"
                )

            state.reportText = result.toString()
            state.showReport = true
            state.engineeringError = null
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "فشل حساب القصر الكهربائي.",
                        "Short-circuit calculation failed."
                    )
        }
    }

    fun runPanelSchedule() {
        try {
            val panel = state.nodes.firstOrNull {
                it.type == SldNodeType.PANEL
            }

            if (panel == null) {
                state.engineeringError =
                    message(
                        "لا توجد لوحة في المخطط.",
                        "No panel exists in the SLD."
                    )
                return
            }

            val currentNetwork = network()

            val topology =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            if (!topology.valid) {
                state.engineeringError =
                    message(
                        "المخطط غير مكتمل لإنشاء جدول اللوحة.",
                        "The SLD is incomplete for panel schedule."
                    )
                return
            }

            val result =
                SldEngineeringFacade.calculateComplete(
                    network = currentNetwork,
                    panelNodeId = panel.id
                )

            state.engineeringPackage = result

            state.reportTitle =
                message(
                    "جدول اللوحة",
                    "Panel Schedule"
                )

            state.reportText =
                result.panelSchedule?.toString()
                    ?: message(
                        "لم يتم إنشاء جدول اللوحة.",
                        "Panel schedule was not generated."
                    )

            state.showReport = true
            state.engineeringError = null
        } catch (error: Throwable) {
            state.engineeringError =
                error.message?.takeIf(String::isNotBlank)
                    ?: message(
                        "فشل إنشاء جدول اللوحة.",
                        "Panel schedule failed."
                    )
        }
    }

    // ---------------------------------------------------------------------
    // Numeric helpers
    // ---------------------------------------------------------------------

    private fun parsePositive(
        value: String,
        fallback: Double
    ): Double {
        return value.toDoubleOrNull()
            ?.takeIf {
                it.isFinite() && it > 0.0
            }
            ?: fallback.coerceAtLeast(0.000001)
    }

    private fun parseNonNegative(
        value: String,
        fallback: Double
    ): Double {
        return value.toDoubleOrNull()
            ?.takeIf {
                it.isFinite() && it >= 0.0
            }
            ?: fallback.coerceAtLeast(0.0)
    }

    private fun parseRange(
        value: String,
        fallback: Double,
        minValue: Double,
        maxValue: Double
    ): Double {
        return value.toDoubleOrNull()
            ?.takeIf(Double::isFinite)
            ?.coerceIn(minValue, maxValue)
            ?: fallback.coerceIn(minValue, maxValue)
    }

    // ---------------------------------------------------------------------
    // Busbar engineering
    // ---------------------------------------------------------------------

    private fun estimateBusbarCurrent(
        panel: SldNode
    ): Double {
        val kva = panel.ratedKva
        val voltage = panel.voltage

        if (
            kva.isFinite() &&
            voltage.isFinite() &&
            kva > 0.0 &&
            voltage > 0.0
        ) {
            val current =
                kva * 1000.0 /
                    (sqrt(3.0) * voltage)

            if (current.isFinite() && current > 0.0) {
                return max(
                    DEFAULT_BUSBAR_CURRENT_A,
                    current
                )
            }
        }

        return DEFAULT_BUSBAR_CURRENT_A
    }

    // ---------------------------------------------------------------------
    // IDs / formatting
    // ---------------------------------------------------------------------

    private fun createNodeId(): String {
        var id = "node-${System.nanoTime()}"
        var index = 0

        while (state.nodes.any { it.id == id }) {
            index++
            id = "node-${System.nanoTime()}-$index"

            if (index > 1000) break
        }

        return id
    }

    private fun createConnectionId(): String {
        var id = "connection-${System.nanoTime()}"
        var index = 0

        while (state.connections.any { it.id == id }) {
            index++
            id = "connection-${System.nanoTime()}-$index"

            if (index > 1000) break
        }

        return id
    }

    private fun Double.toEngineeringString(): String {
        return if (isFinite()) {
            String.format(
                Locale.US,
                "%.2f",
                this
            )
        } else {
            "0.00"
        }
    }
}
