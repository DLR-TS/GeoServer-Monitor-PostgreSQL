CREATE SEQUENCE geoserver_request_info_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;

CREATE TABLE geoserver_request_info (
	id bigserial NOT NULL DEFAULT nextval('geoserver_request_info_id_seq'),
	service varchar NOT NULL,
	operation varchar NOT NULL,
	resources varchar NOT NULL,
	httpmethod varchar NULL,
	starttime timestamptz NOT NULL,
	endtime timestamptz NOT NULL,
	username varchar NOT NULL,
	useragent varchar NULL,
	status int4 NOT NULL,
	responselength int8 NOT NULL,
	contenttype varchar NULL,
	totaltime int8 NOT NULL,
	CONSTRAINT geoserver_request_info_pk PRIMARY KEY (id)
);

CREATE INDEX geoserver_request_info_endtime_idx ON geoserver_request_info USING btree (endtime);
CREATE INDEX geoserver_request_info_starttime_idx ON geoserver_request_info USING btree (starttime);
