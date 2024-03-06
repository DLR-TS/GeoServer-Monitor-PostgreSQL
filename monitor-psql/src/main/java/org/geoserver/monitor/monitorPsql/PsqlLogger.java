package org.geoserver.monitor.monitorPsql;

/*
 * author: Sangeetha Shankar (DLR-TS)
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataListener;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class PsqlLogger implements RequestDataListener, ApplicationListener<ApplicationEvent> {

    MonitorConfig config;
    private static final Logger LOGGER = Logging.getLogger(PsqlLogger.class);

    String SQL;

    Connection dbconn;

    String pgTablename;

    public PsqlLogger(MonitorConfig config) {
        this.config = config;

        if (this.config.getStorage().equals("postgres")) {
            // test db connection
            if (connectToDb()) {
                LOGGER.info("Connection to database successfully tested.");
                closeDbConn();

                this.SQL =
                        "INSERT INTO "
                                + this.pgTablename
                                + "(service,operation,resources,httpmethod,"
                                + "starttime,endtime,username,useragent,"
                                + "status,responselength,contenttype,totaltime) VALUES"
                                + "(?,?,?,?,"
                                + "?,?,?,?,"
                                + "?,?,?,?)";
            }
        }
    }

    private boolean connectToDb() {
        Properties props = config.getProperties();
        String url;
        Properties conn_prop;

        // connect to database
        try {
            url = props.getProperty("postgres.host");
            conn_prop = new Properties();
            conn_prop.setProperty("user", props.getProperty("postgres.user"));
            conn_prop.setProperty("password", props.getProperty("postgres.password"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while reading database connection params", e);
            return false;
        }

        try {
            Class.forName("org.postgresql.Driver");
            this.dbconn = DriverManager.getConnection(url, conn_prop);
            this.pgTablename = props.getProperty("postgres.tablename");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
            return false;
        }

        return true;
    }

    private boolean closeDbConn() {
        try {
            this.dbconn.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to close database", e);
            return false;
        }

        return true;
    }

    private int insertRow(RequestData rd) {

        PreparedStatement ps;
        int affectedRows = -1;

        try {
            // Note: request id resets to 1 each time geoserver is restarted. Hence, the database
            // cannot use request id as primary key.
            ps = this.dbconn.prepareStatement(this.SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, rd.getService());
            ps.setString(2, rd.getOperation());
            ps.setString(3, String.join(",", rd.getResources()));
            ps.setString(4, rd.getHttpMethod());
            ps.setTimestamp(
                    5,
                    new Timestamp(
                            rd.getStartTime().getTime())); // convert java Date to sql Timestamp
            ps.setTimestamp(
                    6,
                    new Timestamp(rd.getEndTime().getTime())); // convert java Date to sql Timestamp
            ps.setString(7, rd.getRemoteUser());
            ps.setString(8, rd.getRemoteUserAgent());
            ps.setInt(9, rd.getResponseStatus());
            ps.setLong(10, rd.getResponseLength());
            ps.setString(11, rd.getBodyContentType());
            ps.setLong(12, rd.getTotalTime());

            if (!this.dbconn.isClosed()) {
                affectedRows = ps.executeUpdate();
                LOGGER.log(Level.FINER, rd_to_string(rd));
            } else {
                throw new Exception("Database connection is closed!");
            }

        } catch (Exception e) {
            if (e instanceof org.postgresql.util.PSQLException) {
                LOGGER.log(Level.WARNING, "Error while inserting record", e);
                LOGGER.log(Level.WARNING, rd_to_string(rd));

                // when the frequency of requests is high, an I/O error occurs.
                // this error shows up once for every 40-50 requests. Its occurrence is
                // unpredictable and not fixed yet.
                // the error could be occurring due to a bug in java.sql library.
                // org.postgresql.util.PSQLException: An I/O error occurred while sending to the
                // backend.
                // Caused by: javax.net.ssl.SSLException: Socket closed
                // Caused by: java.net.SocketException: Socket closed
            } else {
                LOGGER.log(Level.WARNING, "Error while inserting record", e);
                LOGGER.log(Level.WARNING, rd_to_string(rd));
            }
        }

        return affectedRows;
    }

    // custom function to convert RequestData to string. The function defined in the GeoServer
    // Monitoring extension provides only request ID
    private String rd_to_string(RequestData rd) {
        String rdString = "RequestData object: ";
        rdString += rd.getService() + "; ";
        rdString += rd.getOperation() + "; ";
        rdString += String.join(",", rd.getResources()) + "; ";
        rdString += rd.getHttpMethod() + "; ";
        rdString += String.valueOf(rd.getStartTime()) + "; ";
        rdString += String.valueOf(rd.getEndTime()) + "; ";
        rdString += rd.getRemoteUser() + "; ";
        rdString += rd.getRemoteUserAgent() + "; ";
        rdString += rd.getResponseStatus() + "; ";
        rdString += rd.getResponseLength() + "; ";
        rdString += rd.getBodyContentType() + "; ";
        rdString += rd.getTotalTime();

        return rdString;
    }

    @Override
    public void requestStarted(RequestData rd) {
        // do nothing
    }

    @Override
    public void requestUpdated(RequestData rd) {
        // do nothing
    }

    @Override
    public void requestCompleted(RequestData rd) {
        // do nothing
    }

    @Override
    public void requestPostProcessed(RequestData rd) {
        // check if monitor storage is postgres
        if (!this.config.getStorage().equals("postgres")) {
            // psqlMonitor not enabled
            return;
        }

        if (rd == null) {
            // do nothing
            return;
        }

        // write the request data to database

        if (connectToDb()) {
            // write the record to db
            insertRow(rd);

            // close db conn
            closeDbConn();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // do nothing
    }
}
