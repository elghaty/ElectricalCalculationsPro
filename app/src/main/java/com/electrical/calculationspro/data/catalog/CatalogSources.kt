package com.electrical.calculationspro.data.catalog

object CatalogSources {

    val schneiderCompactNsx = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric Egypt",
        sourceReference = "ComPacT NSX",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/en/product-range/1887-compact-nsx/",
        verificationNote =
            "Official Schneider Electric ComPacT NSX family. " +
                "Exact commercial reference, rated current, Icu/Ics and trip unit " +
                "must be selected for the actual design voltage."
    )

    val schneiderCompactNsxC25B32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric Egypt",
        sourceReference = "C25B32D250",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/en/product/C25B32D250/",
        verificationNote =
            "Exact Schneider Electric commercial reference. " +
                "250 A, 3P, 415 V AC, ComPacT NSX250B, MicroLogic 2.2. " +
                "The product page provides the applicable breaking-capacity options; " +
                "the selected reference must be matched to the required Icu/Ics."
    )

    val schneiderCompactNsxC25N32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25N32D250",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/ar/product/C25N32D250/",
        verificationNote =
            "Exact ComPacT NSX250N commercial reference. " +
                "250 A, 3P, 415 V AC, MicroLogic 2.2. " +
                "Official Schneider data identifies the 50 kA class at 415 V AC."
    )

    val schneiderCompactNsxC25H32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25H32D250",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/en/product/C25H32D250/",
        verificationNote =
            "Exact ComPacT NSX250H commercial reference. " +
                "250 A, 3P, 415 V AC, MicroLogic 2.2. " +
                "Official manufacturer data identifies the 70 kA class at 415 V AC."
    )

    val schneiderEasyPactCvs = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "EasyPact CVS",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/",
        verificationNote =
            "Manufacturer family identified. Exact commercial reference and " +
                "voltage-specific electrical data are required before use as a verified product."
    )

    val abbTmaxXt = CatalogSource(
        manufacturer = Manufacturer.ABB,
        sourceName = "ABB",
        sourceReference = "SACE Tmax XT",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://new.abb.com/low-voltage",
        verificationNote =
            "Official ABB low-voltage product family. " +
                "Exact order number and voltage-specific Icu/Ics data are required."
    )

    val siemensSentron = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "SENTRON MCCB",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.siemens.com/en-us/products/sentron/molded-case-circuit-breakers/",
        verificationNote =
            "Official Siemens SENTRON MCCB family. " +
                "Exact order number is required for verified engineering selection."
    )

    val siemens3vm = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "SENTRON 3VM11103ED220AA0",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://simplicityhub.siemens.com/in/en/products/sentron-item-prod0000076025",
        verificationNote =
            "Exact Siemens commercial reference. " +
                "Electrical breaking-capacity values are voltage dependent and " +
                "must not be generalized to other 3VM variants."
    )

    val legrandDpx = CatalogSource(
        manufacturer = Manufacturer.LEGRAND,
        sourceName = "Legrand",
        sourceReference = "DPX",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.legrand.com/",
        verificationNote =
            "Official manufacturer family reference. " +
                "Exact commercial reference and electrical data are required."
    )

    val lsSusol = CatalogSource(
        manufacturer = Manufacturer.LS_ELECTRIC,
        sourceName = "LS Electric",
        sourceReference = "Susol",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.ls-electric.com/",
        verificationNote =
            "Official manufacturer family reference. " +
                "Exact commercial reference and electrical data are required."
    )

    val elsewedyCable = CatalogSource(
        manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
        sourceName = "Elsewedy Electric",
        sourceReference = "LV Cable Products",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED,
        officialUrl =
            "https://www.elsewedyelectric.com/",
        verificationNote =
            "Manufacturer source only. Exact cable construction, conductor class, " +
                "insulation, installation method, ampacity, voltage drop and short-circuit " +
                "withstand data must be taken from the applicable manufacturer datasheet."
    )

    val nexansCable = CatalogSource(
        manufacturer = Manufacturer.NEXANS,
        sourceName = "Nexans",
        sourceReference = "LV Cable Products",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED,
        officialUrl =
            "https://www.nexans.com/",
        verificationNote =
            "Manufacturer source only. Exact commercial cable reference and technical " +
                "data must be verified before engineering selection."
    )

    val gizaCable = CatalogSource(
        manufacturer = Manufacturer.GIZA_CABLES,
        sourceName = "Giza Cables",
        sourceReference = "LV Cable Products",
        status = ProductFamilyStatus.ENGINEERING_DATA_REQUIRED,
        officialUrl =
            "https://gizacables.com/",
        verificationNote =
            "Manufacturer source only. Exact cable construction and technical data " +
                "must be verified before engineering selection."
    )

    val generic = CatalogSource(
        manufacturer = Manufacturer.GENERIC,
        sourceName = "Engineering Generic",
        sourceReference = "Generic Engineering Record",
        status = ProductFamilyStatus.GENERIC,
        officialUrl = "",
        verificationNote =
            "Generic engineering record. " +
                "Must never be presented as a manufacturer-verified commercial product."
    )
}
