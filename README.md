# Cognitive and Emotional Multi-Agent System for Ludo

A Multi-Agent System (MAS) simulation of the classic board game **Ludo**, built to evaluate how different predefined personalities and dynamic emotional states interact within a competitive environment. 

This project integrates logical reasoning via **Jason (BDI)**, environment management via **CArtAgO**, and emotional evaluation using the **VEsNA Pro** framework.

##  Agent Profiles
The simulation features 4 autonomous agents, each driven by a distinct psychological profile:
- 🔴 **Aggressive (Red):** Predatory playstyle, actively hunts opponents using circular modular distance calculations.
- 🟢 **Prudent (Green):** Risk-averse, prioritizes safely escorting one token at a time to the private corridor.
- 🟡 **Balanced (Yellow):** Dynamically shifts strategies based on the current state of the board (e.g., deploys urgently if active tokens drop below 2).
- 🔵 **Emotive (Blue):** Driven by the VEsNA Pro dynamic mood engine. Reacts to trauma (token captures) by spiking its `anger` level.

##  Prerequisites
To run this simulation, ensure you have the following installed on your machine:
- **Java Development Kit (JDK)** 11 or higher
- **Python 3.x**
- **Gradle** (or use the included Gradle wrapper)

##  How to Run the Simulation

The execution requires two sequential steps in two separate terminal windows.

### Step 1: Start the Dummy WebSocket Server
The VEsNA framework requires a body endpoint to establish agent connections. Open a terminal, navigate to the server directory, and run the Python script:
Terminal 1
python dummy_server.py

### Step 2: Launch the Multi-Agent System
Terminal 2
gradle run
