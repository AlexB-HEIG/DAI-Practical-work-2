# Practical work 2

## Author 
- Alex Berberat
- Lisa Gorgerat

## Table of content
- [Introduction](#introduction)
- [Getting Started Using GitHub](#getting-started-using-github)
    - [Prerequisites](#prerequisites)
    - [Recommendation](#recommendation)
    - [Setup](#setup)
        - [Clone the repository](#clone-the-repository)
        - [Build the application](#build-the-application)
- [Getting Started using Docker](#getting-started-using-docker)
  - [Prerequisites](#prerequisites-1)
  - [Recommendation](#recommendation-1)
  - [Setup](#setup-1)
    - [Get the package](#get-package)
    - [Build the application](#build-the-application-1)
- [Usage](#usage)
    - [Run the application](#run-the-application)
- [Demonstration](#demonstration)

## Introduction

This practical work running in java 21 is a picocli based CLI that use the TCP protocol to play a game of tic tac toe.

A player can connect to a server and choose the size of the board for this game.
Another player can connect to the same server to play with the first one.

With each move, the new situation is printed in the terminal.

A new game can be started without closing and reopening a server connection.

## Getting Started Using GitHub
### Prerequisites
- JDK
- Maven (optional, a maven wrapper comes with the project)

### Recommendation
Use Intellij IDEA because the whole project was built using it.

### Setup
#### Clone the repository

1. Go to the [repository](https://github.com/AlexB-HEIG/DAI-Practical-work-2) on GitHub and choose your favorite clone option.
2. Open the terminal in the folder where you want to clone the repository.
3. Clone the repo.
    ```sh 
    git clone git@github.com:AlexB-HEIG/DAI-Practical-work-2.git
    ```
4. Change git remote url to avoid accidental pushes to base project.
    ```sh
    git remote set-url origin <github_username/repo_name>
    git remote -v
    ```

#### Build the application
To build the application, you can use the following commands in your terminal.
```sh
# Download the dependencies and their transitive dependencies
./mvnw dependency:go-offline

# Package the application
./mvnw package
```
Or you can use _Package application as JAR file_ configuration file to build easily in Intellij IDEA.  
![maven_config](doc/img/package.png)

## Getting Started Using Docker
### Prerequisites
To use our game, you first need to ensure that docker is installed on your machine.  
If that's not the case, please go to the offical website ([Dockerdocs](https://docs.docker.com/engine/)) and follow the instruction for the version you need.

### Recommendation

### Setup
#### Get package

Go get our package on [placeholder](https://github.com/LisaGorgerat?tab=packages)

#### Build the application
 
To build the image, go in the folder of the app and use the command:
```sh
docker build -t <app_name> .
```

## Usage
Once the app is built, you can run it.
### Run the application

In the case of docker, we also need to create a network, so that 2 container can communicate with eachother:
````sh
docker network create <network_name>
````

And you can list the Docker networks with:
````sh
docker network ls
````

Then you need to run each container:

- the first one:
````sh
docker run --rm -it --network <network_name> --name <container_name> <img_name> -l <port>
````

- the second one:
````sh
docker run --rm -it --network <network_name> --name <img_name> <container_name> <port>
````

Otherwise, you can use the terminal with:
````sh
java -jar target/DAI-Practical-work-2-1.0-SNAPSHOT.jar client <host> <port>
java -jar target/DAI-Practical-work-2-1.0-SNAPSHOT.jar server <port> 
````
or with the premades configs:

![start server]()

![start client]()

## Demonstration


