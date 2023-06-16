/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractID3v1Tag;
import org.jaudiotagger.tag.id3.AbstractTag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

public class EditTagsDialog implements SelectionListener {

    private final Jukebox jukebox;
    private final int albumid;
    private final Shell dialog;
    private final Text artist_entry;
    private final Button album_artist_button;
    private final Button track_artist_button;
    private final Text album_entry;
    private final Text year_entry;
    private final Label artwork_label;
    private Artwork artwork;

    public EditTagsDialog(Jukebox jukebox, int albumid) {
        this.jukebox = jukebox;
        this.albumid = albumid;

        this.dialog = new Shell(this.jukebox.main_window, SWT.SHELL_TRIM | SWT.MODELESS);
        this.dialog.setText("Edit Album Tags");
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = layout.marginHeight = 20;
        this.dialog.setLayout(layout);
        GridData data;

        Label label = new Label(this.dialog, 0);
        label.setText("Changes will be saved in the Albumish database\n"
                + "and in each audio file's tags.");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, false, false, 2, 1);
        label.setLayoutData(data);

        label = new Label(this.dialog, 0);
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
        label.setLayoutData(data);

        label = new Label(this.dialog, 0);
        label.setText("Artist:");
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
        label.setLayoutData(data);
        this.artist_entry = new Text(this.dialog, SWT.SINGLE | SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
        this.artist_entry.setLayoutData(data);

        this.album_artist_button = new Button(this.dialog, SWT.RADIO);
        this.album_artist_button.setText("Add Album artist in addition to track artist");
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
        this.album_artist_button.setLayoutData(data);

        this.track_artist_button = new Button(this.dialog, SWT.RADIO);
        this.track_artist_button.setText("Overwrite track artist");
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1);
        this.track_artist_button.setLayoutData(data);

        label = new Label(this.dialog, 0);
        label.setText("Album:");
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
        label.setLayoutData(data);
        this.album_entry = new Text(this.dialog, SWT.SINGLE | SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
        this.album_entry.setLayoutData(data);

        label = new Label(this.dialog, 0);
        label.setText("Year:");
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
        label.setLayoutData(data);
        this.year_entry = new Text(this.dialog, SWT.SINGLE | SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
        this.year_entry.setLayoutData(data);

        label = new Label(this.dialog, 0);
        label.setText("Artwork:");
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
        label.setLayoutData(data);
        Button button = new Button(this.dialog, SWT.PUSH);
        button.setText("Load");
        button.setData(this);
        button.addSelectionListener(this);
        data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
        button.setLayoutData(data);

        label = new Label(this.dialog, 0);
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false, 2, 1);
        label.setLayoutData(data);
        this.artwork_label = label;

        Composite button_box = new Composite(this.dialog, 0);
        FillLayout fill_layout = new FillLayout(SWT.HORIZONTAL);
        fill_layout.spacing = 40;
        button_box.setLayout(fill_layout);
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false, 2, 1);
        button_box.setLayoutData(data);

        button = new Button(button_box, SWT.PUSH);
        button.setText("OK");
        button.setData(SWT.OK);
        button.addSelectionListener(this);

        button = new Button(button_box, SWT.PUSH);
        button.setText("Cancel");
        button.setData(SWT.CANCEL);
        button.addSelectionListener(this);

        Database database = this.jukebox.database;
        Album album = database.album_list.get(albumid);
        int artistid = 0, year = 0;
        boolean initialized = false;
        for (int songid : album.song_list) {
            Song song = database.song_list.get(songid);
            if (!initialized) {
                artistid = song.artistid;
                year = song.year;
                initialized = true;
            } else {
                if (song.artistid != artistid) {
                    artistid = 0;
                }
                if (song.year != year) {
                    year = 0;
                }
            }
        }
        Artist artist = database.artist_list.get(album.artistid);
        if (artist.name != null) {
            this.artist_entry.setText(artist.name);
        }
        if (artistid == 0 || artistid != album.artistid) {
            this.album_artist_button.setSelection(true);
        } else {
            this.track_artist_button.setSelection(true);
        }
        this.album_entry.setText(album.name);
        if (year > 0) {
            this.year_entry.setText(Integer.toString(year));
        }

