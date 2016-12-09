
# Homecloud REST server

HTTP server following the Homecloud protocol to communicate between the App and the house's controller.

## Overall architecture

The following figure shows how the webserver fits into a home automation system that runs the Rainfall protocol. The left portion of the
figure represents the local sensor/actuator network, which communicates using the Rainfall protocol. Every communication involving the webserver works on a request/response patterns, dictated by the Homecloud protocol.

<img src="https://github.com/HomeSkyLtd/server/blob/master/doc/figures/overall_architecture.png" width="800" />

The figure below shows the technologies employed in the communications between the system components. Every message destined to the server is transmitted using the HTTP protocol. Notifications to the local controller are made using websockets, since they provide a means of sending messages in real time to the controller. Finally, notifications to the mobile app are made using Firebase.

<img src="https://github.com/HomeSkyLtd/server/blob/master/doc/figures/communication_diagram.png" width="500" />

## Structure
The server is implemented in a modular fashion, with each request type being handled by a corresponding module. The main modules are described below.

- handler.clj: This file is responsible for the bulk of the server. It listens for HTTP POST requests, checks for authorization tokens (sessions) and routes the requests to the corresponding module. Moreover, it handles websockets connections with local controllers;
- Authentication: This module is responsible for everything user-related, such as registration and authentication;
- Node: This module deals with local node management. Here, "node" means sensors and actuators running in the home network;
- Rule: This module is responsible for automation rules, such as defining or accepting them;
- State: This module is responsible for managing the state of the devices in the home network, including data read by sensors and the state of actuators.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run `lein run`. This will start a server on port 3000.

## Documentation

For documentation on the public functions of each module, please refer to [this link](https://homeskyltd.github.io/server/)
