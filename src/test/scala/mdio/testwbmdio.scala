package mdio

import chisel3._
import chisel3.iotesters.PeekPokeTester
import org.scalatest.{Matchers, FlatSpec}

import wbplumbing.WbSlave

// general options are in testmdio.scala

// Usefull classes

class WishboneMaster(dut: MdioWb, wbs: WbSlave) extends PeekPokeTester(dut){

  // some usefull registers address
  val statusAddr = 0
  val controlAddr = 1
  val readDataAddr = 2
  val writeDataAddr = 3

  def wbsRead(addr: BigInt, timeOut: BigInt = 10): BigInt = {
    poke(wbs.adr_i, addr)
    poke(wbs.we_i, 0)
    poke(wbs.stb_i, 1)
    poke(wbs.cyc_i, 1)
    var tout = BigInt(0)
    var ack = BigInt(0)
    var readValue = BigInt(0)
    do {
      step(1)
      ack = peek(wbs.ack_o)
      readValue = peek(wbs.dat_o)
      tout = tout + 1
    } while (ack == 0 && tout <= timeOut)
    poke(wbs.we_i, 0)
    poke(wbs.stb_i, 0)
    poke(wbs.cyc_i, 0)
    step(1)
    readValue 
  }

  def wbsWrite(addr: BigInt, data: BigInt, timeOut: BigInt = 10): Unit = {
    poke(wbs.adr_i, addr)
    poke(wbs.dat_i, data)
    poke(wbs.we_i, 1)
    poke(wbs.stb_i, 1)
    poke(wbs.cyc_i, 1)
    var tout = BigInt(0)
    var ack = BigInt(0)
    do {
      step(1)
      ack = peek(wbs.ack_o)
      tout = tout + 1
    } while (ack == 0 && tout <= timeOut)
    poke(wbs.we_i, 0)
    poke(wbs.stb_i, 0)
    poke(wbs.cyc_i, 0)
    step(1)
  }
}

class MdioWbTest(dut: MdioWb) extends PeekPokeTester(dut) {

  def readMdio(aPhy: BigInt, aReg: BigInt): BigInt = {
    val wbs = new WishboneMaster(dut, dut.io.wbs)
    step(1)
    var addr = (aPhy<<5) + aReg
    var retValue = BigInt(1)
    var timeOut = 10000000
    // check if not busy
    do {
      step(1)
      retValue = wbs.wbsRead(wbs.statusAddr)
      timeOut = timeOut - 1
    } while (timeOut != 0 && (retValue&0x0001) != 0)

    // write phy and reg address with R bit (read)
    wbs.wbsWrite(wbs.controlAddr, (BigInt(1)<<15) + addr)
    
    // wait for reading with busy bit
    do {
      step(100)
      retValue = wbs.wbsRead(wbs.statusAddr)
      timeOut = timeOut - 1
    } while (timeOut != 0 && (retValue&0x0001) != 0)
    
    // read value read
    retValue = wbs.wbsRead(wbs.readDataAddr)

    // return value read
    retValue
  }

  def writeMdio(aPhy: BigInt, aReg: BigInt, aData: BigInt): Unit = {
    val wbs = new WishboneMaster(dut, dut.io.wbs)
    step(1)
    var addr = (aPhy<<5) + aReg
    var retValue = BigInt(1)
    var timeOut = 10000
    // check if not busy
    do {
      step(1)
      retValue = wbs.wbsRead(wbs.statusAddr)
      timeOut = timeOut - 1
    } while (timeOut != 0 && (retValue&0x0001) != 0)

    // write phy and reg address
    wbs.wbsWrite(wbs.controlAddr, addr)
    // write data value to write
    wbs.wbsWrite(wbs.writeDataAddr, aData)
   
    step(500)

    // wait for writing with busy bit
    do {
      step(100)
      retValue = wbs.wbsRead(wbs.statusAddr)
      timeOut = timeOut - 1
    } while (timeOut != 0 && (retValue&0x0001) != 0)
  }
}

// Tests

class TestMdioWbWriteFrame(dut: MdioWb) extends PeekPokeTester(dut){
  println("Test Mdio Wb Write Frame")
  val mdioWb = new MdioWbTest(dut)
  step(1)
  val phy = BigInt(1)
  val reg = BigInt(10)
  val data = BigInt(0x8180)
  mdioWb.writeMdio(phy, reg, data)
}

class TestMdioWbWriteReadFrame(dut: MdioWb) extends PeekPokeTester(dut){
  val mdioWb = new MdioWbTest(dut)
  poke(dut.io.mdio.mdi, 1)
  step(1)
  val phy = BigInt(1)
  val reg = BigInt(10)
  var data = BigInt(0x1234)
  println(f"Write data 0x$data%04X")
  mdioWb.writeMdio(phy, reg, data)
  step(100)
  data = mdioWb.readMdio(phy, reg)
  println(f"read data 0x$data%04X")
  expect(data==0xFFFF, f"Wrong data read $data%04X, should be 0xFFFF")
  step(10)
  poke(dut.io.mdio.mdi, 0)
  data = mdioWb.readMdio(phy, reg)
  println(f"read data 0x$data%04X")
  expect(data==0, f"Wrong data read $data%04X, should be 0")
}

class TestMdioWbReadFrame(dut: MdioWb) extends PeekPokeTester(dut){
  val mdioWb = new MdioWbTest(dut)
  step(1)
  val phy = BigInt(1)
  val reg = BigInt(10)
  val data = BigInt(0x1234)
  var retVal = mdioWb.readMdio(phy, reg)
}

class TestMdioWbRead(dut: MdioWb) extends PeekPokeTester(dut) {
  println("Begin of MdioWbRead test")
  val wbs = new WishboneMaster(dut, dut.io.wbs)
  step(1)
  val data = BigInt(0xcafe)
  val statusAddr = wbs.statusAddr
  println(f"Write at 0x$statusAddr%02X : 0x$data%04X")
  wbs.wbsWrite(statusAddr, data)
  step(2)
  val retVal = wbs.wbsRead(wbs.statusAddr)
  println(f"value read in 0x$statusAddr%02X : 0x$retVal%04X")
}

// Specs

class MdioWbSpec extends FlatSpec with Matchers {
  behavior of "A MdioWb"
  val mainClock = 5
  val mdioClock = 1

  it should "Write value in MDIO" in {
    val args = general.optn
    chisel3.iotesters.Driver.execute(args, () => new MdioWb(mainClock, mdioClock))
          {c => new TestMdioWbWriteFrame(c) } should be(true)

  }

//  it should "Read value in MDIO " in {
//    val args = general.optn
//    chisel3.iotesters.Driver.execute(args, () => new MdioWb(mainClock, mdioClock))
//          {c => new TestMdioWbRead(c)} should be(true)
//  }
//
//  it should "Write mdio value then read mdio value sending two frame" in {
//    val args = Array("--fint-write-vcd",
//                     "--tr-vcd-show-underscored-vars") ++ general.optn
//    chisel3.iotesters.Driver.execute(args, () => new MdioWb(mainClock, mdioClock))
//          {c => new TestMdioWbWriteReadFrame(c) } should be(true)
//  }
}
