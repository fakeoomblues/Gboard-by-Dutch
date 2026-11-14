# Messages branding artwork

The supplied "Messages AA" artwork (Messages' speech bubbles with an Android Auto badge) is
converted into an adaptive launcher icon. Faint stray pixels around the shape (alpha 8 or less)
are removed, and the shape is scaled so its farthest visible pixel sits inside the 66dp safe circle
of the 108dp canvas, on a white background like stock Messages. Themed icons keep Messages' own
monochrome layer. A 192×192 legacy launcher PNG is included. `sources.json` records the original
filename and SHA-256 digest.

Google Messages and Android Auto are trademarks of Google LLC. This is user-selected launcher
artwork for a patched copy and does not imply affiliation.
