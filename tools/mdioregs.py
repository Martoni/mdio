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
    0x05: {
        15:("Next Page", {1:"Next page capable", 0:"No next page capability"}, "RO"),
        13:("Remote Fault", {1:"Remote fault supported",
                             0:"No Remote Fault supported"}, "RO"),
   (11,10):("Pause", {"00":"No pause",
                      "10":"Asymmetric pause",
                      "01":"Symmetric pause",
                      "11":"Asymmetric and symmetric pause"}, "RO"),
         9:("100BASE-T4", {1:"T4 capable", 0:"No T4 capability"}, "RO"),
         8:("100BASE-TX Full-Duplex", {1:"100 Mbps full-duplex capable",
                                       0:"No 100 Mbps full-duplex capability"},
                                       "RO"),
         7:("100BASE-TX Half-Duplex", {1:"100 Mbps half-duplex capable",
                                       0:"No 100 Mbps half-duplex capability"},
                                       "RO"),
         6:("10BASE-T Full-Duplex", {1:"10 Mbps full-duplex capable",
                                     0:"No 10 Mbps full-duplex capability"},
                                     "RO"),
         5:("10BASE-T Half-Duplex", {1:"10 Mbps half-duplex capable",
                                     0:"No 10 Mbps half-duplex capability"},
                                     "RO"),
     (4,0):("Selector Field", {"00001":"IEEE 802.3"}, "RO")

            },
    0x06: {
            4:("Parallel Detection Fault",
                {1:"Fault detected by parallel detection",
                 0:"No fault detected by parallel detection"}, "RO/LH"),
            3:("Link Partner Next Page Able",
                {1:"Link partner has next page capability",
                 0:"Link partner does not have next page capability"},
                "RO"),
            2:("Next Page Able", {1:"Local device has next page capability",
                                  0:"Local device does not have next page capability"},
                                  "RO"),
            1:("Page Received", {1:"New page received",
                                 0:"New page not received yet"},
                                 "RO/LH"),
            0:("Link Partner Auto-Negotiation Able",
                {1:"Link partner has auto-negotiation capability",
                 0:"Link partner does not have autonegotiation capability"},
                "RO"),
    },
    0x07: {
            13:("Message Page", {1:"Message page",
                                 0:"Unformatted page"}, "RW"),
            12:("Acknowledge 2", {1:"Will comply with message",
                                  0:"Cannot comply with message"}, "RW"),
            11:("Toggle",
                {1:"Previous value of the transmitted link code word equaled logic 1",
                 0:"Logic 0"}, "RO"),
        (10,0):("Message Field", {True:"11-bit wide field to encode 2048 messages"}, "RW"),
    },
    0x08: None,
    0x09: None,
    0x0a: None,
    0x0b: None,
    0x0c: None,
    0x0d: None,
    0x0e: None,
    0x0f: None,
    0x10: None,
    0x11: None,
    0x12: None,
    0x13: None,
    0x14: None,
    0x15: None,
    0x16: {
        15:("Factory Mode", {0:"Normal operation", 1:"Factory test mode"},
        "RW"),
         9:("B-CAST_OFF Override", {1:"Override strap-in for B-CAST_OFF",
                                    0:"No Override  strap-in for B-CAST_OFF"},
                                    "RW"),
         7:("MII B-to-B Override", {1:"Override strap-in for MII back-to-back mode",
                                    0:"No Override"}, "RW"),
         6:("RMII B-to-B Override", {1:"Override strap-in for RMII Back-to-Back mode",
                            0:"No Override"}, "RW"),
         5:("NAND Tree Override", {1:"Override strap-in for NAND tree mode",
                                   0:"No Override"}, "RW"),
         1:("RMII Override", {1:"Override strap-in for RMII mode",
                              0:"No Override"}, "RW"),
         0:("MII Override", {1:"Override strap-in for MII mode",
                             0:"No Override"}, "RW"),
    },
    0x17: {
        (15,13):("PHYAD[2:0] Strap-In Status",
                    {True:"Strap to PHY Address"}, "RO"),
        9:("B-CAST_OFF", {1:"Strap to B-CAST_OFF", 0:"not strap"}, "RO"),
        7:("MII B-to-B", {1:"Strap to MII b-to-b mode", 0:"no strap"}, "RO"),
        6:("RMII B-to-B", {1:"Strap to RMII Back-to-Back", 0:"no strap"}, "RO"),
        5:("NAND Tree", {1:"Strap to NAND tree mode", 0:"no strap"}, "RO"),
        1:("RMII Strap-In", {1:"Strap to RMII mode", 0:"no strap"}, "RO"),
        0:("MII Strap-In", {1:"Strap to MII mode", 0:"no strap"}, "RO"),
    },
    0x18: None,
    0x19: None,
    0x1a: None,
    0x1b: {
            15:("Jabber Interrupt Enable", {1:"enable", 0:"disable"}, "RW"),
            14:("Receive Error Interrupt Enable", {1:"enable", 0:"disable"}, "RW"),
            13:("Page Received Interrupt Enable", {1:"enable", 0:"disable"}, "RW"),
            12:("Parallel Detect Fault Interrupt Enable", {1:"enable", 0:"disable"}, "RW"),
            11:("Link Partner Acknowledge Interrput Enable", {1:"enable", 0:"disable"}, "RW"),
            10:("Link-Down Interrupt Enable", {1:"enable", 0:"disable"}, "RW"),
             9:("Remote Fault Interrupt Enable", {1:"enable", 0:"disable"}, "RW"),
             8:("Link-Up Interrupt Enable", {1:"enable", 0:"disable"}, "RW"),
             7:("Jabber Interrupt", {1:"enable", 0:"disable"}, "RW"),
             6:("Receive Error Interrupt", {1:"enable", 0:"disable"}, "RW"),
             5:("Page Receive Interrupt", {1:"enable", 0:"disable"}, "RW"),
             4:("Parallel Detect Fault Interrupt", {1:"enable", 0:"disable"}, "RW"),
             3:("Link Partner Acknowledge Interrupt", {1:"enable", 0:"disable"}, "RW"),
             2:("Link-Down Interrupt", {1:"enable", 0:"disable"}, "RW"),
             1:("Remote Fault Interrupt", {1:"enable", 0:"disable"}, "RW"),
             0:("Link-Up Interrupt", {1:"enable", 0:"disable"}, "RW"),
    },
    0x1c: None,
    0x1d: None,
    0x1e: {
            9:("Enable Pause (Flow Control)", {1:"Flow control capable",
                                               0:"No flow control capability"},
                                               "RO"),
            8:("Link Status", {1:"Link is Up", 0:"Link is Down"}, "RO"),
            7:("Polarity Status", {1:"Polarity is reversed",
                                   0:"Polarity is not reversed"}, "RO"),
            5:("MDI/MDI-X State", {1:"MDI-X", 0:"MDI"}, "RO"),
            4:("Energy Detect",
                {1:"Signal present on receive differential pair",
                 0:"No signal detected on receive differential pair"}, "RO"),
            3:("PHY Isolate", {1:"PHY in isolate mode",
                               0:"PHY in normal operation"}, "RO"),
        (2,0):("Operation Mode Indication",
            {"000":"Still in auto-negotiation",
             "001":"10BASE-T half-duplex",
             "010":"100BASE-TX half-duplex",
             "011":"Reserved0",
             "100":"Reserved1",
             "101":"10BASE-T full-duplex",
             "110":"100BASE-TX full-duplex",
             "111":"Reserved2",
                }, "RO"),
    },
    0x1f: {
            15:("HP_MDIX", {1:"HP Auto MDI/MDI-X mode",
                            0:"Auto MDI/MDI-X mode"}, "RW"),
            14:("MDI/MDI-X Select", {1:"MDI-X mode",
                                     0:"MDI mode"}, "RW"),
            13:("Pair Swap", {1:"Disable Auto MDI/MDI-X",
                              0:"Enable Auto MDI/MDI-X"}, "RW"),
            11:("Force Link", {1:"Force link pass",
                               0:"Normal link operation"}, "RW"),
            10:("Power Saving", {1:"Enable power saving",
                                 0:"Disable power saving"}, "RW"),
             9:("Interrupt Level", {1:"Interrupt pin active high",
                                    0:"Interrupt pin active low"}, "RW"),
             8:("Enable Jabber", {1:"Enable jabber counter",
                                  0:"Disable jabber counter"}, "RW"),
             7:("RMII Reference Clock",
                 {1:"RMII 50 MHz clock mode; clock input to XI (Pin 9) is 50 MHz",
                  0:"RMII 25 MHz clock mode; clock input to XI (Pin 9) is 25 MHz"}, "RW"),
         (5,4):("LED Mode", {"00":"LED1: Speed, LED0: Link/Activity",
                             "01":"LED1: Activity, LED0: Link",
                             "10":"Reserved",
                             "11":"Reserved"}, "RW"),
             3:("Disable Transmitter", {1:"Disable transmitter",
                                        0:"Enable transmitter"}, "RW"),
             2:("Remote Loopback", {1:"Remote (analog) loopback is enabled",
                                    0:"Normal mode"}, "RW"),
             1:("Enable SQE", {1:"Enable SQE test",
                               0:"Disable SQE test"}, "RW"),
             0:("Disable Data Scrambling", {1:"Disable scrambler",
                                            0:"Enable scrambler"}, "RW"),
    },
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
                    if bitdesc[1].get(True, False):
                        print("({:}:{:})[{}] -> {:25} : {:30} : ({:6})"
                            .format( bitnum[0], bitnum[1], bitvalue, bitdesc[0],
                                bitdesc[1][True], bitdesc[2]))
                    else:
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
