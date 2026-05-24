#!/bin/bash
set -e

# Build script for OplusNfcTool
# Requires: javac and d8 (or dx) from Android SDK Build-Tools

SCRIPTDIR="$(cd "$(dirname "$0")" && pwd)"
SRCDIR="$SCRIPTDIR/src"
OUTDIR="$SCRIPTDIR/build"
JAROUT="$SCRIPTDIR/tools/nfctool.jar"

# --- Locate android.jar ---
ANDROID_JAR="${ANDROID_JAR:-}"
if [ -z "$ANDROID_JAR" ] || [ ! -f "$ANDROID_JAR" ]; then
    for path in \
        "$HOME/android-sdk/platforms/android-33/android.jar" \
        "$HOME/android-sdk/platforms/android-34/android.jar" \
        "$HOME/Android/Sdk/platforms/android-33/android.jar" \
        "$HOME/Android/Sdk/platforms/android-34/android.jar" \
        "/usr/share/android-sdk/platforms/android-33/android.jar" \
        "/opt/android-sdk/platforms/android-33/android.jar"
    do
        if [ -f "$path" ]; then
            ANDROID_JAR="$path"
            break
        fi
    done
fi

if [ -z "$ANDROID_JAR" ] || [ ! -f "$ANDROID_JAR" ]; then
    echo "[-] android.jar not found."
    echo "    Install Android SDK Platform (API 33+) and export its path:"
    echo "    export ANDROID_JAR=/path/to/android.jar"
    exit 1
fi

echo "[+] Using android.jar: $ANDROID_JAR"

# --- Clean & prepare ---
rm -rf "$OUTDIR"
mkdir -p "$OUTDIR"
mkdir -p "$(dirname "$JAROUT")"

# --- Compile ---
echo "[+] Compiling Java sources (targeting Java 8)..."
javac -source 1.8 -target 1.8 -cp "$ANDROID_JAR" -d "$OUTDIR" "$SRCDIR/com/oplus/nfctool/NfcTool.java"

# --- Dex ---
echo "[+] Converting to DEX..."
if command -v d8 >/dev/null 2>&1; then
    d8 --output "$OUTDIR" $(find "$OUTDIR" -name "*.class")
    mv "$OUTDIR/classes.dex" "$OUTDIR/classes.dex.tmp" 2>/dev/null || true
elif command -v dx >/dev/null 2>&1; then
    dx --dex --output="$OUTDIR/classes.dex" "$OUTDIR"
else
    echo "[-] Neither d8 nor dx found in PATH."
    echo "    Install Android SDK Build-Tools:"
    echo "    sdkmanager 'build-tools;34.0.0'"
    exit 1
fi

if [ ! -f "$OUTDIR/classes.dex" ] && [ -f "$OUTDIR/classes.dex.tmp" ]; then
    mv "$OUTDIR/classes.dex.tmp" "$OUTDIR/classes.dex"
fi

if [ ! -f "$OUTDIR/classes.dex" ]; then
    echo "[-] Failed to produce classes.dex"
    exit 1
fi

# --- Package ---
echo "[+] Packaging nfctool.jar..."
cd "$OUTDIR"
jar cf "$JAROUT" classes.dex
cd "$SCRIPTDIR"
rm -rf "$OUTDIR"

echo "[+] Build complete: $JAROUT"
echo "[+] Next: zip the module folder and install via Magisk/KSU Manager."
