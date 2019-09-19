SBT = sbt
#BACKEND=--backend-name verilator
VCD=--generate-vcd-output on


hdl:
	$(SBT) "runMain mdio.Mdio"

test:
	$(SBT) "test:runMain mdio.TestMdio $(BACKEND) $(VCD)"

testall:
	$(SBT) "test:testOnly mdio.MdioSpec"

mrproper:
	-rm *.anno.json
	-rm *.fir
	-rm *.v
	-rm -rf target
	-rm -rf test_run_dir
	-rm -rf project

