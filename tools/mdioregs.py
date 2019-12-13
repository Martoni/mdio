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
         9:("Restart Auto-Negotiation", {1:"Restart auto-Negotiation",
                                         0:"Normal Operation"}, "RW/SC"),
         8:("Duplex Mode", {1:"Full-duplex", 0:"Half-duplex"}, "RW"),
         7:("Collision Test", {1:"Enable COL test", 0:"Disable COL test"}, "RW"),
        },
    0x01: {
        15:("100BASE-T4", {1:"T4 capable", 0:"Not T4 capable"}, "RO"),
        14:("100BASE-TX Full-Duplex", {1:"Capable of 100 Mbps full-duplex",
                                       0:"Not capable of 100 Mbps full-duplex"}, "RO"),
        13:("100BASE-TX Half-Duplex", {1:"Capable of 100 Mbps half-duplex",
                                       0:"Not capable of 100 Mbps half-duplex"}, "RO"),
        12:("10BASE-T Full-Duplex", {1:"Capable of 10 Mbps full-duplex",
                                     0:"Not capable of 10 Mbps full-duplex"}, "RO"),
        11:("10BASE-T Half-Duplex", {1:"Capable of 10 Mbps half-duplex",
                                     0:"Not capable of 10 Mbps half-duplex"}, "RO"),
         6:("No Preamble", {1:"Preamble suppression", 0:"Normal preamble"}, "RO"),
         5:("Auto-Negotiation Complete", {1:"Auto-negotiation process Completed",
                                          0:"Auto-negotiation process not Completed"}, "RO"),
         4:("Remote Fault", {1:"Remote Fault", 0:"No Remote Fault"}, "RO"),
         3:("Auto-Negotiation Ability", {1:"Can perform auto-negotiation",
                                         0:"Cannot perform auto-negotiation"}, "RO"),
         2:("Link Status", {1:"Link is up", 0:"Link is down"}, "RO"),
         1:("Jabber Detect", {1:"Jabber detected", 0:"Jabber not detected"}, "RO"),
         0:("Extended Capability", {1:"Support Extended Capability registers",
                                    0:"No suppor for Capability Registers"}, "RO"),
        },
    0x02: None,
    0x03: None,
    0x04: {
        15:("Next Page", {1:"Next page capable", 0:"No next page capability"}, "RW"),
        13:("Remote Fault", {1:"Remote fault supported",
                             0:"No Remote Fault supported"}, "RW"),
   (11,10):("Pause", {"00":"No pause",
                      "10":"Asymmetric pause",
                      "01":"Symmetric pause",
                      "11":"Asymmetric and symmetric pause"}, "RW"),
         9:("100BASE-T4", {1:"T4 capable", 0:"No T4 capability"}, "RO"),
         8:("100BASE-TX Full-Duplex", {1:"100 Mbps full-duplex capable",
                                       0:"No 100 Mbps full-duplex capability"},
                                       "RW"),
         7:("100BASE-TX Half-Duplex", {1:"100 Mbps half-duplex capable",
                                       0:"No 100 Mbps half-duplex capability"},
                                       "RW"),
         6:("10BASE-T Full-Duplex", {1:"10 Mbps full-duplex capable",
                                     0:"No 10 Mbps full-duplex capability"},
                                     "RW"),
         5:("10BASE-T Half-Duplex", {1:"10 Mbps half-duplex capable",
                                     0:"No 10 Mbps half-duplex capability"},
                                     "RW"),
     (4,0):("Selector Field", {"00001":"IEEE 802.3"}, "RW")
    },
    0x05: None,
    0x06: None,
    0x07: None,
    0x08: None,
    0x09: None,
    0x0a: None,
    0x0b: None,
    0x0c: None,
    0x0d: None,
    0x0e: None,
    0x0f: None,
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
            print(f"No details for register number 0x{self._regnum:02X}" +
                    f" value 0x{self._value:04X}")
            return
        for bitnum, bitdesc in descdict.items():
            if type(bitnum) is int:
                bitvalue = (self._value>>bitnum)&0x1
                print("{:2}[{:1}] -> {:25} : {:30} : ({:6})"
                        .format( bitnum, bitvalue, bitdesc[0],
                            bitdesc[1][bitvalue], bitdesc[2]))
            elif type(bitnum) is tuple:
                bitmask = int("1"*((bitnum[0]-bitnum[1])+1), 2)
                bitvalue = "{:032b}".format((self._value>>bitnum[1])&bitmask)
                bitvalue = bitvalue[-(bitnum[0] - bitnum[1] + 1):]
                try:
                    print("({:}:{:})[{}] -> {:25} : {:30} : ({:6})"
                        .format( bitnum[0], bitnum[1], bitvalue, bitdesc[0],
                            bitdesc[1][bitvalue], bitdesc[2]))
                except KeyError:
                    print("{:2}:{:2}[{}] -> {:25} : Undocumented value : ({:6})"
                        .format( bitnum[0], bitnum[1], bitvalue, bitdesc[0], bitdesc[2]))
            else:
                print(bitnum)
                raise Exception("Wrong format {}".format(bitnum))


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
