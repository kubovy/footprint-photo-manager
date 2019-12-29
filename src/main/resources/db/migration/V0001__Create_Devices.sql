create table DEVICE
(
    ID           VARCHAR(255)  NOT NULL PRIMARY KEY,
    NAME         VARCHAR(255)  NOT NULL,
    TYPE         VARCHAR(255)  NOT NULL,
    AUTH         CLOB,
    URI          VARCHAR(1024) NOT NULL,
    IS_PRIMARY   BOOLEAN       NOT NULL,
    CREATED_AT   TIMESTAMP     NOT NULL,
    LAST_SEEN_AT TIMESTAMP     NOT NULL,
);

CREATE INDEX IDX_DEVICE_NAME ON DEVICE (NAME);
CREATE UNIQUE INDEX UQ_DEVICE_NAME ON DEVICE (NAME);
CREATE INDEX IDX_DEVICE_IS_PRIMARY ON DEVICE (IS_PRIMARY);
CREATE INDEX IDX_DEVICE_CREATED_AT ON DEVICE (CREATED_AT);
CREATE INDEX IDX_DEVICE_LAST_SEEN_AT ON DEVICE (LAST_SEEN_AT);