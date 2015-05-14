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

setup = 
SYMPHONY_HOME = /opt/ibm/psde611/6.1.1
export SYMPHONY_HOME

COS_HOME = /opt/ibm/ILOG/CPLEX_Studio_Preview1261/
COS_SYSLIB  = x86-64_linux/static_pic
export COS_HOME COS_SYSLIB

DBG_FLAGS = -g3 -ggdb

tar: all
	rm -f Output/PortfolioServiceCPP.tar*
	cd Output && \
		tar cf PortfolioServiceCPP.tar PortfolioServiceCPP && \
		gzip -f -9 PortfolioServiceCPP.tar

all:
	$(MAKE) -C Common all
	$(MAKE) -C Service all
	$(MAKE) -C Client all

depend:
	$(MAKE) -C Common depend
	$(MAKE) -C Service depend
	$(MAKE) -C Client depend

clean:
	$(MAKE) -C Common clean
	$(MAKE) -C Service clean
	$(MAKE) -C Client clean
	rm -f Output/PortfolioServiceCPP.tar*
	$(MAKE) -C java/src clean

gui: 
	$(MAKE) -C java/src 
