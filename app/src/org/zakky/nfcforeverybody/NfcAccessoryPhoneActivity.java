
package org.zakky.nfcforeverybody;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.widget.TextView;

public class NfcAccessoryPhoneActivity extends BaseAccessoryActivity {

    private TextView mTagType;

    private TextView mTagId;

    private TextView mNdef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reflector);

        mTagType = (TextView) findViewById(R.id.tag_type);
        mTagId = (TextView) findViewById(R.id.tag_id);
        mNdef = (TextView) findViewById(R.id.ndef);
        
        enableControls(false);
    }

    @Override
    protected void enableControls(boolean enable) {
        super.enableControls(enable);
        
        mTagType.setEnabled(enable);
        if (!enable) {
            mTagType.setText("");
        }
        mTagId.setEnabled(enable);
        if (!enable) {
            mTagId.setText("");
        }
        mNdef.setEnabled(enable);
        if (!enable) {
            mNdef.setText("");
        }
    }

    @Override
    protected void handleFelicaMessage(FelicaInfoMsg o) {
        
        mTagType.setText("FeliCa");
        mTagId.setText(toHexString(o.getIdm()));
        final byte[] data = o.getData();
        if (data.length == 0) {
            mNdef.setText("non NDEF tag");
        } else {
            try {
                final NdefMessage message = new NdefMessage(data);
                mNdef.setText("NDEF tag");
            } catch (FormatException e) {
                mNdef.setText("invalid NDEF tag");
            }
        }
    }

    private static String toHexString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            final String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        final String result = sb.toString();
        return result;
    }
}
