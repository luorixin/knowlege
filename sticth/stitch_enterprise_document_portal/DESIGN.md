---
name: Sapphire Enterprise
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434655'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#525657'
  on-tertiary: '#ffffff'
  tertiary-container: '#6b6e70'
  on-tertiary-container: '#eff1f3'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#e0e3e5'
  tertiary-fixed-dim: '#c4c7c9'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#444749'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  container-max: 1280px
  gutter: 24px
  margin-mobile: 16px
---

## Brand & Style

This design system targets high-stakes enterprise environments where clarity, speed, and precision are paramount. The aesthetic merges the refined minimalism of Apple with the systematic utility of high-performance tools like Linear. 

The personality is authoritative yet unobtrusive, prioritizing content (documents) over chrome. The emotional response is one of calm confidence—achieved through generous whitespace, a strict "less is more" approach to color, and a tactile sense of depth provided by subtle glassmorphism and layered shadows. It is designed to feel like a premium physical workspace translated into a digital interface.

## Colors

The palette is anchored in a sophisticated range of slates and sapphire. 

- **Primary (#2563eb):** Used sparingly for high-intent actions, progress indicators, and active states. It provides a sharp focal point against the muted background.
- **Surface & Backgrounds:** The main background uses a pale slate-white (#f8fafc). Elevated surfaces use pure white (#ffffff) with varying levels of opacity to enable glassmorphic effects.
- **Grays:** A scale of cool slates is used for secondary text (#64748b), borders (#e2e8f0), and subtle UI decorators.
- **Accents:** Success, warning, and error states should utilize desaturated versions of green, amber, and red to maintain the professional, low-fatigue atmosphere.

## Typography

This design system uses **Inter** exclusively to ensure a systematic and utilitarian feel. The hierarchy is established through weight and subtle letter-spacing adjustments rather than extreme size variances.

- **Headlines:** Use Semi-Bold (600) weights with negative letter-spacing to create a "tight," premium editorial look.
- **Body:** Standard reading text should remain at 14px or 16px for optimal legibility in data-dense dashboards.
- **Labels:** Small labels and metadata should use Medium (500) or Semi-Bold (600) weights to ensure they remain legible even at smaller scales.
- **Functionality:** Use tabular lining for any numeric data (file sizes, dates, upload percentages) to ensure vertical alignment in lists.

## Layout & Spacing

The layout follows a strict 8px grid system to ensure alignment and rhythmic consistency. 

- **Dashboard Layout:** Use a fixed-width sidebar (240px-280px) with a fluid content area. On extra-wide screens, the content area should be capped at 1280px and centered.
- **Margins:** Standard page padding is 32px on desktop and 16px on mobile. 
- **Grids:** For the document gallery view, use a CSS Grid with a `repeat(auto-fill, minmax(280px, 1fr))` pattern to maintain card integrity.
- **Reflow:** On mobile, sidebars should collapse into a bottom navigation bar or a hidden drawer, and cards should stack vertically with full width.

## Elevation & Depth

Depth is used to signify interactivity and priority. This design system employs a "Layered Lightness" model:

- **Level 0 (Background):** #f8fafc. The canvas.
- **Level 1 (Cards/Panels):** White at 70% opacity with a 20px Backdrop Blur. This creates a soft "glass" effect that lets the background tint show through subtly.
- **Level 2 (Modals/Popovers):** Pure white with a multi-layered shadow (e.g., `0 10px 15px -3px rgba(0,0,0,0.05), 0 4px 6px -2px rgba(0,0,0,0.02)`).
- **Outlines:** Use a 1px solid border (#e2e8f0) even on glassmorphic elements to define edges against white backgrounds.

## Shapes

The shape language is friendly yet structured, using generous radii to soften the enterprise-heavy data.

- **Cards & Containers:** Use `rounded-2xl` (1.5rem / 24px) to create a soft, modern container feel.
- **Input Fields & Buttons:** Use `rounded-lg` (0.5rem / 8px) to maintain a professional, clickable appearance.
- **Small Elements:** Tooltips and tags use `rounded-md` (0.375rem / 6px).
- **Icons:** Icons should utilize the "Rounded" style from Material Symbols to match the UI's corner radius.

## Components

### Buttons
- **Primary:** Solid Sapphire Blue (#2563eb) with white text. No shadow, or a very slight inset shadow on press.
- **Secondary:** Transparent background with a 1px border (#e2e8f0) and Slate text. 
- **Tertiary/Ghost:** No border or background. Sapphire Blue text for actions, Slate for navigation.

### Input Fields
- **Default:** White background, 1px border (#e2e8f0).
- **Focus State:** 1px Sapphire Blue border with a 3px soft outer glow (Blue at 10% opacity).
- **Upload Zone:** `rounded-2xl` with a dashed border (#cbd5e1), centered icon, and light sapphire tint on drag-over.

### Cards
- White background (70% opacity), 20px backdrop blur, 1px border (#e2e8f0), and a `rounded-2xl` corner radius. 
- Hover state: Slight lift using a deeper layered shadow and a subtle increase in border contrast.

### Chips & Tags
- Used for document status (e.g., "Pending," "Verified"). Use small font-size (12px), Semi-Bold, and low-saturation background tints.

### Lists
- For document tables, use 64px row heights. Use subtle horizontal dividers (#f1f5f9) and ensure "hover" states for rows change the background to a faint sapphire-white tint.

### Icons
- Material Symbols (Rounded). Use a 20px or 24px optical size with a 'Light' or 'Regular' weight (300-400) to keep the interface airy.