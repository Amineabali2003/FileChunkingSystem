CREATE TABLE IF NOT EXISTS chunks (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      hash TEXT NOT NULL UNIQUE,
                                      file_path TEXT NOT NULL,
                                      order_index INTEGER NOT NULL,
                                      data BLOB NOT NULL
);