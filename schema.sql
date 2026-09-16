CREATE TABLE `spending`
(
    `id`             binary(255) NOT NULL,
    `date`           date                 DEFAULT NULL,
    `description`    varchar(255)         DEFAULT NULL,
    `amount`         double               DEFAULT NULL,
    `transaction_id` varchar(255)         DEFAULT NULL,
    `version`        bigint(20)           DEFAULT NULL,
    `creation_date`  timestamp   NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`id`)
);

ALTER TABLE `spending` ADD COLUMN `currency` varchar(10) NOT NULL;