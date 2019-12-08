PREFIX=/usr/local
JARDIR=$(PREFIX)/share/java

j1=/usr/share/java/swt4.jar
j2=/usr/share/java/gson-2.8.5.jar
j3=/usr/share/java/imgscalr-lib-4.2.jar
j4=/usr/share/java/jl-1.0.1.jar
j5=/usr/share/java/jna-4.5.1.jar
tagger=jaudiotagger-2.2.6-SNAPSHOT.jar

all:	albumish.jar

albumish.jar:
	ant -e jar

clean:
	rm -rf albumish.jar bin *~

bin/albumish.sh:
	echo '#!/bin/sh' > $@
	echo 'exec /usr/bin/java -cp "$(JARDIR)/albumish.jar:$(j1):$(j2):$(j3):$(j4):$(j5):$(JARDIR)/$(tagger)" albumish.Jukebox "$$@"' >> $@
	chmod 755 $@

install: all bin/albumish.sh
	mkdir -p $(PREFIX)/bin
	mkdir -p $(JARDIR)
	cp bin/albumish.sh $(PREFIX)/bin/albumish
	cp albumish.jar $(JARDIR)
	cp snapshot/$(tagger) $(JARDIR)
