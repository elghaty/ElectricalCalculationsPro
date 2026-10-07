package com.electrical.calculationspro.data.catalog

object CatalogSources {

    val schneiderCompactNsx = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric Egypt",
        sourceReference = "ComPact NSX",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl = "https://www.se.com/eg/en/product/C25B32D250/",
        verificationNote =
            "Official Schneider Electric product-family/product reference. " +
                "Exact Icu/Ics must be selected for the exact product reference and voltage."
    )

    val schneiderEasyPactCvs = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "EasyPact CVS",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl = "https://www.se.com/",
        verificationNote =
            "Official family reference. Exact commercial reference and electrical data are required."
    )

    val abbTmaxXt = CatalogSource(
        manufacturer = Manufacturer.ABB,
        sourceName = "ABB",
        sourceReference = "SACE Tmax XT",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl = "https://new.abb.com/low-voltage",
        verificationNote =
            "Official ABB low-voltage product-family reference. Exact order number and rating data are required."
    )

    val siemensSentron = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "SENTRON",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.siemens.com/en-gb/products/sentron/molded-case-circuit-breakers/",
        verificationNote =
            "Official Siemens SENTRON family reference. Exact order number is required for verified selection."
    )

    val siemens3vm = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "SENTRON 3VM11103ED220AA0",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://simplicityhub.siemens.com/in/en/products/sentron-item-prod0000076025",
        verificationNote =
            "Exact Siemens product reference. Icu/Ics values are voltage dependent."
    )

    val legrandDpx = CatalogSource(
        manufacturer = Manufacturer.LEGRAND,
        sourceName = "Legrand",
        sourceReference = "DPX / DMX",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl = "https://www.legrand.com/",
        verificationNote =
            "Official manufacturer family reference. Exact product reference is required."
    )

    val lsSusol = CatalogSource(
        manufacturer = Manufacturer.LS_ELECTRIC,
        sourceName = "LS Electric",
        sourceReference = "Susol",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl = "https://www.ls-electric.com/",
        verificationNote =
            "Official manufacturer family reference. Exact product reference is required."
    )

    val elsewedyCable = CatalogSource(
        manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
        sourceName = "Elsewedy Electric",
        sourceReference = "LV cable product families",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED,
        officialUrl = "https://www.elsewedyelectric.com/",
        verificationNote =
            "Manufacturer family reference only. Exact cable construction, catalogue reference, " +
                "ampacity and installation data must be verified."
    )

    val nexansCable = CatalogSource(
        manufacturer = Manufacturer.NEXANS,
        sourceName = "Nexans",
        sourceReference = "LV cable product families",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED,
        officialUrl = "https://www.nexans.com/",
        verificationNote =
            "Manufacturer family reference only. Exact product reference and technical data required."
    )

    val gizaCable = CatalogSource(
        manufacturer = Manufacturer.GIZA_CABLES,
        sourceName = "Giza Cables",
        sourceReference = "LV cable product families",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED,
        officialUrl = "https://gizacables.com/",
        verificationNote =
            "Manufacturer family reference only. Exact catalogue data must be verified."
    )

    val generic = CatalogSource(
        manufacturer = Manufacturer.GENERIC,
        sourceName = "Engineering generic",
        sourceReference = "Generic engineering placeholder",
        status = ProductFamilyStatus.GENERIC,
        verificationNote =
            "Generic engineering record. Never present as a manufacturer-verified commercial product."
    )
}
