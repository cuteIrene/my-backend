#!/bin/bash
mkdir -p bin
javac -d bin -cp lib/mysql-connector-j-9.2.0.jar src/com/example/*.java
