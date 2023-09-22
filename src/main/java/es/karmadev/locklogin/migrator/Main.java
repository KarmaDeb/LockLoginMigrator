package es.karmadev.locklogin.migrator;

import es.karmadev.locklogin.migrator.util.ArgumentParser;
import es.karmadev.locklogin.migrator.util.argument.AbstractArgument;

public class Main {

    public static void main(final String[] args) throws Exception {
        AbstractArgument<?>[] arguments = AbstractArgument.getFromClass(Migrator.class);
        ArgumentParser parser = ArgumentParser.parse(args, arguments);

        Migrator migrator = new Migrator();
        parser.map(true, migrator);

        migrator.start();
    }
}
