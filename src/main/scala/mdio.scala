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
class MdioClock (val mainFreq: Int,
                 val targetFreq: Int) extends Module{
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

class Mdio (private val mainFreq: Int,
            private val targetFreq: Int) extends Module {
  val io = IO(new Bundle {
    val mdio = new MdioIf()
    val phyreg = Flipped(Decoupled(UInt((3+5).W)))
    val data_i = Flipped(Decoupled(UInt(16.W)))
    val data_o = Decoupled(UInt(16.W))
  })

// Notes: MDIO format
//           sheadaddr             sta     sdata        sidle
//Read  32 1’s 01   10 00AAA RRRRR Z0  DDDDDDDD_DDDDDDDD Z
//Write 32 1’s 01   01 00AAA RRRRR 10  DDDDDDDD_DDDDDDDD Z

  val preamble = "1"*32
  val startOfFrame = "01"
  val readOPCode = "10"
  val writeOPCode = "01"
  val readHeader = ("b" + preamble + startOfFrame + readOPCode).U
  val writeHeader= ("b" + preamble + startOfFrame + writeOPCode).U

  val mdioClock = Module(new MdioClock(mainFreq, targetFreq))
  //      0        1     2       3     4      5 
  val sheadaddr::srta::srdata::swta::swdata::sidle::Nil = Enum(6)
  val stateReg = RegInit(sidle)

  val preambleCount = RegInit(32.U)
  val wrReg = RegInit(false.B)

  switch(stateReg){
    is(sidle){
    }
    is(sheadaddr){ // send preamble on mdo
    }
    is(swta){
    }
    is(swdata){
    }
    is(srta){
    }
    is(srdata){
    }
  }

  io.mdio.mdir := 1.U

  //XXX: delete it
  io.mdio.mdc := 0.U
  io.mdio.mdo := 0.U
  io.phyreg.ready := 0.U
  io.data_o.bits := 0.U
  io.data_o.valid:= 0.U
  io.data_i.ready:= 0.U
}


object Mdio extends App {
  val mainClock = 50
  val mdioClock = 1
  println(" Generating verilog sources")
  println(" Main clock frequency is " + mainClock + " Mhz")
  println(" MDC frequency is " + mdioClock + " Mhz")
  chisel3.Driver.execute(Array[String](), () => new Mdio(mainClock, mdioClock))
}
