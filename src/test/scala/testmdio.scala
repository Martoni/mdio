package mdio

import chisel3._
import chisel3.iotesters.PeekPokeTester

class TestMdio (dut: Mdio) extends PeekPokeTester(dut) {
  println("Begin of Mdio test")
  for (i <- 1 to 10) {
    step(1)
    println("step " + i)
  }
  println("End of Mdio test")
}

class TestMdioClock (dut: MdioClock) extends PeekPokeTester(dut) {
  println("Testing MdioClock")
  step(50)
}

object TestMdio extends App {
  val mainClock = 50
  val mdioClock = 1
  chisel3.iotesters.Driver.execute(args, () => new Mdio(mainClock, mdioClock)){
    c => new TestMdio(c)
  }
  chisel3.iotesters.Driver.execute(args, () => new MdioClock(mainClock, mdioClock)){
    c => new TestMdioClock(c)
  }

}
