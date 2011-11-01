
package org.zakky.nfcforeverybody;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NfcAccessoryPhoneActivity extends BaseAccessoryActivity {

    private static final String TAG = NfcAccessoryPhoneActivity.class.getSimpleName();

    private final NfcAccessoryPhoneActivity self = this;

    private TextView mNoTag;

    private TextView mTagType;

    private TextView mTagId;

    private TextView mNdef;

    private Button mSendButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reflector);

        mNoTag = (TextView) findViewById(R.id.no_tag);

        mTagType = (TextView) findViewById(R.id.tag_type);
        mTagId = (TextView) findViewById(R.id.tag_id);
        mNdef = (TextView) findViewById(R.id.ndef);

        mSendButton = (Button) findViewById(R.id.send);
        mSendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FelicaInfoMsg o = (FelicaInfoMsg) mSendButton.getTag();

                final Tag tag = NfcUtil.createFelicaTag(o.getIdm());
                final NdefMessage[] messages = NfcUtil.createNdefMessages(o.getData());

                // TODO 本当は、messages != null であれば先に ACTION_NDEF_DISCOVERED を受ける人がいないか探す

                final Intent i;
                if (NfcUtil.existsActivitiesForTechDiscovered(self)) {
                    i = new Intent(NfcAdapter.ACTION_TECH_DISCOVERED);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra(NfcAdapter.EXTRA_ID, o.getIdm());
                    i.putExtra(NfcAdapter.EXTRA_TAG, tag);
                    if (messages != null) {
                        i.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, messages);
                    }
                } else if (NfcUtil.existsActivitiesForTagDiscovered(self)) {
                    i = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra(NfcAdapter.EXTRA_ID, o.getIdm());
                    i.putExtra(NfcAdapter.EXTRA_TAG, tag);
                    if (messages != null) {
                        i.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, messages);
                    }
                } else {
                    i = null;
                }
                if (i != null) {
                    startActivity(i);
                } else {
                    Log.i(TAG, "activity not found");
                }
            }
        });

        enableControls(false);
    }

    @Override
    protected void enableControls(boolean enable) {
        super.enableControls(enable);

        mNoTag.setVisibility(enable ? View.GONE : View.VISIBLE);

        mTagType.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (!enable) {
            mTagType.setText("");
        }
        mTagId.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (!enable) {
            mTagId.setText("");
        }
        mNdef.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (!enable) {
            mNdef.setText("");
        }

        mSendButton.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (!enable) {
            mSendButton.setTag(null);
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

        mSendButton.setTag(o);
        enableControls(true);
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
