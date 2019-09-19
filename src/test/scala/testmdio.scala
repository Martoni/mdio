package mdio

import chisel3._
import chisel3.iotesters.PeekPokeTester
import org.scalatest.{Matchers, FlatSpec}

class TestMdio (dut: Mdio) extends PeekPokeTester(dut) {
  println("Begin of Mdio test")
  for (i <- 1 to 10) {
    step(1)
    println("step " + i)
  }
  println("End of Mdio test")
}

class TestMdioClock (dut: MdioClock) extends PeekPokeTester(dut) {
  var count = 0
  var mdc_rise = peek(dut.io.mdc_rise)
  while(mdc_rise != 1){
    step(1)
    mdc_rise = peek(dut.io.mdc_rise)
  }
  expect(dut.io.mdc_rise, 1)
  expect(dut.io.mdc_fall, 0)
  expect(dut.io.mdc, 1)

  step((dut.mainFreq/dut.targetFreq)/2)
  expect(dut.io.mdc_rise, 0)
  expect(dut.io.mdc_fall, 1)
  expect(dut.io.mdc, 0)

  step((dut.mainFreq/dut.targetFreq)/2)
  expect(dut.io.mdc_rise, 1)
  expect(dut.io.mdc_fall, 0)
  expect(dut.io.mdc, 1)
  peek(dut.io.mdc_rise)
  peek(dut.io.mdc_fall)
  peek(dut.io.mdc)
  step(1)
}

object TestMdio extends App {
  val mainClock = 50
  val mdioClock = 1
   chisel3.iotesters.Driver.execute(args, () => new MdioClock(mainClock, mdioClock)){
    c => new TestMdioClock(c)
  }
}

class MdioSpec extends FlatSpec with Matchers {

  "A MdioClock " should "Generate MDC clock with right frequency" in {
    val mainClock = 50
    val mdioClock = 1
    chisel3.iotesters.Driver.execute(Array[String](), () => new MdioClock(mainClock, mdioClock))
          { c => new TestMdioClock(c) } should be(true)
  }

}
