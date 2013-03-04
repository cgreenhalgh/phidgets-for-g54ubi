#!/bin/bash
java -cp lib/log4j.jar:lib:lib/mariner.jar:lib/phidget21.jar:lib/union.jar:bin:. g54ubi.UnionClient > tmp.txt 2>&1 &
