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
     * SAFE SAVE
     * ============================================================
     *
     * IMPORTANT:
     *
     * The drawing is the primary editor state.
     *
     * Saving / persistence / engineering calculation must never
     * be allowed to destroy the editor when an incomplete LOAD,
     * BREAKER or other component is being added.
     *
     * onSave is therefore isolated from engineering calculation.
     */

    fun saveAndRecalculate() {

        /*
         * First try engineering calculation.
         *
         * recalculateEngineering() already contains its own
         * protection and never throws intentionally.
         */
        runCatching {
            recalculateEngineering()
        }.onFailure { error ->

            state.engineeringPackage = null

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

        /*
         * IMPORTANT:
         *
         * Persistence must NEVER crash the SLD editor.
         *
         * This is especially important immediately after adding
         * a LOAD or BREAKER because the drawing is intentionally
         * allowed to be incomplete.
         */
        try {

            onSave?.invoke(
                network()
            )

        } catch (error: Throwable) {

            /*
             * Do NOT remove the node.
             * Do NOT clear the drawing.
             * Do NOT navigate.
             * Do NOT rethrow.
             */
            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "تعذر حفظ التغييرات، لكن العنصر ما زال موجودًا في المخطط."
                    } else {
                        "The changes could not be persisted, but the component remains in the SLD."
                    }
        }
    }

    /*
     * ============================================================
     * ENGINEERING
     * ============================================================
     */

    fun recalculateEngineering() {

        /*
         * Take a snapshot before entering the engineering layer.
         *
         * The snapshot is never modified by the calculation layer.
         */
        val currentNetwork =
            try {
                network()
            } catch (error: Throwable) {

                state.engineeringPackage = null

                state.engineeringError =
                    error.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: if (arabic) {
                            "تعذر قراءة بيانات المخطط."
                        } else {
                            "Unable to read the SLD data."
                        }

                return
            }

        try {

            /*
             * ====================================================
             * AUTHORITATIVE TOPOLOGY GATE
             * ====================================================
             *
             * The editor may contain:
             *
             * - disconnected LOAD
             * - disconnected BREAKER
             * - incomplete feeder
             * - temporary drawing elements
             *
             * These are valid EDITOR states.
             *
             * They are NOT valid ENGINEERING states.
             */
            val topologyResult =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            /*
             * ====================================================
             * INVALID / INCOMPLETE DRAWING
             * ====================================================
             *
             * This is NOT an application error.
             *
             * Keep everything visible and editable.
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
             * VALID TOPOLOGY
             * ====================================================
             *
             * Only here is the strict engineering calculation
             * allowed.
             */
            val result =
                SldEngineeringFacade.calculateComplete(
                    network = currentNetwork
                )

            state.engineeringPackage =
                result

            state.engineeringError =
                null

        } catch (error: Throwable) {

            /*
             * Engineering failure must NEVER propagate into
             * Compose / Android.
             *
             * Most importantly:
             *
             * LOAD remains in state.nodes.
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
        saveAndRecalculate()
    }

    /*
     * ============================================================
     * COMPLETE SLD GENERATOR
     * ============================================================
     */

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

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
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

        /*
         * The operation itself is protected.
         *
         * No engineering calculation is allowed to prevent
         * creation of the drawing object.
         */
        try {

            state.nodeType =
                type

            val selectedPanel =
                if (
                    type ==
                    SldNodeType.BREAKER
                ) {
                    findPanelForNewBreaker()
                } else {
                    null
                }

            val position =
                if (
                    type ==
                    SldNodeType.BREAKER &&
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
                createDefaultNode(
                    type = type,
                    position = position
                )

            /*
             * ====================================================
             * CRITICAL ORDER
             * ====================================================
             *
             * Add to drawing FIRST.
             *
             * Engineering comes AFTER.
             */
            state.nodes =
                state.nodes + node

            /*
             * BUSBAR is optional.
             *
             * A BREAKER without PANEL remains a valid editor object.
             */
            if (
                type ==
                SldNodeType.BREAKER &&
                selectedPanel != null
            ) {

                runCatching {

                    createBusbarConnection(
                        from = selectedPanel,
                        to = node
                    )

                }.onFailure {
                    /*
                     * BUSBAR failure must not remove BREAKER.
                     */
                }
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
             * This can only affect engineeringPackage /
             * engineeringError. It cannot remove the node.
             */
            saveAndRecalculate()

        } catch (error: Throwable) {

            /*
             * Last-resort protection.
             *
             * If construction/positioning fails before insertion,
             * show the error instead of crashing Android.
             */
            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "تعذر إضافة العنصر إلى المخطط."
                    } else {
                        "Unable to add the component to the SLD."
                    }
        }
    }

    private fun createDefaultNode(
        type: SldNodeType,
        position: Pair<Float, Float>
    ): SldNode {

        return SldNode(
            id =
                createNodeId(),

            name =
                defaultNodeName(
                    type
                ),

            type =
                type,

            x =
                position.first,

            y =
                position.second,

            voltage =
                400.0,

            loadKw =
                if (
                    type ==
                    SldNodeType.LOAD
                ) {
                    100.0
                } else {
                    0.0
                },

            powerFactor =
                0.90,

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

        var index =
            1

        while (
            state.nodes.any {
                it.name.equals(
                    "$prefix-$index",
                    ignoreCase = true
                )
            }
        ) {

            index++

            /*
             * Absolute safety against a pathological state.
             */
            if (index > 100000) {
                break
            }
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

        state.editingNodeId =
            null

        state.nodeType =
            type

        state.name =
            ""

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
            node.generatorXdSubtransient.toEngineeringString()

        state.sourceMva =
            node.sourceShortCircuitMva.toEngineeringString()

        state.showNodeDialog =
            true
    }

    /*
     * ============================================================
     * SAVE NODE
     * ============================================================
     */

    fun saveNode() {

        /*
         * Entire editor operation is protected.
         *
         * This is the critical boundary for the LOAD dialog.
         */
        try {

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
                        it.isFinite() &&
                            it > 0.0
                    }
                    ?: existing?.voltage
                    ?: 400.0

            val loadKw =
                state.loadKw
                    .toDoubleOrNull()
                    ?.takeIf {
                        it.isFinite()
                    }
                    ?.coerceAtLeast(0.0)
                    ?: existing?.loadKw
                    ?: 0.0

            val pf =
                state.pf
                    .toDoubleOrNull()
                    ?.takeIf {
                        it.isFinite()
                    }
                    ?.coerceIn(
                        0.50,
                        1.00
                    )
                    ?: existing?.powerFactor
                    ?: 0.90

            val demand =
                state.demand
                    .toDoubleOrNull()
                    ?.takeIf {
                        it.isFinite()
                    }
                    ?.coerceIn(
                        0.0,
                        1.0
                    )
                    ?: existing?.demandFactor
                    ?: 1.0

            val kva =
                state.kva
                    .toDoubleOrNull()
                    ?.takeIf {
                        it.isFinite()
                    }
                    ?.coerceAtLeast(0.0)
                    ?: existing?.ratedKva
                    ?: 0.0

            val transformerZ =
                state.transformerZ
                    .toDoubleOrNull()
                    ?.takeIf {
                        it.isFinite()
                    }
                    ?.coerceAtLeast(0.0)
                    ?: existing?.transformerPercentZ
                    ?: 0.0

            val generatorXd =
                state.generatorXd
                    .toDoubleOrNull()
                    ?.takeIf {
                        it.isFinite()
                    }
                    ?.coerceAtLeast(0.0)
                    ?: existing?.generatorXdSubtransient
                    ?: 0.0

            val sourceMva =
                state.sourceMva
                    .toDoubleOrNull()
                    ?.takeIf {
                        it.isFinite()
                    }
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

            /*
             * ====================================================
             * NEW NODE
             * ====================================================
             */

            if (existing == null) {

                val type =
                    state.nodeType

                val panel =
                    if (
                        type ==
                        SldNodeType.BREAKER
                    ) {
                        findPanelForNewBreaker()
                    } else {
                        null
                    }

                val position =
                    if (
                        type ==
                        SldNodeType.BREAKER &&
                        panel != null
                    ) {
                        findBreakerPosition(
                            panel
                        )
                    } else {
                        findFreeNodePosition(
                            type
                        )
                    }

                val node =
                    SldNode(
                        id =
                            createNodeId(),

                        name =
                            cleanName,

                        type =
                            type,

                        x =
                            position.first,

                        y =
                            position.second,

                        voltage =
                            voltage,

                        loadKw =
                            if (
                                type ==
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
                 * CRITICAL:
                 *
                 * Insert before any calculation.
                 */
                state.nodes =
                    state.nodes + node

                /*
                 * A BREAKER gets an internal BUSBAR only if a PANEL
                 * exists. Failure to create it must never remove
                 * the BREAKER.
                 */
                if (
                    type ==
                    SldNodeType.BREAKER &&
                    panel != null
                ) {

                    runCatching {

                        createBusbarConnection(
                            from = panel,
                            to = node
                        )

                    }
                }

                state.selectedNodeId =
                    node.id

            } else {

                /*
                 * =================================================
                 * EXISTING NODE
                 * =================================================
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
                        node ->

                        if (
                            node.id ==
                            existing.id
                        ) {
                            updated
                        } else {
                            node
                        }
                    }

                state.selectedNodeId =
                    updated.id
            }

            /*
             * ====================================================
             * CLOSE EDITOR ONLY AFTER NODE IS SAFELY STORED
             * ====================================================
             */

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
             * Engineering is now secondary.
             *
             * It cannot remove the newly inserted LOAD.
             */
            saveAndRecalculate()

        } catch (error: Throwable) {

            /*
             * CRITICAL:
             *
             * Do NOT throw.
             *
             * The node is retained if it was already inserted.
             */
            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "تعذر حفظ العنصر. تم الاحتفاظ بالمخطط كما هو."
                    } else {
                        "Unable to save the component. The SLD has been kept intact."
                    }
        }
    }

    /*
     * ============================================================
     * PANEL / BREAKER POSITIONING
     * ============================================================
     */

    private fun findPanelForNewBreaker():
        SldNode? {

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
        ): Float {

            if (!value.isFinite()) {
                return 0f
            }

            return (
                value / GRID
            ).toInt() * GRID
        }

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

        val safeDx =
            if (deltaX.isFinite()) {
                deltaX
            } else {
                0f
            }

        val safeDy =
            if (deltaY.isFinite()) {
                deltaY
            } else {
                0f
            }

        val newX =
            maxOf(
                MIN_X,
                node.x + safeDx
            )

        val newY =
            maxOf(
                MIN_Y,
                node.y + safeDy
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

        try {

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

        } catch (error: Throwable) {

            state.connectionStartId =
                null

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "تعذر إنشاء الاتصال."
                    } else {
                        "Unable to create the connection."
                    }
        }
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

        if (
            from.type !=
                SldNodeType.PANEL ||
            to.type !=
                SldNodeType.BREAKER
        ) {

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

        try {

            val editingId =
                state.editingConnectionId

            val existing =
                editingId?.let { id ->
                    state.connections.firstOrNull {
                        it.id == id
                    }
                }

            val type =
                runCatching {
                    SldConnectionType.valueOf(
                        state.connectionType.uppercase(
                            Locale.US
                        )
                    )
                }.getOrDefault(
                    SldConnectionType.CABLE
                )

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
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: existing.busbarRatedCurrentA,

                            busbarShortCircuitKA =
                                state.busbarShortCircuit
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: existing.busbarShortCircuitKA
                        )

                    } else {

                        existing.copy(

                            connectionType =
                                SldConnectionType.CABLE,

                            lengthMeters =
                                state.length
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            resistanceOhmPerKm =
                                state.resistance
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            reactanceOhmPerKm =
                                state.reactance
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            cableSizeMm2 =
                                state.cableSize
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            parallelRuns =
                                state.parallelRuns
                                    .toIntOrNull()
                                    ?.coerceAtLeast(1)
                                    ?: 1,

                            currentCapacityA =
                                state.capacity
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
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
                        connection ->

                        if (
                            connection.id ==
                            existing.id
                        ) {
                            updated
                        } else {
                            connection
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
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            resistanceOhmPerKm =
                                state.resistance
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            reactanceOhmPerKm =
                                state.reactance
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            cableSizeMm2 =
                                state.cableSize
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,

                            parallelRuns =
                                state.parallelRuns
                                    .toIntOrNull()
                                    ?.coerceAtLeast(1)
                                    ?: 1,

                            voltageDropPercent =
                                0.0,

                            currentCapacityA =
                                state.capacity
                                    .toDoubleOrNull()
                                    ?.takeIf {
                                        it.isFinite()
                                    }
                                    ?.coerceAtLeast(0.0)
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

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "تعذر حفظ الاتصال. المخطط ما زال محفوظًا وقابلًا للتعديل."
                    } else {
                        "Unable to save the connection. The SLD remains intact and editable."
                    }
        }
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

        try {

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
                        it.fromNodeId != nodeId &&
                            it.toNodeId != nodeId
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

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?: if (arabic) {
                        "تعذر حذف العنصر."
                    } else {
                        "Unable to delete the component."
                    }
        }
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

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
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
                SldEngineeringFacade.calculateShortCircuit(
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

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
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

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
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
            panel.ratedKva.isFinite() &&
            panel.voltage.isFinite() &&
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
                        connection.busbarMaterial
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

        /*
         * nanoTime is normally unique, but make the loop finite
         * and deterministic enough for the editor.
         */
        var id =
            "node-${System.nanoTime()}"

        var counter =
            0

        while (
            state.nodes.any {
                it.id == id
            } &&
            counter < 1000
        ) {

            counter++

            id =
                "node-${System.nanoTime()}-$counter"
        }

        return id
    }

    private fun createConnectionId():
        String {

        var id =
            "connection-${System.nanoTime()}"

        var counter =
            0

        while (
            state.connections.any {
                it.id == id
            } &&
            counter < 1000
        ) {

            counter++

            id =
                "connection-${System.nanoTime()}-$counter"
        }

        return id
    }

    private fun Double.toEngineeringString():
        String =
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
