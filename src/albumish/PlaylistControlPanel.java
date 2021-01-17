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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.concurrent.TimeUnit;

public class PlaylistControlPanel extends Composite implements SelectionListener {

    private enum Cmd {
        UP_BUTTON,
        DOWN_BUTTON,
        ADD_BUTTON,
        DELETE_BUTTON,
        UNDO_BUTTON
    }

    private Jukebox jukebox;

    public PlaylistControlPanel(Jukebox jukebox, Composite parent) {
        super(parent, 0 /* SWT.BORDER */);
        this.jukebox = jukebox;

        GridLayout layout = new GridLayout(1, false);
        this.setLayout(layout);
        add_button("up25.png", Cmd.UP_BUTTON);
        add_button("down25.png", Cmd.DOWN_BUTTON);
        add_button("left25.png", Cmd.ADD_BUTTON);
        add_button("delete25.jpg", Cmd.DELETE_BUTTON);
        // add_button("Undo", Cmd.UNDO_BUTTON);
        pack();
    }

    private void add_button(String text, Cmd cmd) {
        Button button = new Button(this, SWT.PUSH);
        if (text.indexOf('.') > 0) {
            button.setImage(jukebox.get_icon(text));
        } else {
            button.setText(text);
        }
        button.setData(cmd);
        button.addSelectionListener(this);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, true);
        data.heightHint = 50;
        button.setLayoutData(data);
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.widget.getData() == null) {
            return;
        }
        switch ((Cmd) event.widget.getData()) {
        case UP_BUTTON:
            this.jukebox.move_selected_item_in_playlist(-1);
            break;
        case DOWN_BUTTON:
            this.jukebox.move_selected_item_in_playlist(+1);
            break;
        case ADD_BUTTON:
            this.jukebox.add_selection_to_playlist();
            break;
        case DELETE_BUTTON:
            this.jukebox.delete_selection_from_playlist();
            break;
        case UNDO_BUTTON:
            break;
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }
}
