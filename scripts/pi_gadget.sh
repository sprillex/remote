#!/bin/bash

# Exit on error
set -e

# Target requirements: ConfigFS multi-function composite gadget script (HID + Mass Storage + RNDIS)

# Make sure we run as root
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

# Load libcomposite
modprobe libcomposite

# Gadget ConfigFS directory
GADGET_DIR=/sys/kernel/config/usb_gadget/g1
mkdir -p "$GADGET_DIR"
cd "$GADGET_DIR"

# Set USB descriptors
echo 0x1d6b > idVendor  # Linux Foundation
echo 0x0104 > idProduct # Multifunction Composite Gadget
echo 0x0200 > bcdUSB    # USB 2.0
echo 0x0100 > bcdDevice # v1.0.0

# Strings
mkdir -p strings/0x409
echo "fedcba9876543210" > strings/0x409/serialnumber
echo "Manufacturer" > strings/0x409/manufacturer
echo "Multi-function Gadget" > strings/0x409/product

# Configuration
mkdir -p configs/c.1/strings/0x409
echo "Config 1: ECM/RNDIS, Mass Storage, HID" > configs/c.1/strings/0x409/configuration
echo 250 > configs/c.1/MaxPower

# --- RNDIS ---
mkdir -p functions/rndis.usb0
# Set up mac address
HOST="42:f2:97:74:b2:92"
SELF="42:f2:97:74:b2:93"
echo "$HOST" > functions/rndis.usb0/dev_addr
echo "$SELF" > functions/rndis.usb0/host_addr
# OS Descriptors (required by Windows)
mkdir -p os_desc
echo 1 > os_desc/use
echo 0xcd > os_desc/b_vendor_code
echo MSFT100 > os_desc/qw_sign
# Windows needs this to automatically load RNDIS drivers
mkdir -p functions/rndis.usb0/os_desc/interface.rndis
echo RNDIS > functions/rndis.usb0/os_desc/interface.rndis/compatible_id
echo 5162001 > functions/rndis.usb0/os_desc/interface.rndis/sub_compatible_id

# --- Mass Storage ---
mkdir -p functions/mass_storage.usb0
# We need a backing file. We'll check if one exists, else default to /piusb.bin
BACKING_FILE="/piusb.bin"
if [ ! -f "$BACKING_FILE" ]; then
    # Create a simple 10MB backing file if it doesn't exist
    dd if=/dev/zero of="$BACKING_FILE" bs=1M count=10
    mkfs.vfat "$BACKING_FILE"
fi
echo "$BACKING_FILE" > functions/mass_storage.usb0/lun.0/file
echo 1 > functions/mass_storage.usb0/lun.0/removable
echo 0 > functions/mass_storage.usb0/lun.0/cdrom

# --- HID (Keyboard/Mouse) ---
mkdir -p functions/hid.usb0
echo 1 > functions/hid.usb0/protocol
echo 1 > functions/hid.usb0/subclass
echo 8 > functions/hid.usb0/report_length
# Write standard keyboard report descriptor
echo -ne \\x05\\x01\\x09\\x06\\xa1\\x01\\x05\\x07\\x19\\xe0\\x29\\xe7\\x15\\x00\\x25\\x01\\x75\\x01\\x95\\x08\\x81\\x02\\x95\\x01\\x75\\x08\\x81\\x03\\x95\\x05\\x75\\x01\\x05\\x08\\x19\\x01\\x29\\x05\\x91\\x02\\x95\\x01\\x75\\x03\\x91\\x03\\x95\\x06\\x75\\x08\\x15\\x00\\x25\\x65\\x05\\x07\\x19\\x00\\x29\\x65\\x81\\x00\\xc0 > functions/hid.usb0/report_desc

# Link functions to configuration
ln -s functions/rndis.usb0 configs/c.1/
ln -s functions/mass_storage.usb0 configs/c.1/
ln -s functions/hid.usb0 configs/c.1/

# Link OS descriptors
ln -s configs/c.1 os_desc

# Bind to UDC
ls /sys/class/udc > UDC

echo "Gadget initialized successfully."
