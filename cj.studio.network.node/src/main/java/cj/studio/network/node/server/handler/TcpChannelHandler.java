package cj.studio.gateway.server.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IServiceProvider;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.DefaultSegmentCircuit;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.IOutputChannel;
import cj.studio.ecm.net.ISegmentCircuitContent;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.gateway.IGatewaySocketContainer;
import cj.studio.gateway.IJunctionTable;
import cj.studio.gateway.conf.ServerInfo;
import cj.studio.gateway.junction.ForwardJunction;
import cj.studio.gateway.junction.Junction;
import cj.studio.gateway.server.util.GetwayDestHelper;
import cj.studio.gateway.socket.IGatewaySocket;
import cj.studio.gateway.socket.io.TcpInputChannel;
import cj.studio.gateway.socket.io.TcpOutputChannel;
import cj.studio.gateway.socket.io.WSOutputChannel;
import cj.studio.gateway.socket.pipeline.IInputPipeline;
import cj.studio.gateway.socket.pipeline.IInputPipelineBuilder;
import cj.studio.gateway.socket.pipeline.InputPipelineCollection;
import cj.studio.gateway.socket.serverchannel.tcp.TcpServerChannelGatewaySocket;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.gateway.socket.util.SocketName;
import cj.studio.network.node.ServerInfo;
import cj.ultimate.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

public class TcpChannelHandler extends ChannelHandlerAdapter {
	IServiceProvider parent;
	public static ILogging logger;
	InputPipelineCollection pipelines;
	private ServerInfo info;
	// 心跳丢失计数器
	private int counter;
	private Circuit currentCircuit;
	boolean streaming;// 正在处理流

