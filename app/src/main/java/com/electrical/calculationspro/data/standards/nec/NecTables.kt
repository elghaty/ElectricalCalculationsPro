package com.electrical.calculationspro.data.standards.nec

/**
 * Central NEC data registry.
 *
 * This object prevents NEC data from being mixed with IEC or
 * Egyptian-code data.
 */
object NecTables {

    const val STANDARD = "NFPA 70 - National Electrical Code"

    /**
     * Edition selected by the application.
     *
     * This should be changed only after the actual project NEC edition
     * has been verified and its datasets have been entered.
     */
    const val DEFAULT_EDITION = "Project-selected NEC edition"

    val referencedArticles: List<String> =
        listOf(
            "210",
            "215",
            "220",
            "240",
            "250",
            "310"
        )

    /**
     * NEC conductor size representation.
     *
     * NEC commonly uses AWG/kcmil rather than the IEC metric series.
     */
    val standardConductorIdentifiers: List<String> =
        listOf(
            "14 AWG",
            "12 AWG",
            "10 AWG",
            "8 AWG",
            "6 AWG",
            "4 AWG",
            "3 AWG",
            "2 AWG",
            "1 AWG",
            "1/0 AWG",
            "2/0 AWG",
            "3/0 AWG",
            "4/0 AWG",
            "250 kcmil",
            "300 kcmil",
            "350 kcmil",
            "400 kcmil",
            "500 kcmil",
            "600 kcmil",
            "750 kcmil",
            "1000 kcmil"
        )

    data class CodeDatasetStatus(
        val article: String,
        val populated: Boolean,
        val sourceRequired: String,
        val notes: String
    )

    fun datasetStatus(): List<CodeDatasetStatus> =
        referencedArticles.map { article ->

            CodeDatasetStatus(
                article = article,
                populated = false,
                sourceRequired =
                    "Verified NEC edition / licensed source",
                notes =
                    "Dataset structure exists; exact table values must be populated before declaring NEC compliance."
            )
        }

    fun isReadyForCompliance(): Boolean =
        datasetStatus().all {
            it.populated
        }
}
