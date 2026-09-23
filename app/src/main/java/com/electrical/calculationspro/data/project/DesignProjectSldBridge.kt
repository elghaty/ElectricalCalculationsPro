package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode

/**
 * Canonical bridge between the project model and the SLD
 * engineering model.
 *
 * UI does not write directly into DesignProject.
 * SLD data passes through this bridge.
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
     * Creates an initial SLD from the actual project electrical
     * entities when no manually edited SLD exists yet.
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
                    kotlin.math.sqrt(3.0) *
                        sourceVoltage *
                        it /
                        1000.0
                }
                ?: 500.0

        nodes +=
            SldNode(
                id = sourceId,
                name = "MAIN SOURCE",
                type = com.electrical.calculationspro.data.SldNodeType.SOURCE,
                x = 80f,
                y = 300f,
                voltage = sourceVoltage,
                sourceShortCircuitMva =
                    sourceShortCircuitMva
            )

        electrical.transformers.forEachIndexed {
                index,
                transformer
            ->

            val nodeId =
                transformer.id

            nodes +=
                SldNode(
                    id = nodeId,
                    name = transformer.name,
                    type =
                        com.electrical.calculationspro.data.SldNodeType.TRANSFORMER,
                    x = 300f + index * 260f,
                    y = 300f,
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
                        "source-to-$nodeId",
                    fromNodeId =
                        sourceId,
                    toNodeId =
                        nodeId,
                    lengthMeters = 0.0
                )
        }

        electrical.generators.forEachIndexed {
                index,
                generator
            ->

            val nodeId =
                generator.id

            nodes +=
                SldNode(
                    id = nodeId,
                    name = generator.name,
                    type =
                        com.electrical.calculationspro.data.SldNodeType.GENERATOR,
                    x = 300f + index * 260f,
                    y = 520f,
                    voltage =
                        generator.voltageV,
                    ratedKva =
                        generator.ratingKva,
                    powerFactor =
                        generator.powerFactor
                )
        }

        electrical.panels.forEachIndexed {
                index,
                panel
            ->

            val nodeId =
                panel.id

            nodes +=
                SldNode(
                    id = nodeId,
                    name = panel.name,
                    type =
                        com.electrical.calculationspro.data.SldNodeType.PANEL,
                    x = 560f + index * 260f,
                    y = 300f,
                    voltage =
                        panel.voltageV,
                    loadKw =
                        panel.designLoadKw,
                    sourceShortCircuitMva =
                        if (panel.shortCircuitKA > 0.0) {
                            kotlin.math.sqrt(3.0) *
                                panel.voltageV *
                                panel.shortCircuitKA /
                                1000.0
                        } else {
                            0.0
                        }
                )

            val sourcePanel =
                panel.sourceId

            val parent =
                sourcePanel
                    ?.takeIf {
                        nodes.any { node ->
                            node.id == it
                        }
                    }
                    ?: electrical.transformers
                        .firstOrNull()
                        ?.id
                    ?: sourceId

            if (parent != nodeId) {
                connections +=
                    SldConnection(
                        id =
                            "feeder-$parent-$nodeId",
                        fromNodeId =
                            parent,
                        toNodeId =
                            nodeId
                    )
            }
        }

        electrical.loads.forEachIndexed {
                index,
                load
            ->

            val nodeId =
                load.id

            nodes +=
                SldNode(
                    id = nodeId,
                    name = load.name,
                    type =
                        com.electrical.calculationspro.data.SldNodeType.LOAD,
                    x = 820f + index * 220f,
                    y = 300f +
                        (index % 4) * 160f,
                    voltage =
                        load.voltageV,
                    loadKw =
                        load.designLoadKw
                            .takeIf { it > 0.0 }
                            ?: load.connectedLoadKw *
                                load.quantity,
                    powerFactor =
                        load.powerFactor,
                    demandFactor =
                        load.demandFactor
                )

            val parent =
                load.sourcePanelId
                    ?.takeIf {
                        nodes.any { node ->
                            node.id == it
                        }
                    }
                    ?: electrical.panels
                        .firstOrNull()
                        ?.id
                    ?: electrical.transformers
                        .firstOrNull()
                        ?.id
                    ?: sourceId

            if (parent != nodeId) {
                connections +=
                    SldConnection(
                        id =
                            "load-feeder-$parent-$nodeId",
                        fromNodeId =
                            parent,
                        toNodeId =
                            nodeId
                    )
            }
        }

        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }
}
