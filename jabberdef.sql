create table jabberuser(
userid int primary key,
username varchar(255) not null,
emailadd varchar(255) not null
);

create table jab(
jabid int primary key,
userid int references jabberuser(userid) not null,
jabtext varchar(255) not null
);

create table likes(
userid int references jabberuser(userid),
jabid int references jab(jabid) not null,
primary key(userid, jabid)
);

create table follows(
useridA int references jabberuser(userid) not null,
useridB int references jabberuser(userid) not null,
primary key (useridA, useridB)
);