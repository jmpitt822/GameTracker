package com.theironyard;

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    static HashMap<String, User> users = new HashMap<>();

    public static void insertGame(Connection conn, String gameName, String gameGenre, String gamePlatform, int gameYear) throws SQLException { //here, textArray is a string array comprised of all of the information input by the user
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO games VALUES (NULL, ?, ?, ?, ?)");
        stmt.setString(1, gameName); //sets name
        stmt.setString(2, gameGenre); // sets genre
        stmt.setString(3, gamePlatform); //sets platform
        stmt.setInt(4, gameYear); //sets release year
        stmt.execute();
    }

    public static void deleteGame(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM games WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    public static ArrayList<Game> selectGames(Connection conn) throws SQLException {
        ArrayList<Game> games = new ArrayList<>();
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM games");
        while (results.next()){
            int id = results.getInt("id");
            String name = results.getString("name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("release_year");
            games.add(new Game(id, name, genre, platform, releaseYear));
        }
        return games;
    }

    public static void updateGame(Connection conn, int id, String gameName, String gameGenre, String gamePlatform, int gameYear) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE games SET name =?, genre = ?, platform = ?, release_year = ? WHERE id = ?");
        stmt.setString(1, gameName);
        stmt.setString(2, gameGenre);
        stmt.setString(3, gamePlatform);
        stmt.setInt(4, gameYear);
        stmt.setInt(5, id);
        stmt.execute();
    }

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();

        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS games (id IDENTITY , name VARCHAR , genre VARCHAR , platform VARCHAR , release_year INT)");

        Spark.init();
        Spark.get(
                "/",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
//                    selectGames(conn);
                    HashMap m = new HashMap<>();
                    if (user == null) {
                        return new ModelAndView(m, "login.html");
                    }
                    ArrayList<Game> games = selectGames(conn);
                    m.put("games", games);
                    return new ModelAndView(m, "home.html");

                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/create-user",
                ((request, response) -> {
                    String name = request.queryParams("loginName");
                    User user = users.get(name);
                    if (user == null) {
                        user = new User(name);
                        users.put(name, user);
                    }

                    Session session = request.session();
                    session.attribute("userName", name);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/create-game",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    if (user == null) {
                        //throw new Exception("User is not logged in");
                        Spark.halt(403);
                    }

                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("gameYear"));
                    insertGame(conn, gameName, gameGenre, gamePlatform, gameYear);
//                    Game game = new Game(gameName, gameGenre, gamePlatform, gameYear);
//                    user.games.add(game);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/delete-game",
                ((request, response) -> {
                    int id = Integer.parseInt(request.queryParams("deleteGame"));
                    deleteGame(conn, id);

                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/update-game",
                ((request, response) -> {
                    int id = Integer.parseInt(request.queryParams("updateGame"));
                    String gameName = request.queryParams("updateName");
                    String gameGenre = request.queryParams("updateGenre");
                    String gamePlatform = request.queryParams("updatePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("updateYear"));
                    updateGame(conn, id, gameName, gameGenre, gamePlatform, gameYear);

                    response.redirect("/");
                    return "";
                })
        );
    }

    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return users.get(name);
    }
}
