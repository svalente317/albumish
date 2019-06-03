#!/bin/sh
j0=${PWD}/albumish.jar
j1=/usr/share/java/swt4.jar
j2=snapshot/jaudiotagger-2.2.6-SNAPSHOT.jar
j3=lib/gson-2.8.5.jar
j4=lib/imgscalr-lib-4.2.jar
j5=lib/jlayer-1.0.1.jar
j6=lib/jna-5.3.1.jar
exec java -cp "bin:${j1}:${j2}:${j3}:${j4}:${j5}:${j6}" albumish.Jukebox "$@"
