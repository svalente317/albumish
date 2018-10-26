#!/bin/sh
j0=${PWD}/albumish.jar
j1=lib/gson-2.4.jar
j2=lib/imgscalr-lib-4.2.jar
j3=lib/jaudiotagger-2.2.3.jar
j4=lib/jl-1.0.1.jar
j5=lib/swt.jar
exec java -cp "bin:${j1}:${j2}:${j3}:${j4}:${j5}" albumish.Jukebox "$@"
