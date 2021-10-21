package dev.cerus.mapads.image.storage;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqliteImageStorageImpl extends SqlImageStorage {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final HikariDataSource dataSource;

    public SqliteImageStorageImpl(final HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.initTable();
    }

    @Override
    protected String makeImageInsertQuery() {
        return "INSERT INTO `mapads_images` (id, width, height, imgdata) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET width = ?, height = ?, imgdata = ?";
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    @Override
    protected void run(final Runnable runnable) {
        this.executorService.execute(runnable);
    }

    @Override
    public void close() throws Exception {
        this.dataSource.close();
    }

}
