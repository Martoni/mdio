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

object TestMdio extends App {
  chisel3.iotesters.Driver.execute(args, () => new Mdio()){
    c => new TestMdio(c)
  }
}
