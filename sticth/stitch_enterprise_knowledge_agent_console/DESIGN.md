---
name: Knowledge Agent Console
colors:
  surface: '#faf8ff'
  surface-dim: '#d8d9e6'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#ecedfa'
  surface-container-high: '#e6e7f4'
  surface-container-highest: '#e1e2ee'
  on-surface: '#191b24'
  on-surface-variant: '#424656'
  inverse-surface: '#2e303a'
  inverse-on-surface: '#eff0fd'
  outline: '#727687'
  outline-variant: '#c2c6d8'
  surface-tint: '#0054d6'
  primary: '#0050cb'
  on-primary: '#ffffff'
  primary-container: '#0066ff'
  on-primary-container: '#f8f7ff'
  inverse-primary: '#b3c5ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#a33200'
  on-tertiary: '#ffffff'
  tertiary-container: '#cc4204'
  on-tertiary-container: '#fff6f4'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae1ff'
  primary-fixed-dim: '#b3c5ff'
  on-primary-fixed: '#001849'
  on-primary-fixed-variant: '#003fa4'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#ffdbd0'
  tertiary-fixed-dim: '#ffb59d'
  on-tertiary-fixed: '#390c00'
  on-tertiary-fixed-variant: '#832600'
  background: '#faf8ff'
  on-background: '#191b24'
  surface-variant: '#e1e2ee'
typography:
  display:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: '1.4'
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: 0em
  label-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: '1.4'
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.03em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: '1.3'
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
  xl: 40px
  sidebar_width: 260px
  container_max_width: 1200px
---

## Brand & Style
The design system for the Knowledge Agent Console centers on **Modern Enterprise Minimalism**. It is designed to evoke a sense of focused intelligence, precision, and executive-grade reliability. The target audience consists of knowledge managers and AI operators who require a tool that feels as powerful as a terminal but as intuitive and refined as a high-end document editor.

The visual style leverages:
- **Functional Clarity:** Heavy use of negative space to reduce cognitive load during complex AI workflows.
- **Glassmorphic Accents:** Strategic use of translucency in persistent navigational elements to maintain context of the underlying data.
- **Subtle Technicality:** A blend of soft UI patterns (Linear-inspired) with rigorous information density.

## Colors
This design system utilizes a sophisticated, low-vibrancy palette to ensure long-term usability without eye strain.

- **Primary:** A vibrant, high-contrast blue (#0066FF) reserved exclusively for primary actions, active selection states, and focus indicators.
- **Background:** A cool, pale slate (#F7F9FB) serves as the canvas, providing a distinct separation from white content cards.
- **Surface:** Pure white (#FFFFFF) is used for the "work area" and floating cards to indicate the highest level of hierarchy.
- **Neutral/Slate:** A range of slate grays are used for text and iconography to maintain a professional, monochromatic foundation.

## Typography
Inter is the sole typeface, utilized for its exceptional legibility in data-dense environments. 

- **Hierarchy through Weight:** Use SemiBold (600) for headlines and Medium (500) for interactive labels. Regular (400) is reserved for body copy to ensure a clean, airy feel.
- **Micro-copy:** Small labels (11px) use uppercase with increased letter spacing to distinguish metadata from content.
- **Tight Kerning:** Negative letter spacing is applied to larger headlines to achieve the "premium" condensed look found in modern SaaS tools.

## Layout & Spacing
The layout follows a **structured grid** with flexible content zones. 

- **Sidebar:** A fixed-width (260px) left navigation using a glassmorphic blur to overlay the background.
- **Content Area:** A centered, fixed-width container for reading and editing, while dashboard views utilize a 12-column fluid grid.
- **Rhythm:** An 8px linear scale (4, 8, 16, 24, 40) governs all padding and margins, ensuring vertical rhythm across modular components.
- **Mobile Adaptivity:** Sidebars collapse into a bottom sheet or a full-screen overlay; horizontal padding reduces from 40px (Desktop) to 16px (Mobile).

## Elevation & Depth
This design system uses **Tonal Layering** and **Soft Shadows** rather than heavy skeuomorphism.

- **Level 0 (Background):** #F7F9FB.
- **Level 1 (Sidebar/Secondary Panels):** Semi-transparent white with a 20px backdrop blur and a 1px inner border (#E2E8F0) to define edges.
- **Level 2 (Cards/Main Surface):** Solid white with a very soft, diffused shadow (0px 4px 12px rgba(0,0,0,0.03)).
- **Level 3 (Modals/Popovers):** Solid white with a more pronounced shadow (0px 12px 32px rgba(0,0,0,0.08)) to indicate significant distance from the base plane.

## Shapes
The shape language is modern and approachable. 

- **Default Radius:** 0.5rem (8px) for standard inputs and buttons.
- **Large Radius (XL):** 1.5rem (24px) for active state indicators, pill-shaped chips, and main container wrappers.
- **Strictness:** Borders should always be 1px wide, using a subtle slate-200 (#E2E8F0) to maintain the clean aesthetic without adding visual weight.

## Components
- **Buttons:** Primary buttons use a solid blue background with white text. Secondary buttons use a white background with a subtle border and ghost-like hover effect (light gray fill).
- **Active Items:** Selected navigation items or active list rows should use a `rounded-xl` (1.5rem) shape with a very light blue tint or a 2px left-side accent bar.
- **Input Fields:** Minimalist design with a 1px border. On focus, the border transitions to the primary blue with a subtle glow (2px blue shadow at 10% opacity).
- **Chips:** Small, pill-shaped indicators with high-contrast text. Use for "AI Status" or "Knowledge Tags."
- **Glassmorphic Sidebar:** Uses a background of `rgba(255, 255, 255, 0.7)` with `backdrop-filter: blur(20px)`.
- **Knowledge Cards:** White surfaces with a 1px border. Hovering over a card should slightly deepen the shadow and shift the border color to a slightly darker slate.