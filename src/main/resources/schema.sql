create table if not exists media
(
    id            int generated by default as identity primary key,
    filename      varchar(255) not null,
    hash          varchar(64)  not null,
    creation_date timestamp(0) not null,
    location_city varchar(255),
    location_country varchar(255),
    location_latitude DECIMAL(15,10) default 0.0,
    location_longitude DECIMAL(15,10) default 0.0
);