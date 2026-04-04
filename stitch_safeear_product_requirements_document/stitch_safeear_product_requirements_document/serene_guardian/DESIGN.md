# Teal Sentinel Design System

### 1. Overview & Creative North Star
**Creative North Star: The Vigilant Sanctuary**
Teal Sentinel is a design system built for high-stakes domestic security and acoustic monitoring. It transcends traditional dashboard aesthetics by blending the clinical precision of medical monitoring with the warmth of a high-end editorial publication. The system prioritizes "calm urgency"—using significant typographic scale to communicate status instantly, while maintaining a serene, spacious environment that doesn't overwhelm the user.

Asymmetry and "Bento-style" layouts are used to create a hierarchical flow that feels intentional and curated rather than mechanical. It rejects the standard 12-column grid in favor of weighted content blocks that emphasize priority through size and color saturation.

### 2. Colors
The palette is rooted in deep, reassuring teals and crisp, cool neutrals.
- **The "No-Line" Rule:** Visual separation is strictly achieved through shifts in surface containers (e.g., a `surface_container_lowest` card on a `surface` background). Explicit 1px borders are prohibited except for high-priority alert states where they must be semi-transparent.
- **Surface Hierarchy:** 
  - `Background`: The base canvas.
  - `Surface-Container-Lowest`: The primary card level for white-space heavy sections.
  - `Surface-Container`: Used for grouped history or secondary utility areas.
- **The Glass & Gradient Rule:** Navigation bars and headers must use an 80% opacity blur (`backdrop-blur-xl`) to maintain context of the content beneath. Use linear gradients (Primary to Primary-Container) for high-impact performance metrics.

### 3. Typography
The system employs a sophisticated dual-font strategy. **Manrope** provides a bold, geometric authority for headlines, while **Inter** ensures maximum readability for data and labels.

**Typography Scale:**
- **Display (Active Status):** 3.75rem (60px) / Bold. Used for critical single-word status.
- **Headline (Bento Metrics):** 3rem (48px) / Extrabold. Used for primary numerical data.
- **Title (Section):** 1.25rem (20px) / Bold. Used for card and section titles.
- **Body:** 0.875rem (14px) / Regular. Optimized for descriptions and contextual help.
- **Label/Nav:** 0.625rem (10px) / Bold / Uppercase / Wide Tracking. Used for navigation and metadata.

### 4. Elevation & Depth
Depth is created through "Tonal Stacking" rather than elevation.
- **The Layering Principle:** Instead of shadows, use `surface_container_low` to indicate an interactive "well" or `surface_container_lowest` to indicate a "raised" interactive card.
- **Ambient Shadows:** For primary cards, use a surgical shadow: `0 8px 24px rgba(0,0,0,0.02)`. It should be almost imperceptible.
- **Interactive Pulse:** Critical active states use a dynamic shadow: `0 0 0 10px rgba(0, 70, 80, 0)` to indicate life and activity without structural clutter.

### 5. Components
- **Buttons:** Large, high-contrast blocks. Primary buttons use `primary` fill with `on_primary` text, featuring a 12px (`xl`) corner radius.
- **Status Badges:** Pill-shaped, using `tertiary_fixed` for positive states. They must include a "Pulse" animation element for live statuses.
- **Progress Bars:** Minimalist 8px tracks using `surface_container_high` as the background and `primary` as the fill.
- **Navigation:** A floating bottom bar with "Squircle" active states (16px radius) and iconic representation with micro-labels.
- **Avatar Stacks:** Overlapping circles with a 2px `surface_container_lowest` ring to create separation without line-work.

### 6. Do's and Don'ts
**Do:**
- Use wide tracking and uppercase for labels to create an editorial feel.
- Use extreme contrast in font sizes (e.g., 60px next to 14px) to define hierarchy.
- Utilize backdrop blurs on fixed elements.

**Don't:**
- Use solid black for text; use `on_surface` or `on_surface_variant` for a softer, premium look.
- Add borders to cards unless they are in an `error` state.
- Crowd elements; if in doubt, increase the `spacing` to the maximum (Level 3).