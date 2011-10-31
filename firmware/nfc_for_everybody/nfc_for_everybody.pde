#include <RCS620S.h>
#include <inttypes.h>
#include <string.h>

#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#define COMMAND_TIMEOUT  400
//#define PUSH_TIMEOUT    2100
#define POLLING_INTERVAL 500

#define LED_PIN 13

#define SYSTEM_CODE_FOR_NDEF (0x12fc)

RCS620S rcs620s;
int waitCardReleased = 0;

uint8_t prev_idm[8];

AndroidAccessory acc("Zakky.org",
"NfcForEverybody",
"NfcForEverybody Android Accessory",
"1.0",
"https://market.android.com/search?q=pname:org.zakky.nfcforeverybody",
"0000000012345678");

void setup()
{
  int ret;

  // clear
  memset(prev_idm, 0x0, sizeof(prev_idm));

  digitalWrite(LED_PIN, LOW);
  pinMode(LED_PIN, OUTPUT);

  Serial.begin(115200);
  SERIAL_RCS620S.begin(115200);

  ret = rcs620s.initDevice();
  while (!ret) {
    flush_led(2);
    delay(700);
  }

  // USB ホストコントローラ を起動
  acc.powerOn();

  Serial.println("setup(): done");

  flush_led(3);
}

int do_polling(uint16_t systemCode);
size_t read_ndef(uint8_t* buf, size_t buf_len);
void write_felica_target_info(uint8_t *id, size_t id_len, uint8_t *data, size_t data_len);
void flush_led(int count);

void loop()
{
  if (acc.isConnected()) {
    digitalWrite(LED_PIN, HIGH);
    {
      uint16_t systemCode = 0xffff;
      // 最初に NDEF のシステムコードで polling
      int found = do_polling(SYSTEM_CODE_FOR_NDEF);
      if (found) {
        uint8_t idm[sizeof(rcs620s.idm)];
        memcpy(idm, rcs620s.idm, sizeof(idm)); // from rcs620s.idm to idm
        systemCode = SYSTEM_CODE_FOR_NDEF;

        uint8_t data[1024];
        size_t data_len = read_ndef(data, sizeof(data));

        write_felica_target_info(idm, sizeof(idm), data, data_len);
      }
      else { // ワイルドカードでポーリング。 NDEF で無いことは確定
        found = do_polling(systemCode);
        if (found) {
          uint8_t idm[sizeof(rcs620s.idm)];
          memcpy(idm, rcs620s.idm, sizeof(idm)); // from rcs620s.idm to idm
          write_felica_target_info(idm, sizeof(idm), NULL, 0);
        }
      }
    }
  }

  // 常に実行する処理
  rcs620s.rfOff();
  digitalWrite(LED_PIN, LOW);
  delay(POLLING_INTERVAL);
}

int do_polling(uint16_t systemCode = 0xffff)
{
  int ret;

  // Polling
  rcs620s.timeout = COMMAND_TIMEOUT;
  ret = rcs620s.polling(systemCode);
  if (!ret) {
    //カードが検出されなかったので、リリース待ちを解除
    waitCardReleased = 0;
    memset(prev_idm, 0x0, sizeof(prev_idm));
    return 0;
  }

  if (memcmp(prev_idm, rcs620s.idm, sizeof(prev_idm)) == 0) {
    // 前回と同じなので無視
    return 0;
  }

  memcpy(prev_idm, rcs620s.idm, sizeof(prev_idm));

  /*
  else if (!waitCardReleased) {
    // Push
    digitalWrite(LED_PIN, HIGH);
    rcs620s.timeout = PUSH_TIMEOUT;
    ret = rcs620s.push(data, length);
    if (ret) {
      waitCardReleased = 1;
    }
  }
  */

  return ret;
}

size_t read_ndef(uint8_t* buf, size_t buf_len)
{
  // TODO NDEF message を読み込む
  return 0;
}


/*
 * Android へ創出するデータのフォーマット。とりあえず FeliCa の分だけ
 * type: 'F'
 * IDm : uint8[8]
 * length of ndef : uint16 (big endian)
 * ndef : uint8[]
 */
void write_felica_target_info(uint8_t *id, size_t id_len, uint8_t *data, size_t data_len)
{
  uint8_t outbuf[1 + id_len + 2];
  size_t outbuf_len = sizeof(outbuf);
  uint8_t *p = outbuf;
  (*p++) = 'F'; // FeliCa
  memcpy(p, id, id_len); // from id to p
  p += id_len;
  (*p++) = (uint8_t) ((data_len >> 8) & 0xff);
  (*p++) = (uint8_t) ((data_len >> 0) & 0xff);

  acc.write(outbuf, outbuf_len);
  if (data != NULL && 0 < data_len) {
    acc.write(data, data_len);
  }
}

void flush_led(int count)
{
  for (int i = 0; i < count; i++) {
    digitalWrite(LED_PIN, HIGH);
    delay(100);
    digitalWrite(LED_PIN, LOW);
    if (i != count - 1) {
      delay(100);
    }
  }
}

