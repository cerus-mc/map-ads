package dev.cerus.mapads.advert.storage;

import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.mapads.image.storage.ImageStorage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySqlAdvertStorageImpl extends SqlAdvertStorage {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final HikariDataSource dataSource;

    public MySqlAdvertStorageImpl(final HikariDataSource dataSource, final ImageStorage imageStorage) {
        super(imageStorage);
        this.dataSource = dataSource;
        this.initTable();
        this.loadAllAds();
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
    public void close() throws IOException {
        this.dataSource.close();
    }

}
