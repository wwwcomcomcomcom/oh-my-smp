package iieiiergn.smpAuth.auth

import iieiiergn.smpAuth.common.Json
import iieiiergn.smpAuth.common.StudentData
import java.sql.Connection
import java.sql.DriverManager

/**
 * Persistent {@code uuid -> StudentData} store backed by SQLite.
 * The snapshot is written once at link time and never refreshed (per spec §5.3).
 * A single connection is guarded by a lock — write volume here is tiny.
 */
class LinkRepository(dbPath: String) {

    private val lock = Any()
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

    init {
        synchronized(lock) {
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS links (
                        uuid           TEXT PRIMARY KEY,
                        username       TEXT,
                        datagsm_id     INTEGER,
                        grade          INTEGER,
                        class_num      INTEGER,
                        student_number INTEGER,
                        student_json   TEXT NOT NULL,
                        linked_at      INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }

    /** Insert or overwrite the link for [uuid]. */
    fun upsert(uuid: String, username: String?, student: StudentData) {
        val json = Json.GSON.toJson(student)
        synchronized(lock) {
            conn.prepareStatement(
                """
                INSERT INTO links (uuid, username, datagsm_id, grade, class_num, student_number, student_json, linked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    username = excluded.username,
                    datagsm_id = excluded.datagsm_id,
                    grade = excluded.grade,
                    class_num = excluded.class_num,
                    student_number = excluded.student_number,
                    student_json = excluded.student_json,
                    linked_at = excluded.linked_at
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, uuid)
                ps.setString(2, username)
                ps.setObject(3, student.datagsmId())
                ps.setObject(4, student.grade())
                ps.setObject(5, student.classNum())
                ps.setObject(6, student.studentNumber())
                ps.setString(7, json)
                ps.setLong(8, System.currentTimeMillis())
                ps.executeUpdate()
            }
        }
    }

    /** Return the snapshot for [uuid], or null if not linked. */
    fun find(uuid: String): StudentData? {
        synchronized(lock) {
            conn.prepareStatement("SELECT student_json FROM links WHERE uuid = ?").use { ps ->
                ps.setString(1, uuid)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return Json.GSON.fromJson(rs.getString(1), StudentData::class.java)
                }
            }
        }
    }
}
