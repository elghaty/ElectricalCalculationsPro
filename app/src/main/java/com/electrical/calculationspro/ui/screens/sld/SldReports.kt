package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldShortCircuitStudy

fun buildShortCircuitReport(
    study: SldShortCircuitStudy,
    arabic: Boolean
): String {

    val result = StringBuilder()

    result.appendLine(
        if (arabic) {
            "تقرير هندسي لحساب تيارات القصر"
        } else {
            "SHORT CIRCUIT ENGINEERING REPORT"
        }
    )

    result.appendLine("================================")

    result.appendLine(
        "Maximum symmetrical fault current = " +
            fmt(study.maximumFaultCurrentKa) +
            " kA"
    )

    result.appendLine(
        "Maximum peak current = " +
            fmt(study.maximumPeakCurrentKa) +
            " kA"
    )

    result.appendLine(
        "Maximum fault level = " +
            fmt(study.maximumFaultMva) +
            " MVA"
    )

    study.results.values.forEachIndexed { index, item ->

        result.appendLine()

        result.appendLine(
            "${index + 1}. ${item.nodeName}"
        )

        result.appendLine(
            "Voltage = ${fmt(item.voltageV)} V"
        )

        result.appendLine(
            "R = ${fmt(item.resistanceOhm)} Ω"
        )

        result.appendLine(
            "X = ${fmt(item.reactanceOhm)} Ω"
        )

        result.appendLine(
            "Z = ${fmt(item.impedanceOhm)} Ω"
        )

        result.appendLine(
            "Ik'' = ${fmt(item.initialSymmetricalCurrentKa)} kA"
        )

        result.appendLine(
            "Ip = ${fmt(item.peakCurrentKa)} kA"
        )

        result.appendLine(
            "Ith = ${fmt(item.thermalCurrentKa)} kA"
        )

        result.appendLine(
            "Fault MVA = ${fmt(item.shortCircuitMva)} MVA"
        )

        result.appendLine(
            "X/R = ${fmt(item.xrRatio)}"
        )

        result.appendLine(
            "Breaker Required = " +
                "${fmt(item.breakerRequiredKa)} kA"
        )

        if (item.notes.isNotEmpty()) {
            result.appendLine("Notes:")

            item.notes.forEach { note ->
                result.appendLine("  - $note")
            }
        }
    }

    if (study.notes.isNotEmpty()) {

        result.appendLine()
        result.appendLine("Study Notes:")

        study.notes.forEach { note ->
            result.appendLine("  - $note")
        }
    }

    return result.toString()
}

fun buildPanelSchedule(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    arabic: Boolean
): String {

    val result = StringBuilder()

    var totalConnected = 0.0
    var totalDemand = 0.0
    var index = 1

    result.appendLine(
        if (arabic) {
            "جدول أحمال اللوحة"
        } else {
            "PANEL SCHEDULE"
        }
    )

    result.appendLine("================================")

    result.appendLine(
        "Panel: ${panel.name}"
    )

    result.appendLine(
        "Voltage: ${fmt(panel.voltage)} V"
    )

    result.appendLine(
        "Rating: ${fmt(panel.ratedKva)} kVA"
    )

    result.appendLine()

    connections
        .filter { it.fromNodeId == panel.id }
        .forEach { connection ->

            val load =
                nodes.firstOrNull {
                    it.id == connection.toNodeId
                } ?: return@forEach

            if (
                load.type !=
                    com.electrical.calculationspro.data.SldNodeType.LOAD
            ) {
                return@forEach
            }

            val connected =
                load.loadKw.coerceAtLeast(0.0)

            val demand =
                connected *
                    load.demandFactor.coerceIn(
                        0.0,
                        1.0
                    )

            totalConnected += connected
            totalDemand += demand

            result.appendLine(
                "$index | ${load.name}"
            )

            result.appendLine(
                "   Connected Load = " +
                    "${fmt(connected)} kW"
            )

            result.appendLine(
                "   Demand Load = " +
                    "${fmt(demand)} kW"
            )

            result.appendLine(
                "   PF = ${fmt(load.powerFactor)}"
            )

            result.appendLine(
                "   Cable = " +
                    "${fmt(connection.cableSizeMm2)} mm²"
            )

            result.appendLine(
                "   Runs = ${connection.parallelRuns}"
            )

            result.appendLine(
                "   Capacity = " +
                    "${fmt(connection.currentCapacityA)} A"
            )

            result.appendLine()

            index++
        }

    result.appendLine(
        "TOTAL CONNECTED LOAD = " +
            "${fmt(totalConnected)} kW"
    )

    result.appendLine(
        "TOTAL DEMAND LOAD = " +
            "${fmt(totalDemand)} kW"
    )

    if (index == 1) {

        result.appendLine()

        result.appendLine(
            if (arabic) {
                "لا توجد مغذيات أحمال خارجة من اللوحة."
            } else {
                "No outgoing load feeders are connected to this panel."
            }
        )
    }

    return result.toString()
}

