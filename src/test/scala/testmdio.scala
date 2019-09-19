package mdio

import chisel3._
import chisel3.iotesters.PeekPokeTester
import org.scalatest.{Matchers, FlatSpec}

class TestMdioWriteFrame (dut: Mdio) extends PeekPokeTester(dut) {
  println("Begin of Mdio test")
  for (i <- 1 to 10) {
    step(1)
    println("step " + i)
  }
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
    val args = Array("--generate-vcd-output", "on") //++ Array("--backend-name", "verilator")
    //val args = Array[String]()
    chisel3.iotesters.Driver.execute(args, () => new MdioClock(mainClock, mdioClock))
          { c => new TestMdioClock(c) } should be(true)
  }

  it should "Rise mdc_fall when enable " in {
    //val args = Array("--generate-vcd-output", "on")
    val args = Array[String]()
    chisel3.iotesters.Driver.execute(args, () => new MdioClock(mainClock, mdioClock))
          { c => new TestMdioClockEnable(c) } should be(true)
  }
}

class MdioSpec extends FlatSpec with Matchers {
  behavior of "A Mdio"
  val mainClock = 5
  val mdioClock = 1

  it should "Send a simple write frame " in {
    val args = Array("--generate-vcd-output", "on") //++ Array("--backend-name", "verilator")
    chisel3.iotesters.Driver.execute(args, () => new Mdio(mainClock, mdioClock))
          {c => new TestMdioWriteFrame(c)} should be(true)
  }

}
