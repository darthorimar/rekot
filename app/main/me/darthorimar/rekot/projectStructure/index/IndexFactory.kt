package me.darthorimar.rekot.projectStructure.index

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.logging.logger
import me.darthorimar.rekot.util.withTimeLogging
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.koin.core.component.inject
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.name

@Suppress(
    "EXPOSED_FUNCTION_RETURN_TYPE",
    "INFERRED_INVISIBLE_WHEN_TYPE_WARNING",
    "INFERRED_INVISIBLE_RETURN_TYPE_WARNING",
)
class IndexFactory : AppComponent {
    private val config: AppConfig by inject()
    private val serializer: IndexSerializer by inject()
    private val indexer: Indexer by inject()

    private var lock = ReentrantLock()

    fun index(root: Path, kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment): Index {
        lock.withLock {  
            return withIndexFile(root) { indexFile ->
                if (indexFile.exists()) {
                    logger.info("Using existing index for `$root` at `${indexFile.absolutePathString()}`")
                    val indexStamp = serializer.deserializeStamp(indexFile)
                    if (!indexStamp.isUpToDate(root)) {
                        logger.info("Index for `$root` is outdated, reindexing...")
                        indexFile.deleteIfExists()
                        doIndex(
                            root = root,
                            indexFile = indexFile,
                            kotlinCoreProjectEnvironment = kotlinCoreProjectEnvironment,
                        )
                    } else {
                        logger.info("Index for `$root` is up to date, using existing index.")
                        withTimeLogging("Deserializing index for `$root`") {
                            serializer.deserialize(indexFile, kotlinCoreProjectEnvironment)
                        }
                    }
                } else {
                    logger.info("No existing index for `$root`, creating new index at `${indexFile.absolutePathString()}`")
                    doIndex(
                        root = root,
                        indexFile = indexFile,
                        kotlinCoreProjectEnvironment = kotlinCoreProjectEnvironment,
                    )
                }
            }
        }
    }

    private fun doIndex(
        root: Path,
        indexFile: Path,
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment
    ): Index {
        logger.info("Indexing for `$root` at `${indexFile.absolutePathString()}`")
        return withTimeLogging("Indexing for `$root`") {
            val index = indexer.index(root, kotlinCoreProjectEnvironment)
            serializer.serialize(index, indexFile)
            index
        }
    }

    private fun <R> withIndexFile(root: Path, action: (indexFile: Path) -> R): R {
        val rootStr = root.absolutePathString()

        createConnection().use { conn ->
            val newFileName = root.name.replace(".", "_") + "_" + System.currentTimeMillis()
            conn.autoCommit = false
            conn
                .prepareStatement(
                    """
            INSERT OR IGNORE INTO root_index (root, index_file, locked)
            VALUES (?, ?, 0)
        """)
                .use { stmt ->
                    stmt.setString(1, rootStr)
                    stmt.setString(2, newFileName)
                    stmt.executeUpdate()
                }
            conn.commit()

            while (true) {
                try {
                    val indexFile =
                        conn
                            .prepareStatement(
                                """
                    UPDATE root_index
                    SET locked = 1
                    WHERE root = ? AND locked = 0
                    RETURNING index_file
                """)
                            .use { stmt ->
                                stmt.setString(1, rootStr)
                                val rs = stmt.executeQuery()
                                if (rs.next()) {
                                    (config.indexDir / "data" / rs.getString("index_file"))
                                } else {
                                    conn.rollback()
                                    null
                                }
                            }

                    if (indexFile == null) {
                        Thread.sleep(100)
                        continue
                    }

                    val result = action(indexFile)

                    conn
                        .prepareStatement(
                            """
                    UPDATE root_index
                    SET locked = 0
                    WHERE root = ?
                """)
                        .use { stmt ->
                            stmt.setString(1, rootStr)
                            stmt.executeUpdate()
                        }

                    conn.commit()
                    return result
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
        }
    }


    private fun createConnection(): Connection {
        val db = config.indexDir / "index.db"
        val c = DriverManager.getConnection("jdbc:sqlite:${db.absolutePathString()}?busy_timeout=5000")
        c.createStatement().use { stmt ->
            stmt.execute("PRAGMA journal_mode=WAL;");
        }
        c.autoCommit = false
        c.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS root_index (
                        root TEXT PRIMARY KEY,
                        index_file TEXT NOT NULL,
                        locked BOOLEAN
                    )
                """
                    .trimIndent())
        }
        c.commit()
        return c
    }
}

private val logger = logger<IndexFactory>()
