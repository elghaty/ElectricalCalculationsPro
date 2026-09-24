package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import kotlin.math.sqrt

/**
 * Canonical bridge between DesignProject and the SLD engineering model.
 *
 * The SLD is a topology representation of the actual electrical design.
 *
 * Initial generated topology:
 *
 * SOURCE
 *   ↓
 * TRANSFORMER / GENERATOR
 *   ↓
 * MAIN BREAKER
 *   ↓
 * BUS
 *   ↓
 * PANEL
 *   ↓
 * FEEDER BREAKER
 *   ↓
 * LOAD / PUMP
 *
 * Important:
 * - Stored SLD is never rebuilt during normal recalculation.
 * - Manual node positions are preserved.
 * - Automatic generation happens only when no SLD exists.
 * - Engineering calculations remain in the engineering core.
 */
object DesignProjectSldBridge {

    fun getNetwork(
        project: DesignProject
    ): SldNetwork? =
        project.electrical.sld?.network

    fun getActiveNetwork(): SldNetwork? =
        DesignProjects
            .getActive()
            ?.let(::getNetwork)

    fun saveNetwork(
        project: DesignProject,
        network: SldNetwork,
        name: String = "Main SLD",
        source: String = "Electrical Design"
    ): DesignProject {

        val current =
            project.electrical.sld

        val sld =
            ElectricalSldDesign(
                id =
                    current?.id
                        ?: java.util.UUID.randomUUID().toString(),

                name =
                    name.ifBlank {
                        current?.name ?: "Main SLD"
                    },

                source =
                    source.ifBlank {
                        current?.source ?: "Electrical Design"
                    },

                nodes =
                    network.nodes.map {
                        it.id
                    },

                connections =
                    network.connections.map {
                        it.id
                    },

                network =
                    network,

                status =
                    DesignCalculationStatus.CALCULATED
            )

        return project.withElectrical(
            project.electrical.copy(
                sld = sld
            )
        )
    }

    fun saveNetworkToActiveProject(
        network: SldNetwork,
        name: String = "Main SLD",
        source: String = "Electrical Design"
    ): DesignProject? {

        val project =
            DesignProjects.getActive()
                ?: return null

        val updated =
            saveNetwork(
                project = project,
                network = network,
                name = name,
                source = source
            )

        return DesignProjects.save(updated)
    }

    fun clearNetwork(
        project: DesignProject
    ): DesignProject =
        project.withElectrical(
            project.electrical.copy(
                sld = null
            )
        )

    /**
     * Returns the stored SLD when one exists.
     *
     * This is deliberately different from buildFromElectricalDesign().
     *
     * Normal project recalculation must not destroy manual SLD editing.
     */
    fun rebuildFromProject(
        project: DesignProject
    ): SldNetwork {

        val stored =
            getNetwork(project)

        if (stored != null) {
            return stored
        }

        return buildFromElectricalDesign(
            project.electrical
        )
    }

