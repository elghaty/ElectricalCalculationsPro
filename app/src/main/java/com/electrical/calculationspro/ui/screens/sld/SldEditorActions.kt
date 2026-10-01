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

        const val NODE_WIDTH = 180f
        const val NODE_HEIGHT = 118f
    }

    // ============================================================
    // NETWORK
    // ============================================================

    private fun network(): SldNetwork {
        return SldNetwork(
            nodes = state.nodes.toList(),
            connections = state.connections.toList()
        )
    }

    // ============================================================
    // SAFE PERSISTENCE
    // ============================================================

    private fun safePersist() {

        val callback = onSave ?: return

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

    // ============================================================
    // SAVE + RECALCULATE
    // ============================================================

    fun saveAndRecalculate() {

        safePersist()

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

    // ============================================================
    // ENGINEERING
    // ============================================================

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

            val result =
                SldEngineeringFacade.calculateComplete(
                    network = currentNetwork
                )

            state.engineeringPackage = result
            state.engineeringError = null

        } catch (error: Throwable) {

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

    // ============================================================
    // NODE CREATION
    // ============================================================

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

            state.nodes =
                state.nodes + node

            state.selectedNodeId = node.id
            state.selectedConnectionId = null
            state.connectionStartId = null

            state.engineeringPackage = null

            state.engineeringError =
                if (arabic) {
                    "تمت إضافة ${node.name}. العنصر غير متصل بعد."
                } else {
                    "${node.name} was added. The component is not connected yet."
                }

            safePersist()

        } catch (error: Throwable) {

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
            id = createNodeId(),

            name = defaultNodeName(type),

            type = type,

            x = safeX,

            y = safeY,

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

    // ============================================================
    // NODE DIALOG
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

    // ============================================================
    // SAVE NODE
    // ============================================================

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

            val type = state.nodeType

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
                        id = createNodeId(),

                        name = name,

                        type = type,

                        x = position.first,

                        y = position.second,

                        voltage = voltage,

                        loadKw =
                            if (type == SldNodeType.LOAD) {
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
                    state.nodes + node

                state.selectedNodeId = node.id
                state.selectedConnectionId = null
                state.connectionStartId = null

            } else {

                val updated =
                    existing.copy(

                        name = name,

                        type = type,

                        voltage = voltage,

                        loadKw =
                            if (type == SldNodeType.LOAD) {
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
                        node ->
                        if (node.id == existing.id) {
                            updated
                        } else {
                            node
                        }
                    }

                state.selectedNodeId = updated.id
            }

            state.showNodeDialog = false
            state.editingNodeId = null
            state.connectionStartId = null

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

    // ============================================================
    // BREAKER POSITION
    // ============================================================

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

    // ============================================================
    // FREE POSITION
    // ============================================================

    private fun findFreeNodePosition(
        type: SldNodeType
    ): Pair<Float, Float> {

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

                if (it.id == node.id) {

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

        safePersist()

        try {
            recalculateEngineering()
        } catch (_: Throwable) {
            state.engineeringPackage = null
        }
    }

    // ============================================================
    // CONNECTION MODE
    // ============================================================

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
                }

            if (target == null) {

                state.engineeringError =
                    if (arabic) {
                        "العنصر المحدد غير موجود."
                    } else {
                        "The selected component does not exist."
                    }

                return
            }

            val startId =
                state.connectionStartId

            /*
             * ====================================================
             * FIRST CLICK
             * ====================================================
             */

            if (startId == null) {

                state.selectedNodeId =
                    target.id

                state.selectedConnectionId =
                    null

                state.connectionStartId =
                    target.id

                state.engineeringError = null

                return
            }

            /*
             * ====================================================
             * SAME NODE
             * ====================================================
             */

            if (startId == target.id) {

                state.engineeringError =
                    if (arabic) {
                        "اختر عنصرًا آخر كطرف ثانٍ."
                    } else {
                        "Select another component as the destination."
                    }

                /*
                 * Keep connection mode active.
                 */
                return
            }

            val from =
                state.nodes.firstOrNull {
                    it.id == startId
                }

            if (from == null) {

                state.connectionStartId = null

                state.engineeringError =
                    if (arabic) {
                        "تعذر العثور على العنصر الأول."
                    } else {
                        "The first component could not be found."
                    }

                return
            }

            /*
             * ====================================================
             * DUPLICATE
             * ====================================================
             */

            val duplicate =
                state.connections.any {
                    it.fromNodeId == from.id &&
                        it.toNodeId == target.id
                }

            if (duplicate) {

                state.engineeringError =
                    if (arabic) {
                        "هذا الاتصال موجود بالفعل."
                    } else {
                        "This connection already exists."
                    }

                /*
                 * Keep the first element selected so the user can
                 * choose another destination.
                 */
                state.selectedNodeId =
                    from.id

                state.connectionStartId =
                    from.id

                return
            }

            /*
             * ====================================================
             * PANEL -> BREAKER = BUSBAR
             * ====================================================
             */

            if (
                from.type == SldNodeType.PANEL &&
                target.type == SldNodeType.BREAKER
            ) {

                createBusbarConnection(
                    from = from,
                    to = target
                )

                state.selectedNodeId =
                    target.id

                state.selectedConnectionId =
                    state.connections
                        .lastOrNull {
                            it.fromNodeId == from.id &&
                                it.toNodeId == target.id &&
                                it.connectionType ==
                                SldConnectionType.BUSBAR
                        }
                        ?.id

                state.connectionStartId = null

                state.engineeringError = null

                safePersist()

                /*
                 * Recalculate only after the drawing operation has
                 * completed.
                 */
                try {
                    recalculateEngineering()
                } catch (_: Throwable) {
                    state.engineeringPackage = null
                }

                return
            }

            /*
             * ====================================================
             * CABLE CONNECTION
             * ====================================================
             *
             * CRITICAL FIX:
             *
             * The second node MUST become selectedNodeId before
             * opening the dialog.
             *
             * saveConnection() uses:
             *
             * connectionStartId -> FROM
             * selectedNodeId    -> TO
             *
             * Without this assignment the old implementation saw
             * the first node as both FROM and TO.
             */

            state.selectedNodeId =
                target.id

            state.selectedConnectionId =
                null

            state.editingConnectionId =
                null

            state.connectionType =
                SldConnectionType.CABLE.name

            state.length = "50"
            state.resistance = "0.125"
            state.reactance = "0.080"
            state.cableSize = "240"
            state.parallelRuns = "1"
            state.capacity = "350"

            state.showConnectionDialog = true

            state.engineeringError = null

        } catch (error: Throwable) {

            /*
             * The dialog is never allowed to become a crash source.
             */
            state.showConnectionDialog = false

            state.editingConnectionId = null

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

    // ============================================================
    // BUSBAR
    // ============================================================

    private fun createBusbarConnection(
        from: SldNode,
        to: SldNode
    ) {

        if (
            from.type != SldNodeType.PANEL ||
            to.type != SldNodeType.BREAKER
        ) {
            return
        }

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
                 * BUSBAR has no cable parameters.
                 */
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
                        },

                busbarRatedCurrentA =
                    estimateBusbarCurrent(from),

                busbarShortCircuitKA =
                    parseNonNegative(
                        state.busbarShortCircuit,
                        DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
                    )
            )

        state.connections =
            state.connections + connection
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
                .ifBlank {
                    "Copper"
                }

        state.insulationType =
            connection.insulationType
                .ifBlank {
                    "PVC"
                }

        state.installationMethodCode =
            connection.installationMethodCode
                .ifBlank {
                    "B1"
                }

        state.busbarMaterial =
            connection.busbarMaterial
                .ifBlank {
                    "Copper"
                }

        state.busbarRatedCurrent =
            connection.busbarRatedCurrentA
                .toEngineeringString()

        state.busbarShortCircuit =
            connection.busbarShortCircuitKA
                .toEngineeringString()

        /*
         * For editing an existing connection, endpoint IDs are
         * not changed by the dialog.
         */
        state.connectionStartId = null

        state.showConnectionDialog = true
    }

    // ============================================================
    // SAVE CONNECTION
    // ============================================================
    //
    // IMPORTANT:
    //
    // The dialog is closed BEFORE persistence/calculation.
    //
    // This prevents the Save button from leaving the dialog visible
    // when an engineering calculation or persistence callback fails.
    // ============================================================

    fun saveConnection() {

        /*
         * ========================================================
         * CAPTURE ALL REQUIRED STATE FIRST
         * ========================================================
         */

        val editingId =
            state.editingConnectionId

        val newFromId =
            state.connectionStartId

        val newToId =
            state.selectedNodeId

        /*
         * ========================================================
         * CLOSE DIALOG IMMEDIATELY
         * ========================================================
         *
         * This is intentionally before any operation that may
         * throw.
         */

        state.showConnectionDialog = false
        state.editingConnectionId = null

        /*
         * Keep endpoint IDs in local variables.
         *
         * connectionStartId is cleared only after these local
         * values have been captured.
         */
        state.connectionStartId = null

        try {

            /*
             * ====================================================
             * EDIT EXISTING CONNECTION
             * ====================================================
             */

            if (editingId != null) {

                val existing =
                    state.connections.firstOrNull {
                        it.id == editingId
                    }

                if (existing == null) {

                    state.engineeringError =
                        if (arabic) {
                            "تعذر العثور على الوصلة المطلوب تعديلها."
                        } else {
                            "The connection to edit could not be found."
                        }

                    return
                }

                /*
                 * ------------------------------------------------
                 * EXISTING BUSBAR
                 * ------------------------------------------------
                 */

                if (
                    existing.connectionType ==
                    SldConnectionType.BUSBAR
                ) {

                    val from =
                        state.nodes.firstOrNull {
                            it.id == existing.fromNodeId
                        }

                    val to =
                        state.nodes.firstOrNull {
                            it.id == existing.toNodeId
                        }

                    if (
                        from == null ||
                        to == null ||
                        from.type != SldNodeType.PANEL ||
                        to.type != SldNodeType.BREAKER
                    ) {

                        state.engineeringError =
                            if (arabic) {
                                "وصلة الـBUSBAR غير مرتبطة بلوحة وقاطع بشكل صحيح."
                            } else {
                                "The BUSBAR connection is not a valid PANEL-to-BREAKER connection."
                            }

                        return
                    }

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

                            currentCapacityA = 0.0,

                            conductorMaterial = "",

                            insulationType = "",

                            installationMethodCode = "",

                            busbarMaterial =
                                state.busbarMaterial
                                    .ifBlank {
                                        "Copper"
                                    },

                            busbarRatedCurrentA =
                                parseNonNegative(
                                    state.busbarRatedCurrent,
                                    existing.busbarRatedCurrentA
                                ),

                            busbarShortCircuitKA =
                                parseNonNegative(
                                    state.busbarShortCircuit,
                                    existing.busbarShortCircuitKA
                                )
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

                    state.selectedConnectionId =
                        existing.id

                    state.selectedNodeId =
                        to.id

                } else {

                    /*
                     * ------------------------------------------------
                     * EXISTING CABLE
                     * ------------------------------------------------
                     */

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

                    state.selectedNodeId =
                        state.nodes.firstOrNull {
                            it.id == existing.toNodeId
                        }?.id
                }

            } else {

                /*
                 * ====================================================
                 * CREATE NEW CONNECTION
                 * ====================================================
                 */

                val from =
                    newFromId?.let { id ->
                        state.nodes.firstOrNull {
                            it.id == id
                        }
                    }

                val to =
                    newToId?.let { id ->
                        state.nodes.firstOrNull {
                            it.id == id
                        }
                    }

                if (
                    from == null ||
                    to == null
                ) {

                    state.engineeringError =
                        if (arabic) {
                            "حدد عنصري البداية والنهاية قبل الحفظ."
                        } else {
                            "Select valid start and destination components before saving."
                        }

                    return
                }

                if (from.id == to.id) {

                    state.engineeringError =
                        if (arabic) {
                            "لا يمكن توصيل العنصر بنفسه."
                        } else {
                            "A component cannot be connected to itself."
                        }

                    return
                }

                /*
                 * Duplicate protection.
                 */
                val duplicate =
                    state.connections.any {
                        it.fromNodeId == from.id &&
                            it.toNodeId == to.id
                    }

                if (duplicate) {

                    state.selectedNodeId =
                        to.id

                    state.engineeringError =
                        if (arabic) {
                            "هذا الاتصال موجود بالفعل."
                        } else {
                            "This connection already exists."
                        }

                    return
                }

                /*
                 * ------------------------------------------------
                 * PANEL -> BREAKER = BUSBAR
                 * ------------------------------------------------
                 */

                if (
                    from.type == SldNodeType.PANEL &&
                    to.type == SldNodeType.BREAKER
                ) {

                    createBusbarConnection(
                        from = from,
                        to = to
                    )

                    state.selectedNodeId =
                        to.id

                    state.selectedConnectionId =
                        state.connections
                            .lastOrNull {
                                it.fromNodeId == from.id &&
                                    it.toNodeId == to.id &&
                                    it.connectionType ==
                                    SldConnectionType.BUSBAR
                            }
                            ?.id

                } else {

                    /*
                     * ------------------------------------------------
                     * NORMAL EXTERNAL FEEDER = CABLE
                     * ------------------------------------------------
                     */

                    val cable =
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

                            voltageDropPercent = 0.0,

                            currentCapacityA =
                                parseNonNegative(
                                    state.capacity,
                                    350.0
                                ),

                            conductorMaterial =
                                state.conductorMaterial
                                    .ifBlank {
                                        "Copper"
                                    },

                            insulationType =
                                state.insulationType
                                    .ifBlank {
                                        "XLPE"
                                    },

                            installationMethodCode =
                                state.installationMethodCode
                                    .ifBlank {
                                        "C"
                                    },

                            busbarMaterial = "",

                            busbarRatedCurrentA = 0.0,

                            busbarShortCircuitKA = 0.0
                        )

                    state.connections =
                        state.connections + cable

                    state.selectedNodeId =
                        to.id

                    state.selectedConnectionId =
                        cable.id
                }
            }

            /*
             * ====================================================
             * DRAWING STATE IS NOW AUTHORITATIVE
             * ====================================================
             */

            state.engineeringPackage = null
            state.engineeringError = null

            /*
             * Persist the connection.
             *
             * safePersist itself catches persistence exceptions.
             */
            safePersist()

            /*
             * ====================================================
             * ENGINEERING RECALCULATION
             * ====================================================
             *
             * An incomplete topology is NOT a drawing failure.
             */

            try {

                recalculateEngineering()

            } catch (error: Throwable) {

                state.engineeringPackage = null

                state.engineeringError =
                    error.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: if (arabic) {
                            "تم حفظ التوصيل، لكن المخطط يحتاج إلى استكمال التوصيلات قبل الحساب."
                        } else {
                            "The connection was saved, but the SLD requires additional connections before calculation."
                        }
            }

        } catch (error: Throwable) {

            /*
             * FINAL SAFETY NET
             *
             * The dialog has already been closed.
             * Therefore an unexpected exception cannot leave the
             * Save dialog stuck on screen.
             */

            state.showConnectionDialog = false
            state.editingConnectionId = null
            state.connectionStartId = null

            state.engineeringError =
                error.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "حدث خطأ أثناء حفظ التوصيل، وتم الاحتفاظ بالمخطط."
                    } else {
                        "An error occurred while saving the connection. The drawing was preserved."
                    }
        }
    }

    // ============================================================
    // CANCEL CONNECTION
    // ============================================================

    fun cancelConnectionEdit() {

        state.showConnectionDialog = false
        state.editingConnectionId = null
        state.connectionStartId = null
        state.selectedConnectionId = null
    }

    // ============================================================
    // DELETE
    // ============================================================

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

            state.engineeringPackage = null

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

            state.clearSelection()

            state.engineeringPackage = null

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

    // ============================================================
    // GENERATE COMPLETE SLD
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

            state.engineeringPackage = null

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

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

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

    // ============================================================
    // PANEL SCHEDULE
    // ============================================================

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

    // ============================================================
    // NUMERIC HELPERS
    // ============================================================

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

    // ============================================================
    // BUSBAR CURRENT
    // ============================================================

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

    // ============================================================
    // IDS
    // ============================================================

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

    // ============================================================
    // FORMATTING
    // ============================================================

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
