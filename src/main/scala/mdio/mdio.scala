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
                 val targetFreq: Int,
                 val maxPerCount: Int = 65) extends Module{
  assert(mainFreq > targetFreq,
    "target frequency must be less than mainFreq")
  assert(mainFreq%targetFreq == 0,
    "Main frequency must be a multiple of target frequency")

  val io = IO(new Bundle{
    val mdc = Output(Bool())
    val mdc_rise = Output(Bool())
    val mdc_fall = Output(Bool())
    val per_count = Output(UInt(log2Ceil(maxPerCount+1).W))
    val enable = Input(Bool())
  })

  def risingedge(x: Bool) = x && !RegNext(x, true.B)
  def fallingedge(x: Bool) = !x && RegNext(x, false.B)

  val mdcReg = RegInit(false.B)
  val cValidReg = RegInit(false.B)
  val maxCount = mainFreq/targetFreq
  val halfMaxCount = maxCount/2
  val countReg = RegInit(0.U(log2Ceil(maxCount+1).W))
  val periodCountReg = RegInit(0.U(log2Ceil(maxPerCount+1).W))

  countReg := countReg + 1.U
  when(cValidReg){
    when(countReg === (halfMaxCount.U - 1.U)){
      mdcReg := true.B
    }.elsewhen(countReg === (maxCount.U - 1.U)){
      mdcReg := false.B
      countReg := 0.U
    }
  }.otherwise{
    when(countReg === (maxCount.U - 1.U)){
      countReg := 0.U
      mdcReg := false.B
    }
  }

  io.mdc := mdcReg
  io.mdc_rise := risingedge(mdcReg)
  io.mdc_fall := fallingedge(mdcReg)
  io.per_count := periodCountReg
  when(fallingedge(io.enable) ||
    periodCountReg === maxPerCount.U){
    cValidReg := false.B
  }
  when(risingedge(io.enable)){
    io.mdc_fall := 1.U
    periodCountReg := 0.U
    cValidReg := true.B
  }
  when(risingedge(mdcReg)) {
    periodCountReg := periodCountReg + 1.U
  }
}

class Mdio (val mainFreq: Int,
            val targetFreq: Int) extends Module {
  val io = IO(new Bundle {
    val mdio = new MdioIf()
    val phyreg = Flipped(Decoupled(UInt((3+5).W)))
    val data_i = Flipped(Decoupled(UInt(16.W)))
    val data_o = Decoupled(UInt(16.W))
  })

// Notes: MDIO format
//           sheadaddr             sta     sdata        strail
//Read  32 1’s 01   10 00AAA RRRRR Z0  DDDDDDDD_DDDDDDDD Z
//Dir   32 1's 11   11 11111 11111 00  00000000 00000000 0
//Write 32 1’s 01   01 00AAA RRRRR 10  DDDDDDDD_DDDDDDDD Z
//Dir   32 1's 11   11 11111 11111 11  11111111 11111111 0

  val preamble = ("b" + "1"*32).U(32.W)
  val startOfFrame = "b01".U(2.W)
  val readOPCode = "b10".U(2.W)
  val writeOPCode = "b01".U(2.W)
  val readHeader = preamble ## startOfFrame ## readOPCode
  val writeHeader= preamble ## startOfFrame ## writeOPCode
  val sizeFrame = (writeHeader ## "b00".U(2.W) ##
                   io.phyreg.bits ## "b10".U(2.W) ##
                   io.data_i.bits ## "b0".U(1.W)).getWidth
  val sizeHeader = (writeHeader ## "b00".U(2.W) ## io.phyreg.bits).getWidth


  val mdoReg = RegInit(true.B)
  val mdioClock = Module(new MdioClock(mainFreq, targetFreq, sizeFrame))
  val mdioClockEnable = RegInit(false.B)

  val dataOValidReg = RegInit(false.B)
  val dataOReg = RegInit(0.U(16.W))
  //XXX: delete it

  //      0        1               2       3        4        5
  val sheadaddr::swriteframe::sreaddata::sidle::sreadidle::strail::Nil = Enum(6)
  val stateReg = RegInit(sidle)

  var writeFrameReg = RegInit(0.U(sizeFrame.W))
  var readFrameReg = RegInit(0.U(sizeFrame.W))

  switch(stateReg){
    is(sidle){
      dataOValidReg := 0.U
      dataOReg := 0.U
      when(io.phyreg.valid && io.data_i.valid){
        stateReg := swriteframe
        writeFrameReg := (writeHeader ## "b00".U(2.W) ##
                          io.phyreg.bits ## "b10".U(2.W) ##
                          io.data_i.bits ## "b0".U(1.W))
      }
      when(io.phyreg.valid && io.data_o.ready && !io.data_i.valid) {
        stateReg := sheadaddr
        readFrameReg :=  (readHeader ## "b00".U(2.W) ##
                          io.phyreg.bits ## "b0".U((2+16+1).W))
      }
    }
    is(swriteframe) {
      when(mdioClock.io.mdc_fall){
        mdoReg := writeFrameReg(sizeFrame.U - mdioClock.io.per_count - 1.U)
      }
      when(mdioClock.io.mdc_fall && mdioClock.io.per_count === (sizeFrame.U - 1.U)){
        stateReg := strail
      }
    }
    is(strail) {
      when(mdioClock.io.mdc_fall) {
        stateReg := sidle
      }
    }
    is(sheadaddr){ // send preamble on mdo
      when(mdioClock.io.mdc_fall){
        mdoReg := readFrameReg(sizeFrame.U - mdioClock.io.per_count - 1.U)
      }
      when(mdioClock.io.mdc_fall && mdioClock.io.per_count === (sizeHeader.U - 1.U)){
        stateReg := sreaddata
      }
    }
    is(sreaddata){
      when(mdioClock.io.mdc_rise && mdioClock.io.per_count >= (sizeFrame.U - 18.U)){
        dataOReg := dataOReg(14, 0) ## io.mdio.mdi
      }
      when(mdioClock.io.mdc_fall && mdioClock.io.per_count === (sizeFrame.U - 2.U)){
        stateReg := sreadidle
        dataOValidReg := true.B
      }
    }
    is(sreadidle){
      dataOValidReg := false.B
      when(mdioClock.io.mdc_rise){
        stateReg := sidle
      }
    }
  }

  // mdioClock connexions
  mdioClockEnable := stateReg =/= sidle
  mdioClock.io.enable := mdioClockEnable

  // communication interface
  io.phyreg.ready := stateReg === sidle
  io.data_i.ready := stateReg === sidle
  io.data_o.valid := dataOValidReg
  io.data_o.bits := dataOReg

  // mdio connexions
  io.mdio.mdir:= stateReg === sheadaddr || stateReg === swriteframe
  io.mdio.mdc := mdioClock.io.mdc
  io.mdio.mdo := mdoReg
}


object Mdio extends App {
  val mainClock = 50
  val mdioClock = 1
  println(" Generating verilog sources")
  println(" Main clock frequency is " + mainClock + " Mhz")
  println(" MDC frequency is " + mdioClock + " Mhz")
  chisel3.Driver.execute(Array[String](), () => new Mdio(mainClock, mdioClock))
}
