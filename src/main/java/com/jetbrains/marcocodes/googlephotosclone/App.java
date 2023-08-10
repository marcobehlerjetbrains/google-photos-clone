package com.jetbrains.marcocodes.googlephotosclone;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Hello world!
 */
public class App {

    /*

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    static ImageMagick magick = new ImageMagick();
    private static DataSource dataSource = dataSource();





    public static void main(String[] args) throws IOException, InterruptedException {

    }

    private static void writeHtmlFile() throws IOException {
        Map<LocalDate, List<String>> images = new TreeMap<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "select hash, CAST(creation_date AS DATE) creation_date from media " +
                     "order by creation_date desc")) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String hash = resultSet.getString("hash");
                LocalDate creationDate = resultSet.getObject("creation_date", LocalDate.class);

                images.putIfAbsent(creationDate, new ArrayList<>());
                images.get(creationDate).add(hash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        StringBuilder html = new StringBuilder();
        images.forEach((date, hashes) -> {
            html.append("<h2>").append(date).append("</h2>");

            hashes.forEach(hash -> {
                Path image = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + ".webp");
                html.append("<img width='300' src='").append(image.toAbsolutePath()).append("' loading='lazy'/>");
            });
            html.append("<br/>");
        });

        Files.write(Paths.get("./output.html"), template.replace("{{pics}}", html.toString()).getBytes());
    }

    private static boolean exists(Path image, String hash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select 1 from media where filename = ? and hash = ?")) {
            stmt.setString(1, image.getFileName().toString());
            stmt.setString(2, hash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void save(Path image, String hash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into media (filename, hash, creation_date) values ( ?, ?, ?)")) {
            stmt.setString(1, image.getFileName().toString());
            stmt.setString(2, hash);
            stmt.setObject(3, creationTime(image));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
*/

}