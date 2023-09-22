package es.karmadev.locklogin.migrator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class Configuration {

    private final JsonObject object;

    public Configuration() throws IOException {
        Gson gson = new GsonBuilder().create();

        Path config = Paths.get("config.json");
        if (!Files.exists(config)) {
            try (InputStream stream = Configuration.class.getResourceAsStream("/config.json")) {
                if (stream == null) throw new RuntimeException("Cannot create configuration file");

                Files.createFile(config);
                Files.copy(stream, config, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        JsonElement element = gson.fromJson(Files.newBufferedReader(config), JsonElement.class);
        if (element == null || !element.isJsonObject()) throw new IOException("Cannot read configuration file");

        this.object = element.getAsJsonObject().get("settings").getAsJsonObject();
    }

    public int getType() {
        String type = object.get("type").getAsString();
        return (type.equalsIgnoreCase("mysql") ? 0 : 1);
    }

    public String getHost() {
        return object.get("host").getAsString();
    }

    public int getPort() {
        return object.get("port").getAsInt();
    }

    public String getDatabase() {
        return object.get("database").getAsJsonObject().get("name").getAsString();
    }

    public String getUser() {
        return object.get("user").getAsString();
    }

    public String getPassword() {
        return object.get("password").getAsString();
    }

    public String getUserTable() {
        return object.get("tables").getAsJsonObject().get("user_table").getAsJsonObject()
                .get("name").getAsString();
    }

    public String getAccountTable() {
        return object.get("")
    }
}
