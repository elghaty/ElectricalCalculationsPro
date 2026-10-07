package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.Standard

/**
 * =====================================================================
 * PROFESSIONAL ENGINEERING RULE BOOK
 * =====================================================================
 *
 * Central gateway used by calculation engines to ask:
 *
 * "Which engineering rule governs this calculation?"
 *
 * It does NOT contain copyrighted standard text.
 * It does NOT replace the actual standard.
 * It does NOT fabricate missing values.
 *
 * Its responsibility is:
 *
 *     Calculation
 *          |
 *          v
 *     Rule Domain
 *          |
 *          v
 *     Standard
 *          |
 *          v
 *     Controlled Reference
 *
 * This gives the application a deterministic and auditable
 * calculation architecture.
 * =====================================================================
 */
object EngineeringRuleBook {

    data class RuleContext(
        val standard: Standard,
        val domain: CodeRuleRegistry.RuleDomain,
        val references: List<CodeRuleRegistry.CodeReference>,
        val verified: Boolean,
        val message: String
    )

    fun resolve(
        standard: Standard,
        domain: CodeRuleRegistry.RuleDomain
    ): RuleContext {

        val references =
            CodeRuleRegistry.referencesForDomain(
                standard = standard,
                domain = domain
            )

        if (references.isEmpty()) {
            return RuleContext(
                standard = standard,
                domain = domain,
                references = emptyList(),
                verified = false,
                message =
                    "No controlled engineering reference is registered for " +
                        "$standard / $domain."
            )
        }

        val hasVerified =
            references.any {
                it.dataStatus ==
                    CodeRuleRegistry.DataStatus.VERIFIED
            }

        return RuleContext(
            standard = standard,
            domain = domain,
            references = references,
            verified = hasVerified,
            message =
                if (hasVerified) {
                    "Controlled engineering reference available."
                } else {
                    "Reference exists, but the required numerical/design dataset is not fully verified."
                }
        )
    }

    fun requireReference(
        standard: Standard,
        domain: CodeRuleRegistry.RuleDomain
    ): RuleContext {

        val context =
            resolve(
                standard = standard,
                domain = domain
            )

        require(context.references.isNotEmpty()) {
            context.message
        }

        return context
    }

    fun referenceIds(
        standard: Standard,
        domain: CodeRuleRegistry.RuleDomain
    ): List<String> =
        CodeRuleRegistry
            .referencesForDomain(
                standard = standard,
                domain = domain
            )
            .map {
                buildString {
                    append(it.document)
                    append(" ")
                    append(it.edition)
                    append(" ")
                    append(it.clause)
                }
            }

    fun isDomainAvailable(
        standard: Standard,
        domain: CodeRuleRegistry.RuleDomain
    ): Boolean =
        CodeRuleRegistry
            .referencesForDomain(
                standard = standard,
                domain = domain
            )
            .isNotEmpty()

    fun isNumericalDatasetVerified(
        standard: Standard,
        domain: CodeRuleRegistry.RuleDomain
    ): Boolean =
        CodeRuleRegistry
            .referencesForDomain(
                standard = standard,
                domain = domain
            )
            .any {
                it.dataStatus ==
                    CodeRuleRegistry.DataStatus.VERIFIED
            }

    fun professionalStatus(
        standard: Standard
    ): String {

        val profile =
            CodeRuleRegistry.profile(standard)

        val verifiedDomains =
            profile.references.count {
                it.dataStatus ==
                    CodeRuleRegistry.DataStatus.VERIFIED
            }

        val totalDomains =
            profile.references.size

        return buildString {

            append(profile.displayName)
            append(" | ")
            append(profile.primaryRevision)
            append(" | ")

            when (profile.status) {
                CodeRuleRegistry.DataStatus.VERIFIED ->
                    append("VERIFIED")

                CodeRuleRegistry.DataStatus.PARTIALLY_VERIFIED ->
                    append(
                        "PARTIALLY VERIFIED "
                    )

                CodeRuleRegistry.DataStatus.NOT_AVAILABLE ->
                    append("DATA NOT AVAILABLE")

                CodeRuleRegistry.DataStatus.NOT_IMPLEMENTED ->
                    append("NOT IMPLEMENTED")
            }

            append(" | verified references=")
            append(verifiedDomains)
            append("/")
            append(totalDomains)
        }
    }

    /**
     * Produces a complete audit trail for a calculation domain.
     *
     * This is intended for engineering reports.
     */
    fun auditTrail(
        standard: Standard,
        domain: CodeRuleRegistry.RuleDomain
    ): List<String> {

        return CodeRuleRegistry
            .referencesForDomain(
                standard = standard,
                domain = domain
            )
            .map { reference ->

                buildString {

                    append(reference.document)
                    append(" | ")
                    append(reference.edition)
                    append(" | ")
                    append(reference.clause)
                    append(" | ")
                    append(reference.kind.name)
                    append(" | ")
                    append(reference.dataStatus.name)

                    if (reference.note.isNotBlank()) {
                        append(" | ")
                        append(reference.note)
                    }
                }
            }
    }
}
