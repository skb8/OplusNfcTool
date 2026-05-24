#!/system/bin/sh
# Magisk / KernelSU customize.sh

ui_print "********************************"
ui_print "  Oplus Vendor NFC Tool"
ui_print "********************************"

MODDIR="$MODPATH"

mkdir -p "$MODDIR/tools"

if [ -f "$MODDIR/tools/nfctool.jar" ]; then
    ui_print "[+] nfctool.jar found"
else
    ui_print "[!] nfctool.jar NOT found inside module."
    ui_print "    Build it first: ./build.sh (on PC or Termux with SDK)"
    ui_print "    Then place tools/nfctool.jar into the module zip and reflash."
fi

chmod 755 "$MODDIR/system/bin/nfctool"

ui_print "[+] Module installed to $MODDIR"
ui_print "[+] Reboot or run: su -c nfctool --help"
