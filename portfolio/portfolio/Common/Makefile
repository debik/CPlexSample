#
# Copyright International Business Machines Corp, 1992-2013. US Government
# Users Restricted Rights - Use, duplication or disclosure restricted by GSA
# ADP Schedule Contract with IBM Corp.  
# Accelerating Intelligence(TM). All rights reserved. 
#
# This exposed source code is the confidential and proprietary property of
# IBM Corporation. Your right to use is strictly limited by the terms of the
# license agreement entered into with IBM Corporation. 
#
######################################################################
#
# 		Makefile for C++ API Solve Library
#  ------- R E A D   M E   T O   A V O I D    F A I L U R E  --------
#
#
#
######################################################################

## load predefined macro

TOP = $(SYMPHONY_HOME)

ifeq ($(shell uname -m) , ppc64)
BIT64_FLAG = -m64
endif

CC = g++ $(BIT64_FLAG) -Wno-deprecated -Wall
#CC = icc -gcc-version=346
#set the correct gcc version installed on your platform (for example for RHEL6 set -gcc-version=446)

INCLUDE = -I $(TOP)/src -I $(TOP)/include -I $(COS_HOME)/cplex/include
OUTPUT = ../Output
LIBS = 

CFLAGS_RELEASE = -DGCC34 -DLINUX
CFLAGS_DEBUG = -g -DGCC34 -DLINUX
CFLAGS_=$(CFLAGS_DEBUG)

OUTF = -o $@
XAR = ar cru $@
OEXT =o

## sources and targets
TARGET_LIB =  libsampleCommon.a
LIB = $(OUTPUT)/$(TARGET_LIB)

OBJS = PortfolioInvestment.o PortfolioIO.o

SRCS = $(OBJS:.${OEXT}=.cpp)

.SUFFIXES: .cpp .$(OEXT)

## building process

build all: ${LIB}

${LIB}: $(OBJS)
	$(XAR) $(OBJS); 

%.o:  %.cpp
	${CC} ${CFLAGS_$(MODE)} $(INCLUDE) -c $(OUTF) $< 

clean:
	-rm -f $(OBJS) $(LIB)

depend:
	makedepend -fdependlist -- $(INCLUDE) -Y -- $(SRCS) $(NUL)


