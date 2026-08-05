# CREDITS — AURA Studio 8085 Simulator

---

## Original Project

**8085 Simulator** — The foundation this project is built upon.

| Field | Info |
|-------|------|
| Original Author | [jm61288](https://github.com/jm61288) |
| Original Repository | https://github.com/jm61288/8085Simulator |
| Original Source | http://8085simulator.codeplex.com/ |
| License | GNU General Public License v2.0 |

> AURA Studio is a derivative work. Full credit for the core CPU emulation engine,
> assembler parser, and base UI goes to the original author.

---

## AURA Studio — Enhancements & Extensions

**Developed by:** [Web-Dev-With-Dev](https://github.com/Web-Dev-With-Dev)  
**Year:** 2026  
**Repository:** https://github.com/Web-Dev-With-Dev/8085simulator

### Contributions

| Area | Description |
|------|-------------|
| Branding | Renamed to AURA Studio, custom logo and splash video |
| UI/Theme | Complete dark-mode redesign of IDE, memory, and register windows |
| File I/O | Multi-format import/export (.asm, .hex, .bin, .dat), error handling |
| Assembler Fix | Normalized comma spacing, patched MOV/LXI instruction encoding |
| I/O Architecture | Added `portQueue[]` (FIFO) and `portWriteCount[]` to Matrix.java |
| 7-Segment Display | Real-time BCD/hex 7-segment LED visualizer |
| ADC/DAC Oscilloscope | 8-bit waveform oscilloscope with sawtooth/square/triangle presets |
| Traffic Light | 4-way intersection controller with dual/single port decoding |
| Stepper Motor | Physical motion visualizer — CW/CCW, coil excitation, step telemetry |
| LCD Display (HD44780) | Full 16x2 character LCD emulation with DDRAM, scrolling, cursor |
| Call Stack Visualizer | Stack tower graphic and subroutine call tree |
| Bug Fixes | LCD queue flush bug, assembler parsing errors, UI threading issues |

---

## Third-Party Libraries & Licenses

| Library | Purpose | License |
|---------|---------|---------|
| Java Swing (JDK) | UI framework | Oracle Binary Code License |
| JavaFX (optional) | Media/video | GPLv2 + Classpath Exception |

---

## Acknowledgements

- Intel 8085 Architecture Reference — *Intel Corporation*
- HD44780 LCD Controller Datasheet — *Hitachi Semiconductor*
- The open-source 8085 educator community for sample programs and documentation

---

## License Notice

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

See [LICENSE](LICENSE) for the complete text.

---

*AURA Studio — Built on open-source, extended for the future.*
