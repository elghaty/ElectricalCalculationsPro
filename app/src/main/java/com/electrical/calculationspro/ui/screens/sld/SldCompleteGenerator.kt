package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType

object SldCompleteGenerator {

    fun generate(): SldNetwork {

        val source =
            SldNode(
                id = "auto-source",
                name = "UTILITY SOURCE",
                type = SldNodeType.SOURCE,
                x = 70f,
                y = 380f,
                voltage = 400.0,
                sourceShortCircuitMva = 500.0
            )

        val breaker1 =
            SldNode(
                id = "auto-breaker-1",
                name = "MAIN ACB",
                type = SldNodeType.BREAKER,
                x = 310f,
                y = 380f,
                voltage = 400.0
            )

        val transformer =
            SldNode(
                id = "auto-transformer",
                name = "TR-01",
                type = SldNodeType.TRANSFORMER,
                x = 550f,
                y = 380f,
                voltage = 400.0,
                ratedKva = 1000.0,
                transformerPercentZ = 6.0
            )

        val bus =
            SldNode(
                id = "auto-bus",
                name = "MSB",
                type = SldNodeType.BUS,
                x = 790f,
                y = 380f,
                voltage = 400.0
            )

        val breaker2 =
            SldNode(
                id = "auto-breaker-2",
                name = "FEEDER ACB",
                type = SldNodeType.BREAKER,
                x = 1030f,
                y = 380f,
                voltage = 400.0
            )

        val panel =
            SldNode(
                id = "auto-panel",
                name = "MDB-01",
                type = SldNodeType.PANEL,
                x = 1270f,
                y = 380f,
                voltage = 400.0,
                ratedKva = 630.0
            )

        val load1 =
            SldNode(
                id = "auto-load-1",
                name = "LOAD-01",
                type = SldNodeType.LOAD,
                x = 1550f,
                y = 220f,
                voltage = 400.0,
                loadKw = 150.0,
                powerFactor = 0.90,
                demandFactor = 0.80
            )

        val load2 =
            SldNode(
                id = "auto-load-2",
                name = "LOAD-02",
                type = SldNodeType.LOAD,
                x = 1550f,
                y = 380f,
                voltage = 400.0,
                loadKw = 200.0,
                powerFactor = 0.90,
                demandFactor = 0.85
            )

        val load3 =
            SldNode(
                id = "auto-load-3",
                name = "LOAD-03",
                type = SldNodeType.LOAD,
                x = 1550f,
                y = 540f,
                voltage = 400.0,
                loadKw = 100.0,
                powerFactor = 0.92,
                demandFactor = 0.75
            )

        fun connection(
            id: String,
            from: String,
            to: String,
            length: Double,
            section: Double,
            runs: Int,
            capacity: Double,
            resistance: Double = 0.125,
            reactance: Double = 0.080
        ): SldConnection =
            SldConnection(
                id = id,
                fromNodeId = from,
                toNodeId = to,
                lengthMeters = length,
                resistanceOhmPerKm = resistance,
                reactanceOhmPerKm = reactance,
                cableSizeMm2 = section,
                parallelRuns = runs,
                currentCapacityA = capacity
            )

        return SldNetwork(

            nodes =
                listOf(
                    source,
                    breaker1,
                    transformer,
                    bus,
                    breaker2,
                    panel,
                    load1,
                    load2,
                    load3
                ),

            connections =
                listOf(

                    connection(
                        "auto-c1",
                        source.id,
                        breaker1.id,
                        10.0,
                        240.0,
                        1,
                        350.0
                    ),

                    connection(
                        "auto-c2",
                        breaker1.id,
                        transformer.id,
                        15.0,
                        240.0,
                        2,
                        700.0
                    ),

                    connection(
                        "auto-c3",
                        transformer.id,
                        bus.id,
                        5.0,
                        300.0,
                        2,
                        850.0,
                        0.080,
                        0.070
                    ),

                    connection(
                        "auto-c4",
                        bus.id,
                        breaker2.id,
                        5.0,
                        300.0,
                        2,
                        850.0,
                        0.080,
                        0.070
                    ),

                    connection(
                        "auto-c5",
                        breaker2.id,
                        panel.id,
                        30.0,
                        240.0,
                        2,
                        700.0
                    ),

                    connection(
                        "auto-c6",
                        panel.id,
                        load1.id,
                        25.0,
                        95.0,
                        1,
                        190.0
                    ),

                    connection(
                        "auto-c7",
                        panel.id,
                        load2.id,
                        30.0,
                        120.0,
                        1,
                        220.0
                    ),

                    connection(
                        "auto-c8",
                        panel.id,
                        load3.id,
                        20.0,
                        70.0,
                        1,
                        160.0
                    )
                )
        )
    }
}
