# MNK Game

## Introduction
Project for the Algorithms and Data Structures course at the University of Bologna (A.Y. 2020-2021).
### Description
MNK Game is a generalized version of tic-tac-toe with a M x N grid and the objective of aligning K elements.\
The task is to implement a resource efficient algorithm able to play the game (potentially any configuration) in an optimal way.

### Players
`RandomPlayer` plays completely random moves.\
`QuasiRandomPlayer` plays randomly but is able to detect a single-move win or loss.\
`OurPlayer` the implementation for the project.

### Packages
`mnkgame` contains the base MNK Game program provided for the project.\
`player` contains the implementation of the players (Note: `RandomPlayer` and `QuasiRandomPlayer` were already provided).


## Compiling
From the project root folder, run:
```
javac mnkgame/*.java
javac player/*.java
```

## Usage
### Human vs Computer
```
java mnkgame.MNKGame [M] [N] [K] player.[Player]
```

### Computer vs Computer
```
java mnkgame.MNKGame [M] [N] [K] player.[Player1] player.[Player2]
```

### Automated tester (Computer vs Computer)
Text based game that only shows the result
```
java mnkgame.MNKPlayerTester [M] [N] [K] player.[Player1] player.[Player2]
```
#### Flags
`-v`     Verbose\
`-t [n]` Timeout of [n] seconds to select the next move\
`-r [n]` Play [n] rounds
