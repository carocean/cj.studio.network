package cj.studio.network.console;

import cj.studio.ecm.EcmException;
import cj.ultimate.util.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleEditor {
	
	public static BufferedReader newReader(){
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader read = new BufferedReader(input);
		return read;
	}
	/**
	 * 撤消某个任务，输入q回撤即可
	 * <pre>
	 * 用法：CancelHandle判断是否已可撤消，如果 任务完成请调用：setCanceled(true)
	 * 
	 * 问题：
	 * 当任务正常执行完后，却无法将控制台的读阻塞取消，所以影响到后继命令的输入
	 * </pre>
	 * @return
	 */
	public static CancelHandle CancelTask() {
		CancelHandle handle=new CancelHandle();
		Thread t=new Thread(handle);
		t.start();
		handle.handler=t;
		return handle;
	}
	public static boolean confirmConsole(String okWord,String cancelWord,String indent, BufferedReader read) {
		String line = "";
		System.out.print(String.format("%s$:", indent));
		try {
			while (true) {
				line = read.readLine();
				if (StringUtil.isEmpty(line)) {
					System.out.print(String.format("%s$:", indent));
					continue;
				}
				if (line.trim().equals(okWord)) {
					return true;
				} if (line.trim().equals(cancelWord)) {
					return false;
				}else{
				}
				System.out.print(String.format("%s$:", indent));
			}
		} catch (Exception e) {
			throw new EcmException(e);
		}

	}
	public static void readConsole(String indent, BufferedReader read,
			StringBuffer sb) {
		String line = "";
		System.out.print(String.format("%s$:", indent));
		try {
			while (true) {
				line = read.readLine();
				if (StringUtil.isEmpty(line)) {
					System.out.print(String.format("%s$:", indent));
					continue;
				}
				if (line.endsWith("!q")) {
					sb.append(line.substring(0, line.indexOf("!q")));
					break;
				} else {
					sb.append(line.trim());
				}
				System.out.print(String.format("%s$:", indent));
			}
		} catch (Exception e) {
			throw new EcmException(e);
		}

	}
	public static void readConsole(String indent,String lineTerminus, BufferedReader read,
			StringBuffer sb) {
		String line = "";
		System.out.print(String.format("%s$:", indent));
		try {
			while (true) {
				line = read.readLine();
				
				if (StringUtil.isEmpty(line)) {
					System.out.print(String.format("%s$:", indent));
					continue;
				}
				if (line.endsWith("!q")) {
					sb.append(line.substring(0, line.indexOf("!q")));
					break;
				} else {
					sb.append(String.format("%s%s",line.trim(),lineTerminus));
				}
				System.out.print(String.format("%s$:", indent));
			}
		} catch (Exception e) {
			throw new EcmException(e);
		}

	}
}
