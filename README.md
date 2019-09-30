# MDIO
Drive MDIO phy interface with a Chisel component.
Test it with [ksz8081rnb](https://www.microchip.com/wwwproducts/en/KSZ8081) phy.

## Wishbone interface

Wishbone slave is a simple 16bits data interface with 4 registers.

   16bits addr  | register name
   ------------ | -------------
         0x0    | status 
         0x1    | control 
         0x2    | readData
         0x3    | writeData

### status

 bit  |15 .. 1 |   0
 ---- |------- | ----
 name | void   | busy

with:
- busy: set if frame is sent

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

## tests

To launch test use the makefile with commands:
```
$ make testall
```


