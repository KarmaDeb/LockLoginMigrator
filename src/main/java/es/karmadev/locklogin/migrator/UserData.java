package es.karmadev.locklogin.migrator;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor @Getter
public class UserData {

    private final String user;
    private final UUID uniqueId;
    private final String password;
    private final boolean geyser;

}
