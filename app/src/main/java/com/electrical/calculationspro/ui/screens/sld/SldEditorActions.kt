package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldAutoLayoutEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldDesignValidator
import com.electrical.calculationspro.data.SldEngineeringReportEngine
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.DesignProjects
import java.util.ArrayDeque
import java.util.Locale

/**
 * Central action/controller layer for the interactive SLD editor.
 *
 * UI
 * ↓
 * SldEditorActions
 * ↓
 * DesignProjectCoreBridge
 * ↓
 * Engineering layer
 *
 * Responsibilities:
 *
 * - Edit SLD nodes.
 * - Create and edit connections.
 * - Maintain electrical connection direction.
 * - Automatically classify internal panel connections as BUSBAR.
 * - Persist the current topology.
 * - Trigger engineering recalculation after changes.
 * - Run validation and reports.
 *
 * IMPORTANT:
 *
 * This class does NOT implement engineering formulas.
 * Engineering calculations remain inside the engineering/data layer.
 */
class SldEditorActions(
    private val state: SldEditorState,
    private val language: AppLanguage
) {

    private val arabic: Boolean
        get() = language == AppLanguage.ARABIC

    private fun network(): SldNetwork =
        SldNetwork(
            nodes = state.nodes,
            connections = state.connections
        )

    // ============================================================
    // PROJECT LOAD / SAVE
    // ============================================================

    fun loadProjectNetwork() {

        try {

            val project =
                DesignProjects.getActive()

            if (project != null) {

                val loaded =
                    DesignProjectCoreBridge.getProjectSld(
                        project
                    )

                state.nodes =
                    loaded.nodes

                state.connections =
                    loaded.connections
            }

            state.engineeringError = null

            recalculateEngineering()

        } catch (e: Exception) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "تعذر تحميل مخطط SLD."
                    } else {
                        "Unable to load SLD project."
                    }
        }
    }

    fun saveProjectNetwork() {

        try {

            val project =
                DesignProjects.getActive()

            if (project != null) {

                DesignProjectCoreBridge.saveProjectSld(
                    project = project,
                    network = network()
                )
            }

        } catch (e: Exception) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "تعذر حفظ مخطط SLD."
                    } else {
                        "Unable to save SLD project."
                    }
        }
    }

    // ============================================================
    // SAVE + ENGINEERING RECALCULATION
    // ============================================================

    fun saveAndRecalculate() {

        try {

            state.engineeringError = null

            saveProjectNetwork()

            val project =
                DesignProjects.getActive()

            if (project == null) {

                state.engineeringPackage = null

                state.engineeringError =
                    if (arabic) {
                        "لا يوجد مشروع نشط."
                    } else {
                        "No active design project."
                    }

                return
            }

            state.engineeringPackage =
                DesignProjectCoreBridge
                    .calculateCurrentProjectSld(
                        project = project,
                        network = network()
                    )

        } catch (e: Exception) {

            state.engineeringPackage = null

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "تعذر تحديث الحسابات الهندسية."
                    } else {
                        "Engineering recalculation failed."
                    }
        }
    }

    fun recalculateEngineering() {

        try {

            state.engineeringError = null

            val project =
                DesignProjects.getActive()

            if (project == null) {

                state.engineeringPackage = null

                state.engineeringError =
                    if (arabic) {
                        "لا يوجد مشروع نشط."
                    } else {
                        "No active design project."
                    }

                return
            }

            state.engineeringPackage =
                DesignProjectCoreBridge
                    .calculateCurrentProjectSld(
                        project = project,
                        network = network()
                    )

        } catch (e: Exception) {

            state.engineeringPackage = null

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "تعذر تنفيذ الحسابات الهندسية."
                    } else {
                        "Engineering calculation failed."
                    }
        }
    }

    // ============================================================
    // DESIGN VALIDATION
    // ============================================================

    fun validateDesign() {

        try {

            val result =
                SldDesignValidator.validate(
                    network()
                )

            state.reportTitle =
                if (arabic) {
                    "فحص التصميم"
                } else {
                    "DESIGN VALIDATION"
                }

            state.reportText =
                result.toString()

            state.showReport = true

        } catch (e: Exception) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "فشل فحص التصميم."
                    } else {
                        "Validation failed."
                    }
        }
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

            state.connections =
                result.network.connections

            saveAndRecalculate()

        } catch (e: Exception) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "فشل الترتيب التلقائي."
                    } else {
                        "Auto layout failed."
                    }
        }
    }

    // ============================================================
    // NODE EDITOR
    // ============================================================

    fun resetNodeEditor(
        type: SldNodeType
    ) {

        state.editingNodeId = null

        state.nodeType =
            type

        state.name =
            when (type) {

                SldNodeType.SOURCE ->
                    "Utility Source"

                SldNodeType.TRANSFORMER ->
                    "Transformer"

                SldNodeType.GENERATOR ->
                    "Generator"

                SldNodeType.BUS ->
                    "Main Bus"

                SldNodeType.PANEL ->
                    "Panel"

                SldNodeType.BREAKER ->
                    "Breaker"

                SldNodeType.LOAD ->
                    "Load"
            }

        state.voltage = "400"
        state.loadKw = "100"
        state.pf = "0.90"
        state.demand = "0.80"
        state.kva = "500"
        state.transformerZ = "6"
        state.generatorXd = "15"
        state.sourceMva = "500"

        state.showNodeDialog = true
    }

    fun editNode(
        node: SldNode
    ) {

        state.editingNodeId =
            node.id

        state.nodeType =
            node.type

        state.name =
            node.name

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
            node.transformerPercentZ
                .toEngineeringString()

        state.generatorXd =
            node.generatorXdSubtransient
                .toEngineeringString()

        state.sourceMva =
            node.sourceShortCircuitMva
                .toEngineeringString()

        state.showNodeDialog = true
    }

    // ============================================================
    // SAVE NODE
    // ============================================================

    fun saveNode() {

        val name =
            state.name
                .trim()
                .ifEmpty {
                    if (arabic) {
                        "عنصر"
                    } else {
                        "Element"
                    }
                }

        val voltage =
            state.voltage
                .toDoubleOrNull()
                ?.coerceAtLeast(1.0)
                ?: 400.0

        val loadKw =
            state.loadKw
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val pf =
            state.pf
                .toDoubleOrNull()
                ?.coerceIn(0.1, 1.0)
                ?: 0.90

        val demand =
            state.demand
                .toDoubleOrNull()
                ?.coerceIn(0.0, 1.0)
                ?: 1.0

        val kva =
            state.kva
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val transformerZ =
            state.transformerZ
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val generatorXd =
            state.generatorXd
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val sourceMva =
            state.sourceMva
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val existingId =
            state.editingNodeId

        if (existingId != null) {

            state.nodes =
                state.nodes.map { old ->

                    if (old.id == existingId) {

                        old.copy(
                            name = name,
                            type = state.nodeType,
                            voltage = voltage,
                            loadKw = loadKw,
                            powerFactor = pf,
                            demandFactor = demand,
                            ratedKva = kva,
                            transformerPercentZ =
                                transformerZ,
                            generatorXdSubtransient =
                                generatorXd,
                            sourceShortCircuitMva =
                                sourceMva
                        )

                    } else {
                        old
                    }
                }

        } else {

            val anchor =
                state.selectedNodeId
                    ?.let { id ->
                        state.nodes.firstOrNull {
                            it.id == id
                        }
                    }

            val position =
                findFreeNodePosition(
                    anchor = anchor,
                    type = state.nodeType,
                    existing = state.nodes
                )

            val node =
                SldNode(
                    id =
                        createNodeId(),

                    name =
                        name,

                    type =
                        state.nodeType,

                    x =
                        position.first,

                    y =
                        position.second,

                    voltage =
                        voltage,

                    loadKw =
                        loadKw,

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
                state.nodes + node
        }

        state.clearDialogs()

        saveAndRecalculate()
    }

    // ============================================================
    // SMART NODE PLACEMENT
    // ============================================================

    private fun findFreeNodePosition(
        anchor: SldNode?,
        type: SldNodeType,
        existing: List<SldNode>
    ): Pair<Float, Float> {

        val baseX =
            anchor?.x
                ?: 520f

        val baseY =
            anchor?.y
                ?: 160f

        val stepX =
            NODE_WIDTH +
                NODE_CLEARANCE

        val stepY =
            NODE_HEIGHT +
                NODE_CLEARANCE

        val preferred =
            when (type) {

                SldNodeType.SOURCE ->
                    listOf(
                        baseX - stepX to baseY,
                        baseX to baseY - stepY,
                        baseX - stepX to baseY - stepY
                    )

                SldNodeType.TRANSFORMER ->
                    listOf(
                        baseX + stepX to baseY,
                        baseX to baseY + stepY,
                        baseX + stepX to baseY + stepY
                    )

                SldNodeType.BUS ->
                    listOf(
                        baseX + stepX to baseY,
                        baseX to baseY + stepY,
                        baseX + stepX to baseY + stepY
                    )

                SldNodeType.PANEL ->
                    listOf(
                        baseX + stepX to baseY,
                        baseX + stepX to baseY + stepY,
                        baseX to baseY + stepY
                    )

                SldNodeType.BREAKER ->
                    listOf(
                        baseX + stepX to baseY,
                        baseX + stepX to baseY + stepY,
                        baseX to baseY + stepY
                    )

                SldNodeType.LOAD ->
                    listOf(
                        baseX + stepX to baseY,
                        baseX + stepX to baseY + stepY,
                        baseX + stepX to baseY - stepY
                    )

                SldNodeType.GENERATOR ->
                    listOf(
                        baseX - stepX to baseY,
                        baseX - stepX to baseY + stepY,
                        baseX to baseY - stepY
                    )
            }

        val candidates =
            mutableListOf<Pair<Float, Float>>()

        candidates += preferred

        for (ring in 2..12) {

            val dx =
                stepX * ring

            val dy =
                stepY * ring

            candidates +=
                baseX + dx to baseY

            candidates +=
                baseX + dx to baseY + dy

            candidates +=
                baseX + dx to baseY - dy

            candidates +=
                baseX - dx to baseY

            candidates +=
                baseX - dx to baseY + dy

            candidates +=
                baseX - dx to baseY - dy

            candidates +=
                baseX to baseY + dy

            candidates +=
                baseX to baseY - dy

            candidates +=
                baseX + dx to baseY + dy

            candidates +=
                baseX + dx to baseY - dy

            candidates +=
                baseX - dx to baseY + dy

            candidates +=
                baseX - dx to baseY - dy
        }

        fun isFree(
            x: Float,
            y: Float
        ): Boolean {

            val left =
                x

            val top =
                y

            val right =
                x + NODE_WIDTH

            val bottom =
                y + NODE_HEIGHT

            return existing.none { node ->

                val nodeLeft =
                    node.x

                val nodeTop =
                    node.y

                val nodeRight =
                    node.x +
                        NODE_WIDTH

                val nodeBottom =
                    node.y +
                        NODE_HEIGHT

                val horizontal =
                    left <
                        nodeRight +
                        NODE_CLEARANCE &&
                        right >
                        nodeLeft -
                        NODE_CLEARANCE

                val vertical =
                    top <
                        nodeBottom +
                        NODE_CLEARANCE &&
                        bottom >
                        nodeTop -
                        NODE_CLEARANCE

                horizontal &&
                    vertical
            }
        }

        for (candidate in candidates) {

            val x =
                candidate.first
                    .coerceAtLeast(
                        MIN_POSITION
                    )

            val y =
                candidate.second
                    .coerceAtLeast(
                        MIN_POSITION
                    )

            if (
                isFree(
                    x = x,
                    y = y
                )
            ) {
                return x to y
            }
        }

        var x =
            (
                existing.maxOfOrNull {
                    it.x
                }
                    ?: baseX
                ) +
                stepX

        var y =
            baseY

        var guard =
            0

        while (
            !isFree(
                x,
                y
            ) &&
            guard < 200
        ) {

            x += stepX

            guard++
        }

        return x.coerceAtLeast(
            MIN_POSITION
        ) to
            y.coerceAtLeast(
                MIN_POSITION
            )
    }

    // ============================================================
    // CONNECTION EDITOR
    // ============================================================

    fun editConnection(
        connection: SldConnection
    ) {

        state.editingConnectionId =
            connection.id

        state.connectionType =
            connection.connectionType.name

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

        state.showConnectionDialog =
            true
    }

    // ============================================================
    // SAVE CONNECTION
    // ============================================================

    fun saveConnection() {

        val rawFromId =
            state.connectionStartId

        val rawToId =
            state.selectedNodeId

        val existingId =
            state.editingConnectionId

        if (existingId != null) {

            val connectionType =
                normalizeConnectionType(
                    connectionType =
                        state.connectionType
                )

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

            val busbarRatedCurrent =
                state.busbarRatedCurrent
                    .toDoubleOrNull()
                    ?.coerceAtLeast(0.0)
                    ?: 0.0

            val busbarShortCircuit =
                state.busbarShortCircuit
                    .toDoubleOrNull()
                    ?.coerceAtLeast(0.0)
                    ?: 0.0

            state.connections =
                state.connections.map { old ->

                    if (old.id != existingId) {

                        old

                    } else {

                        old.copy(

                            connectionType =
                                connectionType,

                            lengthMeters =
                                if (
                                    connectionType ==
                                    SldConnectionType.CABLE
                                ) {
                                    length
                                } else {
                                    0.0
                                },

                            resistanceOhmPerKm =
                                if (
                                    connectionType ==
                                    SldConnectionType.CABLE
                                ) {
                                    resistance
                                } else {
                                    0.0
                                },

                            reactanceOhmPerKm =
                                if (
                                    connectionType ==
                                    SldConnectionType.CABLE
                                ) {
                                    reactance
                                } else {
                                    0.0
                                },

                            cableSizeMm2 =
                                if (
                                    connectionType ==
                                    SldConnectionType.CABLE
                                ) {
                                    cableSize
                                } else {
                                    0.0
                                },

                            parallelRuns =
                                if (
                                    connectionType ==
                                    SldConnectionType.CABLE
                                ) {
                                    runs
                                } else {
                                    1
                                },

                            currentCapacityA =
                                if (
                                    connectionType ==
                                    SldConnectionType.CABLE
                                ) {
                                    capacity
                                } else {
                                    busbarRatedCurrent
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
                                busbarRatedCurrent,

                            busbarShortCircuitKA =
                                busbarShortCircuit
                        )
                    }
                }

            state.clearDialogs()

            saveAndRecalculate()

            return
        }

        if (
            rawFromId == null ||
            rawToId == null
        ) {

            state.engineeringError =
                if (arabic) {
                    "اختر عنصرين لإنشاء التوصيل."
                } else {
                    "Select two components to create a connection."
                }

            return
        }

        if (
            rawFromId ==
            rawToId
        ) {

            state.engineeringError =
                if (arabic) {
                    "لا يمكن توصيل العنصر بنفسه."
                } else {
                    "A component cannot be connected to itself."
                }

            return
        }

        val fromNode =
            state.nodes.firstOrNull {
                it.id == rawFromId
            }

        val toNode =
            state.nodes.firstOrNull {
                it.id == rawToId
            }

        if (
            fromNode == null ||
            toNode == null
        ) {

            state.engineeringError =
                if (arabic) {
                    "العنصر المحدد غير موجود."
                } else {
                    "Selected component does not exist."
                }

            return
        }

        val directed =
            normalizeConnectionDirection(
                from = fromNode,
                to = toNode
            )

        val finalFrom =
            directed.first

        val finalTo =
            directed.second

        val duplicate =
            state.connections.any { connection ->

                (
                    connection.fromNodeId ==
                        finalFrom.id &&
                        connection.toNodeId ==
                        finalTo.id
                    ) ||
                    (
                        connection.fromNodeId ==
                            finalTo.id &&
                            connection.toNodeId ==
                            finalFrom.id
                        )
            }

        if (duplicate) {

            state.engineeringError =
                if (arabic) {
                    "هذا التوصيل موجود بالفعل."
                } else {
                    "This connection already exists."
                }

            state.connectionStartId = null

            return
        }

        val automaticType =
            automaticConnectionType(
                from = finalFrom,
                to = finalTo
            )

        if (
            automaticType ==
            SldConnectionType.BUSBAR
        ) {

            createBusbarConnection(
                from = finalFrom,
                to = finalTo
            )

            state.clearDialogs()

            saveAndRecalculate()

            return
        }

        state.connectionStartId =
            finalFrom.id

        state.selectedNodeId =
            finalTo.id

        state.editingConnectionId =
            null

        state.connectionType =
            SldConnectionType.CABLE.name

        state.conductorMaterial =
            "Copper"

        state.insulationType =
            "PVC"

        state.installationMethodCode =
            "B1"

        state.busbarMaterial =
            "Copper"

        state.busbarRatedCurrent =
            "400"

        state.busbarShortCircuit =
            "25"

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

    // ============================================================
    // SMART CONNECTION START / COMPLETE
    // ============================================================

    /**
     * Compatibility entry point for toolbar usage.
     *
     * The toolbar may use the currently selected node.
     *
     * Canvas node clicks should use the overload below and pass
     * the clicked node ID directly.
     */
    fun startOrCompleteConnection() {

        val selectedId =
            state.selectedNodeId

        if (selectedId == null) {

            state.engineeringError =
                if (arabic) {
                    "حدد العنصر الأول ثم اضغط توصيل."
                } else {
                    "Select the first component, then press Connect."
                }

            return
        }

        startOrCompleteConnection(
            nodeId = selectedId
        )
    }

    /**
     * Reliable connection entry point.
     *
     * The node ID comes directly from the click event.
     *
     * This avoids depending on Compose state having already
     * propagated a selectedNodeId change.
     */
    fun startOrCompleteConnection(
        nodeId: String
    ) {

        val selectedNode =
            state.nodes.firstOrNull {
                it.id == nodeId
            }

        if (selectedNode == null) {

            state.engineeringError =
                if (arabic) {
                    "العنصر المحدد غير موجود."
                } else {
                    "Selected component does not exist."
                }

            return
        }

        /*
         * Keep UI selection synchronized, but do not use the
         * state update as the source of truth for this operation.
         */
        state.selectedNodeId =
            nodeId

        state.selectedConnectionId =
            null

        val startId =
            state.connectionStartId

        /*
         * First node.
         */
        if (startId == null) {

            state.connectionStartId =
                nodeId

            state.engineeringError =
                null

            return
        }

        /*
         * Same node cancels the operation.
         */
        if (
            startId ==
            nodeId
        ) {

            state.connectionStartId =
                null

            state.engineeringError =
                if (arabic) {
                    "تم إلغاء التوصيل."
                } else {
                    "Connection cancelled."
                }

            return
        }

        val startNode =
            state.nodes.firstOrNull {
                it.id == startId
            }

        if (startNode == null) {

            state.connectionStartId =
                null

            state.engineeringError =
                if (arabic) {
                    "العنصر الأول غير موجود."
                } else {
                    "The first component no longer exists."
                }

            return
        }

        /*
         * Electrical direction is determined by the actual
         * existing directed topology.
         *
         * We do NOT use an artificial component-type ranking.
         *
         * Valid examples:
         *
         * SOURCE -> BREAKER -> BUS
         * BUS -> TRANSFORMER
         * TRANSFORMER -> BREAKER -> BUS
         * BUS -> PANEL
         * PANEL -> BREAKER
         * BREAKER -> LOAD
         */
        val directed =
            normalizeConnectionDirection(
                from = startNode,
                to = selectedNode
            )

        val from =
            directed.first

        val to =
            directed.second

        /*
         * Never create self connection.
         */
        if (
            from.id ==
            to.id
        ) {

            state.connectionStartId =
                null

            return
        }

        /*
         * Prevent duplicate connection in either direction.
         */
        val duplicate =
            state.connections.any { connection ->

                (
                    connection.fromNodeId ==
                        from.id &&
                        connection.toNodeId ==
                        to.id
                    ) ||
                    (
                        connection.fromNodeId ==
                            to.id &&
                            connection.toNodeId ==
                            from.id
                        )
            }

        if (duplicate) {

            state.connectionStartId =
                null

            state.engineeringError =
                if (arabic) {
                    "هذا التوصيل موجود بالفعل."
                } else {
                    "This connection already exists."
                }

            return
        }

        /*
         * Automatically classify the connection.
         */
        val type =
            automaticConnectionType(
                from = from,
                to = to
            )

        /*
         * PANEL -> BREAKER
         *
         * Internal panel busbar.
         *
         * No cable dialog.
         * No cable length.
         * No cable size.
         */
        if (
            type ==
            SldConnectionType.BUSBAR
        ) {

            createBusbarConnection(
                from = from,
                to = to
            )

            state.connectionStartId =
                null

            state.selectedNodeId =
                to.id

            state.selectedConnectionId =
                state.connections.lastOrNull()?.id

            state.clearDialogs()

            saveAndRecalculate()

            return
        }

        /*
         * External feeder.
         *
         * Keep the normalized direction in state so that
         * saveConnection() creates FROM -> TO correctly.
         */
        state.connectionStartId =
            from.id

        state.selectedNodeId =
            to.id

        state.editingConnectionId =
            null

        state.connectionType =
            SldConnectionType.CABLE.name

        state.conductorMaterial =
            "Copper"

        state.insulationType =
            "PVC"

        state.installationMethodCode =
            "B1"

        state.busbarMaterial =
            "Copper"

        state.busbarRatedCurrent =
            "400"

        state.busbarShortCircuit =
            "25"

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

    // ============================================================
    // BUSBAR CREATION
    // ============================================================

    private fun createBusbarConnection(
        from: SldNode,
        to: SldNode
    ) {

        val ratedCurrent =
            estimateBusbarCurrent(
                panel = from
            )

        val shortCircuit =
            DEFAULT_BUSBAR_SHORT_CIRCUIT_KA

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

                lengthMeters =
                    0.0,

                resistanceOhmPerKm =
                    0.0,

                reactanceOhmPerKm =
                    0.0,

                cableSizeMm2 =
                    0.0,

                parallelRuns =
                    1,

                voltageDropPercent =
                    0.0,

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
                    shortCircuit
            )

        state.connections =
            state.connections +
                connection
    }

    private fun estimateBusbarCurrent(
        panel: SldNode
    ): Double {

        if (
            panel.ratedKva > 0.0 &&
            panel.voltage > 0.0
        ) {

            val calculated =
                panel.ratedKva *
                    1000.0 /
                    (
                        kotlin.math.sqrt(3.0) *
                            panel.voltage
                        )

            if (
                calculated.isFinite() &&
                calculated > 0.0
            ) {

                return calculated.coerceAtLeast(
                    DEFAULT_BUSBAR_CURRENT_A
                )
            }
        }

        return DEFAULT_BUSBAR_CURRENT_A
    }

    // ============================================================
    // CONNECTION DIRECTION
    // ============================================================

    /**
     * Determines connection direction from the actual directed
     * network instead of using a fixed ranking for node types.
     *
     * The old topologyRank() approach was incorrect because an
     * electrical SLD cannot be represented by a universal type
     * ordering.
     *
     * Examples that must remain valid:
     *
     * SOURCE -> BREAKER
     * BREAKER -> BUS
     * BUS -> TRANSFORMER
     * TRANSFORMER -> BREAKER
     * BREAKER -> BUS
     * BUS -> PANEL
     * PANEL -> BREAKER
     * BREAKER -> LOAD
     *
     * If the existing directed graph already proves that "to" is
     * upstream of "from", the direction is reversed to preserve
     * the existing topology.
     *
     * Otherwise the user's connection order is preserved.
     */
    private fun normalizeConnectionDirection(
        from: SldNode,
        to: SldNode
    ): Pair<SldNode, SldNode> {

        if (
            isReachable(
                startId = to.id,
                targetId = from.id
            )
        ) {
            return to to from
        }

        return from to to
    }

    /**
     * Directed graph reachability.
     *
     * Used only to preserve an already established upstream
     * direction. It does not classify components and does not
     * perform engineering calculations.
     */
    private fun isReachable(
        startId: String,
        targetId: String
    ): Boolean {

        if (
            startId ==
            targetId
        ) {
            return true
        }

        val adjacency =
            mutableMapOf<
                String,
                MutableList<String>
            >()

        state.nodes.forEach { node ->

            adjacency[node.id] =
                mutableListOf()
        }

        state.connections.forEach { connection ->

            adjacency[
                connection.fromNodeId
            ]?.add(
                connection.toNodeId
            )
        }

        val visited =
            mutableSetOf<String>()

        val queue =
            ArrayDeque<String>()

        queue.add(
            startId
        )

        while (
            queue.isNotEmpty()
        ) {

            val current =
                queue.removeFirst()

            if (
                !visited.add(
                    current
                )
            ) {
                continue
            }

            if (
                current ==
                targetId
            ) {
                return true
            }

            adjacency[
                current
            ].orEmpty().forEach { next ->

                if (
                    next !in visited
                ) {
                    queue.add(
                        next
                    )
                }
            }
        }

        return false
    }

    // ============================================================
    // AUTOMATIC CONNECTION TYPE
    // ============================================================

    private fun automaticConnectionType(
        from: SldNode,
        to: SldNode
    ): SldConnectionType {

        /*
         * Internal panel busbar:
         *
         * PANEL -> BREAKER
         *
         * This is intentionally independent of cable data.
         */
        if (
            from.type ==
            SldNodeType.PANEL &&
            to.type ==
            SldNodeType.BREAKER
        ) {

            return SldConnectionType.BUSBAR
        }

        /*
         * Breaker to load is an external feeder.
         */
        if (
            from.type ==
            SldNodeType.BREAKER &&
            to.type ==
            SldNodeType.LOAD
        ) {

            return SldConnectionType.CABLE
        }

        /*
         * All other connections are currently represented as
         * external feeders unless explicitly classified as an
         * internal panel busbar.
         */
        return SldConnectionType.CABLE
    }

    private fun normalizeConnectionType(
        connectionType: String
    ): SldConnectionType {

        return if (
            connectionType.equals(
                SldConnectionType.BUSBAR.name,
                ignoreCase = true
            )
        ) {

            SldConnectionType.BUSBAR

        } else {

            SldConnectionType.CABLE
        }
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
    // SHORT CIRCUIT
    // ============================================================

    fun runShortCircuit() {

        try {

            recalculateEngineering()

            state.reportTitle =
                if (arabic) {
                    "دراسة تيارات القصر"
                } else {
                    "SHORT CIRCUIT STUDY"
                }

            state.reportText =
                buildShortCircuitReportText()

            state.showReport = true

        } catch (e: Exception) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "فشل حساب تيار القصر."
                    } else {
                        "Short circuit calculation failed."
                    }
        }
    }

    private fun buildShortCircuitReportText(): String {

        val builder =
            StringBuilder()

        builder.appendLine(
            if (arabic) {
                "دراسة تيارات القصر"
            } else {
                "SHORT CIRCUIT STUDY"
            }
        )

        builder.appendLine(
            "--------------------------------"
        )

        state.nodes.forEach { node ->

            builder.appendLine(
                "${node.name} | " +
                    "${node.voltage.toEngineeringString()} V"
            )

            if (
                node.sourceShortCircuitMva > 0.0
            ) {

                builder.appendLine(
                    "Fault Level = " +
                        "${node.sourceShortCircuitMva.toEngineeringString()} MVA"
                )
            }

            if (
                node.transformerPercentZ > 0.0
            ) {

                builder.appendLine(
                    "Transformer Z = " +
                        "${node.transformerPercentZ.toEngineeringString()} %"
                )
            }

            if (
                node.generatorXdSubtransient > 0.0
            ) {

                builder.appendLine(
                    "Generator Xd'' = " +
                        "${node.generatorXdSubtransient.toEngineeringString()} %"
                )
            }
        }

        return builder.toString()
    }

    // ============================================================
    // PANEL SCHEDULE
    // ============================================================

    fun runPanelSchedule() {

        try {

            state.reportTitle =
                if (arabic) {
                    "جدول اللوحات"
                } else {
                    "PANEL SCHEDULE"
                }

            state.reportText =
                buildPanelSchedule()

            state.showReport = true

        } catch (e: Exception) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "فشل إنشاء جدول اللوحات."
                    } else {
                        "Panel schedule failed."
                    }
        }
    }

    private fun buildPanelSchedule(): String {

        val builder =
            StringBuilder()

        builder.appendLine(
            if (arabic) {
                "جدول اللوحات"
            } else {
                "PANEL SCHEDULE"
            }
        )

        builder.appendLine(
            "--------------------------------"
        )

        state.nodes
            .filter {
                it.type ==
                    SldNodeType.PANEL
            }
            .forEach { panel ->

                builder.appendLine(
                    "Panel: ${panel.name}"
                )

                builder.appendLine(
                    "Voltage: " +
                        "${panel.voltage.toEngineeringString()} V"
                )

                builder.appendLine(
                    "Rating: " +
                        "${panel.ratedKva.toEngineeringString()} kVA"
                )

                state.connections
                    .filter {
                        it.fromNodeId ==
                            panel.id
                    }
                    .forEach { feeder ->

                        val load =
                            state.nodes.firstOrNull {
                                it.id ==
                                    feeder.toNodeId
                            }

                        if (load != null) {

                            val type =
                                feeder.connectionType.name

                            builder.appendLine(
                                "  -> ${load.name} | " +
                                    "${load.loadKw.toEngineeringString()} kW | " +
                                    type
                            )
                        }
                    }

                builder.appendLine()
            }

        return builder.toString()
    }

    // ============================================================
    // COMPLETE ENGINEERING REPORT
    // ============================================================

    fun generateCompleteSld() {

        try {

            recalculateEngineering()

            state.reportTitle =
                if (arabic) {
                    "الدراسة الهندسية الكاملة"
                } else {
                    "COMPLETE ENGINEERING STUDY"
                }

            val engineering =
                state.engineeringPackage
                    ?: error(
                        "Engineering study is not available."
                    )

            state.reportText =
                SldEngineeringReportEngine
                    .build(
                        network = network(),
                        engineering = engineering
                    )
                    .asText()

            state.showReport = true

        } catch (e: Exception) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "فشل إنشاء الدراسة الهندسية."
                    } else {
                        "Complete SLD study failed."
                    }
        }
    }

    fun runCompleteEngineeringReport() {
        generateCompleteSld()
    }

    // ============================================================
    // IDS
    // ============================================================

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

    // ============================================================
    // NUMBER FORMATTING
    // ============================================================

    private fun Double.toEngineeringString(): String =
        String.format(
            Locale.US,
            "%.2f",
            this
        )

    // ============================================================
    // CONSTANTS
    // ============================================================

    private companion object {

        const val NODE_WIDTH =
            180f

        const val NODE_HEIGHT =
            118f

        const val NODE_CLEARANCE =
            70f

        const val MIN_POSITION =
            40f

        const val DEFAULT_BUSBAR_CURRENT_A =
            400.0

        const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA =
            25.0
    }
}
