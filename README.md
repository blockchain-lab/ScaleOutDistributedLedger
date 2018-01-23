<h1 align="center">
  Scale-out Distributed Ledger
</h1>

<h4 align="center">TU Delft Blockchain Engineering course project on scale-out distributed ledger.</h4>

## Paper
This project implements the system described in the following paper:

> ## A Scale-out Blockchain for Value Transfer with Spontaneous Sharding
> #### By Zhijie Ren and Zekeriya Erkin [[PDF]](https://arxiv.org/abs/1801.02531)
> Blockchain technology, sometimes known by its applications like cryptocurrencies, suffers from the scalability problem mainly due to the unideal throughput of Byzantine fault tolerance consensus algorithms. Recently, many blockchains have been proposed to achieve scale-out throughput, i.e., the throughput of the system grows with the number of nodes. In this paper, we propose a novel scale-out blockchain system for the most considered type of ledgers, we call Value Transferring Ledgers, in which a transaction is a transfer of positive value from one node to another. In our system, nodes commonly agree on a main chain and individually generate their own chains. We propose a locally executable validation scheme with uncompromised validity and scalable throughput. Furthermore, a smart transacting algorithm is introduced so that the system is spontaneously sharded for individual transactions and achieves scale-out throughput.

An in-depth explanation of the system is available as a technical report in the `docs` folder.

## Running instructions
### Requirements
- [Java](https://java.com/en/download/) 8 or newer
- [NodeJS](https://nodejs.org/) 6 or newer
- [Tendermint](https://tendermint.com/) 0.15.0 or newer

### Setup
- Place a tendermint executable (V0.14) called `tendermint.exe` in the root folder of the project. The file-extension of the file must also be present on non-Windows systems. [Download here](https://tendermint.com/downloads)
- Install the tracker server
  - In the folder `tracker-server` run `npm install`
- Determine the master machine (when only using a single machine this must also be the master)
- Determine the IP address of the tracker

### Configuration
Apply these configuration steps for every machine
- In the `SimulationMain`-class:
  - Give each machine its sets of node by changing the `LOCAL_NODES_NUMBER`, `TOTAL_NODES_NUMBER`, `NODES_FROM_NUMBER` values
  - Set `IS_MASTER` to `true` for the master machine and to `false` for all the others
  - If the current machine is the master, also specify the simulation time in seconds.
- In the `Application`-class:
  - Set `TRACKER_SERVER_ADDRESS` and `TRACKER_SERVER_PORT` to point to the server location
    
### Running
- Start the tracker server
  - In folder `tracker-server` run `npm start`
- Start the master machine by calling the main method in `SimulationMain`. 
- Start the other machines the same way as the master.

### Run Tests
From the root folder run `mvn test`.
