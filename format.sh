#!/bin/sh
cd src/albumish
for file in *.java; do
    expand -t 4 $file | sed 's/[[:space:]]*$//' > x
    cmp -s $file x || mv x $file
done
rm -f x
