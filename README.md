# Albumish Music Organizer and MP3 Player

Albumish is a classic desktop application for those of us who built
our music library by ripping CDs to local disk. It organizes,
displays, and plays music files stored locally. You can build
playlists, edit tags, rip CDs, sync to MTP devices and flash drives,
and "turn off" songs that should not be played or synced by default.

(That last feature was the reason that I wrote Albumish. When I got my
first iPhone a million years ago, I had a collection of about a
hundred CDs. I wanted to select the songs to sync to the phone, but I
didn't like the iTunes interface for doing so. I wanted to focus on
one album -- consider each song, and select the songs that I really
liked. Then move on to the next album. So I wrote Albumish as a song
selector. Of course, it gradually grew into a full-fledged MP3 player
jukebox application.)

## Building and Running

Albumish is a Java application. It uses SWT for graphics, and JLayer
for MP3 processing, so it should run on all popular platforms.

Albumish actually has six dependencies. In addition to SWT and JLayer,
it requires `gson`, `imgscalr`, `jaudiogger`, and `jna`. Currently,
you need to download all six of them, then tell `build.xml` where to
find them so that you can compile albumish, and then tell
`albumish.sh` where to find them so that you can run it. I guess I
should use Maven?

## Users Guide

When you first run Albumish, it displays an empty library. To add
albums, select Add Folder to Library. Select a directory that is the
root of a tree containing MP3 files, such as your home directory, or
your Music directory. Albumish will search the directory tree for
music files.

To scroll left and right through your albums, dragging does not work
yet, and the cursor keys don't work yet, so use F11 and F12.

To play an album, double-click on the album cover, or on any of the
album's songs. This updates the transient "auto playlist" with the
selected songs from the album.

To create a permanent playlist (with any songs from any albums), use
the Playlist menu.

If you unclick the check box next to a song, then that song will no
longer be included in auto-playlists (although you can still manually
add it to playlists), and it will not be synced to devices.

## Album Cover Art

Depending on how you ripped or downloaded your music files, the files
may not contain cover art. Albumish does not automatically download
cover art. The reason is that some of us are "ridiculously obsessive"
about album cover art. So Albumish allows you to manually find and
select the best images, and to maintain a cover art library in
parallel with your music library.

Download your favorite album cover art image file to local disk. Then,
right click on the album and select "Load Album Art". Select the file
that you downloaded. Albumish will copy the file to the directory
"Pictures/covers" in your home directory. After that, you can delete
the downloaded image file, since Albumish only reads the copy in the
"covers" directory.

## Sync to MTP Device and USB Device

Any reasonably new Android phone can be connected to your computer and
then accessed using the MTP protocol. Unfortunately, many phones don't
make it easy or intuitive to do so. Once you have your phone connected
with MTP, Albumish can read and write your phone's local music library
(slowly) using the MTP client built into the Linux desktop.

It would be nice to expand this functionality to support other
operating systems, and other desktops, and iPhones. Volunteers?

Of course, if a USB flash drive is mounted on the local file system,
then Albumish can sync to the device using the local file system.
