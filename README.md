# Apex - Vape layout (Fabric 1.21.1, Yarn 1.21.1+build.3)

Build: `gradle build` (Gradle 8.8+, JDK 21) -> `build/libs/cpvpclient-1.0.0.jar`.
In-game: RIGHT SHIFT opens the ClickGUI (rebindable under Controls).
LMB a card = toggle, RMB = expand settings (sliders, checkboxes, keybind binder).

## Layout
- `CpvpClient`            entrypoint: tick, per-frame (WorldRenderEvents.START), HUD hooks, GUI key
- `mixin/MouseMixin`      raw GLFW RMB press -> Hold Explode click path
- `mixin/KeyboardMixin`   module keybinds
- `module/*`              Module, Category, ModuleManager
- `module/impl/*`         HoldExplode, AutoAnchor, AutoBreach, SwapStun (+ HUD, Interface)
- `gui/*`                 ClickGuiScreen, RenderUtil (rounded rects, icons, checkbox)
- `util/*`                PacketUtil, SlotUtil, TargetUtil

## Notes
- Written against Yarn 1.21.1 names; not compile-tested in this environment. If a mapping
  differs, fix the single call site (all packet/slot logic is isolated in util/).
- Text uses the vanilla bitmap font scaled via the matrix stack. For true TTF anti-aliasing,
  add a `ttf` font provider JSON and render with a Style that sets that font.
- Swaps mirror the client slot locally, so vanilla may send one redundant slot packet.
- Anticheats on most public servers flag packet-burst attacks and instant swaps. Use where allowed.
