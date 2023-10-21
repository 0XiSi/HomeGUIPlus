package net.oxisi.homeguiplus.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HomeCommand implements CommandExecutor {

    private final Connection conn;

    public HomeCommand(Connection connection) {
        this.conn = connection;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("home")) {
            try {
                if (storePlayerHome(player)) {
                    player.sendMessage("Home saved successfully.");
                } else {
                    player.sendMessage("You have reached the maximum number of homes.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage("An error occurred while processing your request.");
            }
            return true;
        }

        return false;
    }

    private boolean storePlayerHome(Player player) throws SQLException {
        String playerUUID = player.getUniqueId().toString();

        // Check if player has reached their homes limit
        String countQuery = "SELECT homes_limit FROM player_data WHERE player_uuid=?";
        try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
            countStmt.setString(1, playerUUID);
            ResultSet rs = countStmt.executeQuery();
            if (!rs.next()) {
                // If player data does not exist, you might want to handle this (e.g., insert a default record).
                return false;
            }

            int homesLimit = rs.getInt("homes_limit");

            String existingHomesQuery = "SELECT COUNT(*) FROM homes WHERE owner_id=?";
            try (PreparedStatement existingHomesStmt = conn.prepareStatement(existingHomesQuery)) {
                existingHomesStmt.setString(1, playerUUID);
                ResultSet homesRs = existingHomesStmt.executeQuery();
                if (homesRs.next() && homesRs.getInt(1) >= homesLimit) {
                    return false; // Player has reached the limit
                }
            }
        }

        // Store the location first
        Location playerLoc = player.getLocation();
        String worldUUID = playerLoc.getWorld().getUID().toString();
        String insertLocationQuery = "INSERT INTO locations (loc_x, loc_y, loc_z, yaw, pitch, world_uuid) VALUES (?, ?, ?, ?, ?, ?)";
        int locationID;

        try (PreparedStatement insertLocationStmt = conn.prepareStatement(insertLocationQuery, Statement.RETURN_GENERATED_KEYS)) {
            insertLocationStmt.setDouble(1, playerLoc.getX());
            insertLocationStmt.setDouble(2, playerLoc.getY());
            insertLocationStmt.setDouble(3, playerLoc.getZ());
            insertLocationStmt.setFloat(4, playerLoc.getYaw());
            insertLocationStmt.setFloat(5, playerLoc.getPitch());
            insertLocationStmt.setString(6, worldUUID);
            insertLocationStmt.executeUpdate();

            ResultSet rs = insertLocationStmt.getGeneratedKeys();
            if (rs.next()) {
                locationID = rs.getInt(1);
            } else {
                throw new SQLException("Failed to retrieve generated location ID.");
            }
        }

        // Store the home with the newly stored location
        String insertHomeQuery = "INSERT INTO homes (owner_id, location_id) VALUES (?, ?)";
        try (PreparedStatement insertHomeStmt = conn.prepareStatement(insertHomeQuery)) {
            insertHomeStmt.setString(1, playerUUID);
            insertHomeStmt.setInt(2, locationID);
            insertHomeStmt.executeUpdate();
        }

        return true;
    }
}
