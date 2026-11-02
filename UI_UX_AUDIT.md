# UI/UX audit — baseline captured 2026-11-02

Audit performed before this transformation's presentation edits. The running production
app at http://127.0.0.1:8090 was inspected at 1440 and 390 CSS pixels. Screenshots of
Search explorer and Document studio are retained in evidence/ui-before/. Source copies,
protected-source hashes and actual query/diagnostic responses are retained in workspace
work/ui-audit-baseline. Existing storage remains untouched.

## Architecture and coverage

React 19.3.1, TypeScript 7.0.2, Vite 8.3.5; hand-authored CSS, no component library or
client router. One route `/` contains two existing state-switched screens: Search explorer
and Document studio. `#cluster` is the diagnostics anchor. `engine.ts` owns the unchanged
HTTP data contracts, 10-second diagnostics refresh, query history and request state.
`main.tsx` presents the screens; lazy `ClusterScene.tsx` uses existing Three.js/OrbitControls
for actual shard selection and visual topology. No autocomplete, filters, document detail
route, dialogs, theme preferences, delete UI or authentication screens exist.

Existing controls/states/text are enumerated in FEATURE_PARITY_CHECKLIST.md. Backend Java,
tests, samples, engine.ts and API payloads are protected by baseline hashes. Rendering and
presentation are the intended edit boundary; regression fixtures use a separate data copy.

## Findings

1. The 510-pixel hero and decorative treatment put search below the first fold on a laptop.
   Product use should take priority over a landing-page composition.
2. Blue gradients, glow, metallic cards and repeated rounded surfaces dominate the visual
   identity. They make the interface resemble a generic futuristic dashboard.
3. Supporting metadata is frequently 6–9px. Several muted colors are difficult to read on
   dark surfaces. This is a readability/contrast concern, not a measured compliance verdict.
4. Every shard is an identical boxed card, nested inside an already boxed workspace. Fine
   rows and aligned values can communicate the same data with less visual weight.
5. The 3D graph is functional for selection/orbit, but its original scale, floating motion
   and animated packet-like markers overstate its role. It is a topology view, not telemetry.
6. Search/result metadata has weak contrast and hierarchy. The actual title/snippet can
   occupy less visual emphasis than surrounding decoration.
7. Small icon buttons and suggestions have inconsistent target sizes. Native controls and
   labels exist, but navigation selection is mostly visual and there is no skip link.
8. Responsive composition exists, but some technical descriptions are hidden at narrow
   widths. Important content should wrap/reflow instead of disappearing.
9. Error/partial states have real meaning and existing role=status/alert usage. Preserve
   that meaning; improve contrast and announcing the result count without inventing progress.
10. All formatting is compressed into dense source lines and scattered literal CSS values.
    A tokenized spacing/type/color system will make refinement less brittle.

## Proposed direction

Precision instrument × editorial clarity: charcoal identity/navigation, warm off-white
reading surface, graphite text, restrained oxidized-copper accent, clear semantic colors,
legible technical values and fine dividers. Keep every heading, description, sample,
query/result value and existing control. Move the existing topology into the diagnostics
rail, reduce its visual ornament, and prioritize search above the fold. No backend changes.

Motion should respond to actions/state, not run as decorative infrastructure activity.
Existing orbit/zoom/selection and pause/resume controls remain. Use 120–180ms focus/hover
transitions, honor reduced motion, and render 3D only when its view/state changes.

Automated accessibility, responsive, behavioral and performance findings will be recorded
after implementation. No Lighthouse score, WCAG certification or perfect design score is
claimed from this initial inspection.
