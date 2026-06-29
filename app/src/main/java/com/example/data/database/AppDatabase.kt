package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppDao
import com.example.data.model.*

@Database(
    entities = [
        Profile::class,
        Teacher::class,
        Student::class,
        Attendance::class,
        Payment::class,
        Expense::class,
        PaymentHistory::class,
        Group::class,
        Assignment::class,
        Exam::class,
        ExamGrade::class,
        StudentNote::class,
        ActivityLog::class,
        MessageTemplate::class,
        CommunicationLog::class,
        ScheduleEvent::class,
        Classroom::class,
        StudentTeacherCrossRef::class,
        Staff::class,
        Parent::class,
        ParentStudentLink::class
    ],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `student_teacher_cross_ref` (`studentId` TEXT NOT NULL, `teacherId` TEXT NOT NULL, PRIMARY KEY(`studentId`, `teacherId`))")
                db.execSQL("INSERT OR IGNORE INTO `student_teacher_cross_ref` (studentId, teacherId) SELECT id, teacherId FROM students WHERE teacherId IS NOT NULL AND teacherId != ''")
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `student_teacher_cross_ref` ADD COLUMN `customFee` REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "center_plus_database"
                )
                .addMigrations(MIGRATION_16_17, MIGRATION_18_19)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
