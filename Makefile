SBT = sbt
#BACKEND=--backend-name verilator
VCD=--generate-vcd-output on

hdl:
	$(SBT) "runMain mdio.Mdio"

test:
	$(SBT) "test:testOnly mdio.MdioWbSpec"

testall:
	$(SBT) "test:testOnly mdio.MdioClockSpec"
	$(SBT) "test:testOnly mdio.MdioSpec"
	$(SBT) "test:testOnly mdio.MdioWbSpec"

cleanoutput:
	-rm -rf output/mdio.*

mrproper: cleanoutput
	-rm *.anno.json
	-rm *.fir
	-rm *.v
	-rm -rf target
	-rm -rf test_run_dir
	-rm -rf project

