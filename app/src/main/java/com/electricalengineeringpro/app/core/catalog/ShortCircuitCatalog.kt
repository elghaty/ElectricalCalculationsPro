package com.electricalengineeringpro.app.core.catalog

import com.electricalengineeringpro.app.core.shortcircuit.CableCatalogData
import com.electricalengineeringpro.app.core.shortcircuit.SourceCatalogData
import com.electricalengineeringpro.app.core.shortcircuit.TransformerCatalogData

interface ShortCircuitCatalog {

    fun source(
        id: String
    ): SourceCatalogData?

    fun transformer(
        id: String
    ): TransformerCatalogData?

    fun cable(
        id: String
    ): CableCatalogData?
}

class InMemoryShortCircuitCatalog(
    private val sources:
        Map<String, SourceCatalogData> = emptyMap(),

    private val transformers:
        Map<String, TransformerCatalogData> = emptyMap(),

    private val cables:
        Map<String, CableCatalogData> = emptyMap()
) : ShortCircuitCatalog {

    override fun source(
        id: String
    ): SourceCatalogData? {
        return sources[id]
    }

    override fun transformer(
        id: String
    ): TransformerCatalogData? {
        return transformers[id]
    }

    override fun cable(
        id: String
    ): CableCatalogData? {
        return cables[id]
    }
}