fun buildCompleteSldReport(
    study: SldShortCircuitStudy,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    arabic: Boolean
): String {

    val result = StringBuilder()

    result.appendLine(
        if (arabic) {
            "تقرير SLD الكامل"
        } else {
            "COMPLETE SLD ENGINEERING REPORT"
        }
    )

    result.appendLine("================================")

    result.appendLine(
        "Nodes = ${nodes.size}"
    )

    result.appendLine(
        "Connections = ${connections.size}"
    )

    result.appendLine()

    result.appendLine(
        if (arabic) {
            "العناصر:"
        } else {
            "ELEMENTS:"
        }
    )

    nodes.forEachIndexed { index, node ->

        result.appendLine(
            "${index + 1}. ${node.name} [${node.type}]"
        )

        result.appendLine(
            "   Voltage = ${fmt(node.voltage)} V"
        )

        if (node.loadKw > 0.0) {

            result.appendLine(
                "   Load = ${fmt(node.loadKw)} kW"
            )

            result.appendLine(
                "   PF = ${fmt(node.powerFactor)}"
            )

            result.appendLine(
                "   Demand = ${fmt(node.demandFactor)}"
            )
        }

        if (node.ratedKva > 0.0) {

            result.appendLine(
                "   Rating = ${fmt(node.ratedKva)} kVA"
            )
        }

        if (node.transformerPercentZ > 0.0) {

            result.appendLine(
                "   Transformer Z = " +
                    "${fmt(node.transformerPercentZ)} %"
            )
        }

        if (node.generatorXdSubtransient > 0.0) {

            result.appendLine(
                "   Generator Xd'' = " +
                    "${fmt(node.generatorXdSubtransient)} %"
            )
        }

        if (node.sourceShortCircuitMva > 0.0) {

            result.appendLine(
                "   Source Fault Level = " +
                    "${fmt(node.sourceShortCircuitMva)} MVA"
            )
        }
    }

    result.appendLine()

    result.appendLine(
        if (arabic) {
            "المغذيات:"
        } else {
            "FEEDERS:"
        }
    )

    connections.forEachIndexed { index, connection ->

        val from =
            nodes.firstOrNull {
                it.id == connection.fromNodeId
            }

        val to =
            nodes.firstOrNull {
                it.id == connection.toNodeId
            }

        result.appendLine(
            "${index + 1}. " +
                "${from?.name ?: connection.fromNodeId} -> " +
                "${to?.name ?: connection.toNodeId}"
        )

        result.appendLine(
            "   Length = " +
                "${fmt(connection.lengthMeters)} m"
        )

        result.appendLine(
            "   Cable = " +
                "${fmt(connection.cableSizeMm2)} mm²"
        )

        result.appendLine(
            "   Runs = ${connection.parallelRuns}"
        )

        result.appendLine(
            "   Capacity = " +
                "${fmt(connection.currentCapacityA)} A"
        )
    }

    result.appendLine()

    result.append(
        buildShortCircuitReport(
            study = study,
            arabic = arabic
        )
    )

    return result.toString()
}
