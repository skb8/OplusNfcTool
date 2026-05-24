package com.oplus.nfctool;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Console utility for interacting with Oplus/Realme Vendor NFC Binder interface.
 * Uses raw Binder transactions matching IVendorNfcAdapter.Stub transactions.
 */
public class NfcTool {

    private static final String DESCRIPTOR = "com.vendor.nfc.IVendorNfcAdapter";

    // Transaction codes from IVendorNfcAdapter.Stub
    private static final int TRANSACTION_getCplc = 1;
    private static final int TRANSACTION_getNfcc = 2;
    private static final int TRANSACTION_setABFListenTechMask = 3;
    private static final int TRANSACTION_registerRfFieldLevelCallback = 4;
    private static final int TRANSACTION_unregisterRfFieldLevelCallback = 5;
    private static final int TRANSACTION_hasFeature = 6;
    private static final int TRANSACTION_eseReset = 7;
    private static final int TRANSACTION_enableNfcRecovery = 8;
    private static final int TRANSACTION_activateEseSwp = 9;
    private static final int TRANSACTION_setConfig = 10;
    private static final int TRANSACTION_setPoweroffTask = 11;
    private static final int TRANSACTION_setNfcWorkMode = 12;
    private static final int TRANSACTION_getNfcWorkMode = 13;
    private static final int TRANSACTION_notifyCardChange = 14;
    private static final int TRANSACTION_enableNfcShareMode = 15;
    private static final int TRANSACTION_setHceTypeAConfig = 16;
    private static final int TRANSACTION_setDH85Config = 17;
    private static final int TRANSACTION_saveUpdateConfig = 18;
    private static final int TRANSACTION_getParamsList = 19;

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        try {
            IBinder vendorBinder = getVendorNfcBinder();
            if (vendorBinder == null) {
                System.err.println("[-] Failed to obtain vendor NFC binder.");
                System.err.println("    Ensure this is an Oppo/Realme/OnePlus device with vendor NFC extensions.");
                System.exit(1);
            }

            String cmd = args[0];
            switch (cmd) {
                case "cplc":
                    System.out.println(getCplc(vendorBinder));
                    break;
                case "nfcc":
                    System.out.println(getNfcc(vendorBinder));
                    break;
                case "ese-reset":
                    System.out.println("eSE Reset: " + eseReset(vendorBinder));
                    break;
                case "hce-uid":
                    if (args.length < 2) {
                        System.err.println("Usage: nfctool hce-uid <hex_uid> [atqa] [sak]");
                        System.err.println("Example: nfctool hce-uid DEADBEEF");
                        System.exit(1);
                    }
                    byte[] uid = hexToBytes(args[1]);
                    byte[] atqa = (args.length > 2) ? hexToBytes(args[2]) : new byte[]{(byte) 0x00, (byte) 0x04};
                    byte[] sak = (args.length > 3) ? hexToBytes(args[3]) : new byte[]{(byte) 0x20};
                    boolean ok = setHceTypeAConfig(vendorBinder, true, atqa, sak, uid);
                    System.out.println("Set HCE Type A Config: " + ok);
                    break;
                case "work-mode":
                    if (args.length < 2) {
                        System.out.println("Current work mode: " + getNfcWorkMode(vendorBinder));
                    } else {
                        int mode = Integer.parseInt(args[1]);
                        System.out.println("Set work mode: " + setNfcWorkMode(vendorBinder, mode));
                    }
                    break;
                case "feature":
                    if (args.length < 2) {
                        System.err.println("Usage: nfctool feature <name>");
                        System.err.println("Known: ESE_RESET, SET_CONFIG, ACCESSCARD_TUNNING, CONTENT_PROVIER_CALL, SHUTDOWN_AID_ACTIVE_DEACTIVE");
                        System.exit(1);
                    }
                    System.out.println("Feature '" + args[1] + "': " + hasFeature(vendorBinder, args[1]));
                    break;
                case "ese-swp":
                    if (args.length < 2) {
                        System.err.println("Usage: nfctool ese-swp <true|false>");
                        System.exit(1);
                    }
                    boolean eseEn = Boolean.parseBoolean(args[1]);
                    System.out.println("Activate eSE SWP: " + activateEseSwp(vendorBinder, eseEn));
                    break;
                case "share-mode":
                    if (args.length < 2) {
                        System.err.println("Usage: nfctool share-mode <true|false>");
                        System.exit(1);
                    }
                    boolean sm = Boolean.parseBoolean(args[1]);
                    System.out.println("NFC Share Mode: " + enableNfcShareMode(vendorBinder, sm));
                    break;
                case "recovery":
                    if (args.length < 2) {
                        System.err.println("Usage: nfctool recovery <true|false>");
                        System.exit(1);
                    }
                    boolean rec = Boolean.parseBoolean(args[1]);
                    System.out.println("NFC Recovery: " + enableNfcRecovery(vendorBinder, rec));
                    break;
                case "help":
                default:
                    printUsage();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void printUsage() {
        System.out.println("Oplus Vendor NFC Tool v1.0");
        System.out.println("");
        System.out.println("Commands:");
        System.out.println("  cplc                          Get CPLC from eSE");
        System.out.println("  nfcc                          Get NFCC info string");
        System.out.println("  ese-reset                     Reset eSE chip");
        System.out.println("  hce-uid <hex> [atqa] [sak]    Set custom HCE UID (e.g., DEADBEEF)");
        System.out.println("  work-mode [int]               Get or set NFC work mode");
        System.out.println("  feature <name>                Check vendor feature (e.g., ESE_RESET)");
        System.out.println("  ese-swp <true|false>          Activate/deactivate eSE SWP");
        System.out.println("  share-mode <true|false>       Enable/disable NFC share mode");
        System.out.println("  recovery <true|false>         Enable/disable NFC recovery");
    }

    /**
     * Obtains the vendor-specific NFC binder by calling the hidden
     * getNfcAdapterVendorInterface("vendor") on the main NFC service.
     */
    static IBinder getVendorNfcBinder() throws Exception {
        Class<?> smCls = Class.forName("android.os.ServiceManager");
        Object nfc = smCls.getMethod("getService", String.class).invoke(null, "nfc");
        if (nfc == null) {
            return null;
        }
        IBinder nfcBinder = (IBinder) nfc;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("android.nfc.INfcAdapter");
            data.writeString("vendor");
            nfcBinder.transact(6, data, reply, 0);
            reply.readException();
            return reply.readStrongBinder();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    static String getCplc(IBinder b) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            b.transact(TRANSACTION_getCplc, d, r, 0);
            r.readException();
            return r.readString();
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static String getNfcc(IBinder b) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            b.transact(TRANSACTION_getNfcc, d, r, 0);
            r.readException();
            return r.readString();
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static boolean eseReset(IBinder b) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            b.transact(TRANSACTION_eseReset, d, r, 0);
            r.readException();
            return r.readInt() != 0;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static boolean enableNfcRecovery(IBinder b, boolean enable) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            d.writeInt(enable ? 1 : 0);
            b.transact(TRANSACTION_enableNfcRecovery, d, r, 0);
            r.readException();
            return r.readInt() != 0;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static boolean activateEseSwp(IBinder b, boolean enable) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            d.writeInt(enable ? 1 : 0);
            b.transact(TRANSACTION_activateEseSwp, d, r, 0);
            r.readException();
            return r.readInt() != 0;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static boolean setNfcWorkMode(IBinder b, int mode) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            d.writeInt(mode);
            b.transact(TRANSACTION_setNfcWorkMode, d, r, 0);
            r.readException();
            return r.readInt() != 0;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static int getNfcWorkMode(IBinder b) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            b.transact(TRANSACTION_getNfcWorkMode, d, r, 0);
            r.readException();
            return r.readInt();
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static boolean enableNfcShareMode(IBinder b, boolean enable) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            d.writeInt(enable ? 1 : 0);
            b.transact(TRANSACTION_enableNfcShareMode, d, r, 0);
            r.readException();
            return r.readInt() != 0;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static boolean hasFeature(IBinder b, String name) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            d.writeString(name);
            b.transact(TRANSACTION_hasFeature, d, r, 0);
            r.readException();
            return r.readInt() != 0;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static boolean setHceTypeAConfig(IBinder b, boolean enabled, byte[] atqa, byte[] sak, byte[] uid) throws RemoteException {
        Parcel d = Parcel.obtain();
        Parcel r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            d.writeInt(enabled ? 1 : 0);
            d.writeByteArray(atqa);
            d.writeByteArray(sak);
            d.writeByteArray(uid);
            b.transact(TRANSACTION_setHceTypeAConfig, d, r, 0);
            r.readException();
            return r.readInt() != 0;
        } finally {
            r.recycle();
            d.recycle();
        }
    }

    static byte[] hexToBytes(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
