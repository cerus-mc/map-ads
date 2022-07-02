# Transition recording format

The format for transition recordings is as follows:

```
{
  Header h,         // The header - See the 'Header' section
  Frame[h.frames] f // Array of frames - The amount can be found in the header
                    // Please note: The frame array is gzipped!
}
```

### Header

The header is always 15 bytes long.

```
Header {
  char[6] magic, // 6 bytes magic value, always MAPADS
  byte version,  // Currently 1
  int32 width,   // Width of the recorded transition
  int32 height,  // Height of the recorded transition
  int32 frames   // The amount of recorded frames
}
```

### Frame

Each frame can have a variable length.

```
Frame {
  int32 len,     // The amount of bytes in the following array
  byte[len] data // Array of colors
}
```
