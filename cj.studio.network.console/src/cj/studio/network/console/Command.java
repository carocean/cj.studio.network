package cj.studio.network.console;

import org.apache.commons.cli.Options;

import java.io.IOException;

/**
 * 用于定义和执行命令行
 * <pre>
 *
 * </pre>
 * @author carocean
 *
 */
public abstract class Command {
	public abstract String cmd();
	public abstract String cmdDesc();
	public abstract Options options();
	public abstract boolean doCommand(CmdLine cl) throws IOException;
	protected void dispose(){
		
	}
}
