SBT = sbt
#BACKEND=--backend-name verilator
VCD=--generate-vcd-output on

SRCDIR=src/main/scala/mdio

hdl:
	$(SBT) "runMain mdio.Mdio"

test:
	$(SBT) "test:testOnly mdio.MdioWbSpec"

testall:
	$(SBT) "test:testOnly mdio.MdioClockSpec"
	$(SBT) "test:testOnly mdio.MdioSpec"
	$(SBT) "test:testOnly mdio.MdioWbSpec"

publishlocal:
	$(SBT) publishLocal

package: $(SRCDIR)/mdio.scala $(SRCDIR)/wbmdio.scala
	$(SBT) package

cleanoutput:
	-rm -rf output/mdio.*

mrproper: cleanoutput
	-rm *.anno.json
	-rm *.fir
	-rm *.v
	-rm -rf target
	-rm -rf test_run_dir
	-rm -rf project

