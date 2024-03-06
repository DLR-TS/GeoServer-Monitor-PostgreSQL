package org.geoserver.monitor.monitorPsql;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorConfig.Mode;

/*
 * author: Sangeetha Shankar (DLR-TS)
 */

import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geotools.util.logging.Logging;

public class PsqlMonitorDAO implements MonitorDAO {
	
	public static final String NAME = "postgres";
	
	static Logger LOGGER = Logging.getLogger(PsqlMonitorDAO.class);
	
	Queue<RequestData> live = new ConcurrentLinkedQueue<>();
    Queue<RequestData> history = new ConcurrentLinkedQueue<>();

    Mode mode = Mode.HISTORY;
    
    public PsqlMonitorDAO() {
        setMode(Mode.HISTORY);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(MonitorConfig config) {
    	setMode(config.getMode());
    }

    @Override
    public RequestData init(RequestData data) {
    	//do nothing
        return data;
    }

    @Override
    public void add(RequestData data) {
        live.add(data);
    }

    @Override
    public void update(RequestData data) {}

    @Override
    public void save(RequestData data) {
        live.remove(data);
        history.add(data);

        if (history.size() > 100) {
            history.poll();
        }
    }

    @Override
    public RequestData getRequest(long id) {
        for (RequestData r : getRequests()) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    @Override
    public List<RequestData> getRequests() {
        List<RequestData> requests = new LinkedList<>();
        requests.addAll(live);
        requests.addAll(history);
        return requests;
    }

    @Override
    public List<RequestData> getRequests(Query q) {
       return null;
    }

    @Override
    public void getRequests(Query query, RequestDataVisitor visitor) {
    	return;
    }

    @Override
    public long getCount(Query query) {
        return -1L;
    }

    @Override
    public Iterator<RequestData> getIterator(Query query) {
        return getRequests(query).iterator();
    }

    @Override
    public List<RequestData> getOwsRequests() {
        return null;
    }

    @Override
    public java.util.List<RequestData> getOwsRequests(String service, String operation, String version) {
        return null;
    };

    @Override
    public void clear() {
        live.clear();
        history.clear();
    }

    @Override
    public void dispose() {
        live.clear();
        history.clear();
    }
}
