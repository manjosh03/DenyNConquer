#  Deny and Conquer - Multiplayer Online Game

A real-time multiplayer territory-capture game built using a custom client-server architecture and socket programming. Players compete to capture squares on an 8×8 board by coloring more than 50% of a cell before their opponents. The player with the most captured territories at the end of the game wins.

---

##  Features

- Real-time multiplayer gameplay
- Custom client-server architecture
- TCP socket communication
- Multi-client support
- Shared resource locking and concurrency control
- Live game state synchronization
- Territory capture mechanics
- Score tracking and winner detection
- Custom application-layer messaging protocol

---

##  System Architecture

The game follows a distributed client-server model:

- The server manages game state, player connections, and synchronization.
- Multiple clients connect remotely to the server.
- Clients send player actions to the server.
- The server validates actions and broadcasts updates to all connected players.

```text
Client 1
    |
Client 2 ---- Server ---- Client 3
    |
Client 4
```

---

## Concurrency & Synchronization

A key challenge in multiplayer games is preventing multiple users from modifying the same game object simultaneously.

To solve this:

- Each game cell acts as a shared resource.
- Cells are locked when a player begins interacting with them.
- Other players are prevented from accessing locked cells.
- Locks are released after the capture attempt is completed.
- This ensures data consistency and eliminates race conditions.

---

## Networking Implementation

Built the networking layer from scratch without using external networking or multiplayer frameworks.

Implemented:

- Socket creation and management
- Client connection handling
- Message serialization
- Custom application-layer protocol
- Real-time game state updates
- Multi-client communication

### Example Message Types

```text
JOIN_GAME
LOCK_CELL
CAPTURE_CELL
BOARD_UPDATE
GAME_OVER
```

---

##  Gameplay Rules

1. Players choose a unique color.
2. The board consists of 64 cells (8×8).
3. Players attempt to capture unclaimed cells by coloring at least 50% of the cell.
4. Successfully captured cells become permanently owned.
5. Captured cells cannot be reclaimed.
6. The game ends when all cells have been captured.
7. The player with the highest number of cells wins.
   
---

##  Key Concepts Demonstrated

- Distributed Systems
- Client-Server Architecture
- Socket Programming
- Network Protocol Design
- Concurrent Programming
- Resource Locking
- Real-Time Communication
- Multiplayer Game Development
---

##  What I Learned

Through this project, I gained hands-on experience with:

- Designing networked applications
- Building custom communication protocols
- Managing concurrent access to shared resources
- Developing real-time multiplayer systems
- Debugging distributed applications
- Implementing synchronization mechanisms in client-server environments

---

##  Highlights

- Developed a multiplayer game using low-level socket programming.
- Designed a custom application-layer communication protocol.
- Implemented concurrency control and resource locking mechanisms.
- Built a scalable client-server architecture supporting multiple simultaneous users.
- Solved real-world networking and synchronization challenges common in distributed systems.

---
