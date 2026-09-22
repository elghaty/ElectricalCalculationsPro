package com.electrical.calculationspro.data.catalog

object CatalogSources {

    val schneiderCompactNsx = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric Egypt",
        sourceReference = "ComPact NSX product range",
        status = ProductFamilyStatus.VERIFIED_FAMILY
    )

    val schneiderEasyPactCvs = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric Egypt",
        sourceReference = "EasyPact CVS product range",
        status = ProductFamilyStatus.VERIFIED_FAMILY
    )

    val abbTmaxXt = CatalogSource(
        manufacturer = Manufacturer.ABB,
        sourceName = "ABB eCatalog",
        sourceReference = "SACE Tmax XT",
        status = ProductFamilyStatus.VERIFIED_FAMILY
    )

    val siemensSentron = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens SENTRON",
        sourceReference = "SENTRON low-voltage protection family",
        status = ProductFamilyStatus.VERIFIED_FAMILY
    )

    val legrandDpx = CatalogSource(
        manufacturer = Manufacturer.LEGRAND,
        sourceName = "Legrand",
        sourceReference = "DPX / DMX low-voltage protection families",
        status = ProductFamilyStatus.VERIFIED_FAMILY
    )

    val lsSusol = CatalogSource(
        manufacturer = Manufacturer.LS_ELECTRIC,
        sourceName = "LS Electric",
        sourceReference = "Susol low-voltage circuit breaker family",
        status = ProductFamilyStatus.VERIFIED_FAMILY
    )

    val elsewedyCable = CatalogSource(
        manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
        sourceName = "Elsewedy Electric",
        sourceReference = "LV cable product family",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED
    )

    val nexansCable = CatalogSource(
        manufacturer = Manufacturer.NEXANS,
        sourceName = "Nexans",
        sourceReference = "LV cable product families",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED
    )

    val gizaCable = CatalogSource(
        manufacturer = Manufacturer.GIZA_CABLES,
        sourceName = "Giza Cables",
        sourceReference = "LV cable product families",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED
    )

    val generic = CatalogSource(
        manufacturer = Manufacturer.GENERIC,
        sourceName = "Engineering generic",
        sourceReference = "No manufacturer catalog",
        status = ProductFamilyStatus.GENERIC
    )
}
