package mdio

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.Driver


// MDIO interface
class MdioIf extends Bundle {
  val mdc = Output(Bool())
  val mdi = Input(Bool())   // \
  val mdo = Output(Bool())  //  }-> MDIO
  val mdir = Output(Bool()) // /
}

class Mdio extends Module {
  val io = IO(new Bundle {
    val mdio = new MdioIf()
    val phyadd = Decoupled(Input(UInt(3.W)))
    val regadd = Decoupled(Input(UInt(5.W)))
    val data_i = Decoupled(Input(UInt(16.W)))
    val data_o = Decoupled(Output(UInt(16.W)))
  })
}


object Mdio extends App {
  println(" Generating verilog sources")
  chisel3.Driver.execute(Array[String](), () => new Mdio())
}
