#! /bin/sh
java -Xmx8000M -cp target:lib/ECLA.jar:lib/DTNConsoleConnection.jar:lib/gson-2.8.5.jar:lib/commons-math3-3.2.jar core.DTNSim $*
