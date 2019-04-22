#!/bin/bash

set -e
set -v

mkdir build
cd src
javac -d ../build/ Main.java
javac -d ../build/ assembler/*.java
javac -d ../build/ assembler/*.java
javac -d ../build/ interpreter/*.java
javac -d ../build/ utils/*.java