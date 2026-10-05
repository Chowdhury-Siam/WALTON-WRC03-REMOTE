# WRC03 signal provenance

The functional IR timing values were recovered from the user-supplied
`Walton TV Remote_2.0.4.apks` for interoperability with their Walton
WD1-JX32-SY200 TV. The user confirmed that WRC03 works on their Redmi Note 10 Pro.
The reference app identifies WRC03 as `walton3`, uses the `q30` timing table,
and transmits at 38,000 Hz. All timing literals were independently compared
with the DEX array payloads. Only functional timing data is included here;
the original app's code, assets, brand graphics, advertising SDKs and APKs
are not distributed with this project.

The frames decode as NEC-format data with address bytes `00 BC` in wire
order, a command byte and its inverse. This is the extended address `0xBC00`
when treating the first transmitted byte as the low byte. The recorded
timings and included repeat frame/gaps are retained exactly instead of
substituting a generic encoder. Some commands have 68 values; others have 72.

| Button | Command byte |
| --- | --- |
| Power | 00 |
| Mute | 01 |
| 1–9 | 05–0D |
| OK | 10 |
| Left / Right | 11 / 12 |
| Up / Down | 13 / 14 |
| Menu | 40 |
| 0 | 42 |
| Input | 43 |
| Back/Recall | 44 |
| Home | 45 |
| Volume + / − | 48 / 49 |
| Channel + / − | 4A / 4B |

The reference app's Settings entry maps to an empty array. Guide, Info and
dash are displayed but have no handlers in that screen. This project does
not guess missing commands or transmit empty arrays.

`tools/signal_checksums.json` records the input bundle SHA-256 and the count,
duration and SHA-256 of each pattern. `tools/check_signals.py` verifies these
values, the decoded commands, duration bounds and the IR-only permission.

Actual operation of this new app on the phone and TV still requires a
physical test. No TV power or volume state can be read through this IR emitter.
