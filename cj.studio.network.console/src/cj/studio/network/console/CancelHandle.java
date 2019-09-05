package cj.studio.network.console;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

public class CancelHandle implements Runnable {
	class MyFileInputStream extends FileInputStream{

		public MyFileInputStream(FileDescriptor fdObj) {
			super(fdObj);
			// TODO Auto-generated constructor stub
		}
		@Override
		public void close() throws IOException {
			
		}
	}
	public CancelHandle() {
	}
	FileInputStream in;
	public void run() {
		while (!isCanceled) {
			// 不采用System.in是因为，该线程被终止时System.in无法终止
			FileInputStream in = new MyFileInputStream(FileDescriptor.in);//隔断对控制台的close
			FileChannel ch=in.getChannel();
			InputStream stdInCh = Channels.newInputStream(ch);
			try {
				int i = stdInCh.read();
				if (i == 113) {
					isCanceled = true;
					System.out.println("任务结束");
					break;
				}
			} catch (IOException e) {
			}
		}
	}

	boolean isCanceled;
	public Thread handler;

	public boolean isCanceled() {
		return isCanceled;
	}

	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
		if (handler != null) {
			handler.interrupt();
		}
	}
}