        this.dialog.pack();
        this.dialog.open();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.widget.getData().equals(SWT.CANCEL)) {
            this.dialog.close();
            return;
        }
        if (event.widget.getData().equals(SWT.OK)) {
            edit_tags();
            this.dialog.close();
            return;
        }
        if (event.widget.getData() == this) {
            // Load artwork into memory but do not edit the MP3 file.
            Database database = this.jukebox.database;
            Album album = database.album_list.get(this.albumid);
            Artist artist = database.artist_list.get(album.artistid);
            String title = "";
            if (artist.name != null) {
                title = artist.name + " / ";
            }
            if (album.name != null) {
                title += album.name;
            }
            FileDialog dialog = new FileDialog(this.dialog, SWT.OPEN);
            dialog.setText(title);
            String filename = dialog.open();
            try {
                this.artwork = StandardArtwork.createArtworkFromFile(new File(filename));
            } catch (Exception exception) {
                // TODO show error
                return;
            }
            this.artwork_label.setText(Utils.basename(filename));
            this.artwork_label.pack();
            this.dialog.pack();
        }
    }

    private void edit_tags() {
        Database database = this.jukebox.database;
        String new_artist = this.artist_entry.getText();
        int artistid = 0;
        if (new_artist != null && !new_artist.isEmpty()) {
            artistid = database.get_artistid(new_artist);
        }
        boolean is_album_artist = this.album_artist_button.getSelection();
        String new_album = this.album_entry.getText();
        String text = this.year_entry.getText();
        int year = 0;
        try {
            year = Integer.parseInt(text);
        } catch (Exception ignore) {
        }
        Album album = database.album_list.get(this.albumid);
        if (new_album != null && !new_album.isEmpty()) {
            if (new_album.equals(album.name)) {
                new_album = null;
            } else {
                album.name = new_album;
            }
        }
        if (year > 0) {
            album.year = year;
        }
        // To correctly update the database, we would need to know if the MP3
        // file has an album_artist tag. Assume it does not.
        if (artistid > 0) {
            album.artistid = artistid;
        }
        for (int songid : album.song_list) {
            Song song = database.song_list.get(songid);
            AudioFile audio = null;
            try {
                audio = AudioFileIO.read(this.jukebox.getFile(song));
                Tag tag = audio.getTag();
                if (this.artwork != null) {
                    if (tag instanceof AbstractID3v1Tag) {
                        tag = new ID3v23Tag((AbstractTag) tag);
                        audio.setTag(tag);
                    }
                    tag.deleteArtworkField();
                    tag.setField(this.artwork);
                }
                if (new_album != null && !new_album.isEmpty()) {
                    tag.setField(FieldKey.ALBUM, new_album);
                }
                if (year > 0) {
                    tag.setField(FieldKey.YEAR, Integer.toString(year));
                }
                if (artistid > 0) {
                    if (is_album_artist) {
                        tag.setField(FieldKey.ALBUM_ARTIST, new_artist);
                    } else {
                        tag.setField(FieldKey.ARTIST, new_artist);
                    }
                }
            } catch (Exception exception) {
                System.err.println(exception.toString());
            } finally {
                try {
                    if (audio != null) {
                        audio.commit();
                    }
                    // After successfully writing mp3 file, update database.
                    if (year > 0) {
                        song.year = year;
                    }
                    if (artistid > 0) {
                        song.artistid = artistid;
                    }
                } catch (Exception exception) {
                    System.err.println(exception.toString());
                }
            }
        }
        database.is_changed = true;
        if (this.artwork != null) {
            this.jukebox.gallery.invalidate(this.albumid);
        }
        this.jukebox.invalidate_album(this.albumid);
        this.jukebox.reset_albums();
        // TODO update playlist panel
        // TODO what if artwork is loaded from external file?
    }
}
