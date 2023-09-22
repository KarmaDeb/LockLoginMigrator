package es.karmadev.locklogin.migrator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import es.karmadev.locklogin.migrator.configuration.Configuration;
import es.karmadev.locklogin.migrator.configuration.sql.Database;
import es.karmadev.locklogin.migrator.configuration.sql.Table;
import es.karmadev.locklogin.migrator.configuration.sql.column.TableColumn;
import es.karmadev.locklogin.migrator.configuration.sql.column.TableRelation;
import es.karmadev.locklogin.migrator.util.argument.ClassArgument;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * LockLogin migrator
 */
public class Migrator {

    @ClassArgument(
            name = "--source",
            description = "The database file to migrate from",
            help = "Usages:\n\t--source \"./data.db\"\n\t--source \"C:\\Users\\user\\Desktop\\Servers\\plugins\\Data\\data.db\""
    )
    private String source;

    @ClassArgument(
            name = "--target",
            description = "The directory name in where to place the migrated files",
            help = "Usages:\n\t--target \"./output\"\n\t--target \"C:\\Users\\user\\Desktop\\Servers\\plugins\\Data\\output\""
    )
    @SuppressWarnings("FieldMayBeFinal")
    private String destination = "output";

    private HikariDataSource dataSource;

    /**
     * Start the migration
     */
    public void start() throws Exception {
        Configuration configuration = new Configuration();
        int type = configuration.getType();

        if (type == 1 && source == null) {
            throw new IOException("Cannot proceed with migration, database type is sqlite but no source file was provided");
        }
        source = source.replaceAll("\\\\", "/");

        Database database = configuration.getDatabase();

        String name = database.getName();
        String host = database.getHost();
        int port = database.getPort();
        String user = database.getUser();
        String pass = database.getPassword();
        boolean ssl = configuration.useSSL();
        boolean certificates = configuration.verifyCertificates();

        HikariConfig config = new HikariConfig();
        config.setPoolName("locklogin-migrator");

        if (type == 0) {
            System.out.printf("Preparing to connect to %s:%d@%s, [user: %s] using password (%b)%n", host, port, name, user, !pass.isEmpty());

            config.setJdbcUrl(
                    String.format(
                            "jdbc:mysql://%s:%d/%s?useSSL=%b?verifyServerCertificate=%b",
                            host, port, database, ssl, certificates
                    )
            );

            config.setUsername(user);
            if (!pass.isEmpty()) {
                config.setPassword(pass);
            }
        } else {
            Path databaseFile = Paths.get("");
            if (source.contains("/")) {
                String[] data = source.split("/");
                for (String str : data) {
                    if (str.matches("[A-Z]:")) {
                        databaseFile = databaseFile.resolve(str + "/"); //Windows letter
                    } else {
                        databaseFile = databaseFile.resolve(str);
                    }
                }
            } else {
                databaseFile = Paths.get(source);
            }

            if (!Files.exists(databaseFile)) {
                throw new FileNotFoundException("Failed to find database file (" + databaseFile + ")");
            }

            System.out.println("Preparing to connect to " + databaseFile);
            config.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        }

        dataSource = new HikariDataSource(config);
        System.out.println("Fetching tables, please wait...");

        boolean userTableFound = false;
        boolean accTableFound = false;
        {
            Connection connection = null;
            Statement statement = null;
            try {
                connection = dataSource.getConnection();

                DatabaseMetaData meta = connection.getMetaData();
                ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});

                while (tables.next()) {
                    String tbName = tables.getString("TABLE_NAME");
                    if (tbName.equals(database.getUserTable())) {
                        userTableFound = true;
                        System.out.println("Found users table: " + tbName);
                    }
                    if (tbName.equals(database.getAccTable())) {
                        accTableFound = true;
                        System.out.println("Found accounts table: " + tbName);
                    }
                }
            } finally {
                close(connection, statement);
            }
        }

        if (!userTableFound) {
            throw new RuntimeException("Couldn't find required table (users table): " + database.getUserTable());
        }
        if (!accTableFound) {
            throw new RuntimeException("Couldn't find required table (accounts table): " + database.getAccTable());
        }

        Table usersTable = database.getTable("user:" + database.getUserTable());
        Table accountsTable = database.getTable("account:" + database.getAccTable());

        TableColumn idColumn = usersTable.getColumn("id");
        TableColumn nameColumn = usersTable.getColumn("name");
        TableColumn uuidColumn = usersTable.getColumn("uniqueid");
        TableColumn passwordColumn = accountsTable.getColumn("password");
        //TableColumn saltColumn = accountsTable.getColumn("salt");

        if (nameColumn == null || !nameColumn.isEnabled()) {
            throw new RuntimeException("Couldn't find required column in configuration (type: name|table: user_table)");
        }
        if (passwordColumn == null || !passwordColumn.isEnabled()) {
            throw new RuntimeException("Couldn't find required column in configuration (type: password|table: account_table)");
        }

        boolean generateUUID = false;
        if (uuidColumn == null || !uuidColumn.isEnabled()) {
            generateUUID = true;
        }

        String query;
        if (generateUUID) {
            if (idColumn != null && idColumn.isEnabled() && idColumn.getRelation() != null) {
                TableRelation relation = idColumn.getRelation();

                Table destination = relation.getDestination().getTable();
                Table source = relation.getOrigin().getTable();

                query = String.format("SELECT `%s`.`%s` AS '%s', `%s`.`%s` AS '%s' FROM `%s` INNER JOIN `%s` ON `%s`.`%s` = `%s`.`%s` ORDER BY `%s`.`%s`",
                        source.getName(),
                        nameColumn.getName(),
                        nameColumn.getName(),

                        destination.getName(),
                        passwordColumn.getName(),
                        passwordColumn.getName(),

                        source.getName(),

                        destination.getName(),

                        source.getName(),
                        idColumn.getName(),

                        destination.getName(),
                        relation.getDestination().getName(),

                        source.getName(),
                        nameColumn.getName());
            } else {
                query = String.format("SELECT `%s`,`%s` FROM `%s` ORDER BY `%s`", nameColumn.getName(), passwordColumn.getName(), usersTable.getName(), nameColumn.getName());
            }
        } else {
            if (idColumn != null && idColumn.isEnabled() && idColumn.getRelation() != null) {
                TableRelation relation = idColumn.getRelation();

                Table destination = relation.getDestination().getTable();
                Table source = relation.getOrigin().getTable();

                query = String.format("SELECT `%s`.`%s` AS '%s', `%s`.`%s` AS '%s', `%s`.`%s` AS '%s' FROM `%s` INNER JOIN `%s` ON `%s`.`%s` = `%s`.`%s` ORDER BY `%s`.`%s`",
                        source.getName(),
                        nameColumn.getName(),
                        nameColumn.getName(),

                        source.getName(),
                        uuidColumn.getName(),
                        uuidColumn.getName(),

                        destination.getName(),
                        passwordColumn.getName(),
                        passwordColumn.getName(),

                        source.getName(),

                        destination.getName(),

                        source.getName(),
                        idColumn.getName(),

                        destination.getName(),
                        relation.getDestination().getName(),

                        source.getName(),
                        nameColumn.getName());
            } else {
                query = String.format("SELECT `%s`,`%s`,`%s` FROM `%s` ORDER BY `%s`", nameColumn.getName(), passwordColumn.getName(), uuidColumn.getName(), usersTable.getName(), nameColumn.getName());
            }
        }

        List<UserData> users = new ArrayList<>();
        {
            Connection connection = null;
            Statement statement = null;
            try {
                System.out.println("Preparing to fetch users...");
                connection = dataSource.getConnection();
                statement = connection.createStatement();

                ResultSet results = statement.executeQuery(query);
                while (results.next()) {
                    String userName = results.getString(nameColumn.getName());
                    boolean geyser = false;
                    if (configuration.removeGeyserPrefix()) {
                        String prefix = configuration.geyserPrefix();
                        if (userName.startsWith(prefix)) {
                            userName = userName.substring(prefix.length());
                            geyser = true;
                        }
                    }

                    String rawUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + userName).getBytes()).toString();
                    if (!generateUUID) {
                        rawUUID = results.getString(uuidColumn.getName());
                    }

                    String password = results.getString(passwordColumn.getName());
                    if (configuration.ignoreEmpty() && password.isEmpty()) {
                        continue;
                    }

                    UUID id;
                    if (rawUUID.contains("-")) {
                        id = UUID.fromString(rawUUID);
                    } else {
                        StringBuilder idBuff = new StringBuilder(rawUUID);
                        idBuff.insert(20, '-');
                        idBuff.insert(16, '-');
                        idBuff.insert(12, '-');
                        idBuff.insert(8, '-');

                        id = UUID.fromString(idBuff.toString());
                    }

                    UserData data = new UserData(userName, id, password, geyser);
                    users.add(data);
                }
            } finally {
                close(connection, statement);
            }
        }

        List<String> duplicatedNames = new ArrayList<>();
        Map<UUID, Set<UserData>> duplications = new HashMap<>();
        users.forEach((usr) -> {
            Set<UserData> entries = duplications.computeIfAbsent(usr.getUniqueId(), (set) -> new HashSet<>());
            for (UserData entry : entries) {
                if (!entry.getPassword().equals(usr.getPassword())) {
                    duplicatedNames.add(usr.getUser());
                }
            }

            entries.add(usr);
            duplications.put(usr.getUniqueId(), entries);
        });

        if (duplicatedNames.isEmpty()) {
            System.out.printf("Loaded %d users, with no duplicated entries%n", users.size());
        } else {
            System.out.printf("Loaded %d users, from which %s were duplicated and using different passwords (possibly because of geyser)%n", users.size(), duplicatedNames);
            System.out.println("Geyser users won't be migrated! Only their java accounts will");
        }

        destination = destination.replaceAll("\\\\", "/");
        Path destinationDirectory = Paths.get(destination);
        if (destination.contains("/")) {
            destinationDirectory = Paths.get("");
            String[] data = destination.split("/");
            for (String str : data) {
                if (str.matches("[A-Z]:")) {
                    destinationDirectory = destinationDirectory.resolve(str + "/"); //Windows letter
                } else {
                    destinationDirectory = destinationDirectory.resolve(str);
                }
            }
        }

        if (Files.exists(destinationDirectory) || !Files.isDirectory(destinationDirectory)) {
            if (!Files.isDirectory(destinationDirectory)) {
                Files.deleteIfExists(destinationDirectory);
            } else {
                remove(destinationDirectory);
            }
        }

        if (!Files.exists(destinationDirectory)) {
            Files.createDirectories(destinationDirectory);
        }

        Path finalDestination = destinationDirectory;
        try (InputStream stream = Migrator.class.getResourceAsStream("/acc_file.lldb")) {
            if (stream == null) throw new IllegalStateException("Cannot migrate because user account file template is null");

            try (InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8); BufferedReader reader = new BufferedReader(isr)) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }

                String raw = builder.toString();
                users.forEach((data) -> {
                    String password = data.getPassword();
                    if (isHex(data.getPassword()) && configuration.hexToBase()) {
                        password = new String(hexToBase(password));
                    }
                    if (!isBase64(password) && configuration.plainToBase()) {
                        password = Base64.getEncoder().encodeToString(password.getBytes());
                    }

                    String modRaw = raw.replace("%name%", data.getUser())
                            .replace("%uniqueid%", data.getUniqueId().toString())
                            .replace("%password%", password);

                    Path outputFile = finalDestination.resolve(data.getUniqueId().toString().replaceAll("-", "") + ".lldb");
                    try {
                        Files.write(outputFile, modRaw.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Wrote: " + outputFile);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
    }

    private static boolean isBase64(final String str) {
        String regex =
                "([A-Za-z0-9+/]{4})*" +
                        "([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";

        Pattern patron = Pattern.compile(regex);
        return patron.matcher(str).matches();
    }

    private static byte[] hexToBase(final String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }

        return Base64.getEncoder().encode(data);
    }

    private static boolean isHex(final String str) {
        if (str.length() % 2 != 0) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {
                return false;
            }
        }

        return true;
    }

    private static void remove(final Path file) {
        if (Files.isDirectory(file)) {
            try (Stream<Path> p = Files.list(file)) {
                p.forEach(Migrator::remove);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void close(final Connection connection, final Statement statement) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {}
        }
    }
}
