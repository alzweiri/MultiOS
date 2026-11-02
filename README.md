# M21 HyperOS Cockpit v3.0 — Ultimate DJ, Freeform & Universal Edition

## Massive Feature Expansion in v3.0.0-ULTIMATE-DJ-EDITION
1. **CD Crate & Interactive DJ Turntable Studio (`CdTurntableScreen.kt` + `DjSynthEngine.kt`)**:
   - Browse 6 holographic CDs in the CD Crate on the left.
   - Real-time 24kHz PCM synthesizer engine generates live multi-oscillator beats and basslines.
   - Grab the spinning optical CD in the center and turn it backwards/forwards with your finger to **scratch, rewind, or scrub** in real time!
   - 4 DJ Remix FX Sampler Pads (**BASS DROP**, **STUTTER 1/4**, **VINYL BRAKE**, **CYBER RISER**) + **Pitch/Tempo Crossfader** (0.5x to 2.0x BPM) + **Resonant Bass Filter**.
2. **Silent Volume Control (Zero Stock Android Volume Bar)**:
   - Custom Sliders and Hardware Volume Up/Down keys adjust `AudioManager.STREAM_MUSIC` with `flags = 0` (`setVolumeDirect` & `MainActivity.onKeyDown`), completely suppressing the stock Android volume popup!
3. **5 Knob Designs & Reassignable Knob Functions (`RotaryDial.kt`)**:
   - Tap the header pill on ANY rotary knob (`⟳ CPU GOVERNOR`) to reassign what it controls across 7 functions: CPU Governor, Thermal Limit, Display Brightness, Silent Volume, CD Scrub/Rewind, DJ Tempo Speed, or DJ Bass Filter.
   - 5 switchable Knob visual presets: **Liquid Glass Lens**, **Machined Titanium**, **Neon Cyber Ring**, **Minimalist Halo**, and **DJ Vinyl Jogger**.
4. **4 Custom Slider Visual Styles (`CustomLiquidSlider.kt`)**:
   - Switch between **Liquid Glass Capsule**, **Neon Laser Beam**, **16-Segment Studio LED**, and **DJ Console Crossfader**.
5. **Freeform Drag-Anywhere Mode (Dashboard & App Drawer)**:
   - Unlock `🔓 DRAG WIDGETS: ON` in the Custom Status Bar to drag any rotary knob or the central HUD anywhere on the screen.
   - In the App Drawer, toggle `✥ FREEFORM DRAG ICONS` (or long-press any app icon) to drag app icons anywhere on the canvas.
6. **Universal Hardware & Screen Size Calibration (`SystemSettingsPanel.kt`)**:
   - Switch between **M21 Stand Locked (2140x1080)** and **Universal Fullscreen Mode** (runs on any phone or tablet), plus live sliders for **Custom DPI (200–400 DPI)** and **Stand Horizontal Bezel Masking (0–240 px)**.
7. **6 Color Palettes & 6 Animated Procedural Wallpapers (`DynamicWallpaperCanvas.kt`)**:
   - Switch live between Cyber Cyan, HyperOS Purple, Solar Gold, Matrix Emerald, Crimson Red, and Liquid Ice palettes, plus Liquid Aurora, 3D Synthwave Grid, Deep Space Plasma, Pure AMOLED Pitch Black, Matrix Rain, and Hyper Prism wallpapers.
