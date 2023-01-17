create table `media`
(
    `id`         identity not null primary key,
    `file_name` varchar(255) default null,
    `reference` varchar(255) default null,
    primary key (`id`)
);