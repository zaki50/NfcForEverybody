/*
 * RC-S620/S sample library for Arduino
 *
 * Copyright 2010 Sony Corporation
 */

#include <inttypes.h>

#ifndef RCS620S_H_
#define RCS620S_H_

/* --------------------------------
 * Constant
 * -------------------------------- */

#define RCS620S_MAX_CARD_RESPONSE_LEN    254
#define RCS620S_MAX_RW_RESPONSE_LEN      265


/*
  for default Serial port, use Serial
  for TX1/RX1,             use Serial1
  for TX2/RX2,             use Serial2
  for TX3/RX3,             use Serial3
*/
#define SERIAL_RCS620S Serial1

/* --------------------------------
 * Class Declaration
 * -------------------------------- */

class RCS620S
{
public:
    RCS620S();

    int initDevice(void);
    int polling(uint16_t systemCode = 0xffff);
    int cardCommand(
        const uint8_t* command,
        uint8_t commandLen,
        uint8_t response[RCS620S_MAX_CARD_RESPONSE_LEN],
        uint8_t* responseLen);
    int rfOff(void);

    int push(
        const uint8_t* data,
        uint8_t dataLen);

private:
    int rwCommand(
        const uint8_t* command,
        uint16_t commandLen,
        uint8_t response[RCS620S_MAX_RW_RESPONSE_LEN],
        uint16_t* responseLen);
    void cancel(void);
    uint8_t calcDCS(
        const uint8_t* data,
        uint16_t len);

    void writeSerial(
        const uint8_t* data,
        uint16_t len);
    int readSerial(
        uint8_t* data,
        uint16_t len);
    void flushSerial(void);

    int checkTimeout(unsigned long t0);

public:
    unsigned long timeout;
    uint8_t idm[8];
    uint8_t pmm[8];
};

#endif /* !RCS620S_H_ */
