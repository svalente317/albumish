/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DeleteAlbumDialog implements SelectionListener {

    private Jukebox player;
    private int albumid;
    private Shell dialog;

    public DeleteAlbumDialog(Jukebox player, int albumid) {
        this.player = player;
        this.albumid = albumid;
        Album album = player.database.album_list.get(albumid);

        this.dialog = new Shell(player.main_window, SWT.SHELL_TRIM | SWT.MODELESS);
        this.dialog.setText("Delete Album From Library");
        GridLayout layout = new GridLayout(2, true);
        layout.marginWidth = layout.marginHeight = 20;
        layout.verticalSpacing = 20;
        this.dialog.setLayout(layout);
        GridData data;

        Label label = new Label(this.dialog, 0);
        label.setText("Delete album \"" + album.name + "\" from library?");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false, 2, 1);
        label.setLayoutData(data);

        Button button = new Button(this.dialog, SWT.PUSH);
        button.setText("OK");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false);
        data.widthHint = 60;
        button.setLayoutData(data);
        button.setData(SWT.OK);
        button.addSelectionListener(this);

        button = new Button(this.dialog, SWT.PUSH);
        button.setText("Cancel");
        button.setLayoutData(data);
        button.setData(SWT.CANCEL);
        button.addSelectionListener(this);

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
            // This fails to select the correct album.
            this.player.database.delete_album(this.albumid);
            this.player.reset_albums();
            this.dialog.close();
            return;
        }
    }
}
