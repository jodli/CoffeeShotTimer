# Database Migration Guidelines

## Background

We previously had a bug where `DatabaseModule` was creating indices with wrong names in `RoomDatabase.Callback`. This has been fixed:
- ✅ Removed the problematic `createIndexes()` method from `DatabaseModule`  
- ✅ Added migration 7→8 as a one-time cleanup for affected databases
- ✅ Room now handles all index creation through `@Entity` annotations

## Key Principles

1. **Keep migrations simple** - Only change what you need to change
2. **Let Room handle indices** - Define them in `@Entity` annotations, not in migrations
3. **Wrap in try-catch** - Always provide clear error messages

## Writing Migrations

### Standard Migration (Most Common)

```kotlin
private val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            // Just do what you need - add column, create table, etc.
            db.execSQL("ALTER TABLE shots ADD COLUMN newField TEXT")
            
        } catch (e: Exception) {
            throw RuntimeException("Migration $X->$Y failed: ${e.message}", e)
        }
    }
}
```

### When You MUST Drop and Recreate Indices

Only in these specific cases:

#### 1. Changing an index definition (e.g., making it unique)
```kotlin
// Changing from regular to unique index
db.execSQL("DROP INDEX IF EXISTS index_beans_name")
db.execSQL("CREATE UNIQUE INDEX index_beans_name ON beans (name)")
```

#### 2. Renaming an index
```kotlin
// Old index must be dropped, new one created
db.execSQL("DROP INDEX IF EXISTS old_index_name")
db.execSQL("CREATE INDEX new_index_name ON table (column)")
```

#### 3. Changing indexed columns
```kotlin
// Changing from single to composite index
db.execSQL("DROP INDEX IF EXISTS index_shots_beanId")
db.execSQL("CREATE INDEX index_shots_beanId_timestamp ON shots (beanId, timestamp)")
```

## Index Naming Convention

Room automatically generates index names from `@Entity` annotations:
- Pattern: `index_<tableName>_<columnName>`
- Example: `index_beans_isActive`, `index_shots_beanId`
- Composite: `index_shots_beanId_timestamp`

## What NOT to Do

- ❌ **Don't create indices in migrations** unless you're changing them
- ❌ **Don't use `IF NOT EXISTS` when changing indices** - it won't replace them
- ❌ **Don't manually create indices in `RoomDatabase.Callback`** - let Room handle it
- ❌ **Don't add index cleanup to every migration** - that was only needed for the bug fix

## Best Practices

1. **Define indices in `@Entity` annotations** - Room creates them automatically
2. **Keep migrations focused** - Only change what the migration is about
3. **Test migration paths** - But remember: simpler migrations = fewer bugs
4. **Use `IF NOT EXISTS` for new tables** - Safe for table creation
5. **Avoid `IF NOT EXISTS` for index changes** - Won't replace existing indices

## Testing Migrations

Test these scenarios:
1. Fresh install (no migration)
2. Direct migration (X→Y)
3. Skip migrations (X→Z where Z > Y)
4. Ensure indices are correct after migration
