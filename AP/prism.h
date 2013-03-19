/*
 * prism.h
 *
 *  Created on: Nov 18, 2010
 *      Author: cmartin
 *
 *  Copyright 2010 Martin-Azizi Pty Ltd
 *
 */


/* INFORMATION ON PRISM HEADERS
 *
 * Its a bit hard to get reliable information on Prism headers
 * The following information has been "gleamed" from examining
 * other drivers
 *
 * Endianness:  Prism headers are in "host" order
 *
 * did:     values are #defined below
 *
 * status:   0 indicates that the parameter is supplied by the driver
 *     1 indicates that the driver doesn't supply the parameter.
 *     if not supplied it should be ignored
 *
 * len:     1-4  This is the number of bytes in the data field
 *          In all the drivers I have looked at, this value has always been 4
 *
 * data:    The actual value - if all bits are not used they should be zeroed
 *
 *
 * The Broadcom driver PRISM header supplies:
 *
 * hosttime:  In jiffies - for our system this is in 10ms units
 *
 * mactime: In micro-seconds - not much use to us, we want it in milliseconds
 *     a 32 bit usec timer will will role over in just over an hour.
 *     Drivers appear to use a 64bit counter to hold mactime internal
 *     the then fill the prism header with the lower 32 bits
 *
 * channel: Not Supplied
 *
 * rssi:     Appears to be a signed dbm value
 *
 * sq:   Signal quality - I think that this is something to do with the number
 *            of errors received
 *
 * signal:    Should be the signal strength in dbm - but doesn't appear to be.
 *     I have seen some drivers use a value of 100-(signal in dbm) to
 *     provide a positive integer.  We are getting a value of "2"
 *
 * noise:     Not Supplied - Should be signed dbm value
 *
 * rate:     Appears to be in units/multiples of 500Khz
 *
 * istx:     Not Supplied  - 0 = rx packet, 1 = tx packet
 *
 * frmlen:     Length of the following frame in bytes
 *
 *
 */


#ifndef PRISM_H_
#define PRISM_H_

//#include <typedefs.h>

#define DNAMELEN 16 // Device name length

#define PRISM_MSGCODE 0x0041 // Monitor Frame
#define PRISM_DID_HOSTTIME 0x1041 // Host time element
#define PRISM_DID_MACTIME 0x2041 // Mac time element
#define PRISM_DID_CHANNEL 0x3041 // Channel element
#define PRISM_DID_RSSI 0x4041 // RSSI element
#define PRISM_DID_SQ 0x5041 // SQ element
#define PRISM_DID_SIGNAL 0x6041 // Signal element
#define PRISM_DID_NOISE 0x7041 // Noise element
#define PRISM_DID_RATE 0x8041 // Rate element
#define PRISM_DID_ISTX 0x9041 // Is Tx frame
#define PRISM_DID_FRMLEN 0xA041 // Frame length

#define PRISM_STATUS_OK 0 // Prism Status: the associated prism_value is supplied
#define PRISM_STATUS_NO_VALUE 1 // Prism Status: the associated prism_value is NOT supplied


struct prism_value
{
  u_int32_t did; // This has a different ID for each parameter
  u_int16_t status; // 0 = set;  1 = not set (yes - not what you expected)
  u_int16_t len; // length of the data (u32) used 0-4
  u_int32_t data; // The data value
} __attribute__ ((packed));

struct prism_header
{
  u_int32_t msgcode; // = PRISM_MSGCODE
  u_int32_t msglen; // The length of the entire header, often 144 bytes = 0x90
  char devname[DNAMELEN]; // The name of the device that captured this packet
  struct prism_value hosttime; // This is measured in jiffies - I think
  struct prism_value mactime; // This is a truncated microsecond timer,
                              // we get the lower 32 bits of a 64 bit value
  struct prism_value channel;
  struct prism_value rssi;
  struct prism_value sq;
  struct prism_value signal;
  struct prism_value noise;
  struct prism_value rate;
  struct prism_value istx;
  struct prism_value frmlen;
  char   dot_11_header[];
} __attribute__ ((packed));



#endif /* PRISM_H_ */

