package dev.cerus.mapads.advert.storage;

import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.mapads.image.storage.ImageStorage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqliteAdvertStorageImpl extends SqlAdvertStorage {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final HikariDataSource dataSource;

    public SqliteAdvertStorageImpl(final HikariDataSource dataSource, final ImageStorage imageStorage) {
        super(imageStorage);
        this.dataSource = dataSource;
        this.initTable();
        this.loadAllAds();
    }

    @Override
    protected String makeAdvertInsertQuery() {
        return "INSERT INTO `mapads_ads` (id, player_uuid, img_id, ad_screen_id, purchase_timestamp, purchased_minutes, remaining_minutes, price, reviewed) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET player_uuid = ?, img_id = ?, ad_screen_id = ?, " +
                "purchase_timestamp = ?, purchased_minutes = ?, remaining_minutes = ?, price = ?, reviewed = ?";
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
