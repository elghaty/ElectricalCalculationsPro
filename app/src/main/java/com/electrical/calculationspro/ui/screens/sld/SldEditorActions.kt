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
import kotlin.math.max
import kotlin.math.sqrt

class SldEditorActions(
    private val state: SldEditorState
) {

    companion object {

        private const val GRID_SIZE = 40f

        private const val DEFAULT_CABLE_LENGTH = 20.0
        private const val DEFAULT_CABLE_RESISTANCE = 0.075
        private const val DEFAULT_CABLE_REACTANCE = 0.080
        private const val DEFAULT_CABLE_SIZE = 25.0
        private const val DEFAULT_CABLE_CAPACITY = 100.0

        private const val DEFAULT_BUSBAR_CURRENT_A = 250.0
        private const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA = 25.0

        private const val NODE_GAP = 60f
        private const val BUSBAR_GAP = 100f
    }

    private fun network(): SldNetwork {
        return SldNetwork(
            nodes = state.nodes,
            connections = state.connections
        )
    }

    private fun message(
        arabic: String,
        english: String
    ): String {
        return if (
            state.language == AppLanguage.ARABIC
        ) {
            arabic
        } else {
            english
        }
    }

    private fun safePersist() {
        try {
            state.persist()
        } catch (_: Throwable) {
        }
    }

    fun recalculateEngineering() {

        try {

            val currentNetwork =
                network()

            val validation =
                SldDesignValidator.validate(
                    currentNetwork
                )

            if (!validation.valid) {

                state.engineeringPackage = null

                state.engineeringError =
                    validation.errors
                        .joinToString("\n") {
                            "${it.code}: ${it.message}"
                        }

                return
            }

            val result =
                SldEngineeringFacade
                    .calculateComplete(
                        currentNetwork
                    )

            state.engineeringPackage =
                result

            state.engineeringError =
                null

        } catch (error: Throwable) {

            state.engineeringPackage =
                null

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: message(
                        "تعذر تنفيذ الحسابات الهندسية.",
                        "Engineering calculation failed."
                    )
        }
    }

    fun addBusbar() {

        try {

            val panel =
                state.nodes.firstOrNull {
                    it.id ==
                        state.selectedNodeId &&
                        it.type ==
                        SldNodeType.PANEL
                }
                    ?: state.nodes.firstOrNull {
                        it.type ==
                            SldNodeType.PANEL
                    }

            if (panel == null) {

                state.engineeringError =
                    message(
                        "يجب وجود لوحة لإضافة الباسبار.",
                        "A panel is required before adding a busbar."
                    )

                return
            }

            val existingBus =
                state.nodes.firstOrNull { bus ->

                    bus.type ==
                        SldNodeType.BUS &&

                        state.connections.any { connection ->

                            connection.connectionType ==
                                SldConnectionType.BUSBAR &&

                                sameUnorderedPair(
                                    connection.fromNodeId,
                                    connection.toNodeId,
                                    panel.id,
                                    bus.id
                                )
                        }
                }

            if (existingBus != null) {

                state.selectedNodeId =
                    existingBus.id

                state.selectedConnectionId =
                    state.connections
                        .firstOrNull { connection ->
                            connection.connectionType ==
                                SldConnectionType.BUSBAR &&
                                sameUnorderedPair(
                                    connection.fromNodeId,
                                    connection.toNodeId,
                                    panel.id,
                                    existingBus.id
                                )
                        }
                        ?.id

                return
            }

            val position =
                findBusbarPosition(
                    panel
                )

            val bus =
                SldNode(
                    id = createNodeId(),
                    name = "BUS-${state.nodes.count { it.type == SldNodeType.BUS } + 1}",
                    type = SldNodeType.BUS,
                    x = position.first,
                    y = position.second,
                    voltage = panel.voltage,
                    powerFactor = panel.powerFactor,
                    loadKw = 0.0,
                    demandFactor = 1.0,
                    ratedKva = panel.ratedKva,
                    transformerPercentZ = 0.0,
                    generatorXdSubtransient = 0.0,
                    sourceShortCircuitMva = 0.0
                )

            val connection =
                createBusbarConnection(
                    panel,
                    bus
                )

            if (connection == null) {
                state.engineeringError =
                    message(
                        "تعذر إنشاء اتصال الباسبار.",
                        "Unable to create the busbar connection."
                    )
                return
            }

            state.nodes += bus

            state.connections =
                state.connections.filterNot {
                    it.connectionType ==
                        SldConnectionType.BUSBAR &&
                        sameUnorderedPair(
                            it.fromNodeId,
                            it.toNodeId,
                            panel.id,
                            bus.id
                        )
                } + connection

            state.selectedNodeId =
                bus.id

            state.selectedConnectionId =
                connection.id

            state.engineeringPackage =
                null

            state.engineeringError =
                null

            safePersist()
            recalculateEngineering()

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: message(
                        "تعذر إضافة الباسبار.",
                        "Unable to add busbar."
                    )
        }
    }

    private fun createBusbarConnection(
        panel: SldNode,
        bus: SldNode
    ): SldConnection? {

        if (panel.id == bus.id) {
            return null
        }

        return SldConnection(
            id = createConnectionId(),
            fromNodeId = panel.id,
            toNodeId = bus.id,
            connectionType = SldConnectionType.BUSBAR,

            lengthMeters = 0.0,
            resistanceOhmPerKm = 0.0,
            reactanceOhmPerKm = 0.0,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            voltageDropPercent = 0.0,

            // BUSBAR is not a cable.
            currentCapacityA = 0.0,

            conductorMaterial = "",
            insulationType = "",
            installationMethodCode = "",

            busbarMaterial = "Copper",
            busbarRatedCurrentA =
                estimateBusbarCurrent(panel),
            busbarShortCircuitKA =
                DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
        )
    }

    private fun findBusbarPosition(
        panel: SldNode
    ): Pair<Float, Float> {

        val x =
            panel.x +
                NODE_GAP

        val y =
            panel.y +
                BUSBAR_GAP

        val candidates =
            listOf(
                Pair(x, y),
                Pair(x + GRID_SIZE * 2f, y),
                Pair(x, y + GRID_SIZE * 2f),
                Pair(
                    panel.x - GRID_SIZE * 3f,
                    panel.y
                ),
                Pair(
                    panel.x,
                    panel.y + GRID_SIZE * 4f
                )
            )

        return candidates
            .map { candidate ->
                snapToGrid(
                    candidate.first,
                    candidate.second
                )
            }
            .firstOrNull { candidate ->
                isFreePosition(
                    candidate.first,
                    candidate.second
                )
            }
            ?: snapToGrid(
                x,
                y
            )
    }

    private fun isFreePosition(
        x: Float,
        y: Float
    ): Boolean {

        val minimumDistance =
            GRID_SIZE * 2f

        return state.nodes.none { node ->

            val dx =
                node.x - x

            val dy =
                node.y - y

            sqrt(
                dx * dx +
                    dy * dy
            ) < minimumDistance
        }
    }

    private fun snapToGrid(
        x: Float,
        y: Float
    ): Pair<Float, Float> {

        return Pair(
            (x / GRID_SIZE).toInt() *
                GRID_SIZE,
            (y / GRID_SIZE).toInt() *
                GRID_SIZE
        )
    }

    private fun sameUnorderedPair(
        firstFrom: String,
        firstTo: String,
        secondFrom: String,
        secondTo: String
    ): Boolean {

        return (
            firstFrom == secondFrom &&
                firstTo == secondTo
            ) || (
            firstFrom == secondTo &&
                firstTo == secondFrom
        )
    }

    private fun normalizeConnections(
        nodes: List<SldNode>,
        connections: List<SldConnection>
    ): List<SldConnection> {

        val nodeMap =
            nodes.associateBy {
                it.id
            }

        return connections.map { connection ->

            if (
                connection.connectionType !=
                    SldConnectionType.BUSBAR
            ) {
                return@map connection
            }

            val from =
                nodeMap[
                    connection.fromNodeId
                ]

            val to =
                nodeMap[
                    connection.toNodeId
                ]

            if (
                from == null ||
                to == null
            ) {
                return@map connection
            }

            val ordered =
                when {
                    from.type ==
                        SldNodeType.PANEL &&
                        to.type ==
                        SldNodeType.BUS -> {
                        from to to
                    }

                    from.type ==
                        SldNodeType.BUS &&
                        to.type ==
                        SldNodeType.PANEL -> {
                        to to from
                    }

                    from.type ==
                        SldNodeType.BUS &&
                        to.type ==
                        SldNodeType.BREAKER -> {
                        from to to
                    }

                    from.type ==
                        SldNodeType.BREAKER &&
                        to.type ==
                        SldNodeType.BUS -> {
                        to to from
                    }

                    from.type ==
                        SldNodeType.PANEL &&
                        to.type ==
                        SldNodeType.BREAKER -> {
                        from to to
                    }

                    from.type ==
                        SldNodeType.BREAKER &&
                        to.type ==
                        SldNodeType.PANEL -> {
                        to to from
                    }

                    else -> null
                }

            if (ordered == null) {
                connection
            } else {
                connection.copy(
                    fromNodeId = ordered.first.id,
                    toNodeId = ordered.second.id,

                    lengthMeters = 0.0,
                    resistanceOhmPerKm = 0.0,
                    reactanceOhmPerKm = 0.0,
                    cableSizeMm2 = 0.0,
                    parallelRuns = 1,
                    voltageDropPercent = 0.0,

                    // BUSBAR is never a cable.
                    currentCapacityA = 0.0,

                    conductorMaterial = "",
                    insulationType = "",
                    installationMethodCode = ""
                )
            }
        }
    }

    fun saveExistingBusbar(
        existing: SldConnection
    ) {

        val endpoints =
            Pair(
                state.nodes.firstOrNull {
                    it.id ==
                        existing.fromNodeId
                },
                state.nodes.firstOrNull {
                    it.id ==
                        existing.toNodeId
                }
            )

        if (
            endpoints.first == null ||
            endpoints.second == null
        ) {
            state.engineeringError =
                message(
                    "عناصر اتصال الباسبار غير موجودة.",
                    "Busbar endpoints no longer exist."
                )
            return
        }

        val currentFallback =
            existing.busbarRatedCurrentA
                .takeIf {
                    it > 0.0
                }
                ?: DEFAULT_BUSBAR_CURRENT_A

        val shortCircuitFallback =
            existing.busbarShortCircuitKA
                .takeIf {
                    it > 0.0
                }
                ?: DEFAULT_BUSBAR_SHORT_CIRCUIT_KA

        val updated =
            existing.copy(

                connectionType =
                    SldConnectionType.BUSBAR,

                lengthMeters = 0.0,
                resistanceOhmPerKm = 0.0,
                reactanceOhmPerKm = 0.0,
                cableSizeMm2 = 0.0,
                parallelRuns = 1,
                voltageDropPercent = 0.0,

                // Critical: BUSBAR is not cable capacity.
                currentCapacityA = 0.0,

                conductorMaterial = "",
                insulationType = "",
                installationMethodCode = "",

                busbarMaterial =
                    state.busbarMaterial
                        .ifBlank {
                            existing.busbarMaterial
                                .ifBlank {
                                    "Copper"
                                }
                        },

                busbarRatedCurrentA =
                    parseNonNegative(
                        state.busbarRatedCurrent,
                        currentFallback
                    ),

                busbarShortCircuitKA =
                    parseNonNegative(
                        state.busbarShortCircuit,
                        shortCircuitFallback
                    )
            )

        state.connections =
            state.connections.map {
                if (it.id == existing.id) {
                    updated
                } else {
                    it
                }
            }

        state.selectedConnectionId =
            existing.id

        state.selectedNodeId =
            endpoints.second.id
    }

    fun saveExistingCable(
        existing: SldConnection
    ) {

        val updated =
            existing.copy(

                connectionType =
                    SldConnectionType.CABLE,

                lengthMeters =
                    parseNonNegative(
                        state.length,
                        existing.lengthMeters
                    ),

                resistanceOhmPerKm =
                    parseNonNegative(
                        state.resistance,
                        existing.resistanceOhmPerKm
                    ),

                reactanceOhmPerKm =
                    parseNonNegative(
                        state.reactance,
                        existing.reactanceOhmPerKm
                    ),

                cableSizeMm2 =
                    parseNonNegative(
                        state.cableSize,
                        existing.cableSizeMm2
                    ),

                parallelRuns =
                    state.parallelRuns
                        .toIntOrNull()
                        ?.coerceAtLeast(1)
                        ?: existing.parallelRuns,

                currentCapacityA =
                    parseNonNegative(
                        state.capacity,
                        existing.currentCapacityA
                    ),

                conductorMaterial =
                    state.conductorMaterial
                        .ifBlank {
                            existing.conductorMaterial
                        },

                insulationType =
                    state.insulationType
                        .ifBlank {
                            existing.insulationType
                        },

                installationMethodCode =
                    state.installationMethodCode
                        .ifBlank {
                            existing.installationMethodCode
                        },

                busbarMaterial = "",
                busbarRatedCurrentA = 0.0,
                busbarShortCircuitKA = 0.0
            )

        state.connections =
            state.connections.map {
                if (it.id == existing.id) {
                    updated
                } else {
                    it
                }
            }

        state.selectedConnectionId =
            existing.id

        state.selectedNodeId =
            existing.toNodeId
    }

    fun deleteSelected() {

        try {

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

            state.engineeringPackage =
                null

            state.engineeringError =
                null

            safePersist()
            recalculateEngineering()

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: message(
                        "تعذر حذف العنصر.",
                        "Unable to delete the component."
                    )
        }
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

            state.engineeringPackage =
                null

            state.engineeringError =
                null

            safePersist()
            recalculateEngineering()

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: message(
                        "فشل ترتيب المخطط.",
                        "Auto layout failed."
                    )
        }
    }

    private fun parsePositive(
        value: String,
        fallback: Double
    ): Double {

        return value
            .toDoubleOrNull()
            ?.takeIf {
                it.isFinite() &&
                    it > 0.0
            }
            ?: fallback.coerceAtLeast(
                0.000001
            )
    }

    private fun parseNonNegative(
        value: String,
        fallback: Double
    ): Double {

        return value
            .toDoubleOrNull()
            ?.takeIf {
                it.isFinite() &&
                    it >= 0.0
            }
            ?: fallback.coerceAtLeast(
                0.0
            )
    }

    private fun parseRange(
        value: String,
        fallback: Double,
        minValue: Double,
        maxValue: Double
    ): Double {

        return value
            .toDoubleOrNull()
            ?.takeIf {
                it.isFinite()
            }
            ?.coerceIn(
                minValue,
                maxValue
            )
            ?: fallback.coerceIn(
                minValue,
                maxValue
            )
    }

    private fun estimateBusbarCurrent(
        panel: SldNode
    ): Double {

        val kva =
            panel.ratedKva

        val voltage =
            panel.voltage

        if (
            kva.isFinite() &&
            voltage.isFinite() &&
            kva > 0.0 &&
            voltage > 0.0
        ) {

            val current =
                kva *
                    1000.0 /
                    (
                        sqrt(3.0) *
                            voltage
                        )

            if (
                current.isFinite() &&
                current > 0.0
            ) {
                return max(
                    DEFAULT_BUSBAR_CURRENT_A,
                    current
                )
            }
        }

        return DEFAULT_BUSBAR_CURRENT_A
    }

    private fun createNodeId(): String {

        var id =
            "node-${System.nanoTime()}"

        var index = 0

        while (
            state.nodes.any {
                it.id == id
            }
        ) {

            index++

            id =
                "node-${System.nanoTime()}-$index"

            if (index > 1000) {
                break
            }
        }

        return id
    }

    private fun createConnectionId(): String {

        var id =
            "connection-${System.nanoTime()}"

        var index = 0

        while (
            state.connections.any {
                it.id == id
            }
        ) {

            index++

            id =
                "connection-${System.nanoTime()}-$index"

            if (index > 1000) {
                break
            }
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
