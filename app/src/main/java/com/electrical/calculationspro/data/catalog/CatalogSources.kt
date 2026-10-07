package com.electrical.calculationspro.data.catalog

object CatalogSources {

    val schneiderCompactNsxC25B32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25B32D250",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl = "https://www.se.com/eg/en/product/C25B32D250/",
        verificationNote =
            "Exact commercial reference. ComPacT NSX250B, 3P, 250 A, " +
                "415 V AC, 25 kA class at 415 V AC."
    )

    val schneiderCompactNsxC25N32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25N32D250",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl = "https://www.se.com/eg/ar/product/C25N32D250/",
        verificationNote =
            "Exact commercial reference. ComPacT NSX250N, 3P, 250 A, " +
                "415 V AC, 50 kA class at 415 V AC."
    )

    val schneiderCompactNsxC25H32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25H32D250",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl = "https://www.se.com/eg/en/product/C25H32D250/",
        verificationNote =
            "Exact commercial reference. ComPacT NSX250H, 3P, 250 A, " +
                "415 V AC, 70 kA class at 415 V AC."
    )

    val siemens3vm11103ed220aa0 = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "3VM11103ED220AA0",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl = "",
        verificationNote =
            "Exact Siemens reference retained as a controlled catalogue record. " +
                "Voltage-specific breaking-capacity data must be verified from the current manufacturer documentation before selection."
    )

    val schneiderLc1d25d7 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "LC1D25D7",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl =
            "https://www.se.com/eg/ar/product/LC1D25D7/tesys-d-contactor-3p3-no-ac3-440-v-25-a-42-v-ac-coil/",
        verificationNote =
            "Exact TeSys D commercial reference. 3P, AC-3, 25 A, up to 440 V AC."
    )

    val siemens3rt2026_1ap00 = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens Industry Mall",
        sourceReference = "3RT2026-1AP00",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl =
            "https://mall.industry.siemens.com/mall/en/WW/Catalog/Product/3RT2026-1AP00",
        verificationNote =
            "Exact SIRIUS commercial reference. Current manufacturer documentation must be used for final application verification."
    )

    val elsewedyN2xhJ3x15 = CatalogSource(
        manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
        sourceName = "Elsewedy Electric",
        sourceReference = "40035652 / N2XH-J 3 x 1.5 RM",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl =
            "https://elsewedyelectric.com/pdf/Certificates/12212862/40035652-n2xh-j-3x15.pdf",
        verificationNote =
            "Manufacturer certificate identifies N2XH-J 3 x 1.5 RM, Uo/U 0.6/1 kV."
    )

    val elsewedyNhxmh3x15 = CatalogSource(
        manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
        sourceName = "Elsewedy Electric / VDE",
        sourceReference = "40036599 / NHXMH 3 x 1.5 mm²",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl =
            "https://elsewedyelectric.com/pdf/Certificates/03421178/40036599-nhxmh-3-x-1-5-mm.pdf",
        verificationNote =
            "VDE certificate identifies NHXMH 3 x 1.5 mm², Uo/U 300/500 V."
    )

    val schneiderLinergyBw630 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "Linergy BW 630 A",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/uk/en/download/document/04696112/",
        verificationNote =
            "Official Linergy BW documentation. Exact commercial assembly and short-circuit withstand remain configuration-dependent."
    )

    val schneiderPrismaSetG630 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "PrismaSeT G",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/en/product-subcategory/23877549-panel-building-systems/",
        verificationNote =
            "Panel-building system associated with IEC 61439-1/-2. Exact assembly configuration must be engineered."
    )

    val schneiderPrismaSetP4000 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "PrismaSeT P",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/ar/es/product-subcategory/23877549-panel-building-systems/",
        verificationNote =
            "Panel-building system associated with IEC 61439-1/-2. Exact assembly configuration must be engineered."
    )

    val schneiderCompactNsxFamily = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "ComPacT NSX",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/en/download/document/LVPED221001EN/",
        verificationNote =
            "Official ComPacT NSX documentation. Exact commercial reference is required for final selection."
    )

    val abbTmaxXt = CatalogSource(
        manufacturer = Manufacturer.ABB,
        sourceName = "ABB",
        sourceReference = "SACE Tmax XT",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://new.abb.com/low-voltage/products/circuit-breakers/tmax-xt",
        verificationNote =
            "Manufacturer family reference. Exact order code and electrical configuration are required."
    )

    val siemensSentronFamily = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "SENTRON 3VA",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.siemens.com/global/en/products/energy/low-voltage/components/sentron-protection-switching-measuring/3va-molded-case-circuit-breakers.html",
        verificationNote =
            "Manufacturer family reference. Exact order number and voltage-specific ratings are required."
    )

    val legrandDpx = CatalogSource(
        manufacturer = Manufacturer.LEGRAND,
        sourceName = "Legrand",
        sourceReference = "DPX",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.legrand.com/",
        verificationNote =
            "Manufacturer family reference. Exact commercial reference is required."
    )

    val lsSusol = CatalogSource(
        manufacturer = Manufacturer.LS_ELECTRIC,
        sourceName = "LS ELECTRIC",
        sourceReference = "Susol",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.ls-electric.com/",
        verificationNote =
            "Manufacturer family reference. Exact commercial reference is required."
    )

    val elsewedyCable = CatalogSource(
        manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
        sourceName = "Elsewedy Electric",
        sourceReference = "LV Cable Catalogue",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://elsewedyelectric.com/",
        verificationNote =
            "Manufacturer catalogue family. Exact product reference and datasheet are required for final cable selection."
    )

    val nexansCable = CatalogSource(
        manufacturer = Manufacturer.NEXANS,
        sourceName = "Nexans",
        sourceReference = "LV Cable Catalogue",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.nexans.com/",
        verificationNote =
            "Manufacturer catalogue family. Exact regional product reference and datasheet are required."
    )

    val gizaCable = CatalogSource(
        manufacturer = Manufacturer.GIZA_CABLES,
        sourceName = "Giza Cables",
        sourceReference = "LV Cable Catalogue",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://gizacables.com/",
        verificationNote =
            "Manufacturer catalogue family. Exact product reference and datasheet are required."
    )

    val generic = CatalogSource(
        manufacturer = Manufacturer.GENERIC,
        sourceName = "Engineering Generic",
        sourceReference = "NO COMMERCIAL PRODUCT",
        status = ProductFamilyStatus.GENERIC,
        officialUrl = "",
        verificationNote =
            "Engineering placeholder only. It must not be represented as a manufacturer product."
    )
}
