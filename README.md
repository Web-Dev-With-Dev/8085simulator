# AURA Studio — 8085 Microprocessor Simulator

> A modern, feature-rich IDE for Intel 8085 assembly language programming, simulation, and real-time hardware visualization.

---

## What is AURA Studio?

AURA Studio is a complete overhaul of the classic [8085 Simulator](http://8085simulator.codeplex.com/) originally developed for educational use. Built on top of the open-source Java foundation, AURA Studio transforms the bare-bones simulator into a professional-grade development environment with a dark-themed IDE, multi-format file support, live hardware visualizers, and a full assembler/debugger pipeline.

---

## Features

### Core Simulator
- Full Intel 8085 instruction set emulation
- Step-by-step and continuous execution modes
- Live register, flag, and accumulator display
- Call Stack Visualizer with subroutine tree and stack tower
- Memory browser with hex + ASCII view

### Assembly IDE
- Syntax-highlighted code editor
- Inline error reporting with line-level diagnostics
- Assembler with label resolution and comment support
- Keyboard shortcuts for assemble, step, run, and reset

### Multi-Format File I/O
- Import / Export: `.asm`, `.hex`, `.bin`, `.dat`
- Sample program loader with error handling
- Auto-save and restore of last session

### Hardware Visualizer Tools *(Tools Menu)*

| Tool | Shortcut | Description |
|------|----------|-------------|
| 7-Segment LED Display | `Ctrl+Shift+7` | BCD/hex digit display with color themes |
| ADC & DAC Oscilloscope | `Ctrl+Shift+A` | Real-time waveform plotter (sawtooth, square, triangle) |
| Traffic Light Controller | `Ctrl+Shift+T` | 4-way junction with dual/single port decoding |
| Stepper Motor Simulator | `Ctrl+Shift+P` | CW/CCW rotation, coil excitation, step telemetry |
| 16x2 LCD Display (HD44780) | `Ctrl+Shift+L` | Full DDRAM emulation, scrolling, cursor control |

---

## Getting Started

### Requirements
- Java 11 or higher (Java 17+ recommended)
- Windows / Linux / macOS

### Run
Double-click `Launch AURA SIMULATOR.bat` (Windows) or run:

```bash
java -jar dist/AuraSimulator.jar
```

### Build from Source

```bash
javac -cp "lib/*" -d build/classes src/*.java
jar uf dist/AuraSimulator.jar -C build/classes .
```

---

## Sample Programs

Sample `.asm` files are included in the `8085 example code/` folder covering:
- Arithmetic operations
- Data transfer & sorting
- BCD conversion
- String manipulation
- I/O port programming (for use with visualizers)

---

## Project Structure

```
AURA STUDIO/
├── src/                    # Java source files
│   ├── Assembler.java      # Main IDE & assembler engine
│   ├── Matrix.java         # CPU emulation core
│   ├── LCD16x2Visualizer.java
│   ├── TrafficLightVisualizer.java
│   ├── StepperMotorVisualizer.java
│   ├── ADCDACVisualizer.java
│   ├── SevenSegmentVisualizer.java
│   └── CallStackVisualizer.java
├── dist/                   # Compiled JARs
├── lib/                    # Dependencies
├── 8085 example code/      # Sample assembly programs
├── 8085_Documentation_latex/ # PDF documentation
└── README.md
```

---

## Credits & Attribution

See [CREDITS.md](CREDITS.md) for full attribution.

---

## License

This project is licensed under the **GNU General Public License v2.0**.
See [LICENSE](LICENSE) for the full license text.

Original simulator: Copyright (C) [jm61288](https://github.com/jm61288/8085Simulator)  
AURA Studio enhancements: Copyright (C) 2025 [Web-Dev-With-Dev](https://github.com/Web-Dev-With-Dev)
