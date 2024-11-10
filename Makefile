PREFIX=/usr/local
JARDIR=$(PREFIX)/lib

j1=lib/swt4.jar
j2=lib/gson-2.8.6.jar
j3=lib/imgscalr-lib-4.2.jar
j4=lib/jl-1.0.1.jar
j5=lib/commons-net-3.11.1.jar
j6=lib/commons-text-1.9.jar
j7=lib/jaudiotagger-2.2.6-SNAPSHOT.jar

CLASSPATH = ${j1}:${j2}:${j3}:${j4}:${j5}:${j6}:${j7}
SRCS      = src/albumish/*.java src/albumish/*/*.java
TOP       = bin

all:	albumish.jar

albumish.jar: compile
	rm -rf $(TOP)/albumish/icons
	cp -r src/albumish/icons $(TOP)/albumish/
	jar -c -f $@ -C $(TOP) albumish

compile:
	javac -Xlint:deprecation -g $(SRCS) -d $(TOP) -cp $(CLASSPATH)

clean:
	rm -rf albumish.jar bin build out Albumish.app lib/albumish.jar *~

install: albumish.jar
	mkdir -p $(JARDIR)
	cp albumish.jar $(JARDIR)
	cp $(j1) $(JARDIR)
	cp $(j2) $(JARDIR)
	cp $(j3) $(JARDIR)
	cp $(j4) $(JARDIR)
	cp $(j5) $(JARDIR)
	cp $(j6) $(JARDIR)
	cp $(j7) $(JARDIR)
	mkdir -p $(PREFIX)/bin
	cat template.sh | sed 's+@JARS@+lib/albumish.jar $(j1) $(j2) $(j3) $(j4) $(j5) $(j6) $(j7)+' > $(PREFIX)/bin/albumish
	chmod 755 $(PREFIX)/bin/albumish

app: albumish.jar
	cp albumish.jar lib/
	jpackage --type app-image --name Albumish \
	--main-class albumish.Jukebox --app-version 1.0 \
	--description "Albumish Music Organizer and MP3 Player" \
	--java-options -XstartOnFirstThread \
	--main-jar albumish.jar --input lib \
	--icon src/albumish/icons/CD.icns

run:	all
	java -XstartOnFirstThread -cp albumish.jar:$(CLASSPATH) albumish.Jukebox
