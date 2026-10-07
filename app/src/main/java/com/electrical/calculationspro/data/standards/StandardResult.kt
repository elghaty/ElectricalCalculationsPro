package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.Standard

/**
 * =====================================================================
 * PROFESSIONAL STANDARD RESULT METADATA
 * =====================================================================
 *
 * Every professional calculation should be traceable to:
 *
 * Standard
 * Code revision
 * Engineering rule
 * Dataset status
 * Reference
 *
 * This class intentionally contains metadata only.
 * It does not perform calculations.
 * =====================================================================
 */
data class StandardResultMetadata(
    val standard: Standard,
    val codeName: String,
    val codeRevision: String,
    val fullyImplemented: Boolean,
    val implementationStatus: String,

    /**
     * Engineering domains used by the calculation.
     */
    val domains: Set<CodeRuleRegistry.RuleDomain> = emptySet(),

    /**
     * Exact rule/reference identifiers used by the calculation.
     */
    val ruleReferences: List<String> = emptyList(),

    /**
     * Indicates that the result can be presented as a verified
     * code-compliant engineering result.
     *
     * This must remain false when required datasets are incomplete.
     */
    val complianceVerified: Boolean = false
) {

    companion object {

        fun fromEngine(
            engine: StandardEngine,
            domains: Set<CodeRuleRegistry.RuleDomain> = emptySet(),
            ruleReferences: List<String> = emptyList(),
            complianceVerified: Boolean = false
        ): StandardResultMetadata =
            StandardResultMetadata(
                standard = engine.standard,
                codeName = engine.codeName,
                codeRevision = engine.codeRevision,
                fullyImplemented = engine.isFullyImplemented(),
                implementationStatus = engine.implementationStatus(),
                domains = domains,
                ruleReferences = ruleReferences,
                complianceVerified = complianceVerified
            )

        fun fromRegistry(
            standard: Standard
        ): StandardResultMetadata {

            val profile =
                CodeRuleRegistry.profile(standard)

            return StandardResultMetadata(
                standard = standard,
                codeName = profile.displayName,
                codeRevision = profile.primaryRevision,
                fullyImplemented =
                    CodeRuleRegistry.isProfessionalDatasetReady(
                        standard
                    ),
                implementationStatus =
                    profile.status.name,
                domains =
                    profile.engineeringDomains,
                ruleReferences =
                    profile.references.map {
                        "${it.document} ${it.edition} ${it.clause}"
                    },
                complianceVerified =
                    CodeRuleRegistry.isProfessionalDatasetReady(
                        standard
                    )
            )
        }
    }
}
