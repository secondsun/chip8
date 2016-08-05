CHIP 8 by secondsun
===================

A TDD Chip-8 implementation designed to teach how to write an "emulator" from scratch.

[![You Tube Demo](https://img.youtube.com/vi/X8fVRDHrw18/0.jpg)](https://www.youtube.com/watch?v=X8fVRDHrw18)

## Welcome

Chip-8 is a was created in the 1970s by Joseph Weisbecker to run games on the 8-bit computers of the day.  Chip-8 includes an assembly language, an interpreter, and the runtime.  There is a community around the platform which has created several games, and it has also expanded the platform to include newer features.  However, this project focuses on just the "vanilla" Chip-8 from the 1970s.

This project focuses on implementing the Chip8 interpreter in a test driven manner.  You don't need any experience in writing emulators, but understanding Hexedecimal notation and binary operators are important.

The master branch of this project is a stubbed out TDD project.  The [working](https://github.com/secondsun/chip8/tree/working) branch has my finished project.  

## Prerequisites

You need to have [Java 8](http://java.oracle.com) and [Maven](http://maven.apache.org) installed.  You can confirm it is working by running :

```bash
mvn clean install
```

You should maven attempt to run the project and fail because the tests do not pass.  Implementing these tests and thus Chip8 is part of th exercise.

## Code overview

Our tests are in the [test](src/test/java/net/saga/console/chip8/test) directory.  They are grouped into logical categories to to work through.  Feel free to add more tests if you don't feel comfortable with your code or want to see more how it works.

There are a few helper classes for outputting audio, loading roms, displaying graphics, and handling input.  

Finally, the [Chip8](src/main/java/net/saga/console/chip8/Chip8.java) class has a few parameters initialized but is otherwise empty.  This is the class file you will implement.

## Let's Code

The project is broken up into eight sections : 
 - 01 [Arithmetic](src/test/java/net/saga/console/chip8/test/E01ArithmeticOpCodeTest.java)
 - 02 [Bitwise operators](src/test/java/net/saga/console/chip8/test/E02BitwiseOpCodeTest.java)
 - 03 [Execution](src/test/java/net/saga/console/chip8/test/E03ClockExecutionAndMemoryTest.java)
 - 04 [Flow Control](src/test/java/net/saga/console/chip8/test/E04FlowControlTest.java)
 - 05 [Timers](src/test/java/net/saga/console/chip8/test/E05TimersTest.java)
 - 06 [Keypad Input](src/test/java/net/saga/console/chip8/test/E06KeypadInputTest.java)
 - 07 [Graphics](src/test/java/net/saga/console/chip8/test/E07GraphicsTest.java)
 - 08 [Utilities](src/test/java/net/saga/console/chip8/test/E08UtilitiesTest.java)

Additionally you may want to review other documents on CHIP8 from the web.  My reference for writing this project was [Mastering CHIP-8 by Matthew Mikolay](http://mattmik.com/files/chip8/mastering/chip8.html).

# Exec

When you are finished you can run Chip8 in a Swing window

```bash
mvn exec:java  -Dexec.mainClass="net.saga.console.chip8.SwingMain"
```

Find some roms online and run them in your emulator.