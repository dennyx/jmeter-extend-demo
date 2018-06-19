package com.froad.ibm.mq;

import java.io.IOException;
//�����쳣��ջ  
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.froad.mbank.pubfunc.Global;
import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;

public class TestClient extends AbstractJavaSamplerClient {

	private static transient Logger log = LoggingManager.getLoggerForClass();
	private static MQQueueManager mqQueueManager = null;
	private static MQQueue mqQueue = null;

	private static String MQ_MANAGER = "MQ远程队列的名称";
	private static String MQ_HOST_NAME = "主机名";
	private static String MQ_CHANNEL = "服务器连接的通道";
	private static String MQ_QUEUE_NAME = "MQ远程队列的名称";
	private static String MQ_PROT = "端口";
	private static String MQ_CCSID = "服务器MQ编码，1381代表GBK、1208代表UTF-8";
	private static String ID_NUM_STARTER = "idNum起始数字";
	private static String MB_NUM_STARTER = "mbNum起始数字";
	private static String THREAD_NUM = "循环次数";
	

	public SampleResult runTest(JavaSamplerContext context) {
		log.info("测试执行开始");
		SampleResult results = new SampleResult();
		results.setSuccessful(true);
		results.sampleStart();
		try {
			String pk = Global.qzOnlyKey();
			PutDataFromMq(GetTestDataFromMq(context), pk.getBytes(), pk.getBytes(), context);
		} catch (Throwable e) {
			e.printStackTrace();
			results.setSuccessful(false);
			results.setResponseData(toStringStackTrace(e), "utf8");
		}
		results.sampleEnd();
		return results;
	}

