# Pebble GitHub social preview

## Deliverables

- Upload-ready PNG: [social-preview-github.png](../../media/social-preview-github.png)
- Portable artwork source: [social-preview-github.svg](../../media/social-preview-github.svg)
- Background master: [sand-jade-backdrop.png](sand-jade-backdrop.png)
- Layout measurements: [layout.json](layout.json)

## Design

The existing Tidal Lens / Tumbled Jade mark sits beside Pebble's existing Plus Jakarta Sans lettering, on warm sand and diffuse jade light. The exact approved logo raster is embedded unchanged in the SVG. Text is outlined, so rendering does not depend on installed fonts. The background is original artwork made with the built-in image-generation tool; CLI/API fallback was not used.

Copy: “Pebble”, “The web, from your sofa.”, and “A browser for Android TV”.

## GitHub/template compliance

The final PNG is 1280 × 640 pixels (2:1), opaque sRGB, and 592,901 bytes (about 579 KiB), below GitHub's 1 MB limit. [GitHub image requirements](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/customizing-your-repositorys-social-media-preview).

The supplied template's inner guides lie near x78–1202 and y78–562. All important content is inside a stricter 84-pixel margin; measured bounds are in layout.json. Background light/texture is intentionally full bleed. The finished card has no template guide lines.

A separate design-review agent visually checked both the 1280 × 640 card and a 640 × 320 thumbnail: brand fidelity, typography, category recognition, safe area and material finish passed. No app build or TV test is needed for this documentation artwork.

## Background prompt (verbatim)

Use case: ads-marketing. Asset type: premium brand social-card BACKGROUND ONLY, intended to sit behind exact existing Pebble logo and typography which will be composed separately. Create a landscape image with exact 2:1 aspect ratio, ideally 1280x640. Full-bleed warm sand / ivory paper surface, color near #F1E8D5, with extremely subtle fine mineral-paper and sandy grain, calm and minimal. On the RIGHT half only, a soft diffuse translucent sea-glass jade light bloom near #C1DDD0 and a restrained natural frosted-glass caustic, elegant broad organic shading that smoothly disappears into the sand. The left 60% stays very quiet, flat, light and uncluttered for dark typography. Sea-glass / sea-prism material inspiration, tactile, refined, slightly dimensional light, photographed editorial material study. No actual pebbles or foreground objects. No logo, no lettering, no text, no frames, no mockups, no UI, no borders, no watermarks, no hard gradient bands, no saturated blue, no purple, no sharp bright hotspots. Keep all atmosphere low contrast, especially near the perimeter. This is a subtle background plate, not the finished card.

## Upload

On the repository, choose Settings → General → Social preview → Edit → Upload an image, then select the PNG. This deliverable does not change GitHub's repository settings automatically.