    /**
     * Builds the initial SLD from the actual electrical project.
     *
     * This function is used only when there is no stored SLD.
     *
     * The generated network contains explicit protection and bus
     * elements instead of drawing a simple line directly from
     * panel to load.
     */
    fun buildFromElectricalDesign(
        electrical: ElectricalDesign
    ): SldNetwork {

        val nodes =
            mutableListOf<SldNode>()

        val connections =
            mutableListOf<SldConnection>()

        val sourceId =
            "project-source"

        val sourceVoltage =
            electrical.panels
                .firstOrNull()
                ?.voltageV
                ?.takeIf { it > 0.0 }
                ?: electrical.loads
                    .firstOrNull()
                    ?.voltageV
                    ?.takeIf { it > 0.0 }
                ?: 400.0

        val sourceShortCircuitMva =
            electrical.panels
                .firstOrNull()
                ?.shortCircuitKA
                ?.takeIf { it > 0.0 }
                ?.let {
                    sqrt(3.0) *
                        sourceVoltage *
                        it /
                        1000.0
                }
                ?: 500.0

        /*
         * ---------------------------------------------------------
         * SOURCE
         * ---------------------------------------------------------
         */
        nodes +=
            SldNode(
                id = sourceId,
                name = "MAIN SOURCE",
                type = SldNodeType.SOURCE,
                x = 80f,
                y = 400f,
                voltage = sourceVoltage,
                sourceShortCircuitMva =
                    sourceShortCircuitMva
            )

        /*
         * ---------------------------------------------------------
         * UPSTREAM EQUIPMENT
         * ---------------------------------------------------------
         *
         * Transformer is connected to the source.
         *
         * Generator is represented as an independent source-side
         * equipment element. It is not incorrectly connected
         * directly to the utility source.
         */
        electrical.transformers.forEachIndexed {
                index,
                transformer
            ->

            val nodeId =
                transformer.id

            if (
                nodes.none {
                    it.id == nodeId
                }
            ) {

                nodes +=
                    SldNode(
                        id = nodeId,
                        name = transformer.name,
                        type = SldNodeType.TRANSFORMER,
                        x = 330f,
                        y =
                            250f +
                                index * 240f,
                        voltage =
                            transformer.secondaryVoltageV,
                        ratedKva =
                            transformer.ratingKva,
                        transformerPercentZ =
                            transformer.impedancePercent
                    )

                connections +=
                    SldConnection(
                        id =
                            "source-to-transformer-$nodeId",
                        fromNodeId =
                            sourceId,
                        toNodeId =
                            nodeId
                    )
            }
        }

        electrical.generators.forEachIndexed {
                index,
                generator
            ->

            val nodeId =
                generator.id

            if (
                nodes.none {
                    it.id == nodeId
                }
            ) {

                nodes +=
                    SldNode(
                        id = nodeId,
                        name = generator.name,
                        type = SldNodeType.GENERATOR,
                        x = 330f,
                        y =
                            700f +
                                index * 240f,
                        voltage =
                            generator.voltageV,
                        ratedKva =
                            generator.ratingKva,
                        powerFactor =
                            generator.powerFactor
                    )
            }
        }

        /*
         * ---------------------------------------------------------
         * PANELS
         * ---------------------------------------------------------
         *
         * Each panel gets a protection element and a bus element
         * when it is the first downstream panel.
         */
        electrical.panels.forEachIndexed {
                index,
                panel
            ->

            val panelId =
                panel.id

            val panelVoltage =
                panel.voltageV
                    .takeIf { it > 0.0 }
                    ?: sourceVoltage

            val panelFaultMva =
                if (
                    panel.shortCircuitKA > 0.0
                ) {
                    sqrt(3.0) *
                        panelVoltage *
                        panel.shortCircuitKA /
                        1000.0
                } else {
                    0.0
                }

            if (
                nodes.none {
                    it.id == panelId
                }
            ) {

                nodes +=
                    SldNode(
                        id = panelId,
                        name = panel.name,
                        type = SldNodeType.PANEL,
                        x =
                            1120f,
                        y =
                            250f +
                                index * 220f,
                        voltage =
                            panelVoltage,
                        loadKw =
                            panel.designLoadKw,
                        sourceShortCircuitMva =
                            panelFaultMva
                    )
            }

            val sourcePanelId =
                panel.sourceId

            val upstream =
                sourcePanelId
                    ?.takeIf { parentId ->
                        nodes.any {
                            it.id == parentId
                        }
                    }
                    ?: electrical.transformers
                        .firstOrNull()
                        ?.id
                    ?: sourceId

            /*
             * Main protection for the panel.
             */
            val breakerId =
                "main-breaker-$panelId"

            if (
                nodes.none {
                    it.id == breakerId
                }
            ) {

                nodes +=
                    SldNode(
                        id = breakerId,
                        name = "${panel.name} MAIN BREAKER",
                        type = SldNodeType.BREAKER,
                        x = 720f,
                        y =
                            250f +
                                index * 220f,
                        voltage =
                            panelVoltage,
                        ratedKva =
                            panel.designLoadKw
                    )
            }

            val busId =
                "bus-$panelId"

            if (
                nodes.none {
                    it.id == busId
                }
            ) {

                nodes +=
                    SldNode(
                        id = busId,
                        name = "${panel.name} BUS",
                        type = SldNodeType.BUS,
                        x = 930f,
                        y =
                            250f +
                                index * 220f,
                        voltage =
                            panelVoltage,
                        loadKw =
                            panel.designLoadKw
                    )
            }

            addConnectionIfMissing(
                connections,
                id =
                    "upstream-to-breaker-$panelId",
                from =
                    upstream,
                to =
                    breakerId
            )

            addConnectionIfMissing(
                connections,
                id =
                    "breaker-to-bus-$panelId",
                from =
                    breakerId,
                to =
                    busId
            )

            addConnectionIfMissing(
                connections,
                id =
                    "bus-to-panel-$panelId",
                from =
                    busId,
                to =
                    panelId
            )
        }

        /*
         * ---------------------------------------------------------
         * LOADS
         * ---------------------------------------------------------
         *
         * Loads are attached to their source panel where available.
         * Otherwise they are attached to the first project panel.
         */
        electrical.loads.forEachIndexed {
                index,
                load
            ->

            val loadId =
                load.id

            if (
                nodes.none {
                    it.id == loadId
                }
            ) {

                val loadVoltage =
                    load.voltageV
                        .takeIf { it > 0.0 }
                        ?: sourceVoltage

                val calculatedLoadKw =
                    load.designLoadKw
                        .takeIf { it > 0.0 }
                        ?: (
                            load.connectedLoadKw *
                                load.quantity
                            )

                nodes +=
                    SldNode(
                        id = loadId,
                        name = load.name,
                        type = SldNodeType.LOAD,
                        x = 1580f,
                        y =
                            180f +
                                (index % 5) *
                                180f,
                        voltage =
                            loadVoltage,
                        loadKw =
                            calculatedLoadKw,
                        powerFactor =
                            load.powerFactor,
                        demandFactor =
                            load.demandFactor
                    )
            }

            val panelId =
                load.sourcePanelId
                    ?.takeIf { source ->
                        nodes.any {
                            it.id == source &&
                                it.type ==
                                SldNodeType.PANEL
                        }
                    }
                    ?: electrical.panels
                        .firstOrNull()
                        ?.id

            val parent =
                panelId
                    ?: electrical.transformers
                        .firstOrNull()
                        ?.id
                    ?: sourceId

            val breakerId =
                "feeder-breaker-$loadId"

            if (
                nodes.none {
                    it.id == breakerId
                }
            ) {

                val loadNode =
                    nodes.firstOrNull {
                        it.id == loadId
                    }

                nodes +=
                    SldNode(
                        id = breakerId,
                        name =
                            "${load.name} FEEDER BREAKER",
                        type =
                            SldNodeType.BREAKER,
                        x = 1350f,
                        y =
                            loadNode?.y
                                ?: 180f,
                        voltage =
                            loadNode?.voltage
                                ?: sourceVoltage,
                        ratedKva =
                            loadNode?.loadKw
                                ?: 0.0
                    )
            }

            addConnectionIfMissing(
                connections,
                id =
                    "feeder-breaker-input-$loadId",
                from =
                    parent,
                to =
                    breakerId
            )

            addConnectionIfMissing(
                connections,
                id =
                    "feeder-to-load-$loadId",
                from =
                    breakerId,
                to =
                    loadId
            )
        }

        /*
         * ---------------------------------------------------------
         * PUMP LOADS
         * ---------------------------------------------------------
         *
         * PumpElectricalIntegration already converts pumps into
         * ElectricalLoad objects. Therefore pumps automatically
         * enter the SLD through electrical.loads.
         */
        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

    private fun addConnectionIfMissing(
        connections: MutableList<SldConnection>,
        id: String,
        from: String,
        to: String
    ) {

        if (from == to) {
            return
        }

        val exists =
            connections.any {
                it.fromNodeId == from &&
                    it.toNodeId == to
            }

        if (!exists) {

            connections +=
                SldConnection(
                    id = id,
                    fromNodeId = from,
                    toNodeId = to
                )
        }
    }
}
