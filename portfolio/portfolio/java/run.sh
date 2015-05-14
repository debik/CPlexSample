#!/bin/bash

cd bin
SYMPHONY=/opt/sym
VERSION=6.1.1
ARCH=linux2.6-glibc2.3-x86_64

source $SYMPHONY/profile.platform || exit 1

set -x

java -classpath $SYMPHONY/soam/$VERSION/$ARCH/lib/JavaSoamApi.jar:. \
    cpx.portfolio.gui.Portfolio || exit 1
