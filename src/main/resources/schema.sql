CREATE TABLE IF NOT EXISTS chunks (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      hash TEXT NOT NULL,
                                      file_path TEXT NOT NULL,
                                      order_index INTEGER NOT NULL
);