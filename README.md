# Homecloud REST server

HTTP server following the Homecloud protocol to communicate between the App and the house's controller.

## Overall architecture

The following figure shows how the webserver fits into a home automation system that runs the Rainfall protocol. The left portion of the
figure represents the local sensor/actuator network, which communicates using the Rainfall protocol. Every communication involving the webserver works on a request/response patterns, dictated by the Homecloud protocol.
![alt text](https://github.com/HomeSkyLtd/server/blob/master/doc/figures/overall_architecture.png)

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run


