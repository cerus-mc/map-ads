package dev.cerus.mapads.advert.storage;

import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.image.transition.recorded.storage.RecordedTransitionStorage;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySqlAdvertStorageImpl extends SqlAdvertStorage {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final HikariDataSource dataSource;

    public MySqlAdvertStorageImpl(final HikariDataSource dataSource,
                                  final AdScreenStorage adScreenStorage,
                                  final ImageStorage imageStorage,
                                  final RecordedTransitionStorage recordedTransitionStorage) {
        super(adScreenStorage, imageStorage, recordedTransitionStorage);
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
