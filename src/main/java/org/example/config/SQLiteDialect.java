package org.example.config;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

public class SQLiteDialect extends Dialect {
    public SQLiteDialect() {
        super(DatabaseVersion.make(3, 35));
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new SQLiteIdentityColumnSupport();
    }

    private static class SQLiteIdentityColumnSupport extends IdentityColumnSupportImpl {
        @Override
        public boolean supportsIdentityColumns() {
            return true;
        }

        @Override
        public String getIdentityColumnString(int type) {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }

        @Override
        public String getIdentitySelectString(String table, String column, int type) {
            return "SELECT last_insert_rowid()";
        }
    }
}