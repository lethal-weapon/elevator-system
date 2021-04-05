# Elevator System

## Intro
An original and ordinary elevator control system which developed initially in *August 2019* out of curiosity. This is a rewrite after 18 months in *March 2021*.

Project adopts Java 8 as the runtime and JavaFX as the graphic library. Comparing to old one, the new design is simpler and the code is much prettier.

## Important Changes
- Using threads to control all animations instead of the "Timeline" class provided by the FX library. As a result, some of them run faster than defined in code.
- Using simple customized states to track lifts and persons' real time status instead of inferring from their animations.

## Demo
![Working Demo](https://storage.googleapis.com/skramerdesigns/images/ElevatorSystemDemo.gif)

[Click here](https://storage.googleapis.com/skramerdesigns/videos/ElevatorSystemDemo.webm) to watch a 54-second screencast.

## License
MIT
