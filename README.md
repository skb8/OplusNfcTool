# Oplus Vendor NFC Tool (Magisk / KernelSU Module)

A console utility for interacting with the vendor-specific NFC framework on **Oppo, Realme, and OnePlus** (BBK) devices directly via IPC/Binder.

This tool bypasses OS-level API restrictions to manage secure elements (eSE), configure HCE parameters (UID/SAK/ATQA), and toggle low-level radio modes using the hidden `IVendorNfcAdapter` interface.

## Features

- **Get CPLC** — Retrieve the Card Production Life Cycle identifier from the embedded Secure Element (eSE) (`cplc`).
- **Get NFCC Info** — Read the NFC Controller info string (`nfcc`).
- **Reset eSE** — Hardware/software reset of the secure element chip (`ese-reset`).
- **Custom HCE UID** — Emulate Type A cards with custom UID, SAK, and ATQA on the fly (`hce-uid`). Great for badge cloning.
- **NFC Work Modes** — Retrieve or configure internal operational modes (`work-mode`).
- **NFC Share Mode** — Toggle NFC peer sharing/beaming (`share-mode`).
- **NFC Recovery** — Toggle low-level firmware recovery loops (`recovery`).
- **eSE SWP Activation** — Activate/deactivate Single Wire Protocol to eSE (`ese-swp`).
- **Feature Query** — Check support status of vendor features (`feature`).

## Requirements

- Android device with root access (**Magisk** or **KernelSU**).
- An **Oppo, Realme, or OnePlus** (BBK) device containing the proprietary `com.vendor.nfc.IVendorNfcAdapter` interface.
- **Tested & Verified Chipset:** **NXP SN220T** (found in modern OnePlus/Realme flagship devices).

## Installation

You can download the pre-packaged module zip directly from the Releases tab or build it manually:

1. Flash the `OplusNfcTool-v1.0.zip` via **Magisk** or **KernelSU** app.
2. Reboot the device.
3. Open a terminal (e.g. Termux or `adb shell`) and run commands via `su`.

---

## How to Build

### On PC (Linux / macOS / WSL)
Make sure you have Android SDK Platform (API 33+) and JDK installed.
```bash
export ANDROID_JAR=$HOME/Android/Sdk/platforms/android-33/android.jar
./build.sh
```

### In Termux (On-device)
```bash
pkg install ecj dx android-sdk
export ANDROID_JAR=$PREFIX/share/android-sdk/platforms/android-33/android.jar
./build.sh
```

Once built, zip the directory contents (excluding the parent folder itself):
```bash
cd OplusNfcTool
zip -r ../OplusNfcTool-v1.0.zip .
```

---

## Usage

All commands require root privileges:

```bash
# Print help screen
su -c nfctool

# Get CPLC data from eSE
su -c nfctool cplc
# Example Output:
# 9F7F2A479009DC4703D0430600418613900741042048100000005100000449307D582590020000000000365233

# Get NFC Controller name/info
su -c nfctool nfcc
# Example Output:
# SN220T

# Reset secure element (eSE)
su -c nfctool ese-reset
# Example Output:
# eSE Reset: true

# Emulate custom UID (HCE Type A) - Change DEADBEEF to target UID
su -c nfctool hce-uid DEADBEEF
# Example Output:
# Set HCE Type A Config: true

# Emulate custom UID with custom ATQA and SAK
su -c nfctool hce-uid DEADBEEF 0004 20

# Query NFC work mode
su -c nfctool work-mode

# Set NFC work mode to 1
su -c nfctool work-mode 1

# Toggle NFC share mode
su -c nfctool share-mode true

# Check support for a specific vendor feature
su -c nfctool feature ESE_RESET
```

## Technical Details & Background

This utility was developed by reversing the proprietary vendor NFC framework components (typically found in `/system/framework/nfcvendorlib.jar`) on Oppo/Realme/OnePlus devices, which includes:
- `com.vendor.nfc.VendorNfcAdapter` & `IVendorNfcAdapter` (General vendor control & HCE spoofing)
- `com.nxp.nfc.NxpNfcAdapter` (NXP-specific extras & Routing)
- `com.st.android.nfc_extensions.NfcAdapterStExtensions` (STMicroelectronics low-level гейты, пайпы и RF конфигурации)

Instead of relying on proprietary stub libraries at compile time, this utility communicates with the NFC stack purely via IPC.

1. It acquires the main `nfc` binder service via reflection from `android.os.ServiceManager`.
2. It invokes the hidden method `getNfcAdapterVendorInterface("vendor")` (transaction code 6).
3. It performs low-level `transact()` calls directly on the returned vendor Binder, matching transaction IDs parsed from `IVendorNfcAdapter.Stub`.

This design ensures the tool compiles easily on generic Android SDK platforms without vendor-specific binaries, yet retains maximum capabilities on targets.

## Disclaimer

Use this tool at your own risk. Manipulating eSE, hardware registers, and forcing RF work modes may lead to system instability, high battery drain, or bricking payment credentials.
