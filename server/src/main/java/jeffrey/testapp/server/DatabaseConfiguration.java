package jeffrey.testapp.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.metrics.PoolStats;
import jeffrey.testapp.server.metrics.JfrHikariDataSource;
import jeffrey.testapp.server.metrics.JfrPoolMetricsTracker;
import jeffrey.testapp.server.metrics.JfrPoolStatisticsPeriodicRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class DatabaseConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConfiguration.class);

    private static final SQLiteConfig.JournalMode JOURNAL_MODE = SQLiteConfig.JournalMode.WAL;
    private static final SQLiteConfig.SynchronousMode SYNCHRONOUS = SQLiteConfig.SynchronousMode.NORMAL;
    private static final int BUSY_TIMEOUT_MS = 5000;
    private static final int CACHE_SIZE_KB = 65536;
    private static final SQLiteConfig.TempStore TEMP_STORE = SQLiteConfig.TempStore.MEMORY;
    private static final boolean FOREIGN_KEYS = true;

    @Bean(destroyMethod = "close")
    public JfrHikariDataSource dataSource(
            @Value("${database.file}") String file,
            @Value("${database.cleanup-on-shutdown:true}") boolean cleanupOnShutdown,
            @Value("${database.pool.max-size:10}") int maxPoolSize,
            @Value("${database.pool.min-idle:1}") int minIdle) {

        Path dbPath = Path.of(file);
        ensureParentDirectory(dbPath);

        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setJournalMode(JOURNAL_MODE);
        sqliteConfig.setSynchronous(SYNCHRONOUS);
        sqliteConfig.setTempStore(TEMP_STORE);
        sqliteConfig.enforceForeignKeys(FOREIGN_KEYS);
        sqliteConfig.setBusyTimeout(BUSY_TIMEOUT_MS);

        SQLiteDataSource sqliteDataSource = new SQLiteDataSource(sqliteConfig);
        sqliteDataSource.setUrl("jdbc:sqlite:" + dbPath);

        HikariConfig config = new HikariConfig();
        config.setDataSource(sqliteDataSource);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setMetricsTrackerFactory((String poolName, PoolStats poolStats) -> {
            JfrPoolStatisticsPeriodicRecorder.addPool(poolName, poolStats);
            return new JfrPoolMetricsTracker(poolName);
        });

        JfrPoolStatisticsPeriodicRecorder.registerToFlightRecorder();
        return new CleanupAwareDataSource(config, dbPath, cleanupOnShutdown);
    }

    private static void ensureParentDirectory(Path dbPath) {
        Path parent = dbPath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create parent directory for database file: " + parent, ex);
        }
    }

    static final class CleanupAwareDataSource extends JfrHikariDataSource {
        private final Path file;
        private final boolean cleanupOnShutdown;

        CleanupAwareDataSource(HikariConfig config, Path file, boolean cleanupOnShutdown) {
            super(config);
            this.file = file;
            this.cleanupOnShutdown = cleanupOnShutdown;
        }

        @Override
        public void close() {
            super.close();
            if (cleanupOnShutdown) {
                deleteSqliteFiles();
            }
        }

        private void deleteSqliteFiles() {
            String base = file.toString();
            for (String suffix : new String[]{"", "-wal", "-shm"}) {
                Path path = Path.of(base + suffix);
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    LOG.warn("Failed to delete SQLite file {}: {}", path, ex.toString());
                }
            }
        }
    }
}
