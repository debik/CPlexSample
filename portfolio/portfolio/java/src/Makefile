
# create a list of all java files so we can compile all at once
JAVA_FILES := $(wildcard cpx/portfolio/data/*.java) $(wildcard cpx/portfolio/messages/*.java) $(wildcard cpx/portfolio/gui/*java)


JAVA_CLASSES:=$(patsubst %.java,%.class,$(JAVA_FILES))
CLASSPATH:=./:${JAVA_HOME}/lib/rt.jar:${SYMPHONY_HOME}/linux2.6-glibc2.3-x86_64/lib/JavaSoamApi.jar

.PHONY: classes
LIST:=

classes: $(JAVA_CLASSES)
	if [ ! -z "$(LIST)" ] ; then \
		javac -cp ${CLASSPATH} -d ../bin $(LIST) ; \
	fi

$(JAVA_CLASSES) : %.class : %.java
	$(eval LIST+=$$<)

clean:
	rm -rf ../bin/*