	/**
	 * 测试开始时调用，初始化
	 */
	public void setupTest(JavaSamplerContext context) {
		log.info("setup triggered");
		try {
			openMQ(context);
		} catch (MQException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 测试结束时调用
	 */
	public void teardownTest(JavaSamplerContext context) {
		log.info("teardown triggered");
		try {
			closeMQ();
		} catch (MQException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 定义默认参数
	 */
	@Override
	public Arguments getDefaultParameters() {
		Arguments args = new Arguments();
		args.addArgument(MQ_MANAGER, "FT1_OUT01");
		args.addArgument(MQ_HOST_NAME, "66.3.41.18");
		args.addArgument(MQ_CHANNEL, "SVRCONN_GW_OUT");
		args.addArgument(MQ_QUEUE_NAME, "IBM.SERVICE.REQUEST.OUT.FFT");
		args.addArgument(MQ_PROT, "10099");
		args.addArgument(MQ_CCSID, "1208");
		args.addArgument(ID_NUM_STARTER, "622826198809060000");
		args.addArgument(MB_NUM_STARTER, "13600000000");
		args.addArgument(THREAD_NUM, "${__threadNum}");
		return args;
	}

	/**
	 * 处理异常堆栈为String，只有String才能回写响应数据
	 * 
	 * @param e
	 * @return
	 */
	private String toStringStackTrace(Throwable e) {
		String exception = null;
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			exception = sw.toString();
			pw.close();
			sw.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return exception;
	}

	/**
	 * 打开MQ通道
	 * 
	 * @param @throws
	 *            MQException
	 * @return void
	 * @author FuJinpeng
	 * @date May 27, 2017
	 */
	public static void openMQ(JavaSamplerContext context) throws MQException {
		log.info("准备打开通道");
		try {
			MQEnvironment.addConnectionPoolToken();
			MQEnvironment.hostname = context.getParameter(MQ_HOST_NAME);
			MQEnvironment.channel = context.getParameter(MQ_CHANNEL);
			MQEnvironment.port = context.getIntParameter(MQ_PROT);
			MQEnvironment.properties.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES);
			MQEnvironment.CCSID = context.getIntParameter(MQ_CCSID);
			int openOptions = MQC.MQOO_OUTPUT | MQC.MQOO_FAIL_IF_QUIESCING;
			mqQueueManager = new MQQueueManager(context.getParameter(MQ_MANAGER));
			mqQueue = mqQueueManager.accessQueue(context.getParameter(MQ_QUEUE_NAME), openOptions, null, null, null);

			log.info("MQ建立通道成功!");

		} catch (Exception e) {
			log.error("MQ打开读取通道失败" + e.getMessage());
		}
	}

	/**
	 * 关闭MQ通道
	 * 
	 * @param @throws
	 *            MQException
	 * @return void
	 * @author FuJinpeng
	 * @date May 27, 2017
	 */
	public static void closeMQ() throws MQException {
		log.info("准备关闭通道");
		if (mqQueue != null) {
			try {
				mqQueue.close();
			} catch (MQException e) {
				log.error("MQ关闭写入通道失败" + e.getMessage());
			}
		}
		if (mqQueueManager != null) {
			try {
				mqQueueManager.disconnect();
				mqQueueManager.close();
				log.info("已关闭本次MQ写入通道");
			} catch (MQException e) {
				log.error("MQ关闭写入道管理器失败" + e.getMessage());
			}
		}
	}

	/**
	 * 把数据写入到MQ
	 * 
	 * @param @param
	 *            data
	 * @param @param
	 *            messageId
	 * @param @param
	 *            correlationId
	 * @return void
	 * @author FuJinpeng
	 * @date Jun 7, 2017
	 */
	public static void PutDataFromMq(byte[] data, byte[] messageId, byte[] correlationId, JavaSamplerContext context) {
		log.info("准备写入数据到MQ");
		try {
			MQMessage message = new MQMessage();
			message.expiry = -1;
			message.format = MQC.MQFMT_STRING;
			message.characterSet = context.getIntParameter(MQ_CCSID);
			message.messageId = messageId;
			message.correlationId = correlationId;
			message.write(data);
			mqQueue.put(message);
			mqQueueManager.commit();
		} catch (MQException e) {
			log.error("1.MQ写入失败" + e.getMessage());
		} catch (IOException e) {
			log.error("2.MQ写入失败" + e.getMessage());
		} catch (Exception ex) {
			log.error("3.MQ写入失败" + ex.getMessage());
		}
	}

	/**
	 * 构造测试数据
	 */
	public static byte[] GetTestDataFromMq(JavaSamplerContext context) throws Exception {
		log.info("准备构造测试数据");
		byte[] content = null;
		long currentThread = context.getLongParameter(THREAD_NUM);

		long idNum = context.getLongParameter(ID_NUM_STARTER) + currentThread;
		long mbNum = context.getLongParameter(MB_NUM_STARTER) + currentThread;

		String inputxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<Service>" + "<Service_Header>"
				+ "<service_response/>" + "<service_sn>12N0836571609024035</service_sn>"
				+ "<service_id>05400000000000</service_id>" + "<requester_id>0011</requester_id>"
				+ "<branch_id>320188999</branch_id>" + "<channel_id>03</channel_id>" + "<version_id>01</version_id>"
				+ "<service_time>20170607174057</service_time>"
				+ "<trace_msg>SERVICE.SINGLE.ROUTE - Sent single request to provider</trace_msg>"
				+ "<processes currentprocess=\"1\" total=\"1\">" + "<process id=\"1\">"
				+ "<service_id>00040000000000</service_id>" + "<status>INPROCESS</status>" + "</process>"
				+ "</processes>" + "</Service_Header>" + "<Service_Body>" + "<ext_attributes>"
				+ "<INM-MAC-FLAG>1</INM-MAC-FLAG>" + "<INM-BRANCH-ID>320188999</INM-BRANCH-ID>"
				+ "<INM-TERM-TYP>K</INM-TERM-TYP>" + "<INM-TERM-SRL>001</INM-TERM-SRL>" + "<INM-LAN-ID>01</INM-LAN-ID>"
				+ "<INM-TELLER-ID>320188999K02</INM-TELLER-ID>" + "<channelcode>002</channelcode>"
				+ "<channeldate>20170602</channeldate>" + "<channelseq/><brno>320188999</brno>"
				+ "<tellerno>320188999K02</tellerno>" + "<terminalno>000101</terminalno>"
				+ "<chktellerno>901T10100351</chktellerno>" + "<authtellerno>901T10100101</authtellerno>"
				+ "<authtellerno2>901T10100102</authtellerno2>" + "<sysid>000007</sysid>" + "<unitno>99952001</unitno>"
				+ "<subunitno>00000000</subunitno>" + "<agentflag>52999999</agentflag>"
				+ "<zoneno>901T10100001</zoneno>" + "<zhno>901T10100001</zhno>" + "<msgflag>08</msgflag>"
				+ "<returnflag>0000</returnflag>" + "<transcode>05400000000000</transcode>"
				+ "<INM-ENC-DEV-ID>01.wy.zpk</INM-ENC-DEV-ID>" + "<INM-PAGE-CTL>" + "</INM-PAGE-CTL>"
				+ "</ext_attributes>" + "<request>" + "<IdType>A</IdType>" + "<IdNumber>" + String.valueOf(idNum)
				+ "</IdNumber>" + "<MBNumber>" + String.valueOf(mbNum) + "</MBNumber>" + "<ChannelFalg>1</ChannelFalg>"
				+ "<MsgType>1</MsgType>" + "<Msg>您当前的验证码为:889006,请妥善保管好您的验证码!</Msg>" + "<_jym>995400</_jym>"
				+ "</request>" + "<response/>" + "</Service_Body>" + "</Service>";
		content = inputxml.getBytes("UTF-8");
		log.info("本次传输数据为" + inputxml);
		return content;
	}

	/**
	 * 调试方法
	 * @param args
	 */
	public static void main(String[] args) {
		Arguments argument = new Arguments();
		argument.addArgument(MQ_MANAGER, "FT1_OUT01");
		argument.addArgument(MQ_HOST_NAME, "66.3.41.18");
		argument.addArgument(MQ_CHANNEL, "SVRCONN_GW_OUT");
		argument.addArgument(MQ_QUEUE_NAME, "IBM.SERVICE.REQUEST.OUT.FFT");
		argument.addArgument(MQ_PROT, "10099");
		argument.addArgument(MQ_CCSID, "1208");
		argument.addArgument(ID_NUM_STARTER, "622826198809060000");
		argument.addArgument(MB_NUM_STARTER, "13600000000");
		argument.addArgument(THREAD_NUM, "1");
		TestClient test = new TestClient();
		JavaSamplerContext context = new JavaSamplerContext(argument);
		test.setupTest(context);
		test.runTest(context);
		test.teardownTest(context);
	}

}
