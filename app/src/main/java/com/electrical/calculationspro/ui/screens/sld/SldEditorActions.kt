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
import kotlin.math.max
import kotlin.math.min
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

        /*
         * These values belong to the drawing layer too, but are
         * repeated here intentionally so this class never depends
         * on drawing implementation details.
         */
        const val NODE_WIDTH = 180f
        const val NODE_HEIGHT = 118f
    }

    /*
     * ============================================================
     * NETWORK SNAPSHOT
     * ============================================================
     */

    private fun network(): SldNetwork {
        return SldNetwork(
            nodes = state.nodes.toList(),
            connections = state.connections.toList()
        )
    }

    /*
     * ============================================================
     * SAFE PERSISTENCE
     * ============================================================
     *
     * IMPORTANT:
     *
     * Saving is deliberately separated from node creation.
     *
     * A failure in persistence must NEVER:
     *
     * - remove a node
     * - close the SLD
     * - navigate Home
     * - throw into Compose
     */

    private fun safePersist() {

        val callback =
            onSave ?: return

        try {

            callback(
                SldNetwork(
                    nodes = state.nodes.toList(),
                    connections = state.connections.toList()
                )
            )

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "تعذر حفظ التغييرات، لكن المخطط الحالي لم يتم حذفه."
                    } else {
                        "The changes could not be persisted, but the current SLD was kept."
                    }
        }
    }

    /*
     * ============================================================
     * SAFE SAVE + ENGINEERING
     * ============================================================
     */

    fun saveAndRecalculate() {

        /*
         * Persistence first.
         *
         * The drawing is already authoritative at this point.
         */
        safePersist()

        /*
         * Engineering is secondary and isolated.
         */
        try {
            recalculateEngineering()
        } catch (error: Throwable) {

            state.engineeringPackage = null

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "تعذر تحديث الحسابات الهندسية، لكن المخطط ما زال موجودًا."
                    } else {
                        "Engineering calculation failed, but the drawing remains intact."
                    }
        }
    }

    /*
     * ============================================================
     * ENGINEERING
     * ============================================================
     */

    fun recalculateEngineering() {

        val currentNetwork =
            try {
                network()
            } catch (error: Throwable) {

                state.engineeringPackage = null

                state.engineeringError =
                    error.message
                        ?.takeIf { it.isNotBlank() }
                        ?: if (arabic) {
                            "تعذر قراءة بيانات المخطط."
                        } else {
                            "Unable to read the SLD data."
                        }

                return
            }

        try {

            /*
             * NEVER send an incomplete editor state to the strict
             * calculation engines.
             */
            val topology =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            if (!topology.valid) {

                state.engineeringPackage = null

                state.engineeringError =
                    topology.errorMessage
                        ?.takeIf { it.isNotBlank() }
                        ?: if (arabic) {
                            "المخطط غير مكتمل. أكمل التوصيلات ثم أعد الحساب."
                        } else {
                            "The SLD is incomplete. Complete the connections and calculate again."
                        }

                return
            }

            /*
             * Only a valid engineering topology reaches here.
             */
            val result =
                SldEngineeringFacade.calculateComplete(
                    network = currentNetwork
                )

            state.engineeringPackage = result
            state.engineeringError = null

        } catch (error: Throwable) {

            /*
             * Calculation failure is an engineering-state failure,
             * NOT an editor failure.
             */
            state.engineeringPackage = null

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "تعذر تنفيذ الدراسة الهندسية الحالية."
                    } else {
                        "The current engineering study could not be completed."
                    }
        }
    }

    fun loadProjectNetwork() {

        /*
         * Loading a project must not cause the SLD to crash if the
         * stored drawing is incomplete.
         */
        try {
            recalculateEngineering()
        } catch (error: Throwable) {

            state.engineeringPackage = null

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "تعذر تحميل الدراسة الهندسية."
                    } else {
                        "Unable to load the engineering study."
                    }
        }
    }

    /*
     * ============================================================
     * NODE CREATION
     * ============================================================
     *
     * CRITICAL DESIGN:
     *
     * addNode() is the ONLY operation required to put a component
     * into the editor.
     *
     * It does NOT calculate.
     * It does NOT validate topology.
     * It does NOT require a panel.
     * It does NOT require a connection.
     *
     * This guarantees that LOAD #2 / BREAKER #2 can always exist.
     */

    fun addNode(type: SldNodeType) {

        try {

            state.nodeType = type

            /*
             * Do not calculate anything here.
             *
             * The actual node is created immediately.
             */
            val panel =
                if (type == SldNodeType.BREAKER) {
                    findPanelForNewBreaker()
                } else {
                    null
                }

            val position =
                if (
                    type == SldNodeType.BREAKER &&
                    panel != null
                ) {
                    findBreakerPosition(panel)
                } else {
                    findFreeNodePosition(type)
                }

            val node =
                createDefaultNode(
                    type = type,
                    position = position
                )

            /*
             * ====================================================
             * ATOMIC DRAWING INSERT
             * ====================================================
             */

            state.nodes =
                state.nodes + node

            /*
             * Selection is editor state only.
             */
            state.selectedNodeId = node.id
            state.selectedConnectionId = null
            state.connectionStartId = null

            /*
             * Do NOT automatically create a BUSBAR here.
             *
             * This is intentional.
             *
             * The second BREAKER must first become a stable drawing
             * object. Connection creation is a separate operation.
             *
             * This removes the exact failure chain:
             *
             * second breaker
             *     -> automatic busbar
             *     -> topology
             *     -> engineering
             *     -> drawing recalculation
             *     -> crash
             */

            state.engineeringPackage = null

            state.engineeringError =
                if (arabic) {
                    "تمت إضافة ${node.name}. العنصر غير متصل بعد."
                } else {
                    "${node.name} was added. The component is not connected yet."
                }

            /*
             * Persist only the drawing.
             *
             * No calculation is triggered.
             */
            safePersist()

        } catch (error: Throwable) {

            /*
             * Never allow the UI event to propagate an exception.
             */
            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "تعذر إضافة العنصر إلى المخطط."
                    } else {
                        "Unable to add the component to the SLD."
                    }
        }
    }

    /*
     * ============================================================
     * DEFAULT NODE
     * ============================================================
     */

    private fun createDefaultNode(
        type: SldNodeType,
        position: Pair<Float, Float>
    ): SldNode {

        val safeX =
            if (position.first.isFinite()) {
                max(MIN_X, position.first)
            } else {
                DEFAULT_X
            }

        val safeY =
            if (position.second.isFinite()) {
                max(MIN_Y, position.second)
            } else {
                DEFAULT_Y
            }

        return SldNode(

            id =
                createNodeId(),

            name =
                defaultNodeName(type),

            type =
                type,

            x =
                safeX,

            y =
                safeY,

            voltage =
                400.0,

            loadKw =
                if (type == SldNodeType.LOAD) {
                    100.0
                } else {
                    0.0
                },

            powerFactor =
                0.90,

            demandFactor =
                if (type == SldNodeType.LOAD) {
                    0.80
                } else {
                    1.0
                },

            ratedKva =
                when (type) {

                    SldNodeType.TRANSFORMER,
                    SldNodeType.GENERATOR,
                    SldNodeType.PANEL ->
                        500.0

                    else ->
                        0.0
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

            if (index > 100000) {
                break
            }
        }

        return "$prefix-$index"
    }

    /*
     * ============================================================
     * NODE DIALOG
     * ============================================================
     */

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
                SldNodeType.PANEL ->
                    "500"

                else ->
                    "0"
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

        state.engineeringError = null

        state.showNodeDialog = true
    }

    fun editNode(node: SldNode) {

        state.editingNodeId = node.id

        state.name = node.name
        state.nodeType = node.type

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

        state.showNodeDialog = true
    }

    /*
     * ============================================================
     * SAVE NODE
     * ============================================================
     *
     * This method is deliberately different from addNode().
     *
     * addNode() creates immediately.
     *
     * saveNode() edits an existing node or creates one from the
     * dialog. Neither operation performs engineering calculation.
     */

    fun saveNode() {

        try {

            val editingId =
                state.editingNodeId

            val existing =
                editingId?.let { id ->
                    state.nodes.firstOrNull {
                        it.id == id
                    }
                }

            val type =
                state.nodeType

            val voltage =
                parsePositive(
                    state.voltage,
                    existing?.voltage ?: 400.0
                )

            val loadKw =
                parseNonNegative(
                    state.loadKw,
                    existing?.loadKw ?: 0.0
                )

            val pf =
                parseRange(
                    state.pf,
                    existing?.powerFactor ?: 0.90,
                    0.50,
                    1.00
                )

            val demand =
                parseRange(
                    state.demand,
                    existing?.demandFactor ?: 1.0,
                    0.0,
                    1.0
                )

            val kva =
                parseNonNegative(
                    state.kva,
                    existing?.ratedKva ?: 0.0
                )

            val transformerZ =
                parseNonNegative(
                    state.transformerZ,
                    existing?.transformerPercentZ ?: 0.0
                )

            val generatorXd =
                parseNonNegative(
                    state.generatorXd,
                    existing?.generatorXdSubtransient ?: 0.0
                )

            val sourceMva =
                parseNonNegative(
                    state.sourceMva,
                    existing?.sourceShortCircuitMva ?: 0.0
                )

            val name =
                state.name
                    .trim()
                    .ifBlank {
                        existing?.name
                            ?: defaultNodeName(type)
                    }

            if (existing == null) {

                /*
                 * Create directly.
                 */
                val panel =
                    if (type == SldNodeType.BREAKER) {
                        findPanelForNewBreaker()
                    } else {
                        null
                    }

                val position =
                    if (
                        type == SldNodeType.BREAKER &&
                        panel != null
                    ) {
                        findBreakerPosition(panel)
                    } else {
                        findFreeNodePosition(type)
                    }

                val node =
                    SldNode(

                        id =
                            createNodeId(),

                        name =
                            name,

                        type =
                            type,

                        x =
                            position.first,

                        y =
                            position.second,

                        voltage =
                            voltage,

                        loadKw =
                            if (type == SldNodeType.LOAD) {
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
                 * ATOMIC INSERT.
                 */
                state.nodes =
                    state.nodes + node

                state.selectedNodeId =
                    node.id

                state.selectedConnectionId =
                    null

                state.connectionStartId =
                    null

            } else {

                val updated =
                    existing.copy(

                        name =
                            name,

                        type =
                            type,

                        voltage =
                            voltage,

                        loadKw =
                            if (type == SldNodeType.LOAD) {
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
                    state.nodes.map { node ->
                        if (node.id == existing.id) {
                            updated
                        } else {
                            node
                        }
                    }

                state.selectedNodeId =
                    updated.id
            }

            /*
             * Close the editor only after the state has been
             * successfully updated.
             */
            state.showNodeDialog = false
            state.editingNodeId = null
            state.connectionStartId = null

            /*
             * IMPORTANT:
             *
             * Do not sanitize or recalculate here.
             *
             * The drawing must survive even if its topology is
             * temporarily invalid.
             */
            state.engineeringPackage = null

            state.engineeringError =
                if (arabic) {
                    "تم حفظ العنصر. أكمل التوصيلات ثم نفّذ الحساب."
                } else {
                    "Component saved. Complete the connections and run the calculation."
                }

            safePersist()

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "تعذر حفظ العنصر، وتم الاحتفاظ بالمخطط."
                    } else {
                        "Unable to save the component. The drawing was kept intact."
                    }
        }
    }

    /*
     * ============================================================
     * BREAKER POSITIONING
     * ============================================================
     */

    private fun findPanelForNewBreaker(): SldNode? {

        val selected =
            state.selectedNodeId?.let { id ->
                state.nodes.firstOrNull {
                    it.id == id &&
                        it.type == SldNodeType.PANEL
                }
            }

        if (selected != null) {
            return selected
        }

        return state.nodes.firstOrNull {
            it.type == SldNodeType.PANEL
        }
    }

    private fun findBreakerPosition(
        panel: SldNode
    ): Pair<Float, Float> {

        val existingBreakerIds =
            state.connections
                .asSequence()
                .filter {
                    it.connectionType ==
                        SldConnectionType.BUSBAR &&
                        it.fromNodeId ==
                        panel.id
                }
                .map {
                    it.toNodeId
                }
                .toSet()

        val breakers =
            state.nodes.filter {
                it.type == SldNodeType.BREAKER &&
                    it.id in existingBreakerIds
            }

        val occupiedCenters =
            breakers.map {
                it.x + NODE_WIDTH / 2f
            }

        val panelCenter =
            panel.x + NODE_WIDTH / 2f

        val y =
            max(
                MIN_Y,
                panel.y + BREAKER_Y_GAP
            )

        /*
         * Always generate many independent candidate slots.
         *
         * The second breaker therefore never receives the same
         * position as the first one.
         */
        val candidateCenters =
            buildList {

                add(panelCenter)

                for (i in 1..50) {

                    val offset =
                        i * BREAKER_SLOT

                    add(panelCenter - offset)
                    add(panelCenter + offset)
                }
            }

        for (center in candidateCenters) {

            if (
                occupiedCenters.any {
                    abs(it - center) <
                        BREAKER_SLOT * 0.75f
                }
            ) {
                continue
            }

            val candidate =
                snapToGrid(
                    center - NODE_WIDTH / 2f,
                    y
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

        return fallbackFreePosition()
    }

    /*
     * ============================================================
     * FREE POSITION
     * ============================================================
     */

    private fun findFreeNodePosition(
        type: SldNodeType
    ): Pair<Float, Float> {

        /*
         * For breakers, try slots below panels first.
         */
        if (type == SldNodeType.BREAKER) {

            val panels =
                state.nodes.filter {
                    it.type == SldNodeType.PANEL
                }

            for (panel in panels) {

                val center =
                    panel.x +
                        NODE_WIDTH / 2f

                val y =
                    panel.y +
                        BREAKER_Y_GAP

                for (i in 0..30) {

                    val direction =
                        if (i % 2 == 0) {
                            1f
                        } else {
                            -1f
                        }

                    val index =
                        (i + 1) / 2

                    val x =
                        center +
                            direction *
                            index *
                            BREAKER_SLOT -
                            NODE_WIDTH / 2f

                    val candidate =
                        snapToGrid(
                            x,
                            y
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
            }
        }

        /*
         * Generic grid search.
         */
        for (row in 0..30) {

            for (column in 0..30) {

                val candidate =
                    snapToGrid(

                        DEFAULT_X +
                            column *
                            POSITION_STEP_X,

                        DEFAULT_Y +
                            row *
                            POSITION_STEP_Y
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
        }

        return fallbackFreePosition()
    }

    private fun isFreePosition(
        x: Float,
        y: Float
    ): Boolean {

        if (
            !x.isFinite() ||
            !y.isFinite()
        ) {
            return false
        }

        return state.nodes.none { other ->

            val dx =
                abs(
                    (
                        x +
                            NODE_WIDTH / 2f
                        ) -
                        (
                            other.x +
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
                            other.y +
                                NODE_HEIGHT / 2f
                            )
                    )

            dx <
                NODE_WIDTH +
                NODE_GAP_X &&
                dy <
                NODE_HEIGHT +
                NODE_GAP_Y
        }
    }

    private fun fallbackFreePosition():
        Pair<Float, Float> {

        var x = DEFAULT_X
        var y = DEFAULT_Y

        repeat(5000) {

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

            x += POSITION_STEP_X

            if (x > 10000f) {
                x = DEFAULT_X
                y += POSITION_STEP_Y
            }
        }

        /*
         * Even in a completely pathological drawing state,
         * return a finite coordinate.
         */
        return snapToGrid(
            DEFAULT_X,
            y + POSITION_STEP_Y
        )
    }

    private fun snapToGrid(
        x: Float,
        y: Float
    ): Pair<Float, Float> {

        fun safe(value: Float): Float {

            if (!value.isFinite()) {
                return 0f
            }

            return (
                kotlin.math.round(
                    value / GRID
                ) * GRID
            )
        }

        return Pair(
            max(
                MIN_X,
                safe(x)
            ),
            max(
                MIN_Y,
                safe(y)
            )
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

        val dx =
            if (deltaX.isFinite()) {
                deltaX
            } else {
                0f
            }

        val dy =
            if (deltaY.isFinite()) {
                deltaY
            } else {
                0f
            }

        state.nodes =
            state.nodes.map {

                if (it.id == nodeId) {

                    it.copy(
                        x =
                            max(
                                MIN_X,
                                it.x + dx
                            ),
                        y =
                            max(
                                MIN_Y,
                                it.y + dy
                            )
                    )

                } else {
                    it
                }
            }
    }

    fun moveNodeEnd() {

        /*
         * Moving is an editor operation.
         *
         * Calculate only after movement has ended.
         */
        safePersist()

        try {
            recalculateEngineering()
        } catch (error: Throwable) {
            state.engineeringPackage = null
        }
    }

    /*
     * ============================================================
     * CONNECTION MODE
     * ============================================================
     */

    fun startOrCompleteConnection() {

        val selected =
            state.selectedNodeId ?: return

        startOrCompleteConnection(
            selected
        )
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

            if (startId == null) {

                state.selectedNodeId =
                    target.id

                state.selectedConnectionId =
                    null

                state.connectionStartId =
                    target.id

                return
            }

            if (startId == target.id) {

                state.connectionStartId =
                    null

                return
            }

            val from =
                state.nodes.firstOrNull {
                    it.id == startId
                }

            if (from == null) {

                state.connectionStartId =
                    null

                return
            }

            /*
             * Duplicate check without invoking topology engine.
             */
            if (
                state.connections.any {
                    it.fromNodeId == from.id &&
                        it.toNodeId == target.id
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

            if (
                from.type == SldNodeType.PANEL &&
                target.type == SldNodeType.BREAKER
            ) {

                /*
                 * BUSBAR is created directly and safely.
                 *
                 * No cable dialog.
                 */
                createBusbarConnection(
                    from,
                    target
                )

                state.connectionStartId =
                    null

                state.selectedNodeId =
                    target.id

                safePersist()

                return
            }

            /*
             * Cable connection:
             * open the dialog only.
             */
            state.editingConnectionId = null
            state.connectionType =
                SldConnectionType.CABLE.name

            state.length = "50"
            state.resistance = "0.125"
            state.reactance = "0.080"
            state.cableSize = "240"
            state.parallelRuns = "1"
            state.capacity = "350"

            state.showConnectionDialog = true

        } catch (error: Throwable) {

            state.connectionStartId = null

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
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

        /*
         * Strict editor-side rule.
         */
        if (
            from.type != SldNodeType.PANEL ||
            to.type != SldNodeType.BREAKER
        ) {
            return
        }

        /*
         * Never create a duplicate busbar.
         */
        if (
            state.connections.any {
                it.connectionType ==
                    SldConnectionType.BUSBAR &&
                    it.fromNodeId == from.id &&
                    it.toNodeId == to.id
            }
        ) {
            return
        }

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
                 * BUSBAR never has cable parameters.
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
                    estimateBusbarCurrent(
                        from
                    ),

                busbarShortCircuitKA =
                    DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
            )

        /*
         * Atomic append.
         */
        state.connections =
            state.connections + connection
    }

    /*
     * ============================================================
     * CONNECTION EDITOR
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

        state.showConnectionDialog = true
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

            if (existing != null) {

                /*
                 * Existing BUSBAR remains a BUSBAR.
                 */
                if (
                    existing.connectionType ==
                    SldConnectionType.BUSBAR
                ) {

                    state.connections =
                        state.connections.map {
                            connection ->

                            if (
                                connection.id ==
                                existing.id
                            ) {

                                connection.copy(

                                    lengthMeters = 0.0,
                                    resistanceOhmPerKm = 0.0,
                                    reactanceOhmPerKm = 0.0,
                                    cableSizeMm2 = 0.0,
                                    parallelRuns = 1,
                                    voltageDropPercent = 0.0,
                                    currentCapacityA = 0.0,
                                    conductorMaterial = "",
                                    insulationType = "",
                                    installationMethodCode = "",
                                    busbarMaterial =
                                        state.busbarMaterial
                                            .ifBlank {
                                                "Copper"
                                            }
                                )

                            } else {
                                connection
                            }
                        }

                } else {

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
                                state.conductorMaterial,

                            insulationType =
                                state.insulationType,

                            installationMethodCode =
                                state.installationMethodCode
                        )

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
                }

                state.selectedConnectionId =
                    existing.id

            } else {

                val from =
                    state.connectionStartId?.let {
                        id ->
                        state.nodes.firstOrNull {
                            it.id == id
                        }
                    }

                val to =
                    state.selectedNodeId?.let {
                        id ->
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
                            "حدد عنصري البداية والنهاية."
                        } else {
                            "Select valid start and destination components."
                        }

                    return
                }

                if (
                    from.type == SldNodeType.PANEL &&
                    to.type == SldNodeType.BREAKER
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
                                parseNonNegative(
                                    state.length,
                                    50.0
                                ),

                            resistanceOhmPerKm =
                                parseNonNegative(
                                    state.resistance,
                                    0.125
                                ),

                            reactanceOhmPerKm =
                                parseNonNegative(
                                    state.reactance,
                                    0.080
                                ),

                            cableSizeMm2 =
                                parseNonNegative(
                                    state.cableSize,
                                    240.0
                                ),

                            parallelRuns =
                                state.parallelRuns
                                    .toIntOrNull()
                                    ?.coerceAtLeast(1)
                                    ?: 1,

                            voltageDropPercent =
                                0.0,

                            currentCapacityA =
                                parseNonNegative(
                                    state.capacity,
                                    350.0
                                ),

                            conductorMaterial =
                                state.conductorMaterial,

                            insulationType =
                                state.insulationType,

                            installationMethodCode =
                                state.installationMethodCode,

                            busbarMaterial =
                                "",

                            busbarRatedCurrentA =
                                0.0,

                            busbarShortCircuitKA =
                                0.0
                        )

                    state.connections =
                        state.connections + connection

                    state.selectedConnectionId =
                        connection.id
                }
            }

            state.showConnectionDialog = false
            state.editingConnectionId = null
            state.connectionStartId = null

            /*
             * Do not force engineering immediately.
             *
             * The drawing is already valid as editor state.
             */
            safePersist()

            state.engineeringPackage = null

            try {
                recalculateEngineering()
            } catch (_: Throwable) {
                state.engineeringPackage = null
            }

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "تعذر حفظ الاتصال."
                    } else {
                        "Unable to save the connection."
                    }
        }
    }

    fun cancelConnectionEdit() {

        state.showConnectionDialog = false
        state.editingConnectionId = null
        state.connectionStartId = null
        state.selectedConnectionId = null
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

            safePersist()

            try {
                recalculateEngineering()
            } catch (_: Throwable) {
                state.engineeringPackage = null
            }

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
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
                result.network.connections

            state.clearSelection()

            safePersist()

            try {
                recalculateEngineering()
            } catch (_: Throwable) {
                state.engineeringPackage = null
            }

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "فشل ترتيب المخطط."
                    } else {
                        "Auto layout failed."
                    }
        }
    }

    /*
     * ============================================================
     * GENERATE COMPLETE SLD
     * ============================================================
     */

    fun generateCompleteSld() {

        try {

            val generated =
                SldCompleteGenerator.generate()

            state.nodes =
                generated.nodes

            state.connections =
                generated.connections

            state.clearSelection()

            safePersist()

            try {
                recalculateEngineering()
            } catch (_: Throwable) {
                state.engineeringPackage = null
            }

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "فشل إنشاء المخطط الكهربائي."
                    } else {
                        "Failed to generate the SLD."
                    }
        }
    }

    /*
     * ============================================================
     * SHORT CIRCUIT
     * ============================================================
     */

    fun runShortCircuit() {

        try {

            val currentNetwork =
                network()

            val topology =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            if (!topology.valid) {

                state.engineeringError =
                    topology.errorMessage
                        ?.takeIf { it.isNotBlank() }
                        ?: if (arabic) {
                            "المخطط غير مكتمل لحساب القصر."
                        } else {
                            "The SLD is incomplete for short-circuit calculation."
                        }

                return
            }

            val result =
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
                result.toString()

            state.showReport = true
            state.engineeringError = null

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "فشل حساب القصر الكهربائي."
                    } else {
                        "Short-circuit calculation failed."
                    }
        }
    }

    /*
     * ============================================================
     * PANEL SCHEDULE
     * ============================================================
     */

    fun runPanelSchedule() {

        try {

            val panel =
                state.nodes.firstOrNull {
                    it.type == SldNodeType.PANEL
                }

            if (panel == null) {

                state.engineeringError =
                    if (arabic) {
                        "لا توجد لوحة في المخطط."
                    } else {
                        "No panel exists in the SLD."
                    }

                return
            }

            val currentNetwork =
                network()

            val topology =
                SldEngineeringFacade.checkEngineeringTopology(
                    currentNetwork
                )

            if (!topology.valid) {

                state.engineeringError =
                    topology.errorMessage
                        ?.takeIf { it.isNotBlank() }
                        ?: if (arabic) {
                            "المخطط غير مكتمل لإنشاء جدول اللوحة."
                        } else {
                            "The SLD is incomplete for panel schedule."
                        }

                return
            }

            val result =
                SldEngineeringFacade.calculateComplete(
                    network = currentNetwork,
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
                result.panelSchedule?.toString()
                    ?: if (arabic) {
                        "لم يتم إنشاء جدول اللوحة."
                    } else {
                        "Panel schedule was not generated."
                    }

            state.showReport = true
            state.engineeringError = null

        } catch (error: Throwable) {

            state.engineeringError =
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: if (arabic) {
                        "فشل إنشاء جدول اللوحة."
                    } else {
                        "Panel schedule failed."
                    }
        }
    }

    /*
     * ============================================================
     * NUMERIC HELPERS
     * ============================================================
     */

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
            ?: fallback.coerceAtLeast(0.000001)
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
            ?: fallback.coerceAtLeast(0.0)
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

    /*
     * ============================================================
     * BUSBAR CURRENT
     * ============================================================
     */

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

    /*
     * ============================================================
     * IDS
     * ============================================================
     */

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

    /*
     * ============================================================
     * FORMATTING
     * ============================================================
     */

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
