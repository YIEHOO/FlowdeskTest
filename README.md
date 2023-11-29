# FlowdeskTest

This is a project put in place for a challenge.

## Table of Contents

- [About](#about)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
- [Tests](#tests)

## About

The project actively monitors the WebSocket, processes events, and conducts a Selenium test on the Binance website to verify the accuracy of displayed content. It employs Java, Maven, TestNG, Selenium, ExtentReports, and Webdriver Manager to accomplish these tasks.

## Getting Started

### Prerequisites

List of software or tools that need to be installed before setting up the project :

- [Java](https://www.oracle.com/java/technologies/javase-downloads.html)
- [Maven](https://maven.apache.org/download.cgi)

### Installation

1. Clone the repository:

    ```bash
    git clone https://github.com/YIEHOO/FlowdeskTest.git
    ```

2. Navigate to the project directory:

    ```bash
    cd flowdesk
    ```

3. Build the project:

    ```bash
    mvn clean install
    ```


### Tests

1. Run the test

    ```bash
    mvn test
    ```