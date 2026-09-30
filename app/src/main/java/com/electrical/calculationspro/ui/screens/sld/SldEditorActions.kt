package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldAutoLayoutEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldEngineeringFacade
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
        const val NODE_GAP_Y = 70f

        const val DEFAULT_X = 700f
        const val DEFAULT_Y = 160f

        const val POSITION_STEP_X = 220f
        const val POSITION_STEP_Y = 180f

        const val BREAKER_Y_GAP = 230f
        const val BREAKER_SLOT = 150f

        const val DEFAULT_BUSBAR_CURRENT_A = 400.0
        const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA = 25.0
    }

    private fun network(): SldNetwork =
        SldNetwork(
            nodes = state.nodes,
            connections = state.connections
        )

    /*
     * ============================================================
     * SAVE / ENGINEERING
     * ============================================================
     */

    fun saveAndRecalculate() {
        recalculateEngineering()
        onSave?.invoke(network())
    }

    /**
     * The SLD editor is allowed to contain an incomplete topology.
     *
     * Engineering calculation must therefore NEVER be executed
     * against an invalid/incomplete drawing.
     *
     * Invalid/incomplete design:
     * - remains visible
     * - remains editable
     * - remains saved
     * - receives a validation message
     *
     * Valid design:
     * - full engineering package is calculated
     */
    fun recalculateEngineering() {

        val currentNetwork =
            network()

        try {

            /*
             * ====================================================
             * DRAWING STATE != ENGINEERING STATE
             * ====================================================
             *
             * The drawing may temporarily contain disconnected
             * or incomplete elements.
             *
             * SldDesignValidator is diagnostic validation only.
             *
             * SldTopologyEngine is the authoritative gate for
             * deciding whether engineering calculations are allowed.
             */
            val topologyResult =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            /*
             * ====================================================
             * INCOMPLETE DRAWING
             * ====================================================
             *
             * This is a NORMAL editor state.
             *
             * Never:
             * - delete nodes
             * - delete connections
             * - clear the drawing
             * - navigate away
             * - call calculateComplete()
             */
            if (!topologyResult.valid) {

                state.engineeringPackage =
                    null

                state.engineeringError =
                    topologyResult.errorMessage
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: if (arabic) {
                            "المخطط غير مكتمل أو غير متصل بالمصدر. أكمل التوصيلات ثم أعد الحساب."
                        } else {
                            "The SLD is incomplete or not connected to the source. Complete the connections and recalculate."
                        }

                return
            }

            /*
             * ====================================================
             * VALID ENGINEERING TOPOLOGY
             * ====================================================
             *
             * Only after the topology engine accepts the graph
             * may the complete engineering study run.
             *
             * calculateComplete() also has its own hard topology
             * gate as a second safety barrier.
             */
            val result =
                SldEngineeringFacade.calculateComplete(
                    network = currentNetwork
                )

            state.engineeringPackage =
                result

            state.engineeringError =
                null

        } catch (
            error: Throwable
        ) {

            /*
             * ====================================================
             * ENGINEERING FAILURE
             * ====================================================
             *
             * Calculation failure is an editor error.
             *
             * It must NEVER:
             * - crash the application
             * - remove the new LOAD
             * - remove the BREAKER
             * - remove connections
             * - navigate away from SLD
             *
             * The drawing remains intact and editable.
             */
            state.engineeringPackage =
                null

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "تعذر تحديث الدراسة الهندسية. المخطط ما زال محفوظًا وقابلًا للتعديل."
                    } else {
                        "Engineering study could not be updated. The drawing remains intact and editable."
                    }
        }
    }

    fun loadProjectNetwork() {
        recalculateEngineering()
    }

    fun generateCompleteSld() {

        try {

            val generated =
                SldCompleteGenerator.generate()

            state.nodes =
                generated.nodes

            state.connections =
                sanitizeConnections(
                    generated.connections
                )

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

    /*
     * ============================================================
     * NODE CREATION
     * ============================================================
     */

    fun addNode(
        type: SldNodeType
    ) {

        state.nodeType = type

        /*
         * BREAKER is always a valid drawing operation.
         *
         * With PANEL:
         *
         *     PANEL
         *       |
         *     BUSBAR
         *       |
         *     BREAKER
         *
         * Without PANEL:
         *
         *     BREAKER
         *
         * The second case is intentionally allowed because the
         * editor and engineering-study phases are separated.
         */
        val selectedPanel =
            if (
                type == SldNodeType.BREAKER
            ) {
                findPanelForNewBreaker()
            } else {
                null
            }

        val position =
            if (
                type == SldNodeType.BREAKER &&
                selectedPanel != null
            ) {
                findBreakerPosition(
                    selectedPanel
                )
            } else {
                findFreeNodePosition(
                    type
                )
            }

        val node =
            SldNode(
                id = createNodeId(),

                name =
                    defaultNodeName(
                        type
                    ),

                type = type,

                x = position.first,
                y = position.second,

                voltage = 400.0,

                loadKw =
                    if (
                        type ==
                        SldNodeType.LOAD
                    ) {
                        100.0
                    } else {
                        0.0
                    },

                powerFactor = 0.90,

                demandFactor =
                    if (
                        type ==
                        SldNodeType.LOAD
                    ) {
                        0.80
                    } else {
                        1.0
                    },

                ratedKva =
                    when (type) {

                        SldNodeType.TRANSFORMER ->
                            500.0

                        SldNodeType.GENERATOR ->
                            500.0

                        SldNodeType.PANEL ->
                            500.0

                        else ->
                            0.0
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

        /*
         * Insert the node BEFORE attempting any engineering
         * calculation.
         */
        state.nodes =
            state.nodes + node

        /*
         * Internal PANEL -> BREAKER BUSBAR is created only when
         * a PANEL actually exists.
         */
        if (
            type ==
            SldNodeType.BREAKER &&
            selectedPanel != null
        ) {

            createBusbarConnection(
                from = selectedPanel,
                to = node
            )
        }

        state.selectedNodeId =
            node.id

        state.selectedConnectionId =
            null

        state.connectionStartId =
            null

        state.connections =
            sanitizeConnections(
                state.connections
            )

        /*
         * Recalculate safely.
         *
         * If the new drawing is incomplete, the topology gate
         * returns without entering the engineering engine.
         *
         * The node remains in the editor.
         */
        saveAndRecalculate()
    }

    private fun defaultNodeName(
        type: SldNodeType
    ): String {

        val prefix =
            when (type) {

                SldNodeType.SOURCE ->
                    "UTILITY"

                SldNodeType.TRANSFORMER ->
                    "TR"

                SldNodeType.GENERATOR ->
                    "GEN"

                SldNodeType.BUS ->
                    "BUS"

                SldNodeType.PANEL ->
                    "MDB"

                SldNodeType.BREAKER ->
                    "CB"

                SldNodeType.LOAD ->
                    "LOAD"
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
     * ============================================================
     * NODE EDITOR
     * ============================================================
     */

    fun resetNodeEditor(
        type: SldNodeType
    ) {

        state.editingNodeId = null

        state.nodeType =
            type

        state.name = ""

        state.voltage =
            "400"

        state.loadKw =
            if (
                type ==
                SldNodeType.LOAD
            ) {
                "100"
            } else {
                "0"
            }

        state.pf =
            "0.90"

        state.demand =
            if (
                type ==
                SldNodeType.LOAD
            ) {
                "0.80"
            } else {
                "1.00"
            }

        state.kva =
            when (type) {

                SldNodeType.TRANSFORMER,
                SldNodeType.GENERATOR,
                SldNodeType.PANEL ->
                    "500"

                else ->
                    "0"
            }

        state.transformerZ =
            if (
                type ==
                SldNodeType.TRANSFORMER
            ) {
                "6"
            } else {
                "0"
            }

        state.generatorXd =
            if (
                type ==
                SldNodeType.GENERATOR
            ) {
                "15"
            } else {
                "0"
            }

        state.sourceMva =
            if (
                type ==
                SldNodeType.SOURCE
            ) {
                "500"
            } else {
                "0"
            }

        state.showNodeDialog =
            true
    }

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
            node.voltage
                .toEngineeringString()

        state.loadKw =
            node.loadKw
                .toEngineeringString()

        state.pf =
            node.powerFactor
                .toEngineeringString()

        state.demand =
            node.demandFactor
                .toEngineeringString()

        state.kva =
            node.ratedKva
                .toEngineeringString()

        state.transformerZ =
            node.transformerPercentZ
                .toEngineeringString()

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

        val editingId =
            state.editingNodeId

        val existing =
            editingId?.let { id ->
                state.nodes.firstOrNull {
                    it.id == id
                }
            }

        /*
         * IMPORTANT:
         *
         * A BREAKER is allowed to exist before a PANEL.
         *
         * Never reject a drawing element because the engineering
         * topology is incomplete.
         */

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
                ?.coerceAtLeast(
                    0.0
                )
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
                ?.coerceAtLeast(
                    0.0
                )
                ?: existing?.ratedKva
                ?: 0.0

        val transformerZ =
            state.transformerZ
                .toDoubleOrNull()
                ?.coerceAtLeast(
                    0.0
                )
                ?: existing?.transformerPercentZ
                ?: 0.0

        val generatorXd =
            state.generatorXd
                .toDoubleOrNull()
                ?.coerceAtLeast(
                    0.0
                )
                ?: existing?.generatorXdSubtransient
                ?: 0.0

        val sourceMva =
            state.sourceMva
                .toDoubleOrNull()
                ?.coerceAtLeast(
                    0.0
                )
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

        /*
         * ========================================================
         * NEW NODE
         * ========================================================
         */

        if (
            existing == null
        ) {

            val panel =
                if (
                    state.nodeType ==
                    SldNodeType.BREAKER
                ) {
                    findPanelForNewBreaker()
                } else {
                    null
                }

            val position =
                if (
                    state.nodeType ==
                    SldNodeType.BREAKER &&
                    panel != null
                ) {

                    findBreakerPosition(
                        panel
                    )

                } else {

                    findFreeNodePosition(
                        state.nodeType
                    )
                }

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

            /*
             * Always insert.
             */
            state.nodes =
                state.nodes + node

            /*
             * Internal BUSBAR only when PANEL exists.
             */
            if (
                node.type ==
                SldNodeType.BREAKER &&
                panel != null
            ) {

                createBusbarConnection(
                    panel,
                    node
                )
            }

            state.selectedNodeId =
                node.id

        } else {

            /*
             * ====================================================
             * EXISTING NODE
             * ====================================================
             */

            val updated =
                existing.copy(

                    name =
                        cleanName,

                    type =
                        state.nodeType,

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

            state.selectedNodeId =
                updated.id
        }

        state.showNodeDialog =
            false

        state.editingNodeId =
            null

        state.connectionStartId =
            null

        state.connections =
            sanitizeConnections(
                state.connections
            )

        /*
         * Saving a LOAD must not navigate away from SLD.
         * The engineering layer is entered only after topology
         * validation succeeds.
         */
        saveAndRecalculate()
    }

    /*
     * ============================================================
     * PANEL / BREAKER POSITIONING
     * ============================================================
     */

    private fun findPanelForNewBreaker():
        SldNode? {

        /*
         * Selected PANEL has priority.
         */
        val selected =
            state.selectedNodeId?.let { id ->

                state.nodes.firstOrNull {
                    it.id == id &&
                        it.type ==
                        SldNodeType.PANEL
                }
            }

        if (
            selected != null
        ) {
            return selected
        }

        /*
         * Otherwise use first PANEL.
         */
        return state.nodes.firstOrNull {
            it.type ==
                SldNodeType.PANEL
        }
    }

    private fun selectedPanelForNewBreaker():
        SldNode? =
        findPanelForNewBreaker()

    private fun findBreakerPosition(
        panel: SldNode
    ): Pair<Float, Float> {

        val breakers =
            state.connections
                .filter {
                    it.connectionType ==
                        SldConnectionType.BUSBAR &&
                        it.fromNodeId ==
                            panel.id
                }
                .mapNotNull {
                    connection ->

                    state.nodes.firstOrNull {
                        it.id ==
                            connection.toNodeId &&
                            it.type ==
                            SldNodeType.BREAKER
                    }
                }

        val usedXs =
            breakers.map {
                it.x +
                    NODE_WIDTH / 2f
            }

        val panelCenter =
            panel.x +
                NODE_WIDTH / 2f

        val baseY =
            panel.y +
                BREAKER_Y_GAP

        val candidates =
            buildList {

                add(
                    panelCenter
                )

                for (
                    i in 1..40
                ) {

                    val offset =
                        i *
                            BREAKER_SLOT

                    add(
                        panelCenter -
                            offset
                    )

                    add(
                        panelCenter +
                            offset
                    )
                }
            }

        for (
            centerX in candidates
        ) {

            if (
                usedXs.any {
                    abs(
                        it - centerX
                    ) <
                        BREAKER_SLOT *
                        0.70f
                }
            ) {
                continue
            }

            val x =
                centerX -
                    NODE_WIDTH / 2f

            val candidate =
                snapToGrid(
                    x,
                    baseY
                )

            if (
                isFreePosition(
                    candidate.first,
                    candidate.second
                )
            ) {
                return candidate
            }
        }

        return findFreeNodePosition(
            SldNodeType.BREAKER
        )
    }

    private fun findFreeNodePosition(
        type: SldNodeType
    ): Pair<Float, Float> {

        if (
            state.nodes.isEmpty()
        ) {
            return DEFAULT_X to DEFAULT_Y
        }

        val candidates =
            mutableListOf<Pair<Float, Float>>()

        /*
         * BREAKER without PANEL must still have a valid free
         * drawing position.
         */
        if (
            type ==
            SldNodeType.BREAKER
        ) {

            state.nodes
                .filter {
                    it.type ==
                        SldNodeType.PANEL
                }
                .forEach { panel ->

                    val center =
                        panel.x +
                            NODE_WIDTH / 2f

                    val y =
                        panel.y +
                            BREAKER_Y_GAP

                    for (
                        i in 0..20
                    ) {

                        val sign =
                            if (
                                i % 2 == 0
                            ) {
                                1f
                            } else {
                                -1f
                            }

                        val index =
                            (i + 1) / 2

                        candidates +=
                            snapToGrid(

                                center +
                                    sign *
                                    index *
                                    BREAKER_SLOT -
                                    NODE_WIDTH / 2f,

                                y
                            )
                    }
                }
        }

        /*
         * General grid.
         */
        for (
            row in 0..20
        ) {

            for (
                column in 0..20
            ) {

                candidates +=
                    snapToGrid(

                        DEFAULT_X +
                            column *
                            POSITION_STEP_X,

                        DEFAULT_Y +
                            row *
                            POSITION_STEP_Y
                    )
            }
        }

        /*
         * Positions around existing nodes.
         */
        state.nodes.forEach { node ->

            candidates +=
                snapToGrid(
                    node.x +
                        POSITION_STEP_X,
                    node.y
                )

            candidates +=
                snapToGrid(
                    node.x -
                        POSITION_STEP_X,
                    node.y
                )

            candidates +=
                snapToGrid(
                    node.x,
                    node.y +
                        POSITION_STEP_Y
                )
        }

        return candidates
            .asSequence()
            .filter {
                isFreePosition(
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

        return state.nodes.none {
            existing ->

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

        var x =
            DEFAULT_X

        var y =
            DEFAULT_Y

        repeat(1000) {

            if (
                isFreePosition(
                    x,
                    y
                )
            ) {

                return snapToGrid(
                    x,
                    y
                )
            }

            x +=
                POSITION_STEP_X

            if (
                x > 5000f
            ) {

                x =
                    DEFAULT_X

                y +=
                    POSITION_STEP_Y
            }
        }

        return snapToGrid(
            x,
            y
        )
    }

    private fun snapToGrid(
        x: Float,
        y: Float
    ): Pair<Float, Float> {

        fun snap(
            value: Float
        ): Float =
            (
                value / GRID
                ).toInt() * GRID

        return maxOf(
            MIN_X,
            snap(x)
        ) to maxOf(
            MIN_Y,
            snap(y)
        )
    }

    /*
     * ============================================================
     * NODE MOVEMENT
     * ============================================================
     */

    fun moveNode(
        nodeId: String,
        deltaX: Float,
        deltaY: Float
    ) {

        val node =
            state.nodes.firstOrNull {
                it.id == nodeId
            } ?: return

        val newX =
            maxOf(
                MIN_X,
                node.x + deltaX
            )

        val newY =
            maxOf(
                MIN_Y,
                node.y + deltaY
            )

        state.nodes =
            state.nodes.map {

                if (
                    it.id == nodeId
                ) {

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

    /*
     * ============================================================
     * CONNECTION CREATION
     * ============================================================
     */

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
            } ?: return

        val startId =
            state.connectionStartId

        if (
            startId == null
        ) {

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
            startId == target.id
        ) {

            state.connectionStartId =
                null

            return
        }

        val from =
            state.nodes.firstOrNull {
                it.id == startId
            }

        if (
            from == null
        ) {

            state.connectionStartId =
                null

            return
        }

        if (
            state.connections.any {
                it.fromNodeId ==
                    from.id &&
                    it.toNodeId ==
                    target.id
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
                target
            )

        state.selectedNodeId =
            target.id

        if (
            type ==
            SldConnectionType.BUSBAR
        ) {

            createBusbarConnection(
                from,
                target
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

    /*
     * ============================================================
     * BUSBAR
     * ============================================================
     */

    private fun createBusbarConnection(
        from: SldNode,
        to: SldNode
    ) {

        /*
         * BUSBAR is strictly internal to PANEL.
         */
        if (
            from.type !=
                SldNodeType.PANEL ||
            to.type !=
                SldNodeType.BREAKER
        ) {

            state.engineeringError =
                if (arabic) {
                    "BUSBAR مسموح فقط بين اللوحة والقاطع."
                } else {
                    "BUSBAR is only valid from PANEL to BREAKER."
                }

            return
        }

        val duplicate =
            state.connections.any {

                it.fromNodeId ==
                    from.id &&

                    it.toNodeId ==
                    to.id &&

                    it.connectionType ==
                    SldConnectionType.BUSBAR
            }

        if (
            duplicate
        ) {
            return
        }

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

                /*
                 * BUSBAR never uses cable length.
                 */
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
                    0.0,

                conductorMaterial =
                    "",

                insulationType =
                    "",

                installationMethodCode =
                    "",

                busbarMaterial =
                    state.busbarMaterial
                        .ifBlank {
                            "Copper"
                        },

                busbarRatedCurrentA =
                    ratedCurrent,

                busbarShortCircuitKA =
                    DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
            )

        state.connections =
            state.connections +
                connection
    }

    /*
     * ============================================================
     * CONNECTION EDIT
     * ============================================================
     */

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
            editingId?.let { id ->
                state.connections.firstOrNull {
                    it.id == id
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

            } catch (
                _: Exception
            ) {

                SldConnectionType.CABLE
            }

        if (
            existing != null
        ) {

            val updated =

                if (
                    type ==
                    SldConnectionType.BUSBAR
                ) {

                    existing.copy(

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
                            0.0,

                        conductorMaterial =
                            "",

                        insulationType =
                            "",

                        installationMethodCode =
                            "",

                        busbarMaterial =
                            state.busbarMaterial
                                .ifBlank {
                                    "Copper"
                                },

                        busbarRatedCurrentA =
                            state.busbarRatedCurrent
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: existing
                                    .busbarRatedCurrentA,

                        busbarShortCircuitKA =
                            state.busbarShortCircuit
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: existing
                                    .busbarShortCircuitKA
                    )

                } else {

                    existing.copy(

                        connectionType =
                            SldConnectionType.CABLE,

                        lengthMeters =
                            state.length
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        resistanceOhmPerKm =
                            state.resistance
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        reactanceOhmPerKm =
                            state.reactance
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        cableSizeMm2 =
                            state.cableSize
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        parallelRuns =
                            state.parallelRuns
                                .toIntOrNull()
                                ?.coerceAtLeast(
                                    1
                                )
                                ?: 1,

                        currentCapacityA =
                            state.capacity
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        conductorMaterial =
                            state.conductorMaterial,

                        insulationType =
                            state.insulationType,

                        installationMethodCode =
                            state.installationMethodCode,

                        busbarRatedCurrentA =
                            0.0,

                        busbarShortCircuitKA =
                            0.0
                    )
                }

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

            val from =
                state.connectionStartId
                    ?.let { id ->
                        state.nodes.firstOrNull {
                            it.id == id
                        }
                    }

            val to =
                state.selectedNodeId
                    ?.let { id ->
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
                        "حدد عنصر البداية والنهاية."
                    } else {
                        "Select valid connection endpoints."
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

                createBusbarConnection(
                    from,
                    to
                )

            } else {

                val connection =
                    SldConnection(

                        id =
                            createConnectionId(),

                        fromNodeId =
                            from.id,

                        toNodeId =
                            to.id,

                        connectionType =
                            SldConnectionType.CABLE,

                        lengthMeters =
                            state.length
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        resistanceOhmPerKm =
                            state.resistance
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        reactanceOhmPerKm =
                            state.reactance
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        cableSizeMm2 =
                            state.cableSize
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

                        parallelRuns =
                            state.parallelRuns
                                .toIntOrNull()
                                ?.coerceAtLeast(
                                    1
                                )
                                ?: 1,

                        voltageDropPercent =
                            0.0,

                        currentCapacityA =
                            state.capacity
                                .toDoubleOrNull()
                                ?.coerceAtLeast(
                                    0.0
                                )
                                ?: 0.0,

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
        }

        state.clearDialogs()

        state.connections =
            sanitizeConnections(
                state.connections
            )

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

    /*
     * ============================================================
     * DELETE
     * ============================================================
     */

    fun deleteSelected() {

        val nodeId =
            state.selectedNodeId

        val connectionId =
            state.selectedConnectionId

        if (
            nodeId != null
        ) {

            state.nodes =
                state.nodes.filterNot {
                    it.id == nodeId
                }

            state.connections =
                state.connections.filter {
                    it.fromNodeId !=
                        nodeId &&
                        it.toNodeId !=
                        nodeId
                }
        }

        if (
            connectionId != null
        ) {

            state.connections =
                state.connections.filterNot {
                    it.id == connectionId
                }
        }

        state.clearSelection()

        saveAndRecalculate()
    }

    /*
     * ============================================================
     * AUTO LAYOUT
     * ============================================================
     */

    fun autoLayout() {

        try {

            val result =
                SldAutoLayoutEngine.arrange(
                    network()
                )

            state.nodes =
                result.network.nodes

            state.connections =
                sanitizeConnections(
                    result.network.connections
                )

            state.clearSelection()

            saveAndRecalculate()

        } catch (
            e: Exception
        ) {

            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "فشل ترتيب المخطط."
                    } else {
                        "Auto layout failed."
                    }
        }
    }

    /*
     * ============================================================
     * STUDIES
     * ============================================================
     */

    fun runShortCircuit() {

        try {

            val currentNetwork =
                network()

            /*
             * Use the same authoritative topology gate used by
             * automatic engineering recalculation.
             */
            val topologyResult =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            if (
                !topologyResult.valid
            ) {

                state.engineeringError =
                    topologyResult.errorMessage
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: if (arabic) {
                            "المخطط غير مكتمل لحساب القصر."
                        } else {
                            "The SLD topology is not ready for short-circuit study."
                        }

                return
            }

            val study =
                SldEngineeringFacade
                    .calculateShortCircuit(
                        currentNetwork
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

        } catch (
            e: Throwable
        ) {

            state.engineeringError =
                e.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "فشل حساب القصر الكهربائي."
                    } else {
                        "Short-circuit study failed."
                    }
        }
    }

    fun runPanelSchedule() {

        val panel =
            state.nodes.firstOrNull {
                it.type ==
                    SldNodeType.PANEL
            }

        if (
            panel == null
        ) {

            state.engineeringError =
                if (arabic) {
                    "لا توجد لوحة PANEL."
                } else {
                    "No PANEL exists."
                }

            return
        }

        try {

            val currentNetwork =
                network()

            /*
             * Use the authoritative topology gate before creating
             * a panel schedule.
             */
            val topologyResult =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            if (
                !topologyResult.valid
            ) {

                state.engineeringError =
                    topologyResult.errorMessage
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: if (arabic) {
                            "المخطط غير مكتمل لإنشاء جدول اللوحة."
                        } else {
                            "The SLD topology is not ready for panel schedule."
                        }

                return
            }

            val result =
                SldEngineeringFacade.calculateComplete(
                    network =
                        currentNetwork,

                    panelNodeId =
                        panel.id
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

        } catch (
            e: Throwable
        ) {

            state.engineeringError =
                e.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "فشل إنشاء جدول اللوحة."
                    } else {
                        "Panel schedule failed."
                    }
        }
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private fun automaticConnectionType(
        from: SldNode,
        to: SldNode
    ): SldConnectionType =
        if (
            from.type ==
                SldNodeType.PANEL &&
            to.type ==
                SldNodeType.BREAKER
        ) {

            SldConnectionType.BUSBAR

        } else {

            SldConnectionType.CABLE
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

    private fun sanitizeConnections(
        connections:
            List<SldConnection>
    ): List<SldConnection> {

        return connections.map {
            connection ->

            if (
                connection.connectionType ==
                SldConnectionType.BUSBAR
            ) {

                connection.copy(

                    /*
                     * BUSBAR never carries cable data.
                     */
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
                        0.0,

                    conductorMaterial =
                        "",

                    insulationType =
                        "",

                    installationMethodCode =
                        "",

                    busbarMaterial =
                        connection
                            .busbarMaterial
                            .ifBlank {
                                "Copper"
                            }
                )

            } else {

                connection
            }
        }
    }

    private fun createNodeId():
        String {

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

    private fun createConnectionId():
        String {

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
