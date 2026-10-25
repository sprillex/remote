#!/bin/bash
# Raspberry Pi Zero ConfigFS multi-function composite gadget script (HID + Mass Storage + RNDIS)

set -e

# Configuration
GADGET_DIR="/sys/kernel/config/usb_gadget/pi_gadget"
mkdir -p "${GADGET_DIR}"
cd "${GADGET_DIR}" || return 1

# Gadget details
echo 0x1d6b > idVendor  # Linux Foundation
echo 0x0104 > idProduct # Multifunction Composite Gadget
echo 0x0100 > bcdDevice # v1.0.0
echo 0x0200 > bcdUSB    # USB2

mkdir -p strings/0x409
echo "fedcba9876543210" > strings/0x409/serialnumber
echo "Pi Foundation" > strings/0x409/manufacturer
echo "Pi Gadget" > strings/0x409/product

# Configuration instance
mkdir -p configs/c.1/strings/0x409
echo "Config 1: RNDIS/MassStorage/HID" > configs/c.1/strings/0x409/configuration
echo 250 > configs/c.1/MaxPower

# --- Function 1: HID ---
mkdir -p functions/hid.usb0
echo 1 > functions/hid.usb0/protocol
echo 1 > functions/hid.usb0/subclass
echo 8 > functions/hid.usb0/report_length
# Write simple keyboard descriptor
echo -ne \\x05\\x01\\x09\\x06\\xa1\\x01\\x05\\x07\\x19\\xe0\\x29\\xe7\\x15\\x00\\x25\\x01\\x75\\x01\\x95\\x08\\x81\\x02\\x95\\x01\\x75\\x08\\x81\\x03\\x95\\x05\\x75\\x01\\x05\\x08\\x19\\x01\\x29\\x05\\x91\\x02\\x95\\x01\\x75\\x03\\x91\\x03\\x95\\x06\\x75\\x08\\x15\\x00\\x25\\x65\\x05\\x07\\x19\\x00\\x29\\x65\\x81\\x00\\xc0 > functions/hid.usb0/report_desc
ln -s functions/hid.usb0 configs/c.1/

# --- Function 2: Mass Storage ---
mkdir -p functions/mass_storage.usb0
echo 1 > functions/mass_storage.usb0/stall
# Assuming a backing file exists at /pi_usb_disk.img
if [ -f /pi_usb_disk.img ]; then
    echo /pi_usb_disk.img > functions/mass_storage.usb0/lun.0/file
fi
ln -s functions/mass_storage.usb0 configs/c.1/

# --- Function 3: RNDIS ---
mkdir -p functions/rndis.usb0
# Set MAC addresses
HOST="42:63:65:12:34:56" # "HostPC"
SELF="42:63:65:12:34:57" # "BadUSB"
echo "$HOST" > functions/rndis.usb0/host_addr
echo "$SELF" > functions/rndis.usb0/dev_addr
ln -s functions/rndis.usb0 configs/c.1/

# End Gadget setup
ls /sys/class/udc > UDC
