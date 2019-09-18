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

// module that generate mdio clock (MDC)
class MdioClock (private val mainFreq: Int,
                 private val targetFreq: Int) extends Module{
  assert(mainFreq > targetFreq,
    "target frequency must be less than mainFreq")
  assert(mainFreq%targetFreq == 0,
    "Main frequency must be a multiple of target frequency")
  val io = IO(new Bundle{
    val mdc = Output(Bool())
    val mdc_rise = Output(Bool())
    val mdc_fall = Output(Bool())
  })

  def risingedge(x: Bool) = x && !RegNext(x)
  def fallingedge(x: Bool) = !x && RegNext(x)

  val mdcReg = RegInit(true.B)
  val maxCount = mainFreq/targetFreq
  val halfMaxCount = maxCount/2
  val countReg = RegInit(0.U(log2Ceil(maxCount).W))

  countReg := countReg + 1.U
  when(countReg === (halfMaxCount.U - 1.U)){
    mdcReg := 0.U
  }.elsewhen(countReg === (maxCount.U - 1.U)){
    mdcReg := 1.U
    countReg := 0.U
  }

  io.mdc := mdcReg
  io.mdc_rise := risingedge(mdcReg)
  io.mdc_fall := fallingedge(mdcReg)
}

class Mdio (private val mainFreq: Int) extends Module {
  val io = IO(new Bundle {
    val mdio = new MdioIf()
    val phyadd = Flipped(Decoupled(UInt(3.W)))
    val regadd = Flipped(Decoupled(UInt(5.W)))
    val data_i = Flipped(Decoupled(UInt(16.W)))
    val data_o = Decoupled(UInt(16.W))
  })

// Notes: MDIO format
//     spbl   ssof srwo sphy  sreg sta      sdata       sidle
//Read  32 1’s 01   10 00AAA RRRRR Z0 DDDDDDDD_DDDDDDDD Z
//Write 32 1’s 01   01 00AAA RRRRR 10 DDDDDDDD_DDDDDDDD Z

  val spbl::ssof::srwo::sphy::sreg::sta::sdata::sidle::Nil = Enum(8)
  val stateReg = RegInit(sidle)

  switch(stateReg){
    is(sidle){ }
    is(spbl ){ }
    is(ssof ){ }
    is(srwo ){ }
    is(sphy ){ }
    is(sreg ){ }
    is(sta  ){ }
    is(sdata){ }
  }
  //XXX: delete it
  io.mdio.mdc := 0.U
  io.mdio.mdo := 0.U
  io.mdio.mdir:= 0.U
  io.phyadd.ready := 0.U
  io.regadd.ready := 0.U
  io.data_o.bits := 0.U
  io.data_o.valid:= 0.U
  io.data_i.ready:= 0.U

}


object Mdio extends App {
  val mainClock = 50
  println(" Generating verilog sources")
  println(" Main clock frequency is " + mainClock + " Mhz")
  chisel3.Driver.execute(Array[String](), () => new Mdio(mainClock))
}
