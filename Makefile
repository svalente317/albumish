PREFIX=$(HOME)/Applications/Albumish.app/Contents
JARDIR=$(PREFIX)/lib

j1=lib/swt-4.17.jar
j2=lib/gson-2.8.6.jar
j3=lib/imgscalr-lib-4.2.jar
j4=lib/jl-1.0.1.jar
j5=snapshot/jaudiotagger-2.2.6-SNAPSHOT.jar

CLASSPATH = ${j1}:${j2}:${j3}:${j4}:${j5}
SRCS      = src/albumish/*.java
TOP       = bin

all:	albumish.jar

albumish.jar: compile
	rm -rf $(TOP)/bin/icons
	cp -r src/albumish/icons $(TOP)/albumish/
	jar -c -f $@ -C $(TOP) albumish

compile:
	javac -Xlint:deprecation -g $(SRCS) -d $(TOP) -cp $(CLASSPATH)

clean:
	rm -rf albumish.jar bin *~

install: albumish.jar
	mkdir -p $(JARDIR)
	cp $(j1) $(JARDIR)
	cp $(j2) $(JARDIR)
	cp $(j3) $(JARDIR)
	cp $(j4) $(JARDIR)
	cp $(j5) $(JARDIR)
	cp albumish.jar $(JARDIR)
	mkdir -p $(PREFIX)/bin
	cat template.sh | sed 's+@JARS@+albumish.jar $(j1) $(j2) $(j3) $(j4) $(j5)+' > $(PREFIX)/bin/albumish
	chmod 755 $(PREFIX)/bin/albumish
