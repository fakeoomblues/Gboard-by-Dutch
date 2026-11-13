package patches.lightcraft

import patches.shared.ARTCRAFT_ORIGINAL_ICON
import patches.shared.ArtCraftBrand
import patches.shared.artCraftBrandingPatch

internal val lightCraftBrand = ArtCraftBrand(
    appName = "LightCraft",
    packageName = "io.github.artcraftandroid.lightcraft",
    slug = "lightcraft",
    brandName = "Adobe Lightroom",
    iconStyles = linkedMapOf(
        "Original LightCraft" to ARTCRAFT_ORIGINAL_ICON,
        "Adobe Lightroom (default)" to "lightroom",
    ),
    defaultIcon = "lightroom",
    description = "Renames LightCraft to Adobe Lightroom and gives it the Adobe Lightroom launcher icon. " +
        "Choose another name or LightCraft's original icon.",
)

@Suppress("unused")
val customBrandingPatch = artCraftBrandingPatch(lightCraftBrand)
