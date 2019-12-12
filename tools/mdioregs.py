#! /usr/bin/python3
# -*- coding: utf-8 -*-
#-----------------------------------------------------------------------------
# Author:   Fabien Marteau <fabien.marteau@armadeus.com>
# Created:  12/12/2019
#-----------------------------------------------------------------------------
#  Copyright (2019)  Armadeus Systems
#-----------------------------------------------------------------------------
""" mdioregs
"""

import sys
import time
import getopt

class MdioRegs(object):
    """ decode MDIO register
    """
    
    PHYLIST = ["ksz8081rnb"]

    KSZ_REGS = {
        "Basic Control"                          :0x00,
        "Basic Status"                           :0x01, 
        "PHY Identifier 1"                       :0x02, 
        "PHY Identifier 2"                       :0x03, 
        "Auto-Negotiation Advertisement"         :0x04, 
        "Auto-Negotiation Link Partner Ability"  :0x05, 
        "Auto-Negotiation Expansion"             :0x06, 
        "Auto-Negotiation Next Page"             :0x07, 
        "Link Partner Next Page Ability"         :0x08, 
        "Digital Reserved Control"               :0x10, 
        "AFE Control 1"                          :0x11, 
        "RXER Counter"                           :0x15, 
        "Operation Mode Strap Override"          :0x16, 
        "Operation Mode Strap Status"            :0x17, 
        "Expanded Control"                       :0x18, 
        "Interrupt Control/Status"               :0x1B, 
        "LinkMD Control/Status"                  :0x1D, 
        "PHY Control 1"                          :0x1E, 
        "PHY Control 2"                          :0x1F, 
    }

    DESC_REGS = {
    0x00: {
        15:("Reset", {1:"Software reset", 0:"Normal operation"}, "RW/SC"),
        14:("Loopback", {1:"Loopback mode", 0:"Normal operation"}, "RW"),
        13:("Speed Select", {1:"100Mbps", 0:"10Mbps"}, "RW"),
        12:("Auto-Negociation", {1:"Enable", 0:"Disable"}, "RW"),
        11:("Power-Down", {1:"Power down mode", 0:"Normal operation"}, "RW"),
        10:("Isolate", {1:"Electrical isolation of PHY from RMII", 0:"Normal Operation"}, "RW"),
         9:("Restart Auto-Negotiation", {1:"Restart auto-Negotiation", 0:"Normal Operation"}, "RW/SC"),
         8:("Duplex Mode", {1:"Full-duplex", 0:"Half-duplex"}, "RW"),
         7:("Collision Test", {1:"Enable COL test", 0:"Disable COL test"}, "RW"),
    }
    }

    def __init__(self, regnum, value, phy="ksz8081rnb"):
        if phy not in self.PHYLIST:
            raise Exception(f"Unknown phy {phy}")
        self._regnum = regnum
        self._value = value

    def desc(self):
        inv_regs = {v: k for k, v in self.KSZ_REGS.items()}
        return inv_regs[self._regnum]

    def display_decoded(self):
        print(f"Register details of : '{self.desc()}'")
        descdict = self.DESC_REGS.get(self._regnum, None)
        if descdict is None:
            raise Exception(f"No details for register number {self._regnum:0x02X}")
        for bitnum, bitdesc in descdict.items():
            bitvalue = (self._value>>bitnum)&0x1
            print("{:2}:{:1} -> {:25} : {:30} : ({:6})"
                    .format(
                        bitnum,
                        bitvalue,
                        bitdesc[0],
                        bitdesc[1][bitvalue],
                        bitdesc[2]
                        )
            )


def usage():
    """ Print usages """
    print("Usages :")
    print("$ python3 mdioregs.py [options]")
    print("-r, --regs=[regnum]    reg number in hex")
    print("-v, --value=[value]    value of reg in hex")
 
if __name__ == "__main__":
    if sys.version_info[0] < 3:
        raise Exception("Must be using Python 3")

    try:
        opts, args = getopt.getopt(sys.argv[1:], "hr:v:",
                                   ["help", "reg=" "value="])
    except getopt.GetoptError as err:
        print(err)
        usage()
        sys.exit(2)

    reg = None
    value = None
    for opt, arg in opts:
        if opt in ("-h", "--help"):
            usage()
            sys.exit()
        elif opt in ("-r", "--reg"):
            reg = int(arg, 16)
        elif opt in ("-v", "--value"):
            value = int(arg, 16)

    if None in [reg, value]:
        print("Please give a register number and value")
        usage()
        sys.exit()
    
    mr = MdioRegs(reg, value)
    print(f"Register: {mr.desc()}")
    mr.display_decoded()
