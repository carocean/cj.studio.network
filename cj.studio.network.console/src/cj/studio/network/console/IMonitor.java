package cj.studio.network.console;

import cj.studio.ecm.IServiceProvider;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

public interface IMonitor {
    void moniter(IServiceProvider site) throws ParseException, IOException;

}
