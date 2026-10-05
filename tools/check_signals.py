#!/usr/bin/env python3
"""Verify the WRC03 conversion against checksums and decoded button commands."""
import hashlib
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/dev/siam/quietremote/Wrc03.java').read_text()
baseline = json.loads((root / 'tools/signal_checksums.json').read_text())
patterns = {
    key: list(map(int, re.findall(r'\d+', values)))
    for key, values in re.findall(r'PATTERNS\.put\("([^"]+)", new int\[\] \{([^}]+)\}', source)
}
commands = {
    'power': 0x00, 'input': 0x43, 'menu': 0x40, 'mute': 0x01,
    'back': 0x44, 'home': 0x45, 'channel_up': 0x4a, 'channel_down': 0x4b,
    'volume_up': 0x48, 'volume_down': 0x49, 'up': 0x13, 'down': 0x14,
    'left': 0x11, 'right': 0x12, 'ok': 0x10, '0': 0x42,
    **{str(i): i + 4 for i in range(1, 10)},
}
assert set(patterns) == set(commands) == set(baseline['patterns'])
assert 'FREQUENCY = 38000' in source
for key, values in patterns.items():
    reference = baseline['patterns'][key]
    assert len(values) == reference['count'], key
    assert sum(values) == reference['duration_us'] < 2_000_000, key
    assert all(0 < value < 200_000 for value in values), key
    assert hashlib.sha256(','.join(map(str, values)).encode()).hexdigest() == reference['sha256'], key
    assert 8500 < values[0] < 9500 and 4000 < values[1] < 5000, key
    frame = sum((values[3 + bit * 2] > 1100) << bit for bit in range(32))
    address_low, address_high, command, inverse = frame.to_bytes(4, 'little')
    assert (address_low, address_high) == (0x00, 0xbc), key
    assert command == commands[key] and command ^ inverse == 0xff, key

manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
permissions = [e.attrib['{http://schemas.android.com/apk/res/android}name']
               for e in manifest.findall('uses-permission')]
assert permissions == ['android.permission.TRANSMIT_IR'], permissions
print('PASS: all 25 WRC03 patterns, checksums, NEC commands, duration bounds and IR-only permission')
