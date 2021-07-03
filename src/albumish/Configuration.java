/*
 *  Copyright (c) 2021  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Configuration {
    private File file;
    private JsonObject object;
    private boolean is_changed;

    public Configuration(File directory, String filename) {
        this.file = new File(directory, filename);
        try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
            this.object = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            this.object = new JsonObject();
        }
        this.is_changed = false;
    }

    public String get(String key) {
        JsonElement element = this.object.get(key);
        return element != null ? element.getAsString() : null;
    }

    public boolean flag(String key) {
        JsonElement element = this.object.get(key);
        return element != null ? element.getAsBoolean() : false;
    }
}
