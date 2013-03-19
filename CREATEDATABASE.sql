CREATE TABLE AccessPoint (
	id serial PRIMARY KEY, 
	mac_addr varchar(18)
); 

CREATE TABLE Location (
	id serial PRIMARY KEY, 
	x double precision, 
	y double precision,
	map_id Integer
);

CREATE TABLE Rssi (
	id_loc Integer Unique,
	id_ap Integer Unique,
	avg_val double precision,
	std_dev double precision,
	FOREIGN KEY (id_ap) REFERENCES AccessPoint (id),
	FOREIGN KEY (id_loc) REFERENCES Location (id)
);

CREATE TABLE TempRssi (
	ap_id Integer Unique,
	mac_addr varchar(18),
	avg_val double precision,
	FOREIGN KEY (ap_id) REFERENCES AccessPoint (id)
);

CREATE TABLE Maps (
	id serial PRIMARY KEY,
	description varchar(100),
	px_width Integer,
	px_height Integer,
	meters_width double precision,
	meters_height double precision,
	content bytea 
);
