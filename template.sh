#!/bin/sh

JARS="@JARS@"

exe="$0"
real=`readlink "$exe"`
test -n "$real" && exe="$real"
bindir=`dirname "$exe"`
top=`cd $bindir/..; pwd -P`

classpath=""
for jar in $JARS; do
    filename="${top}/${jar}"
    test -f "$filename" || echo "${filename}: not found"
    test -n "$classpath" && classpath="${classpath}:"
    classpath="${classpath}${filename}"
done

command=java
test `uname` = Darwin && command="java -XstartOnFirstThread"
exec ${command} -cp ${classpath} albumish.Jukebox "$@"
