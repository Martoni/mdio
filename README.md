# MDIO
Drive MDIO phy interface with a Chisel component.
Test it with [ksz8081rnb](https://www.microchip.com/wwwproducts/en/KSZ8081) phy.

## Wishbone interface

Wishbone slave is a simple 16bits data interface with 4 registers.

 16bits addr  | register name
 ------------ | -------------
 0x0          | status
 0x1          | control
 0x2          | readData
 0x3          | writeData

### status

 bit  |  15..8  |  7..1 |   0
 ---- | ------- | ----- | ----
 name | version |  void | busy

with:
- version in decimal without dot (1.0 -> 10)
- busy: set if frame is sending

### control

 bit  | 15  |  ...   | 7..5 | 4..0
 ---- |---- | ------ | ---- | ----
 name |  R  |  void  | aphy | areg

with:
- R: read bit, must be set to emit read frame
- aphy: phy address number
- areg: register number

### readData

 bit  | 15 .. 0
 ---- | -------
 name | readData

### writeData

 bit  | 15 .. 0
 ---- | -------
 name | writeData


## Typical use

### Send write data frame

To send data frame on the mdio bus follow this step :

- monitor the busy bit in status register, if the bit is low go to following
	step.
- Write phy address and reg address in control register
- Write data in writeData register. The wishbone write will trigger the data
	mdio write frame.
- monitor the busy bit, once busy bit is low, write data frame has been sent.

### Send read data frame

- monitor the busy bit in status register, if the bit is low go to following
	step.
- Write phy address, reg address and read bit 'R'. Writing the 'R' bit will
	trigger the read mdio frame
- monitor the busy bit, once busy bit is low, read data frame is terminated.
- read readData register to get the data read.

## tests

To launch test use the makefile with commands:
```
$ make testall
```