	public TcpChannelHandler(IServiceProvider parent) {
		this.parent = parent;
		logger = CJSystem.logging();
		this.pipelines = new InputPipelineCollection();
		info = (ServerInfo) parent.getService("$.server.info");
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			// 空闲6s之后触发 (心跳包丢失)
			if (counter >= 10) {
				// 连续丢失10个心跳包 (断开连接)
				ctx.channel().close().sync();
			} else {
				counter++;
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf bb = (ByteBuf) msg;
		if (bb.readableBytes() == 0) {
			return;
		}
		byte[] b = new byte[bb.readableBytes()];
		bb.readBytes(b);
		bb.release();
		IInputChannel input = new MemoryInputChannel(8192);
		MemoryContentReciever reciever = new MemoryContentReciever();
		Frame pack = new Frame(input, reciever, b);
		input.done(b, 0, 0);

		if (!"GATEWAY/1.0".equals(pack.protocol())) {
			CJSystem.logging().error(getClass(), "不是网关协议侦:" + pack.protocol());
			return;
		}
		switch (pack.command()) {
			case "heartbeat":
				CJSystem.logging().debug(getClass(), "收到心跳包.");
				return;
			case "frame":
				if (streaming) {
					resetStreaming(true);
					break;
				}
				streaming = true;// 必须放在doFramePack前面，也就是不管它报不报错流程照走
				doFramePack(ctx, pack);
				break;
			case "content":
				if (!streaming) {
					resetStreaming(true);
					break;
				}
				doContentPack(ctx, pack);
				break;
			case "last":
				if (!streaming) {
					resetStreaming(true);
					break;
				}
				streaming = false;
				doLastPack(ctx, pack);
				break;
			default:
				throw new EcmException("不支持的gateway指令：" + pack.command());
		}

	}

	private void resetStreaming(boolean isLast) throws CircuitException {
		String errText = "";
		if (streaming) {
			errText = "前序流仍未结束，可能会导致前序流数据丢失，请客户端检查前序发送流程是否调用了input.done方法。";
			streaming = false;
		} else {
			errText = "当前流还未开始则发送内容或尾块";
		}
		CJSystem.logging().error(getClass(),errText);
		Circuit circuit = this.currentCircuit;
		if (!circuit.content().isCommited()) {
			circuit.status("503");
			circuit.message(errText);
			circuit.content().clearbuf();
			circuit.content().flush();
		}
		ISegmentCircuitContent cnt = (ISegmentCircuitContent) circuit.content();
		Frame err = cnt.createFirst("error / gateway/1.0");
		cnt.writeBytes(err.toBytes());
		byte[] msg =errText.getBytes();
		cnt.done(msg, 0, msg.length);
		cnt.flush();
		if(isLast) {
			cnt.close();
		}
		throw new CircuitException("503", errText);
	}

	private void doLastPack(ChannelHandlerContext ctx, Frame pack) throws CircuitException {
		if (inputChannel == null) {
			return;
		}

		byte[] b = pack.content().readFully();
		Circuit circuit = this.currentCircuit;
		try {
			inputChannel.done(b, 0, b.length);
			circuit.content().flush();// 到此刷新
		} catch (Exception e) {
			if (!circuit.content().isCommited()) {
				circuit.status("503");
				circuit.message(e.toString().replace("\r", "").replace("\n", ""));
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			ISegmentCircuitContent cnt = (ISegmentCircuitContent) circuit.content();
			Frame err = cnt.createFirst("error / gateway/1.0");
			cnt.writeBytes(err.toBytes());
			byte[] msg = out.toString().getBytes();
			cnt.done(msg, 0, msg.length);
			cnt.flush();
			throw e;
		} finally {
			if (!circuit.content().isClosed()) {
				circuit.content().close();
			}
			currentCircuit.dispose();
			this.currentCircuit = null;
			this.inputChannel = null;
		}
	}

	private void doContentPack(ChannelHandlerContext ctx, Frame pack) throws CircuitException {
		if (inputChannel == null) {
			return;
		}

		byte[] b = pack.content().readFully();
		Circuit circuit = this.currentCircuit;
		try {
			inputChannel.writeBytes(b, 0, b.length);
		} catch (Exception e) {
			if (!circuit.content().isCommited()) {
				circuit.status("503");
				circuit.message(e.toString().replace("\r", "").replace("\n", ""));
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			ISegmentCircuitContent cnt = (ISegmentCircuitContent) circuit.content();
			Frame err = cnt.createFirst("error / gateway/1.0");
			cnt.writeBytes(err.toBytes());
			byte[] msg = out.toString().getBytes();
			cnt.done(msg, 0, msg.length);
			cnt.flush();
			throw e;
		}
	}

	private void doFramePack(ChannelHandlerContext ctx, Frame pack) throws Exception {

		TcpInputChannel input = new TcpInputChannel();
		Frame frame = input.begin(pack);
		IOutputChannel output = new TcpOutputChannel(ctx.channel(), frame);
		Circuit circuit = new DefaultSegmentCircuit(output, String.format("%s 200 OK", frame.protocol()));
		this.currentCircuit = circuit;
		this.inputChannel = input;

		String uri = frame.url();
		String gatewayDestInHeader = frame.head(__frame_gatewayDest);
		String gatewayDest = GetwayDestHelper.getGatewayDestForHttpRequest(uri, gatewayDestInHeader, getClass());
		if (StringUtil.isEmpty(gatewayDest) || gatewayDest.endsWith("://")) {
			throw new CircuitException("404", "缺少路由目标，请求侦被丢掉：" + uri);
		}

		IInputPipeline inputPipeline = pipelines.get(gatewayDest);
		// 检查目标管道是否存在
		if (inputPipeline != null) {
			flowPipeline(inputPipeline, ctx, frame, circuit);
			return;
		}

		// 以下生成目标管道
		inputPipeline = pipelineBuild(gatewayDest, circuit, ctx);
		flowPipeline(inputPipeline, ctx, frame, circuit);// 再把本次请求发送处理
	}

	protected void flowPipeline(IInputPipeline pipeline, ChannelHandlerContext ctx, Frame frame, Circuit circuit)
			throws Exception {
		frame.head(__frame_fromProtocol, pipeline.prop(__pipeline_fromProtocol));
		frame.head(__frame_fromWho, pipeline.prop(__pipeline_fromWho));
		frame.head(__frame_fromPipelineName, pipeline.prop(__pipeline_name));

		try {
			pipeline.headFlow(frame, circuit);
		} catch (Throwable e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			ISegmentCircuitContent cnt = (ISegmentCircuitContent) circuit.content();
			Frame err = cnt.createFirst("error / gateway/1.0");
			cnt.writeBytes(err.toBytes());
			byte[] msg = out.toString().getBytes();
			cnt.done(msg, 0, msg.length);
			cnt.flush();
			throw e;
		}
	}

	protected IInputPipeline pipelineBuild(String gatewayDest, Circuit circuit, ChannelHandlerContext ctx)
			throws Exception {
		TcpServerChannelGatewaySocket wsSocket = new TcpServerChannelGatewaySocket(parent, gatewayDest, ctx.channel());
		sockets.add(wsSocket);// 不放在channelActive方法内的原因是当有构建需要时才添加，是按需索求

		IGatewaySocket socket = this.sockets.getAndCreate(gatewayDest);
		String pipelineName = SocketName.name(ctx.channel().id(), gatewayDest);
		IInputPipelineBuilder builder = (IInputPipelineBuilder) socket.getService("$.pipeline.input.builder");
		IInputPipeline inputPipeline = builder.name(pipelineName).prop(__pipeline_fromProtocol, "tcp")
				.prop(__pipeline_fromWho, info.getName()).prop(__pipeline_fromNetType, "server").createPipeline();
		pipelines.add(gatewayDest, inputPipeline);

		ForwardJunction junction = new ForwardJunction(pipelineName);
		junction.parse(inputPipeline, ctx.channel(), socket);
		this.junctions.add(junction);

		try {
			inputPipeline.headOnActive(pipelineName);// 通知管道激活
		} catch (Exception e) {
			if (!circuit.content().isCommited()) {
				circuit.content().clearbuf();
				circuit.content().flush();
			}
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			ISegmentCircuitContent cnt = (ISegmentCircuitContent) circuit.content();
			Frame err = cnt.createFirst("error / gateway/1.0");
			cnt.writeBytes(err.toBytes());
			byte[] msg = out.toString().getBytes();
			cnt.done(msg, 0, msg.length);
			cnt.close();
			ctx.close();
			throw e;
		}
		return inputPipeline;

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		String gatewayDests = this.info.getProps().get(__channel_onchannelEvent_notify_dests);
		if (StringUtil.isEmpty(gatewayDests)) {
			CJSystem.logging().warn(getClass(), String.format(
					"服务器：%s 未指定通道激活或失活事件的通知目标。应用仅能在之后第一次请求时才能收到激活或失活事件。请在该net的属性中指定：OnChannelEvent-Notify-Dests=dest1,dest2",
					info.getName()));
			return;
		}
		String arr[] = gatewayDests.split(",");
		for (String gatewayDest : arr) {
			Frame frame = new Frame(String.format("onactive /%s/ tcp/1.0", gatewayDest));
			WSOutputChannel output = new WSOutputChannel(ctx.channel(), frame);
			Circuit circuit = new DefaultSegmentCircuit(output, String.format("%s 200 OK", frame.protocol()));
			pipelineBuild(gatewayDest, circuit, ctx);
		}
	}

	protected void pipelineRelease(ChannelHandlerContext ctx) throws Exception {
		Set<String> dests = pipelines.enumDest();
		for (String dest : dests) {
			String pipelineName = SocketName.name(ctx.channel().id(), dest);
			Junction junction = junctions.findInForwards(pipelineName);
			if (junction != null) {
				this.junctions.remove(junction);
			}
			if (sockets.contains(pipelineName)) {
				sockets.remove(pipelineName);// 在此安全移除
			}
			IInputPipeline input = pipelines.get(dest);
			input.headOnInactive(pipelineName);
		}
		pipelines.dispose();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {

		pipelineRelease(ctx);
		counter = 0;
		super.channelInactive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, cause);
	}
}
