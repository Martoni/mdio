package mdio

import chisel3._
import chisel3.iotesters.PeekPokeTester
import org.scalatest.{Matchers, FlatSpec}

object general {
  val optn = Array("-td", "output",
                    // <firrtl|treadle|verilator|ivl|vcs>
                   "--backend-name", "verilator"
                  )
}



class TestMdioWriteFrame (dut: Mdio) extends PeekPokeTester(dut) {
  println("Begin of Mdio test")
  val phyaddr = "111"
  val regaddr = "11111"
  def initvalues = {
    poke(dut.io.phyreg.bits, 1)
    poke(dut.io.phyreg.valid, 0)
    poke(dut.io.data_i.bits, 0)
    poke(dut.io.data_i.valid, 0)
    poke(dut.io.data_o.ready, 0)
  }
  def sendFrame(vWrite:Int, phyaddr: String = "001", regaddr: String = "00010") = {
    expect(phyaddr.length == 3, "Wrong phyaddr")
    expect(regaddr.length == 5, "Wrong regaddr")
    val phyregaddr = Integer.parseInt(phyaddr + regaddr, 2)
    var mdc_old = BigInt(0)
    var mdc = BigInt(0)
    var frameWriteBack = BigInt(0)
    val frameExpectedString = ("1"*32) + "01" + "01" + "00" + phyaddr + regaddr + "10" +
                              ("0"*16 + vWrite.toBinaryString takeRight 16) + "0"
    println("sizeFrame " + dut.sizeFrame)
    println("size expected " + frameExpectedString.length)
    val frameExpected = BigInt(frameExpectedString, 2)
    println("frameExpectedString length " + frameExpectedString.length)
    initvalues
    // initialize values
    step(5)
    // launch write frame
    poke(dut.io.phyreg.bits, phyregaddr)
    poke(dut.io.phyreg.valid, 1)
    poke(dut.io.data_i.bits, vWrite)
    poke(dut.io.data_i.valid, 1)
    step(1)
    initvalues // off all valid signals
    mdc = peek(dut.io.mdio.mdc).toInt
    mdc_old = mdc
    for(i <- 0 until frameExpectedString.length){
      while(!(mdc == 1 && mdc_old == 0)){
        mdc = peek(dut.io.mdio.mdc).toInt
        if(mdc == 1 && mdc_old == 0) { // if rising edge
            val mdo = peek(dut.io.mdio.mdo)
            println(f"mdo $mdo @ $t (i $i)")
            frameWriteBack = (frameWriteBack << 1) + mdo
        } else {
          mdc_old = mdc
        }
        step(1)
      }
      mdc_old = mdc
    }
    step(70*(dut.mainFreq/dut.targetFreq))
    println(f"frame back     0x$frameWriteBack%X")
    println(f"Frame expected 0x$frameExpected%X")
    expect(frameWriteBack == frameExpected, "Wrong write frame generated")
  }

  sendFrame(0x0F0F, phyaddr, regaddr)
  println("End of Mdio test")
}

class TestMdioClockEnable(dut: MdioClock) extends PeekPokeTester(dut) {
  poke(dut.io.enable, 0)
  expect(dut.io.mdc_fall, 0)
  step(5)
  poke(dut.io.enable, 1)
  expect(dut.io.mdc_fall, 1)
  step(1)
  expect(dut.io.mdc_fall, 0)
  step(50)
  poke(dut.io.enable, 0)
  step(10)
  poke(dut.io.enable, 1)
  expect(dut.io.mdc_fall, 1)
  step(10)
}

class TestMdioClockPulse (dut: MdioClock) extends PeekPokeTester(dut) {
  println("Test MdioClock Pulse")
  poke(dut.io.enable, 0)
  step(2)
  poke(dut.io.enable, 1)
  var count = 0
  val maxPerCount = dut.maxPerCount
  val maxTimeCount = (10 + dut.mainFreq/dut.targetFreq) * maxPerCount

  println("maxPerCount : " + maxPerCount)
  println("maxTimeCount : " + maxTimeCount)
  while(t < maxTimeCount) {
    if(peek(dut.io.mdc_rise) == 1){
      count = count + 1
    }
    step(1)
  }
  expect(count == maxPerCount,
    "" + count + " period generated instead of " + maxPerCount)
  expect(dut.io.per_count, maxPerCount,
    "Wrong per_count value : " + peek(dut.io.per_count) + " should be " + maxPerCount)
}

class TestMdioClock (dut: MdioClock) extends PeekPokeTester(dut) {
  var mdc_rise = peek(dut.io.mdc_rise)
  println("Test MdioClock")
  poke(dut.io.enable, 0)
  step(2)
  poke(dut.io.enable, 1)
  mdc_rise = peek(dut.io.mdc_rise)
  while(mdc_rise != 1){
    step(1)
    mdc_rise = peek(dut.io.mdc_rise)
  }
  val maxCount = dut.mainFreq/dut.targetFreq
  var count = maxCount
  step(maxCount)
  expect(dut.io.mdc, 1)
  step(maxCount-1)
  expect(dut.io.mdc, 0)
  step(1)
  expect(dut.io.mdc, 1)
}

class MdioClockSpec extends FlatSpec with Matchers {
  behavior of "A MdioClock"
  val mainClock = 5
  val mdioClock = 1


  it should "Generate MDC clock with right frequency" in {
    val args = general.optn
    chisel3.iotesters.Driver.execute(args, () => new MdioClock(mainClock, mdioClock))
          { c => new TestMdioClock(c) } should be(true)
  }

  it should "Rise mdc_fall when enable " in {
    val args = general.optn
    chisel3.iotesters.Driver.execute(args, () => new MdioClock(mainClock, mdioClock))
          { c => new TestMdioClockEnable(c) } should be(true)
  }

  it should "Generate only number of clock period given in parameter  " in {
    val args = Array("--generate-vcd-output", "on",
                     "--tr-vcd-show-underscored-vars") ++ general.optn
    chisel3.iotesters.Driver.execute(args, () => new MdioClock(mainClock, mdioClock, 5))
          { c => new TestMdioClockPulse(c) } should be(true)
  }

}

class MdioSpec extends FlatSpec with Matchers {
  behavior of "A Mdio"
  val mainClock = 5
  val mdioClock = 1

  it should "Send a simple write frame " in {
    val args = Array("--fint-write-vcd",
                     "--tr-vcd-show-underscored-vars") ++ general.optn
    chisel3.iotesters.Driver.execute(args, () => new Mdio(mainClock, mdioClock))
          {c => new TestMdioWriteFrame(c)} should be(true)
  }

}
