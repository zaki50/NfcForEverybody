
package org.zakky.nfcforeverybody;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.SeekBar;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public abstract class BaseAccessoryActivity extends Activity implements Runnable {
    private static final String TAG = BaseAccessoryActivity.class.getSimpleName();

    private static final String ACTION_USB_PERMISSION = "org.zakky.nfcforeverybody.action.USB_PERMISSION";

    private static final int MESSAGE_FELICA = 1;

    private UsbManager mUsbManager;

    private PendingIntent mPermissionIntent;

    private boolean mPermissionRequestPending;

    UsbAccessory mAccessory;

    ParcelFileDescriptor mFileDescriptor;

    FileInputStream mInputStream;

    FileOutputStream mOutputStream;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = UsbManager.getAccessory(intent);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(TAG, "permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = UsbManager.getAccessory(intent);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
                enableControls(false);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsbManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION),
                0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mAccessory != null) {
            return mAccessory;
        } else {
            return super.onRetainNonConfigurationInstance();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        @SuppressWarnings("unused")
        final Intent intent = getIntent();

        if (mInputStream != null && mOutputStream != null) {
            return;
        }

        final UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        final UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory == null) {
            Log.d(TAG, "accessory is null");
            return;
        }

        if (mUsbManager.hasPermission(accessory)) {
            openAccessory(accessory);
        } else {
            synchronized (mUsbReceiver) {
                if (!mPermissionRequestPending) {
                    mUsbManager.requestPermission(accessory, mPermissionIntent);
                    mPermissionRequestPending = true;
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor == null) {
            Log.d(TAG, "accessory open fail");
            return;
        }

        mAccessory = accessory;
        final FileDescriptor fd = mFileDescriptor.getFileDescriptor();
        mInputStream = new FileInputStream(fd);
        mOutputStream = new FileOutputStream(fd);
        final Thread thread = new Thread(null, this, "NFCforEverybody");
        thread.start();
        Log.d(TAG, "accessory opened");
        enableControls(true);
    }

    private void closeAccessory() {
        enableControls(false);

        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    protected void enableControls(boolean enable) {
    }

    public void run() {

        // IDm(8) + dataLen(2)
        byte[] felicaHeaderBuf = new byte[10];
        byte[] dataBuf = new byte[1024];

        // see http://developer.android.com/guide/topics/usb/accessory.html
        final byte[] buffer = new byte[16384];

        try {
            while (true) {
                mOutputStream.write('p'); // polling指示
                mOutputStream.flush();
                int ret = mInputStream.read(buffer);
                if (ret < 0) {
                    break;
                }
                int consumed = 0;

                while (consumed < ret) {
                    final byte type = buffer[consumed];
                    consumed++;
                    int rest = ret - consumed;

                    switch (type) {
                        case ' ':
                            // not found
                            break;
                        case 'F': {
                            // for FeliCa
                            Log.d(TAG, "FeliCa tag found");

                            if (rest == 0) {
                                ret = mInputStream.read(buffer);
                                if (ret < 0) {
                                    return;
                                }
                                consumed = 0;
                                rest = ret;
                            }
                            if (rest < felicaHeaderBuf.length) {
                                Log.d(TAG, "buffer underflow. expected: " + felicaHeaderBuf.length
                                        + ", rest: " + rest);
                                return;
                            }
                            System.arraycopy(buffer, consumed, felicaHeaderBuf, 0,
                                    felicaHeaderBuf.length);
                            consumed += felicaHeaderBuf.length;
                            rest -= felicaHeaderBuf.length;

                            final int dataLength = ((felicaHeaderBuf[8] >> 8) & 0xff)
                                    + ((felicaHeaderBuf[9] >> 0) & 0xff);
                            if (dataBuf.length < dataLength) {
                                Log.d(TAG, "too big dataLength: " + dataLength);
                                return;
                            }

                            if (dataLength != 0 && rest == 0) {
                                ret = mInputStream.read(buffer);
                                if (ret < 0) {
                                    return;
                                }
                                consumed = 0;
                                rest = ret;
                            }
                            if (rest < dataLength) {
                                Log.d(TAG, "buffer underflow. expected: " + dataLength //
                                        + ", rest: " + rest);
                                return;
                            }
                            System.arraycopy(buffer, consumed, dataBuf, 0, dataLength);
                            consumed += dataLength;
                            rest -= dataLength;

                            final Message m = Message.obtain(mHandler, MESSAGE_FELICA);
                            m.obj = new FelicaInfoMsg(felicaHeaderBuf, 0, 8, dataBuf, 0, dataLength);
                            mHandler.sendMessage(m);
                        }
                            break;
                        default:
                            Log.d(TAG, "unknown msg: " + ret);
                            return;
                    }
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "mInputStream closed.");
        }
    }

    static final class FelicaInfoMsg {
        private final byte[] mIdm;

        private final byte[] mData;

        public FelicaInfoMsg(byte[] idm, int idmOffset, int idmLength, byte[] data, int dataOffset,
                int dataLength) {
            mIdm = new byte[idmLength];
            System.arraycopy(idm, idmOffset, mIdm, 0, idmLength);

            mData = new byte[dataLength];
            System.arraycopy(data, dataOffset, mData, 0, dataLength);
        }

        public byte[] getIdm() {
            return mIdm;
        }

        public byte[] getData() {
            return mData;
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_FELICA:
                    final FelicaInfoMsg o = (FelicaInfoMsg) msg.obj;
                    handleFelicaMessage(o);
                    break;
            }
        }
    };

    protected abstract void handleFelicaMessage(FelicaInfoMsg o);

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
