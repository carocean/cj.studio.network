package cj.studio.network.nodestart;

import cj.studio.ecm.Assembly;
import cj.studio.ecm.IAssembly;
import cj.studio.ecm.adapter.IActuator;
import cj.studio.ecm.adapter.IAdaptable;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class Entrypoint {

	private static String fileName;
	public static void main(String[] args) throws ParseException, IOException {
		fileName = "cj.studio.network.node";
		Options options = new Options();
		Option  m = new Option("m","man", false, "帮助");
		options.addOption(m);
		Option  u = new Option("nohup","nohup", false, "使用nohup后台启动");
		options.addOption(u);
		Option debug = new Option("d","debug", true, "调试命令行程序集时使用，需指定以下jar包所在目录\r\n"+fileName);
		options.addOption(debug);

		// GnuParser
		// BasicParser
		// PosixParser
		CommandLine line = new DefaultParser().parse(options, args);
		if (line.hasOption("m")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("network node", options);
			return;
		}

		String usr = System.getProperty("user.dir");
		File f = new File(usr);
		File[] arr = f.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith(fileName)) {
					return true;
				}
				return false;
			}
		});
		if (arr.length < 1 && !line.hasOption("debug")) {
			throw new IOException(fileName + " 程序集不存在.");
		}
		if (line.hasOption("debug")) {
			File[] da = new File(line.getOptionValue("debug")).listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					if (name.startsWith(fileName)) {
						return true;
					}
					return false;
				}
			});
			if (da.length < 0)
				throw new IOException("调试时不存在指定的必要jar包" + fileName);
			f = da[0];
		} else {
			f = arr[0];
		}

		IAssembly assembly = Assembly.loadAssembly(f.toString());
		assembly.start();
		Object node = assembly.workbin().part("networkNode");
		IAdaptable a = (IAdaptable) node;
		IActuator act = a.getAdapter(IActuator.class);
		act.exeCommand("entrypoint", f.getParent());

	}

}
