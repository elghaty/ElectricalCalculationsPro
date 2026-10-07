package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.Standard

/**
 * =====================================================================
 * PROFESSIONAL CODE / STANDARD RULE REGISTRY
 * =====================================================================
 *
 * This registry is the controlled engineering reference layer between:
 *
 *     Selected Standard
 *             |
 *             v
 *     Code / Standard Reference
 *             |
 *             v
 *     Engineering Rule
 *             |
 *             v
 *     Calculation Engine
 *             |
 *             v
 *     Technical Requirement
 *             |
 *             v
 *     Catalog Compliance
 *
 * IMPORTANT
 * ---------------------------------------------------------------------
 * This file DOES NOT reproduce copyrighted standard text or tables.
 *
 * It stores:
 * - standard identity
 * - edition / revision
 * - normative document references
 * - engineering domains
 * - rule identifiers
 * - clause references
 * - data-source status
 * - applicability
 *
 * Numerical tables must be populated only from an authorized,
 * verified source dataset.
 *
 * A missing numerical dataset is represented as NOT_AVAILABLE.
 * It must never be silently replaced by data from another standard.
 * =====================================================================
 */
object CodeRuleRegistry {

    enum class DataStatus {
        VERIFIED,
        PARTIALLY_VERIFIED,
        NOT_AVAILABLE,
        NOT_IMPLEMENTED
    }

    enum class RuleDomain {
        GENERAL_DESIGN,
        LOAD,
        DEMAND,
        CONDUCTOR,
        AMPACITY,
        INSTALLATION_METHOD,
        AMBIENT_CORRECTION,
        GROUPING_CORRECTION,
        VOLTAGE_DROP,
        SHORT_CIRCUIT,
        OVERCURRENT_PROTECTION,
        EARTH_FAULT_PROTECTION,
        GROUNDING,
        BONDING,
        RESIDUAL_CURRENT,
        ISOLATION,
        SWITCHING,
        BREAKER,
        CONTACTOR,
        MOTOR_STARTING,
        TRANSFORMER,
        GENERATOR,
        BUSBAR,
        PANEL,
        DISTRIBUTION_ASSEMBLY,
        EARTHING,
        SURGE_PROTECTION,
        FIRE_RESISTANCE,
        EMERGENCY_SUPPLY,
        ENERGY_EFFICIENCY,
        DOCUMENTATION,
        VERIFICATION
    }

    enum class RuleKind {
        REQUIREMENT,
        DESIGN_LIMIT,
        SELECTION_RULE,
        CALCULATION_METHOD,
        VERIFICATION,
        EQUIPMENT_STANDARD,
        DATA_TABLE,
        REFERENCE_ONLY
    }

    data class CodeReference(
        val standard: Standard,
        val document: String,
        val edition: String,
        val clause: String,
        val title: String,
        val domain: RuleDomain,
        val kind: RuleKind,
        val dataStatus: DataStatus,
        val sourceUrl: String? = null,
        val note: String = ""
    )

    data class StandardProfile(
        val standard: Standard,
        val displayName: String,
        val primaryRevision: String,
        val status: DataStatus,
        val references: List<CodeReference>,
        val engineeringDomains: Set<RuleDomain>,
        val description: String
    )

