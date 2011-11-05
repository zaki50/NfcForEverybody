/*
 * Copyright 2011 YAMAZAKI Makoto<makoto1975@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zakky.nfcforeverybody;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

public final class NfcUtil {
    @SuppressWarnings("unused")
    private final NfcUtil self = this;

    private static final Method sTagFactory;
    static {
        try {
            final Method method = Tag.class.getDeclaredMethod("createMockTag", byte[].class,
                    int[].class, Bundle[].class);
            sTagFactory = method;
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static Tag createFelicaTag(byte[] idm) {
        try {
            final Tag tag = (Tag) sTagFactory.invoke(null, new Object[] {
                    idm, //
                    new int[] {
                        4
                    /* TagTechnology.NFC_F */}, //
                    new Bundle[] {
                        new Bundle()
                    }
            });
            return tag;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static NdefMessage[] createNdefMessages(byte[] data) {
        final NdefMessage[] messages;
        if (data.length == 0) {
            messages = null;
        } else {
            NdefMessage message;
            try {
                message = new NdefMessage(data);
            } catch (FormatException e) {
                message = null;
            }
            if (message == null) {
                messages = null;
            } else {
                messages = new NdefMessage[1];
                messages[0] = message;
            }
        }
        return messages;
    }

    static boolean existsActivitiesForNdefDiscovered(Context context, String mimetype) {
        final boolean result = existsActivitiesFor(NfcAdapter.ACTION_NDEF_DISCOVERED, mimetype,
                context);
        return result;
    }

    static boolean existsActivitiesForTechDiscovered(Context context) {
        final boolean result = existsActivitiesFor(NfcAdapter.ACTION_TECH_DISCOVERED, null, context);
        return result;
    }

    static boolean existsActivitiesForTagDiscovered(Context context) {
        final boolean result = existsActivitiesFor(NfcAdapter.ACTION_TAG_DISCOVERED, null, context);
        return result;
    }

    private static boolean existsActivitiesFor(String action, String mimetype, Context context) {
        final Intent i = new Intent(action);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (mimetype != null) {
            i.setType(mimetype);
        }
        final List<ResolveInfo> resolvedActivities;
        resolvedActivities = context.getPackageManager().queryIntentActivities(i,
        /* PackageManager.GET_META_DATA*/0);
        return !resolvedActivities.isEmpty();
    }
}
