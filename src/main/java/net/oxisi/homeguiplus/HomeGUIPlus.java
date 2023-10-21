package net.oxisi.homeguiplus;

import net.oxisi.homeguiplus.database.DatabaseManager;
import net.oxisi.homeguiplus.commands.HomeCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Objects;

public final class HomeGUIPlus extends JavaPlugin {

    private DatabaseManager dbManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();  // This ensures your config.yml is saved if it doesn't exist.
        FileConfiguration config = getConfig();

        String dbUrl = config.getString("Database.Database-Url");
        String dbUsername = config.getString("Database.Database-Username");
        String dbPassword = config.getString("Database.Database-Password");

        try {
            dbManager = new DatabaseManager(dbUrl, dbUsername, dbPassword);
            // ... register your commands, listeners, etc.
            HomeCommand homeExecutor = new HomeCommand(dbManager.getConnection());
            // Assume you register your command executor here
            Objects.requireNonNull(getCommand("home")).setExecutor(homeExecutor);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PluginInitializationException("Could not establish database connection! Disabling plugin...");

        }
    }

    @Override
    public void onDisable() {
        if (dbManager != null) {
            try {
                dbManager.closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
