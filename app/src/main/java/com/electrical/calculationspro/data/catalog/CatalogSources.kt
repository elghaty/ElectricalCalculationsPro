package com.electrical.calculationspro.data.catalog

object CatalogSources {

    val schneiderCompactNsxC25B32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25B32D250",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl = "https://www.se.com/eg/en/product/C25B32D250/",
        verificationNote =
            "Exact commercial reference. ComPacT NSX250B, 3P, 250 A, 415 V AC, " +
                "25 kA class at 415 V AC. Final selection remains voltage and configuration dependent."
    )

    val schneiderCompactNsxC25N32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25N32D250",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl = "https://www.se.com/eg/ar/product/C25N32D250/",
        verificationNote =
            "Exact commercial reference. ComPacT NSX250N, 3P, 250 A, 415 V AC, " +
                "50 kA class at 415 V AC."
    )

    val schneiderCompactNsxC25H32D250 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "C25H32D250",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl = "https://www.se.com/eg/en/product/C25H32D250/",
        verificationNote =
            "Exact commercial reference. ComPacT NSX250H, 3P, 250 A, 415 V AC, " +
                "70 kA class at 415 V AC."
    )

    val siemens3vm11103ed220aa0 = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "3VM11103ED220AA0",
        status = ProductFamilyStatus.VERIFIED_PRODUCT,
        officialUrl =
            "https://sieportal.siemens.com/en-ww/products-services/detail/3VA5195-6EF31-0AA0",
        verificationNote =
            "Exact Siemens SENTRON commercial reference already controlled in the project. " +
                "Electrical breaking-capacity data must remain voltage-specific."
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
            "Exact SIRIUS 3RT commercial reference. AC-3e/AC-3, 25 A, 11 kW at 400 V, " +
                "3-pole, 230 V AC coil, 50 Hz."
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
            "Official Linergy BW insulated busbar documentation identifies the 630 A range. " +
                "Exact commercial assembly/reference and short-circuit withstand data must be verified."
    )

    val schneiderPrismaSetG630 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "PrismaSeT G",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/en/product-subcategory/23877549-panel-building-systems/",
        verificationNote =
            "PrismaSeT G panel-building system up to 630 A, designed around IEC 61439-1/-2."
    )

    val schneiderPrismaSetP4000 = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "PrismaSeT P",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/ar/es/product-subcategory/23877549-panel-building-systems/",
        verificationNote =
            "PrismaSeT P panel-building system up to 4000 A under IEC 61439-1/-2. " +
                "Exact assembly configuration must be engineered and verified."
    )

    val schneiderCompactNsxFamily = CatalogSource(
        manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
        sourceName = "Schneider Electric",
        sourceReference = "ComPacT NSX",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.se.com/eg/en/download/document/LVPED221001EN/",
        verificationNote =
            "Official 2026 ComPacT NSX/NSXm catalog. Exact commercial reference required."
    )

    val siemensSentronFamily = CatalogSource(
        manufacturer = Manufacturer.SIEMENS,
        sourceName = "Siemens",
        sourceReference = "SENTRON 3VA",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://sieportal.siemens.com/en-ww/products-services/detail/3VA5195-6EF31-0AA0",
        verificationNote =
            "Official Siemens SENTRON documentation. Exact order number required."
    )

    val nexansLvCatalog = CatalogSource(
        manufacturer = Manufacturer.NEXANS,
        sourceName = "Nexans",
        sourceReference = "Nexans LV Product Catalogue",
        status = ProductFamilyStatus.VERIFIED_FAMILY,
        officialUrl =
            "https://www.nexans.com/activities/nexans-catalog/",
        verificationNote =
            "Official Nexans catalogue. Exact regional product reference and datasheet required."
    )

    val generic = CatalogSource(
        manufacturer = Manufacturer.GENERIC,
        sourceName = "Engineering Generic",
        sourceReference = "NO COMMERCIAL PRODUCT",
        status = ProductFamilyStatus.GENERIC,
        officialUrl = "",
        verificationNote =
            "Engineering placeholder only. Must not be selected as a manufacturer product."
    )
}
