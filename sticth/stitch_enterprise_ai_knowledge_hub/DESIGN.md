---
name: Lumina Enterprise
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#424754'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#727785'
  outline-variant: '#c2c6d6'
  surface-tint: '#005ac2'
  primary: '#0058be'
  on-primary: '#ffffff'
  primary-container: '#2170e4'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#545c72'
  on-tertiary: '#ffffff'
  tertiary-container: '#6c748b'
  on-tertiary-container: '#fefcff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#dae2fd'
  tertiary-fixed-dim: '#bec6e0'
  on-tertiary-fixed: '#131b2e'
  on-tertiary-fixed-variant: '#3f465c'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  title-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  mono-sm:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  container-max: 1280px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 40px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

The design system is built on a foundation of **Modern Minimalism** with a focus on high-density information clarity and premium tactile feedback. It targets enterprise knowledge management where the UI must feel both invisible and authoritative. 

The aesthetic borrows the precision of developer-centric tools like Linear with the soft, approachable elegance of modern consumer hardware. The emotional goal is to evoke a sense of "calm intelligence"—reducing the cognitive load of complex data through expansive white space, subtle depth, and a refined color palette. The interface uses glassmorphism not for decoration, but to establish a logical hierarchy of overlays and context-aware surfaces.

## Colors

This design system utilizes a refined light-mode palette centered around a high-vibrancy primary blue and a sophisticated range of slates.

- **Primary (#3B82F6):** Used for actionable items, active states, and focus indicators.
- **Surface (#F8FAFC):** The primary canvas color, providing a soft, non-clinical backdrop for content.
- **Neutral/Text (#0F172A):** Used for primary headings and body text to ensure maximum contrast and legibility.
- **Secondary/Muted (#64748B):** Reserved for metadata, captions, and secondary icons.
- **Glass Overlays:** Semi-transparent white (`rgba(255, 255, 255, 0.7)`) with a 12px backdrop blur for navigation bars and modal backdrops.

## Typography

The typography system relies exclusively on **Inter** for its neutral, systematic quality and exceptional legibility at small sizes. 

- **Hierarchy:** Use tight letter spacing (-0.01em to -0.02em) for headlines to achieve a premium, "locked-in" editorial look.
- **Weight:** Maintain a high contrast between Bold (700) or SemiBold (600) titles and Regular (400) body text.
- **Code:** For AI-generated snippets or technical references, use **JetBrains Mono** to maintain the professional, developer-friendly aesthetic.
- **Vertical Rhythm:** A base 4px grid should guide all line-height values to ensure consistent spacing between paragraphs and headings.

## Layout & Spacing

This design system employs a **Fluid Grid** with fixed maximum constraints for desktop readability. 

- **Grid Model:** A 12-column system is used for dashboard layouts, while a single-column centered layout (800px max) is preferred for document reading and chat interfaces.
- **Rhythm:** All spacing (padding, margins, gaps) must be multiples of 4px.
- **Desktop:** Sidebars are fixed at 280px, with the main content area expanding fluidly.
- **Mobile:** Transition to a 4-column grid with 16px margins. Bottom-sheets replace complex dropdowns or sidebars for better thumb-reach.

## Elevation & Depth

Hierarchy is established through a combination of **Tonal Layers** and **Ambient Shadows**.

1. **Base Layer:** `#F8FAFC` (Slate 50).
2. **Surface Layer:** White (`#FFFFFF`) with a very soft `shadow-sm` (0 1px 2px rgba(0,0,0,0.05)). Used for primary cards and content sections.
3. **Overlay Layer:** White with 70% opacity and 12px blur. Used for headers and floating navigation.
4. **Active/Interactive Layer:** `shadow-md` (0 4px 6px rgba(0,0,0,0.07)) to signify lift on hover or active modals.

Avoid heavy black shadows; instead, use shadows tinted with the primary slate color (`rgba(15, 23, 42, 0.08)`) to maintain the "clean" aesthetic.

## Shapes

The design system utilizes a high degree of roundedness to feel approachable and premium.

- **Base Components:** 0.5rem (8px) for buttons and inputs.
- **Cards & Containers:** 1rem (16px) for `rounded-lg` elements like document previews.
- **Feature Surfaces:** 1.5rem (24px) for `rounded-xl` elements like the primary AI chat interface container or search bars.
- **Chat Bubbles:** Mixed radii (e.g., 16px for most corners, 4px for the tail corner) to distinguish message origin.

## Components

### Buttons
- **Primary:** Solid `#3B82F6` with white text. Sublte inner glow (white 10% opacity) on top edge for a tactile feel.
- **Secondary:** White background with a 1px border of `#E2E8F0` and `#0F172A` text.

### Chat Interface
- **User Messages:** High-contrast background (`#0F172A`) with white text, aligned to the right.
- **AI Responses:** Soft neutral background (`#FFFFFF`) with a subtle `shadow-sm` and `#0F172A` text, aligned to the left. 
- **Input Field:** A large, `rounded-xl` text area with a glassmorphic background and a prominent "Send" button.

### Cards (Document References)
- Use a white surface with a 1px `#F1F5F9` border. 
- Include a small favicon or icon representing the file type in the top left.
- Hover state: Slight scale-up (1.02x) and transition to `shadow-md`.

### Dropdowns & Menus
- Full glassmorphism (70% white, 12px blur).
- 8px padding between menu items.
- Item hover state: `#F1F5F9` background with `rounded-md` (4px).

### Input Fields
- Focus state: 2px ring of `rgba(59, 130, 246, 0.2)` with a `#3B82F6` border. 
- Placeholder text: `#94A3B8`.