    /*
     * -----------------------------------------------------------------
     * IEC
     * -----------------------------------------------------------------
     *
     * Current controlled references used by this application.
     */
    private val iecReferences = listOf(

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60364-1",
            edition = "2025",
            clause = "1",
            title =
                "Fundamental principles, assessment of general characteristics and definitions",
            domain = RuleDomain.GENERAL_DESIGN,
            kind = RuleKind.REQUIREMENT,
            dataStatus = DataStatus.VERIFIED,
            sourceUrl = "https://webstore.iec.ch/en/publication/63699"
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60364-4-41",
            edition = "Project-controlled edition",
            clause = "4-41",
            title =
                "Protection against electric shock",
            domain = RuleDomain.GROUNDING,
            kind = RuleKind.REQUIREMENT,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60364-4-43",
            edition = "Project-controlled edition",
            clause = "4-43",
            title =
                "Protection against overcurrent",
            domain = RuleDomain.OVERCURRENT_PROTECTION,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60364-5-52",
            edition = "Project-controlled edition",
            clause = "5-52",
            title =
                "Selection and erection of wiring systems",
            domain = RuleDomain.CONDUCTOR,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60364-5-53",
            edition = "2015 reference / Egyptian adoption available",
            clause = "5-53",
            title =
                "Selection and erection of electrical equipment - isolation, switching and control",
            domain = RuleDomain.SWITCHING,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60364-6",
            edition = "Project-controlled edition",
            clause = "6",
            title =
                "Verification",
            domain = RuleDomain.VERIFICATION,
            kind = RuleKind.VERIFICATION,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60287",
            edition = "Project-controlled edition",
            clause = "Calculation framework",
            title =
                "Electric cables - calculation of current rating",
            domain = RuleDomain.AMPACITY,
            kind = RuleKind.CALCULATION_METHOD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60909",
            edition = "Project-controlled edition",
            clause = "Calculation framework",
            title =
                "Short-circuit currents in three-phase AC systems",
            domain = RuleDomain.SHORT_CIRCUIT,
            kind = RuleKind.CALCULATION_METHOD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60947-2",
            edition = "2024",
            clause = "General",
            title =
                "Circuit-breakers",
            domain = RuleDomain.BREAKER,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.VERIFIED,
            sourceUrl = "https://webstore.iec.ch/en/publication/66277"
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60947-3",
            edition = "Project-controlled edition",
            clause = "General",
            title =
                "Switches, disconnectors, switch-disconnectors and fuse-combination units",
            domain = RuleDomain.ISOLATION,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60947-4-1",
            edition = "Project-controlled edition",
            clause = "General",
            title =
                "Contactors and motor-starters",
            domain = RuleDomain.CONTACTOR,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 61439-1",
            edition = "Project-controlled edition",
            clause = "General",
            title =
                "Low-voltage switchgear and controlgear assemblies - general rules",
            domain = RuleDomain.DISTRIBUTION_ASSEMBLY,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 61439-2",
            edition = "Project-controlled edition",
            clause = "General",
            title =
                "Power switchgear and controlgear assemblies",
            domain = RuleDomain.PANEL,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.IEC,
            document = "IEC 60076",
            edition = "Project-controlled edition",
            clause = "General",
            title =
                "Power transformers",
            domain = RuleDomain.TRANSFORMER,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        )
    )

    /*
     * -----------------------------------------------------------------
     * NEC / NFPA 70
     * -----------------------------------------------------------------
     *
     * Current edition: 2026.
     *
     * Only rule identifiers and references are stored here.
     * NEC copyrighted tables/text are not reproduced.
     */
    private val necReferences = listOf(

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "General",
            title =
                "National Electrical Code",
            domain = RuleDomain.GENERAL_DESIGN,
            kind = RuleKind.REQUIREMENT,
            dataStatus = DataStatus.VERIFIED,
            sourceUrl = "https://www.nfpa.org/codes-and-standards/nfpa-70-development/70"
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 210",
            title =
                "Branch Circuits",
            domain = RuleDomain.LOAD,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 215",
            title =
                "Feeders",
            domain = RuleDomain.LOAD,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 220",
            title =
                "Branch-Circuit, Feeder, and Service Load Calculations",
            domain = RuleDomain.LOAD,
            kind = RuleKind.CALCULATION_METHOD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 240",
            title =
                "Overcurrent Protection",
            domain = RuleDomain.OVERCURRENT_PROTECTION,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 250",
            title =
                "Grounding and Bonding",
            domain = RuleDomain.GROUNDING,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 310",
            title =
                "Conductors for General Wiring",
            domain = RuleDomain.CONDUCTOR,
            kind = RuleKind.DATA_TABLE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 430",
            title =
                "Motors, Motor Circuits, and Controllers",
            domain = RuleDomain.MOTOR_STARTING,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.NEC,
            document = "NFPA 70",
            edition = "2026",
            clause = "Article 450",
            title =
                "Transformers and Transformer Vaults",
            domain = RuleDomain.TRANSFORMER,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        )
    )

    /*
     * -----------------------------------------------------------------
     * EGYPTIAN ELECTRICAL REQUIREMENTS
     * -----------------------------------------------------------------
     *
     * Egypt has both Egyptian specifications and regulatory/utility
     * requirements. They must not be collapsed into one imaginary
     * "Egyptian code table".
     *
     * The registry therefore distinguishes:
     *
     * - Egyptian adopted electrical specifications
     * - Egyptian regulatory requirements
     * - utility connection requirements
     *
     * Exact edition numbers must come from the controlled project
     * document set.
     */
    private val egyptianReferences = listOf(

        CodeReference(
            standard = Standard.EGYPTIAN,
            document = "Egyptian Electrical Code",
            edition = "Controlled project edition required",
            clause = "General",
            title =
                "Egyptian electrical installation requirements",
            domain = RuleDomain.GENERAL_DESIGN,
            kind = RuleKind.REQUIREMENT,
            dataStatus = DataStatus.PARTIALLY_VERIFIED,
            note =
                "Exact controlled edition must be supplied from the project's authorized Egyptian Code document set."
        ),

        CodeReference(
            standard = Standard.EGYPTIAN,
            document = "Egyptian Standard 1576 series",
            edition = "Adopted Egyptian specifications",
            clause = "Series",
            title =
                "Electrical installations of buildings",
            domain = RuleDomain.GENERAL_DESIGN,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.PARTIALLY_VERIFIED,
            note =
                "The Egyptian Organization for Standardization publishes adopted IEC-based Egyptian specifications."
        ),

        CodeReference(
            standard = Standard.EGYPTIAN,
            document = "Egyptian Standard 1576-7",
            edition = "2019",
            clause = "Part 5-53",
            title =
                "Selection and erection of electrical equipment - isolation, switching and control",
            domain = RuleDomain.SWITCHING,
            kind = RuleKind.EQUIPMENT_STANDARD,
            dataStatus = DataStatus.VERIFIED,
            note =
                "Egyptian adopted specification referencing IEC 60364-5-53:2015."
        ),

        CodeReference(
            standard = Standard.EGYPTIAN,
            document = "Egyptian Standard 1576-8",
            edition = "1992 reference / Egyptian adoption",
            clause = "Part 4-473",
            title =
                "Measures of protection against over-current",
            domain = RuleDomain.OVERCURRENT_PROTECTION,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.EGYPTIAN,
            document = "Egyptian Standard 1576-10",
            edition = "2019",
            clause = "Part 5-51",
            title =
                "Selection and erection of electrical equipment - common rules",
            domain = RuleDomain.INSTALLATION_METHOD,
            kind = RuleKind.SELECTION_RULE,
            dataStatus = DataStatus.VERIFIED
        ),

        CodeReference(
            standard = Standard.EGYPTIAN,
            document = "Egyptian Standard 2347",
            edition = "1993",
            clause = "General",
            title =
                "Current-carrying capacities for wiring systems",
            domain = RuleDomain.AMPACITY,
            kind = RuleKind.DATA_TABLE,
            dataStatus = DataStatus.PARTIALLY_VERIFIED
        ),

        CodeReference(
            standard = Standard.EGYPTIAN,
            document = "Electricity Supply Connection Guide",
            edition = "Authority-approved revision",
            clause = "Connection requirements",
            title =
                "Main electrical supply connection rules",
            domain = RuleDomain.GENERAL_DESIGN,
            kind = RuleKind.REQUIREMENT,
            dataStatus = DataStatus.PARTIALLY_VERIFIED,
            note =
                "Utility connection requirements are separate from the technical installation code."
        )
    )

    /*
     * -----------------------------------------------------------------
     * Profiles
     * -----------------------------------------------------------------
     */
    private val profiles: Map<Standard, StandardProfile> = mapOf(

        Standard.IEC to StandardProfile(
            standard = Standard.IEC,
            displayName = "IEC",
            primaryRevision = "IEC 60364-1:2025 / IEC 60947-2:2024",
            status = DataStatus.PARTIALLY_VERIFIED,
            references = iecReferences,
            engineeringDomains =
                iecReferences.map { it.domain }.toSet(),
            description =
                "IEC-based engineering reference profile for LV electrical design."
        ),

        Standard.NEC to StandardProfile(
            standard = Standard.NEC,
            displayName = "NFPA 70 - NEC",
            primaryRevision = "NFPA 70:2026",
            status = DataStatus.PARTIALLY_VERIFIED,
            references = necReferences,
            engineeringDomains =
                necReferences.map { it.domain }.toSet(),
            description =
                "NEC engineering reference profile. Numerical NEC tables require verified licensed source data."
        ),

        Standard.EGYPTIAN to StandardProfile(
            standard = Standard.EGYPTIAN,
            displayName = "Egyptian Electrical Requirements",
            primaryRevision = "Controlled Egyptian project dataset",
            status = DataStatus.PARTIALLY_VERIFIED,
            references = egyptianReferences,
            engineeringDomains =
                egyptianReferences.map { it.domain }.toSet(),
            description =
                "Egyptian electrical requirements combining controlled Egyptian specifications and applicable regulatory references."
        ),

        Standard.CEI to StandardProfile(
            standard = Standard.CEI,
            displayName = "CEI 64-8",
            primaryRevision = "Dedicated controlled dataset required",
            status = DataStatus.NOT_IMPLEMENTED,
            references = emptyList(),
            engineeringDomains = emptySet(),
            description =
                "CEI is retained as an explicit independent standard and must not inherit IEC data silently."
        ),

        Standard.CEC to StandardProfile(
            standard = Standard.CEC,
            displayName = "Canadian Electrical Code",
            primaryRevision = "Dedicated controlled dataset required",
            status = DataStatus.NOT_IMPLEMENTED,
            references = emptyList(),
            engineeringDomains = emptySet(),
            description =
                "CEC is retained as an explicit independent standard and must not inherit NEC or IEC data."
        )
    )

    fun profile(
        standard: Standard
    ): StandardProfile =
        profiles[standard]
            ?: error("No standard profile registered for $standard.")

    fun references(
        standard: Standard
    ): List<CodeReference> =
        profile(standard).references

    fun referencesForDomain(
        standard: Standard,
        domain: RuleDomain
    ): List<CodeReference> =
        references(standard)
            .filter { it.domain == domain }

    fun hasVerifiedReference(
        standard: Standard,
        domain: RuleDomain
    ): Boolean =
        referencesForDomain(standard, domain)
            .any {
                it.dataStatus == DataStatus.VERIFIED
            }

    fun supportedDomains(
        standard: Standard
    ): Set<RuleDomain> =
        profile(standard).engineeringDomains

    fun isProfessionalDatasetReady(
        standard: Standard
    ): Boolean =
        profile(standard).status == DataStatus.VERIFIED

    fun status(
        standard: Standard
    ): DataStatus =
        profile(standard).status

    fun allProfiles(): List<StandardProfile> =
        profiles.values.toList()
}
