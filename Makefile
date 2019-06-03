PREFIX=/usr/local
JARDIR=$(PREFIX)/share/java

j1=/usr/share/java/swt4.jar
j2=jaudiotagger-2.2.6-SNAPSHOT.jar
j3=gson-2.8.5.jar
j4=imgscalr-lib-4.2.jar
j5=jlayer-1.0.1.jar
j6=jna-5.3.1.jar

all:	albumish.jar

albumish.jar:
	ant -e jar

clean:
	rm -rf albumish.jar bin lib *~

bin/albumish.sh:
	echo '#!/bin/sh' > $@
	echo 'exec /usr/bin/java -cp "$(JARDIR)/albumish.jar:$(j1):$(JARDIR)/$(j2):$(JARDIR)/$(j3):$(JARDIR)/$(j4):$(JARDIR)/$(j5):$(JARDIR)/$(j6)" albumish.Jukebox "$$@"' >> $@
	chmod 755 $@

install: all bin/albumish.sh
	mkdir -p $(PREFIX)/bin
	mkdir -p $(JARDIR)
	cp bin/albumish.sh $(PREFIX)/bin/albumish
	cp albumish.jar $(JARDIR)
	cp snapshot/$(j2) lib/$(j3) lib/$(j4) lib/$(j5) lib/$(j6) $(JARDIR)
