package cj.studio.network.nodeapp.subscriber;

import cj.studio.util.reactor.disk.jdbm3.DB;
import cj.studio.util.reactor.disk.jdbm3.DBMaker;

import java.util.concurrent.ConcurrentMap;

public class DefaultNodeOrientor implements INodeOrientor {
    ConcurrentMap<String, String> map;
    DB db;

    public DefaultNodeOrientor(String file) {
        db = DBMaker.openFile(file)
                .closeOnExit()
//                .enableEncryption("password", false)
                .make();
        map = db.getHashMap("nodes");
        if (map == null) {
            map = db.createHashMap("nodes");
        }
    }

    @Override
    public String get(String objkey) {
        return map.get(objkey);
    }

    @Override
    public void set(String objkey, String nodeName) {
        map.put(objkey, nodeName);
        db.commit();
    }


}